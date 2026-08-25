export interface AuthCompany {
  id?: string | number;
  companyId?: string | number;
  name?: string;
  companyName?: string;
  type?: string;
  companyType?: string;
  status?: string;
  supplierServiceTypes?: string[];
}

export interface AuthUser {
  id?: string | number;
  userId?: string | number;
  account?: string;
  username?: string;
  name?: string;
  displayName?: string;
  phone?: string;
  email?: string;
  status?: string;
  userType?: string;
}

export interface AuthMenu {
  key?: string;
  code?: string;
  label?: string;
  labelKey?: string;
  route?: string;
  path?: string;
  requiredPermission?: string;
  children?: AuthMenu[];
}

export interface AuthSession {
  token: string;
  user?: AuthUser | null;
  company?: AuthCompany | null;
  roles: string[];
  permissions: string[];
  menus: AuthMenu[];
  defaultRoute: string;
  profileStatus: CompanyProfileStatus;
  reviewReason?: string;
}

export interface LoginRequest {
  account: string;
  password: string;
  remember?: boolean;
}

export interface RegisterRequest {
  account: string;
  password: string;
  confirmPassword: string;
  companyType: string;
  supplierServiceTypes?: string[];
}

export interface RegistrationSubmissionRequest extends RegisterRequest {
  companyName: string;
  unifiedSocialCreditCode: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
}

export interface RegisterOption {
  value: string;
  labelKey?: string;
  label?: string;
}

export interface RegisterConfiguration {
  companyTypes: RegisterOption[];
  supplierServiceTypes: RegisterOption[];
}

export type CompanyProfileStatus = "PROFILE_REQUIRED" | "PENDING_REVIEW" | "REJECTED" | "ACTIVE" | "";

export interface QualificationFile {
  id?: string | number;
  fileId?: string | number;
  name: string;
  url?: string;
}

export interface CompanyProfile {
  companyType: string;
  companyName: string;
  unifiedSocialCreditCode?: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  qualificationFiles: QualificationFile[];
  status?: CompanyProfileStatus;
  reviewReason?: string;
  supplierServiceTypes: string[];
}

export interface CompanyProfileRequest {
  companyType: string;
  companyName: string;
  unifiedSocialCreditCode?: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  qualificationFileIds: Array<string | number>;
  qualificationFiles?: QualificationFile[];
  supplierServiceTypes: string[];
}
