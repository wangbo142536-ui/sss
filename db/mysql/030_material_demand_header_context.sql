-- Material demand header context from owner/supplier Excel templates.
-- Idempotent migration. Scope: material demand import main information only.

SET @schema_name := DATABASE();

SET @has_inquiry_no := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'inquiry_no'
);
SET @sql := IF(@has_inquiry_no = 0,
  'ALTER TABLE material_demand ADD COLUMN inquiry_no VARCHAR(120) NULL COMMENT ''Business inquiry number from uploaded template'' AFTER application_no',
  'SELECT ''material_demand.inquiry_no already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_material_type := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'material_type'
);
SET @sql := IF(@has_material_type = 0,
  'ALTER TABLE material_demand ADD COLUMN material_type VARCHAR(120) NULL COMMENT ''Material type from uploaded template'' AFTER inquiry_no',
  'SELECT ''material_demand.material_type already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_currency := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'currency'
);
SET @sql := IF(@has_currency = 0,
  'ALTER TABLE material_demand ADD COLUMN currency VARCHAR(20) NULL COMMENT ''Template currency'' AFTER material_type',
  'SELECT ''material_demand.currency already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_recipient_company := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'recipient_company'
);
SET @sql := IF(@has_recipient_company = 0,
  'ALTER TABLE material_demand ADD COLUMN recipient_company VARCHAR(255) NULL COMMENT ''Recipient company from uploaded template'' AFTER currency',
  'SELECT ''material_demand.recipient_company already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_handler_name := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'handler_name'
);
SET @sql := IF(@has_handler_name = 0,
  'ALTER TABLE material_demand ADD COLUMN handler_name VARCHAR(120) NULL COMMENT ''Handler name from uploaded template'' AFTER recipient_company',
  'SELECT ''material_demand.handler_name already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_handler_email := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'handler_email'
);
SET @sql := IF(@has_handler_email = 0,
  'ALTER TABLE material_demand ADD COLUMN handler_email VARCHAR(180) NULL COMMENT ''Handler email from uploaded template'' AFTER handler_name',
  'SELECT ''material_demand.handler_email already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_inquiry_no_idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND index_name = 'idx_material_demand_inquiry_no'
);
SET @sql := IF(@has_inquiry_no_idx = 0,
  'CREATE INDEX idx_material_demand_inquiry_no ON material_demand (inquiry_no)',
  'SELECT ''idx_material_demand_inquiry_no already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
