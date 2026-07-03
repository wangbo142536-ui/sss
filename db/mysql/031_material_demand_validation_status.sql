ALTER TABLE material_demand_item
    ADD COLUMN validation_status VARCHAR(30) NULL COMMENT '需求导入用户口径校验状态：MATCHED/ABNORMAL' AFTER reason,
    ADD COLUMN validation_reason VARCHAR(80) NULL COMMENT '需求导入用户口径校验原因' AFTER validation_status;

ALTER TABLE purchase_order_item
    ADD COLUMN actual_quote_price DECIMAL(18,4) NULL COMMENT '船代实际报价单价' AFTER amount_usd,
    ADD COLUMN actual_quote_currency VARCHAR(20) NULL COMMENT '船代实际报价币种' AFTER actual_quote_price,
    ADD COLUMN quote_markup_percent DECIMAL(10,4) NULL COMMENT '报价利润百分比' AFTER actual_quote_currency,
    ADD COLUMN quote_profit_amount DECIMAL(18,4) NULL COMMENT '行利润金额' AFTER quote_markup_percent;
