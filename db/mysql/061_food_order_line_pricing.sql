ALTER TABLE food_purchase_order_item
  ADD COLUMN quoted_unit_price DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER unit_price,
  ADD COLUMN quoted_amount DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER amount;

UPDATE food_purchase_order_item
SET quoted_unit_price = unit_price,
    quoted_amount = amount
WHERE quoted_unit_price = 0 AND quoted_amount = 0;
