-- Zhoushan supply-port hierarchy for material procurement.
-- Keep the existing material_demand.supply_port_code/name contract:
--   supply_port_code = operation area code
--   supply_port_name = port area / operation area display text

INSERT INTO sys_dictionary_type (type_code, type_name, description, sort_order, enabled)
VALUES
  ('PORT_AREA', '港区', '港口下属港区；item_value 存储所属港口编码', 11, 1),
  ('PORT_OPERATION_AREA', '作业区', '港区下属作业区；item_value 存储所属港区编码', 12, 1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);

INSERT INTO sys_dictionary_item (type_code, item_code, item_name, item_value, item_name_en, sort_order, enabled, built_in)
VALUES
  ('PORT_AREA', 'ZHOUSHAN_YANGSHAN', '洋山港区', 'ZHOUSHAN', 'Yangshan Port Area', 10, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_LIUHENG', '六横港区', 'ZHOUSHAN', 'Liuheng Port Area', 20, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_QUSHAN', '衢山港区', 'ZHOUSHAN', 'Qushan Port Area', 30, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_JINTANG', '金塘港区', 'ZHOUSHAN', 'Jintang Port Area', 40, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_CENGANG', '岑港港区', 'ZHOUSHAN', 'Cengang Port Area', 50, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_SHENGSI', '嵊泗港区', 'ZHOUSHAN', 'Shengsi Port Area', 60, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_DAISHAN', '岱山港区', 'ZHOUSHAN', 'Daishan Port Area', 70, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_BAIQUAN', '白泉港区', 'ZHOUSHAN', 'Baiquan Port Area', 80, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_MAAO', '马岙港区', 'ZHOUSHAN', 'Ma''ao Port Area', 90, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_DINGHAI', '定海港区', 'ZHOUSHAN', 'Dinghai Port Area', 100, 1, 1),
  ('PORT_AREA', 'ZHOUSHAN_SHENJIAMEN', '沈家门港区', 'ZHOUSHAN', 'Shenjiamen Port Area', 110, 1, 1)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  item_value = VALUES(item_value),
  item_name_en = VALUES(item_name_en),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  built_in = VALUES(built_in);

INSERT INTO sys_dictionary_item (type_code, item_code, item_name, item_value, item_name_en, sort_order, enabled, built_in)
VALUES
  ('PORT_OPERATION_AREA', 'SULANGHU', 'SULANGHU', 'ZHOUSHAN_QUSHAN', 'Sulanghu Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'XIAOHUANGSHA', '小黄沙', 'ZHOUSHAN_QUSHAN', 'Xiaohuangsha Operation Area', 20, 1, 1),
  ('PORT_OPERATION_AREA', 'NILUOSHAN', '泥螺山', 'ZHOUSHAN_QUSHAN', 'Niluoshan Operation Area', 30, 1, 1),
  ('PORT_OPERATION_AREA', 'HUQINAO', '胡琴岙', 'ZHOUSHAN_QUSHAN', 'Huqinao Operation Area', 40, 1, 1),
  ('PORT_OPERATION_AREA', 'SHEYIMEN', '蛇移门', 'ZHOUSHAN_QUSHAN', 'Sheyimen Operation Area', 50, 1, 1),
  ('PORT_OPERATION_AREA', 'HUANGZE', '黄泽', 'ZHOUSHAN_QUSHAN', 'Huangze Operation Area', 60, 1, 1),
  ('PORT_OPERATION_AREA', 'YANGSHAN', '洋山', 'ZHOUSHAN_YANGSHAN', 'Yangshan Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'LIUHENG', '六横', 'ZHOUSHAN_LIUHENG', 'Liuheng Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'JINTANG', '金塘', 'ZHOUSHAN_JINTANG', 'Jintang Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'CENGANG', '岑港', 'ZHOUSHAN_CENGANG', 'Cengang Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'LAOTANGSHAN', '老塘山', 'ZHOUSHAN_CENGANG', 'Laotangshan Operation Area', 20, 1, 1),
  ('PORT_OPERATION_AREA', 'SHENGSI', '嵊泗', 'ZHOUSHAN_SHENGSI', 'Shengsi Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'DAISHAN', '岱山', 'ZHOUSHAN_DAISHAN', 'Daishan Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'BAIQUAN', '白泉', 'ZHOUSHAN_BAIQUAN', 'Baiquan Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'MAAO', '马岙', 'ZHOUSHAN_MAAO', 'Ma''ao Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'DINGHAI', '定海', 'ZHOUSHAN_DINGHAI', 'Dinghai Operation Area', 10, 1, 1),
  ('PORT_OPERATION_AREA', 'SHENJIAMEN', '沈家门', 'ZHOUSHAN_SHENJIAMEN', 'Shenjiamen Operation Area', 10, 1, 1)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  item_value = VALUES(item_value),
  item_name_en = VALUES(item_name_en),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  built_in = VALUES(built_in);

UPDATE material_demand
SET supply_port_code = 'SULANGHU',
    supply_port_name = '衢山港区/SULANGHU'
WHERE UPPER(REPLACE(REPLACE(REPLACE(COALESCE(supply_port_code, ''), ' ', ''), '-', ''), '_', '')) IN ('SULANGHU', 'SHULANGHU')
   OR UPPER(REPLACE(REPLACE(REPLACE(COALESCE(supply_port_name, ''), ' ', ''), '-', ''), '_', '')) IN ('SULANGHU', 'SHULANGHU')
   OR COALESCE(supply_port_code, '') LIKE '%鼠浪湖%'
   OR COALESCE(supply_port_name, '') LIKE '%鼠浪湖%';

UPDATE purchase_order
SET supply_port = '衢山港区/SULANGHU'
WHERE UPPER(REPLACE(REPLACE(REPLACE(COALESCE(supply_port, ''), ' ', ''), '-', ''), '_', '')) IN ('SULANGHU', 'SHULANGHU')
   OR COALESCE(supply_port, '') LIKE '%鼠浪湖%';
