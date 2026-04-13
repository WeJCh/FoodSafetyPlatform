CREATE TABLE IF NOT EXISTS enterprise_profile_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  attachment_type VARCHAR(50) NOT NULL COMMENT '附件类型',
  attachment_name VARCHAR(255) COMMENT '附件名称',
  attachment_url VARCHAR(500) NOT NULL COMMENT '附件访问地址',
  uploaded_by BIGINT COMMENT '上传人用户ID',
  uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_epa_enterprise (enterprise_id),
  KEY idx_epa_enterprise_type (enterprise_id, attachment_type),
  KEY idx_epa_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业备案附件表';
