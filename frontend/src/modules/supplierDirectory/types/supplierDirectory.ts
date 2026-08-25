export type SupplierDirectoryStatus = "ACTIVE" | "DISABLED" | string;

export type SupplierDirectoryItem = {
  companyId: number;
  id: string;
  name: string;
  creditCode: string;
  logoFileId: string;
  logoUrl: string;
  introduction: string;
  servicePorts: string[];
  categories: string[];
  status: SupplierDirectoryStatus;
  skuCount: number;
  categoryCount: number;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  averageRating: number | null;
  evaluationCount: number;
  positiveRate: number | null;
  reputationLevel: string;
};

export type SupplierDirectoryFilters = {
  keyword: string;
  port: string;
  category: string;
  status: string;
};

export type SupplierDirectoryListResponse = {
  items: SupplierDirectoryItem[];
  total: number;
};

export type SupplierQualification = {
  qualificationId: number;
  companyId: number;
  fileName: string;
  fileUrl: string;
  qualificationType: string;
  title: string;
  description: string;
  contentType: string;
  updatedAt: string;
};
