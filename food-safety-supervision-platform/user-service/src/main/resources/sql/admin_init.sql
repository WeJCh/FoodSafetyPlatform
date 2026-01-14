-- Initialize the unique system administrator (username: admin, password: Admin@123)
INSERT INTO sys_user (username, password, real_name, user_type, status, create_time, update_time)
SELECT 'admin', '.2UsuKCrNSXFi./.hNCYtLMtkZhDu.8ERMQ4kK4X9y1r.c6', '系统管理员', 'ADMIN', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
