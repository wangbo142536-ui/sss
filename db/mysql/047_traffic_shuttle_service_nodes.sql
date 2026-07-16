SET @schema_name = DATABASE();

SET @has_service_nodes_json = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'traffic_shuttle_service'
    AND column_name = 'service_nodes_json'
);

SET @sql = IF(
  @has_service_nodes_json = 0,
  'ALTER TABLE traffic_shuttle_service ADD COLUMN service_nodes_json JSON NULL AFTER remark',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
