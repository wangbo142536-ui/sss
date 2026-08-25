import { ApiError, getAuthSession, uploadQualificationFile } from "@/services/authService";
import type { SupplierDirectoryItem, SupplierDirectoryListResponse, SupplierDirectoryStatus, SupplierQualification } from "../types/supplierDirectory";
import type {
  ShopCatalogSku,
  ShopQualitySelection,
  ShopQualitySelectionAuditEntry,
  ShopQualitySelectionSavePayload
} from "@/modules/shopManagement/types/shopProductCatalog";

type JsonRecord = Record<string, unknown>;

function isRecord(value: unknown): value is JsonRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function text(value: unknown): string {
  return typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
}

function number(value: unknown): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function list(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(text).filter(Boolean);
  const raw = text(value);
  if (!raw || raw === "--") return [];
  return raw.split(/[,，;；|]/).map((item) => item.trim()).filter(Boolean);
}

function status(value: unknown): SupplierDirectoryStatus {
  const normalized = text(value).toUpperCase();
  if (normalized === "ACTIVE" || normalized === "ENABLED") return "ACTIVE";
  if (normalized === "WARNING" || normalized === "DISABLED" || normalized === "INACTIVE") return "DISABLED";
  return normalized || "DISABLED";
}

function normalizeSupplier(value: unknown): SupplierDirectoryItem | null {
  if (!isRecord(value)) return null;
  const companyId = number(value.companyId);
  const name = text(value.name ?? value.companyName);
  if (!companyId || !name) return null;
  const averageRatingValue = value.averageRating ?? value.rating ?? value.score;
  const evaluationCount = number(value.evaluationCount);
  const averageRating = evaluationCount > 0 && Number.isFinite(Number(averageRatingValue))
    ? Number(Number(averageRatingValue).toFixed(1))
    : null;
  const positiveRateValue = value.positiveRate;
  return {
    companyId,
    id: text(value.id) || `SUP-${companyId}`,
    name,
    creditCode: text(value.creditCode ?? value.unifiedSocialCreditCode),
    logoFileId: text(value.logoFileId),
    logoUrl: text(value.logoUrl),
    introduction: text(value.introduction ?? value.companyIntroduction),
    servicePorts: list(value.servicePorts ?? value.port),
    categories: list(value.categories ?? value.category),
    status: status(value.status),
    skuCount: number(value.skuCount),
    categoryCount: number(value.categoryCount),
    contactName: text(value.contactName),
    contactPhone: text(value.contactPhone),
    contactEmail: text(value.contactEmail),
    averageRating,
    evaluationCount,
    positiveRate: evaluationCount > 0 && Number.isFinite(Number(positiveRateValue)) ? Number(Number(positiveRateValue).toFixed(0)) : null,
    reputationLevel: text(value.reputationLevel) || (evaluationCount > 0 ? "已评价" : "暂无评价")
  };
}

async function request(endpoint: string, init: RequestInit = {}): Promise<unknown> {
  const session = getAuthSession();
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {})
    }
  });
  const raw = await response.text();
  let payload: unknown = null;
  if (raw) {
    try { payload = JSON.parse(raw); } catch { payload = raw; }
  }
  if (!response.ok) {
    const message = isRecord(payload) ? text(payload.message ?? payload.error ?? payload.detail) : text(payload);
    throw new ApiError(message || `HTTP_${response.status}`, response.status, payload);
  }
  return payload;
}

export async function listSupplierDirectory(): Promise<SupplierDirectoryListResponse> {
  const items: SupplierDirectoryItem[] = [];
  const size = 200;
  let page = 1;
  let total = 0;
  do {
    const payload = await request(`/api/shop/suppliers?page=${page}&size=${size}`);
    const source = isRecord(payload) ? payload : {};
    const pageItems = (Array.isArray(source.items) ? source.items : [])
      .map(normalizeSupplier)
      .filter((item): item is SupplierDirectoryItem => Boolean(item));
    items.push(...pageItems);
    total = number(source.total) || items.length;
    if (!pageItems.length || items.length >= total) break;
    page += 1;
  } while (page <= Math.ceil(total / size));
  return { items, total };
}

export async function getSupplierDirectorySupplier(companyId: number): Promise<SupplierDirectoryItem> {
  const payload = await request(`/api/shop/suppliers?companyId=${encodeURIComponent(String(companyId))}&page=1&size=1`);
  const source = isRecord(payload) ? payload : {};
  const item = (Array.isArray(source.items) ? source.items : []).map(normalizeSupplier).find(Boolean);
  if (!item) throw new ApiError("SUPPLIER_NOT_FOUND", 404, payload);
  return item as SupplierDirectoryItem;
}

export async function updateSupplierDirectoryStatus(companyId: number, nextStatus: "ACTIVE" | "DISABLED"): Promise<void> {
  await request(`/api/shop/suppliers/${encodeURIComponent(String(companyId))}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status: nextStatus })
  });
}

export async function loadSupplierLogo(logoUrl: string): Promise<string> {
  const normalizedUrl = text(logoUrl);
  if (!normalizedUrl || !normalizedUrl.startsWith("/api/files/")) return normalizedUrl;
  const session = getAuthSession();
  const response = await fetch(normalizedUrl, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  });
  if (!response.ok) return "";
  return URL.createObjectURL(await response.blob());
}

export async function listSupplierDirectoryQualifications(companyId: number): Promise<SupplierQualification[]> {
  const payload = await request(`/api/shop/suppliers/${encodeURIComponent(String(companyId))}/qualifications`);
  const source = isRecord(payload) ? payload : {};
  return (Array.isArray(source.items) ? source.items : [])
    .filter(isRecord)
    .map((item) => ({
      qualificationId: number(item.qualificationId),
      companyId: number(item.companyId),
      fileName: text(item.fileName),
      fileUrl: text(item.fileUrl),
      qualificationType: text(item.qualificationType),
      title: text(item.title),
      description: text(item.description),
      contentType: text(item.contentType),
      updatedAt: text(item.updatedAt)
    }))
    .filter((item) => item.qualificationId > 0 && item.companyId === companyId && Boolean(item.fileUrl));
}

export async function loadSupplierQualificationImage(fileUrl: string): Promise<string> {
  const normalizedUrl = text(fileUrl);
  if (!normalizedUrl.startsWith("/api/shop/suppliers/")) return "";
  const session = getAuthSession();
  const response = await fetch(normalizedUrl, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  });
  if (!response.ok) throw new ApiError("企业证书图片加载失败", response.status, await response.text());
  return URL.createObjectURL(await response.blob());
}

async function resolveSupplierProductImage(value: unknown): Promise<string> {
  if (!isRecord(value)) return "";
  const images = Array.isArray(value.images) ? value.images.filter(isRecord) : [];
  const primary = images.find((image) => Boolean(image.primary ?? image.isPrimary)) ?? images[0];
  const source = text(value.thumbnailUrl ?? value.imageUrl ?? primary?.thumbnailUrl ?? primary?.imageUrl);
  if (!source || !source.startsWith("/api/files/")) return source;
  return loadSupplierLogo(source);
}

function normalizeSupplierProduct(value: unknown, index: number): ShopCatalogSku | null {
  if (!isRecord(value)) return null;
  const productName = text(value.productName);
  if (!productName) return null;
  const skuId = value.skuId ?? value.id;
  const specItems = Array.isArray(value.specifications)
    ? value.specifications.filter(isRecord).map((item, specIndex) => ({
        id: text(item.attributeId ?? item.id) || `supplier-spec-${specIndex + 1}`,
        key: text(item.key ?? item.attributeKey) || `specification_${specIndex + 1}`,
        name: text(item.name ?? item.attributeName) || "规格",
        value: text(item.value ?? item.attributeValue),
        unit: text(item.unit)
      }))
    : [];
  const specs = specItems.map((item) => item.value).filter(Boolean);
  return {
    id: text(skuId) || `supplier-product-${index}`,
    skuId: typeof skuId === "string" || typeof skuId === "number" ? skuId : undefined,
    productType: text(value.productType) || "MATERIAL",
    category: text(value.categoryName ?? value.categoryCode),
    categoryCode: text(value.categoryCode),
    categoryName: text(value.categoryName),
    productName,
    productDescription: text(value.productDescription),
    productTags: Array.isArray(value.productTags) ? value.productTags.map(text).filter(Boolean) : [],
    supplierSkuCode: text(value.supplierSkuCode),
    platformCode: text(value.platformCode),
    impaCode: text(value.impaCode),
    thumbnail: "",
    imageUrl: "",
    packing: text(value.packing),
    stock: number(value.stockQty),
    unit: text(value.unit ?? value.stockUnit),
    price: number(value.unitPrice),
    currency: text(value.currency) || "CNY",
    specs,
    specItems,
    leadTimeDays: number(value.leadTimeDays),
    deliveryArea: text(value.deliveryArea),
    brand: text(value.brand),
    barcode: text(value.barcode),
    codingStatus: text(value.codeStatus ?? value.codingStatus),
    listingStatus: text(value.shelfStatus ?? value.listingStatus)
  };
}

function normalizeQualitySelection(value: unknown): ShopQualitySelection | null {
  if (!isRecord(value)) return null;
  const skuId = number(value.skuId);
  const qualitySelectionId = number(value.qualitySelectionId);
  if (!skuId || !qualitySelectionId) return null;
  const auditTrail = (Array.isArray(value.auditTrail) ? value.auditTrail : [])
    .filter(isRecord)
    .map((item): ShopQualitySelectionAuditEntry => ({
      occurredAt: text(item.occurredAt),
      operatorUserId: number(item.operatorUserId),
      action: text(item.action),
      inspectionContent: text(item.inspectionContent),
      inspectionProcess: text(item.inspectionProcess),
      inspectionConclusion: text(item.inspectionConclusion),
      reportFileId: text(item.reportFileId),
      reportFileName: text(item.reportFileName)
    }));
  return {
    qualitySelectionId,
    companyId: number(value.companyId),
    skuId,
    inspectionTime: text(value.inspectionTime),
    inspectionContent: text(value.inspectionContent),
    inspectionProcess: text(value.inspectionProcess),
    reportFileId: text(value.reportFileId),
    reportFileName: text(value.reportFileName),
    reportUrl: text(value.reportUrl),
    inspectionConclusion: text(value.inspectionConclusion),
    auditTrail,
    status: text(value.status) || "ACTIVE",
    updatedAt: text(value.updatedAt)
  };
}

export async function listSupplierQualitySelections(companyId: number): Promise<ShopQualitySelection[]> {
  const payload = await request(`/api/shop/suppliers/${encodeURIComponent(String(companyId))}/quality-selections`);
  const source = isRecord(payload) ? payload : {};
  return (Array.isArray(source.items) ? source.items : [])
    .map(normalizeQualitySelection)
    .filter((item): item is ShopQualitySelection => Boolean(item));
}

export async function saveSupplierQualitySelection(
  companyId: number,
  skuId: number,
  payload: ShopQualitySelectionSavePayload
): Promise<ShopQualitySelection> {
  const response = await request(`/api/shop/suppliers/${encodeURIComponent(String(companyId))}/skus/${encodeURIComponent(String(skuId))}/quality-selection`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
  const normalized = normalizeQualitySelection(response);
  if (!normalized) throw new ApiError("QUALITY_SELECTION_RESPONSE_INVALID", 0, response);
  return normalized;
}

export async function uploadSupplierQualityReport(file: File): Promise<{ fileId: string; fileName: string }> {
  const uploaded = await uploadQualificationFile(file);
  return { fileId: String(uploaded.fileId), fileName: uploaded.name || file.name };
}

export async function downloadSupplierQualityReport(reportUrl: string, fileName: string): Promise<void> {
  const session = getAuthSession();
  const response = await fetch(reportUrl, { headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {} });
  if (!response.ok) throw new ApiError("检测报告下载失败", response.status, await response.text());
  const url = URL.createObjectURL(await response.blob());
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName || "海事严选检测报告";
  anchor.click();
  URL.revokeObjectURL(url);
}

export async function listSupplierDirectoryProducts(companyId: number): Promise<ShopCatalogSku[]> {
  const size = 200;
  const rows: ShopCatalogSku[] = [];
  let page = 1;
  let total = 0;
  let received = 0;
  do {
    const payload = await request(`/api/shop/skus?companyId=${encodeURIComponent(String(companyId))}&page=${page}&size=${size}`);
    const source = isRecord(payload) ? payload : {};
    const rawItems = Array.isArray(source.items) ? source.items : [];
    const pageRows = rawItems.map(normalizeSupplierProduct).filter((item): item is ShopCatalogSku => Boolean(item));
    const imageUrls = await Promise.all(rawItems.map(resolveSupplierProductImage));
    pageRows.forEach((row, index) => {
      row.thumbnail = imageUrls[index] || "";
      row.imageUrl = imageUrls[index] || "";
    });
    rows.push(...pageRows);
    received += rawItems.length;
    total = number(source.total) || rows.length;
    if (!rawItems.length || received >= total) break;
    page += 1;
  } while (true);
  return rows;
}
