-- テストデータをクリーンアップ
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM roles;

-- ロールの作成
INSERT INTO roles (id, name, prefix) VALUES 
(1, 'ADMIN', 'ROLE_'),
(2, 'USER', 'ROLE_');

-- テストユーザーの作成（パスワード: admin123）
INSERT INTO users (
    id, 
    name, 
    email, 
    password, 
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    1, 
    'Admin User', 
    'admin@example.com',
    '$2a$10$rNQkxF8oNxHFnwRbjX1d8.KhhhpIX9XYWqX.zf0.XoxFR6iLHXm3m',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP(),
    CURRENT_TIMESTAMP()
);

-- 一般ユーザーの作成（パスワード: user123）
INSERT INTO users (
    id, 
    name, 
    email, 
    password, 
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    2, 
    'Test User', 
    'user@example.com',
    '$2a$10$6HxaT9J0YkXYDQ.OqS6ANu1JYbKprvzZkoSYyXKsW6ggBGTiGgGOa',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP(),
    CURRENT_TIMESTAMP()
);

-- 無効化されたユーザーの作成（パスワード: disabled123）
INSERT INTO users (
    id, 
    name, 
    email, 
    password, 
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    3, 
    'Disabled User', 
    'disabled@example.com',
    '$2a$10$8KzI1UT4c6QYXHiScQvb7ubvE8HKp4kqhS0KJXkR0ZVnU.037p.p2',
    false,
    true,
    true,
    true,
    CURRENT_TIMESTAMP(),
    CURRENT_TIMESTAMP()
);

-- ユーザーとロールの関連付け
INSERT INTO users_roles (user_id, role_id) VALUES 
(1, 1), -- Admin User -> ADMIN
(1, 2), -- Admin User -> USER
(2, 2), -- Test User -> USER
(3, 2); -- Disabled User -> USER