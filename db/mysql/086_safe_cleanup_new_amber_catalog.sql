-- Safely normalize the NEW AMBER catalog for supplier companies A/E/F.
-- The migration preserves every order-referenced SKU and creates recoverable backups before mutation.

CREATE TABLE IF NOT EXISTS shop_sku_safe_backup_20260824 LIKE shop_sku;
CREATE TABLE IF NOT EXISTS shop_sku_unit_price_safe_backup_20260824 LIKE shop_sku_unit_price;
CREATE TABLE IF NOT EXISTS shop_sku_attribute_safe_backup_20260824 LIKE shop_sku_attribute;
CREATE TABLE IF NOT EXISTS shop_sku_image_safe_backup_20260824 LIKE shop_sku_image;
CREATE TABLE IF NOT EXISTS shop_sku_exception_log_safe_backup_20260824 LIKE shop_sku_exception_log;
CREATE TABLE IF NOT EXISTS shop_sku_import_item_safe_backup_20260824 LIKE shop_sku_import_item;
CREATE TABLE IF NOT EXISTS external_supplier_product_map_safe_backup_20260824 LIKE external_supplier_product_map;
CREATE TABLE IF NOT EXISTS shop_sku_classification_backfill_audit_safe_backup_20260824 LIKE shop_sku_classification_backfill_audit;
CREATE TABLE IF NOT EXISTS shop_sku_quality_selection_safe_backup_20260824 LIKE shop_sku_quality_selection;

INSERT IGNORE INTO shop_sku_safe_backup_20260824
SELECT s.*
FROM shop_sku s
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_unit_price_safe_backup_20260824
SELECT child.*
FROM shop_sku_unit_price child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_attribute_safe_backup_20260824
SELECT child.*
FROM shop_sku_attribute child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_image_safe_backup_20260824
SELECT child.*
FROM shop_sku_image child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_exception_log_safe_backup_20260824
SELECT child.*
FROM shop_sku_exception_log child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_import_item_safe_backup_20260824
SELECT child.*
FROM shop_sku_import_item child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO external_supplier_product_map_safe_backup_20260824
SELECT child.*
FROM external_supplier_product_map child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_classification_backfill_audit_safe_backup_20260824
SELECT child.*
FROM shop_sku_classification_backfill_audit child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

INSERT IGNORE INTO shop_sku_quality_selection_safe_backup_20260824
SELECT child.*
FROM shop_sku_quality_selection child
JOIN shop_sku s ON s.id = child.sku_id
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL';

START TRANSACTION;

INSERT IGNORE INTO shop_sku_classification_backfill_audit (
  run_id, sku_id, company_id,
  old_standard_category_code, old_category_code, old_category_name,
  old_impa_item_id, old_impa_code, old_platform_code, old_code_status, old_exception_reason,
  new_standard_category_code, new_category_code, new_category_name,
  new_impa_item_id, new_impa_code, decision_method, confidence_level, evidence
)
SELECT 'SAFE_NEW_AMBER_20260824',
       s.id,
       s.company_id,
       s.category_code,
       s.category_code,
       s.category_name,
       ii.id,
       s.impa_code,
       s.platform_code,
       s.code_status,
       s.exception_reason,
       category.category_code,
       category.category_code,
       category.category_name_cn,
       ii.id,
       s.impa_code,
       CASE WHEN ii.id IS NULL THEN 'IMPA_PREFIX_STANDARD_CATEGORY' ELSE 'IMPA_EXACT_STANDARD_ITEM' END,
       CASE WHEN ii.id IS NULL THEN 'MEDIUM' ELSE 'HIGH' END,
       CASE
         WHEN ii.id IS NULL THEN '六位编码未命中当前IMPA明细库；仅按合法两位IMPA前缀赋予标准大类，不伪造精确标准品。'
         ELSE '六位编码命中当前IMPA明细库；分类与规格使用标准库字段。'
       END
FROM shop_sku s
JOIN impa_category category
  ON category.category_code = LEFT(s.impa_code, 2)
 AND category.enabled = 1
LEFT JOIN impa_item ii
  ON ii.impa_code = s.impa_code
 AND ii.enabled = 1
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
  AND s.supplier_sku_code LIKE 'NAMB-%';

UPDATE shop_sku s
JOIN impa_category category
  ON category.category_code = LEFT(s.impa_code, 2)
 AND category.enabled = 1
LEFT JOIN impa_item ii
  ON ii.impa_code = s.impa_code
 AND ii.enabled = 1
SET s.category_code = category.category_code,
    s.category_name = category.category_name_cn,
    s.specification_summary = NULLIF(TRIM(ii.specification_cn), ''),
    s.normalized_specification = CASE
      WHEN NULLIF(TRIM(ii.specification_cn), '') IS NULL THEN NULL
      ELSE UPPER(REPLACE(REPLACE(REPLACE(TRIM(ii.specification_cn), ' ', ''), CHAR(13), ''), CHAR(10), ''))
    END,
    s.packing = NULL,
    s.updated_at = CURRENT_TIMESTAMP
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
  AND s.supplier_sku_code LIKE 'NAMB-%';

UPDATE shop_sku_attribute attribute_row
JOIN shop_sku s ON s.id = attribute_row.sku_id
JOIN impa_item ii
  ON ii.impa_code = s.impa_code
 AND ii.enabled = 1
SET attribute_row.attribute_value = NULLIF(TRIM(ii.specification_cn), ''),
    attribute_row.raw_text = NULLIF(TRIM(ii.specification_cn), ''),
    attribute_row.updated_at = CURRENT_TIMESTAMP
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
  AND s.supplier_sku_code LIKE 'NAMB-%'
  AND attribute_row.attribute_key = 'specification'
  AND NULLIF(TRIM(ii.specification_cn), '') IS NOT NULL;

DELETE attribute_row
FROM shop_sku_attribute attribute_row
JOIN shop_sku s ON s.id = attribute_row.sku_id
LEFT JOIN impa_item ii
  ON ii.impa_code = s.impa_code
 AND ii.enabled = 1
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
  AND s.supplier_sku_code LIKE 'NAMB-%'
  AND attribute_row.attribute_key = 'specification'
  AND NULLIF(TRIM(ii.specification_cn), '') IS NULL;

UPDATE shop_sku s
SET s.shelf_status = 'OFF_SHELF',
    s.updated_at = CURRENT_TIMESTAMP
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
  AND COALESCE(s.supplier_sku_code, '') NOT LIKE 'NAMB-%'
  AND EXISTS (
    SELECT 1
    FROM purchase_order_item order_item
    WHERE order_item.sku_id = s.id
  );

DELETE s
FROM shop_sku s
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
  AND COALESCE(s.supplier_sku_code, '') NOT LIKE 'NAMB-%'
  AND NOT EXISTS (
    SELECT 1
    FROM purchase_order_item order_item
    WHERE order_item.sku_id = s.id
  );

COMMIT;

SELECT s.company_id,
       COUNT(*) AS retained_rows,
       SUM(s.shelf_status = 'ON_SHELF') AS visible_rows,
       SUM(s.supplier_sku_code LIKE 'NAMB-%') AS latest_rows,
       SUM(COALESCE(s.supplier_sku_code, '') NOT LIKE 'NAMB-%') AS archived_history_rows,
       COUNT(DISTINCT CASE WHEN s.shelf_status = 'ON_SHELF' THEN s.category_code END) AS visible_category_count
FROM shop_sku s
WHERE s.company_id IN (24, 27, 28)
  AND s.product_type = 'MATERIAL'
GROUP BY s.company_id
ORDER BY s.company_id;
