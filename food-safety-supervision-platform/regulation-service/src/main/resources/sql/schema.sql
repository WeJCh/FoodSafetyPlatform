-- regulation-service schema
CREATE DATABASE IF NOT EXISTS food_regulation_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE food_regulation_db;

-- 按照依赖顺序删除表（先删除有外键约束的子表，再删除父表）
DROP TABLE IF EXISTS enterprise_key_reason;
DROP TABLE IF EXISTS public_bulletin;
DROP TABLE IF EXISTS food_product;
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

CREATE TABLE IF NOT EXISTS food_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL COMMENT '企业ID',
  product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
  category VARCHAR(50) NOT NULL COMMENT '产品类别',
  specification VARCHAR(100) COMMENT '规格',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE',
  remark VARCHAR(255) COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_product_enterprise (enterprise_id),
  KEY idx_product_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业产品档案表';

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

CREATE TABLE IF NOT EXISTS public_bulletin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL COMMENT '公告标题',
  summary VARCHAR(255) COMMENT '公告摘要',
  content TEXT NOT NULL COMMENT '公告内容',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED / OFFLINE',
  created_by BIGINT COMMENT '创建人用户ID',
  published_by BIGINT COMMENT '发布人用户ID',
  published_time DATETIME COMMENT '发布时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 1-已删 0-未删',
  KEY idx_bulletin_status (status),
  KEY idx_bulletin_published_time (published_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众公告表';
