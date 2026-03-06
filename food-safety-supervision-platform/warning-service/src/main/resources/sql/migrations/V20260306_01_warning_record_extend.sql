-- warning_record 增量字段（阶段1）
USE food_warning_db;

ALTER TABLE warning_record
    ADD COLUMN IF NOT EXISTS assigned_to BIGINT COMMENT '指派处理人ID' AFTER trigger_count,
    ADD COLUMN IF NOT EXISTS assigned_time DATETIME COMMENT '指派时间' AFTER assigned_to,
    ADD COLUMN IF NOT EXISTS resolved_by BIGINT COMMENT '解决人ID' AFTER assigned_time,
    ADD COLUMN IF NOT EXISTS resolved_time DATETIME COMMENT '解决时间' AFTER resolved_by,
    ADD COLUMN IF NOT EXISTS close_reason VARCHAR(500) COMMENT '关闭原因' AFTER resolved_time;

-- 兼容老库：仅当索引不存在时新增 idx_warning_assigned_status
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'warning_record'
      AND index_name = 'idx_warning_assigned_status'
);

SET @ddl := IF(
    @idx_exists = 0,
    'ALTER TABLE warning_record ADD KEY idx_warning_assigned_status (assigned_to, status)',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
