-- ROLEの初期データ
INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_USER');
INSERT INTO roles (id, name) VALUES (3, 'ROLE_MODERATOR');

-- テストユーザーの初期データ
-- パスワード: test123 (BCryptでハッシュ化済み)
INSERT INTO users (id, name, email, password, account_non_expired, account_non_locked, credentials_non_expired, enabled, created_at, updated_at)
VALUES (1, 'testAdmin', 'test@example.com', '$2a$10$8iHJ0spbYTYeqwUz.BXJ5.HkN0iJRJRyR9VpVf5e9L3HGq5qiQDQ2',
true, true, true, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- テストユーザーにROLE_ADMINを割り当て
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);