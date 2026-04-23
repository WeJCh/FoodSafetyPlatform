-- complaint-service 基础数据初始化
USE food_complaint_db;

-- ============================================
-- 1. 公众投诉记录 (对应user-service的公众用户和regulation-service的企业)
-- ============================================
INSERT INTO complaint (id, complaint_no, complainant_name, contact, submitter_user_id, enterprise_id, 
                       complaint_type, content, image_urls, status, source_type, source_id, 
                       assigned_to, assigned_by, assigned_time, deadline_time, 
                       accepted_by, accepted_time, processed_by, processed_time, 
                       feedback_summary, rejected_by, rejected_time, reject_reason, deleted)
VALUES 
-- 已处理完成的投诉
(1, 'COMP202401001', '市民张三', '13800000014', 14, 1, '食品质量问题', 
 '在美味餐饮用餐时发现菜品中有异物，疑似头发，影响食品安全。', 
 '["https://file.example.com/complaint/1/photo1.jpg","https://file.example.com/complaint/1/photo2.jpg"]', 
 'FEEDBACKED', 'COMPLAINT', NULL, 
 4, 2, '2024-01-22 09:00:00', '2024-01-29 18:00:00', 
 4, '2024-01-22 10:00:00', 4, '2024-01-25 16:00:00', 
 '经调查核实，该餐厅确实存在后厨管理不严的问题。已责令企业加强从业人员培训，规范操作流程。企业已整改完毕并向投诉人道歉。', 
 NULL, NULL, NULL, 0),

(2, 'COMP202402001', '市民李四', '13800000015', 15, 2, '食品过期', 
 '在鲜果园购买的苹果，回家后发现部分已经腐烂，怀疑是过期商品。', 
 '["https://file.example.com/complaint/2/photo1.jpg"]', 
 'FEEDBACKED', 'COMPLAINT', NULL, 
 5, 2, '2024-02-08 14:00:00', '2024-02-15 18:00:00', 
 5, '2024-02-08 15:00:00', 5, '2024-02-12 11:00:00', 
 '经现场检查，该店铺进货渠道正规，但存在商品周转不及时的问题。已要求企业加强商品质量检查，及时下架不合格商品。企业已向投诉人退款并道歉。', 
 NULL, NULL, NULL, 0),

(3, 'COMP202402002', '市民王五', '13800000016', 16, 4, '卫生条件差', 
 '在快乐小吃店用餐时发现后厨卫生状况很差，地面有积水和油污，担心食品安全。', 
 '["https://file.example.com/complaint/3/photo1.jpg","https://file.example.com/complaint/3/photo2.jpg","https://file.example.com/complaint/3/photo3.jpg"]', 
 'FEEDBACKED', 'COMPLAINT', NULL, 
 4, 2, '2024-02-10 16:00:00', '2024-02-17 18:00:00', 
 4, '2024-02-11 09:00:00', 4, '2024-02-14 15:00:00', 
 '经现场检查，该店铺确实存在卫生管理不到位的问题。已立即责令企业停业整改，整改完成并通过复查后方可营业。', 
 NULL, NULL, NULL, 0),

(4, 'COMP202403001', '市民张三', '13800000014', 14, 4, '服务态度差', 
 '在快乐小吃店用餐时，服务员态度恶劣，且食品上菜速度很慢。', 
 NULL, 
 'FEEDBACKED', 'COMPLAINT', NULL, 
 4, 2, '2024-03-01 12:00:00', '2024-03-08 18:00:00', 
 4, '2024-03-01 14:00:00', 4, '2024-03-05 10:00:00', 
 '已与企业沟通，要求加强员工培训，提升服务质量。企业已向投诉人道歉。', 
 NULL, NULL, NULL, 0),

(5, 'COMP202403002', '市民李四', '13800000015', 15, 4, '价格欺诈', 
 '在快乐小吃店消费时，菜单价格与实际收费不符，怀疑存在价格欺诈。', 
 '["https://file.example.com/complaint/5/receipt.jpg"]', 
 'FEEDBACKED', 'COMPLAINT', NULL, 
 4, 2, '2024-03-10 18:00:00', '2024-03-17 18:00:00', 
 4, '2024-03-11 09:00:00', 4, '2024-03-14 16:00:00', 
 '经核查，该店铺菜单未及时更新，导致价格不一致。已要求企业立即更新菜单，规范价格标示。企业已向投诉人退还差价。', 
 NULL, NULL, NULL, 0),

-- 处理中的投诉
(6, 'COMP202404001', '市民王五', '13800000016', 16, 5, '食品中毒疑似', 
 '在香满楼餐厅用餐后出现腹泻症状，怀疑食物不新鲜导致食物中毒。', 
 '["https://file.example.com/complaint/6/medical_record.jpg"]', 
 'PROCESSING', 'COMPLAINT', NULL, 
 7, 3, '2024-04-15 20:00:00', '2024-04-22 18:00:00', 
 7, '2024-04-16 09:00:00', NULL, NULL, 
 NULL, NULL, NULL, NULL, 0),

-- 已分配待受理的投诉
(7, 'COMP202404002', '市民张三', '13800000014', 14, 3, '包装破损', 
 '在网上购买健康食品加工厂的全麦面包，收到时包装已破损，担心食品安全。', 
 '["https://file.example.com/complaint/7/package.jpg"]', 
 'ASSIGNED', 'COMPLAINT', NULL, 
 6, 3, '2024-04-18 10:00:00', '2024-04-25 18:00:00', 
 NULL, NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, 0),

-- 待分配的投诉
(8, 'COMP202404003', '市民李四', '13800000015', 15, 6, '虚假宣传', 
 '新鲜蔬菜配送中心宣传是有机蔬菜，但未提供有机认证证书，怀疑虚假宣传。', 
 NULL, 
 'PENDING', 'COMPLAINT', NULL, 
 NULL, NULL, NULL, '2024-04-27 18:00:00', 
 NULL, NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, 0),

-- 已驳回的投诉
(9, 'COMP202403003', '匿名', '13900000000', NULL, 1, '恶意投诉', 
 '该餐厅食品质量差，服务态度恶劣，要求赔偿10000元。', 
 NULL, 
 'REJECTED', 'COMPLAINT', NULL, 
 4, 2, '2024-03-20 15:00:00', '2024-03-27 18:00:00', 
 4, '2024-03-20 16:00:00', NULL, NULL, 
 NULL, 2, '2024-03-21 10:00:00', '经核查，投诉内容与事实不符，且投诉人无法提供有效证据，联系方式无法核实，疑似恶意投诉。', 0);

-- ============================================
-- 2. 投诉处理记录
-- ============================================
INSERT INTO complaint_handle (complaint_id, handler_id, handle_result, handle_time, deleted)
VALUES 
(1, 4, '1. 现场检查发现后厨管理存在漏洞；2. 责令企业加强从业人员培训；3. 企业已整改并向投诉人道歉；4. 投诉人表示满意。', 
 '2024-01-25 16:00:00', 0),

(2, 5, '1. 检查进货记录和储存条件；2. 发现商品周转不及时；3. 要求企业加强质量管理；4. 企业已退款并道歉。', 
 '2024-02-12 11:00:00', 0),

(3, 4, '1. 现场检查确认卫生状况不达标；2. 责令停业整改；3. 整改完成后通过复查；4. 已恢复营业。', 
 '2024-02-14 15:00:00', 0),

(4, 4, '1. 与企业和投诉人沟通；2. 要求企业加强员工培训；3. 企业已道歉；4. 投诉人接受处理结果。', 
 '2024-03-05 10:00:00', 0),

(5, 4, '1. 核查菜单和收费记录；2. 发现菜单未及时更新；3. 要求规范价格标示；4. 企业已退还差价。', 
 '2024-03-14 16:00:00', 0);

-- 重置自增ID
ALTER TABLE complaint AUTO_INCREMENT = 100;
ALTER TABLE complaint_handle AUTO_INCREMENT = 100;
