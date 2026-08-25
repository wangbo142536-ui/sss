-- 新建物料比价策略默认选择 3 家最低混供供货商。
-- 仅修改后续新记录的默认值，不覆盖用户已经保存的 2 家或自定义数量。

ALTER TABLE material_comparison_strategy_setting
  ALTER COLUMN mixed_supplier_count SET DEFAULT 3;
