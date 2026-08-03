import { ApiError, getAuthSession } from "@/services/authService";
import type {
  FoodComparison,
  FoodComparisonSettings,
  FoodDemandDetail,
  FoodDemandSavePayload,
  FoodDemandSummary,
  FoodEvaluation,
  FoodImportPreview,
  FoodInquiry,
  FoodMatchPreview,
  FoodOrderDetail,
  FoodOrderSummary,
  FoodSupplierOrderSummary,
  FoodQuote,
  FoodSettlement,
  FoodSupplier
} from "../types";

const BUYER = "/api/procurement/food";
const SUPPLIER = "/api/supplier/food";

async function readPayload(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    const value = JSON.parse(text) as Record<string, unknown>;
    return value && typeof value === "object" && "data" in value ? value.data : value;
  } catch {
    return text;
  }
}

async function request<T>(endpoint: string, init: RequestInit = {}): Promise<T> {
  const session = getAuthSession();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (!(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
  if (session?.token) headers.set("Authorization", `Bearer ${session.token}`);
  const response = await fetch(endpoint, { ...init, headers });
  const payload = await readPayload(response);
  if (!response.ok) {
    const reason = typeof payload === "string"
      ? payload
      : String((payload as Record<string, unknown> | null)?.message || `HTTP_${response.status}`);
    throw new ApiError(reason, response.status, payload);
  }
  return payload as T;
}

function queryPath(path: string, query: Record<string, string | undefined>) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value?.trim()) params.set(key, value.trim());
  });
  return params.size ? `${path}?${params}` : path;
}

export function previewFoodDemand(file: File, sheetName?: string) {
  const formData = new FormData();
  formData.append("file", file);
  if (sheetName) formData.append("sheetName", sheetName);
  return request<FoodMatchPreview>(`${BUYER}/demands/match-preview`, { method: "POST", body: formData });
}

export function saveFoodDemand(payload: FoodDemandSavePayload) {
  const endpoint = payload.demandId ? `${BUYER}/demands/${payload.demandId}` : `${BUYER}/demands`;
  return request<{ demandId: number; demandNo: string; inquiryNo?: string; status: string; supplierCount: number }>(endpoint, {
    method: payload.demandId ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });
}

export function listFoodDemands(keyword = "", status = "") {
  return request<FoodDemandSummary[]>(queryPath(`${BUYER}/demands`, { keyword, status }));
}

export function getFoodDemand(id: number | string) {
  return request<FoodDemandDetail>(`${BUYER}/demands/${id}`);
}

export function listFoodSuppliers() {
  return request<FoodSupplier[]>(`${BUYER}/suppliers`);
}

export function sendFoodInquiry(demandId: number, supplierCompanyIds: number[], quoteValidityDays = 3) {
  return request<{ demandId: number; supplierCount: number; status: string }>(`${BUYER}/demands/${demandId}/send-inquiry`, {
    method: "POST",
    body: JSON.stringify({ supplierCompanyIds, quoteValidityDays })
  });
}

export function listBuyerFoodInquiries(keyword = "", status = "") {
  return request<FoodInquiry[]>(queryPath(`${BUYER}/inquiries`, { keyword, status }));
}

export function listSupplierFoodInquiries(keyword = "", status = "") {
  return request<FoodInquiry[]>(queryPath(`${SUPPLIER}/inquiries`, { keyword, status }));
}

export function getBuyerFoodQuote(id: number) {
  return request<FoodQuote>(`${BUYER}/quotes/${id}`);
}

export function getSupplierFoodQuote(id: number) {
  return request<FoodQuote>(`${SUPPLIER}/quotes/${id}`);
}

export function saveSupplierFoodQuote(quote: FoodQuote) {
  return request<FoodQuote>(`${SUPPLIER}/quotes/${quote.quoteId}/draft`, {
    method: "PUT",
    body: JSON.stringify({
      items: quote.items.map((item) => ({
        quoteItemId: item.quoteItemId,
        quotedQuantity: item.requestedQuantity,
        unitPrice: item.unitPrice,
        availability: "AVAILABLE",
        supplierRemark: item.supplierRemark,
        priceSource: item.priceSource || "MANUAL"
      }))
    })
  });
}

export function virtualFillFoodQuote(quoteId: number, overwriteExisting = false) {
  return request<{ quoteId: number; filledCount: number; preservedCount: number; priceSource: string }>(
    `${SUPPLIER}/quotes/${quoteId}/virtual-fill`,
    { method: "POST", body: JSON.stringify({ overwriteExisting }) }
  );
}

export function submitFoodQuote(quoteId: number) {
  return request<FoodQuote>(`${SUPPLIER}/quotes/${quoteId}/submit`, { method: "POST" });
}

export async function exportFoodQuote(quoteId: number) {
  const session = getAuthSession();
  const response = await fetch(`${SUPPLIER}/quotes/${quoteId}/export`, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  });
  if (!response.ok) throw new ApiError("FOOD_QUOTE_EXPORT_FAILED", response.status, await readPayload(response));
  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") || "";
  const matched = disposition.match(/filename\*?=(?:UTF-8''|\")?([^\";]+)/i);
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = decodeURIComponent(matched?.[1] || `food-quote-${quoteId}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(link.href);
}

export function previewFoodQuoteImport(quoteId: number, file: File, sheetName?: string) {
  const formData = new FormData();
  formData.append("file", file);
  if (sheetName) formData.append("sheetName", sheetName);
  return request<FoodImportPreview>(`${SUPPLIER}/quotes/${quoteId}/import-preview`, { method: "POST", body: formData });
}

export function commitFoodQuoteImport(quoteId: number, batchId: number) {
  return request<{ batchId: number; quoteId: number; updatedCount: number; versionNo: number }>(
    `${SUPPLIER}/quotes/${quoteId}/imports/${batchId}/commit`, { method: "POST" }
  );
}

export function getFoodComparison(demandId: number) {
  return request<FoodComparison>(`${BUYER}/demands/${demandId}/comparison`);
}

export function saveFoodComparisonSettings(demandId: number, settings: FoodComparisonSettings) {
  return request<FoodComparisonSettings>(`${BUYER}/demands/${demandId}/comparison-settings`, {
    method: "PUT",
    body: JSON.stringify(settings)
  });
}

export function saveFoodComparisonItems(demandId: number, payload: {
  items: Array<{
    quoteItemId: number;
    demandItemId: number;
    requestedQuantity: number;
    quotedQuantity: number;
    unitPrice: number;
    remark?: string;
  }>;
}) {
  return request<{ demandId: number; updatedCount: number }>(`${BUYER}/demands/${demandId}/comparison-items`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function exportFoodComparison(demandId: number, quoteItemIds: number[]) {
  const session = getAuthSession();
  const params = new URLSearchParams();
  quoteItemIds.forEach((quoteItemId) => params.append("quoteItemIds", String(quoteItemId)));
  const response = await fetch(`${BUYER}/demands/${demandId}/comparison-export?${params}`, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  });
  if (!response.ok) throw new ApiError("FOOD_COMPARISON_EXPORT_FAILED", response.status, await readPayload(response));
  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") || "";
  const matched = disposition.match(/filename\*?=(?:UTF-8''|\")?([^\";]+)/i);
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = decodeURIComponent(matched?.[1] || `food-comparison-${demandId}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(link.href);
}

export function importFoodComparison(demandId: number, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request<{ demandId: number; updatedCount: number }>(`${BUYER}/demands/${demandId}/comparison-import`, {
    method: "POST",
    body: formData
  });
}

export function createFoodOrder(
  demandId: number,
  strategyType: string,
  selectedSupplierCompanyId?: number,
  selectedItems: Array<{ demandItemId: number; quoteItemId: number }> = [],
  allowPartial = false,
  orderInfo: {
    requiredDeliveryTime?: string;
    deliveryAddress?: string;
    deliveryContactName?: string;
    deliveryContactPhone?: string;
    deliveryContactEmail?: string;
    defaultPackagingMethod?: string;
    buyerRemark?: string;
  } = {}
) {
  return request<{ orderId: number; orderNo: string; status: string; totalAmount: number }>(`${BUYER}/demands/${demandId}/purchase-orders`, {
    method: "POST",
    body: JSON.stringify({ strategyType, allowPartial, selectedSupplierCompanyId, selectedItems, ...orderInfo })
  });
}

export function listBuyerFoodOrders(keyword = "", status = "") {
  return request<FoodOrderSummary[]>(queryPath(`${BUYER}/purchase-orders`, { keyword, status }));
}

export function listSupplierFoodOrders(keyword = "", status = "") {
  return request<FoodSupplierOrderSummary[]>(queryPath(`${SUPPLIER}/purchase-orders`, { keyword, status }));
}

export function getFoodOrder(id: number, supplier = false) {
  return request<FoodOrderDetail>(`${supplier ? SUPPLIER : BUYER}/purchase-orders/${id}`);
}

export function actionSupplierFoodOrder(orderId: number, supplierOrderId: number, targetStatus: string, extra: Record<string, unknown> = {}) {
  return request<FoodOrderDetail>(`${SUPPLIER}/purchase-orders/${orderId}/supplier-orders/${supplierOrderId}/action`, {
    method: "POST",
    body: JSON.stringify({ targetStatus, ...extra })
  });
}

export function listFoodSettlements(supplier = false, status = "") {
  return request<FoodSettlement[]>(queryPath(`${supplier ? SUPPLIER : BUYER}/settlements`, { status }));
}

export function actionFoodSettlement(
  id: number,
  targetStatus: string,
  supplier = false,
  invoice: string | {
    invoiceNo?: string;
    actualAmount?: number;
    invoiceAttachments?: Array<{ fileId?: string; fileName: string; fileUrl: string }>;
  } = ""
) {
  const payload = typeof invoice === "string" ? { invoiceNo: invoice } : invoice;
  return request<FoodSettlement>(`${supplier ? SUPPLIER : BUYER}/settlements/${id}/action`, {
    method: "POST",
    body: JSON.stringify({ targetStatus, ...payload })
  });
}

export function listFoodEvaluations(status = "") {
  return request<FoodEvaluation[]>(queryPath(`${BUYER}/evaluations`, { status }));
}

export function submitFoodEvaluation(
  id: number,
  qualityRating: number,
  logisticsRating: number,
  comment: string,
  attachments: Array<{ fileId?: string; fileName: string; fileUrl: string }> = []
) {
  return request<FoodEvaluation>(`${BUYER}/evaluations/${id}/submit`, {
    method: "POST",
    body: JSON.stringify({ qualityRating, logisticsRating, comment, attachments })
  });
}

export function reviewFoodEvaluation(id: number, targetStatus: string, reviewRemark = "") {
  return request<FoodEvaluation>(`${BUYER}/evaluations/${id}/review`, {
    method: "POST",
    body: JSON.stringify({ targetStatus, reviewRemark })
  });
}
