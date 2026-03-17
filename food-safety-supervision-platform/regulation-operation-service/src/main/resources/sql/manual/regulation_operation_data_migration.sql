-- Manual migration script for regulation-operation-service
-- Prerequisites:
-- 1. Stop writes to /api/regulation-operation/tasks/**, /api/regulation-operation/inspections/**, /api/regulation-operation/rectifications/**
-- 2. Back up food_regulation_db before running
-- 3. Start regulation-operation-service once to create food_regulation_operation_db and its tables

USE food_regulation_operation_db;

TRUNCATE TABLE warning_event_outbox;
TRUNCATE TABLE rectification_action_log;
TRUNCATE TABLE rectification_task;
TRUNCATE TABLE inspection_item;
TRUNCATE TABLE inspection_record;
TRUNCATE TABLE inspection_task;

INSERT INTO food_regulation_operation_db.inspection_task (
  id, task_no, enterprise_id, region_id, task_title, task_desc, priority, status,
  created_by, assigned_to, assigned_by, assigned_time, started_time, completed_time,
  deadline, create_time, update_time, deleted
)
SELECT
  id, task_no, enterprise_id, region_id, task_title, task_desc, priority, status,
  created_by, assigned_to, assigned_by, assigned_time, started_time, completed_time,
  deadline, create_time, update_time, deleted
FROM food_regulation_db.inspection_task;

INSERT INTO food_regulation_operation_db.inspection_record (
  id, task_id, enterprise_id, inspector_id, inspection_date, result,
  problem_desc, create_time, update_time, deleted
)
SELECT
  id, task_id, enterprise_id, inspector_id, inspection_date, result,
  problem_desc, create_time, update_time, deleted
FROM food_regulation_db.inspection_record;

INSERT INTO food_regulation_operation_db.inspection_item (
  id, inspection_id, item_name, item_result, problem_desc, create_time, update_time, deleted
)
SELECT
  id, inspection_id, item_name, item_result, problem_desc, create_time, update_time, deleted
FROM food_regulation_db.inspection_item;

INSERT INTO food_regulation_operation_db.rectification_task (
  id, inspection_id, enterprise_id, rectification_desc, progress, status,
  submit_deadline, review_deadline, finish_time, confirmed_by, confirmed_time,
  create_time, update_time, deleted
)
SELECT
  id, inspection_id, enterprise_id, rectification_desc, progress, status,
  submit_deadline, review_deadline, finish_time, confirmed_by, confirmed_time,
  create_time, update_time, deleted
FROM food_regulation_db.rectification_task;

INSERT INTO food_regulation_operation_db.rectification_action_log (
  id, rectification_id, action_type, operator_id, action_comment, attachment_urls, create_time, deleted
)
SELECT
  id, rectification_id, action_type, operator_id, action_comment, attachment_urls, create_time, deleted
FROM food_regulation_db.rectification_action_log;

INSERT INTO food_regulation_operation_db.warning_event_outbox (
  id, event_key, event_type, payload_json, status, retry_count, next_retry_time,
  last_attempt_time, last_error, create_time, update_time, deleted
)
SELECT
  id, event_key, event_type, payload_json, status, retry_count, next_retry_time,
  last_attempt_time, last_error, create_time, update_time, deleted
FROM food_regulation_db.warning_event_outbox;

SELECT 'inspection_task' AS table_name,
       (SELECT COUNT(*) FROM food_regulation_db.inspection_task) AS source_count,
       (SELECT COUNT(*) FROM food_regulation_operation_db.inspection_task) AS target_count;

SELECT 'inspection_record' AS table_name,
       (SELECT COUNT(*) FROM food_regulation_db.inspection_record) AS source_count,
       (SELECT COUNT(*) FROM food_regulation_operation_db.inspection_record) AS target_count;

SELECT 'inspection_item' AS table_name,
       (SELECT COUNT(*) FROM food_regulation_db.inspection_item) AS source_count,
       (SELECT COUNT(*) FROM food_regulation_operation_db.inspection_item) AS target_count;

SELECT 'rectification_task' AS table_name,
       (SELECT COUNT(*) FROM food_regulation_db.rectification_task) AS source_count,
       (SELECT COUNT(*) FROM food_regulation_operation_db.rectification_task) AS target_count;

SELECT 'rectification_action_log' AS table_name,
       (SELECT COUNT(*) FROM food_regulation_db.rectification_action_log) AS source_count,
       (SELECT COUNT(*) FROM food_regulation_operation_db.rectification_action_log) AS target_count;

SELECT 'warning_event_outbox' AS table_name,
       (SELECT COUNT(*) FROM food_regulation_db.warning_event_outbox) AS source_count,
       (SELECT COUNT(*) FROM food_regulation_operation_db.warning_event_outbox) AS target_count;
