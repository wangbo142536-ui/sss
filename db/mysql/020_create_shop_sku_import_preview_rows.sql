-- Shop SKU import preview rows.
-- Idempotent migration: stores preview rows before confirm-import writes shop_sku.

CREATE TABLE IF NOT EXISTS shop_sku_import_preview_row (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Preview row id',
  batch_id BIGINT NOT NULL COMMENT 'Import batch id',
  company_id BIGINT NOT NULL COMMENT 'Owner company id',
  shop_id BIGINT NULL COMMENT 'Shop id',
  row_no INT NOT NULL COMMENT 'Source row number',
  supplier_sku_code VARCHAR(120) NULL COMMENT 'Supplier SKU code',
  product_name VARCHAR(500) NULL COMMENT 'Product name',
  product_type VARCHAR(30) NOT NULL DEFAULT 'MATERIAL' COMMENT 'MATERIAL, FOOD',
  platform_code VARCHAR(100) NULL COMMENT 'Platform standard code',
  impa_code VARCHAR(80) NULL COMMENT 'Matched IMPA code',
  category_code VARCHAR(50) NULL COMMENT 'Category code',
  category_name VARCHAR(255) NULL COMMENT 'Category name',
  specification_summary VARCHAR(1000) NULL COMMENT 'Specification summary',
  stock_qty DECIMAL(18,4) NULL COMMENT 'Stock quantity',
  stock_unit VARCHAR(80) NULL COMMENT 'Stock unit',
  unit_price DECIMAL(18,4) NULL COMMENT 'Unit price',
  currency VARCHAR(20) NOT NULL DEFAULT 'CNY' COMMENT 'Currency',
  packing VARCHAR(300) NULL COMMENT 'Packing',
  barcode VARCHAR(120) NULL COMMENT 'Barcode',
  code_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_EXCEPTION' COMMENT 'CODE_MATCHED, SPEC_MATCHED, PENDING_EXCEPTION',
  exception_reason VARCHAR(500) NULL COMMENT 'Exception reason',
  raw_row_json JSON NULL COMMENT 'Raw import row JSON',
  candidate_snapshot_json JSON NULL COMMENT 'Candidate snapshot JSON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_shop_preview_batch_row (batch_id, row_no),
  KEY idx_shop_preview_company_batch (company_id, batch_id),
  KEY idx_shop_preview_status (code_status),
  CONSTRAINT fk_shop_preview_batch
    FOREIGN KEY (batch_id) REFERENCES shop_sku_import_batch (id) ON DELETE CASCADE,
  CONSTRAINT fk_shop_preview_company
    FOREIGN KEY (company_id) REFERENCES company (id),
  CONSTRAINT fk_shop_preview_shop
    FOREIGN KEY (shop_id) REFERENCES shop_store (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Shop SKU import preview rows';
