-- regulation-operation-service 基础数据初始化
USE food_regulation_operation_db;

-- ============================================
-- 1. 检查任务（对应 regulation-service 的企业和监管人员）
-- 监管人员相关字段统一使用 regulation-service 的 food_regulator.id
-- ============================================
INSERT INTO inspection_task (
    id, task_no, enterprise_id, region_id, task_title, task_desc, priority, status,
    created_by, assigned_to, assigned_by, assigned_time, started_time, completed_time,
    deadline, deleted
)
VALUES
-- 已完成的检查任务
(1, 'TASK202401001', 1, 7, '美味餐饮日常检查', '对餐饮服务单位进行日常监督检查', 'MEDIUM', 'COMPLETED',
 1, 3, 1, '2024-01-20 09:00:00', '2024-01-21 10:00:00', '2024-01-21 16:00:00',
 '2024-01-25 18:00:00', 0),

(2, 'TASK202402001', 2, 9, '鲜果园食品店检查', '对食品销售单位进行监督检查', 'MEDIUM', 'COMPLETED',
 2, 4, 2, '2024-02-05 09:00:00', '2024-02-06 10:00:00', '2024-02-06 15:00:00',
 '2024-02-10 18:00:00', 0),

(3, 'TASK202402002', 4, 7, '快乐小吃店重点检查', '对重点监管企业开展专项检查', 'HIGH', 'COMPLETED',
 1, 3, 1, '2024-02-15 09:00:00', '2024-02-16 10:00:00', '2024-02-16 17:00:00',
 '2024-02-20 18:00:00', 0),

(4, 'TASK202403001', 5, 10, '香满楼餐厅复查', '对企业整改情况进行复查', 'HIGH', 'COMPLETED',
 2, 6, 2, '2024-03-10 09:00:00', '2024-03-11 10:00:00', '2024-03-11 16:30:00',
 '2024-03-15 18:00:00', 0),

-- 进行中的检查任务
(5, 'TASK202404001', 3, 8, '健康食品加工厂检查', '对食品生产单位进行监督检查', 'MEDIUM', 'IN_PROGRESS',
 1, 5, 1, '2024-04-15 09:00:00', '2024-04-16 10:00:00', NULL,
 '2024-04-20 18:00:00', 0),

-- 待分派的检查任务
(6, 'TASK202404002', 6, 9, '新鲜蔬菜配送中心初检', '新备案企业首次监督检查', 'MEDIUM', 'CREATED',
 2, NULL, NULL, NULL, NULL, NULL,
 '2024-04-30 18:00:00', 0);

-- ============================================
-- 2. 抽检任务（对应企业和产品）
-- ============================================
INSERT INTO sampling_task (
    id, task_no, enterprise_id, product_id, region_id, task_title, task_desc, priority, status,
    created_by, assigned_to, assigned_by, assigned_time, completed_time, deadline, deleted
)
VALUES
-- 已完成的抽检任务
(1, 'SAMPLE202401001', 3, 7, 8, '全麦面包抽检', '对烘焙食品进行质量抽检', 'MEDIUM', 'COMPLETED',
 1, 5, 1, '2024-01-25 09:00:00', '2024-01-26 15:00:00', '2024-01-30 18:00:00', 0),

(2, 'SAMPLE202402001', 2, 4, 9, '新鲜苹果抽检', '对水果进行农药残留检测', 'MEDIUM', 'COMPLETED',
 2, 4, 2, '2024-02-10 09:00:00', '2024-02-11 14:00:00', '2024-02-15 18:00:00', 0),

(3, 'SAMPLE202403001', 4, 10, 7, '炸鸡腿抽检', '对快餐食品进行微生物检测', 'HIGH', 'COMPLETED',
 1, 3, 1, '2024-03-05 09:00:00', '2024-03-06 16:00:00', '2024-03-10 18:00:00', 0),

-- 待分派的抽检任务
(4, 'SAMPLE202404001', 5, 13, 10, '麻辣火锅抽检', '对火锅类食品进行添加剂检测', 'HIGH', 'CREATED',
 2, NULL, NULL, NULL, NULL, '2024-04-25 18:00:00', 0);

-- ============================================
-- 3. 抽检结果
-- ============================================
INSERT INTO sampling_result (
    task_id, enterprise_id, product_id, sampled_by, sampled_time, result, conclusion,
    disposal_suggestion, public_status, published_time, deleted
)
VALUES
-- 合格的抽检结果
(1, 3, 7, 5, '2024-01-26 10:00:00', 'PASS',
 '经检验，该批次全麦面包各项指标符合相关食品安全标准要求，检验结论为合格。',
 '继续保持良好的生产规范。',
 'PUBLISHED', '2024-02-01 09:00:00', 0),

(2, 2, 4, 4, '2024-02-11 10:00:00', 'PASS',
 '经检验，该批次新鲜苹果农药残留检测结果符合相关食品安全标准要求，检验结论为合格。',
 '继续加强进货查验与冷链管理。',
 'PUBLISHED', '2024-02-15 09:00:00', 0),

-- 不合格的抽检结果
(3, 4, 10, 3, '2024-03-06 10:00:00', 'FAIL',
 '经检验，该批次炸鸡腿菌落总数超标，检验结论为不合格。',
 '责令企业立即停止销售该批次产品，查明原因并完成整改。',
 'PUBLISHED', '2024-03-10 09:00:00', 0);

-- ============================================
-- 4. 监督检查记录
-- ============================================
INSERT INTO inspection_record (id, task_id, enterprise_id, inspector_id, inspection_date, result, problem_desc, deleted)
VALUES
-- 检查合格的记录
(1, 1, 1, 3, '2024-01-21', 'PASS', NULL, 0),

(2, 2, 2, 4, '2024-02-06', 'PASS', NULL, 0),

-- 检查不合格的记录
(3, 3, 4, 3, '2024-02-16', 'FAIL',
 '发现以下问题：1. 后厨卫生状况较差，地面有油污；2. 部分食材未按要求分类存放；3. 从业人员未规范佩戴口罩和手套；4. 消毒记录不完整。', 0),

(4, 4, 5, 6, '2024-03-11', 'FAIL',
 '发现以下问题：1. 火锅底料存放温度不符合要求；2. 食品留样记录缺失；3. 餐具消毒设施运行异常。', 0),

-- 进行中的检查（暂无结果）
(5, 5, 3, 5, '2024-04-16', NULL, NULL, 0);

-- ============================================
-- 5. 检查明细
-- ============================================
INSERT INTO inspection_item (inspection_id, item_name, item_result, problem_desc, deleted)
VALUES
(1, '食品经营许可证', 'PASS', NULL, 0),
(1, '从业人员健康证', 'PASS', NULL, 0),
(1, '食品安全管理制度', 'PASS', NULL, 0),
(1, '食品加工场所卫生', 'PASS', NULL, 0),
(1, '食品储存条件', 'PASS', NULL, 0),
(1, '餐饮具清洗消毒', 'PASS', NULL, 0);

INSERT INTO inspection_item (inspection_id, item_name, item_result, problem_desc, deleted)
VALUES
(2, '食品经营许可证', 'PASS', NULL, 0),
(2, '进货查验记录', 'PASS', NULL, 0),
(2, '食品储存条件', 'PASS', NULL, 0),
(2, '食品标签标识', 'PASS', NULL, 0),
(2, '过期食品处理', 'PASS', NULL, 0);

INSERT INTO inspection_item (inspection_id, item_name, item_result, problem_desc, deleted)
VALUES
(3, '食品经营许可证', 'PASS', NULL, 0),
(3, '从业人员健康证', 'PASS', NULL, 0),
(3, '食品加工场所卫生', 'FAIL', '后厨地面有油污，墙面存在污渍', 0),
(3, '食品储存条件', 'FAIL', '食材未按要求分类存放，生熟混放', 0),
(3, '从业人员操作规范', 'FAIL', '从业人员未规范佩戴口罩和手套', 0),
(3, '餐饮具清洗消毒', 'FAIL', '消毒记录不完整，部分日期缺失', 0);

INSERT INTO inspection_item (inspection_id, item_name, item_result, problem_desc, deleted)
VALUES
(4, '食品经营许可证', 'PASS', NULL, 0),
(4, '从业人员健康证', 'PASS', NULL, 0),
(4, '食品储存条件', 'FAIL', '火锅底料存放温度超标，未按冷藏要求存放', 0),
(4, '食品留样管理', 'FAIL', '近一周食品留样记录缺失', 0),
(4, '餐饮具清洗消毒', 'FAIL', '消毒柜故障，未能正常运行', 0);

-- ============================================
-- 6. 整改任务
-- ============================================
INSERT INTO rectification_task (
    id, inspection_id, enterprise_id, rectification_desc, progress, status,
    submit_deadline, review_deadline, finish_time, confirmed_by, confirmed_time, deleted
)
VALUES
(1, 3, 4,
 '针对检查发现的问题，要求企业在规定时间内完成以下整改：\n1. 彻底清洁后厨地面和墙面，保持环境卫生；\n2. 规范食材存放，做到生熟分开、分类存放；\n3. 要求从业人员严格按规范操作，佩戴口罩和手套；\n4. 完善消毒记录，做到每日如实填写。',
 '企业已完成整改，提交了整改报告和现场照片。',
 'SUBMITTED',
 '2024-02-26 18:00:00', '2024-03-01 18:00:00', NULL, NULL, NULL, 0),

(2, 4, 5,
 '针对检查发现的问题，要求企业在规定时间内完成以下整改：\n1. 调整火锅底料存放温度，确保符合冷藏要求；\n2. 建立完善的食品留样制度，每日留样并记录；\n3. 维修或更换餐具消毒设施，确保正常运行。',
 '企业提交了整改报告，但复核发现消毒设施仍未完全修复，要求继续整改。',
 'REWORK',
 '2024-03-21 18:00:00', '2024-03-26 18:00:00', NULL, NULL, NULL, 0);

-- ============================================
-- 7. 整改动作日志
-- ============================================
INSERT INTO rectification_action_log (rectification_id, action_type, operator_id, action_comment, attachment_urls, create_time, deleted)
VALUES
(1, 'SYSTEM_CREATE', NULL, '系统自动创建整改任务', NULL, '2024-02-16 17:00:00', 0),
(1, 'ENTERPRISE_SUBMIT', 11, '企业提交整改报告：已完成所有整改项目，后厨已彻底清洁，食材存放已规范，从业人员已培训，消毒记录已完善。',
 '["https://file.example.com/rectification/1/kitchen_after.jpg","https://file.example.com/rectification/1/storage_after.jpg","https://file.example.com/rectification/1/disinfection_record.pdf"]',
 '2024-02-25 16:00:00', 0);

INSERT INTO rectification_action_log (rectification_id, action_type, operator_id, action_comment, attachment_urls, create_time, deleted)
VALUES
(2, 'SYSTEM_CREATE', NULL, '系统自动创建整改任务', NULL, '2024-03-11 16:30:00', 0),
(2, 'ENTERPRISE_SUBMIT', 12, '企业提交整改报告：已调整存放温度，建立留样制度，消毒设施已联系维修。',
 '["https://file.example.com/rectification/2/temperature_record.jpg","https://file.example.com/rectification/2/sample_record.pdf"]',
 '2024-03-20 15:00:00', 0),
(2, 'REVIEW_REWORK', 6, '复核意见：存放温度和留样制度已整改到位，但消毒设施仍未完全修复，要求尽快完成维修并提交验收材料。',
 NULL, '2024-03-25 14:00:00', 0),
(2, 'ENTERPRISE_SUBMIT', 12, '企业再次提交：消毒设施已维修完成，已通过第三方检验验收。',
 '["https://file.example.com/rectification/2/disinfection_repair_report.pdf","https://file.example.com/rectification/2/inspection_certificate.pdf"]',
 '2024-04-10 16:00:00', 0);

-- ============================================
-- 8. 预警事件 Outbox（示例数据）
-- ============================================
INSERT INTO warning_event_outbox (event_key, event_type, payload_json, status, retry_count, next_retry_time, deleted)
VALUES
('SAMPLING:3:SAMPLING_FAIL', 'SAMPLING_FAIL',
 '{"warningType":"SAMPLING_FAIL","bizType":"SAMPLING","bizId":3,"regionId":7,"ownerRegulatorId":3,"title":"抽检不合格预警","content":"快乐小吃店炸鸡腿抽检不合格，菌落总数超标","sourceService":"regulation-operation-service","dedupKey":"SAMPLING:3:SAMPLING_FAIL","samplingResultId":3,"enterpriseId":4}',
 'SENT', 0, '2024-03-06 16:30:00', 0),

('RECTIFICATION:2:SLA_OVERDUE_SUBMIT', 'RECTIFICATION_OVERDUE',
 '{"warningType":"RECTIFICATION_OVERDUE","bizType":"RECTIFICATION","bizId":2,"regionId":10,"ownerRegulatorId":6,"title":"整改逾期预警","content":"香满楼餐厅整改任务已超过提交截止时间，请立即处理","sourceService":"regulation-operation-service","dedupKey":"RECTIFICATION:2:SLA_OVERDUE_SUBMIT","rectificationId":2,"inspectionId":4,"enterpriseId":5,"actionType":"SLA_OVERDUE_SUBMIT","deadline":"2024-03-21 18:00:00"}',
 'SENT', 0, '2024-04-12 09:00:00', 0);

-- 重置自增 ID
ALTER TABLE inspection_task AUTO_INCREMENT = 100;
ALTER TABLE sampling_task AUTO_INCREMENT = 100;
ALTER TABLE sampling_result AUTO_INCREMENT = 100;
ALTER TABLE inspection_record AUTO_INCREMENT = 100;
ALTER TABLE inspection_item AUTO_INCREMENT = 100;
ALTER TABLE rectification_task AUTO_INCREMENT = 100;
ALTER TABLE rectification_action_log AUTO_INCREMENT = 100;
ALTER TABLE warning_event_outbox AUTO_INCREMENT = 100;
