SET @schema_name = DATABASE();

SET @has_departure_point = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'traffic_shuttle_service'
    AND column_name = 'departure_point'
);

SET @sql = IF(
  @has_departure_point = 0,
  'ALTER TABLE traffic_shuttle_service ADD COLUMN departure_point VARCHAR(128) NULL AFTER anchorage_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_destination_point = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'traffic_shuttle_service'
    AND column_name = 'destination_point'
);

SET @sql = IF(
  @has_destination_point = 0,
  'ALTER TABLE traffic_shuttle_service ADD COLUMN destination_point VARCHAR(128) NULL AFTER departure_point',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
