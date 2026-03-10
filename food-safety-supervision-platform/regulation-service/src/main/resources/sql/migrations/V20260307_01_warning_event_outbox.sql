-- regulation-service: warning 事件 outbox 表（增量）
USE food_regulation_db;

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

