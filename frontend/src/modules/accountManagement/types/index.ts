export type PlatformAccount = {
  userId: string;
  username: string;
  name: string;
  phone: string;
  email: string;
  userType: string;
  accountSource: "REGISTERED_ADMIN" | "INTERNAL_CREATED" | string;
  companyOwner: boolean;
  roleCodes: string[];
  status: string;
  createdAt: string;
  lastLoginAt: string;
};

export type PlatformCompanyAccounts = {
  companyId: string;
  companyName: string;
  companyType: string;
  supplierServiceTypes: string[];
  companyStatus: string;
  accountCount: number;
  activeAccountCount: number;
  accounts: PlatformAccount[];
};

export type PlatformCompanyAccountPage = {
  items: PlatformCompanyAccounts[];
  page: number;
  pageSize: number;
  totalCompanies: number;
  totalAccounts: number;
};

export type PlatformAccountQuery = {
  companyType?: string;
  supplierServiceType?: string;
  accountSource?: string;
  roleCode?: string;
  status?: string;
  keyword?: string;
  page?: number;
  pageSize?: number;
};
