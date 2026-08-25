export type ShopCatalogProductType = "MATERIAL" | "FOOD" | string;

export type ShopCatalogCategoryOption = {
  value: string;
  label: string;
};

export type ShopCatalogSpec = {
  id: string;
  key: string;
  name: string;
  value: string;
  unit: string;
};

export type ShopQualitySelectionAuditEntry = {
  occurredAt: string;
  operatorUserId: number;
  action: string;
  inspectionContent: string;
  inspectionProcess: string;
  inspectionConclusion: string;
  reportFileId: string;
  reportFileName: string;
};

export type ShopQualitySelection = {
  qualitySelectionId: number;
  companyId: number;
  skuId: number;
  inspectionTime: string;
  inspectionContent: string;
  inspectionProcess: string;
  reportFileId: string;
  reportFileName: string;
  reportUrl: string;
  inspectionConclusion: string;
  auditTrail: ShopQualitySelectionAuditEntry[];
  status: string;
  updatedAt: string;
};

export type ShopQualitySelectionSavePayload = Pick<
  ShopQualitySelection,
  "inspectionTime" | "inspectionContent" | "inspectionProcess" | "reportFileId" | "reportFileName" | "inspectionConclusion"
>;

export type ShopCatalogSku = {
  id: string;
  skuId?: string | number;
  productType: ShopCatalogProductType;
  category?: string;
  categoryCode: string;
  categoryName: string;
  productName: string;
  productDescription?: string;
  productTags?: string[];
  qualitySelection?: ShopQualitySelection;
  supplierSkuCode: string;
  platformCode: string;
  impaCode?: string;
  thumbnail: string;
  imageUrl: string;
  packing: string;
  stock: number;
  unit: string;
  price: number;
  currency: string;
  specs?: string[];
  specItems?: ShopCatalogSpec[];
  leadTimeDays?: number;
  deliveryArea?: string;
  brand?: string;
  barcode?: string;
  codingStatus?: string;
  listingStatus?: string;
};

export type ShopCatalogCategory = {
  key: string;
  code: string;
  label: string;
  skuCount: number;
  skus: ShopCatalogSku[];
};

export type ShopCatalogGroup = {
  key: string;
  label: string;
  skuCount: number;
  categories: ShopCatalogCategory[];
};
