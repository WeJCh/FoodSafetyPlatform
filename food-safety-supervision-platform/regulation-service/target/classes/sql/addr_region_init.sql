-- addr_region init data (demo subset, to street level)
-- Run after schema.sql in food_regulation_db

USE food_regulation_db;

-- Optional cleanup before init:
-- DELETE FROM addr_region;

INSERT INTO addr_region (id, parent_id, name, level, deleted) VALUES
  (360000, NULL, '江西省', 1, 0),
  (360100, 360000, '南昌市', 2, 0),
  (360102, 360100, '东湖区', 3, 0),
  (360103, 360100, '西湖区', 3, 0),
  (360102001, 360102, '公园街道', 4, 0),
  (360102002, 360102, '滕王阁街道', 4, 0),
  (360103001, 360103, '朝阳洲街道', 4, 0),
  (360103002, 360103, '丁公路街道', 4, 0),
  (360900, 360000, '宜春市', 2, 0),
  (360902, 360900, '袁州区', 3, 0),
  (360923, 360900, '上高县', 3, 0),
  (360902001, 360902, '秀江街道', 4, 0),
  (360902002, 360902, '官塘街道', 4, 0),
  (360923001, 360923, '敖阳街道', 4, 0),
  (360923002, 360923, '塔下乡', 4, 0);
