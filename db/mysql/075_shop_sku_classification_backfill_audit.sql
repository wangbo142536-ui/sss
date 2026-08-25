-- Audit trail for the 2026-08-19 unclassified SKU category backfill.
-- Exact IMPA linkage and broad category assignment are intentionally tracked separately.

CREATE TABLE IF NOT EXISTS shop_sku_classification_backfill_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  run_id VARCHAR(80) NOT NULL,
  sku_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  old_standard_category_code VARCHAR(80) NULL,
  old_category_code VARCHAR(80) NULL,
  old_category_name VARCHAR(200) NULL,
  old_impa_item_id BIGINT NULL,
  old_impa_code VARCHAR(80) NULL,
  old_platform_code VARCHAR(80) NULL,
  old_code_status VARCHAR(40) NULL,
  old_exception_reason VARCHAR(200) NULL,
  new_standard_category_code VARCHAR(80) NULL,
  new_category_code VARCHAR(80) NULL,
  new_category_name VARCHAR(200) NULL,
  new_impa_item_id BIGINT NULL,
  new_impa_code VARCHAR(80) NULL,
  decision_method VARCHAR(80) NOT NULL,
  confidence_level VARCHAR(20) NOT NULL,
  evidence VARCHAR(2000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sku_classification_backfill_run (run_id, sku_id),
  KEY idx_sku_classification_backfill_sku (sku_id),
  KEY idx_sku_classification_backfill_company (company_id),
  KEY idx_sku_classification_backfill_confidence (run_id, confidence_level),
  CONSTRAINT fk_sku_classification_backfill_sku FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE CASCADE,
  CONSTRAINT fk_sku_classification_backfill_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU category backfill before/after audit';
