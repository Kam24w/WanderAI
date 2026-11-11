-- =============================================================================
-- WanderAI: carga datos de DEMOSTRACIÓN en una cuenta demo.
--
-- Son datos de ejemplo para mostrar las pantallas de Perfil y Logros con
-- historial; no son recorridos reales. Se identifican así:
--   * el perfil se llama "Demo WanderAI";
--   * los itinerarios tienen id "demo-01" ... "demo-08".
--
-- Uso:
--   1. Crea la cuenta demo desde la app (Registrarse) con un correo tuyo.
--   2. Cambia demo_email abajo por ese correo.
--   3. Supabase -> SQL Editor -> pega y ejecuta este archivo.
--   4. En la app cierra sesión y vuelve a entrar con la cuenta demo.
--
-- Es idempotente: borra y vuelve a crear solo las filas "demo-*" de esa cuenta.
-- Nunca toca otros usuarios. Para quitar los datos: ver el bloque final.
-- =============================================================================
do $$
declare
    demo_email constant text := 'CAMBIA-ESTE-CORREO@ejemplo.com';
    uid uuid;
begin
    select id into uid from auth.users where lower(email) = lower(demo_email);
    if uid is null then
        raise exception 'No existe una cuenta con el correo %. Créala primero desde la app.', demo_email;
    end if;

    insert into public.profiles (id, display_name) values (uid, 'Demo WanderAI')
    on conflict (id) do update set display_name = excluded.display_name;

    delete from public.saved_itineraries where user_id = uid and id like 'demo-%';

    -- 8 itinerarios en las últimas 6 semanas (18.9 km planificados) por lugares
    -- reales del catálogo de Pasto. Distancias: línea recta x 1,3 (calles); 75 m/min a pie.
    insert into public.saved_itineraries
        (id, user_id, created_at_ms, total_minutes, total_distance_m, estimated_cost_cop, stops)
    select v.id, uid, v.created_at_ms, v.total_minutes, v.total_distance_m, v.estimated_cost_cop, v.stops
    from (values
        ('demo-01', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '39 days' + interval '10 hours') at time zone 'America/Bogota') * 1000)::bigint, 109, 929, 15000, '[{"place_id": "plaza-narino", "walk_min": 2, "dist_m": 120}, {"place_id": "catedral", "walk_min": 4, "dist_m": 264}, {"place_id": "san-juan", "walk_min": 3, "dist_m": 172}, {"place_id": "cafe-galeras", "walk_min": 5, "dist_m": 373}]'::jsonb),
        ('demo-02', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '33 days' + interval '15 hours') at time zone 'America/Bogota') * 1000)::bigint, 121, 1417, 26000, '[{"place_id": "san-felipe", "walk_min": 14, "dist_m": 998}, {"place_id": "taminango", "walk_min": 4, "dist_m": 238}, {"place_id": "pasto-cafe", "walk_min": 3, "dist_m": 181}]'::jsonb),
        ('demo-03', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '26 days' + interval '9 hours') at time zone 'America/Bogota') * 1000)::bigint, 132, 1190, 28000, '[{"place_id": "cristo-rey", "walk_min": 3, "dist_m": 217}, {"place_id": "museo-oro", "walk_min": 7, "dist_m": 486}, {"place_id": "sabores-narino", "walk_min": 7, "dist_m": 487}]'::jsonb),
        ('demo-04', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '24 days' + interval '16 hours') at time zone 'America/Bogota') * 1000)::bigint, 88, 2424, 0, '[{"place_id": "mirador-centenario", "walk_min": 21, "dist_m": 1530}, {"place_id": "parque-bolivar", "walk_min": 12, "dist_m": 894}]'::jsonb),
        ('demo-05', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '17 days' + interval '11 hours') at time zone 'America/Bogota') * 1000)::bigint, 156, 4816, 5000, '[{"place_id": "parque-infantil", "walk_min": 12, "dist_m": 840}, {"place_id": "mirador-rumipamba", "walk_min": 21, "dist_m": 1532}, {"place_id": "museo-carnaval", "walk_min": 33, "dist_m": 2444}]'::jsonb),
        ('demo-06', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '12 days' + interval '14 hours') at time zone 'America/Bogota') * 1000)::bigint, 141, 2216, 55000, '[{"place_id": "catedral", "walk_min": 4, "dist_m": 264}, {"place_id": "san-felipe", "walk_min": 11, "dist_m": 802}, {"place_id": "cuyeria", "walk_min": 16, "dist_m": 1150}]'::jsonb),
        ('demo-07', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '5 days' + interval '8 hours') at time zone 'America/Bogota') * 1000)::bigint, 141, 4507, 8000, '[{"place_id": "mirador-galeras", "walk_min": 36, "dist_m": 2685}, {"place_id": "taminango", "walk_min": 25, "dist_m": 1822}]'::jsonb),
        ('demo-08', (extract(epoch from (date_trunc('day', now() at time zone 'America/Bogota') - interval '2 days' + interval '17 hours') at time zone 'America/Bogota') * 1000)::bigint, 115, 1433, 18000, '[{"place_id": "plaza-narino", "walk_min": 2, "dist_m": 120}, {"place_id": "san-juan", "walk_min": 2, "dist_m": 120}, {"place_id": "cristo-rey", "walk_min": 4, "dist_m": 294}, {"place_id": "pasto-cafe", "walk_min": 12, "dist_m": 899}]'::jsonb)
    ) as v(id, created_at_ms, total_minutes, total_distance_m, estimated_cost_cop, stops);

    -- Logros que esos itinerarios cumplen de verdad según TravelerStatsCalculator:
    --   FIRST_ROUTE (1 itinerario) y VIEWPOINT_HUNTER (3 miradores distintos).
    -- Los demás quedan en progreso: Explorador colonial 4/5, Sibarita 4/6, Caminante 18.9/50 km.
    insert into public.user_achievements (user_id, achievement_code)
    values (uid, 'FIRST_ROUTE'), (uid, 'VIEWPOINT_HUNTER')
    on conflict (user_id, achievement_code) do nothing;

    raise notice 'Datos demo cargados para % (8 itinerarios).', demo_email;
end;
$$;

-- Para borrar los datos demo (cambia el correo):
-- delete from public.saved_itineraries
--  where id like 'demo-%' and user_id = (select id from auth.users where email = 'CAMBIA-ESTE-CORREO@ejemplo.com');
-- delete from public.user_achievements
--  where user_id = (select id from auth.users where email = 'CAMBIA-ESTE-CORREO@ejemplo.com');
