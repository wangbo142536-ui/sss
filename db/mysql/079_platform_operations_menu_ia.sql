-- Group enterprise information, data analysis and supplier governance under Platform Operations.
-- Idempotent and metadata-only: existing permissions, role bindings and user menu bindings are preserved.

INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
)
VALUES (
  'PLATFORM_OPERATIONS', '平台运营', '', 'OP', NULL, 95,
  NULL, 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1
)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  route_path = VALUES(route_path),
  icon = VALUES(icon),
  parent_code = VALUES(parent_code),
  sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission),
  visible_roles = VALUES(visible_roles),
  enabled = VALUES(enabled);

-- Data analysis was previously supplied only by the frontend menu fallback.
-- Reuse SUPPLIER_ORDER_VIEW: its existing bindings already cover supplier users and platform admins,
-- while excluding ship-agent and barge-agent companies. No permission or role binding is created or widened.
INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
)
VALUES (
  'SUPPLIER_DATA_ANALYSIS', '数据分析', '/platform-operations/data-analysis', 'DA',
  'PLATFORM_OPERATIONS', 20,
  'SUPPLIER_ORDER_VIEW', 'SUPPLIER,PLATFORM_ADMIN', 1
)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  route_path = VALUES(route_path),
  icon = VALUES(icon),
  parent_code = VALUES(parent_code),
  sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission),
  visible_roles = VALUES(visible_roles),
  enabled = VALUES(enabled);

UPDATE sys_menu
SET parent_code = 'PLATFORM_OPERATIONS',
    sort_order = CASE menu_code
      WHEN 'SHOP_MANAGEMENT' THEN 10
      WHEN 'SUPPLIER_DATA_ANALYSIS' THEN 20
      WHEN 'ADMIN_REGISTRATIONS' THEN 30
      WHEN 'SUPPLIERS' THEN 40
      ELSE sort_order
    END,
    menu_name = CASE menu_code
      WHEN 'SHOP_MANAGEMENT' THEN '企业信息'
      WHEN 'SUPPLIER_DATA_ANALYSIS' THEN '数据分析'
      WHEN 'ADMIN_REGISTRATIONS' THEN '服务商审核'
      WHEN 'SUPPLIERS' THEN '服务商管理'
      ELSE menu_name
    END
WHERE menu_code IN (
  'SHOP_MANAGEMENT',
  'SUPPLIER_DATA_ANALYSIS',
  'ADMIN_REGISTRATIONS',
  'SUPPLIERS'
);
