import { ApiError, getAuthSession } from "@/services/authService";

export type CompanyMemberStatus = "PENDING" | "INVITED" | "ACTIVE" | "DISABLED" | "LEFT" | "PASSWORD_RESET_REQUIRED" | "UNKNOWN";

export type CompanyRole = {
  code: string;
  name: string;
  description?: string;
  menuPermissionKeys: string[];
};

export type CompanyMenuOption = { code: string; name: string };

export type CompanyMember = {
  id: string;
  username: string;
  name: string;
  phone: string;
  email: string;
  companyName: string;
  companyType: string;
  roleCodes: string[];
  status: CompanyMemberStatus;
  source: string;
  lastLoginAt: string;
  createdAt: string;
  isOwner: boolean;
};

export type CompanyMemberQuery = {
  keyword?: string;
  status?: string;
  roleCode?: string;
};

export type CreateCompanyMemberRequest = {
  username: string;
  phone: string;
  password: string;
  roleCodes: string[];
  name?: string;
  email?: string;
};

const MEMBERS_ENDPOINT = "/api/company/members";
const ROLES_ENDPOINT = "/api/company/roles";

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

function readString(source: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "string" && value.trim()) return value.trim();
    if (typeof value === "number") return String(value);
  }
  return "";
}

function readBoolean(source: Record<string, unknown>, keys: string[]): boolean {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "boolean") return value;
  }
  return false;
}

function readArray(source: Record<string, unknown>, keys: string[]): string[] {
  for (const key of keys) {
    const value = source[key];
    if (!Array.isArray(value)) continue;

    return value
      .map((item) => {
        if (typeof item === "string") return item;
        if (isRecord(item)) return readString(item, ["code", "roleCode", "key", "name"]);
        return "";
      })
      .filter(Boolean);
  }
  return [];
}

function unwrapPayload(payload: unknown): unknown {
  if (!isRecord(payload)) return payload;
  if ("data" in payload) return payload.data;
  if ("result" in payload) return payload.result;
  return payload;
}

function unwrapArray(payload: unknown): unknown[] {
  const unwrapped = unwrapPayload(payload);
  if (Array.isArray(unwrapped)) return unwrapped;
  if (!isRecord(unwrapped)) return [];

  for (const key of ["rows", "list", "items", "members", "roles", "records"]) {
    const value = unwrapped[key];
    if (Array.isArray(value)) return value;
  }
  return [];
}

function readErrorMessage(payload: unknown): string {
  if (!isRecord(payload)) return "";
  return readString(payload, ["message", "error", "detail", "msg", "reason", "code"]);
}

async function requestJson(endpoint: string, init: RequestInit = {}): Promise<unknown> {
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

function normalizeStatus(value: string): CompanyMemberStatus {
  const normalized = value.toUpperCase();
  if (
    normalized === "PENDING" ||
    normalized === "INVITED" ||
    normalized === "ACTIVE" ||
    normalized === "DISABLED" ||
    normalized === "LEFT" ||
    normalized === "PASSWORD_RESET_REQUIRED"
  ) {
    return normalized;
  }
  return "UNKNOWN";
}

function normalizeRole(value: unknown): CompanyRole | null {
  if (!isRecord(value)) return null;
  const code = readString(value, ["code", "roleCode", "key"]);
  if (!code) return null;

  return {
    code,
    name: readString(value, ["name", "label", "nameCn", "roleName"]) || code,
    description: readString(value, ["description", "remark"]) || undefined,
    menuPermissionKeys: readArray(value, ["menuPermissionKeys", "menuCodes"])
  };
}

function normalizeMember(value: unknown): CompanyMember | null {
  if (!isRecord(value)) return null;
  const id = readString(value, ["id", "userId", "memberId"]);
  const username = readString(value, ["username", "account", "loginName", "mobile"]);
  if (!id || !username) return null;

  return {
    id,
    username,
    name: readString(value, ["name", "realName", "displayName", "contactName"]) || username,
    phone: readString(value, ["phone", "mobile", "contactPhone"]),
    email: readString(value, ["email", "contactEmail"]),
    companyName: readString(value, ["companyName"]),
    companyType: readString(value, ["companyType", "enterpriseType"]),
    roleCodes: readArray(value, ["roleCodes", "roles", "authorities"]),
    status: normalizeStatus(readString(value, ["status", "memberStatus", "userStatus"])),
    source: readString(value, ["source", "createdSource", "createSource"]),
    lastLoginAt: readString(value, ["lastLoginAt", "lastLoginTime"]),
    createdAt: readString(value, ["createdAt", "createTime"]),
    isOwner: readBoolean(value, ["owner", "isOwner", "companyOwner"])
  };
}

export async function getCompanyRoles(): Promise<CompanyRole[]> {
  const payload = await requestJson(ROLES_ENDPOINT, { method: "GET" });
  return unwrapArray(payload).map(normalizeRole).filter(Boolean) as CompanyRole[];
}

export async function getCompanyMembers(query: CompanyMemberQuery = {}): Promise<CompanyMember[]> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("keyword", query.keyword.trim());
  if (query.status) params.set("status", query.status);
  if (query.roleCode) params.set("roleCode", query.roleCode);

  const endpoint = params.toString() ? `${MEMBERS_ENDPOINT}?${params.toString()}` : MEMBERS_ENDPOINT;
  const payload = await requestJson(endpoint, { method: "GET" });
  return unwrapArray(payload).map(normalizeMember).filter(Boolean) as CompanyMember[];
}

export async function createCompanyMember(request: CreateCompanyMemberRequest): Promise<CompanyMember | null> {
  const payload = await requestJson(MEMBERS_ENDPOINT, {
    method: "POST",
    body: JSON.stringify(request)
  });
  return normalizeMember(unwrapPayload(payload));
}

export async function updateCompanyMember(userId: string, request: { name: string; phone: string; email: string; roleCodes: string[] }): Promise<CompanyMember | null> {
  const payload = await requestJson(`${MEMBERS_ENDPOINT}/${encodeURIComponent(userId)}`, {
    method: "PUT",
    body: JSON.stringify(request)
  });
  return normalizeMember(unwrapPayload(payload));
}

export async function updateCompanyMemberStatus(userId: string, status: "ACTIVE" | "DISABLED"): Promise<void> {
  await requestJson(`${MEMBERS_ENDPOINT}/${encodeURIComponent(userId)}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}

export async function resetCompanyMemberPassword(userId: string, password: string): Promise<void> {
  await requestJson(`${MEMBERS_ENDPOINT}/${encodeURIComponent(userId)}/reset-password`, {
    method: "POST",
    body: JSON.stringify({ newPassword: password })
  });
}

export async function updateCompanyMemberRoles(userId: string, roleCodes: string[]): Promise<void> {
  await requestJson(`${MEMBERS_ENDPOINT}/${encodeURIComponent(userId)}/roles`, {
    method: "PUT",
    body: JSON.stringify({ roles: roleCodes })
  });
}

export async function getCompanyMenuOptions(): Promise<CompanyMenuOption[]> {
  const payload = await requestJson("/api/company/menus/permissions", { method: "GET" });
  const rows = unwrapArray(payload);
  const result: CompanyMenuOption[] = [];
  const visit = (items: unknown[]) => {
    items.forEach((item) => {
      if (!isRecord(item)) return;
      const code = readString(item, ["menuCode", "code", "key"]);
      if (code) result.push({ code, name: readString(item, ["menuName", "name", "label"]) || code });
      if (Array.isArray(item.children)) visit(item.children);
    });
  };
  visit(rows);
  return result;
}

export async function createCompanyRole(request: { roleCode?: string; roleName: string; menuPermissionKeys: string[] }): Promise<void> {
  await requestJson(ROLES_ENDPOINT, {
    method: "POST",
    body: JSON.stringify({ ...request, roleType: "CUSTOM" })
  });
}

export async function updateCompanyRole(roleCode: string, request: { roleName: string; menuPermissionKeys: string[] }): Promise<void> {
  await requestJson(`${ROLES_ENDPOINT}/${encodeURIComponent(roleCode)}`, {
    method: "PUT",
    body: JSON.stringify({ roleName: request.roleName, roleType: "CUSTOM", menuPermissionKeys: request.menuPermissionKeys })
  });
}

export async function disableCompanyRole(roleCode: string): Promise<void> {
  await requestJson(`${ROLES_ENDPOINT}/${encodeURIComponent(roleCode)}`, { method: "DELETE" });
}
