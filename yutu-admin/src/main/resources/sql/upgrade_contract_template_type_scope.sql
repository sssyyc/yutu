SET @add_template_type = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'contract_template'
        AND COLUMN_NAME = 'template_type'
    ),
    'SELECT 1',
    'ALTER TABLE contract_template ADD COLUMN template_type VARCHAR(32) NOT NULL DEFAULT ''STANDARD'' AFTER template_code'
  )
);
PREPARE stmt FROM @add_template_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_apply_scope = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'contract_template'
        AND COLUMN_NAME = 'apply_scope'
    ),
    'SELECT 1',
    'ALTER TABLE contract_template ADD COLUMN apply_scope VARCHAR(32) NOT NULL DEFAULT ''DOMESTIC'' AFTER template_type'
  )
);
PREPARE stmt FROM @add_apply_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE contract_template
SET template_type = COALESCE(NULLIF(template_type, ''), 'STANDARD'),
    apply_scope = COALESCE(NULLIF(apply_scope, ''), 'DOMESTIC');
