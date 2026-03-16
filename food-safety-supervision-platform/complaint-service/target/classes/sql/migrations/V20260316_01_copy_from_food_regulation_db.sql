USE food_complaint_db;

INSERT INTO food_complaint_db.complaint (
  id,
  complaint_no,
  complainant_name,
  contact,
  submitter_user_id,
  enterprise_id,
  complaint_type,
  content,
  image_urls,
  status,
  source_type,
  source_id,
  assigned_to,
  assigned_by,
  assigned_time,
  accepted_by,
  accepted_time,
  processed_by,
  processed_time,
  rejected_by,
  rejected_time,
  create_time,
  update_time,
  deleted
)
SELECT
  id,
  complaint_no,
  complainant_name,
  contact,
  submitter_user_id,
  enterprise_id,
  complaint_type,
  content,
  image_urls,
  status,
  source_type,
  source_id,
  assigned_to,
  assigned_by,
  assigned_time,
  accepted_by,
  accepted_time,
  processed_by,
  processed_time,
  rejected_by,
  rejected_time,
  create_time,
  update_time,
  deleted
FROM food_regulation_db.complaint
ON DUPLICATE KEY UPDATE
  complaint_no = VALUES(complaint_no),
  complainant_name = VALUES(complainant_name),
  contact = VALUES(contact),
  submitter_user_id = VALUES(submitter_user_id),
  enterprise_id = VALUES(enterprise_id),
  complaint_type = VALUES(complaint_type),
  content = VALUES(content),
  image_urls = VALUES(image_urls),
  status = VALUES(status),
  source_type = VALUES(source_type),
  source_id = VALUES(source_id),
  assigned_to = VALUES(assigned_to),
  assigned_by = VALUES(assigned_by),
  assigned_time = VALUES(assigned_time),
  accepted_by = VALUES(accepted_by),
  accepted_time = VALUES(accepted_time),
  processed_by = VALUES(processed_by),
  processed_time = VALUES(processed_time),
  rejected_by = VALUES(rejected_by),
  rejected_time = VALUES(rejected_time),
  create_time = VALUES(create_time),
  update_time = VALUES(update_time),
  deleted = VALUES(deleted);

INSERT INTO food_complaint_db.complaint_handle (
  id,
  complaint_id,
  handler_id,
  handle_result,
  handle_time,
  create_time,
  update_time,
  deleted
)
SELECT
  id,
  complaint_id,
  handler_id,
  handle_result,
  handle_time,
  create_time,
  update_time,
  deleted
FROM food_regulation_db.complaint_handle
ON DUPLICATE KEY UPDATE
  complaint_id = VALUES(complaint_id),
  handler_id = VALUES(handler_id),
  handle_result = VALUES(handle_result),
  handle_time = VALUES(handle_time),
  create_time = VALUES(create_time),
  update_time = VALUES(update_time),
  deleted = VALUES(deleted);

SELECT 'complaint source count' AS metric, COUNT(*) AS total FROM food_regulation_db.complaint
UNION ALL
SELECT 'complaint target count' AS metric, COUNT(*) AS total FROM food_complaint_db.complaint
UNION ALL
SELECT 'complaint_handle source count' AS metric, COUNT(*) AS total FROM food_regulation_db.complaint_handle
UNION ALL
SELECT 'complaint_handle target count' AS metric, COUNT(*) AS total FROM food_complaint_db.complaint_handle;
