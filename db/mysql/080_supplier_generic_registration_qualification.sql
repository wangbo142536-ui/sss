-- 给每个服务商企业补充一份平台初始化企业注册资质示例。
-- 图片为应用内置只读资源，不依赖部署机器的绝对文件路径。
-- 每家企业使用独立 file_id 和独立资质记录，保留企业原有真实资质。

INSERT INTO sys_file (
  file_id,
  uploader_user_id,
  original_name,
  storage_path,
  content_type,
  file_size,
  status,
  created_at
)
SELECT
  CONCAT('INIT-SUPPLIER-REG-CERT-', LPAD(company.id, 6, '0')),
  NULL,
  '企业注册资质通用示例.png',
  'classpath:initialization/generic-enterprise-registration-qualification.png',
  'image/png',
  1651641,
  'ACTIVE',
  CURRENT_TIMESTAMP
FROM company
WHERE company.company_type = 'SUPPLIER'
ON DUPLICATE KEY UPDATE
  original_name = VALUES(original_name),
  storage_path = VALUES(storage_path),
  content_type = VALUES(content_type),
  file_size = VALUES(file_size),
  status = 'ACTIVE';

INSERT INTO company_qualification (
  company_id,
  file_id,
  file_type,
  file_name,
  title,
  description,
  status,
  created_at,
  updated_at
)
SELECT
  company.id,
  CONCAT('INIT-SUPPLIER-REG-CERT-', LPAD(company.id, 6, '0')),
  'ENTERPRISE_REGISTRATION_QUALIFICATION',
  '企业注册资质通用示例.png',
  '企业注册资质证明（平台初始化示例）',
  '平台初始化通用企业注册资质，仅用于系统展示，不作为政府签发证件或法律凭证。',
  'APPROVED',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM company
WHERE company.company_type = 'SUPPLIER'
  AND NOT EXISTS (
    SELECT 1
    FROM company_qualification qualification
    WHERE qualification.company_id = company.id
      AND qualification.file_id = CONCAT('INIT-SUPPLIER-REG-CERT-', LPAD(company.id, 6, '0'))
  );
