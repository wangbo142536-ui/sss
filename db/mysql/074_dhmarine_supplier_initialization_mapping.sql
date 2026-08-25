-- Stable source mapping for the approved DH Marine supplier/product initialization.
-- Business primary keys remain platform-generated; external identifiers are retained only for traceability.

CREATE TABLE IF NOT EXISTS external_supplier_company_map (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mapping id',
  source_system VARCHAR(40) NOT NULL COMMENT 'External source system',
  source_company_code VARCHAR(40) NOT NULL COMMENT 'Normalized source company code',
  source_shop_code VARCHAR(40) NOT NULL COMMENT 'Normalized source shop code',
  source_shop_id VARCHAR(80) NOT NULL COMMENT 'Original source shop id',
  company_id BIGINT NOT NULL COMMENT 'Platform company id',
  shop_id BIGINT NOT NULL COMMENT 'Platform shop id',
  owner_user_id BIGINT NOT NULL COMMENT 'Generated company owner user id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_external_supplier_company_source (source_system, source_company_code),
  UNIQUE KEY uk_external_supplier_company_shop (source_system, source_shop_id),
  UNIQUE KEY uk_external_supplier_company_target (source_system, company_id),
  KEY idx_external_supplier_company_shop_id (shop_id),
  KEY idx_external_supplier_company_owner (owner_user_id),
  CONSTRAINT fk_external_supplier_company_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE,
  CONSTRAINT fk_external_supplier_company_shop FOREIGN KEY (shop_id) REFERENCES shop_store (id) ON DELETE CASCADE,
  CONSTRAINT fk_external_supplier_company_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='External supplier company source mapping';

CREATE TABLE IF NOT EXISTS external_supplier_product_map (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mapping id',
  source_system VARCHAR(40) NOT NULL COMMENT 'External source system',
  source_product_code VARCHAR(40) NOT NULL COMMENT 'Normalized source product code',
  source_product_id VARCHAR(80) NOT NULL COMMENT 'Original source product id',
  source_supplier_sku VARCHAR(160) NULL COMMENT 'Original supplier SKU',
  source_company_code VARCHAR(40) NOT NULL COMMENT 'Normalized source company code',
  company_id BIGINT NOT NULL COMMENT 'Platform company id',
  sku_id BIGINT NOT NULL COMMENT 'Platform SKU id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_external_supplier_product_source (source_system, source_product_code),
  UNIQUE KEY uk_external_supplier_product_target (source_system, sku_id),
  KEY idx_external_supplier_product_company (company_id),
  KEY idx_external_supplier_product_company_code (source_system, source_company_code),
  CONSTRAINT fk_external_supplier_product_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE,
  CONSTRAINT fk_external_supplier_product_sku FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='External supplier product source mapping';
