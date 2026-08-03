ALTER TABLE food_demand
  ADD COLUMN quote_markup_percent DECIMAL(8,4) NOT NULL DEFAULT 10 AFTER pending_count,
  ADD COLUMN fixed_freight_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER quote_markup_percent,
  ADD COLUMN fixed_customs_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER fixed_freight_fee,
  ADD COLUMN fixed_crane_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER fixed_customs_fee,
  ADD COLUMN fixed_other_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER fixed_crane_fee,
  ADD COLUMN supply_mode VARCHAR(20) NOT NULL DEFAULT 'SEA' AFTER fixed_other_fee,
  ADD COLUMN fixed_provider_type VARCHAR(20) NULL AFTER supply_mode,
  ADD COLUMN fixed_provider_id VARCHAR(80) NULL AFTER fixed_provider_type,
  ADD COLUMN fixed_provider_name VARCHAR(255) NULL AFTER fixed_provider_id,
  ADD COLUMN traffic_service_json JSON NULL AFTER fixed_provider_name;

ALTER TABLE food_purchase_order
  ADD COLUMN cost_amount DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER total_amount,
  ADD COLUMN quoted_amount DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER cost_amount,
  ADD COLUMN profit_amount DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER quoted_amount,
  ADD COLUMN quote_markup_percent DECIMAL(8,4) NOT NULL DEFAULT 10 AFTER profit_amount,
  ADD COLUMN fixed_freight_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER quote_markup_percent,
  ADD COLUMN fixed_customs_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER fixed_freight_fee,
  ADD COLUMN fixed_crane_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER fixed_customs_fee,
  ADD COLUMN fixed_other_fee DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER fixed_crane_fee,
  ADD COLUMN supply_mode VARCHAR(20) NOT NULL DEFAULT 'SEA' AFTER fixed_other_fee,
  ADD COLUMN fixed_provider_type VARCHAR(20) NULL AFTER supply_mode,
  ADD COLUMN fixed_provider_id VARCHAR(80) NULL AFTER fixed_provider_type,
  ADD COLUMN fixed_provider_name VARCHAR(255) NULL AFTER fixed_provider_id,
  ADD COLUMN traffic_service_json JSON NULL AFTER fixed_provider_name;

UPDATE food_purchase_order
SET cost_amount = total_amount,
    quoted_amount = total_amount,
    profit_amount = 0
WHERE cost_amount = 0 AND quoted_amount = 0 AND total_amount <> 0;
