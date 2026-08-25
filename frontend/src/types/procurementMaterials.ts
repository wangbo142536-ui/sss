export type MaterialDocumentType = "DEMAND_INQUIRY" | "SUPPLIER_QUOTATION" | "UNKNOWN" | string;

export type MaterialMatchResult = "EXACT" | "SIMILAR" | "UNMATCHED" | string;
export type MaterialValidationStatus = "MATCHED" | "ABNORMAL" | string;

export interface MaterialMatchCandidate {
  impaCode?: string;
  nameCn?: string;
  nameEn?: string;
  specification?: string;
  unit?: string;
  candidateMatchType?: string;
  matchScore?: number;
  reason?: string;
}

export interface MaterialSupplierCandidate {
  skuId?: number;
  companyId?: number;
  supplierName?: string;
  supplierSkuCode?: string;
  productName?: string;
  impaCode?: string;
  platformCode?: string;
  categoryCode?: string;
  categoryName?: string;
  specification?: string;
  specifications?: unknown;
  attributeSummary?: string;
  unitPrice?: number;
  currency?: string;
  currencySymbol?: string;
  unit?: string;
  stockQty?: number;
  stockUnit?: string;
  packageSpec?: string;
  imageUrl?: string;
  thumbnailUrl?: string;
  shelfStatus?: string;
  codeStatus?: string;
  matchType?: string;
  reason?: string;
  productTags?: string[];
  qualityScore?: number;
  priceScore?: number;
}

export interface MaterialParsedAttribute {
  key?: string;
  name?: string;
  value?: string;
  unit?: string;
  rawText?: string;
}

export interface MaterialMatchPreviewItem {
  documentType?: MaterialDocumentType;
  sourceFormat?: string;
  headerRowIndex?: number;
  sequence?: number;
  sourceRowNo?: number;
  sourceRowNumber?: number;
  rawColumns?: Record<string, string>;
  impaCode?: string;
  platformCode?: string;
  cleanName?: string;
  coreName?: string;
  parsedAttributes?: MaterialParsedAttribute[];
  riskFlags?: string[];
  description?: string;
  sizeModel?: string;
  quantity?: string;
  unit?: string;
  remarks?: string;
  supplierItemNo?: string;
  rawNameSpec?: string;
  price?: string;
  packing?: string;
  stock?: string;
  selectedImpaCode?: string;
  candidateImpaCode?: string;
  candidateNameCn?: string;
  candidateNameEn?: string;
  candidateSpec?: string;
  matchResult?: MaterialMatchResult;
  matchResultName?: string;
  reason?: string;
  validationStatus?: MaterialValidationStatus;
  validationReason?: string;
  hasImage?: boolean;
  imageIndex?: number;
  imageAnchor?: string;
  actualQuotePrice?: number;
  actualQuoteCurrency?: string;
  quoteMarkupPercent?: number;
  quoteSupplierSkuId?: number;
  quoteSelectedUnit?: string;
  quoteUnitPrice?: number;
  quoteUnitPriceUsd?: number;
  quoteStrategyType?: string;
  candidateSnapshot?: MaterialMatchCandidate[];
  candidates?: MaterialMatchCandidate[];
  supplierCandidates?: MaterialSupplierCandidate[];
}

export interface MaterialMatchPreviewResponse {
  documentType: MaterialDocumentType;
  sourceFormat?: string;
  sourceFileId?: string;
  sourceFileName?: string;
  inquiryNo?: string;
  requestNo?: string;
  vesselName?: string;
  materialType?: string;
  currency?: string;
  suggestedPort?: string;
  eta?: string;
  recipientCompany?: string;
  handlerName?: string;
  handlerEmail?: string;
  rawHeaderFields?: Record<string, string>;
  headerRowIndex: number;
  totalRows: number;
  exactCount: number;
  similarCount: number;
  unmatchedCount: number;
  items: MaterialMatchPreviewItem[];
}

export interface MaterialDemandSummary {
  demandId: number;
  demandNo: string;
  applicationNo?: string;
  inquiryNo?: string;
  materialType?: string;
  currency?: string;
  fixedFreightFee?: number;
  fixedCustomsFee?: number;
  fixedCraneFee?: number;
  fixedOtherFee?: number;
  trafficService?: MaterialDemandTrafficService;
  recipientCompany?: string;
  handlerName?: string;
  handlerEmail?: string;
  vesselName?: string;
  supplyPortCode?: string;
  supplyPortName?: string;
  vesselEta?: string;
  inquiryDate?: string;
  sourceFileName?: string;
  sourceFileId?: string;
  documentType?: MaterialDocumentType;
  headerRowIndex?: number;
  skuCount: number;
  exactCount: number;
  similarCount: number;
  unmatchedCount: number;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MaterialDemandTrafficCargo {
  cargoName?: string;
  weightKg?: number;
  volumeCbm?: number;
}

export interface MaterialDemandTrafficService {
  trafficServiceOrderId?: number;
  bookingId?: number;
  selectedNodeIndex?: number;
  departurePoint?: string;
  destinationPoint?: string;
  startTime?: string;
  returnTime?: string;
  serviceNodes?: Array<{ nodeName?: string; startTime?: string; endTime?: string }>;
  supplyMode?: "SEA" | "LAND" | string;
  supplyAddress?: string;
  supplyRemark?: string;
  fixedProviderType?: "BARGE" | "SUPPLIER" | string;
  fixedProviderId?: string | number;
  fixedProviderName?: string;
  shuttleNo?: string;
  trafficVesselName?: string;
  seaArea?: string;
  anchorageCode?: string;
  anchorageName?: string;
  useTime?: string;
  serviceType?: "PERSONNEL" | "GOODS" | string;
  passengerType?: string;
  passengerCount?: number;
  cargoType?: string;
  returnTrip?: boolean;
  allowShare?: boolean;
  basePrice?: number;
  sharedPrice?: number;
  remark?: string;
  cargos?: MaterialDemandTrafficCargo[];
}

export interface MaterialDemandDetail {
  demand: MaterialDemandSummary;
  items: MaterialMatchPreviewItem[];
}

export interface MaterialDemandListResponse {
  items: MaterialDemandSummary[];
  page: number;
  size: number;
  total: number;
}

export interface MaterialDemandSavePayload {
  demandId?: number;
  demandNo?: string;
  applicationNo?: string;
  inquiryNo?: string;
  materialType?: string;
  currency?: string;
  recipientCompany?: string;
  handlerName?: string;
  handlerEmail?: string;
  vesselName?: string;
  supplyPortCode?: string;
  supplyPortName?: string;
  vesselEta?: string;
  inquiryDate?: string;
  sourceFileName?: string;
  sourceFileId?: string;
  documentType?: MaterialDocumentType;
  headerRowIndex?: number;
  trafficService?: MaterialDemandTrafficService;
  items: MaterialMatchPreviewItem[];
}

export interface MaterialDemandSaveResponse {
  demandId: number;
  demandNo: string;
  status?: string;
  defaultRoute?: string;
  redirectTo?: string;
}

export interface MaterialComparisonSupplyInfo {
  vesselName?: string;
  supplyPort?: string;
  supplyPortCode?: string;
  supplyPortName?: string;
  vesselEta?: string;
  port?: string;
  supplyDate?: string;
  inquiryDate?: string;
  weather?: string;
  weatherInfo?: string;
  weatherText?: string;
}

export interface MaterialComparisonSupplierSummary {
  supplierId?: number;
  companyId?: number;
  supplierName?: string;
  matchedCount?: number;
  skuCount?: number;
  totalAmount?: number;
  totalAmountUsd?: number;
  amount?: number;
  currency?: string;
  attributeTags?: string[];
  coreItemCount?: number;
  qualityScore?: number;
  priceScore?: number;
}

export interface MaterialComparisonStrategy {
  strategyType: string;
  strategyName?: string;
  matchedCount: number;
  totalCount: number;
  unmatchedCount: number;
  unpricedCount: number;
  totalAmount?: number;
  totalAmountUsd?: number;
  currency?: string;
  suppliers: MaterialComparisonSupplierSummary[];
  enabled?: boolean;
  disabledReason?: string;
  attributeTags?: string[];
}

export interface MaterialComparisonCandidate {
  skuId?: number;
  companyId?: number;
  supplierName?: string;
  supplierSkuCode?: string;
  productName?: string;
  impaCode?: string;
  platformCode?: string;
  categoryCode?: string;
  categoryName?: string;
  specification?: string;
  specifications?: unknown;
  attributeSummary?: string;
  unitPrice?: number;
  unitPriceUsd?: number;
  lineAmount?: number;
  lineAmountUsd?: number;
  currency?: string;
  currencySymbol?: string;
  stockQty?: number;
  stockUnit?: string;
  unit?: string;
  selectedUnit?: string;
  unitPriceOptions?: Array<{
    unit?: string;
    unitPrice?: number;
    unitPriceUsd?: number;
    defaultSelected?: boolean;
  }>;
  packageSpec?: string;
  imageUrl?: string;
  thumbnailUrl?: string;
  shelfStatus?: string;
  codeStatus?: string;
  matchType?: string;
  reason?: string;
  productTags?: string[];
  qualityScore?: number;
  priceScore?: number;
}

export interface MaterialComparisonStrategySettings {
  mixedSupplierCount: number;
  priceEnabled: boolean;
  priceLevel: number;
  qualityEnabled: boolean;
  qualityLevel: number;
  coreDemandItemIds: number[];
  strategyVersion: number;
}

export type MaterialComparisonStrategySettingsPayload = Omit<MaterialComparisonStrategySettings, "strategyVersion">;

export interface MaterialComparisonItem {
  demandItemId?: number;
  rowNo?: number;
  impaCode?: string;
  platformCode?: string;
  productName?: string;
  description?: string;
  specification?: string;
  quantity?: string;
  pricingQuantity?: number;
  pricingQuantityNote?: string;
  unit?: string;
  remarks?: string;
  sourceSkuCode?: string;
  sourceSkuName?: string;
  lowestCandidate?: MaterialComparisonCandidate;
  singleSupplierCandidate?: MaterialComparisonCandidate;
  candidates: MaterialComparisonCandidate[];
  emptyReason?: string;
  actualQuotePrice?: number;
  actualQuoteCurrency?: string;
  quoteMarkupPercent?: number;
  quoteSupplierSkuId?: number;
  quoteSelectedUnit?: string;
  quoteUnitPrice?: number;
  quoteUnitPriceUsd?: number;
  quoteStrategyType?: string;
}

export interface MaterialDemandComparisonResponse {
  demand?: MaterialDemandSummary;
  supplyInfo?: MaterialComparisonSupplyInfo;
  strategies: MaterialComparisonStrategy[];
  items: MaterialComparisonItem[];
  isOrdered?: boolean;
  isDiscarded?: boolean;
  existingPurchaseOrderId?: number;
  aiProcessing?: MaterialComparisonAiProcessing;
  strategySettings?: MaterialComparisonStrategySettings;
}

export type MaterialComparisonAiStatus =
  | "DETERMINISTIC"
  | "MODEL_APPLIED"
  | "MODEL_FALLBACK"
  | "MODEL_CONFIGURATION_REQUIRED";

export interface MaterialComparisonAiProcessing {
  status: MaterialComparisonAiStatus;
  appliedItemCount: number;
  fallbackItemCount: number;
}

export interface MaterialComparisonQuoteImportItem {
  demandItemId?: number;
  sourceRowNumber?: number;
  quantity?: string;
}

export interface MaterialComparisonQuoteImportResponse {
  demandId?: number;
  matchedCount?: number;
  items: MaterialComparisonQuoteImportItem[];
}
