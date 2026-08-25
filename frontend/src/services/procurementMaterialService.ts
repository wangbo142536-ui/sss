import { ApiError, getAuthSession } from "@/services/authService";
import type {
  MaterialDocumentType,
  MaterialComparisonCandidate,
  MaterialComparisonItem,
  MaterialComparisonAiProcessing,
  MaterialComparisonStrategy,
  MaterialComparisonStrategySettingsPayload,
  MaterialDemandDetail,
  MaterialDemandComparisonResponse,
  MaterialComparisonQuoteImportResponse,
  MaterialDemandListResponse,
  MaterialDemandSavePayload,
  MaterialDemandSaveResponse,
  MaterialDemandSummary,
  MaterialDemandTrafficService,
  MaterialMatchCandidate,
  MaterialMatchPreviewItem,
  MaterialMatchPreviewResponse,
  MaterialMatchResult,
  MaterialParsedAttribute,
  MaterialSupplierCandidate
} from "@/types/procurementMaterials";

const MATERIAL_MATCH_PREVIEW_ENDPOINT = "/api/procurement/materials/match-preview";
const MATERIAL_AI_CAPABILITIES_ENDPOINT = "/api/procurement/materials/ai-capabilities";
const MATERIAL_DEMAND_ENDPOINT = "/api/procurement/material-demands";

export type MaterialAiCapabilities = {
  categoryAnalysisConfigured: boolean;
  comparisonRerankConfigured: boolean;
  status: string;
};

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

function readStringArray(value: unknown): string[] {
  return Array.isArray(value)
    ? value.map((item) => String(item || "").trim()).filter(Boolean)
    : [];
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
    reason: readString(value, "reason"),
    productTags: readStringArray(value.productTags),
    qualityScore: readNumber(value, "qualityScore"),
    priceScore: readNumber(value, "priceScore")
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
    reason: readString(value, "reason"),
    productTags: readStringArray(value.productTags),
    qualityScore: readNumber(value, "qualityScore"),
    priceScore: readNumber(value, "priceScore")
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
    validationStatus: readString(value, "validationStatus"),
    validationReason: readString(value, "validationReason"),
    hasImage: readBoolean(value, "hasImage"),
    imageIndex: readNumber(value, "imageIndex"),
    imageAnchor: readString(value, "imageAnchor"),
    actualQuotePrice: readNumber(value, "actualQuotePrice"),
    actualQuoteCurrency: readString(value, "actualQuoteCurrency"),
    quoteMarkupPercent: readNumber(value, "quoteMarkupPercent"),
    quoteSupplierSkuId: readNumber(value, "quoteSupplierSkuId"),
    quoteSelectedUnit: readString(value, "quoteSelectedUnit"),
    quoteUnitPrice: readNumber(value, "quoteUnitPrice"),
    quoteUnitPriceUsd: readNumber(value, "quoteUnitPriceUsd"),
    quoteStrategyType: readString(value, "quoteStrategyType"),
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
    inquiryNo: readString(value, "inquiryNo"),
    materialType: readString(value, "materialType"),
    currency: readString(value, "currency"),
    fixedFreightFee: readNumber(value, "fixedFreightFee"),
    fixedCustomsFee: readNumber(value, "fixedCustomsFee"),
    fixedCraneFee: readNumber(value, "fixedCraneFee"),
    fixedOtherFee: readNumber(value, "fixedOtherFee"),
    trafficService: normalizeTrafficService(value.trafficService),
    recipientCompany: readString(value, "recipientCompany"),
    handlerName: readString(value, "handlerName"),
    handlerEmail: readString(value, "handlerEmail"),
    vesselName: readString(value, "vesselName"),
    supplyPortCode: readString(value, "supplyPortCode"),
    supplyPortName: readString(value, "supplyPortName"),
    vesselEta: readString(value, "vesselEta"),
    inquiryDate: readString(value, "inquiryDate"),
    sourceFileName: readString(value, "sourceFileName"),
    sourceFileId: readString(value, "sourceFileId"),
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

function normalizeTrafficService(value: unknown) {
  if (!isRecord(value)) return undefined;
  const cargos = Array.isArray(value.cargos)
    ? value.cargos
        .filter(isRecord)
        .map((item) => ({
          cargoName: readString(item, "cargoName"),
          weightKg: readNumber(item, "weightKg"),
          volumeCbm: readNumber(item, "volumeCbm")
        }))
    : [];
  return {
    trafficServiceOrderId: readNumber(value, "trafficServiceOrderId"),
    bookingId: readNumber(value, "bookingId"),
    selectedNodeIndex: readNumber(value, "selectedNodeIndex"),
    departurePoint: readString(value, "departurePoint"),
    destinationPoint: readString(value, "destinationPoint"),
    startTime: readString(value, "startTime"),
    returnTime: readString(value, "returnTime"),
    serviceNodes: normalizeTrafficServiceNodes(value.serviceNodes),
    supplyMode: readString(value, "supplyMode"),
    supplyAddress: readString(value, "supplyAddress"),
    supplyRemark: readString(value, "supplyRemark"),
    fixedProviderType: readString(value, "fixedProviderType"),
    fixedProviderId: readString(value, "fixedProviderId") || readNumber(value, "fixedProviderId"),
    fixedProviderName: readString(value, "fixedProviderName"),
    shuttleNo: readString(value, "shuttleNo"),
    trafficVesselName: readString(value, "trafficVesselName"),
    seaArea: readString(value, "seaArea"),
    anchorageCode: readString(value, "anchorageCode"),
    anchorageName: readString(value, "anchorageName"),
    useTime: readString(value, "useTime"),
    serviceType: readString(value, "serviceType"),
    passengerType: readString(value, "passengerType"),
    passengerCount: readNumber(value, "passengerCount"),
    cargoType: readString(value, "cargoType"),
    returnTrip: readBoolean(value, "returnTrip"),
    allowShare: readBoolean(value, "allowShare"),
    basePrice: readNumber(value, "basePrice"),
    sharedPrice: readNumber(value, "sharedPrice"),
    remark: readString(value, "remark"),
    cargos
  };
}

function normalizeTrafficServiceNodes(value: unknown) {
  if (!Array.isArray(value)) return [];
  return value
    .filter(isRecord)
    .map((node) => ({
      nodeName: readString(node, "nodeName") || readString(node, "name"),
      startTime: readString(node, "startTime") || readString(node, "start"),
      endTime: readString(node, "endTime") || readString(node, "end")
    }))
    .filter((node) => node.nodeName || node.startTime || node.endTime);
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
    currency: readString(value, "currency"),
    attributeTags: readStringArray(value.attributeTags),
    coreItemCount: readNumber(value, "coreItemCount"),
    qualityScore: readNumber(value, "qualityScore"),
    priceScore: readNumber(value, "priceScore")
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
    disabledReason: readString(value, "disabledReason"),
    attributeTags: readStringArray(value.attributeTags)
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
    remarks: readString(value, "remarks"),
    sourceSkuCode: readString(value, "sourceSkuCode"),
    sourceSkuName: readString(value, "sourceSkuName"),
    lowestCandidate: normalizeComparisonCandidate(value.lowestCandidate),
    singleSupplierCandidate: normalizeComparisonCandidate(value.singleSupplierCandidate),
    candidates,
    emptyReason: readString(value, "emptyReason"),
    actualQuotePrice: readNumber(value, "actualQuotePrice"),
    actualQuoteCurrency: readString(value, "actualQuoteCurrency"),
    quoteMarkupPercent: readNumber(value, "quoteMarkupPercent"),
    quoteSupplierSkuId: readNumber(value, "quoteSupplierSkuId"),
    quoteSelectedUnit: readString(value, "quoteSelectedUnit"),
    quoteUnitPrice: readNumber(value, "quoteUnitPrice"),
    quoteUnitPriceUsd: readNumber(value, "quoteUnitPriceUsd"),
    quoteStrategyType: readString(value, "quoteStrategyType")
  };
}

function normalizeComparisonStrategySettings(value: unknown) {
  if (!isRecord(value)) return undefined;
  return {
    mixedSupplierCount: readNumber(value, "mixedSupplierCount") ?? 3,
    priceEnabled: readBoolean(value, "priceEnabled") !== false,
    priceLevel: readNumber(value, "priceLevel") ?? 5,
    qualityEnabled: readBoolean(value, "qualityEnabled") !== false,
    qualityLevel: readNumber(value, "qualityLevel") ?? 3,
    coreDemandItemIds: Array.isArray(value.coreDemandItemIds)
      ? value.coreDemandItemIds.map(Number).filter((item) => Number.isFinite(item) && item > 0)
      : [],
    strategyVersion: readNumber(value, "strategyVersion") ?? 1
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
  const aiSource = isRecord(unwrapped.aiProcessing) ? unwrapped.aiProcessing : null;
  const aiStatus = aiSource ? readString(aiSource, "status") : undefined;
  const supportedAiStatuses = ["DETERMINISTIC", "MODEL_APPLIED", "MODEL_FALLBACK", "MODEL_CONFIGURATION_REQUIRED"];
  const aiProcessing: MaterialComparisonAiProcessing | undefined = aiSource && aiStatus && supportedAiStatuses.includes(aiStatus)
    ? {
        status: aiStatus as MaterialComparisonAiProcessing["status"],
        appliedItemCount: readRequiredNumber(aiSource, "appliedItemCount"),
        fallbackItemCount: readRequiredNumber(aiSource, "fallbackItemCount")
      }
    : undefined;
  return {
    demand: demand ?? undefined,
    supplyInfo: normalizeComparisonSupplyInfo(unwrapped.supplyInfo),
    strategies: strategySource.map(normalizeComparisonStrategy).filter((item): item is MaterialComparisonStrategy => Boolean(item)),
    items: itemSource.map(normalizeComparisonItem).filter((item): item is MaterialComparisonItem => Boolean(item)),
    isOrdered: readBoolean(unwrapped, "isOrdered"),
    isDiscarded: readBoolean(unwrapped, "isDiscarded"),
    existingPurchaseOrderId: readNumber(unwrapped, "existingPurchaseOrderId"),
    aiProcessing,
    strategySettings: normalizeComparisonStrategySettings(unwrapped.strategySettings)
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
    sourceFileId: readString(unwrapped, "sourceFileId"),
    sourceFileName: readString(unwrapped, "sourceFileName"),
    inquiryNo: readString(unwrapped, "inquiryNo"),
    requestNo: readString(unwrapped, "requestNo"),
    vesselName: readString(unwrapped, "vesselName"),
    materialType: readString(unwrapped, "materialType"),
    currency: readString(unwrapped, "currency"),
    suggestedPort: readString(unwrapped, "suggestedPort"),
    eta: readString(unwrapped, "eta"),
    recipientCompany: readString(unwrapped, "recipientCompany"),
    handlerName: readString(unwrapped, "handlerName"),
    handlerEmail: readString(unwrapped, "handlerEmail"),
    rawHeaderFields: normalizeStringRecord(unwrapped.rawHeaderFields),
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

export async function getMaterialAiCapabilities(): Promise<MaterialAiCapabilities> {
  const payload = await requestJson(MATERIAL_AI_CAPABILITIES_ENDPOINT);
  const source = isRecord(payload) ? payload : {};
  const categoryAnalysisConfigured = readBoolean(source, "categoryAnalysisConfigured") === true;
  const comparisonRerankConfigured = readBoolean(source, "comparisonRerankConfigured") === true;
  return {
    categoryAnalysisConfigured,
    comparisonRerankConfigured,
    status: readString(source, "status") || (categoryAnalysisConfigured || comparisonRerankConfigured ? "CONFIGURED" : "MODEL_CONFIGURATION_REQUIRED")
  };
}

export async function listMaterialDemands(query: { keyword?: string; status?: string; dateFrom?: string; dateTo?: string; stage?: string; page?: number; size?: number } = {}): Promise<MaterialDemandListResponse> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("keyword", query.keyword.trim());
  if (query.status?.trim()) params.set("status", query.status.trim());
  if (query.dateFrom?.trim()) params.set("dateFrom", query.dateFrom.trim());
  if (query.dateTo?.trim()) params.set("dateTo", query.dateTo.trim());
  if (query.stage?.trim()) params.set("stage", query.stage.trim());
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

export async function saveMaterialComparisonStrategy(
  demandId: number | string,
  payload: MaterialComparisonStrategySettingsPayload
) {
  return normalizeComparisonStrategySettings(await requestJson(
    `${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/comparison-strategy`,
    { method: "PUT", body: JSON.stringify(payload) }
  ));
}

export async function discardMaterialDemand(demandId: number | string): Promise<unknown> {
  return requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/discard`, { method: "POST" });
}

export async function saveMaterialComparisonQuotes(
  demandId: number | string,
  payload: {
    strategyType?: string;
    markupPercent?: number;
    fixedFreightFee?: number;
    fixedCustomsFee?: number;
    fixedCraneFee?: number;
    fixedOtherFee?: number;
    trafficService?: MaterialDemandTrafficService;
    items: Array<{
      demandItemId?: number | string;
      skuId?: number;
      selectedUnit?: string;
      quantity?: string;
      remarks?: string;
      unitPrice?: number;
      unitPriceUsd?: number;
      actualQuotePrice?: number;
      quoteMarkupPercent?: number;
      currency?: string;
    }>;
  }
): Promise<{ demandId?: number; savedCount?: number }> {
  const response = await requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/comparison-quotes`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
  return isRecord(response) ? { demandId: readNumber(response, "demandId"), savedCount: readNumber(response, "savedCount") } : {};
}

export async function exportMaterialQuoteTemplate(demandId: number | string): Promise<void> {
  const session = getAuthSession();
  const response = await fetch(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/quote-export`, {
    method: "GET",
    headers: {
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {})
    }
  });
  if (!response.ok) {
    throw createApiError(response, await readJson(response));
  }
  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") || "";
  const filenameStar = disposition.match(/filename\\*=UTF-8''([^;]+)/i);
  const filenameBasic = disposition.match(/filename="?([^";]+)"?/i);
  const fileName = decodeURIComponent(filenameStar?.[1] || filenameBasic?.[1] || `material-quotation-${demandId}.xlsx`);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export async function importMaterialQuoteQuantities(demandId: number | string, file: File): Promise<MaterialComparisonQuoteImportResponse> {
  const session = getAuthSession();
  const formData = new FormData();
  formData.append("file", file);
  const response = await fetch(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/quote-import`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {})
    },
    body: formData
  });
  const payload = await readJson(response);
  if (!response.ok) {
    throw createApiError(response, payload);
  }
  const unwrapped = unwrapPayload(payload);
  const source = isRecord(unwrapped) ? unwrapped : {};
  const itemsSource = Array.isArray(source.items) ? source.items : [];
  return {
    demandId: readNumber(source, "demandId"),
    matchedCount: readNumber(source, "matchedCount"),
    items: itemsSource
      .filter(isRecord)
      .map((item) => ({
        demandItemId: readNumber(item, "demandItemId"),
        sourceRowNumber: readNumber(item, "sourceRowNumber"),
        quantity: readString(item, "quantity")
      }))
  };
}

export async function listMaterialDemandItemSupplierCandidates(demandId: number | string, itemId: number | string): Promise<MaterialComparisonCandidate[]> {
  const payload = await requestJson(`${MATERIAL_DEMAND_ENDPOINT}/${encodeURIComponent(String(demandId))}/items/${encodeURIComponent(String(itemId))}/supplier-candidates`);
  const unwrapped = unwrapPayload(payload);
  const source = Array.isArray(unwrapped) ? unwrapped : [];
  return source.map(normalizeComparisonCandidate).filter((item): item is MaterialComparisonCandidate => Boolean(item));
}
