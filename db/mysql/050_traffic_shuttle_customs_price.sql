SET @schema_name = DATABASE();

SET @has_customs_price = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'traffic_shuttle_service'
    AND column_name = 'customs_price'
);

SET @sql = IF(
  @has_customs_price = 0,
  'ALTER TABLE traffic_shuttle_service ADD COLUMN customs_price DECIMAL(12,2) NULL AFTER shared_price',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
