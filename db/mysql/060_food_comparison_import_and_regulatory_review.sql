CREATE TABLE IF NOT EXISTS food_comparison_quote_override (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  quote_item_id BIGINT NOT NULL,
  quoted_quantity DECIMAL(18,4) NOT NULL,
  unit_price DECIMAL(18,4) NOT NULL,
  supplier_remark VARCHAR(2000) NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_comparison_quote_item (demand_id, quote_item_id),
  KEY idx_food_comparison_override_demand (demand_id, updated_at),
  CONSTRAINT fk_food_comparison_override_demand FOREIGN KEY (demand_id) REFERENCES food_demand(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_comparison_override_quote_item FOREIGN KEY (quote_item_id) REFERENCES food_supplier_quote_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_comparison_override_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @schema_name := DATABASE();

SET @has_food_evaluation_reviewer := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'food_evaluation' AND column_name = 'reviewer_id'
);
SET @sql := IF(@has_food_evaluation_reviewer = 0,
  'ALTER TABLE food_evaluation ADD COLUMN reviewer_id BIGINT NULL AFTER review_remark',
  'SELECT ''food_evaluation.reviewer_id already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_food_evaluation_reviewer_fk := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE table_schema = @schema_name AND table_name = 'food_evaluation'
    AND constraint_name = 'fk_food_evaluation_reviewer' AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@has_food_evaluation_reviewer_fk = 0,
  'ALTER TABLE food_evaluation ADD CONSTRAINT fk_food_evaluation_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)',
  'SELECT ''food_evaluation reviewer foreign key already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
