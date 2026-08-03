export type FoodDemandItem = {
  itemId?: number;
  sourceRow?: number;
  sequenceNo: number;
  nameEn?: string;
  nameZh?: string;
  remark?: string;
  specification?: string;
  unit: string;
  requestedQuantity: number;
  matchStatus: string;
  matchReason?: string;
  matchKey?: string;
  rawColumns?: Record<string, string>;
};

export type FoodDemandSummary = {
  demandId: number;
  demandNo: string;
  inquiryNo?: string;
  vesselName: string;
  supplyPort: string;
  vesselEta: string;
  currency: string;
  status: string;
  itemCount: number;
  matchedCount: number;
  pendingCount: number;
  supplierCount: number;
  submittedQuoteCount: number;
  inquirySentAt?: string;
  quoteDeadlineAt?: string;
  updatedAt: string;
};

export type FoodDemandDetail = {
  demand: FoodDemandSummary;
  sourceFileName?: string;
  sourceSheetName?: string;
  items: FoodDemandItem[];
};

export type FoodMatchPreview = {
  fileName: string;
  selectedSheet: string;
  headerRow: number;
  totalRows: number;
  matchedCount: number;
  pendingCount: number;
  sheets: Array<{ name: string; headerRow: number; validRows: number }>;
  items: FoodDemandItem[];
};

export type FoodDemandSavePayload = {
  demandId?: number;
  inquiryNo?: string;
  vesselName: string;
  supplyPort: string;
  vesselEta: string;
  quoteDeadlineAt: string;
  currency: string;
  sourceFileName?: string;
  sourceSheetName?: string;
  items: FoodDemandItem[];
};

export type FoodSupplier = {
  supplierCompanyId: number;
  supplierName: string;
  companyType: string;
};

export type FoodInquiry = {
  inquirySupplierId: number;
  demandId: number;
  quoteId?: number;
  demandNo: string;
  inquiryNo?: string;
  vesselName: string;
  supplyPort: string;
  vesselEta: string;
  buyerName: string;
  supplierName: string;
  status: string;
  quoteStatus: string;
  itemCount: number;
  quotedItemCount: number;
  missingItemCount: number;
  totalAmount: number;
  sentAt?: string;
  quoteDeadlineAt?: string;
  updatedAt: string;
};

export type FoodQuoteItem = {
  quoteItemId: number;
  demandItemId: number;
  sequenceNo: number;
  nameEn?: string;
  nameZh?: string;
  remark?: string;
  specification?: string;
  unit: string;
  requestedQuantity: number;
  quotedQuantity?: number;
  unitPrice?: number;
  amount?: number;
  availability: "AVAILABLE" | "PARTIAL" | "UNAVAILABLE";
  priceSource: "MANUAL" | "IMPORTED" | "VIRTUAL";
  matchStatus: string;
  supplierRemark?: string;
};

export type FoodQuote = {
  quoteId: number;
  demandId: number;
  quoteNo: string;
  demandNo: string;
  inquiryNo?: string;
  vesselName: string;
  supplyPort: string;
  vesselEta: string;
  buyerName: string;
  supplierName: string;
  currency: string;
  status: string;
  versionNo: number;
  totalAmount: number;
  quotedItemCount: number;
  missingItemCount: number;
  quantityDifferenceCount: number;
  submittedAt?: string;
  updatedAt: string;
  items: FoodQuoteItem[];
};

export type FoodImportPreview = {
  batchId: number;
  fileName: string;
  selectedSheet: string;
  matchedCount: number;
  issueCount: number;
  updates: Array<{
    sourceRow: number;
    quoteItemId: number;
    demandItemId: number;
    quotedQuantity: number;
    unitPrice: number;
    matchStatus: string;
  }>;
  issues: Array<{
    sourceRow: number;
    nameEn?: string;
    nameZh?: string;
    specification?: string;
    unit?: string;
    issueType: string;
    message: string;
  }>;
};

export type FoodComparisonOption = {
  demandItemId: number;
  quoteItemId: number;
  quoteId: number;
  supplierCompanyId: number;
  supplierName: string;
  requestedQuantity: number;
  quotedQuantity: number;
  unitPrice: number;
  amount: number;
  availability: string;
  priceSource: string;
  quantitySatisfied: boolean;
  lowestPrice: boolean;
};

export type FoodComparisonSettings = {
  markupPercent: number;
  fixedFreightFee: number;
  fixedCustomsFee: number;
  fixedCraneFee: number;
  fixedOtherFee: number;
  supplyMode: "SEA" | "LAND";
  fixedProviderType?: "BARGE" | "SUPPLIER";
  fixedProviderId?: string;
  fixedProviderName?: string;
  trafficServiceJson?: string;
  selectedDemandItemIds?: number[] | null;
};

export type FoodComparison = {
  demand: FoodDemandSummary;
  submittedSupplierCount: number;
  strategies: Array<{
    strategyType: string;
    label: string;
    enabled: boolean;
    coveredItemCount: number;
    totalItemCount: number;
    totalAmount?: number;
    supplierCompanyId?: number;
    supplierName?: string;
  }>;
  items: Array<{
    demandItemId: number;
    sequenceNo: number;
    nameEn?: string;
    nameZh?: string;
    specification?: string;
    unit: string;
    requestedQuantity: number;
    quotes: FoodComparisonOption[];
  }>;
  settings: FoodComparisonSettings;
};

export type FoodOrderSummary = {
  orderId: number;
  orderNo: string;
  demandId: number;
  demandNo: string;
  vesselName: string;
  supplyPort: string;
  vesselEta: string;
  currency: string;
  status: string;
  totalAmount: number;
  supplierCount: number;
  itemCount: number;
  updatedAt: string;
  requiredDeliveryTime?: string;
  deliveryAddress?: string;
  deliveryContactName?: string;
  deliveryContactPhone?: string;
  deliveryContactEmail?: string;
  defaultPackagingMethod?: string;
  buyerRemark?: string;
  fixedFreightFee?: number;
  fixedCustomsFee?: number;
  fixedCraneFee?: number;
  fixedOtherFee?: number;
  supplyMode?: string;
  fixedProviderType?: string;
  fixedProviderId?: string;
  fixedProviderName?: string;
  trafficServiceJson?: string;
};

export type FoodSupplierOrderSummary = FoodOrderSummary & {
  supplierOrderId: number;
  supplierCompanyId: number;
  supplierName: string;
  expectedReadyAt?: string;
};

export type FoodOrderDetail = {
  order: FoodOrderSummary;
  suppliers: Array<{
    supplierOrderId: number;
    supplierCompanyId: number;
    supplierName: string;
    status: string;
    subtotalAmount: number;
    expectedReadyAt?: string;
    rejectReason?: string;
    shipmentRemark?: string;
  }>;
  items: Array<{
    orderItemId: number;
    supplierOrderId: number;
    demandItemId: number;
    quoteItemId: number;
    supplierName: string;
    nameEn?: string;
    nameZh?: string;
    specification?: string;
    unit: string;
    requestedQuantity: number;
    orderedQuantity: number;
    unitPrice: number;
    amount: number;
    quotedUnitPrice?: number;
    quotedAmount?: number;
  }>;
};

export type FoodSettlement = {
  settlementId: number;
  orderId: number;
  orderNo: string;
  supplierOrderId: number;
  supplierName: string;
  buyerCompanyName?: string;
  vesselName?: string;
  currency?: string;
  status: string;
  amount: number;
  actualAmount?: number;
  invoiceNo?: string;
  invoiceFileId?: string;
  invoiceAttachments?: Array<{
    fileId?: string;
    fileName: string;
    fileUrl: string;
  }>;
  updatedAt: string;
};

export type FoodEvaluation = {
  evaluationId: number;
  orderId: number;
  orderNo: string;
  supplierName: string;
  status: string;
  qualityRating?: number;
  logisticsRating?: number;
  comment?: string;
  attachments?: Array<{
    fileId?: string;
    fileName: string;
    fileUrl: string;
  }>;
  reviewRemark?: string;
  updatedAt: string;
};
