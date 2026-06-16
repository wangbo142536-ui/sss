CREATE DATABASE IF NOT EXISTS ship_supply_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ship_supply_platform;

CREATE TABLE IF NOT EXISTS impa_category (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_code VARCHAR(20) NOT NULL COMMENT 'IMPA大类编码，例如11、21、79',
  category_name_cn VARCHAR(255) NOT NULL COMMENT '中文大类名称',
  category_name_en VARCHAR(255) NOT NULL COMMENT '英文大类名称',
  parent_code VARCHAR(20) NULL COMMENT '父级编码，一期为空，后续支持多级目录',
  level TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '目录层级',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '展示排序',
  item_count INT NOT NULL DEFAULT 0 COMMENT '该大类下标准物料数量',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_impa_category_code (category_code),
  KEY idx_impa_category_parent (parent_code),
  KEY idx_impa_category_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IMPA大类目录表';

CREATE TABLE IF NOT EXISTS impa_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  impa_code VARCHAR(20) NOT NULL COMMENT 'IMPA标准物料编码',
  category_code VARCHAR(20) NOT NULL COMMENT 'IMPA大类编码',
  default_unit_cn VARCHAR(100) NULL COMMENT '中文默认单位',
  default_unit_en VARCHAR(100) NULL COMMENT '英文默认单位',
  specification_cn VARCHAR(1000) NULL COMMENT '中文规格描述',
  specification_en VARCHAR(1000) NULL COMMENT '英文规格描述',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  source_file VARCHAR(255) NULL COMMENT '来源文件',
  source_row_no_cn INT NULL COMMENT '中文版来源行号',
  source_row_no_en INT NULL COMMENT '英文版来源行号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_impa_item_code (impa_code),
  KEY idx_impa_item_category (category_code),
  KEY idx_impa_item_enabled (enabled),
  CONSTRAINT fk_impa_item_category
    FOREIGN KEY (category_code) REFERENCES impa_category (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IMPA标准物料主表';

CREATE TABLE IF NOT EXISTS impa_item_i18n (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  impa_code VARCHAR(20) NOT NULL COMMENT 'IMPA标准物料编码',
  category_code VARCHAR(20) NOT NULL COMMENT 'IMPA大类编码，关联impa_category.category_code',
  language VARCHAR(20) NOT NULL COMMENT '语言，例如zh-CN、en-US',
  description VARCHAR(1000) NOT NULL COMMENT '物料名称，对应Excel的DESCRIPTION',
  specification VARCHAR(1000) NULL COMMENT '规格描述，对应Excel的SPECIFICATION',
  unit VARCHAR(100) NULL COMMENT '单位，对应Excel的UNIT',
  remark TEXT NULL COMMENT '备注，对应Excel的REMARK',
  source_sheet VARCHAR(100) NULL COMMENT '来源Sheet',
  source_row_no INT NULL COMMENT '来源行号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_impa_i18n_code_lang (impa_code, language),
  KEY idx_impa_i18n_category (category_code),
  KEY idx_impa_i18n_language (language),
  KEY idx_impa_i18n_description (description(191)),
  CONSTRAINT fk_impa_i18n_item
    FOREIGN KEY (impa_code) REFERENCES impa_item (impa_code)
    ON DELETE CASCADE,
  CONSTRAINT fk_impa_i18n_category
    FOREIGN KEY (category_code) REFERENCES impa_category (category_code)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IMPA物料多语言名称表';

CREATE TABLE IF NOT EXISTS impa_item_alias (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  impa_code VARCHAR(20) NOT NULL COMMENT 'IMPA标准物料编码',
  alias_name VARCHAR(1000) NOT NULL COMMENT '别名',
  language VARCHAR(20) NULL COMMENT '语言',
  alias_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL' COMMENT '别名来源：DEMAND、SUPPLIER、MANUAL、IMPORT_HISTORY',
  match_weight INT NOT NULL DEFAULT 100 COMMENT '匹配权重',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by BIGINT UNSIGNED NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_impa_alias_code (impa_code),
  KEY idx_impa_alias_name (alias_name(191)),
  KEY idx_impa_alias_type_enabled (alias_type, enabled),
  CONSTRAINT fk_impa_alias_item
    FOREIGN KEY (impa_code) REFERENCES impa_item (impa_code)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IMPA物料别名与匹配表';

CREATE TABLE IF NOT EXISTS supplier_sku (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  supplier_id BIGINT UNSIGNED NOT NULL COMMENT '供货商ID',
  supplier_sku_code VARCHAR(100) NULL COMMENT '供货商自己的SKU编码',
  supplier_sku_name VARCHAR(1000) NOT NULL COMMENT '供货商自己的SKU名称',
  supplier_specification VARCHAR(1000) NULL COMMENT '供货商规格',
  supplier_unit VARCHAR(100) NULL COMMENT '供货商单位',
  standard_type VARCHAR(30) NOT NULL COMMENT '标准库类型：IMPA、PROVISION',
  standard_code VARCHAR(100) NULL COMMENT '标准库编码，IMPA时关联impa_item.impa_code',
  match_status VARCHAR(30) NOT NULL DEFAULT 'UNMATCHED' COMMENT '匹配状态：UNMATCHED、AUTO_MATCHED、PENDING_CONFIRM、CONFIRMED、REJECTED',
  match_score DECIMAL(6,2) NULL COMMENT '匹配分数',
  service_port VARCHAR(100) NULL COMMENT '服务港口',
  available_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE' COMMENT '可供状态：AVAILABLE、OUT_OF_STOCK、DISABLED',
  last_quote_price DECIMAL(18,4) NULL COMMENT '最近报价',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_supplier_sku_supplier (supplier_id),
  KEY idx_supplier_sku_standard (standard_type, standard_code),
  KEY idx_supplier_sku_match_status (match_status),
  KEY idx_supplier_sku_name (supplier_sku_name(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供货商SKU与标准库映射表';

CREATE TABLE IF NOT EXISTS demand_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  demand_id BIGINT UNSIGNED NOT NULL COMMENT '需求单ID',
  line_no INT NOT NULL COMMENT '需求清单行号',
  raw_item_code VARCHAR(100) NULL COMMENT '原始物料编码',
  raw_item_name VARCHAR(1000) NOT NULL COMMENT '原始物料名称',
  raw_specification VARCHAR(1000) NULL COMMENT '原始规格',
  raw_unit VARCHAR(100) NULL COMMENT '原始单位',
  quantity DECIMAL(18,4) NULL COMMENT '需求数量',
  standard_type VARCHAR(30) NULL COMMENT '标准库类型：IMPA、PROVISION',
  standard_code VARCHAR(100) NULL COMMENT '匹配到的标准库编码',
  match_status VARCHAR(30) NOT NULL DEFAULT 'UNMATCHED' COMMENT '匹配状态：EXACT、SIMILAR、PENDING_CONFIRM、UNMATCHED、CONFIRMED',
  match_score DECIMAL(6,2) NULL COMMENT '匹配分数',
  confirmed_by BIGINT UNSIGNED NULL COMMENT '人工确认人',
  confirmed_at DATETIME NULL COMMENT '人工确认时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_demand_item_demand (demand_id, line_no),
  KEY idx_demand_item_standard (standard_type, standard_code),
  KEY idx_demand_item_match_status (match_status),
  KEY idx_demand_item_raw_name (raw_item_name(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='需求清单明细与标准库映射表';

CREATE TABLE IF NOT EXISTS data_import_batch (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  import_type VARCHAR(50) NOT NULL COMMENT '导入类型：IMPA、PROVISION、SUPPLIER_SKU、DEMAND_LIST',
  source_file VARCHAR(255) NOT NULL COMMENT '来源文件',
  source_sheet VARCHAR(100) NULL COMMENT '来源Sheet',
  total_rows INT NOT NULL DEFAULT 0 COMMENT '总行数',
  success_rows INT NOT NULL DEFAULT 0 COMMENT '成功行数',
  failed_rows INT NOT NULL DEFAULT 0 COMMENT '失败行数',
  status VARCHAR(30) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED、PROCESSING、SUCCESS、PARTIAL_SUCCESS、FAILED',
  started_at DATETIME NULL COMMENT '开始时间',
  finished_at DATETIME NULL COMMENT '结束时间',
  created_by BIGINT UNSIGNED NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_import_batch_type_status (import_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据导入批次表';

CREATE TABLE IF NOT EXISTS data_import_error (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  batch_id BIGINT UNSIGNED NOT NULL COMMENT '导入批次ID',
  source_sheet VARCHAR(100) NULL COMMENT '来源Sheet',
  source_row_no INT NULL COMMENT '来源行号',
  business_key VARCHAR(255) NULL COMMENT '业务键，例如IMPA编码',
  error_code VARCHAR(100) NOT NULL COMMENT '错误编码',
  error_message VARCHAR(1000) NOT NULL COMMENT '错误信息',
  raw_payload JSON NULL COMMENT '原始行数据',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_import_error_batch (batch_id),
  KEY idx_import_error_key (business_key),
  CONSTRAINT fk_import_error_batch
    FOREIGN KEY (batch_id) REFERENCES data_import_batch (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据导入异常表';

INSERT INTO impa_category
  (category_code, category_name_en, category_name_cn, parent_code, level, sort_order, item_count, enabled, remark)
VALUES
  ('11', 'Welfare Items', '船员后勤、娱乐用品', NULL, 1, 11, 236, 1, NULL),
  ('15', 'Cloth & Linen Products', '亚麻布类', NULL, 1, 15, 166, 1, NULL),
  ('17', 'Tableware & Galley Utensils', '厨房用品', NULL, 1, 17, 1796, 1, NULL),
  ('19', 'Clothing', '衣类', NULL, 1, 19, 329, 1, NULL),
  ('21', 'Rope & Hawsers', '绳子和钢缆', NULL, 1, 21, 1603, 1, NULL),
  ('23', 'Rigging Equipment & General Deck Items', '装配、索具类、甲板消耗品', NULL, 1, 23, 1574, 1, NULL),
  ('25', 'Marine Paint', '船舶油漆', NULL, 1, 25, 314, 1, NULL),
  ('27', 'Painting Equipment', '涂装用器具类', NULL, 1, 27, 200, 1, NULL),
  ('31', 'Safety Protective Gear', '安全防护用品', NULL, 1, 31, 0, 1, 'IMPA目录预留，当前文件无明细'),
  ('33', 'Safety Equipment', '救生救难用具、消火器类', NULL, 1, 33, 1017, 1, NULL),
  ('35', 'Hose & Couplings', '管件、连接器', NULL, 1, 35, 430, 1, NULL),
  ('37', 'Nautical Equipment', '航海器具类', NULL, 1, 37, 763, 1, NULL),
  ('39', 'Medicine', '卫生、医药品类', NULL, 1, 39, 314, 1, NULL),
  ('45', 'Petroleum Products', '石油制品类', NULL, 1, 45, 284, 1, NULL),
  ('47', 'Stationery', '文具类', NULL, 1, 47, 1214, 1, NULL),
  ('49', 'Hardware', '五金制品类', NULL, 1, 49, 354, 1, NULL),
  ('51', 'Brushes & Mats', '刷子、垫子类', NULL, 1, 51, 348, 1, NULL),
  ('53', 'Lavatory Equipment', '洗手间用具', NULL, 1, 53, 126, 1, NULL),
  ('55', 'Cleaning Material & Chemicals', '洗涤、化学制品类', NULL, 1, 55, 456, 1, NULL),
  ('59', 'Pneumatics & Electrical Tools', '风动、电动工具', NULL, 1, 59, 672, 1, NULL),
  ('61', 'Hand Tools', '一般作业工具类', NULL, 1, 61, 3692, 1, NULL),
  ('63', 'CuttingTools', '切削工具', NULL, 1, 63, 2295, 1, NULL),
  ('65', 'Measuring Tools', '测量工具', NULL, 1, 65, 860, 1, NULL),
  ('67', 'Metal Sheets,Bars,etc…', '金属板、钢筋类', NULL, 1, 67, 1169, 1, NULL),
  ('69', 'Screws & Nuts', '螺钉、螺帽类', NULL, 1, 69, 422, 1, NULL),
  ('71', 'Pipes & Tubes', '管类', NULL, 1, 71, 598, 1, NULL),
  ('73', 'Pipe & Tube Fittings', '管接头类', NULL, 1, 73, 2298, 1, NULL),
  ('75', 'Valves & Cocks', '阀、旋塞类', NULL, 1, 75, 700, 1, NULL),
  ('77', 'Bearings', '轴承类', NULL, 1, 77, 1358, 1, NULL),
  ('79', 'Electrical Equipment', '电器设备', NULL, 1, 79, 1948, 1, NULL),
  ('81', 'Packing & Jointing', '接口密封用品', NULL, 1, 81, 1369, 1, NULL),
  ('85', 'Welding Equipment', '焊接设备', NULL, 1, 85, 326, 1, NULL),
  ('87', 'Machinery Equipment (Blank)', '机器设备（空白）', NULL, 1, 87, 0, 1, 'IMPA目录预留，当前文件无明细')
ON DUPLICATE KEY UPDATE
  category_name_en = VALUES(category_name_en),
  category_name_cn = VALUES(category_name_cn),
  item_count = VALUES(item_count),
  enabled = VALUES(enabled),
  remark = VALUES(remark),
  updated_at = CURRENT_TIMESTAMP;
