ALTER TABLE food_purchase_order
  ADD COLUMN delivery_address VARCHAR(500) NULL AFTER required_delivery_time;

ALTER TABLE food_settlement
  ADD COLUMN actual_amount DECIMAL(18,4) NULL AFTER amount,
  ADD COLUMN invoice_attachments_json JSON NULL AFTER invoice_file_id;

INSERT IGNORE INTO food_settlement
  (order_id, supplier_order_id, buyer_company_id, supplier_company_id, status, amount)
SELECT o.id, os.id, o.buyer_company_id, os.supplier_company_id,
       'PENDING_INVOICE', os.subtotal_amount
FROM food_purchase_order_supplier os
JOIN food_purchase_order o ON o.id = os.order_id
WHERE os.status = 'SUPPLIED';
