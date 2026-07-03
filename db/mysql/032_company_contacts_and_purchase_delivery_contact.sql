SET @schema_name := DATABASE();

CREATE TABLE IF NOT EXISTS company_contact (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Contact id',
  company_id BIGINT NOT NULL COMMENT 'Company id',
  contact_name VARCHAR(120) NOT NULL COMMENT 'Contact name',
  contact_phone VARCHAR(80) NOT NULL COMMENT 'Contact phone',
  contact_email VARCHAR(180) NULL COMMENT 'Contact email',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Contact status',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (id),
  KEY idx_company_contact_company_status (company_id, status),
  KEY idx_company_contact_company_name (company_id, contact_name),
  CONSTRAINT fk_company_contact_company
    FOREIGN KEY (company_id) REFERENCES company (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Company contacts';

SET @has_delivery_contact_name := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order' AND column_name = 'delivery_contact_name'
);
SET @sql := IF(@has_delivery_contact_name = 0,
  'ALTER TABLE purchase_order ADD COLUMN delivery_contact_name VARCHAR(120) NULL COMMENT ''Delivery contact name'' AFTER required_delivery_time',
  'SELECT ''purchase_order.delivery_contact_name already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_delivery_contact_phone := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order' AND column_name = 'delivery_contact_phone'
);
SET @sql := IF(@has_delivery_contact_phone = 0,
  'ALTER TABLE purchase_order ADD COLUMN delivery_contact_phone VARCHAR(80) NULL COMMENT ''Delivery contact phone'' AFTER delivery_contact_name',
  'SELECT ''purchase_order.delivery_contact_phone already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_delivery_contact_email := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order' AND column_name = 'delivery_contact_email'
);
SET @sql := IF(@has_delivery_contact_email = 0,
  'ALTER TABLE purchase_order ADD COLUMN delivery_contact_email VARCHAR(180) NULL COMMENT ''Delivery contact email'' AFTER delivery_contact_phone',
  'SELECT ''purchase_order.delivery_contact_email already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
