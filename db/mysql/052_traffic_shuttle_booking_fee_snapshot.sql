SET @schema_name = DATABASE();
SET @table_name = 'traffic_shuttle_booking';

SET @has_request_no = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'request_no'
);
SET @sql = IF(@has_request_no = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN request_no VARCHAR(80) NULL AFTER request_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_allow_share = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'allow_share'
);
SET @sql = IF(@has_allow_share = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN allow_share TINYINT(1) NOT NULL DEFAULT 1 AFTER amount', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_customs_service = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'customs_service'
);
SET @sql = IF(@has_customs_service = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN customs_service TINYINT(1) NOT NULL DEFAULT 0 AFTER allow_share', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_crane_service = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'crane_service'
);
SET @sql = IF(@has_crane_service = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN crane_service TINYINT(1) NOT NULL DEFAULT 0 AFTER customs_service', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_crane_count = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'crane_count'
);
SET @sql = IF(@has_crane_count = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN crane_count INT NOT NULL DEFAULT 0 AFTER crane_service', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_freight_fee = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'freight_fee'
);
SET @sql = IF(@has_freight_fee = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN freight_fee DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER crane_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_customs_fee = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'customs_fee'
);
SET @sql = IF(@has_customs_fee = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN customs_fee DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER freight_fee', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_crane_fee = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = @table_name AND column_name = 'crane_fee'
);
SET @sql = IF(@has_crane_fee = 0, 'ALTER TABLE traffic_shuttle_booking ADD COLUMN crane_fee DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER customs_fee', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
