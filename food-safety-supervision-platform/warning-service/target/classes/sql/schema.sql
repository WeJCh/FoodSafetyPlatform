-- warning-service schema
CREATE DATABASE IF NOT EXISTS food_warning_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_warning_db;

-- 原型阶段按依赖顺序重建表结构，避免历史字段干扰新逻辑
DROP TABLE IF EXISTS warning_process_log;
DROP TABLE IF EXISTS warning_record;
DROP TABLE IF EXISTS warning_rule;

CREATE TABLE IF NOT EXISTS warning_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
  rule_type VARCHAR(50) COMMENT '规则类型',
  threshold INT COMMENT '阈值',
  enabled TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

CREATE TABLE IF NOT EXISTS warning_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warning_no VARCHAR(40) COMMENT '预警编号',
  warning_type VARCHAR(64) NOT NULL COMMENT '预警类型',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
  biz_id BIGINT NOT NULL COMMENT '业务ID',
  region_id BIGINT COMMENT '辖区ID（用于管理员权限过滤）',
  owner_regulator_id BIGINT COMMENT '责任执法员ID（用于执法员权限过滤）',
  dedup_key VARCHAR(120) NOT NULL COMMENT '幂等去重键',
  level VARCHAR(16) DEFAULT 'L1' COMMENT 'L1 / L2',
  status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN / ACKED / PROCESSING / RESOLVED / CLOSED',
  title VARCHAR(100) COMMENT '预警标题',
  content VARCHAR(500) COMMENT '预警内容',
  source_service VARCHAR(50) COMMENT '来源服务',
  first_occur_time DATETIME COMMENT '首次触发时间',
  last_occur_time DATETIME COMMENT '最近触发时间',
  trigger_count INT DEFAULT 1 COMMENT '触发次数',
  assigned_to BIGINT COMMENT '指派处理人ID',
  assigned_time DATETIME COMMENT '指派时间',
  resolved_by BIGINT COMMENT '解决人ID',
  resolved_time DATETIME COMMENT '解决时间',
  close_reason VARCHAR(500) COMMENT '关闭原因',
  payload_json TEXT COMMENT '扩展负载',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_warning_dedup (dedup_key),
  KEY idx_warning_type_status (warning_type, status),
  KEY idx_warning_biz (biz_type, biz_id),
  KEY idx_warning_level_status (level, status),
  KEY idx_warning_region_status (region_id, status),
  KEY idx_warning_owner_status (owner_regulator_id, status),
  KEY idx_warning_assigned_status (assigned_to, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

CREATE TABLE IF NOT EXISTS warning_process_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warning_id BIGINT NOT NULL COMMENT '预警ID',
  action_type VARCHAR(32) NOT NULL COMMENT 'EVENT_UPSERT / ACK / ASSIGN / RESOLVE / CLOSE / LEVEL_UP / AUTO_LEVEL_UP',
  operator_id BIGINT COMMENT '操作人ID',
  operator_name VARCHAR(50) COMMENT '操作人名称',
  action_comment VARCHAR(500) COMMENT '处理说明',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_warning_process_warning (warning_id),
  KEY idx_warning_process_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警处理日志';
