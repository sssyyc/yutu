USE yutu_travel;
SET NAMES utf8mb4;

SET @current_schema = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'merchant_shop'
      AND COLUMN_NAME = 'license_image'
  ),
  'SELECT 1',
  'ALTER TABLE merchant_shop ADD COLUMN license_image VARCHAR(255) NULL AFTER license_no'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'merchant_shop'
      AND COLUMN_NAME = 'id_card_front_image'
  ),
  'SELECT 1',
  'ALTER TABLE merchant_shop ADD COLUMN id_card_front_image VARCHAR(255) NULL AFTER license_image'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'merchant_shop'
      AND COLUMN_NAME = 'id_card_back_image'
  ),
  'SELECT 1',
  'ALTER TABLE merchant_shop ADD COLUMN id_card_back_image VARCHAR(255) NULL AFTER id_card_front_image'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'merchant_shop'
      AND COLUMN_NAME = 'audit_remark'
  ),
  'SELECT 1',
  'ALTER TABLE merchant_shop ADD COLUMN audit_remark VARCHAR(255) NULL AFTER audit_status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'merchant_shop'
      AND COLUMN_NAME = 'audit_time'
  ),
  'SELECT 1',
  'ALTER TABLE merchant_shop ADD COLUMN audit_time DATETIME NULL AFTER audit_remark'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE merchant_shop
SET audit_remark = COALESCE(audit_remark, '初始化示例商户，已审核通过'),
    audit_time = COALESCE(audit_time, NOW())
WHERE audit_status = 1;
