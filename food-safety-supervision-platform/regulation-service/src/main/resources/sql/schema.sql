-- regulation-service schema
CREATE DATABASE IF NOT EXISTS food_regulation_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_regulation_db;

-- 按照依赖顺序删除表（先删除有外键约束的子表，再删除父表）
DROP TABLE IF EXISTS complaint_handle;
DROP TABLE IF EXISTS complaint;
DROP TABLE IF EXISTS warning_event_outbox;
DROP TABLE IF EXISTS rectification_action_log;
DROP TABLE IF EXISTS rectification_task;
DROP TABLE IF EXISTS enterprise_key_reason;
DROP TABLE IF EXISTS inspection_item;
DROP TABLE IF EXISTS inspection_record;
DROP TABLE IF EXISTS inspection_task;
DROP TABLE IF EXISTS food_regulator_region;
DROP TABLE IF EXISTS addr_location;
DROP TABLE IF EXISTS addr_region;
DROP TABLE IF EXISTS food_enterprise;
DROP TABLE IF EXISTS food_regulator;

CREATE TABLE IF NOT EXISTS addr_region (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT COMMENT '上级区域',
  name VARCHAR(50) NOT NULL COMMENT '区域名称',
  level TINYINT NOT NULL COMMENT '1省 2市 3区县 4街道',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划';

CREATE TABLE IF NOT EXISTS addr_location (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  region_id BIGINT NOT NULL COMMENT '所属区域',
  detail VARCHAR(255) NOT NULL COMMENT '详细地址',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_location_region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址信息';

CREATE TABLE IF NOT EXISTS food_enterprise (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '对应user-service账号ID',
  enterprise_name VARCHAR(100) NOT NULL COMMENT '企业名称',
  license_no VARCHAR(50) COMMENT '许可证编号',
  region_id BIGINT NOT NULL COMMENT '所属行政区',
  address_id BIGINT NOT NULL COMMENT '地址ID',
  principal VARCHAR(50) COMMENT '负责人',
  principal_phone VARCHAR(20) COMMENT '负责人电话',
  regulator_name VARCHAR(50) COMMENT '包保责任人',
  status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL / KEY',
  approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  approval_comment VARCHAR(255) COMMENT '审核意见',
  approved_by BIGINT COMMENT '审核人ID',
  approved_time DATETIME COMMENT '审核时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_enterprise_user (user_id),
  KEY idx_enterprise_region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食品企业信息表';

CREATE TABLE IF NOT EXISTS food_regulator (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '对应user-service账号ID',
  name VARCHAR(50) NOT NULL COMMENT '姓名',
  phone VARCHAR(20) COMMENT '手机号',
  role_type VARCHAR(30) NOT NULL COMMENT 'REGULATOR_ADMIN / REGULATOR_ENFORCER',
  status TINYINT DEFAULT 1 COMMENT '1-在岗 0-停用',
  work_id_url VARCHAR(255) COMMENT '工作证件附件',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_regulator_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管人员档案表';

CREATE TABLE IF NOT EXISTS food_regulator_region (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  regulator_id BIGINT NOT NULL COMMENT '监管人员ID',
  region_id BIGINT NOT NULL COMMENT '辖区ID',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_regulator_region (regulator_id, region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管人员辖区';

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
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
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

CREATE TABLE IF NOT EXISTS complaint (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  complaint_no VARCHAR(40) COMMENT '投诉单号',
  complainant_name VARCHAR(50) COMMENT '投诉人姓名',
  contact VARCHAR(50) COMMENT '联系方式',
  submitter_user_id BIGINT COMMENT '提交用户ID',
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  complaint_type VARCHAR(50) COMMENT '投诉类型',
  content TEXT NOT NULL COMMENT '投诉内容',
  image_urls TEXT COMMENT '现场图片URL(JSON)',
  status VARCHAR(20) DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED / PENDING / ASSIGNED / PROCESSING / FEEDBACKED',
  source_type VARCHAR(20) COMMENT 'ROUTINE / COMPLAINT / WARNING / MANUAL',
  source_id BIGINT COMMENT '来源ID',
  assigned_to BIGINT COMMENT '指派处理人',
  assigned_by BIGINT COMMENT '指派人',
  assigned_time DATETIME COMMENT '指派时间',
  accepted_by BIGINT COMMENT '受理人',
  accepted_time DATETIME COMMENT '受理时间',
  processed_by BIGINT COMMENT '处理完成人',
  processed_time DATETIME COMMENT '处理完成时间',
  rejected_by BIGINT COMMENT '驳回人',
  rejected_time DATETIME COMMENT '驳回时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众投诉表';

CREATE TABLE IF NOT EXISTS complaint_handle (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  complaint_id BIGINT NOT NULL COMMENT '投诉ID',
  handler_id BIGINT NOT NULL COMMENT '处理人ID',
  handle_result TEXT COMMENT '处理结果',
  handle_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  UNIQUE KEY uk_complaint_handle (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉处理记录表';

CREATE TABLE IF NOT EXISTS enterprise_key_reason (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  reason_type VARCHAR(30) NOT NULL COMMENT 'WARNING_TRIGGERED / COMPLAINT_OVERFLOW / CONSECUTIVE_FAIL / MANUAL_SET',
  reason_detail VARCHAR(255) COMMENT '原因描述',
  source_type VARCHAR(20) COMMENT 'ROUTINE / COMPLAINT / WARNING / MANUAL',
  source_id BIGINT COMMENT '来源ID',
  operator_id BIGINT COMMENT '操作人ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重点监管原因记录';

ALTER TABLE complaint ADD COLUMN image_urls TEXT NULL COMMENT '现场图片URL(JSON)';
ALTER TABLE complaint ADD COLUMN accepted_by BIGINT NULL COMMENT '受理人';
ALTER TABLE complaint ADD COLUMN accepted_time DATETIME NULL COMMENT '受理时间';
ALTER TABLE complaint ADD COLUMN processed_by BIGINT NULL COMMENT '处理完成人';
ALTER TABLE complaint ADD COLUMN processed_time DATETIME NULL COMMENT '处理完成时间';
ALTER TABLE complaint ADD COLUMN submitter_user_id BIGINT NULL COMMENT '提交用户ID';
ALTER TABLE complaint ADD COLUMN rejected_by BIGINT NULL COMMENT '驳回人';
ALTER TABLE complaint ADD COLUMN rejected_time DATETIME NULL COMMENT '驳回时间';
ALTER TABLE complaint_handle ADD UNIQUE KEY uk_complaint_handle (complaint_id);
ALTER TABLE rectification_task MODIFY COLUMN progress VARCHAR(1000) COMMENT '整改进度';
ALTER TABLE rectification_task ADD COLUMN IF NOT EXISTS submit_deadline DATETIME COMMENT '企业提交截止时间';
ALTER TABLE rectification_task ADD COLUMN IF NOT EXISTS review_deadline DATETIME COMMENT '监管复核截止时间';
ALTER TABLE rectification_task ADD UNIQUE KEY uk_rectification_inspection (inspection_id);
