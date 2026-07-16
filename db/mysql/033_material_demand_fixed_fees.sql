-- Persist comparison fixed fees on the material demand header.
-- These values are demand-level quote settings used by comparison strategy totals.

SET @schema_name := DATABASE();

SET @has_fixed_freight_fee := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'fixed_freight_fee'
);
SET @sql := IF(@has_fixed_freight_fee = 0,
  'ALTER TABLE material_demand ADD COLUMN fixed_freight_fee DECIMAL(18,4) NULL COMMENT ''Fixed freight fee'' AFTER currency',
  'SELECT ''material_demand.fixed_freight_fee already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fixed_customs_fee := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'fixed_customs_fee'
);
SET @sql := IF(@has_fixed_customs_fee = 0,
  'ALTER TABLE material_demand ADD COLUMN fixed_customs_fee DECIMAL(18,4) NULL COMMENT ''Fixed customs declaration fee'' AFTER fixed_freight_fee',
  'SELECT ''material_demand.fixed_customs_fee already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fixed_crane_fee := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'fixed_crane_fee'
);
SET @sql := IF(@has_fixed_crane_fee = 0,
  'ALTER TABLE material_demand ADD COLUMN fixed_crane_fee DECIMAL(18,4) NULL COMMENT ''Fixed crane fee'' AFTER fixed_customs_fee',
  'SELECT ''material_demand.fixed_crane_fee already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fixed_other_fee := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'fixed_other_fee'
);
SET @sql := IF(@has_fixed_other_fee = 0,
  'ALTER TABLE material_demand ADD COLUMN fixed_other_fee DECIMAL(18,4) NULL COMMENT ''Other fixed fee'' AFTER fixed_crane_fee',
  'SELECT ''material_demand.fixed_other_fee already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
