USE food_warning_db;

-- ============================================
-- 1. 预警规则
-- 仅保留当前项目中已真实接入的预警类型
-- ============================================
INSERT INTO warning_rule (id, rule_name, rule_type, threshold, enabled, deleted)
VALUES
(1, '投诉过量预警规则', 'COMPLAINT_OVERFLOW', 3, 1, 0),
(2, '抽检不合格预警规则', 'SAMPLING_FAIL', 1, 1, 0),
(3, '整改逾期预警规则', 'RECTIFICATION_OVERDUE', 1, 1, 0),
(4, '连续检查不合格预警规则', 'CONSECUTIVE_INSPECTION_FAIL', 2, 1, 0);

-- ============================================
-- 2. 预警记录
-- 口径说明：
-- - owner_regulator_id = 当前责任执法人员 food_regulator.id
-- - assigned_to 仅管理员 assign 后写入；创建时仅写 owner_regulator_id
-- - source_service 与真实上报来源保持一致
-- - payload_json 与当前业务链路字段保持一致
-- ============================================
INSERT INTO warning_record (
    id, warning_no, warning_type, biz_type, biz_id, region_id, owner_regulator_id,
    dedup_key, level, status, title, content, source_service,
    first_occur_time, last_occur_time, trigger_count,
    assigned_to, assigned_time, resolved_by, resolved_time, close_reason, payload_json,
    create_time, update_time, deleted
)
VALUES
(
    1, 'WARN202403001', 'COMPLAINT_OVERFLOW', 'ENTERPRISE', 4, 7, 3,
    'COMPLAINT_OVERFLOW_ENTERPRISE_4', 'L1', 'RESOLVED',
    '快乐小吃店投诉过量预警',
    '企业近 30 天有效投诉达到 3 件，已达到投诉过量预警阈值。',
    'complaint-service',
    '2024-03-11 09:00:00', '2024-03-11 09:00:00', 1,
    3, '2024-03-11 09:30:00', 3, '2024-03-15 16:00:00',
    '已完成现场核查并督促企业整改，后续投诉量明显下降。',
    '{"warningType":"COMPLAINT_OVERFLOW","bizType":"ENTERPRISE","bizId":4,"enterpriseId":4,"regionId":7,"ownerRegulatorId":3,"complaintCount":3,"threshold":3}',
    '2024-03-11 09:00:00', '2024-03-15 16:00:00', 0
),
(
    2, 'WARN202403002', 'SAMPLING_FAIL', 'SAMPLING', 3, 7, 3,
    'SAMPLING:3:SAMPLING_FAIL', 'L2', 'RESOLVED',
    '快乐小吃店抽检不合格预警',
    '炸鸡腿抽检不合格，菌落总数超标，存在食品安全风险。',
    'regulation-operation-service',
    '2024-03-06 16:30:00', '2024-03-06 16:30:00', 1,
    3, '2024-03-06 17:00:00', 3, '2024-03-20 15:00:00',
    '问题批次已下架，企业完成整改并通过复查。',
    '{"warningType":"SAMPLING_FAIL","bizType":"SAMPLING","bizId":3,"samplingResultId":3,"enterpriseId":4,"regionId":7,"ownerRegulatorId":3}',
    '2024-03-06 16:30:00', '2024-03-20 15:00:00', 0
),
(
    3, 'WARN202404001', 'RECTIFICATION_OVERDUE', 'RECTIFICATION', 2, 10, 6,
    'RECTIFICATION:2:SLA_OVERDUE_SUBMIT', 'L2', 'PROCESSING',
    '香满楼餐厅整改逾期预警',
    '整改任务已超过提交截止时间，企业仍未完成整改闭环，需持续跟进。',
    'regulation-operation-service',
    '2024-03-22 09:00:00', '2024-04-12 09:00:00', 2,
    6, '2024-03-22 10:00:00', NULL, NULL, NULL,
    '{"warningType":"RECTIFICATION_OVERDUE","bizType":"RECTIFICATION","bizId":2,"rectificationId":2,"inspectionId":4,"enterpriseId":5,"regionId":10,"ownerRegulatorId":6,"actionType":"SLA_OVERDUE_SUBMIT","deadline":"2024-03-21 18:00:00","overdueMinutes":30960}',
    '2024-03-22 09:00:00', '2024-04-12 09:00:00', 0
),
(
    4, 'WARN202404002', 'CONSECUTIVE_INSPECTION_FAIL', 'INSPECTION', 6, 10, 6,
    'INSPECTION:6:CONSECUTIVE_INSPECTION_FAIL', 'L2', 'OPEN',
    '香满楼餐厅连续检查不合格预警',
    '企业最近2次检查均为不合格（2024-03-11、2024-04-10），已自动纳入重点监管。',
    'regulation-operation-service',
    '2024-04-10 11:00:00', '2024-04-10 11:00:00', 1,
    NULL, NULL, NULL, NULL, NULL,
    '{"warningType":"CONSECUTIVE_INSPECTION_FAIL","bizType":"INSPECTION","bizId":6,"inspectionId":6,"enterpriseId":5,"taskId":7,"regionId":10,"ownerRegulatorId":6,"consecutiveFailCount":2,"inspectionDate":"2024-04-10"}',
    '2024-04-10 11:00:00', '2024-04-10 11:00:00', 0
),
(
    5, 'WARN202404003', 'COMPLAINT_OVERFLOW', 'ENTERPRISE', 5, 10, 6,
    'COMPLAINT_OVERFLOW_ENTERPRISE_5', 'L1', 'OPEN',
    '香满楼餐厅投诉过量预警',
    '企业近 30 天有效投诉达到 3 件，需重点关注并及时研判风险。',
    'complaint-service',
    '2024-04-18 10:00:00', '2024-04-18 10:00:00', 1,
    NULL, NULL, NULL, NULL, NULL,
    '{"warningType":"COMPLAINT_OVERFLOW","bizType":"ENTERPRISE","bizId":5,"enterpriseId":5,"regionId":10,"ownerRegulatorId":6,"complaintCount":3,"threshold":3}',
    '2024-04-18 10:00:00', '2024-04-18 10:00:00', 0
),
(
    6, 'WARN202402001', 'COMPLAINT_OVERFLOW', 'ENTERPRISE', 2, 9, 4,
    'COMPLAINT_OVERFLOW_ENTERPRISE_2', 'L1', 'CLOSED',
    '鲜果园食品店投诉过量预警',
    '企业近 30 天有效投诉达到 3 件，达到投诉过量预警阈值。',
    'complaint-service',
    '2024-02-15 10:00:00', '2024-02-15 10:00:00', 1,
    4, '2024-02-15 11:00:00', 4, '2024-02-20 16:00:00',
    '系统自动归档。',
    '{"warningType":"COMPLAINT_OVERFLOW","bizType":"ENTERPRISE","bizId":2,"enterpriseId":2,"regionId":9,"ownerRegulatorId":4,"complaintCount":3,"threshold":3}',
    '2024-02-15 10:00:00', '2024-02-27 16:05:00', 0
);

-- ============================================
-- 3. 预警处理日志
-- 约束：
-- - 每条预警最多一条 PROCESS
-- - 重复触发使用 EVENT_UPSERT 表达合并
-- - operator_id 统一使用 food_regulator.id；系统动作使用 NULL
-- ============================================
INSERT INTO warning_process_log (
    warning_id, action_type, operator_id, operator_name, action_comment, create_time, update_time, deleted
)
VALUES
-- warning 1
(1, 'EVENT_UPSERT', NULL, 'system', '新事件创建预警记录。', '2024-03-11 09:00:00', '2024-03-11 09:00:00', 0),
(1, 'ASSIGN', 1, '张区域', '分配给执法人员王执法处理。', '2024-03-11 09:30:00', '2024-03-11 09:30:00', 0),
(1, 'PROCESS', 3, '王执法', '已开展现场核查，确认企业近期投诉集中，预警进入处理中。', '2024-03-11 14:00:00', '2024-03-11 14:00:00', 0),
(1, 'RESOLVE', 3, '王执法', '企业已完成整改并持续改进服务管理，预警解除。', '2024-03-15 16:00:00', '2024-03-15 16:00:00', 0),

-- warning 2
(2, 'EVENT_UPSERT', NULL, 'system', '新事件创建预警记录。', '2024-03-06 16:30:00', '2024-03-06 16:30:00', 0),
(2, 'ASSIGN', 1, '张区域', '分配给执法人员王执法处理。', '2024-03-06 17:00:00', '2024-03-06 17:00:00', 0),
(2, 'PROCESS', 3, '王执法', '已责令企业停止销售问题批次产品，并启动整改复查。', '2024-03-07 10:00:00', '2024-03-07 10:00:00', 0),
(2, 'RESOLVE', 3, '王执法', '企业完成整改并通过复查，抽检不合格风险已消除。', '2024-03-20 15:00:00', '2024-03-20 15:00:00', 0),

-- warning 3
(3, 'EVENT_UPSERT', NULL, 'system', '新事件创建预警记录。', '2024-03-22 09:00:00', '2024-03-22 09:00:00', 0),
(3, 'ASSIGN', 2, '李区域', '分配给执法人员陈执法跟进整改逾期事项。', '2024-03-22 10:00:00', '2024-03-22 10:00:00', 0),
(3, 'PROCESS', 6, '陈执法', '已联系企业并启动逾期整改跟进，预警进入处理中。', '2024-03-23 14:00:00', '2024-03-23 14:00:00', 0),
(3, 'AUTO_LEVEL_UP', NULL, 'system', '整改逾期达到 L2 升级阈值，系统自动升级。', '2024-04-01 09:00:00', '2024-04-01 09:00:00', 0),
(3, 'EVENT_UPSERT', NULL, 'system', '重复事件合并，触发次数 +1。', '2024-04-12 09:00:00', '2024-04-12 09:00:00', 0),

-- warning 4
(4, 'EVENT_UPSERT', NULL, 'system', '新事件创建预警记录。', '2024-04-10 11:00:00', '2024-04-10 11:00:00', 0),

-- warning 5
(5, 'EVENT_UPSERT', NULL, 'system', '新事件创建预警记录。', '2024-04-18 10:00:00', '2024-04-18 10:00:00', 0),

-- warning 6
(6, 'EVENT_UPSERT', NULL, 'system', '新事件创建预警记录。', '2024-02-15 10:00:00', '2024-02-15 10:00:00', 0),
(6, 'ASSIGN', 2, '李区域', '分配给执法人员赵执法处理。', '2024-02-15 11:00:00', '2024-02-15 11:00:00', 0),
(6, 'PROCESS', 4, '赵执法', '已核查投诉情况并督促企业加强商品质量管理，预警进入处理中。', '2024-02-16 14:00:00', '2024-02-16 14:00:00', 0),
(6, 'RESOLVE', 4, '赵执法', '企业完成整改，后续未再出现同类集中投诉。', '2024-02-20 16:00:00', '2024-02-20 16:00:00', 0),
(6, 'AUTO_ARCHIVE', NULL, 'system', '预警解决满 7 天且无新问题，系统自动归档。', '2024-02-27 16:05:00', '2024-02-27 16:05:00', 0);

ALTER TABLE warning_rule AUTO_INCREMENT = 100;
ALTER TABLE warning_record AUTO_INCREMENT = 100;
ALTER TABLE warning_process_log AUTO_INCREMENT = 100;
