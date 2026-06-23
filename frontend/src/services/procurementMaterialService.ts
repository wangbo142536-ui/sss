import { ApiError, getAuthSession } from "@/services/authService";
import type {
  MaterialDocumentType,
  MaterialComparisonCandidate,
  MaterialComparisonItem,
  MaterialComparisonStrategy,
  MaterialDemandDetail,
  MaterialDemandComparisonResponse,
  MaterialDemandListResponse,
  MaterialDemandSavePayload,
  MaterialDemandSaveResponse,
  MaterialDemandSummary,
  MaterialMatchCandidate,
  MaterialMatchPreviewItem,
  MaterialMatchPreviewResponse,
  MaterialMatchResult,
  MaterialParsedAttribute,
  MaterialSupplierCandidate
} from "@/types/procurementMaterials";

const MATERIAL_MATCH_PREVIEW_ENDPOINT = "/api/procurement/materials/match-preview";
const MATERIAL_DEMAND_ENDPOINT = "/api/procurement/material-demands";

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
  const value = payload.message ?? payload.error ?? payload.detail ?? payload.msg ?? payload.reason;
  return typeof value === "string" && value.trim() ? value.trim() : "";
}

function createApiError(response: Response, payload: unknown): ApiError {
  return new ApiError(readErrorMessage(payload) || `HTTP_${response.status}`, response.status, payload);
}

function unwrapPayload(payload: unknown): unknown {
  if (!isRecord(payload)) return payload;
  if ("data" in payload) return payload.data;
  if ("result" in payload) return payload.result;
  return payload;
}

function readString(source: Record<string, unknown>, key: string): string | undefined {
  const value = source[key];
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function readRequiredNumber(source: Record<string, unknown>, key: string): number {
  return readNumber(source, key) ?? 0;
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

function normalizeStringRecord(value: unknown): Record<string, string> {
  if (!isRecord(value)) return {};
  return Object.fromEntries(
    Object.entries(value)
      .map(([key, item]) => [key, item == null ? "" : String(item)])
      .filter(([, item]) => item.trim())
  );
}

function normalizeCandidate(value: unknown): MaterialMatchCandidate | null {
  if (!isRecord(value)) return null;
  return {
    impaCode: readString(value, "impaCode"),
    nameCn: readString(value, "nameCn"),
    nameEn: readString(value, "nameEn"),
    specification: readString(value, "specification"),
    unit: readString(value, "unit"),
    candidateMatchType: readString(value, "candidateMatchType"),
    matchScore: readNumber(value, "matchScore"),
    reason: readString(value, "reason")
  };
}

function normalizeSupplierCandidate(value: unknown): MaterialSupplierCandidate | null {
  if (!isRecord(value)) return null;
  return {
    skuId: readNumber(value, "skuId"),
    companyId: readNumber(value, "companyId"),
    supplierName: readString(value, "supplierName"),
    supplierSkuCode: readString(value, "supplierSkuCode"),
    productName: readString(value, "productName"),
    impaCode: readString(value, "impaCode"),
    platformCode: readString(value, "platformCode"),
    categoryCode: readString(value, "categoryCode"),
    categoryName: readString(value, "categoryName"),
    specification: readString(value, "specification"),
    specifications: value.specifications,
    attributeSummary: readString(value, "attributeSummary"),
    unitPrice: readNumber(value, "unitPrice"),
    currency: readString(value, "currency"),
    currencySymbol: readString(value, "currencySymbol"),
    unit: readString(value, "unit"),
    stockQty: readNumber(value, "stockQty"),
    stockUnit: readString(value, "stockUnit"),
    packageSpec: readString(value, "packageSpec"),
    imageUrl: readString(value, "imageUrl"),
    thumbnailUrl: readString(value, "thumbnailUrl"),
    shelfStatus: readString(value, "shelfStatus"),
    codeStatus: readString(value, "codeStatus"),
    matchType: readString(value, "matchType"),
    reason: readString(value, "reason")
  };
}

function normalizeComparisonCandidate(value: unknown): MaterialComparisonCandidate | undefined {
  if (!isRecord(value)) return undefined;
  const unitPriceOptions = Array.isArray(value.unitPriceOptions)
    ? value.unitPriceOptions
        .filter(isRecord)
        .map((option) => ({
          unit: readString(option, "unit"),
          unitPrice: readNumber(option, "unitPrice"),
          unitPriceUsd: readNumber(option, "unitPriceUsd"),
          defaultSelected: readBoolean(option, "defaultSelected")
        }))
    : [];
  return {
    skuId: readNumber(value, "skuId"),
    companyId: readNumber(value, "companyId"),
    supplierName: readString(value, "supplierName"),
    supplierSkuCode: readString(value, "supplierSkuCode"),
    productName: readString(value, "productName"),
    impaCode: readString(value, "impaCode"),
    platformCode: readString(value, "platformCode"),
    categoryCode: readString(value, "categoryCode"),
    categoryName: readString(value, "categoryName"),
    specification: readString(value, "specification"),
    specifications: value.specifications,
    attributeSummary: readString(value, "attributeSummary"),
    unitPrice: readNumber(value, "unitPrice"),
    unitPriceUsd: readNumber(value, "unitPriceUsd"),
    lineAmount: readNumber(value, "lineAmount"),
    lineAmountUsd: readNumber(value, "lineAmountUsd"),
    currency: readString(value, "currency"),
    currencySymbol: readString(value, "currencySymbol"),
    stockQty: readNumber(value, "stockQty"),
    stockUnit: readString(value, "stockUnit"),
    unit: readString(value, "unit"),
    selectedUnit: readString(value, "selectedUnit"),
    unitPriceOptions,
    packageSpec: readString(value, "packageSpec"),
    imageUrl: readString(value, "imageUrl"),
    thumbnailUrl: readString(value, "thumbnailUrl"),
    shelfStatus: readString(value, "shelfStatus"),
    codeStatus: readString(value, "codeStatus"),
    matchType: readString(value, "matchType"),
    reason: readString(value, "reason")
  };
}

function normalizeParsedAttribute(value: unknown): MaterialParsedAttribute | null {
  if (!isRecord(value)) return null;
  return {
    key: readString(value, "key"),
    name: readString(value, "name"),
    value: readString(value, "value"),
    unit: readString(value, "unit"),
    rawText: readString(value, "rawText")
  };
}

function normalizeStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map((item) => (item == null ? "" : String(item).trim())).filter(Boolean);
}

function normalizeItem(value: unknown): MaterialMatchPreviewItem | null {
  if (!isRecord(value)) return null;
  return {
    documentType: readString(value, "documentType") as MaterialDocumentType | undefined,
    sourceFormat: readString(value, "sourceFormat") || readString(value, "rfqFormat"),
    headerRowIndex: readNumber(value, "headerRowIndex"),
    sequence: readNumber(value, "sequence"),
    sourceRowNo: readNumber(value, "sourceRowNo"),
    sourceRowNumber: readNumber(value, "sourceRowNumber"),
    rawColumns: normalizeStringRecord(value.rawColumns),
    impaCode: readString(value, "impaCode"),
    platformCode: readString(value, "platformCode"),
    cleanName: readString(value, "cleanName"),
    coreName: readString(value, "coreName"),
    parsedAttributes: Array.isArray(value.parsedAttributes) ? value.parsedAttributes.map(normalizeParsedAttribute).filter((item): item is MaterialParsedAttribute => Boolean(item)) : [],
    riskFlags: normalizeStringArray(value.riskFlags),
    description: readString(value, "description"),
    sizeModel: readString(value, "sizeModel"),
    quantity: readString(value, "quantity"),
    unit: readString(value, "unit"),
    remarks: readString(value, "remarks"),
    supplierItemNo: readString(value, "supplierItemNo"),
    rawNameSpec: readString(value, "rawNameSpec"),
    price: readString(value, "price"),
    packing: readString(value, "packing"),
    stock: readString(value, "stock"),
    selectedImpaCode: readString(value, "selectedImpaCode"),
    candidateImpaCode: readString(value, "candidateImpaCode"),
    candidateNameCn: readString(value, "candidateNameCn"),
    candidateNameEn: readString(value, "candidateNameEn"),
    candidateSpec: readString(value, "candidateSpec"),
    matchResult: readString(value, "matchResult") as MaterialMatchResult | undefined,
    matchResultName: readString(value, "matchResultName"),
    reason: readString(value, "reason"),
    hasImage: readBoolean(value, "hasImage"),
    imageIndex: readNumber(value, "imageIndex"),
    imageAnchor: readString(value, "imageAnchor"),
    candidateSnapshot: Array.isArray(value.candidateSnapshot) ? value.candidateSnapshot.map(normalizeCandidate).filter((item): item is MaterialMatchCandidate => Boolean(item)) : [],
    candidates: Array.isArray(value.candidates)
      ? value.candidates.map(normalizeCandidate).filter((item): item is MaterialMatchCandidate => Boolean(item))
      : Array.isArray(value.candidateSnapshot)
        ? value.candidateSnapshot.map(normalizeCandidate).filter((item): item is MaterialMatchCandidate => Boolean(item))
        : [],
    supplierCandidates: Array.isArray(value.supplierCandidates)
      ? value.supplierCandidates.map(normalizeSupplierCandidate).filter((item): item is MaterialSupplierCandidate => Boolean(item))
      : []
  };
}

function normalizeDemandSummary(value: unknown): MaterialDemandSummary | null {
  if (!isRecord(value)) return null;
  const demandId = readNumber(value, "demandId") ?? readNumber(value, "id");
  if (!demandId) return null;
  return {
    demandId,
    demandNo: readString(value, "demandNo") || "",
    applicationNo: readString(value, "applicationNo"),
    vesselName: readString(value, "vesselName"),
    supplyPortCode: readString(value, "supplyPortCode"),
    supplyPortName: readString(value, "supplyPortName"),
    vesselEta: readString(value, "vesselEta"),
    inquiryDate: readString(value, "inquiryDate"),
    sourceFileName: readString(value, "sourceFileName"),
    documentType: readString(value, "documentType") as MaterialDocumentType | undefined,
    headerRowIndex: readNumber(value, "headerRowIndex"),
    skuCount: readRequiredNumber(value, "skuCount"),
    exactCount: readRequiredNumber(value, "exactCount"),
    similarCount: readRequiredNumber(value, "similarCount"),
    unmatchedCount: readRequiredNumber(value, "unmatchedCount"),
    status: readString(value, "status"),
    createdAt: readString(value, "createdAt"),
    updatedAt: readString(value, "updatedAt")
  };
}

function normalizeDemandList(payload: unknown): MaterialDemandListResponse {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) return { items: [], page: 1, size: 20, total: 0 };
  const source = Array.isArray(unwrapped.items) ? unwrapped.items : Array.isArray(unwrapped.rows) ? unwrapped.rows : [];
  const items = source.map(normalizeDemandSummary).filter((item): item is MaterialDemandSummary => Boolean(item));
  return {
    items,
    page: readNumber(unwrapped, "page") ?? 1,
    size: readNumber(unwrapped, "size") ?? items.length,
    total: readNumber(unwrapped, "total") ?? items.length
  };
}

function normalizeDemandDetail(payload: unknown): MaterialDemandDetail {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("MATERIAL_DEMAND_DETAIL_RESPONSE_INVALID", 0, payload);
  }
  const demand = normalizeDemandSummary(unwrapped.demand);
  if (!demand) {
    throw new ApiError("MATERIAL_DEMAND_DETAIL_RESPONSE_INVALID", 0, payload);
  }
  const source = Array.isArray(unwrapped.items) ? unwrapped.items : [];
  return {
    demand,
    items: source.map(normalizeItem).filter((item): item is MaterialMatchPreviewItem => Boolean(item))
  };
}

function normalizeDemandSaveResponse(payload: unknown): MaterialDemandSaveResponse {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("MATERIAL_DEMAND_SAVE_RESPONSE_INVALID", 0, payload);
  }
  const demandId = readNumber(unwrapped, "demandId") ?? readNumber(unwrapped, "id");
  if (!demandId) {
    throw new ApiError("MATERIAL_DEMAND_SAVE_RESPONSE_INVALID", 0, payload);
  }
  return {
    demandId,
    demandNo: readString(unwrapped, "demandNo") || "",
    status: readString(unwrapped, "status"),
    defaultRoute: readString(unwrapped, "defaultRoute"),
    redirectTo: readString(unwrapped, "redirectTo")
  };
}

function normalizeComparisonSupplyInfo(value: unknown) {
  if (!isRecord(value)) return undefined;
  return {
    vesselName: readString(value, "vesselName"),
    supplyPort: readString(value, "supplyPort"),
    supplyPortCode: readString(value, "supplyPortCode"),
    supplyPortName: readString(value, "supplyPortName"),
    vesselEta: readString(value, "vesselEta"),
    port: readString(value, "port"),
    supplyDate: readString(value, "supplyDate"),
    inquiryDate: readString(value, "inquiryDate"),
    weather: readString(value, "weather"),
    weatherInfo: readString(value, "weatherInfo"),
    weatherText: readString(value, "weatherText")
  };
}

function normalizeComparisonSupplierSummary(value: unknown) {
  if (!isRecord(value)) return null;
  return {
    supplierId: readNumber(value, "supplierId"),
    companyId: readNumber(value, "companyId"),
    supplierName: readString(value, "supplierName"),
    matchedCount: readNumber(value, "matchedCount"),
    skuCount: readNumber(value, "skuCount"),
    totalAmount: readNumber(value, "totalAmount"),
    totalAmountUsd: readNumber(value, "totalAmountUsd"),
    amount: readNumber(value, "amount"),
    currency: readString(value, "currency")
  };
}

function normalizeComparisonStrategy(value: unknown): MaterialComparisonStrategy | null {
  if (!isRecord(value)) return null;
  const strategyType = readString(value, "strategyType");
  if (!strategyType) return null;
  const suppliers = Array.isArray(value.suppliers)
    ? value.suppliers.map(normalizeComparisonSupplierSummary).filter((item): item is NonNullable<ReturnType<typeof normalizeComparisonSupplierSummary>> => Boolean(item))
    : [];
  return {
    strategyType,
    strategyName: readString(value, "strategyName"),
    matchedCount: readNumber(value, "matchedCount") ?? 0,
    totalCount: readNumber(value, "totalCount") ?? 0,
    unmatchedCount: readNumber(value, "unmatchedCount") ?? 0,
    unpricedCount: readNumber(value, "unpricedCount") ?? 0,
    totalAmount: readNumber(value, "totalAmount"),
    totalAmountUsd: readNumber(value, "totalAmountUsd"),
    currency: readString(value, "currency"),
    suppliers,
    enabled: readBoolean(value, "enabled"),
    disabledReason: readString(value, "disabledReason")
  };
}

function normalizeComparisonItem(value: unknown): MaterialComparisonItem | null {
  if (!isRecord(value)) return null;
  const candidates = Array.isArray(value.candidates)
    ? value.candidates.map(normalizeComparisonCandidate).filter((item): item is MaterialComparisonCandidate => Boolean(item))
    : [];
  return {
    demandItemId: readNumber(value, "demandItemId"),
    rowNo: readNumber(value, "rowNo"),
    impaCode: readString(value, "impaCode"),
    platformCode: readString(value, "platformCode"),
    productName: readString(value, "productName"),
    description: readString(value, "description"),
    specification: readString(value, "specification"),
    quantity: readString(value, "quantity"),
    pricingQuantity: readNumber(value, "pricingQuantity"),
    pricingQuantityNote: readString(value, "pricingQuantityNote"),
    unit: readString(value, "unit"),
    sourceSkuCode: readString(value, "sourceSkuCode"),
    sourceSkuName: readString(value, "sourceSkuName"),
    lowestCandidate: normalizeComparisonCandidate(value.lowestCandidate),
    singleSupplierCandidate: normalizeComparisonCandidate(value.singleSupplierCandidate),
    candidates,
    emptyReason: readString(value, "emptyReason")
  };
}

function normalizeMaterialDemandComparison(payload: unknown): MaterialDemandComparisonResponse {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("MATERIAL_COMPARISON_RESPONSE_INVALID", 0, payload);
  }
  const demand = normalizeDemandSummary(unwrapped.demand);
  const strategySource = Array.isArray(unwrapped.strategies) ? unwrapped.strategies : [];
  const itemSource = Array.isArray(unwrapped.items) ? unwrapped.items : [];
  return {
    demand: demand ?? undefined,
    supplyInfo: normalizeComparisonSupplyInfo(unwrapped.supplyInfo),
    strategies: strategySource.map(normalizeComparisonStrategy).filter((item): item is MaterialComparisonStrategy => Boolean(item)),
    items: itemSource.map(normalizeComparisonItem).filter((item): item is MaterialComparisonItem => Boolean(item)),
    isOrdered: readBoolean(unwrapped, "isOrdered"),
    isDiscarded: readBoolean(unwrapped, "isDiscarded"),
    existingPurchaseOrderId: readNumber(unwrapped, "existingPurchaseOrderId")
  };
}

async function requestJson(endpoint: string, init: RequestInit = { method: "GET" }): Promise<unknown> {
  const session = getAuthSession();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  headers.set("Content-Type", "application/json");
  if (session?.token) headers.set("Authorization", `Bearer ${session.token}`);

  const response = await fetch(endpoint, {
    ...init,
    headers
  });
  const payload = await readJson(response);

  if (!response.ok) {
    throw createApiError(response, payload);
  }

  return unwrapPayload(payload);
}

function normalizeMatchPreview(payload: unknown): MaterialMatchPreviewResponse {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("MATERIAL_MATCH_PREVIEW_RESPONSE_INVALID", 0, payload);
  }

  const itemsSource = Array.isArray(unwrapped.items) ? unwrapped.items : Array.isArray(unwrapped.rows) ? unwrapped.rows : [];
  const items = itemsSource.map(normalizeItem).filter((item): item is MaterialMatchPreviewItem => Boolean(item));

  return {
    documentType: readString(unwrapped, "documentType") || "UNKNOWN",
    sourceFormat: readString(unwrapped, "sourceFormat") || readString(unwrapped, "rfqFormat"),
    headerRowIndex: readNumber(unwrapped, "headerRowIndex") ?? 0,
    totalRows: readNumber(unwrapped, "totalRows") ?? readNumber(unwrapped, "totalCount") ?? items.length,
    exactCount: readNumber(unwrapped, "exactCount") ?? items.filter((item) => item.matchResult === "EXACT").length,
    similarCount: readNumber(unwrapped, "similarCount") ?? items.filter((item) => item.matchResult === "SIMILAR").length,
    unmatchedCount: readNumber(unwrapped, "unmatchedCount") ?? items.filter((item) => item.matchResult === "UNMATCHED").length,
    items
  };
}

export async function uploadMaterialMatchPreview(file: File): Promise<MaterialMatchPreviewResponse> {
  const session = getAuthSession();
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(MATERIAL_MATCH_PREVIEW_ENDPOINT, {
    method: "POST",
    headers: {
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {})
    },
    body: formData
  });
  const payload = await readJson(response);

  if (!response.ok) {
    throw createApiError(response, payload);
  }

  return normalizeMatchPreview(payload);
}

export async function listMaterialDemands(query: { keyword?: string; status?: string; dateFrom?: string; dateTo?: string; page?: number; size?: number } = {}): Promise<MaterialDemandListResponse> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("keyword", query.keyword.trim());
  if (query.status?.trim()) params.set("status", query.status.trim());
  if (query.dateFrom?.trim()) params.set("dateFrom", query.dateFrom.trim());
  if (query.dateTo?.trim()) params.set("dateTo", query.dateTo.trim());
  if (query.page) params.set("page", String(query.page));
  if (query.size) params.set("size", String(query.size));
  const endpoint = params.size ? `${MATERIAL_DEMAND_ENDPOINT}?${params.toString()}` : MATERIAL_DEMAND_ENDPOINT;
  return normalizeDemandList(await requestJson(endpoint));
}

export async function getMaterialDemandDetail(demandId: number | string): Promise<MaterialDemandDetail> {
  return normalizeDemandDetail(await requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}`));
}

export async function saveMaterialDemand(payload: MaterialDemandSavePayload): Promise<MaterialDemandSaveResponse> {
  const endpoint = payload.demandId ? `${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(payload.demandId))}` : MATERIAL_DEMAND_ENDPOINT;
  return normalizeDemandSaveResponse(
    await requestJson(endpoint, {
      method: payload.demandId ? "PUT" : "POST",
      body: JSON.stringify(payload)
    })
  );
}

export async function getMaterialDemandComparison(demandId: number | string): Promise<MaterialDemandComparisonResponse> {
  return normalizeMaterialDemandComparison(await requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/comparison`));
}

export async function discardMaterialDemand(demandId: number | string): Promise<unknown> {
  return requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/discard`, { method: "POST" });
}

export async function listMaterialDemandItemSupplierCandidates(demandId: number | string, itemId: number | string): Promise<MaterialComparisonCandidate[]> {
  const payload = await requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/items/${encodeURIComponent(String(itemId))}/supplier-candidates`);
  const unwrapped = unwrapPayload(payload);
  const source = Array.isArray(unwrapped) ? unwrapped : [];
  return source.map(normalizeComparisonCandidate).filter((item): item is MaterialComparisonCandidate => Boolean(item));
}
