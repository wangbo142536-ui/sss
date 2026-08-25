-- Persist an optional product introduction for company-managed SKUs.
-- Idempotent migration. Existing SKU rows and identifiers are preserved.

SET @schema_name := DATABASE();

SET @has_product_description := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku'
    AND column_name = 'product_description'
);

SET @sql := IF(
  @has_product_description = 0,
  'ALTER TABLE shop_sku ADD COLUMN product_description TEXT NULL AFTER product_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
