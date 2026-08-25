import type { AuthSession } from "@/types/auth";
import type { UserRole } from "@/types/workbench";

const companyTypeRoles: Record<string, UserRole> = {
  SHIP_AGENT: "purchaser",
  SUPPLIER: "supplier",
  BARGE_AGENT: "operator"
};

const authRoleAliases: Record<string, UserRole> = {
  PLATFORM_ADMIN: "admin",
  ADMIN: "admin",
  SHIP_AGENT: "purchaser",
  PURCHASER: "purchaser",
  SUPPLIER: "supplier",
  BARGE_AGENT: "operator",
  OPERATOR: "operator",
  FINANCE: "finance"
};

export function resolveWorkbenchAuthRole(session: AuthSession | null): UserRole | null {
  if (!session) return null;

  const roleCodes = session.roles.map((role) => role.trim().toUpperCase()).filter(Boolean);
  if (roleCodes.includes("PLATFORM_ADMIN") || roleCodes.includes("ADMIN")) return "admin";

  const companyType = String(session.company?.companyType || session.company?.type || "").trim().toUpperCase();
  if (companyTypeRoles[companyType]) return companyTypeRoles[companyType];

  for (const roleCode of roleCodes) {
    if (authRoleAliases[roleCode]) return authRoleAliases[roleCode];
  }

  return null;
}
