-- warning-service 基础数据初始化
USE food_warning_db;

-- ============================================
-- 1. 预警规则配置
-- ============================================
INSERT INTO warning_rule (id, rule_name, rule_type, threshold, enabled, deleted)
VALUES
(1, '投诉数量预警规则', 'COMPLAINT_COUNT', 3, 1, 0),
(2, '抽检不合格预警规则', 'SAMPLING_FAIL', 1, 1, 0),
(3, '整改逾期预警规则', 'RECTIFICATION_OVERDUE', 1, 1, 0),
(4, '连续检查不合格预警规则', 'CONSECUTIVE_INSPECTION_FAIL', 2, 1, 0),
(5, '食品安全事故预警规则', 'FOOD_SAFETY_INCIDENT', 1, 1, 0);

-- ============================================
-- 2. 预警记录（对应各个业务场景）
-- ============================================
INSERT INTO warning_record (
    id, warning_no, warning_type, biz_type, biz_id, region_id, owner_regulator_id,
    dedup_key, level, status, title, content, source_service,
    first_occur_time, last_occur_time, trigger_count,
    assigned_to, assigned_time, resolved_by, resolved_time, close_reason, deleted
)
VALUES
-- 已解决的预警
(1, 'WARN202403001', 'COMPLAINT_OVERFLOW', 'ENTERPRISE', 4, 7, 3,
 'COMPLAINT_OVERFLOW_ENTERPRISE_4', 'L1', 'RESOLVED',
 '快乐小吃店投诉数量超标',
 '该企业近 30 天累计收到 3 次有效投诉，达到预警阈值，需要重点关注。',
 'complaint-service',
 '2024-03-11 09:00:00', '2024-03-11 09:00:00', 1,
 3, '2024-03-11 09:30:00', 3, '2024-03-15 16:00:00',
 '已对企业进行专项检查并责令整改，企业已完成整改，投诉数量明显下降。', 0),

(2, 'WARN202403002', 'SAMPLING_FAIL', 'SAMPLING', 3, 7, 3,
 'SAMPLING_FAIL_SAMPLING_3', 'L1', 'RESOLVED',
 '快乐小吃店抽检不合格',
 '炸鸡腿抽检不合格，菌落总数超标，存在食品安全风险。',
 'regulation-operation-service',
 '2024-03-06 16:30:00', '2024-03-06 16:30:00', 1,
 3, '2024-03-06 17:00:00', 3, '2024-03-20 15:00:00',
 '已责令企业停止销售该批次产品，查明原因并整改，整改完成后通过复查。', 0),

(3, 'WARN202403003', 'RECTIFICATION_OVERDUE', 'RECTIFICATION', 1, 7, 3,
 'RECTIFICATION_OVERDUE_RECTIFICATION_1', 'L1', 'RESOLVED',
 '快乐小吃店整改任务即将逾期',
 '整改任务提交截止时间为 2024-02-26，当前已接近截止时间，请及时跟进。',
 'regulation-operation-service',
 '2024-02-24 09:00:00', '2024-02-24 09:00:00', 1,
 3, '2024-02-24 09:30:00', 3, '2024-02-25 17:00:00',
 '企业已按时提交整改报告，整改任务已完成。', 0),

-- 处理中的预警
(4, 'WARN202404001', 'RECTIFICATION_OVERDUE', 'RECTIFICATION', 2, 10, 6,
 'RECTIFICATION_OVERDUE_RECTIFICATION_2', 'L2', 'PROCESSING',
 '香满楼餐厅整改任务已逾期',
 '整改任务提交截止时间为 2024-03-21，当前已逾期，需要立即处理。该预警已升级为 L2 级别。',
 'regulation-operation-service',
 '2024-03-22 09:00:00', '2024-04-12 09:00:00', 3,
 6, '2024-03-22 10:00:00', NULL, NULL, NULL, 0),

(5, 'WARN202404002', 'FOOD_SAFETY_INCIDENT', 'COMPLAINT', 6, 10, 6,
 'FOOD_SAFETY_INCIDENT_COMPLAINT_6', 'L1', 'PROCESSING',
 '香满楼餐厅疑似食物中毒事件',
 '接到投诉称在该餐厅用餐后出现腹泻症状，疑似食物中毒，需要立即调查处理。',
 'complaint-service',
 '2024-04-16 09:00:00', '2024-04-16 09:00:00', 1,
 6, '2024-04-16 09:30:00', NULL, NULL, NULL, 0),

-- 待分配的预警
(6, 'WARN202404003', 'COMPLAINT_OVERFLOW', 'ENTERPRISE', 5, 10, 6,
 'COMPLAINT_OVERFLOW_ENTERPRISE_5', 'L1', 'OPEN',
 '香满楼餐厅投诉数量增加',
 '该企业近 30 天累计收到 3 次有效投诉，达到预警阈值，需要关注。',
 'complaint-service',
 '2024-04-18 10:00:00', '2024-04-18 10:00:00', 1,
 NULL, NULL, NULL, NULL, NULL, 0),

-- 已关闭的预警（自动归档）
(7, 'WARN202402001', 'COMPLAINT_OVERFLOW', 'ENTERPRISE', 2, 9, 4,
 'COMPLAINT_OVERFLOW_ENTERPRISE_2', 'L1', 'CLOSED',
 '鲜果园食品店投诉数量超标',
 '该企业近 30 天累计收到 3 次有效投诉，达到预警阈值。',
 'complaint-service',
 '2024-02-15 10:00:00', '2024-02-15 10:00:00', 1,
 4, '2024-02-15 11:00:00', 4, '2024-02-20 16:00:00',
 '经调查，投诉主要为商品质量问题，已要求企业加强质量管理。企业已整改，后续无新增投诉。', 0);

-- ============================================
-- 3. 预警处理日志
-- ============================================
-- 预警 1 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(1, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-03-11 09:00:00', 0),
(1, 'ASSIGN', 1, '张区域', '分配给执法人员王执法处理', '2024-03-11 09:30:00', 0),
(1, 'PROCESS', 3, '王执法', '已安排专项检查，发现企业确实存在管理问题', '2024-03-11 14:00:00', 0),
(1, 'PROCESS', 3, '王执法', '已责令企业限期整改投诉反映问题', '2024-03-12 10:00:00', 0),
(1, 'PROCESS', 3, '王执法', '企业整改完成，通过复查', '2024-03-14 15:00:00', 0),
(1, 'RESOLVE', 3, '王执法', '预警已解决，企业管理明显改善，投诉数量下降', '2024-03-15 16:00:00', 0);

-- 预警 2 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(2, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-03-06 16:30:00', 0),
(2, 'ASSIGN', 1, '张区域', '分配给执法人员王执法处理', '2024-03-06 17:00:00', 0),
(2, 'PROCESS', 3, '王执法', '已责令企业停止销售该批次产品，封存库存', '2024-03-07 10:00:00', 0),
(2, 'PROCESS', 3, '王执法', '企业已查明原因为加工温度控制不当，已整改', '2024-03-15 14:00:00', 0),
(2, 'PROCESS', 3, '王执法', '复查合格，企业已建立完善的温度监控制度', '2024-03-20 11:00:00', 0),
(2, 'RESOLVE', 3, '王执法', '预警已解决，企业食品安全管理水平提升', '2024-03-20 15:00:00', 0);

-- 预警 3 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(3, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-02-24 09:00:00', 0),
(3, 'ASSIGN', 1, '张区域', '分配给执法人员王执法处理', '2024-02-24 09:30:00', 0),
(3, 'PROCESS', 3, '王执法', '已联系企业督促提交整改报告', '2024-02-24 10:00:00', 0),
(3, 'PROCESS', 3, '王执法', '企业已按时提交整改报告', '2024-02-25 16:30:00', 0),
(3, 'RESOLVE', 3, '王执法', '整改任务已完成，预警解决', '2024-02-25 17:00:00', 0);

-- 预警 4 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(4, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-03-22 09:00:00', 0),
(4, 'ASSIGN', 2, '李区域', '分配给执法人员陈执法处理', '2024-03-22 10:00:00', 0),
(4, 'PROCESS', 6, '陈执法', '已联系企业，企业表示正在整改中', '2024-03-23 14:00:00', 0),
(4, 'PROCESS', 6, '陈执法', '企业提交整改报告，但复核发现仍有问题', '2024-03-25 15:00:00', 0),
(4, 'AUTO_LEVEL_UP', NULL, 'SYSTEM', '整改任务持续逾期，预警升级为 L2 级别', '2024-04-01 09:00:00', 0),
(4, 'PROCESS', 6, '陈执法', '已约谈企业负责人，要求限期完成整改', '2024-04-02 10:00:00', 0),
(4, 'PROCESS', 6, '陈执法', '企业再次提交整改报告，正在复核中', '2024-04-11 16:00:00', 0);

-- 预警 5 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(5, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-04-16 09:00:00', 0),
(5, 'ASSIGN', 2, '李区域', '分配给执法人员陈执法处理', '2024-04-16 09:30:00', 0),
(5, 'PROCESS', 6, '陈执法', '已启动应急调查程序，现场检查企业', '2024-04-16 10:00:00', 0),
(5, 'PROCESS', 6, '陈执法', '已采集食品样品送检，等待检测结果', '2024-04-17 14:00:00', 0);

-- 预警 6 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(6, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-04-18 10:00:00', 0);

-- 预警 7 的处理日志
INSERT INTO warning_process_log (warning_id, action_type, operator_id, operator_name, action_comment, create_time, deleted)
VALUES
(7, 'EVENT_UPSERT', NULL, 'SYSTEM', '系统自动创建预警记录', '2024-02-15 10:00:00', 0),
(7, 'ASSIGN', 2, '李区域', '分配给执法人员赵执法处理', '2024-02-15 11:00:00', 0),
(7, 'PROCESS', 4, '赵执法', '已调查投诉情况，主要为商品质量问题', '2024-02-16 14:00:00', 0),
(7, 'PROCESS', 4, '赵执法', '已要求企业加强质量管理，企业已整改', '2024-02-18 16:00:00', 0),
(7, 'RESOLVE', 4, '赵执法', '企业整改完成，后续无新增投诉', '2024-02-20 16:00:00', 0),
(7, 'AUTO_ARCHIVE', NULL, 'SYSTEM', '预警解决后 30 天无新问题，自动归档', '2024-03-21 09:00:00', 0);

-- 重置自增 ID
ALTER TABLE warning_rule AUTO_INCREMENT = 100;
ALTER TABLE warning_record AUTO_INCREMENT = 100;
ALTER TABLE warning_process_log AUTO_INCREMENT = 100;
