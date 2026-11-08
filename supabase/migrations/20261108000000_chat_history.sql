-- WanderAI: historial del Chat de viaje IA por usuario.
-- Requiere 20261107000000_wanderai_core.sql (tabla profiles y función touch_updated_at).
-- Aplicar en Supabase: SQL Editor -> pegar y ejecutar, o `supabase db push`. Idempotente.

create table if not exists public.chat_history (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references public.profiles (id) on delete cascade,
    title      text not null check (char_length(title) between 1 and 120),
    -- [{"role": "USER"|"ASSISTANT", "text": "...", "places": [{"id": "...", "name": "...", ...}]}]
    messages   jsonb not null default '[]'::jsonb check (jsonb_typeof(messages) = 'array'),
    created_at timestamptz not null default now(),
    -- Ordena el historial por última actividad al retomar una conversación.
    updated_at timestamptz not null default now()
);

create index if not exists chat_history_user_updated_idx
    on public.chat_history (user_id, updated_at desc);

drop trigger if exists chat_history_touch_updated_at on public.chat_history;
create trigger chat_history_touch_updated_at
    before update on public.chat_history
    for each row execute function public.touch_updated_at();

alter table public.chat_history enable row level security;

drop policy if exists "chat_history_select_own" on public.chat_history;
create policy "chat_history_select_own" on public.chat_history
    for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "chat_history_insert_own" on public.chat_history;
create policy "chat_history_insert_own" on public.chat_history
    for insert to authenticated with check ((select auth.uid()) = user_id);

drop policy if exists "chat_history_update_own" on public.chat_history;
create policy "chat_history_update_own" on public.chat_history
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

drop policy if exists "chat_history_delete_own" on public.chat_history;
create policy "chat_history_delete_own" on public.chat_history
    for delete to authenticated using ((select auth.uid()) = user_id);
