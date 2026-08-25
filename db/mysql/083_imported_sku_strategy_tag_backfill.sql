-- Operator-approved initialization of product strategy tags for the existing imported SKU snapshot.
-- The two independent salted rankings avoid concentrating tags in one supplier or import sequence.
-- Existing non-strategy merchant tags are preserved. Future imports are not tagged automatically.

DROP TEMPORARY TABLE IF EXISTS tmp_price_low_sku;
CREATE TEMPORARY TABLE tmp_price_low_sku (
  id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB AS
SELECT id
FROM (
  SELECT id,
         ROW_NUMBER() OVER (ORDER BY CRC32(CONCAT('PRICE_LOW:', id)), id) AS stable_rank,
         COUNT(*) OVER () AS total_count
  FROM shop_sku
  WHERE import_batch_id IS NOT NULL
     OR import_row_no IS NOT NULL
     OR raw_row_json IS NOT NULL
) ranked
WHERE stable_rank <= ROUND(total_count * 0.80, 0);

UPDATE shop_sku sku
JOIN tmp_price_low_sku selected ON selected.id = sku.id
SET sku.product_tags = JSON_ARRAY_APPEND(
      COALESCE(sku.product_tags, JSON_ARRAY()),
      '$',
      '价格低'
    )
WHERE NOT JSON_CONTAINS(
  COALESCE(sku.product_tags, JSON_ARRAY()),
  JSON_QUOTE('价格低')
);

DROP TEMPORARY TABLE IF EXISTS tmp_quality_high_sku;
CREATE TEMPORARY TABLE tmp_quality_high_sku (
  id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB AS
SELECT id
FROM (
  SELECT id,
         ROW_NUMBER() OVER (ORDER BY CRC32(CONCAT('QUALITY_HIGH:', id)), id) AS stable_rank,
         COUNT(*) OVER () AS total_count
  FROM shop_sku
  WHERE import_batch_id IS NOT NULL
     OR import_row_no IS NOT NULL
     OR raw_row_json IS NOT NULL
) ranked
WHERE stable_rank <= ROUND(total_count * 0.70, 0);

UPDATE shop_sku sku
JOIN tmp_quality_high_sku selected ON selected.id = sku.id
SET sku.product_tags = JSON_ARRAY_APPEND(
      COALESCE(sku.product_tags, JSON_ARRAY()),
      '$',
      '质量高'
    )
WHERE NOT JSON_CONTAINS(
  COALESCE(sku.product_tags, JSON_ARRAY()),
  JSON_QUOTE('质量高')
);

DROP TEMPORARY TABLE IF EXISTS tmp_price_low_sku;
DROP TEMPORARY TABLE IF EXISTS tmp_quality_high_sku;
