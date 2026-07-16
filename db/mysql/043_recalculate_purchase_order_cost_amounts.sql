-- Recalculate purchase order cost amounts from supplier unit prices.
-- Quotation amounts remain stored in purchase_order_item.actual_quote_price.

UPDATE purchase_order_item
SET amount = ROUND(COALESCE(unit_price, 0) * COALESCE(pricing_quantity, 0), 4),
    amount_usd = CASE
      WHEN unit_price_usd IS NOT NULL THEN ROUND(unit_price_usd * COALESCE(pricing_quantity, 0), 4)
      ELSE ROUND(COALESCE(unit_price, 0) * COALESCE(pricing_quantity, 0) / 7, 4)
    END;

UPDATE purchase_order_supplier supplier
JOIN (
  SELECT
    supplier_order_id,
    ROUND(COALESCE(SUM(amount), 0), 4) AS cost_amount,
    ROUND(COALESCE(SUM(amount_usd), 0), 4) AS cost_amount_usd
  FROM purchase_order_item
  GROUP BY supplier_order_id
) item_sum
  ON item_sum.supplier_order_id = supplier.id
SET supplier.subtotal_amount = item_sum.cost_amount,
    supplier.subtotal_amount_usd = item_sum.cost_amount_usd,
    supplier.final_amount = GREATEST(item_sum.cost_amount - COALESCE(supplier.discount_amount, 0), 0),
    supplier.final_amount_usd = item_sum.cost_amount_usd,
    supplier.updated_at = CURRENT_TIMESTAMP;

UPDATE purchase_order order_head
JOIN (
  SELECT
    order_id,
    ROUND(COALESCE(SUM(final_amount), 0), 4) AS cost_amount,
    ROUND(COALESCE(SUM(final_amount_usd), 0), 4) AS cost_amount_usd
  FROM purchase_order_supplier
  GROUP BY order_id
) supplier_sum
  ON supplier_sum.order_id = order_head.id
SET order_head.total_amount = supplier_sum.cost_amount,
    order_head.total_amount_usd = supplier_sum.cost_amount_usd,
    order_head.updated_at = CURRENT_TIMESTAMP;
