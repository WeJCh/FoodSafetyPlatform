USE food_complaint_db;

-- complaint-service 监管人员关联数据校验脚本
-- 目的：
-- 1) 查出 complaint / complaint_handle 中不存在的监管员ID
-- 2) 查出疑似误写成 food_regulator.user_id 的数据
-- 3) 查出角色不匹配的数据（例如 assigned_to 不是执法人员）
-- 4) 查出 complaint_handle 与 complaint.processed_by 不一致的数据
--
-- 说明：
-- 当前业务约定：
-- - complaint.assigned_to / processed_by / complaint_handle.handler_id 应为 REGULATOR_ENFORCER 的 food_regulator.id
-- - complaint.assigned_by / accepted_by / rejected_by 应为 REGULATOR_ADMIN 的 food_regulator.id
--
-- 依赖：
-- - 同一 MySQL 实例下存在 food_regulation_db.food_regulator


-- 1. 投诉表中引用了不存在的监管员 ID
SELECT
    'complaint_invalid_regulator_ref' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'assigned_to' AS field_name,
    c.assigned_to AS field_value
FROM complaint c
LEFT JOIN food_regulation_db.food_regulator r ON r.id = c.assigned_to AND r.deleted = 0
WHERE c.deleted = 0
  AND c.assigned_to IS NOT NULL
  AND r.id IS NULL

UNION ALL

SELECT
    'complaint_invalid_regulator_ref' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'assigned_by' AS field_name,
    c.assigned_by AS field_value
FROM complaint c
LEFT JOIN food_regulation_db.food_regulator r ON r.id = c.assigned_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.assigned_by IS NOT NULL
  AND r.id IS NULL

UNION ALL

SELECT
    'complaint_invalid_regulator_ref' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'accepted_by' AS field_name,
    c.accepted_by AS field_value
FROM complaint c
LEFT JOIN food_regulation_db.food_regulator r ON r.id = c.accepted_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.accepted_by IS NOT NULL
  AND r.id IS NULL

UNION ALL

SELECT
    'complaint_invalid_regulator_ref' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'processed_by' AS field_name,
    c.processed_by AS field_value
FROM complaint c
LEFT JOIN food_regulation_db.food_regulator r ON r.id = c.processed_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.processed_by IS NOT NULL
  AND r.id IS NULL

UNION ALL

SELECT
    'complaint_invalid_regulator_ref' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'rejected_by' AS field_name,
    c.rejected_by AS field_value
FROM complaint c
LEFT JOIN food_regulation_db.food_regulator r ON r.id = c.rejected_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.rejected_by IS NOT NULL
  AND r.id IS NULL
ORDER BY complaint_id, field_name;


-- 2. 处理记录表中引用了不存在的监管员 ID
SELECT
    'handle_invalid_regulator_ref' AS check_type,
    h.id AS handle_id,
    h.complaint_id,
    c.complaint_no,
    h.handler_id
FROM complaint_handle h
LEFT JOIN complaint c ON c.id = h.complaint_id
LEFT JOIN food_regulation_db.food_regulator r ON r.id = h.handler_id AND r.deleted = 0
WHERE h.deleted = 0
  AND r.id IS NULL
ORDER BY h.complaint_id;


-- 3. 疑似把 food_regulator.user_id 写进 complaint 的字段
SELECT
    'complaint_suspect_user_id_used' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'assigned_to' AS field_name,
    c.assigned_to AS field_value,
    r_by_user.id AS matched_regulator_id,
    r_by_user.name AS matched_regulator_name,
    r_by_user.role_type AS matched_role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r_by_user
    ON r_by_user.user_id = c.assigned_to AND r_by_user.deleted = 0
LEFT JOIN food_regulation_db.food_regulator r_by_id
    ON r_by_id.id = c.assigned_to AND r_by_id.deleted = 0
WHERE c.deleted = 0
  AND c.assigned_to IS NOT NULL
  AND r_by_id.id IS NULL

UNION ALL

SELECT
    'complaint_suspect_user_id_used' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'assigned_by' AS field_name,
    c.assigned_by AS field_value,
    r_by_user.id AS matched_regulator_id,
    r_by_user.name AS matched_regulator_name,
    r_by_user.role_type AS matched_role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r_by_user
    ON r_by_user.user_id = c.assigned_by AND r_by_user.deleted = 0
LEFT JOIN food_regulation_db.food_regulator r_by_id
    ON r_by_id.id = c.assigned_by AND r_by_id.deleted = 0
WHERE c.deleted = 0
  AND c.assigned_by IS NOT NULL
  AND r_by_id.id IS NULL

UNION ALL

SELECT
    'complaint_suspect_user_id_used' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'accepted_by' AS field_name,
    c.accepted_by AS field_value,
    r_by_user.id AS matched_regulator_id,
    r_by_user.name AS matched_regulator_name,
    r_by_user.role_type AS matched_role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r_by_user
    ON r_by_user.user_id = c.accepted_by AND r_by_user.deleted = 0
LEFT JOIN food_regulation_db.food_regulator r_by_id
    ON r_by_id.id = c.accepted_by AND r_by_id.deleted = 0
WHERE c.deleted = 0
  AND c.accepted_by IS NOT NULL
  AND r_by_id.id IS NULL

UNION ALL

SELECT
    'complaint_suspect_user_id_used' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'processed_by' AS field_name,
    c.processed_by AS field_value,
    r_by_user.id AS matched_regulator_id,
    r_by_user.name AS matched_regulator_name,
    r_by_user.role_type AS matched_role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r_by_user
    ON r_by_user.user_id = c.processed_by AND r_by_user.deleted = 0
LEFT JOIN food_regulation_db.food_regulator r_by_id
    ON r_by_id.id = c.processed_by AND r_by_id.deleted = 0
WHERE c.deleted = 0
  AND c.processed_by IS NOT NULL
  AND r_by_id.id IS NULL

UNION ALL

SELECT
    'complaint_suspect_user_id_used' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'rejected_by' AS field_name,
    c.rejected_by AS field_value,
    r_by_user.id AS matched_regulator_id,
    r_by_user.name AS matched_regulator_name,
    r_by_user.role_type AS matched_role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r_by_user
    ON r_by_user.user_id = c.rejected_by AND r_by_user.deleted = 0
LEFT JOIN food_regulation_db.food_regulator r_by_id
    ON r_by_id.id = c.rejected_by AND r_by_id.deleted = 0
WHERE c.deleted = 0
  AND c.rejected_by IS NOT NULL
  AND r_by_id.id IS NULL
ORDER BY complaint_id, field_name;


-- 4. 疑似把 food_regulator.user_id 写进 complaint_handle.handler_id
SELECT
    'handle_suspect_user_id_used' AS check_type,
    h.id AS handle_id,
    h.complaint_id,
    c.complaint_no,
    h.handler_id AS field_value,
    r_by_user.id AS matched_regulator_id,
    r_by_user.name AS matched_regulator_name,
    r_by_user.role_type AS matched_role_type
FROM complaint_handle h
LEFT JOIN complaint c ON c.id = h.complaint_id
JOIN food_regulation_db.food_regulator r_by_user
    ON r_by_user.user_id = h.handler_id AND r_by_user.deleted = 0
LEFT JOIN food_regulation_db.food_regulator r_by_id
    ON r_by_id.id = h.handler_id AND r_by_id.deleted = 0
WHERE h.deleted = 0
  AND r_by_id.id IS NULL
ORDER BY h.complaint_id;


-- 5. 角色不匹配：assigned_to / processed_by / handler_id 应为执法人员
SELECT
    'role_mismatch_enforcer_expected' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'assigned_to' AS field_name,
    c.assigned_to AS field_value,
    r.name AS regulator_name,
    r.role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r ON r.id = c.assigned_to AND r.deleted = 0
WHERE c.deleted = 0
  AND c.assigned_to IS NOT NULL
  AND r.role_type <> 'REGULATOR_ENFORCER'

UNION ALL

SELECT
    'role_mismatch_enforcer_expected' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'processed_by' AS field_name,
    c.processed_by AS field_value,
    r.name AS regulator_name,
    r.role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r ON r.id = c.processed_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.processed_by IS NOT NULL
  AND r.role_type <> 'REGULATOR_ENFORCER'

UNION ALL

SELECT
    'role_mismatch_enforcer_expected' AS check_type,
    h.complaint_id,
    c.complaint_no,
    'handler_id' AS field_name,
    h.handler_id AS field_value,
    r.name AS regulator_name,
    r.role_type
FROM complaint_handle h
LEFT JOIN complaint c ON c.id = h.complaint_id
JOIN food_regulation_db.food_regulator r ON r.id = h.handler_id AND r.deleted = 0
WHERE h.deleted = 0
  AND r.role_type <> 'REGULATOR_ENFORCER'
ORDER BY complaint_id, field_name;


-- 6. 角色不匹配：assigned_by / accepted_by / rejected_by 应为区域管理员
SELECT
    'role_mismatch_admin_expected' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'assigned_by' AS field_name,
    c.assigned_by AS field_value,
    r.name AS regulator_name,
    r.role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r ON r.id = c.assigned_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.assigned_by IS NOT NULL
  AND r.role_type <> 'REGULATOR_ADMIN'

UNION ALL

SELECT
    'role_mismatch_admin_expected' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'accepted_by' AS field_name,
    c.accepted_by AS field_value,
    r.name AS regulator_name,
    r.role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r ON r.id = c.accepted_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.accepted_by IS NOT NULL
  AND r.role_type <> 'REGULATOR_ADMIN'

UNION ALL

SELECT
    'role_mismatch_admin_expected' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    'rejected_by' AS field_name,
    c.rejected_by AS field_value,
    r.name AS regulator_name,
    r.role_type
FROM complaint c
JOIN food_regulation_db.food_regulator r ON r.id = c.rejected_by AND r.deleted = 0
WHERE c.deleted = 0
  AND c.rejected_by IS NOT NULL
  AND r.role_type <> 'REGULATOR_ADMIN'
ORDER BY complaint_id, field_name;


-- 7. complaint_handle 与 complaint.processed_by 不一致
SELECT
    'processed_handler_mismatch' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    c.status,
    c.processed_by,
    rp.name AS processed_by_name,
    h.handler_id,
    rh.name AS handler_name
FROM complaint c
JOIN complaint_handle h ON h.complaint_id = c.id AND h.deleted = 0
LEFT JOIN food_regulation_db.food_regulator rp ON rp.id = c.processed_by AND rp.deleted = 0
LEFT JOIN food_regulation_db.food_regulator rh ON rh.id = h.handler_id AND rh.deleted = 0
WHERE c.deleted = 0
  AND (
      c.processed_by IS NULL
      OR h.handler_id <> c.processed_by
  )
ORDER BY c.id;


-- 8. 投诉状态与关键处理字段不一致
SELECT
    'status_field_inconsistent' AS check_type,
    c.id AS complaint_id,
    c.complaint_no,
    c.status,
    c.assigned_to,
    c.accepted_by,
    c.processed_by,
    c.rejected_by
FROM complaint c
WHERE c.deleted = 0
  AND (
      (c.status = 'ASSIGNED' AND c.assigned_to IS NULL)
      OR (c.status = 'PROCESSING' AND (c.assigned_to IS NULL OR c.accepted_by IS NULL))
      OR (c.status = 'FEEDBACKED' AND c.processed_by IS NULL)
      OR (c.status = 'REJECTED' AND c.rejected_by IS NULL)
  )
ORDER BY c.id;


-- 9. 汇总：按监管员角色查看投诉引用分布，便于人工扫一眼
SELECT
    'complaint_actor_distribution' AS check_type,
    x.field_name,
    r.id AS regulator_id,
    r.user_id,
    r.name,
    r.role_type,
    COUNT(*) AS ref_count
FROM (
    SELECT 'assigned_to' AS field_name, assigned_to AS regulator_id
    FROM complaint
    WHERE deleted = 0 AND assigned_to IS NOT NULL
    UNION ALL
    SELECT 'assigned_by' AS field_name, assigned_by AS regulator_id
    FROM complaint
    WHERE deleted = 0 AND assigned_by IS NOT NULL
    UNION ALL
    SELECT 'accepted_by' AS field_name, accepted_by AS regulator_id
    FROM complaint
    WHERE deleted = 0 AND accepted_by IS NOT NULL
    UNION ALL
    SELECT 'processed_by' AS field_name, processed_by AS regulator_id
    FROM complaint
    WHERE deleted = 0 AND processed_by IS NOT NULL
    UNION ALL
    SELECT 'rejected_by' AS field_name, rejected_by AS regulator_id
    FROM complaint
    WHERE deleted = 0 AND rejected_by IS NOT NULL
) x
LEFT JOIN food_regulation_db.food_regulator r ON r.id = x.regulator_id AND r.deleted = 0
GROUP BY x.field_name, r.id, r.user_id, r.name, r.role_type
ORDER BY x.field_name, ref_count DESC, regulator_id;
