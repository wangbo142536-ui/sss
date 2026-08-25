import type {
  AuthCompany,
  AuthMenu,
  AuthSession,
  AuthUser,
  CompanyProfile,
  CompanyProfileRequest,
  CompanyProfileStatus,
  LoginRequest,
  QualificationFile,
  RegisterConfiguration,
  RegisterOption,
  RegisterRequest,
  RegistrationSubmissionRequest
} from "@/types/auth";

const AUTH_STORAGE_KEY = "ship-supply-auth-session";
const REGISTER_OPTIONS_ENDPOINT = "/api/auth/register/options";
const LOGIN_ENDPOINT = "/api/auth/login";
const REGISTER_ENDPOINT = "/api/auth/register";
const REGISTER_AND_SUBMIT_ENDPOINT = "/api/auth/register-and-submit";
const ME_ENDPOINT = "/api/auth/me";
const COMPANY_PROFILE_ENDPOINT = "/api/onboarding/company-profile";
const FILE_UPLOAD_ENDPOINT = "/api/files/upload";

const fallbackRegisterOptions: RegisterOption[] = [
  { value: "SHIP_AGENT", labelKey: "page.register.companyTypeShipAgent" },
  { value: "SUPPLIER", labelKey: "page.register.companyTypeSupplier" },
  { value: "BARGE_AGENT", labelKey: "page.register.companyTypeBargeAgent" }
];
const fallbackSupplierServiceTypes: RegisterOption[] = [
  { value: "MATERIAL", labelKey: "page.register.supplierServiceMaterial" },
  { value: "FOOD", labelKey: "page.register.supplierServiceFood" }
];

export class ApiError extends Error {
  status: number;
  payload: unknown;

  constructor(message: string, status: number, payload?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

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

function unwrapPayload(payload: unknown): unknown {
  if (!isRecord(payload)) return payload;
  if ("data" in payload) return payload.data;
  if ("result" in payload) return payload.result;
  return payload;
}

function readString(source: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return "";
}

function readArray(source: Record<string, unknown>, keys: string[]): string[] {
  for (const key of keys) {
    const value = source[key];
    if (Array.isArray(value)) {
      return value
        .map((item) => {
          if (typeof item === "string") return item;
          if (isRecord(item)) return readString(item, ["roleCode", "permissionCode", "code", "key", "name"]);
          return "";
        })
        .filter(Boolean);
    }
  }
  return [];
}

function readErrorMessage(payload: unknown): string {
  if (!isRecord(payload)) return "";
  return readString(payload, ["message", "error", "detail", "msg", "reason"]);
}

function createApiError(response: Response, payload: unknown): ApiError {
  return new ApiError(readErrorMessage(payload) || `HTTP_${response.status}`, response.status, payload);
}

export function isAuthExpiredError(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

export function getSafeRequestErrorKey(error: unknown, fallbackKey: string): string {
  if (isAuthExpiredError(error)) return "page.onboarding.sessionExpired";
  if (!(error instanceof ApiError)) return fallbackKey;

  const message = error.message.toLowerCase();
  if (message.includes("upload") || message.includes("file") || message.includes("qualification") || message.includes("资质") || message.includes("文件")) {
    return "page.onboarding.fileUploadFailed";
  }
  if (message.includes("required") || message.includes("missing") || message.includes("blank") || message.includes("缺失") || message.includes("不能为空")) {
    return "page.onboarding.profileIncomplete";
  }
  if (message.includes("token") || message.includes("auth") || message.includes("登录")) {
    return "page.onboarding.sessionExpired";
  }

  return fallbackKey;
}

function normalizeMenus(value: unknown): AuthMenu[] {
  if (!Array.isArray(value)) return [];
  return value.filter(isRecord) as AuthMenu[];
}

function normalizeProfileStatus(value: string): CompanyProfileStatus {
  if (value === "PROFILE_REQUIRED" || value === "PENDING_REVIEW" || value === "REJECTED" || value === "ACTIVE") {
    return value;
  }
  return "";
}

function normalizeSession(payload: unknown): AuthSession {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new Error("AUTH_RESPONSE_INVALID");
  }

  const token = readString(unwrapped, ["token", "accessToken", "jwt", "access_token"]);
  if (!token) {
    throw new Error("AUTH_TOKEN_MISSING");
  }

  const user = isRecord(unwrapped.user) ? (unwrapped.user as AuthUser) : null;
  const company = isRecord(unwrapped.company) ? (unwrapped.company as AuthCompany) : null;
  const roles = readArray(unwrapped, ["roles", "roleCodes", "authorities"]);
  const permissions = readArray(unwrapped, ["permissions", "permissionCodes", "permissionPoints"]);
  const menus = normalizeMenus(unwrapped.menus);
  const defaultRoute = readString(unwrapped, ["defaultRoute", "defaultPath", "homePath", "redirectTo"]) || "/dashboard-government";
  const companyRecord = company ? (company as Record<string, unknown>) : {};
  const userRecord = user ? (user as Record<string, unknown>) : {};
  const profileStatus = normalizeProfileStatus(
    readString(unwrapped, ["profileStatus", "companyProfileStatus", "reviewStatus", "status"]) ||
      readString(companyRecord, ["profileStatus", "companyProfileStatus", "reviewStatus", "status"]) ||
      readString(userRecord, ["profileStatus", "companyProfileStatus", "reviewStatus"])
  );
  const reviewReason =
    readString(unwrapped, ["reviewReason", "rejectReason", "rejectedReason"]) ||
    readString(companyRecord, ["reviewReason", "rejectReason", "rejectedReason"]);

  return {
    token,
    user,
    company,
    roles,
    permissions,
    menus,
    defaultRoute,
    profileStatus,
    reviewReason: reviewReason || undefined
  };
}

function normalizeOptionList(value: unknown, fallback: RegisterOption[]): RegisterOption[] {
  if (!Array.isArray(value)) return fallback;
  const options = value
    .map((item) => {
      if (typeof item === "string") return { value: item };
      if (!isRecord(item)) return null;
      const optionValue = readString(item, ["value", "code", "type", "roleCode"]);
      if (!optionValue) return null;
      return {
        value: optionValue,
        label: readString(item, ["label", "name", "nameCn", "text"]) || undefined,
        labelKey: readString(item, ["labelKey", "i18nKey"]) || undefined
      };
    })
    .filter(Boolean) as RegisterOption[];
  return options.length ? options : fallback;
}

function normalizeRegisterOptions(payload: unknown): RegisterConfiguration {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    return { companyTypes: fallbackRegisterOptions, supplierServiceTypes: fallbackSupplierServiceTypes };
  }
  return {
    companyTypes: normalizeOptionList(unwrapped.companyTypes ?? unwrapped.enterpriseTypes ?? unwrapped.roles, fallbackRegisterOptions),
    supplierServiceTypes: normalizeOptionList(unwrapped.supplierServiceTypes, fallbackSupplierServiceTypes)
  };
}

function normalizeProfile(payload: unknown): CompanyProfile | null {
  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) return null;

  const company = isRecord(unwrapped.company) ? unwrapped.company : {};
  const rawFiles = Array.isArray(unwrapped.qualificationFiles)
    ? unwrapped.qualificationFiles
    : Array.isArray(unwrapped.qualifications)
      ? unwrapped.qualifications
      : [];
  const files = rawFiles.length
    ? (rawFiles.filter(isRecord).map((item) => ({
        id: item.id as string | number | undefined,
        fileId: item.fileId as string | number | undefined,
        name: readString(item, ["name", "fileName", "originalName"]),
        url: readString(item, ["url", "fileUrl"])
      })) as QualificationFile[])
    : [];

  return {
    companyType: readString(unwrapped, ["companyType", "type"]) || readString(company, ["companyType", "type"]),
    companyName: readString(unwrapped, ["companyName", "name"]) || readString(company, ["companyName", "name"]),
    unifiedSocialCreditCode: readString(unwrapped, ["unifiedSocialCreditCode", "creditCode"]) || readString(company, ["unifiedSocialCreditCode", "creditCode"]) || undefined,
    contactName: readString(unwrapped, ["contactName", "contact"]) || readString(company, ["contactName", "contact"]),
    contactPhone: readString(unwrapped, ["contactPhone", "phone"]) || readString(company, ["contactPhone", "phone"]),
    contactEmail: readString(unwrapped, ["contactEmail", "email"]) || readString(company, ["contactEmail", "email"]),
    qualificationFiles: files,
    status: normalizeProfileStatus(readString(unwrapped, ["status", "profileStatus", "reviewStatus", "companyStatus"]) || readString(company, ["status", "profileStatus", "reviewStatus"])),
    reviewReason: readString(unwrapped, ["reviewReason", "rejectReason", "rejectedReason"]) || undefined,
    supplierServiceTypes: readArray(company, ["supplierServiceTypes", "serviceTypes"])
  };
}

async function requestJson(endpoint: string, init: RequestInit): Promise<unknown> {
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(init.headers ?? {})
    }
  });
  const payload = await readJson(response);

  if (!response.ok) {
    throw createApiError(response, payload);
  }

  return payload;
}

async function requestAuthorizedJson(endpoint: string, init: RequestInit = {}): Promise<unknown> {
  const session = getAuthSession();
  return requestJson(endpoint, {
    ...init,
    headers: {
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {})
    }
  });
}

export async function login(request: LoginRequest): Promise<AuthSession> {
  const payload = await requestJson(LOGIN_ENDPOINT, {
    method: "POST",
    body: JSON.stringify(request)
  });
  return normalizeSession(payload);
}

export async function register(request: RegisterRequest): Promise<AuthSession> {
  const payload = await requestJson(REGISTER_ENDPOINT, {
    method: "POST",
    body: JSON.stringify({
      ...request
    })
  });

  try {
    return normalizeSession(payload);
  } catch {
    throw new ApiError("AUTH_TOKEN_MISSING", 0, payload);
  }
}

export async function registerAndSubmit(request: RegistrationSubmissionRequest, files: File[]): Promise<AuthSession> {
  const formData = new FormData();
  formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
  files.forEach((file) => formData.append("files", file));
  const response = await fetch(REGISTER_AND_SUBMIT_ENDPOINT, { method: "POST", body: formData });
  const payload = await readJson(response);
  if (!response.ok) throw createApiError(response, payload);
  return normalizeSession(payload);
}

export async function getRegisterOptions(): Promise<RegisterOption[]> {
  return (await getRegisterConfiguration()).companyTypes;
}

export async function getRegisterConfiguration(): Promise<RegisterConfiguration> {
  try {
    const payload = await requestJson(REGISTER_OPTIONS_ENDPOINT, { method: "GET" });
    return normalizeRegisterOptions(payload);
  } catch {
    return { companyTypes: fallbackRegisterOptions, supplierServiceTypes: fallbackSupplierServiceTypes };
  }
}

export async function getCurrentAuth(): Promise<AuthSession> {
  const payload = await requestAuthorizedJson(ME_ENDPOINT, { method: "GET" });
  return normalizeSession(payload);
}

export async function getCompanyProfile(): Promise<CompanyProfile | null> {
  const payload = await requestAuthorizedJson(COMPANY_PROFILE_ENDPOINT, { method: "GET" });
  return normalizeProfile(payload);
}

export async function submitCompanyProfile(request: CompanyProfileRequest): Promise<CompanyProfile | null> {
  const payload = await requestAuthorizedJson(COMPANY_PROFILE_ENDPOINT, {
    method: "POST",
    body: JSON.stringify(request)
  });
  return normalizeProfile(payload);
}

export async function uploadQualificationFile(file: File): Promise<QualificationFile> {
  const session = getAuthSession();
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(FILE_UPLOAD_ENDPOINT, {
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

  const unwrapped = unwrapPayload(payload);
  if (!isRecord(unwrapped)) {
    throw new ApiError("FILE_UPLOAD_RESPONSE_INVALID", 0, payload);
  }

  const fileId = (unwrapped.fileId ?? unwrapped.id) as string | number | undefined;
  if (!fileId) {
    throw new ApiError("FILE_UPLOAD_ID_MISSING", 0, payload);
  }

  return {
    id: (unwrapped.id ?? unwrapped.fileId) as string | number | undefined,
    fileId,
    name: readString(unwrapped, ["name", "fileName", "originalName"]) || file.name,
    url: readString(unwrapped, ["url", "fileUrl"]) || undefined
  };
}

export function resolveAuthRoute(session: AuthSession): string {
  if (session.profileStatus === "PENDING_REVIEW") {
    return "/onboarding/review-status";
  }

  if (session.profileStatus === "PROFILE_REQUIRED" || session.profileStatus === "REJECTED") {
    return "/onboarding/company-profile";
  }

  return session.defaultRoute === "/dashboard" ? "/dashboard-government" : session.defaultRoute || "/dashboard-government";
}

export function saveAuthSession(session: AuthSession): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function getAuthSession(): AuthSession | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;

  try {
    return normalizeSession(JSON.parse(raw));
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function clearAuthSession(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
}
