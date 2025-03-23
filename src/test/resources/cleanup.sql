-- 外部キー制約を考慮して削除順序を設定
DELETE FROM user_roles;
DELETE FROM users;
DELETE FROM roles;