USE food_complaint_db;

-- 修复 complaint-service 初始化数据中监管人员字段混用了 food_regulator.user_id 的问题。
-- 当前 complaint / complaint_handle 相关监管人字段应统一保存 regulation-service 的 food_regulator.id。

UPDATE complaint
SET
    assigned_to = CASE id
        WHEN 1 THEN 3
        WHEN 2 THEN 4
        WHEN 3 THEN 3
        WHEN 4 THEN 3
        WHEN 5 THEN 3
        WHEN 6 THEN 6
        WHEN 7 THEN 5
        WHEN 9 THEN 3
        ELSE assigned_to
    END,
    assigned_by = CASE id
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        WHEN 3 THEN 1
        WHEN 4 THEN 1
        WHEN 5 THEN 1
        WHEN 6 THEN 2
        WHEN 7 THEN 2
        WHEN 9 THEN 1
        ELSE assigned_by
    END,
    accepted_by = CASE id
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        WHEN 3 THEN 1
        WHEN 4 THEN 1
        WHEN 5 THEN 1
        WHEN 6 THEN 2
        WHEN 9 THEN 1
        ELSE accepted_by
    END,
    processed_by = CASE id
        WHEN 1 THEN 3
        WHEN 2 THEN 4
        WHEN 3 THEN 3
        WHEN 4 THEN 3
        WHEN 5 THEN 3
        ELSE processed_by
    END,
    rejected_by = CASE id
        WHEN 9 THEN 1
        ELSE rejected_by
    END
WHERE id IN (1, 2, 3, 4, 5, 6, 7, 9);

UPDATE complaint_handle
SET handler_id = CASE complaint_id
    WHEN 1 THEN 3
    WHEN 2 THEN 4
    WHEN 3 THEN 3
    WHEN 4 THEN 3
    WHEN 5 THEN 3
    ELSE handler_id
END
WHERE complaint_id IN (1, 2, 3, 4, 5);
