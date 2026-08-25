-- 企业介绍与平台“海事严选”产品 QC 记录。
-- 企业介绍属于企业资料，不与店铺介绍混用；严选记录由平台管理员维护。

SET @has_company_introduction := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'company'
    AND COLUMN_NAME = 'company_introduction'
);
SET @company_introduction_sql := IF(
  @has_company_introduction = 0,
  'ALTER TABLE company ADD COLUMN company_introduction VARCHAR(2000) NULL COMMENT ''企业介绍'' AFTER logo_url',
  'SELECT 1'
);
PREPARE company_introduction_statement FROM @company_introduction_sql;
EXECUTE company_introduction_statement;
DEALLOCATE PREPARE company_introduction_statement;

CREATE TABLE IF NOT EXISTS shop_sku_quality_selection (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台严选记录ID',
  company_id BIGINT NOT NULL COMMENT '供货商企业ID',
  sku_id BIGINT NOT NULL COMMENT '商品SKU ID',
  inspection_time DATETIME NOT NULL COMMENT 'QC检查时间',
  inspection_content TEXT NOT NULL COMMENT '检查内容',
  inspection_process TEXT NULL COMMENT '本次检查过程说明',
  inspection_report_file_id VARCHAR(80) NULL COMMENT '检测报告文件ID',
  inspection_report_file_name VARCHAR(255) NULL COMMENT '检测报告文件名',
  inspection_conclusion VARCHAR(1000) NOT NULL COMMENT '检测结论',
  audit_trail_json JSON NOT NULL COMMENT '历次检查及修改留痕',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '严选状态',
  created_by BIGINT NOT NULL COMMENT '创建人用户ID',
  updated_by BIGINT NOT NULL COMMENT '更新人用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_sku_quality_selection_sku (sku_id),
  KEY idx_shop_sku_quality_selection_company_status (company_id, status),
  KEY idx_shop_sku_quality_selection_report (inspection_report_file_id),
  CONSTRAINT fk_shop_sku_quality_selection_company
    FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE,
  CONSTRAINT fk_shop_sku_quality_selection_sku
    FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE CASCADE,
  CONSTRAINT fk_shop_sku_quality_selection_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (id),
  CONSTRAINT fk_shop_sku_quality_selection_updated_by
    FOREIGN KEY (updated_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台海事严选商品QC记录';
