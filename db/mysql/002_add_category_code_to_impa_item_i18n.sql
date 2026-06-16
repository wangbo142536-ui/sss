USE ship_supply_platform;

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'impa_item_i18n'
    AND column_name = 'category_code'
);

SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE impa_item_i18n ADD COLUMN category_code VARCHAR(20) NULL COMMENT ''IMPA大类编码，关联impa_category.category_code'' AFTER impa_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE impa_item_i18n i18n
JOIN impa_item item ON item.impa_code = i18n.impa_code
SET i18n.category_code = item.category_code
WHERE i18n.category_code IS NULL;

SET @missing_category_code := (
  SELECT COUNT(*)
  FROM impa_item_i18n
  WHERE category_code IS NULL
);

SET @sql := IF(
  @missing_category_code = 0,
  'ALTER TABLE impa_item_i18n MODIFY COLUMN category_code VARCHAR(20) NOT NULL COMMENT ''IMPA大类编码，关联impa_category.category_code''',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''impa_item_i18n.category_code backfill has NULL rows'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'impa_item_i18n'
    AND index_name = 'idx_impa_i18n_category'
);

SET @sql := IF(
  @index_exists = 0,
  'ALTER TABLE impa_item_i18n ADD KEY idx_impa_i18n_category (category_code)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'impa_item_i18n'
    AND constraint_name = 'fk_impa_i18n_category'
    AND constraint_type = 'FOREIGN KEY'
);

SET @sql := IF(
  @fk_exists = 0,
  'ALTER TABLE impa_item_i18n ADD CONSTRAINT fk_impa_i18n_category FOREIGN KEY (category_code) REFERENCES impa_category (category_code) ON DELETE CASCADE',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
