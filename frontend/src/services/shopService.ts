import { ApiError, getAuthSession } from "@/services/authService";

export const shopEndpoints = {
  profile: "/api/shop/profile",
  suppliers: "/api/shop/suppliers",
  skus: "/api/shop/skus",
  skuBatchUpsert: "/api/shop/skus/batch-upsert",
  skuDetail: (skuId: string | number) => `/api/shop/skus/${encodeURIComponent(String(skuId))}`,
  shelfStatus: (skuId: string | number) => `/api/shop/skus/${encodeURIComponent(String(skuId))}/shelf-status`,
  importPreview: "/api/shop/skus/import-preview",
  importTemplate: "/api/shop/skus/import-template",
  importConfirm: "/api/shop/skus/import-confirm",
  resolveException: (skuId: string | number) => `/api/shop/skus/${encodeURIComponent(String(skuId))}/exceptions/resolve`
} as const;

export type ShopQueryParams = Record<string, string | number | boolean | undefined | null>;
export type ShopPayload = Record<string, unknown>;

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function readErrorMessage(payload: unknown): string {
  if (!isRecord(payload)) return "";
  const value = payload.message ?? payload.error ?? payload.detail ?? payload.msg ?? payload.reason ?? payload.code;
  return typeof value === "string" && value.trim() ? value.trim() : "";
}

function buildQuery(params: ShopQueryParams = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

async function requestShopJson(endpoint: string, init: RequestInit = {}): Promise<unknown> {
  const session = getAuthSession();
  const isFormData = init.body instanceof FormData;
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      Accept: "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {})
    }
  });
  const payload = await readJson(response);

  if (!response.ok) {
    throw new ApiError(readErrorMessage(payload) || `HTTP_${response.status}`, response.status, payload);
  }

  return payload;
}

export function getShopProfile() {
  return requestShopJson(shopEndpoints.profile, { method: "GET" });
}

export function updateShopProfile(payload: ShopPayload) {
  return requestShopJson(shopEndpoints.profile, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function listShopSkus(params: ShopQueryParams = {}) {
  return requestShopJson(`${shopEndpoints.skus}${buildQuery(params)}`, { method: "GET" });
}

export function listShopSuppliers(params: ShopQueryParams = {}) {
  return requestShopJson(`${shopEndpoints.suppliers}${buildQuery(params)}`, { method: "GET" });
}

export function createShopSku(payload: ShopPayload) {
  return requestShopJson(shopEndpoints.skus, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateShopSku(skuId: string | number, payload: ShopPayload) {
  return requestShopJson(shopEndpoints.skuDetail(skuId), {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function batchUpsertShopSkus(payload: ShopPayload) {
  return requestShopJson(shopEndpoints.skuBatchUpsert, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function deleteShopSku(skuId: string | number) {
  return requestShopJson(shopEndpoints.skuDetail(skuId), { method: "DELETE" });
}

export function updateShopSkuShelfStatus(skuId: string | number, payload: ShopPayload) {
  return requestShopJson(shopEndpoints.shelfStatus(skuId), {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function previewShopSkuImport(formData: FormData) {
  return requestShopJson(shopEndpoints.importPreview, {
    method: "POST",
    body: formData
  });
}

export async function downloadShopSkuImportTemplate() {
  const session = getAuthSession();
  const response = await fetch(shopEndpoints.importTemplate, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  });
  if (!response.ok) throw new ApiError(`HTTP_${response.status}`, response.status, await response.text());
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "商品导入模板-物料与伙食.xlsx";
  link.click();
  URL.revokeObjectURL(url);
}

export function confirmShopSkuImport(payload: ShopPayload) {
  return requestShopJson(shopEndpoints.importConfirm, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function resolveShopSkuException(skuId: string | number, payload: ShopPayload) {
  return requestShopJson(shopEndpoints.resolveException(skuId), {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
