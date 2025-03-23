-- ROLEの初期データ
-- Note: nameフィールドにはプレフィックスなしの値を設定
INSERT INTO roles (id, name, prefix) VALUES (1, 'ADMIN', 'ROLE_');
INSERT INTO roles (id, name, prefix) VALUES (2, 'USER', 'ROLE_');
INSERT INTO roles (id, name, prefix) VALUES (3, 'MODERATOR', 'ROLE_');

-- 管理者ユーザーの初期データ
-- パスワード: admin123 (BCryptでハッシュ化済み)
INSERT INTO users (id, name, email, password, account_non_expired, account_non_locked, credentials_non_expired, enabled, created_at, updated_at)
VALUES (1, 'admin', 'admin@example.com', '$2a$10$6HKTFSpDkoI8zr3ZJUKeKuHU8ZOK5QyeBxKSx3Ru9TAkoT1c8WN.K',
true, true, true, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 管理者ユーザーにADMIN役割を割り当て
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);