-- user-service 基础数据初始化
USE food_user_db;

-- ============================================
-- 1. 系统管理员账号
-- ============================================
-- 密码: 123456
INSERT INTO sys_user (id, username, password, real_name, phone, user_type, status, deleted)
VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800000001', 'ADMIN', 1, 0);

-- ============================================
-- 2. 区域管理员账号（4个）
-- ============================================
-- 密码: 123456
INSERT INTO sys_user (id, username, password, real_name, phone, user_type, status, deleted)
VALUES
(2, 'regulator_admin_1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张区域', '13800000002', 'REGULATOR', 1, 0),
(3, 'regulator_admin_2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李区域', '13800000003', 'REGULATOR', 1, 0),
(17, 'regulator_admin_3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周区域', '13800000017', 'REGULATOR', 1, 0),
(18, 'regulator_admin_4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴区域', '13800000018', 'REGULATOR', 1, 0);

-- ============================================
-- 3. 执法人员账号（4个）
-- ============================================
-- 密码: 123456
INSERT INTO sys_user (id, username, password, real_name, phone, user_type, status, deleted)
VALUES
(4, 'enforcer_1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王执法', '13800000004', 'REGULATOR', 1, 0),
(5, 'enforcer_2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵执法', '13800000005', 'REGULATOR', 1, 0),
(6, 'enforcer_3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '刘执法', '13800000006', 'REGULATOR', 1, 0),
(7, 'enforcer_4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '陈执法', '13800000007', 'REGULATOR', 1, 0);

-- ============================================
-- 4. 企业用户账号（6个）
-- ============================================
-- 密码: 123456
INSERT INTO sys_user (id, username, password, real_name, phone, user_type, status, deleted)
VALUES
(8, 'enterprise_1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张企业', '13800000008', 'ENTERPRISE', 1, 0),
(9, 'enterprise_2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李企业', '13800000009', 'ENTERPRISE', 1, 0),
(10, 'enterprise_3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王企业', '13800000010', 'ENTERPRISE', 1, 0),
(11, 'enterprise_4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵企业', '13800000011', 'ENTERPRISE', 1, 0),
(12, 'enterprise_5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '刘企业', '13800000012', 'ENTERPRISE', 1, 0),
(13, 'enterprise_6', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '陈企业', '13800000013', 'ENTERPRISE', 1, 0);

-- ============================================
-- 5. 公众用户账号（3个）
-- ============================================
-- 密码: 123456
INSERT INTO sys_user (id, username, password, real_name, phone, user_type, status, deleted)
VALUES
(14, 'public_1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '市民张三', '13800000014', 'PUBLIC', 1, 0),
(15, 'public_2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '市民李四', '13800000015', 'PUBLIC', 1, 0),
(16, 'public_3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '市民王五', '13800000016', 'PUBLIC', 1, 0);

-- ============================================
-- 6. 用户角色关联
-- ============================================
INSERT INTO sys_user_role (user_id, role_id, deleted)
VALUES (1, 1, 0);

INSERT INTO sys_user_role (user_id, role_id, deleted)
VALUES
(2, 4, 0),
(3, 4, 0),
(17, 4, 0),
(18, 4, 0);

INSERT INTO sys_user_role (user_id, role_id, deleted)
VALUES
(4, 5, 0),
(5, 5, 0),
(6, 5, 0),
(7, 5, 0);

INSERT INTO sys_user_role (user_id, role_id, deleted)
VALUES
(8, 3, 0),
(9, 3, 0),
(10, 3, 0),
(11, 3, 0),
(12, 3, 0),
(13, 3, 0);

INSERT INTO sys_user_role (user_id, role_id, deleted)
VALUES
(14, 2, 0),
(15, 2, 0),
(16, 2, 0);

-- 重置自增 ID
ALTER TABLE sys_user AUTO_INCREMENT = 100;
ALTER TABLE sys_user_role AUTO_INCREMENT = 100;
