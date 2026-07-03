import { ApiError, getAuthSession } from "@/services/authService";

export const companyEndpoints = {
  profile: "/api/company/profile",
  qualifications: "/api/company/qualifications",
  qualificationDetail: (qualificationId: string | number) => `/api/company/qualifications/${encodeURIComponent(String(qualificationId))}`,
  contacts: "/api/company/contacts",
  contactDetail: (contactId: string | number) => `/api/company/contacts/${encodeURIComponent(String(contactId))}`
} as const;

export type CompanyPayload = Record<string, unknown>;
export type CompanyQueryParams = Record<string, string | number | boolean | undefined | null>;

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

function readErrorMessage(payload: unknown): string {
  if (!isRecord(payload)) return "";
  const value = payload.message ?? payload.error ?? payload.detail ?? payload.msg ?? payload.reason ?? payload.code;
  return typeof value === "string" && value.trim() ? value.trim() : "";
}

async function requestCompanyJson(endpoint: string, init: RequestInit = {}): Promise<unknown> {
  const session = getAuthSession();
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {})
    }
  });
  const payload = await readJson(response);

  if (!response.ok) {
    throw new ApiError(readErrorMessage(payload) || `HTTP_${response.status}`, response.status, payload);
  }

  return payload;
}

function buildCompanyQuery(params: CompanyQueryParams = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    search.set(key, String(value));
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

export function getCompanyProfile() {
  return requestCompanyJson(companyEndpoints.profile, { method: "GET" });
}

export function updateCompanyProfile(payload: CompanyPayload) {
  return requestCompanyJson(companyEndpoints.profile, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function listCompanyQualifications(params: CompanyQueryParams = {}) {
  return requestCompanyJson(`${companyEndpoints.qualifications}${buildCompanyQuery(params)}`, { method: "GET" });
}

export function createCompanyQualification(payload: CompanyPayload) {
  return requestCompanyJson(companyEndpoints.qualifications, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateCompanyQualification(qualificationId: string | number, payload: CompanyPayload) {
  return requestCompanyJson(companyEndpoints.qualificationDetail(qualificationId), {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function deleteCompanyQualification(qualificationId: string | number) {
  return requestCompanyJson(companyEndpoints.qualificationDetail(qualificationId), { method: "DELETE" });
}

export function listCompanyContacts(params: CompanyQueryParams = {}) {
  return requestCompanyJson(`${companyEndpoints.contacts}${buildCompanyQuery(params)}`, { method: "GET" });
}

export function createCompanyContact(payload: CompanyPayload) {
  return requestCompanyJson(companyEndpoints.contacts, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateCompanyContact(contactId: string | number, payload: CompanyPayload) {
  return requestCompanyJson(companyEndpoints.contactDetail(contactId), {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function deleteCompanyContact(contactId: string | number) {
  return requestCompanyJson(companyEndpoints.contactDetail(contactId), { method: "DELETE" });
}
