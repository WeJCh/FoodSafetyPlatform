-- complaint-service schema
CREATE DATABASE IF NOT EXISTS food_complaint_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_complaint_db;

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS complaint;
DROP TABLE IF EXISTS complaint_handle;

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
  status VARCHAR(20) DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED / PENDING / ASSIGNED / PROCESSING / FEEDBACKED / REJECTED',
  source_type VARCHAR(20) COMMENT 'ROUTINE / COMPLAINT / WARNING / MANUAL',
  source_id BIGINT COMMENT '来源ID',
  assigned_to BIGINT COMMENT '指派处理人',
  assigned_by BIGINT COMMENT '指派人',
  assigned_time DATETIME COMMENT '指派时间',
  deadline_time DATETIME COMMENT '办理时限',
  accepted_by BIGINT COMMENT '受理人',
  accepted_time DATETIME COMMENT '受理时间',
  processed_by BIGINT COMMENT '处理完成人',
  processed_time DATETIME COMMENT '处理完成时间',
  feedback_summary VARCHAR(500) COMMENT '反馈摘要',
  rejected_by BIGINT COMMENT '驳回人',
  rejected_time DATETIME COMMENT '驳回时间',
  reject_reason VARCHAR(500) COMMENT '驳回原因',
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

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(64) NOT NULL COMMENT '服务名称',
  operator_user_id BIGINT COMMENT '操作人用户ID',
  operator_user_type VARCHAR(32) COMMENT '操作人类型',
  operator_name VARCHAR(64) COMMENT '操作人名称',
  target_type VARCHAR(64) COMMENT '目标对象类型',
  target_id BIGINT COMMENT '目标对象ID',
  target_user_id BIGINT COMMENT '目标用户ID',
  target_name VARCHAR(128) COMMENT '目标名称',
  biz_type VARCHAR(64) COMMENT '业务类型',
  action_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  action_name VARCHAR(128) COMMENT '操作名称',
  before_data JSON COMMENT '变更前快照',
  after_data JSON COMMENT '变更后快照',
  success_flag TINYINT DEFAULT 1 COMMENT '是否成功 1-成功 0-失败',
  error_message VARCHAR(500) COMMENT '错误信息',
  remark VARCHAR(255) COMMENT '备注',
  client_ip VARCHAR(64) COMMENT '客户端IP',
  trace_id VARCHAR(64) COMMENT '链路追踪ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_audit_target (target_type, target_id),
  KEY idx_audit_operator (operator_user_id, create_time),
  KEY idx_audit_action (action_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
