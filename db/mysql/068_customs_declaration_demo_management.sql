-- Demo customs declaration management for the August milestone.
-- The source PDF remains external and is never stored in this table or the repository.

CREATE TABLE IF NOT EXISTS customs_declaration_demo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  business_type VARCHAR(20) NOT NULL COMMENT 'MATERIAL or FOOD',
  purchase_order_id BIGINT NOT NULL,
  purchase_order_no VARCHAR(80) NOT NULL,
  buyer_company_id BIGINT NOT NULL,
  responsible_company_id BIGINT NOT NULL,
  responsible_type VARCHAR(20) NOT NULL COMMENT 'SUPPLIER or BARGE',
  responsible_company_name VARCHAR(200) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DECLARED',
  vessel_name VARCHAR(160) NULL,
  ship_agent VARCHAR(200) NULL,
  goods_category VARCHAR(160) NULL,
  trade_type VARCHAR(40) NULL,
  declarant_company VARCHAR(200) NULL,
  declarant_contact VARCHAR(120) NULL,
  declarant_phone VARCHAR(80) NULL,
  delivery_start VARCHAR(80) NULL,
  delivery_end VARCHAR(80) NULL,
  delivery_location VARCHAR(500) NULL,
  supply_vessel VARCHAR(160) NULL,
  captain_contact VARCHAR(160) NULL,
  applicant VARCHAR(120) NULL,
  applicant_phone VARCHAR(80) NULL,
  application_date DATE NULL,
  customs_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  attachment_code VARCHAR(40) NOT NULL DEFAULT 'ATTACHMENT_ONE',
  declared_by BIGINT NOT NULL,
  declared_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customs_demo_order (business_type, purchase_order_id),
  KEY idx_customs_demo_buyer (buyer_company_id, declared_at),
  KEY idx_customs_demo_responsible (responsible_company_id, declared_at),
  KEY idx_customs_demo_status (status, declared_at),
  CONSTRAINT fk_customs_demo_buyer FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_customs_demo_responsible FOREIGN KEY (responsible_company_id) REFERENCES company(id),
  CONSTRAINT fk_customs_demo_user FOREIGN KEY (declared_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Explicitly triggered demo customs declarations';

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES ('CUSTOMS_DECLARATION_VIEW', '报关管理查看', 'MENU', 'CUSTOMS_DECLARATION', 'VIEW', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), enabled = VALUES(enabled);

INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
) VALUES
  ('CUSTOMS_SERVICES_GROUP', '海关服务', '', 'FileText', NULL, 50, NULL,
   'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('CUSTOMS_DECLARATION', '报关管理', '/customs-services/declarations', 'FileText',
   'CUSTOMS_SERVICES_GROUP', 10, 'CUSTOMS_DECLARATION_VIEW',
   'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name), route_path = VALUES(route_path), icon = VALUES(icon),
  parent_code = VALUES(parent_code), sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission), visible_roles = VALUES(visible_roles), enabled = VALUES(enabled);

UPDATE sys_menu SET enabled = 0 WHERE menu_code = 'CUSTOMS_SERVICES';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.permission_code = 'CUSTOMS_DECLARATION_VIEW' AND permission.enabled = 1
WHERE role.enabled = 1
  AND (
    (role.company_id IS NULL AND role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN'))
    OR (role.company_id IS NOT NULL AND (role.role_type = 'COMPANY_ADMIN' OR role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)))
  );
