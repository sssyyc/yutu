USE yutu_travel;
SET NAMES utf8mb4;

SET @current_schema = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'tour_route'
      AND COLUMN_NAME = 'audit_remark'
  ),
  'SELECT 1',
  'ALTER TABLE tour_route ADD COLUMN audit_remark VARCHAR(255) NULL AFTER audit_status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
