import { getAuthSession } from "@/services/authService";

export type DictionaryType = {
  id: number;
  typeCode: string;
  typeName: string;
  description?: string | null;
  sortOrder: number;
  enabled: boolean;
};

export type DictionaryItem = {
  id: number;
  typeCode: string;
  itemCode: string;
  itemName: string;
  itemValue?: string | null;
  itemNameEn?: string | null;
  description?: string | null;
  sortOrder: number;
  enabled: boolean;
  builtIn: boolean;
};

export type DictionaryTypePayload = {
  typeCode?: string;
  typeName: string;
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
};

export type DictionaryItemPayload = {
  typeCode: string;
  itemCode: string;
  itemName: string;
  itemValue?: string;
  itemNameEn?: string;
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
};

const ADMIN_DICTIONARY_ENDPOINT = "/api/admin/dictionaries";
const PUBLIC_DICTIONARY_ENDPOINT = "/api/dictionaries";

export class DictionaryApiError extends Error {
  status: number;
  payload: unknown;

  constructor(message: string, status: number, payload: unknown) {
    super(message);
    this.name = "DictionaryApiError";
    this.status = status;
    this.payload = payload;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

function unwrapPayload(payload: unknown) {
  if (isRecord(payload) && "data" in payload) return payload.data;
  return payload;
}

async function requestJson(endpoint: string, init: RequestInit = { method: "GET" }): Promise<unknown> {
  const session = getAuthSession();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body) headers.set("Content-Type", "application/json");
  if (session?.token) headers.set("Authorization", `Bearer ${session.token}`);
  const response = await fetch(endpoint, { ...init, headers });
  const payload = await readJson(response);
  if (!response.ok) {
    const message = isRecord(payload) ? String(payload.message || payload.error || payload.code || "") : "";
    throw new DictionaryApiError(message || `HTTP_${response.status}`, response.status, payload);
  }
  return unwrapPayload(payload);
}

function readString(source: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "string") return value;
    if (typeof value === "number") return String(value);
  }
  return "";
}

function readNumber(source: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "number" && Number.isFinite(value)) return value;
    if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value);
  }
  return 0;
}

function readBoolean(source: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "boolean") return value;
    if (typeof value === "number") return value === 1;
    if (typeof value === "string") return value === "1" || value.toLowerCase() === "true";
  }
  return false;
}

function normalizeType(value: unknown): DictionaryType | null {
  if (!isRecord(value)) return null;
  const typeCode = readString(value, ["typeCode", "type_code", "code"]);
  if (!typeCode) return null;
  return {
    id: readNumber(value, ["id"]),
    typeCode,
    typeName: readString(value, ["typeName", "type_name", "name"]),
    description: readString(value, ["description"]),
    sortOrder: readNumber(value, ["sortOrder", "sort_order"]),
    enabled: readBoolean(value, ["enabled"])
  };
}

function normalizeItem(value: unknown): DictionaryItem | null {
  if (!isRecord(value)) return null;
  const id = readNumber(value, ["id"]);
  const itemCode = readString(value, ["itemCode", "item_code", "code"]);
  if (!id || !itemCode) return null;
  return {
    id,
    typeCode: readString(value, ["typeCode", "type_code"]),
    itemCode,
    itemName: readString(value, ["itemName", "item_name", "name"]),
    itemValue: readString(value, ["itemValue", "item_value", "value"]),
    itemNameEn: readString(value, ["itemNameEn", "item_name_en", "nameEn"]),
    description: readString(value, ["description"]),
    sortOrder: readNumber(value, ["sortOrder", "sort_order"]),
    enabled: readBoolean(value, ["enabled"]),
    builtIn: readBoolean(value, ["builtIn", "built_in"])
  };
}

export async function listDictionaryTypes(query: { keyword?: string; enabled?: boolean } = {}): Promise<DictionaryType[]> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("keyword", query.keyword.trim());
  if (query.enabled !== undefined) params.set("enabled", String(query.enabled));
  const endpoint = params.size ? `${ADMIN_DICTIONARY_ENDPOINT}/types?${params.toString()}` : `${ADMIN_DICTIONARY_ENDPOINT}/types`;
  const payload = await requestJson(endpoint);
  const source = Array.isArray(payload) ? payload : [];
  return source.map(normalizeType).filter((item): item is DictionaryType => Boolean(item));
}

export async function saveDictionaryType(payload: DictionaryTypePayload, originalTypeCode?: string): Promise<DictionaryType> {
  const endpoint = originalTypeCode
    ? `${ADMIN_DICTIONARY_ENDPOINT}/types/${encodeURIComponent(originalTypeCode)}`
    : `${ADMIN_DICTIONARY_ENDPOINT}/types`;
  const result = await requestJson(endpoint, {
    method: originalTypeCode ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });
  const normalized = normalizeType(result);
  if (!normalized) throw new DictionaryApiError("DICTIONARY_TYPE_RESPONSE_INVALID", 0, result);
  return normalized;
}

export async function deleteDictionaryType(typeCode: string): Promise<void> {
  await requestJson(`${ADMIN_DICTIONARY_ENDPOINT}/types/${encodeURIComponent(typeCode)}`, { method: "DELETE" });
}

export async function listDictionaryItems(query: { typeCode?: string; keyword?: string; enabled?: boolean } = {}): Promise<DictionaryItem[]> {
  const params = new URLSearchParams();
  if (query.typeCode?.trim()) params.set("typeCode", query.typeCode.trim());
  if (query.keyword?.trim()) params.set("keyword", query.keyword.trim());
  if (query.enabled !== undefined) params.set("enabled", String(query.enabled));
  const endpoint = params.size ? `${ADMIN_DICTIONARY_ENDPOINT}/items?${params.toString()}` : `${ADMIN_DICTIONARY_ENDPOINT}/items`;
  const payload = await requestJson(endpoint);
  const source = Array.isArray(payload) ? payload : [];
  return source.map(normalizeItem).filter((item): item is DictionaryItem => Boolean(item));
}

export async function listPublicDictionaryItems(typeCode: string): Promise<DictionaryItem[]> {
  const params = new URLSearchParams();
  params.set("typeCode", typeCode);
  const payload = await requestJson(`${PUBLIC_DICTIONARY_ENDPOINT}/items?${params.toString()}`);
  const source = Array.isArray(payload) ? payload : [];
  return source.map(normalizeItem).filter((item): item is DictionaryItem => Boolean(item));
}

export async function saveDictionaryItem(payload: DictionaryItemPayload, itemId?: number): Promise<DictionaryItem> {
  const endpoint = itemId
    ? `${ADMIN_DICTIONARY_ENDPOINT}/items/${encodeURIComponent(String(itemId))}`
    : `${ADMIN_DICTIONARY_ENDPOINT}/items`;
  const result = await requestJson(endpoint, {
    method: itemId ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });
  const normalized = normalizeItem(result);
  if (!normalized) throw new DictionaryApiError("DICTIONARY_ITEM_RESPONSE_INVALID", 0, result);
  return normalized;
}

export async function deleteDictionaryItem(itemId: number): Promise<void> {
  await requestJson(`${ADMIN_DICTIONARY_ENDPOINT}/items/${encodeURIComponent(String(itemId))}`, { method: "DELETE" });
}
