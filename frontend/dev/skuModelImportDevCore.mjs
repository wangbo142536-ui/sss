import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";
import zlib from "node:zlib";

const DEFAULT_BATCH_SIZE = 8;
const DEFAULT_TIMEOUT_MS = 90_000;
const HUOSHAN_CONFIG_PATH = "E:\\pro\\haishi\\huoshan.txt";

const SUPPLIER_HEADER_ALIASES = {
  supplierSkuCode: ["item no.", "item no", "itemno", "supplier item no", "supplieritemno"],
  rawNameSpec: ["name of commodity & specification", "name of commodity specification", "nameofcommodityspecification", "description"],
  unitPrice: ["fob warehouse(rmb)", "fob warehouse (rmb)", "price", "unit price"],
  packing: ["packing", "package", "package spec"],
  stock: ["stock", "qty", "quantity"],
  barcode: ["barcode", "bar code"]
};

const DEMAND_HEADER_ALIASES = {
  impaCode: ["impa", "impa code"],
  description: ["description"],
  sizeModel: ["size/model", "size model", "sizemodel", "specification"],
  quantity: ["inquiry qty.", "inquiry qty", "quantity", "qty"],
  unit: ["unit"],
  remarks: ["remarks", "remark"]
};

const xmlEntities = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'" };

function clearProxyVariables() {
  if (typeof process === "undefined") return;
  for (const key of ["HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"]) {
    delete process.env[key];
  }
}

function decodeXml(text = "") {
  return text
    .replace(/&#x([0-9a-f]+);/gi, (_, value) => String.fromCodePoint(Number.parseInt(value, 16)))
    .replace(/&#(\d+);/g, (_, value) => String.fromCodePoint(Number(value)))
    .replace(/&([a-z]+);/gi, (_, key) => xmlEntities[key] ?? `&${key};`);
}

function stripXmlTags(text = "") {
  return decodeXml(text.replace(/<[^>]+>/g, ""));
}

function normalizeHeader(value = "") {
  return value.toLowerCase().replace(/[\s._\-:/\\()（）&]+/g, "").trim();
}

function firstText(...values) {
  for (const value of values) {
    if (value === undefined || value === null) continue;
    const text = String(value).trim();
    if (text) return text;
  }
  return "";
}

function parseMoney(value) {
  const text = String(value ?? "").replace(/,/g, "");
  const match = text.match(/-?\d+(?:\.\d+)?/);
  return match ? Number(match[0]) : 0;
}

function parseNumber(value) {
  const text = String(value ?? "").replace(/,/g, "");
  const match = text.match(/-?\d+(?:\.\d+)?/);
  return match ? Number(match[0]) : 0;
}

function isStandardCode(value) {
  return /^\d{6}$/.test(String(value ?? "").trim());
}

async function readArkConfig(configPath = HUOSHAN_CONFIG_PATH) {
  const content = await fs.readFile(configPath, "utf8");
  const entries = {};
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const index = trimmed.indexOf("=");
    if (index < 0) continue;
    entries[trimmed.slice(0, index).trim()] = trimmed.slice(index + 1).trim().replace(/^["']|["']$/g, "");
  }
  const baseUrl = firstText(entries.ARK_BASE_URL, entries.BASE_URL, entries.OPENAI_BASE_URL).replace(/\/+$/, "");
  const model = firstText(entries.ARK_MODEL, entries.MODEL, entries.OPENAI_MODEL);
  const apiKey = firstText(entries.ARK_API_KEY, entries.API_KEY, entries.OPENAI_API_KEY);
  if (!baseUrl || !model || !apiKey) throw new Error("ARK_CONFIG_INCOMPLETE");
  return { baseUrl, model, apiKey, host: new URL(baseUrl).host };
}

function readZipEntries(buffer) {
  const entries = new Map();
  let eocdOffset = -1;
  const minOffset = Math.max(0, buffer.length - 66_000);
  for (let offset = buffer.length - 22; offset >= minOffset; offset -= 1) {
    if (buffer.readUInt32LE(offset) === 0x06054b50) {
      eocdOffset = offset;
      break;
    }
  }
  if (eocdOffset < 0) throw new Error("XLSX_ZIP_END_NOT_FOUND");

  const entryCount = buffer.readUInt16LE(eocdOffset + 10);
  let centralOffset = buffer.readUInt32LE(eocdOffset + 16);
  for (let index = 0; index < entryCount; index += 1) {
    if (buffer.readUInt32LE(centralOffset) !== 0x02014b50) break;
    const method = buffer.readUInt16LE(centralOffset + 10);
    const compressedSize = buffer.readUInt32LE(centralOffset + 20);
    const nameLength = buffer.readUInt16LE(centralOffset + 28);
    const extraLength = buffer.readUInt16LE(centralOffset + 30);
    const commentLength = buffer.readUInt16LE(centralOffset + 32);
    const localOffset = buffer.readUInt32LE(centralOffset + 42);
    const name = buffer.slice(centralOffset + 46, centralOffset + 46 + nameLength).toString("utf8");
    if (buffer.readUInt32LE(localOffset) === 0x04034b50) {
      const localNameLength = buffer.readUInt16LE(localOffset + 26);
      const localExtraLength = buffer.readUInt16LE(localOffset + 28);
      const dataStart = localOffset + 30 + localNameLength + localExtraLength;
      const data = buffer.slice(dataStart, dataStart + compressedSize);
      entries.set(name, method === 8 ? zlib.inflateRawSync(data) : data);
    }
    centralOffset += 46 + nameLength + extraLength + commentLength;
  }
  return entries;
}

export function parseSharedStrings(xml = "") {
  const strings = [];
  for (const match of xml.matchAll(/<si\b[\s\S]*?<\/si>/g)) {
    const node = match[0];
    const parts = [...node.matchAll(/<t\b[^>]*>([\s\S]*?)<\/t>/g)].map((item) => decodeXml(item[1]));
    strings.push(parts.length ? parts.join("") : stripXmlTags(node));
  }
  return strings;
}

function columnIndex(cellRef = "") {
  const letters = cellRef.match(/[A-Z]+/i)?.[0]?.toUpperCase() ?? "";
  let value = 0;
  for (const letter of letters) value = value * 26 + (letter.charCodeAt(0) - 64);
  return Math.max(0, value - 1);
}

export function parseSheetRows(xml = "", sharedStrings = []) {
  const rows = [];
  for (const rowMatch of xml.matchAll(/<row\b([^>]*)>([\s\S]*?)<\/row>/g)) {
    const rowAttrs = rowMatch[1];
    const rowXml = rowMatch[2];
    const rowNumber = Number(rowAttrs.match(/\br="(\d+)"/)?.[1] ?? rows.length + 1);
    const values = [];
    for (const cellMatch of rowXml.matchAll(/<c\b([^>]*)\/>|<c\b([^>]*)>([\s\S]*?)<\/c>/g)) {
      const attrs = cellMatch[1] ?? cellMatch[2] ?? "";
      const cellXml = cellMatch[3] ?? "";
      const ref = attrs.match(/(?:^|\s)r="([A-Z]+\d+)"/i)?.[1] ?? "";
      const type = attrs.match(/(?:^|\s)t="([^"]+)"/)?.[1] ?? "";
      const index = columnIndex(ref);
      let value = "";
      if (type === "inlineStr") {
        value = stripXmlTags(cellXml.match(/<is\b[^>]*>([\s\S]*?)<\/is>/)?.[1] ?? "");
      } else {
        const raw = decodeXml(cellXml.match(/<v\b[^>]*>([\s\S]*?)<\/v>/)?.[1] ?? "");
        value = type === "s" ? (sharedStrings[Number(raw)] ?? "") : raw;
      }
      values[index] = String(value ?? "").trim();
    }
    if (values.some(Boolean)) rows.push({ rowNumber, values });
  }
  return rows;
}

function findHeader(rows) {
  let best = null;
  for (const row of rows.slice(0, 40)) {
    const normalized = row.values.map(normalizeHeader);
    const supplierScore = ["supplierSkuCode", "rawNameSpec"].filter((key) =>
      SUPPLIER_HEADER_ALIASES[key].some((alias) => normalized.includes(normalizeHeader(alias)))
    ).length;
    const demandScore = ["description", "sizeModel"].filter((key) =>
      DEMAND_HEADER_ALIASES[key].some((alias) => normalized.includes(normalizeHeader(alias)))
    ).length;
    const score = Math.max(supplierScore, demandScore);
    if (!best || score > best.score) {
      best = { score, documentType: supplierScore >= demandScore ? "SUPPLIER_QUOTATION" : "DEMAND_INQUIRY", row };
    }
  }
  if (!best || best.score < 2) throw new Error("XLSX_HEADER_NOT_FOUND");
  return best;
}

function resolveHeaderMap(headerValues, aliases) {
  const normalized = headerValues.map(normalizeHeader);
  const result = {};
  Object.entries(aliases).forEach(([key, names]) => {
    const foundIndex = names.map(normalizeHeader).map((name) => normalized.indexOf(name)).find((index) => index >= 0);
    if (foundIndex !== undefined && foundIndex >= 0) result[key] = foundIndex;
  });
  return result;
}

export function parseXlsxBuffer(buffer) {
  const entries = readZipEntries(buffer);
  const sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml")?.toString("utf8") ?? "");
  const sheetName = [...entries.keys()].find((name) => /^xl\/worksheets\/sheet\d+\.xml$/i.test(name));
  if (!sheetName) throw new Error("XLSX_SHEET_NOT_FOUND");
  const rows = parseSheetRows(entries.get(sheetName)?.toString("utf8") ?? "", sharedStrings);
  const header = findHeader(rows);
  const headerValues = header.row.values.map((value) => value || "");
  const aliases = header.documentType === "SUPPLIER_QUOTATION" ? SUPPLIER_HEADER_ALIASES : DEMAND_HEADER_ALIASES;
  const headerMap = resolveHeaderMap(headerValues, aliases);
  const dataRows = rows.filter((row) => row.rowNumber > header.row.rowNumber);
  const items = dataRows.map((row) => {
    const rawColumns = {};
    headerValues.forEach((headerText, index) => {
      const key = headerText || `Column ${index + 1}`;
      const value = row.values[index] || "";
      if (key || value) rawColumns[key] = value;
    });
    const read = (key) => {
      const index = headerMap[key];
      return index === undefined ? "" : String(row.values[index] || "").trim();
    };
    const source = { sourceRowNo: row.rowNumber, rawColumns };
    if (header.documentType === "SUPPLIER_QUOTATION") {
      Object.assign(source, {
        supplierSkuCode: read("supplierSkuCode"),
        rawNameSpec: read("rawNameSpec") || firstText(row.values[2], row.values[1]),
        price: read("unitPrice"),
        packing: read("packing"),
        stock: read("stock"),
        barcode: read("barcode")
      });
    } else {
      Object.assign(source, {
        impaCode: read("impaCode"),
        description: read("description"),
        sizeModel: read("sizeModel"),
        quantity: read("quantity"),
        unit: read("unit"),
        remarks: read("remarks")
      });
    }
    return source;
  }).filter((row) => firstText(row.rawNameSpec, row.description, row.supplierSkuCode, row.impaCode));
  return { documentType: header.documentType, headerRowIndex: header.row.rowNumber, rows: items };
}

function localCleanRow(row) {
  const rawName = firstText(row.rawNameSpec, row.description, row.supplierSkuCode, row.impaCode);
  const rawSpec = firstText(row.sizeModel, row.description);
  const attributes = [];
  const addAttribute = (key, name, value, unit = "") => {
    const text = String(value ?? "").trim();
    if (!text || attributes.some((item) => item.key === key && item.value === text)) return;
    attributes.push({ key, name, value: text, unit });
  };
  const sizeMatches = [
    ...rawName.matchAll(/\b\d+(?:\.\d+)?\s*(?:''|["”])\s*\/\s*\d+(?:\.\d+)?\s*(?:mm|cm|m)\b/gi),
    ...rawName.matchAll(/\b\d+(?:\.\d+)?\s*(?:["”]|inch|inches|mm|cm|m)\b/gi),
    ...rawName.matchAll(/\b\d+(?:\.\d+)?\s*[xX*]\s*\d+(?:\.\d+)?(?:\s*[xX*]\s*\d+(?:\.\d+)?)?\s*(?:mm|cm|m)?\b/g)
  ].map((match) => match[0].trim());
  if (sizeMatches.length) addAttribute("size", "Size", Array.from(new Set(sizeMatches)).join(" / "));
  const material = rawName.match(/\bmaterial\s*[:：]?\s*([^\r\n,;，；]+)/i)?.[1];
  if (material) addAttribute("material", "Material", material);
  const color = rawName.match(/\b(red|blue|green|white|black|yellow|orange|grey|gray|silver)\b/i)?.[1];
  if (color) addAttribute("color", "Color", color);
  if (rawSpec && rawSpec !== rawName) addAttribute("specification", "Specification", rawSpec);
  if (row.packing) addAttribute("packing", "Packing", row.packing);
  if (row.barcode) addAttribute("barcode", "Barcode", row.barcode);
  let cleanName = rawName
    .replace(/name\s*of\s*commodity\s*&?\s*specification\s*[:：]?/gi, " ")
    .replace(/specification\s*[:：].*$/gi, " ")
    .replace(/\b\d+(?:\.\d+)?\s*(?:''|["”])\s*\/\s*\d+(?:\.\d+)?\s*(?:mm|cm|m)\b/gi, " ")
    .replace(/material\s*[:：]?\s*[^\r\n,;，；]+/gi, " ")
    .replace(/\b\d+(?:\.\d+)?\s*(?:["”]|inch|inches|mm|cm|m)\b/gi, " ")
    .replace(/\b\d+(?:\.\d+)?\s*[xX*]\s*\d+(?:\.\d+)?(?:\s*[xX*]\s*\d+(?:\.\d+)?)?\s*(?:mm|cm|m)?\b/g, " ")
    .replace(/[()[\]{}【】]/g, " ")
    .replace(/\s{2,}/g, " ")
    .trim();
  if (!cleanName || cleanName.length < 3) cleanName = rawName;
  return { cleanName, parsedAttributes: attributes };
}

function buildPromptRows(rows) {
  return rows.map((row, index) => ({
    rowId: String(index + 1),
    sourceRowNo: row.sourceRowNo,
    itemNo: firstText(row.supplierSkuCode, row.impaCode),
    rawName: firstText(row.rawNameSpec, row.description),
    rawSpec: firstText(row.sizeModel, row.description),
    packing: row.packing || "",
    barcode: row.barcode || ""
  }));
}

function stripJsonFence(text) {
  const trimmed = String(text ?? "").trim();
  const fenced = trimmed.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i);
  return fenced ? fenced[1].trim() : trimmed;
}

function parseJsonFromModel(text) {
  const clean = stripJsonFence(text);
  try {
    return JSON.parse(clean);
  } catch {
    const objectStart = clean.indexOf("{");
    const objectEnd = clean.lastIndexOf("}");
    if (objectStart >= 0 && objectEnd > objectStart) return JSON.parse(clean.slice(objectStart, objectEnd + 1));
    const arrayStart = clean.indexOf("[");
    const arrayEnd = clean.lastIndexOf("]");
    if (arrayStart >= 0 && arrayEnd > arrayStart) return JSON.parse(clean.slice(arrayStart, arrayEnd + 1));
    throw new Error("MODEL_JSON_PARSE_FAILED");
  }
}

async function fetchWithTimeout(url, init, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function callArkBatch(rows, config, timeoutMs) {
  const response = await fetchWithTimeout(`${config.baseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${config.apiKey}`
    },
    body: JSON.stringify({
      model: config.model,
      temperature: 0.1,
      max_tokens: Math.min(2200, Math.max(500, rows.length * 260)),
      messages: [
        {
          role: "system",
          content:
            "Return compact JSON only. Extract supplier SKU data. cleanName must be a short product family name. Put size, material, color, model, packing, barcode and parameters into parsedAttributes. Do not invent IMPA codes."
        },
        {
          role: "user",
          content: JSON.stringify({
            schema:
              "{\"items\":[{\"rowId\":\"1\",\"cleanName\":\"Flat Nose Plier\",\"parsedAttributes\":[{\"key\":\"size\",\"name\":\"Size\",\"value\":\"6 inch / 160mm\",\"unit\":\"\"}],\"impaCode\":\"\",\"categoryCode\":\"\",\"categoryName\":\"\",\"matchReason\":\"\",\"confidenceLevel\":\"HIGH|MEDIUM|LOW\"}]}",
            rows: buildPromptRows(rows)
          })
        }
      ]
    })
  }, timeoutMs);
  const text = await response.text();
  if (!response.ok) {
    const error = new Error(`HTTP_${response.status}`);
    error.status = response.status;
    error.body = text.slice(0, 400);
    throw error;
  }
  const payload = JSON.parse(text);
  const content = payload?.choices?.[0]?.message?.content ?? "";
  const parsed = parseJsonFromModel(content);
  const items = Array.isArray(parsed) ? parsed : Array.isArray(parsed.items) ? parsed.items : [];
  return items.filter((item) => item && typeof item === "object");
}

async function callArkNameBatch(rows, config, timeoutMs) {
  const response = await fetchWithTimeout(`${config.baseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${config.apiKey}`
    },
    body: JSON.stringify({
      model: config.model,
      temperature: 0,
      max_tokens: Math.min(1600, Math.max(300, rows.length * 120)),
      messages: [
        {
          role: "system",
          content:
            "Return compact JSON only. Extract only the product family name from supplier SKU raw names. Remove size, dimensions, material, finish, color, packing, barcode, stock, price and handle details. Do not change supplierSkuCode."
        },
        {
          role: "user",
          content: JSON.stringify({
            schema:
              "{\"items\":[{\"supplierSkuCode\":\"100115\",\"importRowNumber\":3,\"productName\":\"Flat Nose Plier\"}]}",
            examples: [
              {
                supplierSkuCode: "100115",
                rawName: "6\"/160mm Flat Nose Plier material: carbon steel drop-forged hardened nickle-plated finish Bi-matel PP+TPR handle",
                productName: "Flat Nose Plier"
              }
            ],
            rows
          })
        }
      ]
    })
  }, timeoutMs);
  const text = await response.text();
  if (!response.ok) {
    const error = new Error(`HTTP_${response.status}`);
    error.status = response.status;
    error.body = text.slice(0, 400);
    throw error;
  }
  const payload = JSON.parse(text);
  const content = payload?.choices?.[0]?.message?.content ?? "";
  const parsed = parseJsonFromModel(content);
  const items = Array.isArray(parsed) ? parsed : Array.isArray(parsed.items) ? parsed.items : [];
  return items.filter((item) => item && typeof item === "object");
}

async function recognizeRows(rows, options = {}) {
  clearProxyVariables();
  const config = await readArkConfig(options.configPath);
  const env = typeof process === "undefined" ? {} : process.env;
  const batchSize = Number(options.batchSize || env.LOCAL_SKU_IMPORT_BATCH_SIZE || DEFAULT_BATCH_SIZE);
  const timeoutMs = Number(options.timeoutMs || env.LOCAL_SKU_IMPORT_TIMEOUT_MS || DEFAULT_TIMEOUT_MS);
  const results = [];
  const diagnostics = { host: config.host, model: config.model, batchCount: 0, failedBatchCount: 0 };
  for (let offset = 0; offset < rows.length; offset += batchSize) {
    const batch = rows.slice(offset, offset + batchSize);
    diagnostics.batchCount += 1;
    try {
      const modelItems = await callArkBatch(batch, config, timeoutMs);
      batch.forEach((row, index) => {
        const modelItem = modelItems.find((item) => String(item.rowId) === String(index + 1)) || modelItems[index] || null;
        results.push({ row, modelItem, modelStatus: modelItem ? "READY" : "INVALID_RESPONSE", modelReason: modelItem ? "" : "MODEL_EMPTY_ROW" });
      });
    } catch (error) {
      diagnostics.failedBatchCount += 1;
      const modelStatus = error?.name === "AbortError" ? "TIMEOUT" : String(error?.message || "").startsWith("HTTP_") ? "HTTP_ERROR" : "API_ERROR";
      batch.forEach((row) => results.push({ row, modelItem: null, modelStatus, modelReason: error?.message || "MODEL_CALL_FAILED" }));
    }
  }
  return { results, diagnostics };
}

export async function recognizeProductNamesBySupplierSku(rows, options = {}) {
  clearProxyVariables();
  const config = await readArkConfig(options.configPath);
  const env = typeof process === "undefined" ? {} : process.env;
  const batchSize = Number(options.batchSize || env.LOCAL_SKU_NAME_BATCH_SIZE || 12);
  const timeoutMs = Number(options.timeoutMs || env.LOCAL_SKU_NAME_TIMEOUT_MS || DEFAULT_TIMEOUT_MS);
  const items = [];
  const warnings = [];
  const diagnostics = { host: config.host, model: config.model, batchCount: 0, failedBatchCount: 0 };
  const seen = new Set();
  rows.forEach((row) => {
    if (!row?.supplierSkuCode) warnings.push({ type: "MISSING_SUPPLIER_SKU", importRowNumber: row?.importRowNumber || 0 });
    if (row?.supplierSkuCode && seen.has(row.supplierSkuCode)) warnings.push({ type: "DUPLICATE_SUPPLIER_SKU", supplierSkuCode: row.supplierSkuCode });
    if (row?.supplierSkuCode) seen.add(row.supplierSkuCode);
  });

  const usableRows = rows
    .filter((row) => row?.rawName || row?.originalName)
    .map((row) => ({
      supplierSkuCode: String(row.supplierSkuCode || ""),
      importRowNumber: Number(row.importRowNumber || 0),
      rawName: String(row.rawName || row.originalName || "")
    }));

  for (let offset = 0; offset < usableRows.length; offset += batchSize) {
    const batch = usableRows.slice(offset, offset + batchSize);
    diagnostics.batchCount += 1;
    try {
      const modelItems = await callArkNameBatch(batch, config, timeoutMs);
      modelItems.forEach((item) => {
        const productName = firstText(item.productName, item.cleanName, item.name);
        if (!productName) return;
        items.push({
          supplierSkuCode: firstText(item.supplierSkuCode),
          importRowNumber: Number(item.importRowNumber || 0),
          productName,
          status: "READY"
        });
      });
    } catch (error) {
      diagnostics.failedBatchCount += 1;
      batch.forEach((row) => {
        items.push({
          supplierSkuCode: row.supplierSkuCode,
          importRowNumber: row.importRowNumber,
          productName: "",
          status: error?.name === "AbortError" ? "TIMEOUT" : "API_ERROR",
          reason: error?.message || "MODEL_NAME_EXTRACTION_FAILED"
        });
      });
    }
  }

  return { items, warnings, diagnostics };
}

function normalizeModelAttribute(item) {
  if (!item || typeof item !== "object") return null;
  const key = firstText(item.key, item.name).toLowerCase().replace(/\s+/g, "_") || "attribute";
  const name = firstText(item.name, item.key) || "Attribute";
  const value = firstText(item.value, item.rawText);
  const unit = firstText(item.unit);
  if (!value) return null;
  return { key, name, value, unit };
}

function buildPreviewItem(source, modelItem, modelStatus, modelReason, documentType) {
  const local = localCleanRow(source);
  const modelCleanName = firstText(modelItem?.cleanName, modelItem?.productName);
  const modelAttributes = Array.isArray(modelItem?.parsedAttributes) ? modelItem.parsedAttributes.map(normalizeModelAttribute).filter(Boolean) : [];
  const parsedAttributes = modelAttributes.length ? modelAttributes : local.parsedAttributes;
  const rawCode = firstText(source.impaCode, source.supplierSkuCode);
  const modelImpaCode = firstText(modelItem?.impaCode, modelItem?.platformCode);
  const platformCode = isStandardCode(rawCode) ? rawCode : modelImpaCode;
  const codeStatus = isStandardCode(rawCode) ? "CODE_MATCH" : platformCode ? "SPEC_MATCHED" : "PENDING_EXCEPTION";
  const productName = modelCleanName || local.cleanName;
  const exceptionReason =
    codeStatus === "PENDING_EXCEPTION"
      ? modelStatus === "READY"
        ? "模型未返回可用编码，需人工确认"
        : "模型调用未完成，已保留原始行待处理"
      : "";
  const selectedRecommendationSource = modelStatus === "READY" && (modelCleanName || modelImpaCode) ? "MODEL" : "NONE";
  const attributeSummary = parsedAttributes.map((item) => `${item.name}: ${item.value}${item.unit ? ` ${item.unit}` : ""}`).join(" | ");
  const supplierSkuCode = firstText(source.supplierSkuCode, source.impaCode, `ROW-${source.sourceRowNo}`);
  return {
    previewRowId: `dev-${source.sourceRowNo}`,
    sourceRowNo: source.sourceRowNo,
    productType: "MATERIAL",
    categoryCode: firstText(modelItem?.categoryCode),
    categoryName: firstText(modelItem?.categoryName),
    platformCode,
    impaCode: platformCode,
    supplierSkuCode,
    productName,
    cleanName: productName,
    rawName: firstText(source.rawNameSpec, source.description),
    rawSpec: firstText(source.sizeModel, source.description),
    specifications: parsedAttributes,
    parsedAttributes,
    attributeSummary,
    stockQty: parseNumber(source.stock || source.quantity),
    stockUnit: firstText(source.unit),
    leadTimeDays: 0,
    deliveryArea: "",
    imageUrl: "",
    thumbnailUrl: "",
    monthlySales: 0,
    unitPrice: parseMoney(source.price),
    currency: "CNY",
    brand: "",
    unit: firstText(source.unit),
    packageSpec: firstText(source.packing),
    barcode: firstText(source.barcode),
    shelfStatus: "OFF_SHELF",
    codeStatus,
    exceptionReason,
    importBatchId: "dev-local",
    importRowNumber: source.sourceRowNo,
    rawColumns: source.rawColumns,
    documentType,
    modelRecommendation: {
      available: modelStatus === "READY",
      status: modelStatus,
      reason: modelReason || firstText(modelItem?.matchReason),
      matchReason: modelReason || firstText(modelItem?.matchReason),
      confidenceLevel: firstText(modelItem?.confidenceLevel)
    },
    selectedRecommendationSource,
    reviewRequired: codeStatus === "PENDING_EXCEPTION" || firstText(modelItem?.confidenceLevel).toUpperCase() !== "HIGH"
  };
}

export async function buildLocalSkuModelImportPreviewFromBuffer(buffer, options = {}) {
  const parsed = parseXlsxBuffer(buffer);
  const env = typeof process === "undefined" ? {} : process.env;
  const maxRows = Number(options.maxRows || env.LOCAL_SKU_IMPORT_MAX_ROWS || 0);
  const rows = maxRows > 0 ? parsed.rows.slice(0, maxRows) : parsed.rows;
  const { results, diagnostics } = await recognizeRows(rows, options);
  const items = results.map((result) => buildPreviewItem(result.row, result.modelItem, result.modelStatus, result.modelReason, parsed.documentType));
  const successCount = items.filter((item) => item.codeStatus !== "PENDING_EXCEPTION").length;
  const exceptionCount = items.length - successCount;
  return {
    localOnly: true,
    batchId: `dev-local-${Date.now()}`,
    status: exceptionCount ? "PENDING_EXCEPTION" : "READY",
    documentType: parsed.documentType,
    headerRowIndex: parsed.headerRowIndex,
    totalCount: items.length,
    successCount,
    exceptionCount,
    items,
    diagnostics
  };
}

export async function buildLocalSkuModelImportPreviewFromFile(filePath, options = {}) {
  const buffer = await fs.readFile(filePath);
  return buildLocalSkuModelImportPreviewFromBuffer(buffer, options);
}

if (typeof process !== "undefined" && process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const filePath = process.argv[2] || path.resolve("..", "tmp", "provision_analysis", "物料报价单.xlsx");
  const result = await buildLocalSkuModelImportPreviewFromFile(filePath);
  const first = result.items[0];
  console.log(JSON.stringify({
    configRead: true,
    host: result.diagnostics.host,
    model: result.diagnostics.model,
    totalCount: result.totalCount,
    successCount: result.successCount,
    exceptionCount: result.exceptionCount,
    failedBatchCount: result.diagnostics.failedBatchCount,
    sample: first
      ? {
          sourceRowNo: first.sourceRowNo,
          rawName: first.rawName,
          productName: first.productName,
          specs: first.specifications,
          modelStatus: first.modelRecommendation.status,
          codeStatus: first.codeStatus,
          exceptionReason: first.exceptionReason
        }
      : null
  }, null, 2));
}
