SET @schema_name = DATABASE();
SET @table_name = 'traffic_shuttle_booking';

SET @has_node_index = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'node_index'
);
SET @sql = IF(@has_node_index = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN node_index INT NULL AFTER request_no', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_node_name = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'node_name'
);
SET @sql = IF(@has_node_name = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN node_name VARCHAR(80) NULL AFTER node_index', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_node_time = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'node_time'
);
SET @sql = IF(@has_node_time = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN node_time VARCHAR(80) NULL AFTER node_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_vessel_name = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'vessel_name'
);
SET @sql = IF(@has_vessel_name = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN vessel_name VARCHAR(160) NULL AFTER node_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_vessel_imo = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'vessel_imo'
);
SET @sql = IF(@has_vessel_imo = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN vessel_imo VARCHAR(80) NULL AFTER vessel_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_anchorage_time = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'anchorage_time'
);
SET @sql = IF(@has_anchorage_time = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN anchorage_time VARCHAR(80) NULL AFTER vessel_imo', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_anchorage_position = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'anchorage_position'
);
SET @sql = IF(@has_anchorage_position = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN anchorage_position VARCHAR(120) NULL AFTER anchorage_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_pallet_count = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'pallet_count'
);
SET @sql = IF(@has_pallet_count = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN pallet_count VARCHAR(40) NULL AFTER anchorage_position', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
