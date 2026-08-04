CREATE TABLE IF NOT EXISTS company_supplier_service_type (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '企业供应能力ID',
  company_id BIGINT NOT NULL COMMENT '企业ID',
  service_type VARCHAR(30) NOT NULL COMMENT '供应能力：MATERIAL/FOOD',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_supplier_service_type (company_id, service_type),
  KEY idx_supplier_service_type (service_type),
  CONSTRAINT fk_supplier_service_type_company
    FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应服务商业务能力';

-- 历史供应商不在迁移中静默归类。没有能力记录的历史企业继续沿用原菜单范围，
-- 待平台或企业管理员确认 MATERIAL/FOOD 后再写入本表并启用能力隔离。
