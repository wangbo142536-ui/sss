SET @schema_name = DATABASE();

CREATE TABLE IF NOT EXISTS company_vessel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  vessel_name VARCHAR(160) NOT NULL,
  vessel_type VARCHAR(80) NULL,
  build_date DATE NULL,
  next_maintenance_date DATE NULL,
  capacity VARCHAR(80) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_company_vessel_company (company_id, status),
  CONSTRAINT fk_company_vessel_company FOREIGN KEY (company_id) REFERENCES company (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业船舶管理';

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN business_contact_id BIGINT NULL AFTER remark',
    'SELECT ''traffic_service_order.business_contact_id exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'business_contact_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN business_contact_name VARCHAR(100) NULL AFTER business_contact_id',
    'SELECT ''traffic_service_order.business_contact_name exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'business_contact_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN business_contact_phone VARCHAR(60) NULL AFTER business_contact_name',
    'SELECT ''traffic_service_order.business_contact_phone exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'business_contact_phone'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN accepted_at DATETIME NULL AFTER business_contact_phone',
    'SELECT ''traffic_service_order.accepted_at exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'accepted_at'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN traffic_vessel_id BIGINT NULL AFTER accepted_at',
    'SELECT ''traffic_service_order.traffic_vessel_id exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'traffic_vessel_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN traffic_vessel_name VARCHAR(160) NULL AFTER traffic_vessel_id',
    'SELECT ''traffic_service_order.traffic_vessel_name exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'traffic_vessel_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN handler_contact_id BIGINT NULL AFTER traffic_vessel_name',
    'SELECT ''traffic_service_order.handler_contact_id exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'handler_contact_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN handler_name VARCHAR(100) NULL AFTER handler_contact_id',
    'SELECT ''traffic_service_order.handler_name exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'handler_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN handler_phone VARCHAR(60) NULL AFTER handler_name',
    'SELECT ''traffic_service_order.handler_phone exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'handler_phone'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN supplier_message VARCHAR(1000) NULL AFTER handler_phone',
    'SELECT ''traffic_service_order.supplier_message exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'supplier_message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN departure_time DATETIME NULL AFTER supplier_message',
    'SELECT ''traffic_service_order.departure_time exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'departure_time'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN arrival_time DATETIME NULL AFTER departure_time',
    'SELECT ''traffic_service_order.arrival_time exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'arrival_time'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN return_start_time DATETIME NULL AFTER arrival_time',
    'SELECT ''traffic_service_order.return_start_time exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'return_start_time'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN return_end_time DATETIME NULL AFTER return_start_time',
    'SELECT ''traffic_service_order.return_end_time exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'return_end_time'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN sign_photo_url VARCHAR(500) NULL AFTER return_end_time',
    'SELECT ''traffic_service_order.sign_photo_url exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'sign_photo_url'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN pickup_photo_url VARCHAR(500) NULL AFTER sign_photo_url',
    'SELECT ''traffic_service_order.pickup_photo_url exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'pickup_photo_url'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN return_arrival_photo_url VARCHAR(500) NULL AFTER pickup_photo_url',
    'SELECT ''traffic_service_order.return_arrival_photo_url exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'return_arrival_photo_url'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
