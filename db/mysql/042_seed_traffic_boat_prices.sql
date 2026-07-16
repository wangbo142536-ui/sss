-- Seed default enabled traffic boat prices for the three E2E traffic suppliers.
-- This script is idempotent and only depends on sys_user.company_id and ANCHORAGE dictionary items.

INSERT INTO traffic_boat_price (
  supplier_company_id,
  anchorage_code,
  base_price,
  shared_price,
  enabled,
  remark
)
SELECT
  seeded.supplier_company_id,
  seeded.anchorage_code,
  seeded.base_price,
  ROUND(seeded.base_price * 0.8, 2) AS shared_price,
  1 AS enabled,
  'system default traffic boat price' AS remark
FROM (
  SELECT
    supplier.company_id AS supplier_company_id,
    anchorage.item_code AS anchorage_code,
    CAST(
      1000 + MOD(
        supplier.supplier_rank * 337 + anchorage.sort_order * 17 + anchorage.id * 13,
        3000
      ) AS DECIMAL(14, 2)
    ) AS base_price
  FROM (
    SELECT
      user.company_id,
      CASE user.username
        WHEN 'supplier_a_e2e' THEN 1
        WHEN 'supplier_b_e2e' THEN 2
        WHEN 'supplier_c_e2e' THEN 3
        ELSE 9
      END AS supplier_rank
    FROM sys_user user
    WHERE user.username IN ('supplier_a_e2e', 'supplier_b_e2e', 'supplier_c_e2e')
  ) supplier
  JOIN sys_dictionary_item anchorage
    ON anchorage.type_code = 'ANCHORAGE'
   AND anchorage.enabled = 1
) seeded
ON DUPLICATE KEY UPDATE
  base_price = VALUES(base_price),
  shared_price = VALUES(shared_price),
  enabled = 1,
  remark = VALUES(remark),
  updated_at = CURRENT_TIMESTAMP;
