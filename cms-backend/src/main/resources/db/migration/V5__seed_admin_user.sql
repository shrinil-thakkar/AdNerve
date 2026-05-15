INSERT INTO users (id, email, password_hash, is_active)
VALUES (
    gen_random_uuid(),
    'admin@magnificomarketing.in',
    '$2b$10$G1JccITc7IndfBkiKQMEBuF5lp5b6HyrdmTLwi8xEAnuXJnQalXM6',
    TRUE
);
