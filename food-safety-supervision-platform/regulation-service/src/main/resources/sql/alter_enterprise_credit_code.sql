-- 已有库增量：统一社会信用代码（与 schema.sql 中 food_enterprise.credit_code 一致）
ALTER TABLE food_enterprise
  ADD COLUMN credit_code VARCHAR(18) NULL COMMENT '统一社会信用代码' AFTER license_no;
