-- Traffic service dictionary convergence.
-- Sea areas and anchorages are maintained by sys_dictionary_*; traffic prices keep using anchorage codes.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO sys_dictionary_type (type_code, type_name, description, sort_order, enabled)
VALUES
  ('SEA_AREA', '海域', '交通服务海域，如北部海域、南部海域。', 60, 1),
  ('ANCHORAGE', '锚地', '交通服务锚地，item_value 存所属海域编码。', 61, 1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);

INSERT INTO sys_dictionary_item (type_code, item_code, item_name, item_value, item_name_en, description, sort_order, enabled, built_in)
VALUES
  ('SEA_AREA', 'NORTH', '北部海域', 'NORTH', 'North Sea Area', '', 10, 1, 1),
  ('SEA_AREA', 'SOUTH', '南部海域', 'SOUTH', 'South Sea Area', '', 20, 1, 1)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  item_value = VALUES(item_value),
  item_name_en = VALUES(item_name_en),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  built_in = VALUES(built_in);

INSERT INTO sys_dictionary_item (type_code, item_code, item_name, item_value, item_name_en, description, sort_order, enabled, built_in)
VALUES
  ('ANCHORAGE', 'NORTH_XIUSHAN_EAST', '秀山东锚地', 'NORTH', 'Xiushan East Anchorage', '', 10, 1, 1),
  ('ANCHORAGE', 'NORTH_QUSHAN_TEMP', '衢山临时锚地', 'NORTH', 'Qushan Temporary Anchorage', '', 20, 1, 1),
  ('ANCHORAGE', 'NORTH_XIANGLU_HUAPINGJIAO', '香炉花瓶礁锚地', 'NORTH', 'Xianglu Huapingjiao Anchorage', '', 30, 1, 1),
  ('ANCHORAGE', 'NORTH_CHANGBAI_ISLAND', '长白岛', 'NORTH', 'Changbai Island', '', 40, 1, 1),
  ('ANCHORAGE', 'NORTH_XIUSHAN_ISLAND', '秀山岛', 'NORTH', 'Xiushan Island', '', 50, 1, 1),
  ('ANCHORAGE', 'NORTH_XIUSHAN_EAST_OUTER', '秀山东外锚地', 'NORTH', 'Xiushan East Outer Anchorage', '', 60, 1, 1),
  ('ANCHORAGE', 'NORTH_WUZHI_NORTH', '五峙北锚地', 'NORTH', 'Wuzhi North Anchorage', '', 70, 1, 1),
  ('ANCHORAGE', 'NORTH_DAISHAN_CHANGHONG_CHANGTU', '岱山长宏船厂（长涂）锚地', 'NORTH', 'Daishan Changhong Changtu Anchorage', '', 80, 1, 1),
  ('ANCHORAGE', 'NORTH_ZHONGTIAN_SHIPYARD', '中天船厂锚地', 'NORTH', 'Zhongtian Shipyard Anchorage', '', 90, 1, 1),
  ('ANCHORAGE', 'NORTH_CHANGHONG_MAIN', '长宏国际船厂（本部）锚地', 'NORTH', 'Changhong Main Shipyard Anchorage', '', 100, 1, 1),
  ('ANCHORAGE', 'NORTH_PACIFIC_SHIPYARD', '太平洋船厂锚地', 'NORTH', 'Pacific Shipyard Anchorage', '', 110, 1, 1),
  ('ANCHORAGE', 'NORTH_QUSHAN_ISLAND', '衢山岛', 'NORTH', 'Qushan Island', '', 120, 1, 1),
  ('ANCHORAGE', 'NORTH_DAISHAN_HUAFENG', '岱山华丰船厂锚地', 'NORTH', 'Daishan Huafeng Shipyard Anchorage', '', 130, 1, 1),
  ('ANCHORAGE', 'NORTH_DAISHAN_CHANGHONG_JIANGNANSHAN', '岱山长宏船厂（江南山）锚地', 'NORTH', 'Daishan Changhong Jiangnanshan Anchorage', '', 140, 1, 1),
  ('ANCHORAGE', 'NORTH_DAISHAN_TO_CHANGTU', '岱山本岛到岱山长宏船厂（长涂）锚地', 'NORTH', 'Daishan to Changtu Anchorage', '', 150, 1, 1),
  ('ANCHORAGE', 'NORTH_JINHAI_HEAVY', '金海重工锚地', 'NORTH', 'Jinhai Heavy Industry Anchorage', '', 160, 1, 1),
  ('ANCHORAGE', 'NORTH_DAISHAN_TO_JINHAI', '岱山本岛到金海重工', 'NORTH', 'Daishan to Jinhai Heavy Industry', '', 170, 1, 1),
  ('ANCHORAGE', 'SOUTH_MAZHI_1', '马峙1号锚地', 'SOUTH', 'Mazhi No.1 Anchorage', '', 10, 1, 1),
  ('ANCHORAGE', 'SOUTH_MAZHI_2', '马峙2号锚地', 'SOUTH', 'Mazhi No.2 Anchorage', '', 20, 1, 1),
  ('ANCHORAGE', 'SOUTH_AOSHAN_INSPECTION', '岙山联检锚地', 'SOUTH', 'Aoshan Inspection Anchorage', '', 30, 1, 1),
  ('ANCHORAGE', 'SOUTH_XIAZHIMEN_NORTH', '虾峙门北锚地', 'SOUTH', 'Xiazhimen North Anchorage', '', 40, 1, 1),
  ('ANCHORAGE', 'SOUTH_XIAZHIMEN_SOUTH', '虾峙门南锚地', 'SOUTH', 'Xiazhimen South Anchorage', '', 50, 1, 1),
  ('ANCHORAGE', 'SOUTH_TIAOZHOUMEN', '条帚门锚地', 'SOUTH', 'Tiaozhoumen Anchorage', '', 60, 1, 1),
  ('ANCHORAGE', 'SOUTH_LIUHENG_SHIPYARD', '六横船厂锚地', 'SOUTH', 'Liuheng Shipyard Anchorage', '', 70, 1, 1),
  ('ANCHORAGE', 'SOUTH_LAOTANGSHAN_TERMINAL', '老塘山码头', 'SOUTH', 'Laotangshan Terminal', '', 80, 1, 1),
  ('ANCHORAGE', 'SOUTH_WANBANG_SHIPYARD', '万邦船厂锚地', 'SOUTH', 'Wanbang Shipyard Anchorage', '', 90, 1, 1),
  ('ANCHORAGE', 'SOUTH_PUTUO_CHANGHONG', '普陀长宏船厂锚地', 'SOUTH', 'Putuo Changhong Shipyard Anchorage', '', 100, 1, 1),
  ('ANCHORAGE', 'SOUTH_LIUHENG_TO_DONGBALIAN', '六横到东白莲码头', 'SOUTH', 'Liuheng to Dongbailian Terminal', '', 110, 1, 1),
  ('ANCHORAGE', 'SOUTH_WUGANG_TERMINAL', '武港码头', 'SOUTH', 'Wugang Terminal', '', 120, 1, 1),
  ('ANCHORAGE', 'SOUTH_LIUHENG_COAL_POWER', '六横煤电码头', 'SOUTH', 'Liuheng Coal Power Terminal', '', 130, 1, 1),
  ('ANCHORAGE', 'SOUTH_HUATAI_OIL', '华泰油库码头', 'SOUTH', 'Huatai Oil Terminal', '', 140, 1, 1),
  ('ANCHORAGE', 'SOUTH_JINRUN_TERMINAL', '金润码头', 'SOUTH', 'Jinrun Terminal', '', 150, 1, 1),
  ('ANCHORAGE', 'SOUTH_ZHONGAO_TERMINAL', '中奥码头', 'SOUTH', 'Zhongao Terminal', '', 160, 1, 1),
  ('ANCHORAGE', 'SOUTH_CEZI_OIL', '册子油库码头', 'SOUTH', 'Cezi Oil Terminal', '', 170, 1, 1),
  ('ANCHORAGE', 'SOUTH_WAIDIAO_OIL', '外钓油库码头', 'SOUTH', 'Waidiao Oil Terminal', '', 180, 1, 1),
  ('ANCHORAGE', 'SOUTH_YANTIAN_ZHOUSHAN_STORAGE', '深圳盐田舟山储运码头（原光汇码头）', 'SOUTH', 'Yantian Zhoushan Storage Terminal', '', 190, 1, 1),
  ('ANCHORAGE', 'SOUTH_AOSHAN_XINGZHONG', '岙山兴中码头', 'SOUTH', 'Aoshan Xingzhong Terminal', '', 200, 1, 1),
  ('ANCHORAGE', 'SOUTH_DADING_OIL', '大鼎油库码头', 'SOUTH', 'Dading Oil Terminal', '', 210, 1, 1),
  ('ANCHORAGE', 'SOUTH_XINAO_LNG', '新奥LNG码头', 'SOUTH', 'Xinao LNG Terminal', '', 220, 1, 1),
  ('ANCHORAGE', 'SOUTH_DENGBU_ISLAND', '登步岛', 'SOUTH', 'Dengbu Island', '', 230, 1, 1)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  item_value = VALUES(item_value),
  item_name_en = VALUES(item_name_en),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  built_in = VALUES(built_in);

UPDATE sys_menu
SET enabled = 0
WHERE menu_code = 'TRAFFIC_BOAT';
