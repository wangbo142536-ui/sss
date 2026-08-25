-- 物料比价策略设置。
-- 商品质量与价格倾向只读取商家维护的 shop_sku.product_tags，不生成系统商品标签。

CREATE TABLE IF NOT EXISTS material_comparison_strategy_setting (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  demand_id BIGINT NOT NULL,
  mixed_supplier_count INT NOT NULL DEFAULT 2,
  price_enabled TINYINT(1) NOT NULL DEFAULT 1,
  price_level TINYINT NOT NULL DEFAULT 5,
  quality_enabled TINYINT(1) NOT NULL DEFAULT 1,
  quality_level TINYINT NOT NULL DEFAULT 3,
  core_item_ids_json JSON NULL,
  strategy_version INT NOT NULL DEFAULT 1,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_comparison_strategy_demand (company_id, demand_id),
  KEY idx_material_comparison_strategy_demand (demand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- product_tags 已由 073_shop_sku_product_tags.sql 建立；本迁移不修改任何商家标签。
SELECT COUNT(product_tags) FROM shop_sku WHERE 1 = 0;
