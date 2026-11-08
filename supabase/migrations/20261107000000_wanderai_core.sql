-- WanderAI: perfiles, logros, itinerarios guardados y borrado de cuenta (GDPR).
-- Aplicar en Supabase: SQL Editor -> pegar y ejecutar, o `supabase db push`.
-- Es idempotente: se puede ejecutar varias veces.

-- ---------------------------------------------------------------------------
-- profiles: 1 fila por usuario de auth.users
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
    id           uuid primary key references auth.users (id) on delete cascade,
    display_name text not null default '',
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

alter table public.profiles enable row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
    for select to authenticated using ((select auth.uid()) = id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
    for insert to authenticated with check ((select auth.uid()) = id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
    for update to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id);

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
    before update on public.profiles
    for each row execute function public.touch_updated_at();

-- Crea el perfil al registrarse (nombre tomado de user_metadata.full_name).
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (id, display_name)
    values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)))
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Usuarios que ya existían antes de esta migración.
insert into public.profiles (id, display_name)
select u.id, coalesce(u.raw_user_meta_data ->> 'full_name', split_part(u.email, '@', 1))
from auth.users u
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- achievements: catálogo (códigos = enum AchievementType de la app)
-- ---------------------------------------------------------------------------
create table if not exists public.achievements (
    code        text primary key,
    title_es    text not null,
    title_en    text not null,
    goal        integer not null,
    description text not null default ''
);

alter table public.achievements enable row level security;

drop policy if exists "achievements_read_all" on public.achievements;
create policy "achievements_read_all" on public.achievements
    for select to authenticated using (true);

insert into public.achievements (code, title_es, title_en, goal, description) values
    ('FIRST_ROUTE',       'Primera ruta',         'First route',       1,  'Guardar el primer itinerario'),
    ('COLONIAL_EXPLORER', 'Explorador colonial',  'Colonial explorer', 5,  'Incluir 5 templos o monumentos distintos'),
    ('WALKER_50K',        'Caminante 50 km',      '50 km walker',      50, 'Planificar 50 km de recorridos'),
    ('VIEWPOINT_HUNTER',  'Cazador de miradores', 'Viewpoint hunter',  3,  'Incluir 3 miradores distintos'),
    ('FOODIE',            'Sibarita',             'Foodie',            6,  'Incluir 6 restaurantes distintos')
on conflict (code) do update
    set title_es = excluded.title_es,
        title_en = excluded.title_en,
        goal = excluded.goal,
        description = excluded.description;

-- ---------------------------------------------------------------------------
-- user_achievements: logros desbloqueados por cada usuario
-- ---------------------------------------------------------------------------
create table if not exists public.user_achievements (
    user_id          uuid not null references public.profiles (id) on delete cascade,
    achievement_code text not null references public.achievements (code) on delete cascade,
    unlocked_at      timestamptz not null default now(),
    primary key (user_id, achievement_code)
);

alter table public.user_achievements enable row level security;

drop policy if exists "user_achievements_select_own" on public.user_achievements;
create policy "user_achievements_select_own" on public.user_achievements
    for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "user_achievements_insert_own" on public.user_achievements;
create policy "user_achievements_insert_own" on public.user_achievements
    for insert to authenticated with check ((select auth.uid()) = user_id);

-- ---------------------------------------------------------------------------
-- saved_itineraries: itinerarios guardados (paradas = ids del catálogo local)
-- ---------------------------------------------------------------------------
create table if not exists public.saved_itineraries (
    id                 text not null,
    user_id            uuid not null references public.profiles (id) on delete cascade,
    created_at_ms      bigint not null,
    total_minutes      integer not null,
    total_distance_m   integer not null,
    estimated_cost_cop integer not null,
    -- [{"place_id": "...", "walk_min": 8, "dist_m": 650}, ...]
    stops              jsonb not null,
    created_at         timestamptz not null default now(),
    primary key (user_id, id)
);

create index if not exists saved_itineraries_user_created_idx
    on public.saved_itineraries (user_id, created_at_ms desc);

alter table public.saved_itineraries enable row level security;

drop policy if exists "saved_itineraries_select_own" on public.saved_itineraries;
create policy "saved_itineraries_select_own" on public.saved_itineraries
    for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "saved_itineraries_insert_own" on public.saved_itineraries;
create policy "saved_itineraries_insert_own" on public.saved_itineraries
    for insert to authenticated with check ((select auth.uid()) = user_id);

drop policy if exists "saved_itineraries_update_own" on public.saved_itineraries;
create policy "saved_itineraries_update_own" on public.saved_itineraries
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

drop policy if exists "saved_itineraries_delete_own" on public.saved_itineraries;
create policy "saved_itineraries_delete_own" on public.saved_itineraries
    for delete to authenticated using ((select auth.uid()) = user_id);

-- ---------------------------------------------------------------------------
-- delete_user_account(): GDPR. Borra al llamante de auth.users; profiles,
-- user_achievements y saved_itineraries caen por ON DELETE CASCADE.
-- ---------------------------------------------------------------------------
create or replace function public.delete_user_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    delete from auth.users where id = uid;
end;
$$;

revoke all on function public.delete_user_account() from public, anon;
grant execute on function public.delete_user_account() to authenticated;
