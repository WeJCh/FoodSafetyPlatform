USE food_complaint_db;

-- complaint-service 初始化数据
-- submitter_user_id 对应公众账号 user.id
-- accepted_by / assigned_by / assigned_to / processed_by / rejected_by 对应监管账号 food_regulator.id

INSERT INTO complaint (
    id, complaint_no, complainant_name, contact, submitter_user_id, anonymous_flag, enterprise_id,
    complaint_type, content, image_urls, status, source_type, source_id,
    assigned_to, assigned_by, assigned_time, deadline_time,
    accepted_by, accepted_time, processed_by, processed_time,
    feedback_summary, rejected_by, rejected_time, reject_reason, deleted
) VALUES
(1, 'COMP202401001', '张三市民', '13800000014', 14, 0, 1,
 'FOOD_SAFETY', '在美味餐厅就餐时发现菜品中有异物，怀疑存在食品安全问题。',
 '["https://file.example.com/complaint/1/photo1.jpg","https://file.example.com/complaint/1/photo2.jpg"]',
 'FEEDBACKED', 'MANUAL', NULL,
 3, 1, '2024-01-22 09:00:00', '2024-01-29 18:00:00',
 1, '2024-01-22 10:00:00', 3, '2024-01-25 16:00:00',
 '经现场核查，后厨卫生与留样记录存在缺失，已责令企业完成整改并向投诉人致歉。',
 NULL, NULL, NULL, 0),

(2, 'COMP202402001', '李四市民', '13800000015', 15, 0, 2,
 'FOOD_SAFETY', '在鲜果园购买的水果回家后发现已有腐烂变质，怀疑商品保鲜管理不到位。',
 '["https://file.example.com/complaint/2/photo1.jpg"]',
 'FEEDBACKED', 'MANUAL', NULL,
 4, 2, '2024-02-08 14:00:00', '2024-02-15 18:00:00',
 2, '2024-02-08 15:00:00', 4, '2024-02-12 11:00:00',
 '经检查，企业存在商品周转不及时问题，已要求下架问题商品并完善冷藏管理。',
 NULL, NULL, NULL, 0),

(3, 'COMP202402002', '王五市民', '13800000016', 16, 0, 4,
 'HYGIENE', '在快乐小吃店用餐时发现后厨地面积水、油污明显，担心卫生状况影响食品安全。',
 '["https://file.example.com/complaint/3/photo1.jpg","https://file.example.com/complaint/3/photo2.jpg"]',
 'FEEDBACKED', 'MANUAL', NULL,
 3, 1, '2024-02-10 16:00:00', '2024-02-17 18:00:00',
 1, '2024-02-11 09:00:00', 3, '2024-02-14 15:00:00',
 '经现场检查，门店后厨卫生管理不到位，已责令停业整改并复查通过后恢复经营。',
 NULL, NULL, NULL, 0),

(4, 'COMP202403001', '张三市民', '13800000014', 14, 1, 4,
 'SERVICE', '在快乐小吃店消费时，服务态度较差且上菜过慢，希望监管部门协调处理。',
 NULL,
 'REJECTED', 'MANUAL', NULL,
 3, 1, '2024-03-01 12:00:00', '2024-03-08 18:00:00',
 1, '2024-03-01 14:00:00', NULL, NULL,
 NULL, 1, '2024-03-02 10:00:00', '投诉事实依据不足，且未能提供可核验的有效证据，暂不予立案。', 0),

(5, 'COMP202403002', '李四市民', '13800000015', 15, 0, 4,
 'PRICE', '在快乐小吃店消费时，菜单价格与实际收费不一致，怀疑存在价格欺诈。',
 '["https://file.example.com/complaint/5/receipt.jpg"]',
 'FEEDBACKED', 'MANUAL', NULL,
 3, 1, '2024-03-10 18:00:00', '2024-03-17 18:00:00',
 1, '2024-03-11 09:00:00', 3, '2024-03-14 16:00:00',
 '经核查，门店菜单更新不及时导致价签不一致，已责令立即整改并退还差价。',
 NULL, NULL, NULL, 0),

(6, 'COMP202404001', '王五市民', '13800000016', 16, 0, 5,
 'FOOD_SAFETY', '在香满楼餐厅就餐后出现腹泻症状，怀疑食品不新鲜导致身体不适。',
 '["https://file.example.com/complaint/6/medical_record.jpg"]',
 'PROCESSING', 'MANUAL', NULL,
 6, 2, '2024-04-15 20:00:00', '2024-04-22 18:00:00',
 2, '2024-04-16 09:00:00', NULL, NULL,
 NULL, NULL, NULL, NULL, 0),

(7, 'COMP202404002', '张三市民', '13800000014', 14, 0, 3,
 'PACKAGING', '网购全麦面包收到时外包装已经破损，担心运输与包装环节存在问题。',
 '["https://file.example.com/complaint/7/package.jpg"]',
 'ASSIGNED', 'MANUAL', NULL,
 5, 1, '2024-04-18 10:00:00', '2024-04-25 18:00:00',
 1, '2024-04-18 09:30:00', NULL, NULL,
 NULL, NULL, NULL, NULL, 0),

(8, 'COMP202404003', '李四市民', '13800000015', 15, 0, 6,
 'FALSE_AD', '新鲜蔬菜配送中心宣称有机蔬菜，但未能提供相应认证证明，疑似虚假宣传。',
 NULL,
 'PENDING', 'MANUAL', NULL,
 NULL, NULL, NULL, '2024-04-27 18:00:00',
 2, '2024-04-20 10:00:00', NULL, NULL,
 NULL, NULL, NULL, NULL, 0),

(9, 'COMP202404004', '王五市民', '13800000016', 16, 1, 5,
 'FOOD_SAFETY', '在香满楼餐厅点的海鲜口感明显不新鲜，希望监管部门尽快核查。',
 NULL,
 'SUBMITTED', 'MANUAL', NULL,
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, 0);

INSERT INTO complaint_handle (id, complaint_id, handler_id, handle_result, handle_time, create_time, update_time, deleted)
VALUES
(1, 1, 3, '现场检查发现后厨留样和清洁记录不完整，已责令整改并复查通过。', '2024-01-25 16:00:00', '2024-01-25 16:00:00', '2024-01-25 16:00:00', 0),
(2, 2, 4, '已核验进货与仓储记录，问题商品完成下架，企业承诺加强质量检查。', '2024-02-12 11:00:00', '2024-02-12 11:00:00', '2024-02-12 11:00:00', 0),
(3, 3, 3, '门店后厨卫生问题已完成整改，复查结果合格。', '2024-02-14 15:00:00', '2024-02-14 15:00:00', '2024-02-14 15:00:00', 0),
(4, 4, 1, '投诉事实依据不足，暂不予立案。', '2024-03-02 10:00:00', '2024-03-02 10:00:00', '2024-03-02 10:00:00', 0),
(5, 5, 3, '已核对价签与收费记录，企业完成退差并更新菜单。', '2024-03-14 16:00:00', '2024-03-14 16:00:00', '2024-03-14 16:00:00', 0);

ALTER TABLE complaint AUTO_INCREMENT = 100;
ALTER TABLE complaint_handle AUTO_INCREMENT = 100;
ALTER TABLE audit_log AUTO_INCREMENT = 100;
