import { ApiError, getAuthSession } from "@/services/authService";

export type PurchaseOrderStatus =
  | "PENDING_SUPPLIER_CONFIRM"
  | "PARTIALLY_CONFIRMED"
  | "PREPARING"
  | "READY_TO_DELIVER"
  | "PARTIALLY_READY"
  | "SUPPLIED"
  | "PARTIALLY_SUPPLIED"
  | "PARTIALLY_REJECTED"
  | "REJECTED"
  | "CANCELED"
  | "DISCARDED"
  | string;

export type PurchasePackagingMethod = "UNIFIED_PACKAGING" | "SUPPLIER_PACKAGING" | string;

export interface PurchaseOrderCreatePayload {
  demandId?: number;
  strategyType?: string;
  supplyPort?: string;
  vesselEta?: string;
  requiredDeliveryTime?: string;
  deliveryContactName?: string;
  deliveryContactPhone?: string;
  deliveryContactEmail?: string;
  defaultPackagingMethod?: PurchasePackagingMethod;
  buyerRemark?: string;
  selectedItems?: Array<{
    demandItemId?: number;
    skuId?: number;
    supplierCompanyId?: number;
    supplierName?: string;
    supplierSkuCode?: string;
    platformCode?: string;
    impaCode?: string;
    productName?: string;
    specification?: string;
    quantity?: string;
    unit?: string;
    pricingQuantity?: number;
    unitPrice?: number;
    unitPriceUsd?: number;
    currency?: string;
    amount?: number;
    amountUsd?: number;
    actualQuotePrice?: number;
    actualQuoteAmount?: number;
    quoteMarkupPercent?: number;
    selectedUnit?: string;
    unitMismatchFlag?: boolean;
    quantityFallbackFlag?: boolean;
  }>;
}

export interface PurchaseOrderCreateResponse {
  purchaseOrderId: number;
  purchaseOrderNo: string;
  status: PurchaseOrderStatus;
  supplierOrderCount: number;
  itemCount: number;
  totalAmount?: number;
  totalAmountUsd?: number;
  currency?: string;
  redirectTo?: string;
  idempotent?: boolean;
}

export interface PurchaseOrderSummary {
  purchaseOrderId: number;
  purchaseOrderNo: string;
  supplierOrderId?: number;
  demandId?: number;
  demandNo?: string;
  applicationNo?: string;
  inquiryNo?: string;
  materialType?: string;
  demandCurrency?: string;
  recipientCompany?: string;
  handlerName?: string;
  handlerEmail?: string;
  buyerCompanyId?: number;
  buyerCompanyName?: string;
  vesselName?: string;
  supplyPort?: string;
  vesselEta?: string;
  requiredDeliveryTime?: string;
  deliveryContactName?: string;
  deliveryContactPhone?: string;
  deliveryContactEmail?: string;
  strategyType?: string;
  strategyName?: string;
  supplierCount: number;
  quotedSupplierCount?: number;
  totalSupplierCount?: number;
  itemCount: number;
  totalAmount?: number;
  totalAmountUsd?: number;
  currency?: string;
  status?: PurchaseOrderStatus;
  packagingMethod?: PurchasePackagingMethod;
  expectedReadyAt?: string;
  buyerRemark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PurchaseOrderItem {
  itemId?: number;
  supplierOrderId?: number;
  purchaseOrderId?: number;
  demandItemId?: number;
  skuId?: number;
  supplierSkuCode?: string;
  platformCode?: string;
  impaCode?: string;
  productName?: string;
  specification?: string;
  quantity?: string;
  unit?: string;
  pricingQuantity?: number;
  unitPrice?: number;
  unitPriceUsd?: number;
  amount?: number;
  amountUsd?: number;
  actualQuotePrice?: number;
  actualQuoteCurrency?: string;
  quoteMarkupPercent?: number;
  quoteProfitAmount?: number;
  currency?: string;
  unitMismatchFlag?: boolean;
  quantityFallbackFlag?: boolean;
  sourceMatchType?: string;
  sourceReason?: string;
}

export interface PurchaseSupplierOrder {
  supplierOrderId: number;
  supplierOrderNo?: string;
  purchaseOrderId?: number;
  supplierCompanyId?: number;
  supplierName?: string;
  status?: PurchaseOrderStatus;
  itemCount: number;
  subtotalAmount?: number;
  discountType?: string;
  discountValue?: number;
  discountAmount?: number;
  finalAmount?: number;
  currency?: string;
  packagingMethod?: PurchasePackagingMethod;
  expectedReadyAt?: string;
  supplierRemark?: string;
  rejectReason?: string;
  confirmedAt?: string;
  readyAt?: string;
  suppliedAt?: string;
  deliveryImageFileId?: string;
  deliveryImageUrl?: string;
  deliveryRemark?: string;
  rejectedAt?: string;
  items: PurchaseOrderItem[];
}

export interface PurchaseOrderEvent {
  eventId?: number;
  purchaseOrderId?: number;
  supplierOrderId?: number;
  eventType?: string;
  eventMessage?: string;
  operatorUserId?: number;
  operatorCompanyId?: number;
  createdAt?: string;
}

export interface PurchaseOrderDetail {
  order: PurchaseOrderSummary;
  supplierOrders: PurchaseSupplierOrder[];
  events: PurchaseOrderEvent[];
}

export interface PurchaseOrderListResponse {
  items: PurchaseOrderSummary[];
  page: number;
  size: number;
  total: number;
}

export interface PurchaseOrderListQuery {
  keyword?: string;
  status?: string;
  supplier?: string;
  createdFrom?: string;
  createdTo?: string;
  deliveryFrom?: string;
  deliveryTo?: string;
  page?: number;
  size?: number;
}

export interface SupplierOrderListQuery {
  keyword?: string;
  status?: string;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  size?: number;
}

export interface SupplierConfirmPayload {
  expectedReadyAt: string;
  discountType?: string;
  discountValue?: number;
  packagingMethod?: PurchasePackagingMethod;
  supplierRemark?: string;
}

export interface SupplierRejectPayload {
  rejectReason: string;
}

export interface SupplierSupplyCompletePayload {
  deliveryImageFileId?: string;
  deliveryImageUrl?: string;
  deliveryRemark?: string;
}

const PURCHASE_ORDER_ENDPOINT = "/api/procurement/purchase-orders";
const SUPPLIER_ORDER_ENDPOINT = "/api/supplier/procurement/orders";

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

function unwrapPayload(payload: unknown): unknown {
  if (!isRecord(payload)) return payload;
  if ("data" in payload) return payload.data;
  if ("result" in payload) return payload.result;
  return payload;
}

function readErrorMessage(payload: unknown): string {
  if (!isRecord(payload)) return "";
  const value = payload.message ?? payload.error ?? payload.detail ?? payload.msg ?? payload.reason ?? payload.code;
  return typeof value === "string" && value.trim() ? value.trim() : "";
}

function createApiError(response: Response, payload: unknown): ApiError {
  return new ApiError(readErrorMessage(payload) || `HTTP_${response.status}`, response.status, payload);
}

function readString(source: Record<string, unknown>, key: string): string | undefined {
  const value = source[key];
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function readNumber(source: Record<string, unknown>, key: string): number | undefined {
  const value = source[key];
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value);
  return undefined;
}

function readBoolean(source: Record<string, unknown>, key: string): boolean | undefined {
  const value = source[key];
  return typeof value === "boolean" ? value : undefined;
}

function buildQuery(params: object = {}) {
  const query = new URLSearchParams();
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    if (typeof value !== "string" && typeof value !== "number") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

async function requestPurchaseJson(endpoint: string, init: RequestInit = {}): Promise<unknown> {
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
  const payload = await readJson(response);
  if (!response.ok) {
    throw createApiError(response, payload);
  }
  return unwrapPayload(payload);
}

function normalizeOrderSummary(value: unknown): PurchaseOrderSummary | null {
  if (!isRecord(value)) return null;
  const purchaseOrderId = readNumber(value, "purchaseOrderId") ?? readNumber(value, "orderId") ?? readNumber(value, "id");
  if (!purchaseOrderId) return null;
  return {
    purchaseOrderId,
    purchaseOrderNo: readString(value, "purchaseOrderNo") || readString(value, "orderNo") || "",
    supplierOrderId: readNumber(value, "supplierOrderId"),
    demandId: readNumber(value, "demandId"),
    demandNo: readString(value, "demandNo"),
    applicationNo: readString(value, "applicationNo"),
    inquiryNo: readString(value, "inquiryNo"),
    materialType: readString(value, "materialType"),
    demandCurrency: readString(value, "demandCurrency"),
    recipientCompany: readString(value, "recipientCompany"),
    handlerName: readString(value, "handlerName"),
    handlerEmail: readString(value, "handlerEmail"),
    buyerCompanyId: readNumber(value, "buyerCompanyId"),
    buyerCompanyName: readString(value, "buyerCompanyName"),
    vesselName: readString(value, "vesselName"),
    supplyPort: readString(value, "supplyPort"),
    vesselEta: readString(value, "vesselEta"),
    requiredDeliveryTime: readString(value, "requiredDeliveryTime"),
    deliveryContactName: readString(value, "deliveryContactName"),
    deliveryContactPhone: readString(value, "deliveryContactPhone"),
    deliveryContactEmail: readString(value, "deliveryContactEmail"),
    strategyType: readString(value, "strategyType"),
    strategyName: readString(value, "strategyName"),
    supplierCount: readNumber(value, "supplierCount") ?? readNumber(value, "supplierOrderCount") ?? 0,
    quotedSupplierCount: readNumber(value, "quotedSupplierCount"),
    totalSupplierCount: readNumber(value, "totalSupplierCount"),
    itemCount: readNumber(value, "itemCount") ?? 0,
    totalAmount: readNumber(value, "totalAmount"),
    totalAmountUsd: readNumber(value, "totalAmountUsd"),
    currency: readString(value, "currency"),
    status: readString(value, "status"),
    packagingMethod: readString(value, "packagingMethod"),
    expectedReadyAt: readString(value, "expectedReadyAt"),
    buyerRemark: readString(value, "buyerRemark"),
    createdAt: readString(value, "createdAt"),
    updatedAt: readString(value, "updatedAt")
  };
}

function normalizeOrderItem(value: unknown): PurchaseOrderItem | null {
  if (!isRecord(value)) return null;
  return {
    itemId: readNumber(value, "itemId") ?? readNumber(value, "id"),
    supplierOrderId: readNumber(value, "supplierOrderId"),
    purchaseOrderId: readNumber(value, "purchaseOrderId"),
    demandItemId: readNumber(value, "demandItemId"),
    skuId: readNumber(value, "skuId"),
    supplierSkuCode: readString(value, "supplierSkuCode"),
    platformCode: readString(value, "platformCode"),
    impaCode: readString(value, "impaCode"),
    productName: readString(value, "productName"),
    specification: readString(value, "specification"),
    quantity: readString(value, "quantity"),
    unit: readString(value, "unit"),
    pricingQuantity: readNumber(value, "pricingQuantity"),
    unitPrice: readNumber(value, "unitPrice"),
    unitPriceUsd: readNumber(value, "unitPriceUsd"),
    amount: readNumber(value, "amount"),
    amountUsd: readNumber(value, "amountUsd"),
    actualQuotePrice: readNumber(value, "actualQuotePrice"),
    actualQuoteCurrency: readString(value, "actualQuoteCurrency"),
    quoteMarkupPercent: readNumber(value, "quoteMarkupPercent"),
    quoteProfitAmount: readNumber(value, "quoteProfitAmount"),
    currency: readString(value, "currency"),
    unitMismatchFlag: readBoolean(value, "unitMismatchFlag"),
    quantityFallbackFlag: readBoolean(value, "quantityFallbackFlag"),
    sourceMatchType: readString(value, "sourceMatchType"),
    sourceReason: readString(value, "sourceReason")
  };
}

function normalizeSupplierOrder(value: unknown): PurchaseSupplierOrder | null {
  if (!isRecord(value)) return null;
  const supplierOrderId = readNumber(value, "supplierOrderId") ?? readNumber(value, "id");
  if (!supplierOrderId) return null;
  const items = Array.isArray(value.items) ? value.items.map(normalizeOrderItem).filter((item): item is PurchaseOrderItem => Boolean(item)) : [];
  return {
    supplierOrderId,
    supplierOrderNo: readString(value, "supplierOrderNo"),
    purchaseOrderId: readNumber(value, "purchaseOrderId"),
    supplierCompanyId: readNumber(value, "supplierCompanyId"),
    supplierName: readString(value, "supplierName"),
    status: readString(value, "status"),
    itemCount: readNumber(value, "itemCount") ?? items.length,
    subtotalAmount: readNumber(value, "subtotalAmount"),
    discountType: readString(value, "discountType"),
    discountValue: readNumber(value, "discountValue"),
    discountAmount: readNumber(value, "discountAmount"),
    finalAmount: readNumber(value, "finalAmount"),
    currency: readString(value, "currency"),
    packagingMethod: readString(value, "packagingMethod"),
    expectedReadyAt: readString(value, "expectedReadyAt"),
    supplierRemark: readString(value, "supplierRemark"),
    rejectReason: readString(value, "rejectReason"),
    confirmedAt: readString(value, "confirmedAt"),
    readyAt: readString(value, "readyAt"),
    suppliedAt: readString(value, "suppliedAt"),
    deliveryImageFileId: readString(value, "deliveryImageFileId"),
    deliveryImageUrl: readString(value, "deliveryImageUrl"),
    deliveryRemark: readString(value, "deliveryRemark"),
    rejectedAt: readString(value, "rejectedAt"),
    items
  };
}

function normalizeOrderEvent(value: unknown): PurchaseOrderEvent | null {
  if (!isRecord(value)) return null;
  return {
    eventId: readNumber(value, "eventId") ?? readNumber(value, "id"),
    purchaseOrderId: readNumber(value, "purchaseOrderId"),
    supplierOrderId: readNumber(value, "supplierOrderId"),
    eventType: readString(value, "eventType"),
    eventMessage: readString(value, "eventMessage"),
    operatorUserId: readNumber(value, "operatorUserId"),
    operatorCompanyId: readNumber(value, "operatorCompanyId"),
    createdAt: readString(value, "createdAt")
  };
}

function normalizeOrderList(payload: unknown): PurchaseOrderListResponse {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) return { items: [], page: 1, size: 20, total: 0 };
  const source = Array.isArray(unwrapped.items) ? unwrapped.items : Array.isArray(unwrapped.rows) ? unwrapped.rows : [];
  const items = source.map(normalizeOrderSummary).filter((item): item is PurchaseOrderSummary => Boolean(item));
  return {
    items,
    page: readNumber(unwrapped, "page") ?? 1,
    size: readNumber(unwrapped, "size") ?? items.length,
    total: readNumber(unwrapped, "total") ?? items.length
  };
}

function normalizeOrderDetail(payload: unknown): PurchaseOrderDetail {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("PURCHASE_ORDER_DETAIL_RESPONSE_INVALID", 0, payload);
  }
  const order = normalizeOrderSummary(unwrapped.order);
  if (!order) {
    throw new ApiError("PURCHASE_ORDER_DETAIL_RESPONSE_INVALID", 0, payload);
  }
  return {
    order,
    supplierOrders: Array.isArray(unwrapped.supplierOrders)
      ? unwrapped.supplierOrders.map(normalizeSupplierOrder).filter((item): item is PurchaseSupplierOrder => Boolean(item))
      : [],
    events: Array.isArray(unwrapped.events) ? unwrapped.events.map(normalizeOrderEvent).filter((item): item is PurchaseOrderEvent => Boolean(item)) : []
  };
}

function normalizeCreateResponse(payload: unknown): PurchaseOrderCreateResponse {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("PURCHASE_ORDER_CREATE_RESPONSE_INVALID", 0, payload);
  }
  const purchaseOrderId = readNumber(unwrapped, "purchaseOrderId") ?? readNumber(unwrapped, "orderId") ?? readNumber(unwrapped, "id");
  if (!purchaseOrderId) {
    throw new ApiError("PURCHASE_ORDER_CREATE_RESPONSE_INVALID", 0, payload);
  }
  return {
    purchaseOrderId,
    purchaseOrderNo: readString(unwrapped, "purchaseOrderNo") || "",
    status: readString(unwrapped, "status") || "",
    supplierOrderCount: readNumber(unwrapped, "supplierOrderCount") ?? readNumber(unwrapped, "supplierCount") ?? 0,
    itemCount: readNumber(unwrapped, "itemCount") ?? 0,
    totalAmount: readNumber(unwrapped, "totalAmount"),
    totalAmountUsd: readNumber(unwrapped, "totalAmountUsd"),
    currency: readString(unwrapped, "currency"),
    redirectTo: readString(unwrapped, "redirectTo"),
    idempotent: readBoolean(unwrapped, "idempotent")
  };
}

export async function createPurchaseOrderFromDemand(demandId: number | string, payload: PurchaseOrderCreatePayload): Promise<PurchaseOrderCreateResponse> {
  return normalizeCreateResponse(
    await requestPurchaseJson(`/api/procurement/material-demands/${encodeURIComponent(String(demandId))}/purchase-orders`, {
      method: "POST",
      body: JSON.stringify(payload)
    })
  );
}

export async function listPurchaseOrders(query: PurchaseOrderListQuery = {}): Promise<PurchaseOrderListResponse> {
  const endpoint = `${PURCHASE_ORDER_ENDPOINT}${buildQuery(query)}`;
  return normalizeOrderList(await requestPurchaseJson(endpoint, { method: "GET" }));
}

export async function getPurchaseOrderDetail(orderId: number | string): Promise<PurchaseOrderDetail> {
  return normalizeOrderDetail(await requestPurchaseJson(`${PURCHASE_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}`, { method: "GET" }));
}

export async function getSupplierPurchaseOrderDetail(orderId: number | string): Promise<PurchaseOrderDetail> {
  return normalizeOrderDetail(await requestPurchaseJson(`${SUPPLIER_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}`, { method: "GET" }));
}

export async function discardPurchaseOrder(orderId: number | string): Promise<unknown> {
  return requestPurchaseJson(`${PURCHASE_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}/discard`, { method: "POST" });
}

export async function listSupplierPurchaseOrders(query: SupplierOrderListQuery = {}): Promise<PurchaseOrderListResponse> {
  const endpoint = `${SUPPLIER_ORDER_ENDPOINT}${buildQuery(query)}`;
  return normalizeOrderList(await requestPurchaseJson(endpoint, { method: "GET" }));
}

export async function confirmSupplierPurchaseOrder(orderId: number | string, supplierOrderId: number | string, payload: SupplierConfirmPayload): Promise<PurchaseOrderDetail> {
  return normalizeOrderDetail(
    await requestPurchaseJson(`${PURCHASE_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}/supplier-orders/${encodeURIComponent(String(supplierOrderId))}/confirm`, {
      method: "POST",
      body: JSON.stringify(payload)
    })
  );
}

export async function rejectSupplierPurchaseOrder(orderId: number | string, supplierOrderId: number | string, payload: SupplierRejectPayload): Promise<PurchaseOrderDetail> {
  return normalizeOrderDetail(
    await requestPurchaseJson(`${PURCHASE_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}/supplier-orders/${encodeURIComponent(String(supplierOrderId))}/reject`, {
      method: "POST",
      body: JSON.stringify(payload)
    })
  );
}

export async function markSupplierOrderReady(orderId: number | string, supplierOrderId: number | string): Promise<PurchaseOrderDetail> {
  return normalizeOrderDetail(
    await requestPurchaseJson(`${PURCHASE_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}/supplier-orders/${encodeURIComponent(String(supplierOrderId))}/ready`, {
      method: "POST"
    })
  );
}

export async function markSupplierOrderSupplied(orderId: number | string, supplierOrderId: number | string, payload: SupplierSupplyCompletePayload): Promise<PurchaseOrderDetail> {
  return normalizeOrderDetail(
    await requestPurchaseJson(`${PURCHASE_ORDER_ENDPOINT}/${encodeURIComponent(String(orderId))}/supplier-orders/${encodeURIComponent(String(supplierOrderId))}/supplied`, {
      method: "POST",
      body: JSON.stringify(payload)
    })
  );
}
