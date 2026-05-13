-- regulation-service 基础数据初始化
USE food_regulation_db;

-- ============================================
-- 1. 行政区划数据（江西省南昌市示例）
-- ============================================
-- 省级
INSERT INTO addr_region (id, parent_id, name, level, deleted)
VALUES (1, NULL, '江西省', 1, 0);

-- 市级
INSERT INTO addr_region (id, parent_id, name, level, deleted)
VALUES (2, 1, '南昌市', 2, 0);

-- 区县级
INSERT INTO addr_region (id, parent_id, name, level, deleted)
VALUES
(3, 2, '东湖区', 3, 0),
(4, 2, '西湖区', 3, 0),
(5, 2, '青云谱区', 3, 0),
(6, 2, '青山湖区', 3, 0);

-- 街道级
INSERT INTO addr_region (id, parent_id, name, level, deleted)
VALUES
(7, 3, '八一街道', 4, 0),
(8, 3, '百花洲街道', 4, 0),
(9, 4, '绳金塔街道', 4, 0),
(10, 4, '朝阳洲街道', 4, 0),
(11, 5, '岱山街道', 4, 0),
(12, 5, '三家店街道', 4, 0),
(13, 6, '湖坊街道', 4, 0),
(14, 6, '京东街道', 4, 0);

-- ============================================
-- 2. 地址信息
-- ============================================
INSERT INTO addr_location (id, region_id, detail, deleted)
VALUES
(1, 7, '八一大道128号', 0),
(2, 7, '民德路56号', 0),
(3, 8, '百花洲路88号', 0),
(4, 9, '绳金塔街18号', 0),
(5, 9, '象山南路200号', 0),
(6, 10, '抚河北路300号', 0),
(7, 11, '解放西路150号', 0),
(8, 12, '洪都中大道88号', 0),
(9, 13, '湖坊路66号', 0),
(10, 14, '京东大道188号', 0);

-- ============================================
-- 3. 监管人员档案（对应 user-service 的用户 ID）
-- ============================================
-- 区域管理员（user_id: 2, 3, 17, 18）
INSERT INTO food_regulator (id, user_id, name, phone, role_type, status, deleted)
VALUES
(1, 2, '张区域', '13800000002', 'REGULATOR_ADMIN', 1, 0),
(2, 3, '李区域', '13800000003', 'REGULATOR_ADMIN', 1, 0),
(7, 17, '周区域', '13800000017', 'REGULATOR_ADMIN', 1, 0),
(8, 18, '吴区域', '13800000018', 'REGULATOR_ADMIN', 1, 0);

-- 执法人员（user_id: 4, 5, 6, 7）
INSERT INTO food_regulator (id, user_id, name, phone, role_type, status, deleted)
VALUES
(3, 4, '王执法', '13800000004', 'REGULATOR_ENFORCER', 1, 0),
(4, 5, '赵执法', '13800000005', 'REGULATOR_ENFORCER', 1, 0),
(5, 6, '刘执法', '13800000006', 'REGULATOR_ENFORCER', 1, 0),
(6, 7, '陈执法', '13800000007', 'REGULATOR_ENFORCER', 1, 0);

-- ============================================
-- 4. 监管人员辖区分配
-- ============================================
-- 区域管理员 1 负责东湖区
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES
(1, 3, 0);

-- 区域管理员 2 负责西湖区
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES
(2, 4, 0);

-- 区域管理员 3 负责青云谱区
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES (7, 5, 0);

-- 区域管理员 4 负责青山湖区
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES (8, 6, 0);

-- 执法人员 1 负责八一街道
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES (3, 7, 0);

-- 执法人员 2 负责绳金塔街道
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES (4, 9, 0);

-- 执法人员 3 负责百花洲街道
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES (5, 8, 0);

-- 执法人员 4 负责朝阳洲街道
INSERT INTO food_regulator_region (regulator_id, region_id, deleted)
VALUES (6, 10, 0);

-- ============================================
-- 5. 食品企业信息（对应 user-service 的企业用户 ID: 8-13）
-- ============================================
INSERT INTO food_enterprise (
    id, user_id, enterprise_name, license_no, credit_code, legal_representative,
    region_id, address_id, principal, principal_phone, regulator_id, regulator_name,
    status, approval_status, approved_by, approved_time, deleted
)
VALUES
-- 正常企业（东湖区）
(1, 8, '南昌市美味餐饮有限公司', 'JX360102001', '91360102MA35ABC001', '张三',
  7, 1, '张企一', '13800000008', 3, '王执法',
  'NORMAL', 'APPROVED', 1, '2024-01-15 10:00:00', 0),

-- 正常企业（西湖区）
(2, 9, '南昌市鲜果园食品店', 'JX360103002', '91360103MA35ABC002', '李四',
  9, 4, '李企二', '13800000009', 4, '赵执法',
  'NORMAL', 'APPROVED', 2, '2024-01-20 14:30:00', 0),

-- 正常企业（东湖区）
(3, 10, '南昌市健康食品加工厂', 'JX360104003', '91360104MA35ABC003', '王五',
  8, 3, '王企三', '13800000010', 5, '刘执法',
  'NORMAL', 'APPROVED', 1, '2024-02-01 09:00:00', 0),

-- 重点监管企业（东湖区，因投诉过多）
(4, 11, '南昌市快乐小吃店', 'JX360102004', '91360102MA35ABC004', '赵六',
  7, 2, '赵企四', '13800000011', 3, '王执法',
  'KEY', 'APPROVED', 1, '2024-02-10 11:00:00', 0),

-- 重点监管企业（西湖区，因检查不合格）
(5, 12, '南昌市香满楼餐厅', 'JX360111005', '91360111MA35ABC005', '刘七',
  10, 6, '刘企五', '13800000012', 6, '陈执法',
  'KEY', 'APPROVED', 2, '2024-02-15 15:00:00', 0),

-- 待审核企业（西湖区）
(6, 13, '南昌市新鲜蔬菜配送中心', 'JX360103006', '91360103MA35ABC006', '陈八',
  9, 5, '陈企六', '13800000013', NULL, NULL,
  'NORMAL', 'PENDING', NULL, NULL, 0);

-- ============================================
-- 6. 企业产品档案
-- ============================================
INSERT INTO food_product (enterprise_id, product_name, category, specification, status, remark, deleted)
VALUES
-- 美味餐饮的产品
(1, '红烧肉套餐', '餐饮自制食品', '标准份', 'ACTIVE', '招牌菜品', 0),
(1, '宫保鸡丁', '餐饮自制食品', '标准份', 'ACTIVE', '川菜系列', 0),
(1, '西湖醋鱼', '餐饮自制食品', '标准份', 'ACTIVE', '浙菜系列', 0),

-- 鲜果园的产品
(2, '新鲜苹果', '生鲜农产品', '500g/袋', 'ACTIVE', '进口红富士', 0),
(2, '香蕉', '生鲜农产品', '500g/把', 'ACTIVE', '海南香蕉', 0),
(2, '橙子', '生鲜农产品', '1kg/袋', 'ACTIVE', '赣南脐橙', 0),

-- 健康食品加工厂的产品
(3, '全麦面包', '预包装食品', '500g/袋', 'ACTIVE', '无添加剂', 0),
(3, '杂粮饼干', '预包装食品', '300g/盒', 'ACTIVE', '低糖配方', 0),
(3, '燕麦片', '粮油调味品', '1kg/袋', 'ACTIVE', '即食型', 0),

-- 快乐小吃店的产品
(4, '炸鸡腿', '餐饮自制食品', '单份', 'ACTIVE', '现炸现卖', 0),
(4, '汉堡包', '餐饮自制食品', '单份', 'ACTIVE', '牛肉汉堡', 0),
(4, '薯条', '餐饮自制食品', '中份', 'ACTIVE', '配番茄酱', 0),

-- 香满楼餐厅的产品
(5, '麻辣火锅', '餐饮自制食品', '2-4人份', 'ACTIVE', '川渝风味', 0),
(5, '清汤火锅', '餐饮自制食品', '2-4人份', 'ACTIVE', '养生系列', 0),
(5, '特色凉菜拼盘', '餐饮自制食品', '标准份', 'ACTIVE', '开胃菜', 0),

-- 新鲜蔬菜配送中心的产品
(6, '有机蔬菜礼盒', '生鲜农产品', '3kg/盒', 'ACTIVE', '当日采摘', 0),
(6, '时令蔬菜包', '生鲜农产品', '5kg/包', 'ACTIVE', '家庭装', 0);

-- ============================================
-- 7. 重点监管原因记录
-- ============================================
-- 快乐小吃店因投诉过多被列为重点监管
INSERT INTO enterprise_key_reason (enterprise_id, reason_type, reason_detail, source_type, source_id, operator_id, create_time)
VALUES
(4, 'COMPLAINT_OVERFLOW', '近 30 天有效投诉达到 3 件，已自动纳入重点监管', 'COMPLAINT', 5, 1, '2024-03-11 09:00:00');

-- 香满楼餐厅因现场检查问题被人工列为重点监管
INSERT INTO enterprise_key_reason (enterprise_id, reason_type, reason_detail, source_type, source_id, operator_id, create_time)
VALUES
(5, 'MANUAL_SET', '区域管理员结合现场检查情况，人工纳入重点监管', 'MANUAL', NULL, 2, '2024-03-11 17:00:00');

-- ============================================
-- 8. 公众公告
-- ============================================
INSERT INTO public_bulletin (title, category, content, status, created_by, published_by, published_time, deleted)
VALUES
('2024年第一季度食品安全抽检结果公示', '抽检公示',
 '根据《食品安全法》规定，现将 2024 年第一季度食品安全监督抽检结果予以公示。本次共抽检食品样品 200 批次，合格 195 批次，不合格 5 批次，合格率 97.5%。',
 'PUBLISHED', 1, 1, '2024-04-01 09:00:00', 0),

('关于加强春季食品安全监管的通知', '监管通知',
 '春季是食品安全风险高发期，各食品生产经营单位要严格落实食品安全主体责任，加强食品安全管理，确保人民群众饮食安全。',
 'PUBLISHED', 1, 1, '2024-03-15 10:00:00', 0),

('食品安全知识宣传周活动预告', '活动公告',
 '为提高公众食品安全意识，我局将于 5 月 1 日至 7 日举办食品安全知识宣传周活动，欢迎广大市民参与。',
 'PUBLISHED', 1, 1, '2024-04-20 14:00:00', 0),

('夏季食品安全消费提示', '消费提示',
 '夏季气温高，食品容易变质，请广大消费者注意食品储存条件，购买食品时注意查看生产日期和保质期。',
 'DRAFT', 1, NULL, NULL, 0);

-- 重置自增 ID
ALTER TABLE addr_region AUTO_INCREMENT = 100;
ALTER TABLE addr_location AUTO_INCREMENT = 100;
ALTER TABLE food_regulator AUTO_INCREMENT = 100;
ALTER TABLE food_regulator_region AUTO_INCREMENT = 100;
ALTER TABLE food_enterprise AUTO_INCREMENT = 100;
ALTER TABLE food_product AUTO_INCREMENT = 100;
ALTER TABLE enterprise_key_reason AUTO_INCREMENT = 100;
ALTER TABLE public_bulletin AUTO_INCREMENT = 100;
ALTER TABLE audit_log AUTO_INCREMENT = 100;
