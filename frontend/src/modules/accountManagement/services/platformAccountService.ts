import { ApiError, getAuthSession } from "@/services/authService";
import type {
  PlatformAccount,
  PlatformAccountQuery,
  PlatformCompanyAccountPage,
  PlatformCompanyAccounts
} from "@/modules/accountManagement/types";

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function text(value: unknown): string {
  return typeof value === "string" || typeof value === "number" ? String(value) : "";
}

function strings(value: unknown): string[] {
  return Array.isArray(value) ? value.map(text).filter(Boolean) : [];
}

function account(value: unknown): PlatformAccount | null {
  if (!isRecord(value)) return null;
  const userId = text(value.userId ?? value.id);
  if (!userId) return null;
  return {
    userId,
    username: text(value.username),
    name: text(value.name),
    phone: text(value.phone),
    email: text(value.email),
    userType: text(value.userType),
    accountSource: text(value.accountSource),
    companyOwner: value.companyOwner === true || value.isCompanyOwner === true,
    roleCodes: strings(value.roleCodes),
    status: text(value.status),
    createdAt: text(value.createdAt),
    lastLoginAt: text(value.lastLoginAt)
  };
}

function company(value: unknown): PlatformCompanyAccounts | null {
  if (!isRecord(value)) return null;
  const companyId = text(value.companyId ?? value.id);
  if (!companyId) return null;
  return {
    companyId,
    companyName: text(value.companyName),
    companyType: text(value.companyType),
    supplierServiceTypes: strings(value.supplierServiceTypes),
    companyStatus: text(value.companyStatus),
    accountCount: Number(value.accountCount || 0),
    activeAccountCount: Number(value.activeAccountCount || 0),
    accounts: Array.isArray(value.accounts) ? value.accounts.map(account).filter(Boolean) as PlatformAccount[] : []
  };
}

async function request(endpoint: string, init: RequestInit = {}): Promise<unknown> {
  const token = getAuthSession()?.token;
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.headers || {})
    }
  });
  const body = await response.text();
  const payload = body ? JSON.parse(body) : null;
  if (!response.ok) {
    const message = isRecord(payload) ? text(payload.message ?? payload.error) : "";
    throw new ApiError(message || `HTTP_${response.status}`, response.status, payload);
  }
  return payload;
}

export async function getPlatformCompanyAccounts(query: PlatformAccountQuery = {}): Promise<PlatformCompanyAccountPage> {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) params.set(key, String(value));
  });
  const payload = await request(`/api/platform/companies/accounts${params.size ? `?${params.toString()}` : ""}`);
  if (!isRecord(payload)) return { items: [], page: 1, pageSize: 20, totalCompanies: 0, totalAccounts: 0 };
  return {
    items: Array.isArray(payload.items) ? payload.items.map(company).filter(Boolean) as PlatformCompanyAccounts[] : [],
    page: Number(payload.page || 1),
    pageSize: Number(payload.pageSize || 20),
    totalCompanies: Number(payload.totalCompanies || 0),
    totalAccounts: Number(payload.totalAccounts || 0)
  };
}

export async function updatePlatformAccountStatus(userId: string, status: "ACTIVE" | "DISABLED", reason: string): Promise<void> {
  await request(`/api/platform/accounts/${encodeURIComponent(userId)}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status, reason })
  });
}
