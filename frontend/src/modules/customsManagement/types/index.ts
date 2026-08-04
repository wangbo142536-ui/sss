export type CustomsBusinessType = "MATERIAL" | "FOOD";
export type CustomsStatus = "DECLARING" | "DECLARED";

export type CustomsDeclarationItem = {
  id: number;
  code: string;
  productName: string;
  specification: string;
  quantity: string;
  unit: string;
};

export type CustomsDeclarationContext = {
  businessType: CustomsBusinessType;
  purchaseOrderId: number;
  purchaseOrderNo: string;
  responsibleType: "SUPPLIER" | "BARGE";
  responsibleCompanyId: number;
  responsibleCompanyName: string;
  canDeclare: boolean;
  declarationId?: number;
  status: CustomsStatus | "";
  customsFee: number;
  items: CustomsDeclarationItem[];
};

export type CustomsDeclarationRecord = {
  id: number;
  businessType: CustomsBusinessType;
  purchaseOrderId: number;
  purchaseOrderNo: string;
  buyerCompanyId: number;
  responsibleCompanyId: number;
  responsibleType: "SUPPLIER" | "BARGE";
  responsibleCompanyName: string;
  status: CustomsStatus;
  vesselName: string;
  shipAgent: string;
  goodsCategory: string;
  tradeType: string;
  declarantCompany: string;
  declarantContact: string;
  declarantPhone: string;
  deliveryStart: string;
  deliveryEnd: string;
  deliveryLocation: string;
  supplyVessel: string;
  captainContact: string;
  applicant: string;
  applicantPhone: string;
  applicationDate: string;
  customsFee: number;
  declaredAt: string;
};

export type CustomsDeclarationPage = {
  items: CustomsDeclarationRecord[];
  total: number;
  page: number;
  size: number;
};
