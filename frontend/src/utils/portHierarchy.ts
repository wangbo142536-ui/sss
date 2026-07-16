import type { DictionaryItem } from "@/services/dataDictionaryService";

export const PORT_AREA_TYPE = "PORT_AREA";
export const PORT_OPERATION_AREA_TYPE = "PORT_OPERATION_AREA";

const SULANGHU_LABEL = "衢山港区/SULANGHU";
const SULANGHU_ALIASES = ["SULANGHU", "SHULANGHU", "SHU LANG HU", "SHU-LANG-HU", "鼠浪湖"];

const dictionaryItem = (
  id: number,
  typeCode: string,
  itemCode: string,
  itemName: string,
  itemValue: string,
  itemNameEn: string,
  sortOrder: number
): DictionaryItem => ({
  id,
  typeCode,
  itemCode,
  itemName,
  itemValue,
  itemNameEn,
  sortOrder,
  enabled: true,
  builtIn: true
});

export const DEFAULT_ZHOUSHAN_PORT_AREAS: DictionaryItem[] = [
  dictionaryItem(-55101, PORT_AREA_TYPE, "ZHOUSHAN_YANGSHAN", "洋山港区", "ZHOUSHAN", "Yangshan Port Area", 10),
  dictionaryItem(-55102, PORT_AREA_TYPE, "ZHOUSHAN_LIUHENG", "六横港区", "ZHOUSHAN", "Liuheng Port Area", 20),
  dictionaryItem(-55103, PORT_AREA_TYPE, "ZHOUSHAN_QUSHAN", "衢山港区", "ZHOUSHAN", "Qushan Port Area", 30),
  dictionaryItem(-55104, PORT_AREA_TYPE, "ZHOUSHAN_JINTANG", "金塘港区", "ZHOUSHAN", "Jintang Port Area", 40),
  dictionaryItem(-55105, PORT_AREA_TYPE, "ZHOUSHAN_CENGANG", "岑港港区", "ZHOUSHAN", "Cengang Port Area", 50),
  dictionaryItem(-55106, PORT_AREA_TYPE, "ZHOUSHAN_SHENGSI", "嵊泗港区", "ZHOUSHAN", "Shengsi Port Area", 60),
  dictionaryItem(-55107, PORT_AREA_TYPE, "ZHOUSHAN_DAISHAN", "岱山港区", "ZHOUSHAN", "Daishan Port Area", 70),
  dictionaryItem(-55108, PORT_AREA_TYPE, "ZHOUSHAN_BAIQUAN", "白泉港区", "ZHOUSHAN", "Baiquan Port Area", 80),
  dictionaryItem(-55109, PORT_AREA_TYPE, "ZHOUSHAN_MAAO", "马岙港区", "ZHOUSHAN", "Ma'ao Port Area", 90),
  dictionaryItem(-55110, PORT_AREA_TYPE, "ZHOUSHAN_DINGHAI", "定海港区", "ZHOUSHAN", "Dinghai Port Area", 100),
  dictionaryItem(-55111, PORT_AREA_TYPE, "ZHOUSHAN_SHENJIAMEN", "沈家门港区", "ZHOUSHAN", "Shenjiamen Port Area", 110)
];

export const DEFAULT_ZHOUSHAN_OPERATION_AREAS: DictionaryItem[] = [
  dictionaryItem(-55201, PORT_OPERATION_AREA_TYPE, "SULANGHU", "SULANGHU", "ZHOUSHAN_QUSHAN", "Sulanghu Operation Area", 10),
  dictionaryItem(-55202, PORT_OPERATION_AREA_TYPE, "XIAOHUANGSHA", "小黄沙", "ZHOUSHAN_QUSHAN", "Xiaohuangsha Operation Area", 20),
  dictionaryItem(-55203, PORT_OPERATION_AREA_TYPE, "NILUOSHAN", "泥螺山", "ZHOUSHAN_QUSHAN", "Niluoshan Operation Area", 30),
  dictionaryItem(-55204, PORT_OPERATION_AREA_TYPE, "HUQINAO", "胡琴岙", "ZHOUSHAN_QUSHAN", "Huqinao Operation Area", 40),
  dictionaryItem(-55205, PORT_OPERATION_AREA_TYPE, "SHEYIMEN", "蛇移门", "ZHOUSHAN_QUSHAN", "Sheyimen Operation Area", 50),
  dictionaryItem(-55206, PORT_OPERATION_AREA_TYPE, "HUANGZE", "黄泽", "ZHOUSHAN_QUSHAN", "Huangze Operation Area", 60),
  dictionaryItem(-55207, PORT_OPERATION_AREA_TYPE, "YANGSHAN", "洋山", "ZHOUSHAN_YANGSHAN", "Yangshan Operation Area", 10),
  dictionaryItem(-55208, PORT_OPERATION_AREA_TYPE, "LIUHENG", "六横", "ZHOUSHAN_LIUHENG", "Liuheng Operation Area", 10),
  dictionaryItem(-55209, PORT_OPERATION_AREA_TYPE, "JINTANG", "金塘", "ZHOUSHAN_JINTANG", "Jintang Operation Area", 10),
  dictionaryItem(-55210, PORT_OPERATION_AREA_TYPE, "CENGANG", "岑港", "ZHOUSHAN_CENGANG", "Cengang Operation Area", 10),
  dictionaryItem(-55211, PORT_OPERATION_AREA_TYPE, "LAOTANGSHAN", "老塘山", "ZHOUSHAN_CENGANG", "Laotangshan Operation Area", 20),
  dictionaryItem(-55212, PORT_OPERATION_AREA_TYPE, "SHENGSI", "嵊泗", "ZHOUSHAN_SHENGSI", "Shengsi Operation Area", 10),
  dictionaryItem(-55213, PORT_OPERATION_AREA_TYPE, "DAISHAN", "岱山", "ZHOUSHAN_DAISHAN", "Daishan Operation Area", 10),
  dictionaryItem(-55214, PORT_OPERATION_AREA_TYPE, "BAIQUAN", "白泉", "ZHOUSHAN_BAIQUAN", "Baiquan Operation Area", 10),
  dictionaryItem(-55215, PORT_OPERATION_AREA_TYPE, "MAAO", "马岙", "ZHOUSHAN_MAAO", "Ma'ao Operation Area", 10),
  dictionaryItem(-55216, PORT_OPERATION_AREA_TYPE, "DINGHAI", "定海", "ZHOUSHAN_DINGHAI", "Dinghai Operation Area", 10),
  dictionaryItem(-55217, PORT_OPERATION_AREA_TYPE, "SHENJIAMEN", "沈家门", "ZHOUSHAN_SHENJIAMEN", "Shenjiamen Operation Area", 10)
];

export type PortHierarchySelection = {
  portCode: string;
  areaCode: string;
  operationCode: string;
  displayName: string;
  customValue: string;
};

export function normalizePortToken(value?: string | null): string {
  return String(value ?? "")
    .replace(/港区|作业区|港口|港/g, "")
    .replace(/[^A-Za-z0-9\u4e00-\u9fa5]/g, "")
    .toUpperCase();
}

function itemTokens(item: DictionaryItem): string[] {
  return [item.itemCode, item.itemName, item.itemValue, item.itemNameEn].map(normalizePortToken).filter(Boolean);
}

function matchesDictionaryItem(item: DictionaryItem, normalized: string): boolean {
  if (!normalized) return false;
  return itemTokens(item).some((token) => token === normalized);
}

function isSulanghuText(value?: string | null): boolean {
  const normalized = normalizePortToken(value);
  return SULANGHU_ALIASES.map(normalizePortToken).some((alias) => alias === normalized);
}

function splitPortParts(...values: Array<string | undefined | null>): string[] {
  return values
    .flatMap((value) => String(value ?? "").split(/[\/\\>|,，;；]+/))
    .map((part) => part.trim())
    .filter(Boolean);
}

function findItem(items: DictionaryItem[], raw?: string | null): DictionaryItem | undefined {
  const normalized = normalizePortToken(raw);
  if (!normalized) return undefined;
  return items.find((item) => matchesDictionaryItem(item, normalized));
}

function operationLabel(item: DictionaryItem): string {
  return item.itemName || item.itemValue || item.itemCode;
}

export function operationAreasForArea(operationAreas: DictionaryItem[], areaCode: string): DictionaryItem[] {
  return operationAreas.filter((item) => item.itemValue === areaCode);
}

export function portAreasForPort(portAreas: DictionaryItem[], portCode: string): DictionaryItem[] {
  return portAreas.filter((item) => item.itemValue === portCode);
}

export function resolvePortHierarchy(
  rawCode: string | undefined | null,
  rawName: string | undefined | null,
  ports: DictionaryItem[],
  portAreas: DictionaryItem[],
  operationAreas: DictionaryItem[]
): PortHierarchySelection {
  const parts = splitPortParts(rawCode, rawName);
  const possibleOperation = parts.find((part) => isSulanghuText(part)) || [...parts].reverse().find((part) => findItem(operationAreas, part));
  let operation = possibleOperation ? findItem(operationAreas, possibleOperation) : undefined;
  if (!operation && [rawCode, rawName, ...parts].some(isSulanghuText)) {
    operation = operationAreas.find((item) => item.itemCode === "SULANGHU");
  }
  const possibleArea = parts.find((part) => findItem(portAreas, part));
  let area = possibleArea ? findItem(portAreas, possibleArea) : undefined;
  if (!area && operation?.itemValue) {
    area = portAreas.find((item) => item.itemCode === operation?.itemValue);
  }
  const possiblePort = parts.find((part) => findItem(ports, part));
  const port = possiblePort
    ? findItem(ports, possiblePort)
    : area?.itemValue
      ? ports.find((item) => item.itemCode === area?.itemValue)
      : undefined;
  const customValue = String(rawName || rawCode || "").trim();
  const displayName = area && operation
    ? `${area.itemName}/${operationLabel(operation)}`
    : area
      ? area.itemName
      : port
        ? port.itemName
        : isSulanghuText(customValue)
          ? SULANGHU_LABEL
          : customValue;

  return {
    portCode: port?.itemCode || "",
    areaCode: area?.itemCode || "",
    operationCode: operation?.itemCode || (port || area ? "" : String(rawCode || "").trim()),
    displayName,
    customValue
  };
}

export function formatSupplyPortDisplay(
  rawValue: string | undefined | null,
  portAreas: DictionaryItem[] = [],
  operationAreas: DictionaryItem[] = [],
  ports: DictionaryItem[] = []
): string {
  const text = String(rawValue ?? "").trim();
  if (!text) return "";
  const resolved = resolvePortHierarchy(text, text, ports, portAreas, operationAreas);
  return resolved.displayName || text;
}
