-- 仅用于 local Profile 联调。密码哈希不对应一个可公开使用的登录密码。
-- 正式登录注册实现后，应删除该本地测试用户及其替代方案。

SET NAMES utf8mb4;
USE `teachertalk`;

INSERT IGNORE INTO `user` (
    `id`,
    `phone`,
    `password_hash`,
    `nickname`,
    `avatar_url`,
    `role`,
    `created_at`,
    `updated_at`
) VALUES (
    1,
    '19900000000',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '本地测试用户',
    NULL,
    'STUDENT',
    NOW(),
    NOW()
);
