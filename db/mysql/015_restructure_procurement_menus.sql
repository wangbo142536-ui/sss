-- Restructure procurement chain menus into material and food procurement groups.
-- Idempotent: only inserts/updates sys_permission, sys_menu, and missing admin role permissions.
-- It does not clear role permissions, user menu customizations, or business data.

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('PROCUREMENT_MATERIALS_VIEW', '物料采购入口查看', 'MENU', 'PROCUREMENT_MATERIALS', 'VIEW', 1),
  ('PROCUREMENT_FOOD_VIEW', '伙食采购入口查看', 'MENU', 'PROCUREMENT_FOOD', 'VIEW', 1),
  ('INQUIRY_VIEW', '询价管理查看', 'MENU', 'INQUIRIES', 'VIEW', 1),
  ('QUOTE_VIEW', '报价管理查看', 'MENU', 'QUOTES', 'VIEW', 1),
  ('COMPARISON_VIEW', '比价管理查看', 'MENU', 'COMPARISON', 'VIEW', 1),
  ('ORDER_VIEW', '采购管理查看', 'MENU', 'ORDERS', 'VIEW', 1),
  ('FOOD_INQUIRY_VIEW', '伙食询价管理查看', 'MENU', 'FOOD_INQUIRIES', 'VIEW', 1),
  ('FOOD_QUOTE_VIEW', '伙食报价管理查看', 'MENU', 'FOOD_QUOTES', 'VIEW', 1),
  ('FOOD_COMPARISON_VIEW', '伙食比价管理查看', 'MENU', 'FOOD_COMPARISON', 'VIEW', 1),
  ('FOOD_ORDER_VIEW', '伙食采购管理查看', 'MENU', 'FOOD_ORDERS', 'VIEW', 1)
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  resource_code = VALUES(resource_code),
  action_code = VALUES(action_code),
  enabled = VALUES(enabled);

INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
)
VALUES
  ('MATERIAL_PROCUREMENT_GROUP', '物料采购', '', 'PackageSearch', NULL, 10, NULL, 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_PROCUREMENT_GROUP', '伙食采购管理', '', 'Utensils', NULL, 20, NULL, 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('PROCUREMENT_MATERIALS', '物料采购入口', '/procurement/materials', 'PackageSearch', 'MATERIAL_PROCUREMENT_GROUP', 0, 'PROCUREMENT_MATERIALS_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('INQUIRIES', '询价管理', '/inquiries', 'FileSearch', 'MATERIAL_PROCUREMENT_GROUP', 10, 'INQUIRY_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('QUOTES', '报价管理', '/quotes', 'FileText', 'MATERIAL_PROCUREMENT_GROUP', 20, 'QUOTE_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('COMPARISON', '比价管理', '/comparison', 'Scale', 'MATERIAL_PROCUREMENT_GROUP', 30, 'COMPARISON_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('ORDERS', '采购管理', '/orders', 'ShoppingCart', 'MATERIAL_PROCUREMENT_GROUP', 40, 'ORDER_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('PROCUREMENT_FOOD', '伙食采购入口', '/procurement/food', 'Utensils', 'FOOD_PROCUREMENT_GROUP', 0, 'PROCUREMENT_FOOD_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_INQUIRIES', '询价管理', '/food/inquiries', 'FileSearch', 'FOOD_PROCUREMENT_GROUP', 10, 'FOOD_INQUIRY_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_QUOTES', '报价管理', '/food/quotes', 'FileText', 'FOOD_PROCUREMENT_GROUP', 20, 'FOOD_QUOTE_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_COMPARISON', '比价管理', '/food/comparison', 'Scale', 'FOOD_PROCUREMENT_GROUP', 30, 'FOOD_COMPARISON_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_ORDERS', '采购管理', '/food/orders', 'ShoppingCart', 'FOOD_PROCUREMENT_GROUP', 40, 'FOOD_ORDER_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  route_path = VALUES(route_path),
  icon = VALUES(icon),
  parent_code = VALUES(parent_code),
  sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission),
  visible_roles = VALUES(visible_roles),
  enabled = VALUES(enabled);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code IN (
    'PROCUREMENT_MATERIALS_VIEW',
    'PROCUREMENT_FOOD_VIEW',
    'INQUIRY_VIEW',
    'QUOTE_VIEW',
    'COMPARISON_VIEW',
    'ORDER_VIEW',
    'FOOD_INQUIRY_VIEW',
    'FOOD_QUOTE_VIEW',
    'FOOD_COMPARISON_VIEW',
    'FOOD_ORDER_VIEW'
  )
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NULL
  AND role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code IN (
    'PROCUREMENT_MATERIALS_VIEW',
    'PROCUREMENT_FOOD_VIEW',
    'INQUIRY_VIEW',
    'QUOTE_VIEW',
    'COMPARISON_VIEW',
    'ORDER_VIEW',
    'FOOD_INQUIRY_VIEW',
    'FOOD_QUOTE_VIEW',
    'FOOD_COMPARISON_VIEW',
    'FOOD_ORDER_VIEW'
  )
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NOT NULL
  AND (
    role.role_type = 'COMPANY_ADMIN'
    OR role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)
  );
