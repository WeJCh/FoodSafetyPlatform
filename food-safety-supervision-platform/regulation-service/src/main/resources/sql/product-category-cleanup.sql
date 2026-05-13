USE food_regulation_db;

-- 产品分类历史口径清洗（收敛为 8 类监管产品品类）

UPDATE food_product
SET category = '餐饮自制食品'
WHERE category IN ('餐饮服务', '快餐', '餐饮自制食品');

UPDATE food_product
SET category = '预包装食品'
WHERE category IN ('预包装食品', '烘焙食品', '冷冻食品', '饮料', '酒类');

UPDATE food_product
SET category = '散装食品'
WHERE category = '散装食品';

UPDATE food_product
SET category = '生鲜农产品'
WHERE category IN ('水果', '蔬菜', '生鲜农产品');

UPDATE food_product
SET category = '乳制品'
WHERE category = '乳制品';

UPDATE food_product
SET category = '肉及水产制品'
WHERE category IN ('肉制品', '水产制品', '肉及水产制品');

UPDATE food_product
SET category = '粮油调味品'
WHERE category IN ('调味品', '粮油及谷物制品', '谷物制品', '粮油调味品');

UPDATE food_product
SET category = '其他食品'
WHERE category = '其他食品';
