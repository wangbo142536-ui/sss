-- Purchase order customs document attachments.
-- Idempotent migration. Scope: supplier customs documents for purchase orders.

CREATE TABLE IF NOT EXISTS purchase_order_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Attachment id',
  order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  supplier_order_id BIGINT NOT NULL COMMENT 'Supplier order id',
  attachment_type VARCHAR(50) NOT NULL COMMENT 'Attachment type, e.g. CUSTOMS_DOCUMENT',
  file_id VARCHAR(80) NULL COMMENT 'Uploaded file id',
  file_name VARCHAR(255) NULL COMMENT 'Original file name',
  file_url VARCHAR(500) NULL COMMENT 'File URL',
  created_by BIGINT NULL COMMENT 'Creator user id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_purchase_order_attachment_order (order_id, supplier_order_id, attachment_type),
  CONSTRAINT fk_purchase_order_attachment_order
    FOREIGN KEY (order_id) REFERENCES purchase_order (id) ON DELETE CASCADE,
  CONSTRAINT fk_purchase_order_attachment_supplier_order
    FOREIGN KEY (supplier_order_id) REFERENCES purchase_order_supplier (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Purchase order attachments';
