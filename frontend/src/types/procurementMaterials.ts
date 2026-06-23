export type MaterialDocumentType = "DEMAND_INQUIRY" | "SUPPLIER_QUOTATION" | "UNKNOWN" | string;

export type MaterialMatchResult = "EXACT" | "SIMILAR" | "UNMATCHED" | string;

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
  hasImage?: boolean;
  imageIndex?: number;
  imageAnchor?: string;
  candidateSnapshot?: MaterialMatchCandidate[];
  candidates?: MaterialMatchCandidate[];
  supplierCandidates?: MaterialSupplierCandidate[];
}

export interface MaterialMatchPreviewResponse {
  documentType: MaterialDocumentType;
  sourceFormat?: string;
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
  vesselName?: string;
  supplyPortCode?: string;
  supplyPortName?: string;
  vesselEta?: string;
  inquiryDate?: string;
  sourceFileName?: string;
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
  vesselName?: string;
  supplyPortCode?: string;
  supplyPortName?: string;
  vesselEta?: string;
  inquiryDate?: string;
  sourceFileName?: string;
  documentType?: MaterialDocumentType;
  headerRowIndex?: number;
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
}

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
  sourceSkuCode?: string;
  sourceSkuName?: string;
  lowestCandidate?: MaterialComparisonCandidate;
  singleSupplierCandidate?: MaterialComparisonCandidate;
  candidates: MaterialComparisonCandidate[];
  emptyReason?: string;
}

export interface MaterialDemandComparisonResponse {
  demand?: MaterialDemandSummary;
  supplyInfo?: MaterialComparisonSupplyInfo;
  strategies: MaterialComparisonStrategy[];
  items: MaterialComparisonItem[];
  isOrdered?: boolean;
  isDiscarded?: boolean;
  existingPurchaseOrderId?: number;
}
