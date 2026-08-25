-- 商家可维护的商品展示标签（热卖、上新、特卖等）。
SET @product_tags_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shop_sku'
      AND column_name = 'product_tags'
);

SET @product_tags_sql := IF(
    @product_tags_column_exists = 0,
    'ALTER TABLE shop_sku ADD COLUMN product_tags JSON NULL AFTER product_description',
    'SELECT 1'
);

PREPARE product_tags_statement FROM @product_tags_sql;
EXECUTE product_tags_statement;
DEALLOCATE PREPARE product_tags_statement;
