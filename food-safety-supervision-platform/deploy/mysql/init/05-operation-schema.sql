CREATE DATABASE IF NOT EXISTS food_regulation_operation_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_regulation_operation_db;

DROP TABLE IF EXISTS sampling_result;
DROP TABLE IF EXISTS sampling_task;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS warning_event_outbox;
DROP TABLE IF EXISTS rectification_action_log;
DROP TABLE IF EXISTS rectification_task;
DROP TABLE IF EXISTS inspection_item;
DROP TABLE IF EXISTS inspection_record;
DROP TABLE IF EXISTS inspection_task;

CREATE TABLE IF NOT EXISTS inspection_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(40) NOT NULL COMMENT '任务编号',
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  region_id BIGINT NOT NULL COMMENT '所属行政区',
  task_title VARCHAR(100) NOT NULL COMMENT '任务标题',
  task_desc VARCHAR(500) COMMENT '任务描述',
  priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT 'LOW / MEDIUM / HIGH',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT 'CREATED / ASSIGNED / IN_PROGRESS / COMPLETED / CLOSED',
  created_by BIGINT NOT NULL COMMENT '创建人ID',
  assigned_to BIGINT COMMENT '指派给',
  assigned_by BIGINT COMMENT '指派人',
  assigned_time DATETIME COMMENT '指派时间',
  started_time DATETIME COMMENT '开始执行时间',
  completed_time DATETIME COMMENT '完成时间',
  deadline DATETIME COMMENT '截止时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_task_enterprise (enterprise_id),
  KEY idx_task_region (region_id),
  KEY idx_task_status (status),
  KEY idx_task_assigned (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查任务表';

CREATE TABLE IF NOT EXISTS sampling_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(40) NOT NULL COMMENT '任务编号',
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  region_id BIGINT NOT NULL COMMENT '所属行政区',
  task_title VARCHAR(100) NOT NULL COMMENT '任务标题',
  task_desc VARCHAR(500) COMMENT '任务描述',
  priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT 'LOW / MEDIUM / HIGH',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT 'CREATED / ASSIGNED / COMPLETED / CLOSED',
  created_by BIGINT NOT NULL COMMENT '创建人ID',
  assigned_to BIGINT COMMENT '指派给',
  assigned_by BIGINT COMMENT '指派人',
  assigned_time DATETIME COMMENT '指派时间',
  completed_time DATETIME COMMENT '完成时间',
  deadline DATETIME COMMENT '截止时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_sampling_task_enterprise (enterprise_id),
  KEY idx_sampling_task_product (product_id),
  KEY idx_sampling_task_region (region_id),
  KEY idx_sampling_task_status (status),
  KEY idx_sampling_task_assigned (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽检任务表';

CREATE TABLE IF NOT EXISTS sampling_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL COMMENT '抽检任务ID',
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  sampled_by BIGINT NOT NULL COMMENT '采样执行人',
  sampled_time DATETIME NOT NULL COMMENT '采样时间',
  result VARCHAR(20) NOT NULL COMMENT 'PASS / FAIL',
  conclusion VARCHAR(500) COMMENT '抽检结论',
  disposal_suggestion VARCHAR(500) COMMENT '处置建议',
  public_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED / OFFLINE',
  published_time DATETIME COMMENT '公示时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_sampling_result_task (task_id),
  KEY idx_sampling_result_enterprise (enterprise_id),
  KEY idx_sampling_result_product (product_id),
  KEY idx_sampling_result_public (public_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽检结果表';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(64) NOT NULL COMMENT 'service name',
  operator_user_id BIGINT COMMENT 'operator user id',
  operator_user_type VARCHAR(32) COMMENT 'operator user type',
  operator_name VARCHAR(100) COMMENT 'operator name',
  target_type VARCHAR(32) NOT NULL COMMENT 'target type',
  target_id BIGINT NOT NULL COMMENT 'target id',
  target_user_id BIGINT COMMENT 'target user id',
  target_name VARCHAR(200) COMMENT 'target name',
  biz_type VARCHAR(32) NOT NULL COMMENT 'business type',
  action_type VARCHAR(64) NOT NULL COMMENT 'action type',
  action_name VARCHAR(100) COMMENT 'action name',
  before_data LONGTEXT COMMENT 'before snapshot json',
  after_data LONGTEXT COMMENT 'after snapshot json',
  success_flag TINYINT DEFAULT 1 COMMENT '1 success 0 fail',
  error_message VARCHAR(500) COMMENT 'error message',
  remark VARCHAR(500) COMMENT 'remark',
  client_ip VARCHAR(64) COMMENT 'client ip',
  trace_id VARCHAR(128) COMMENT 'trace id',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  KEY idx_audit_target (target_type, target_id),
  KEY idx_audit_biz (biz_type, create_time),
  KEY idx_audit_action (action_type, create_time),
  KEY idx_audit_operator (operator_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='audit log';

CREATE TABLE IF NOT EXISTS inspection_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT COMMENT '任务ID',
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  inspector_id BIGINT NOT NULL COMMENT '检查人员ID',
  inspection_date DATE COMMENT '检查日期',
  result VARCHAR(20) COMMENT 'PASS / FAIL',
  problem_desc TEXT COMMENT '问题描述',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监督检查记录表';

CREATE TABLE IF NOT EXISTS inspection_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inspection_id BIGINT NOT NULL COMMENT '检查记录ID',
  item_name VARCHAR(100) NOT NULL COMMENT '检查项',
  item_result VARCHAR(20) COMMENT 'PASS / FAIL',
  problem_desc TEXT COMMENT '问题描述',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查明细表';

CREATE TABLE IF NOT EXISTS rectification_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inspection_id BIGINT NOT NULL COMMENT '检查记录ID',
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  rectification_desc TEXT COMMENT '整改要求',
  progress VARCHAR(1000) COMMENT '整改进度',
  status VARCHAR(20) DEFAULT 'ONGOING' COMMENT 'ONGOING / SUBMITTED / REWORK / CONFIRMED',
  submit_deadline DATETIME COMMENT '企业提交截止时间',
  review_deadline DATETIME COMMENT '监管复核截止时间',
  finish_time DATETIME COMMENT '完成时间',
  confirmed_by BIGINT COMMENT '复核人ID',
  confirmed_time DATETIME COMMENT '复核时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_rectification_inspection (inspection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改任务表';

CREATE TABLE IF NOT EXISTS rectification_action_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rectification_id BIGINT NOT NULL COMMENT '整改任务ID',
  action_type VARCHAR(30) NOT NULL COMMENT 'SYSTEM_CREATE / ENTERPRISE_SUBMIT / REVIEW_CONFIRM / REVIEW_REWORK / SLA_OVERDUE_*',
  operator_id BIGINT COMMENT '操作人ID',
  action_comment VARCHAR(1000) COMMENT '操作说明',
  attachment_urls TEXT COMMENT '附件URL(JSON)',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_rectification_log_task (rectification_id),
  KEY idx_rectification_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改动作日志表';

CREATE TABLE IF NOT EXISTS warning_event_outbox (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_key VARCHAR(120) NOT NULL COMMENT '事件唯一键（与 dedupKey 对齐）',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
  payload_json LONGTEXT NOT NULL COMMENT '上报 warning-service 的完整请求体',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SENT / DEAD',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
  last_attempt_time DATETIME COMMENT '最近投递时间',
  last_error VARCHAR(500) COMMENT '最近失败原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_warning_outbox_event_key (event_key),
  KEY idx_warning_outbox_status_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警事件Outbox表';
