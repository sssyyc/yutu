USE yutu_travel;
SET NAMES utf8mb4;

SET @current_schema = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'contract_template'
      AND COLUMN_NAME = 'download_count'
  ),
  'SELECT 1',
  'ALTER TABLE contract_template ADD COLUMN download_count BIGINT NOT NULL DEFAULT 0 AFTER template_content'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'contract_template'
      AND COLUMN_NAME = 'use_count'
  ),
  'SELECT 1',
  'ALTER TABLE contract_template ADD COLUMN use_count BIGINT NOT NULL DEFAULT 0 AFTER download_count'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE contract_template
SET download_count = IFNULL(download_count, 0),
    use_count = IFNULL(use_count, 0);
