USE food_regulation_db;

-- 修复企业辖区与执法人员街道辖区不一致的问题。
-- 当前项目里执法端投诉/企业/工作台等查询，会按 food_enterprise.region_id 与监管员辖区做范围匹配。
-- 因此企业种子数据必须落到实际街道层级，不能只停留在区县层级。

UPDATE food_enterprise
SET
    region_id = CASE id
        WHEN 1 THEN 7
        WHEN 2 THEN 9
        WHEN 3 THEN 8
        WHEN 4 THEN 7
        WHEN 5 THEN 10
        WHEN 6 THEN 9
        ELSE region_id
    END,
    address_id = CASE id
        WHEN 1 THEN 1
        WHEN 2 THEN 4
        WHEN 3 THEN 3
        WHEN 4 THEN 2
        WHEN 5 THEN 6
        WHEN 6 THEN 5
        ELSE address_id
    END,
    regulator_name = CASE id
        WHEN 1 THEN '王执法'
        WHEN 2 THEN '赵执法'
        WHEN 3 THEN '刘执法'
        WHEN 4 THEN '王执法'
        WHEN 5 THEN '陈执法'
        ELSE regulator_name
    END
WHERE id IN (1, 2, 3, 4, 5, 6);
