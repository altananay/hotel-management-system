DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_database
        WHERE datname = current_database()
          AND shobj_description(oid, 'pg_database') = 'local-ci-seed'
    ) THEN
        RAISE EXCEPTION 'admin seed requires local-ci-seed database marker';
    END IF;
END
$$;

INSERT INTO admin_platform.admin_users (
    id,
    username,
    email,
    password_hash,
    role,
    active,
    created_at,
    updated_at
)
VALUES
    (
        '0199f2c0-0000-7000-8000-000000004001',
        'superadmin',
        'superadmin@example.com',
        '$argon2id$v=19$m=19456,t=2,p=1$Eqo2f9il+nqcrcOVldir0A$T45wCeuzp9w/7KX5hS8eIWPlrb7WEKLzhZmIjT7UcVE',
        'SUPER_ADMIN',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000004002',
        'manager',
        'manager@example.com',
        '$argon2id$v=19$m=19456,t=2,p=1$57yq37gR0It3DAzbrNcBew$6aTmBESSm1GMvQfHD90I9AbvYwIN+j6EYd4dx1SRs8I',
        'MANAGER',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000004003',
        'frontdesk',
        'frontdesk@example.com',
        '$argon2id$v=19$m=19456,t=2,p=1$FYoruOVwfn5WNQm9C+kPfg$IYw0vxZOmcjzcESp2hnn8v0u3h8KUrwFAA+POaMvZYU',
        'FRONT_DESK',
        TRUE,
        NOW(),
        NOW()
    )
ON CONFLICT (lower(username)) DO NOTHING;
