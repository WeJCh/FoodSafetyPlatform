-- user-service schema
CREATE DATABASE IF NOT EXISTS food_user_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_user_db;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码',
  real_name VARCHAR(50) COMMENT '真实姓名',
  phone VARCHAR(20) COMMENT '手机号',
  user_type VARCHAR(20) NOT NULL COMMENT 'ADMIN / REGULATOR / ENTERPRISE / PUBLIC',
  status TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(64) NOT NULL COMMENT '服务名称',
  operator_user_id BIGINT COMMENT '操作人用户ID',
  operator_user_type VARCHAR(32) COMMENT '操作人类型',
  operator_name VARCHAR(100) COMMENT '操作人名称',
  target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
  target_id BIGINT NOT NULL COMMENT '目标ID',
  target_user_id BIGINT COMMENT '目标用户ID',
  target_name VARCHAR(200) COMMENT '目标名称',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
  action_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  action_name VARCHAR(100) COMMENT '操作名称',
  before_data LONGTEXT COMMENT '变更前快照JSON',
  after_data LONGTEXT COMMENT '变更后快照JSON',
  success_flag TINYINT DEFAULT 1 COMMENT '是否成功 1-成功 0-失败',
  error_message VARCHAR(500) COMMENT '错误信息',
  remark VARCHAR(500) COMMENT '备注',
  client_ip VARCHAR(64) COMMENT '客户端IP',
  trace_id VARCHAR(128) COMMENT '链路追踪ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_audit_target (target_type, target_id),
  KEY idx_audit_operator (operator_user_id, create_time),
  KEY idx_audit_action (action_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 初始化角色数据
INSERT IGNORE INTO sys_role (role_code, role_name)
VALUES
  ('ADMIN', '系统管理员'),
  ('PUBLIC', '公众用户'),
  ('ENTERPRISE', '企业用户'),
  ('REGULATOR_ADMIN', '区域管理员'),
  ('REGULATOR_ENFORCER', '执法人员');
