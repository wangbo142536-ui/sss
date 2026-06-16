-- Shop management and supplier SKU tables.
-- Idempotent migration: creates new shop_* tables only.
-- It does not clear or modify existing supplier_sku, sys_role_permission, sys_user_menu, or business data.

CREATE TABLE IF NOT EXISTS shop_store (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Shop id',
  company_id BIGINT NOT NULL COMMENT 'Owner company id',
  logo_file_id VARCHAR(80) NULL COMMENT 'Logo file business id',
  logo_url VARCHAR(500) NULL COMMENT 'Logo URL',
  shop_name VARCHAR(200) NULL COMMENT 'Shop display name',
  introduction TEXT NULL COMMENT 'Shop introduction',
  main_categories VARCHAR(1000) NULL COMMENT 'Main category names/codes, comma separated',
  delivery_areas VARCHAR(1000) NULL COMMENT 'Delivery areas, comma separated',
  service_ports VARCHAR(1000) NULL COMMENT 'Service ports, comma separated',
  contact_name VARCHAR(100) NULL COMMENT 'Shop contact name',
  contact_phone VARCHAR(50) NULL COMMENT 'Shop contact phone',
  contact_email VARCHAR(160) NULL COMMENT 'Shop contact email',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, DISABLED, PENDING_REVIEW',
  created_by BIGINT NULL COMMENT 'Creator user id',
  updated_by BIGINT NULL COMMENT 'Last updater user id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_store_company (company_id),
  KEY idx_shop_store_status (status),
  KEY idx_shop_store_created_by (created_by),
  CONSTRAINT fk_shop_store_company
    FOREIGN KEY (company_id) REFERENCES company (id),
  CONSTRAINT fk_shop_store_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_store_updated_by
    FOREIGN KEY (updated_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Supplier shop profile';

CREATE TABLE IF NOT EXISTS shop_sku_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Import batch id',
  company_id BIGINT NOT NULL COMMENT 'Owner company id',
  shop_id BIGINT NULL COMMENT 'Shop id',
  file_id VARCHAR(80) NULL COMMENT 'Uploaded file business id',
  source_file_name VARCHAR(255) NOT NULL COMMENT 'Source file name',
  import_type VARCHAR(40) NOT NULL DEFAULT 'SUPPLIER_SKU' COMMENT 'Import type',
  status VARCHAR(30) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED, PARSING, PREVIEWED, CONFIRMED, PARTIAL_SUCCESS, FAILED, CANCELLED',
  total_count INT NOT NULL DEFAULT 0 COMMENT 'Total row count',
  success_count INT NOT NULL DEFAULT 0 COMMENT 'Success row count',
  exception_count INT NOT NULL DEFAULT 0 COMMENT 'Exception row count',
  created_by BIGINT NULL COMMENT 'Operator user id',
  started_at DATETIME NULL COMMENT 'Started at',
  finished_at DATETIME NULL COMMENT 'Finished at',
  error_message VARCHAR(1000) NULL COMMENT 'Batch error message',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_shop_import_company_status (company_id, status),
  KEY idx_shop_import_shop (shop_id),
  KEY idx_shop_import_created_by (created_by),
  CONSTRAINT fk_shop_import_company
    FOREIGN KEY (company_id) REFERENCES company (id),
  CONSTRAINT fk_shop_import_shop
    FOREIGN KEY (shop_id) REFERENCES shop_store (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_import_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Shop SKU import batch';

CREATE TABLE IF NOT EXISTS shop_sku (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU id',
  company_id BIGINT NOT NULL COMMENT 'Owner company id',
  shop_id BIGINT NOT NULL COMMENT 'Shop id',
  product_type VARCHAR(30) NOT NULL COMMENT 'MATERIAL, FOOD',
  category_code VARCHAR(50) NULL COMMENT 'Category code, reserved for IMPA category',
  category_name VARCHAR(255) NULL COMMENT 'Category name',
  platform_code VARCHAR(100) NULL COMMENT 'Platform standard code',
  impa_code VARCHAR(80) NULL COMMENT 'Matched IMPA code',
  supplier_sku_code VARCHAR(120) NULL COMMENT 'Supplier SKU code',
  product_name VARCHAR(500) NOT NULL COMMENT 'Product name',
  specification_summary VARCHAR(1000) NULL COMMENT 'Specification summary',
  normalized_name VARCHAR(1000) NULL COMMENT 'Normalized name for matching',
  normalized_specification VARCHAR(1000) NULL COMMENT 'Normalized specification for matching',
  stock_qty DECIMAL(18,4) NULL COMMENT 'Stock quantity',
  stock_unit VARCHAR(80) NULL COMMENT 'Stock unit',
  lead_time_days INT NULL COMMENT 'Lead time in days',
  delivery_area VARCHAR(500) NULL COMMENT 'Delivery area',
  service_ports VARCHAR(1000) NULL COMMENT 'Service ports, comma separated',
  monthly_sales INT NOT NULL DEFAULT 0 COMMENT 'Monthly sales',
  unit_price DECIMAL(18,4) NULL COMMENT 'Unit price',
  currency VARCHAR(20) NOT NULL DEFAULT 'CNY' COMMENT 'Currency',
  brand VARCHAR(200) NULL COMMENT 'Brand',
  unit VARCHAR(80) NULL COMMENT 'Unit',
  packing VARCHAR(300) NULL COMMENT 'Packing',
  barcode VARCHAR(120) NULL COMMENT 'Barcode',
  shelf_status VARCHAR(30) NOT NULL DEFAULT 'OFF_SHELF' COMMENT 'ON_SHELF, OFF_SHELF, DISABLED',
  code_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_EXCEPTION' COMMENT 'CODE_MATCHED, SPEC_MATCHED, PENDING_EXCEPTION, CONFIRMED, IGNORED',
  exception_reason VARCHAR(500) NULL COMMENT 'Exception reason',
  import_batch_id BIGINT NULL COMMENT 'Import batch id',
  import_row_no INT NULL COMMENT 'Import source row number',
  raw_row_json JSON NULL COMMENT 'Raw import row JSON',
  candidate_snapshot_json JSON NULL COMMENT 'Candidate snapshot JSON',
  created_by BIGINT NULL COMMENT 'Creator user id',
  updated_by BIGINT NULL COMMENT 'Last updater user id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_sku_company_supplier_code (company_id, supplier_sku_code),
  KEY idx_shop_sku_shop_status (shop_id, shelf_status),
  KEY idx_shop_sku_company_code_status (company_id, code_status),
  KEY idx_shop_sku_company_type (company_id, product_type),
  KEY idx_shop_sku_impa (impa_code),
  KEY idx_shop_sku_platform_code (platform_code),
  KEY idx_shop_sku_category (category_code),
  KEY idx_shop_sku_import_batch (import_batch_id),
  KEY idx_shop_sku_name (product_name(191)),
  CONSTRAINT fk_shop_sku_company
    FOREIGN KEY (company_id) REFERENCES company (id),
  CONSTRAINT fk_shop_sku_shop
    FOREIGN KEY (shop_id) REFERENCES shop_store (id) ON DELETE CASCADE,
  CONSTRAINT fk_shop_sku_import_batch
    FOREIGN KEY (import_batch_id) REFERENCES shop_sku_import_batch (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_sku_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_sku_updated_by
    FOREIGN KEY (updated_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Shop supplier SKU';

CREATE TABLE IF NOT EXISTS shop_sku_attribute (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Attribute id',
  sku_id BIGINT NOT NULL COMMENT 'SKU id',
  attribute_key VARCHAR(100) NULL COMMENT 'Attribute key',
  attribute_name VARCHAR(200) NOT NULL COMMENT 'Attribute display name',
  attribute_value VARCHAR(1000) NULL COMMENT 'Attribute value',
  attribute_unit VARCHAR(80) NULL COMMENT 'Attribute unit',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  raw_text VARCHAR(1000) NULL COMMENT 'Raw attribute text',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_shop_sku_attr_sku_sort (sku_id, sort_order),
  KEY idx_shop_sku_attr_key (attribute_key),
  CONSTRAINT fk_shop_sku_attr_sku
    FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Shop SKU attributes';

CREATE TABLE IF NOT EXISTS shop_sku_image (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Image id',
  sku_id BIGINT NOT NULL COMMENT 'SKU id',
  file_id VARCHAR(80) NULL COMMENT 'File business id',
  image_url VARCHAR(500) NULL COMMENT 'Image URL',
  thumbnail_url VARCHAR(500) NULL COMMENT 'Thumbnail URL',
  is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether primary image',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_shop_sku_image_sku_sort (sku_id, sort_order),
  KEY idx_shop_sku_image_file (file_id),
  CONSTRAINT fk_shop_sku_image_sku
    FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Shop SKU images';

CREATE TABLE IF NOT EXISTS shop_sku_exception_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Exception log id',
  company_id BIGINT NOT NULL COMMENT 'Owner company id',
  shop_id BIGINT NULL COMMENT 'Shop id',
  sku_id BIGINT NULL COMMENT 'SKU id',
  import_batch_id BIGINT NULL COMMENT 'Import batch id',
  action_type VARCHAR(40) NOT NULL COMMENT 'SELECT_CANDIDATE, MANUAL_CODE, IGNORE, REOPEN',
  before_code_status VARCHAR(40) NULL COMMENT 'Previous code status',
  after_code_status VARCHAR(40) NULL COMMENT 'New code status',
  before_platform_code VARCHAR(100) NULL COMMENT 'Previous platform code',
  after_platform_code VARCHAR(100) NULL COMMENT 'New platform code',
  before_impa_code VARCHAR(80) NULL COMMENT 'Previous IMPA code',
  after_impa_code VARCHAR(80) NULL COMMENT 'New IMPA code',
  selected_candidate_json JSON NULL COMMENT 'Selected candidate snapshot JSON',
  reason VARCHAR(500) NULL COMMENT 'Handle reason',
  handled_by BIGINT NULL COMMENT 'Handler user id',
  handled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Handled at',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  KEY idx_shop_exception_company_time (company_id, handled_at),
  KEY idx_shop_exception_sku (sku_id),
  KEY idx_shop_exception_batch (import_batch_id),
  KEY idx_shop_exception_handler (handled_by),
  CONSTRAINT fk_shop_exception_company
    FOREIGN KEY (company_id) REFERENCES company (id),
  CONSTRAINT fk_shop_exception_shop
    FOREIGN KEY (shop_id) REFERENCES shop_store (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_exception_sku
    FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_exception_batch
    FOREIGN KEY (import_batch_id) REFERENCES shop_sku_import_batch (id) ON DELETE SET NULL,
  CONSTRAINT fk_shop_exception_handler
    FOREIGN KEY (handled_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Shop SKU exception handling log';
