INSERT INTO public.authorities (id, name, created_at)
VALUES (gen_random_uuid(), 'ROLE_USER', now())
    ON CONFLICT (name) DO NOTHING;