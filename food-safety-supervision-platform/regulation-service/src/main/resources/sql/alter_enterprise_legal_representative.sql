-- 已有库增量：法定代表人（与 schema.sql 中 food_enterprise.legal_representative 一致）
ALTER TABLE food_enterprise
  ADD COLUMN legal_representative VARCHAR(50) NULL COMMENT '法定代表人' AFTER credit_code;
