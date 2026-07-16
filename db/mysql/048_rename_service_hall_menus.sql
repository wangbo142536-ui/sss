UPDATE sys_menu
SET menu_name = '服务管理'
WHERE menu_code IN ('TRAFFIC_SERVICE', 'DELIVERY_TASKS');

UPDATE sys_menu
SET menu_name = '服务大厅'
WHERE menu_code = 'TRAFFIC_BOAT';
