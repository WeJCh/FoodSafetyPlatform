-- complaint old table retire script
-- Usage:
-- 1. Execute this script in Navicat after you have verified complaint-service is already using food_complaint_db.
-- 2. This script does not directly drop old tables. It first verifies counts, then renames old tables as backups.
-- 3. After an observation period, execute the final DROP section manually.

-- Step 0: basic count check
SELECT 'source complaint count' AS metric, COUNT(*) AS total FROM food_regulation_db.complaint
UNION ALL
SELECT 'target complaint count' AS metric, COUNT(*) AS total FROM food_complaint_db.complaint
UNION ALL
SELECT 'source complaint_handle count' AS metric, COUNT(*) AS total FROM food_regulation_db.complaint_handle
UNION ALL
SELECT 'target complaint_handle count' AS metric, COUNT(*) AS total FROM food_complaint_db.complaint_handle;

-- Step 1: hard stop when counts do not match
DROP PROCEDURE IF EXISTS retire_old_complaint_tables;
DELIMITER $$
CREATE PROCEDURE retire_old_complaint_tables()
BEGIN
    DECLARE source_complaint_count BIGINT DEFAULT 0;
    DECLARE target_complaint_count BIGINT DEFAULT 0;
    DECLARE source_handle_count BIGINT DEFAULT 0;
    DECLARE target_handle_count BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO source_complaint_count FROM food_regulation_db.complaint;
    SELECT COUNT(*) INTO target_complaint_count FROM food_complaint_db.complaint;
    SELECT COUNT(*) INTO source_handle_count FROM food_regulation_db.complaint_handle;
    SELECT COUNT(*) INTO target_handle_count FROM food_complaint_db.complaint_handle;

    IF source_complaint_count <> target_complaint_count THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'retire aborted: complaint row count mismatch between food_regulation_db and food_complaint_db';
    END IF;

    IF source_handle_count <> target_handle_count THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'retire aborted: complaint_handle row count mismatch between food_regulation_db and food_complaint_db';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'food_regulation_db'
          AND table_name = 'complaint_bak'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'retire aborted: backup table food_regulation_db.complaint_bak already exists';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'food_regulation_db'
          AND table_name = 'complaint_handle_bak'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'retire aborted: backup table food_regulation_db.complaint_handle_bak already exists';
    END IF;

    RENAME TABLE
        food_regulation_db.complaint TO food_regulation_db.complaint_bak,
        food_regulation_db.complaint_handle TO food_regulation_db.complaint_handle_bak;
END $$
DELIMITER ;

-- Step 2: execute retire operation
CALL retire_old_complaint_tables();

-- Step 3: verify old tables have been renamed successfully
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'food_regulation_db'
  AND table_name IN ('complaint', 'complaint_handle', 'complaint_bak', 'complaint_handle_bak')
ORDER BY table_name;

DROP PROCEDURE IF EXISTS retire_old_complaint_tables;

-- Step 4: optional final cleanup after observation period
-- Recommended: keep complaint_bak and complaint_handle_bak for at least 7-14 days before final delete.
-- Remove the comment markers below only after you are sure rollback is no longer needed.
--
-- DROP TABLE IF EXISTS food_regulation_db.complaint_handle_bak;
-- DROP TABLE IF EXISTS food_regulation_db.complaint_bak;
