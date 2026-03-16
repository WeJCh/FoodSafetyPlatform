-- complaint-service schema
CREATE DATABASE IF NOT EXISTS food_complaint_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_complaint_db;

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
