import { menuItems } from "@/data/mockWorkbench";
import { ApiError, getAuthSession } from "@/services/authService";
import type { UserRole, WorkbenchMenuItem } from "@/types/workbench";

export type PermissionRole = {
  code: string;
  name?: string;
  nameKey?: string;
  description?: string;
  userCount?: number;
  status?: "active" | "warning";
};

export type PermissionMenuNode = {
  menuId?: number;
  key: string;
  label?: string;
  labelKey?: string;
  route?: string;
  icon?: string;
  sortOrder?: number;
  requiredPermission?: string;
  enabled?: boolean;
  children?: PermissionMenuNode[];
};

export type PermissionPoint = {
  code: string;
  label?: string;
  labelKey?: string;
  groupKey?: string;
};

export type AdminUser = {
  id: string;
  account: string;
  name: string;
  roleCodes: string[];
  status?: string;
};

export type AdminRegistration = {
  id: string;
  userId?: string;
  companyId?: string;
  username?: string;
  companyType: string;
  companyName: string;
  contactName: string;
  phone: string;
  email: string;
  qualificationFiles: string[];
  qualificationFileItems: AdminQualificationFile[];
  submittedAt: string;
  status: "pending" | "approved" | "rejected";
  accountStatus?: string;
  companyStatus?: string;
  rejectReason?: string;
};

export type AdminQualificationFile = {
  fileId: string;
  fileType: string;
  fileName: string;
  status: string;
  url?: string;
};

export type AdminRegistrationQuery = {
  keyword?: string;
  status?: string;
  page?: number;
  pageSize?: number;
};

const roles: UserRole[] = ["admin", "purchaser", "supplier", "operator", "finance"];

const fallbackRoles: PermissionRole[] = [
  { code: "admin", nameKey: "role.admin", userCount: 2, status: "active" },
  { code: "purchaser", nameKey: "role.purchaser", userCount: 6, status: "active" },
  { code: "supplier", nameKey: "role.supplier", userCount: 18, status: "active" },
  { code: "operator", nameKey: "role.operator", userCount: 4, status: "active" },
  { code: "finance", nameKey: "role.finance", userCount: 3, status: "active" }
];

const fallbackUsers: AdminUser[] = [
  { id: "U-001", account: "admin", name: "平台管理员", roleCodes: ["admin"], status: "active" },
  { id: "U-002", account: "buyer01", name: "采购专员", roleCodes: ["purchaser"], status: "active" },
  { id: "U-003", account: "supplier01", name: "供货商账号", roleCodes: ["supplier"], status: "active" }
];

const fallbackPermissions: PermissionPoint[] = [
  { code: "dashboard:view", labelKey: "nav.dashboard", groupKey: "permission.group.page" },
  { code: "impa:view", labelKey: "nav.impa", groupKey: "permission.group.page" },
  { code: "suppliers:view", labelKey: "nav.suppliers", groupKey: "permission.group.page" },
  { code: "supplier-products:view", labelKey: "nav.supplierProducts", groupKey: "permission.group.page" },
  { code: "materialProcurement:view", labelKey: "nav.materialProcurement", groupKey: "permission.group.page" },
  { code: "requests:view", labelKey: "nav.requests", groupKey: "permission.group.page" },
  { code: "inquiries:view", labelKey: "nav.inquiries", groupKey: "permission.group.page" },
  { code: "quotes:view", labelKey: "nav.quotes", groupKey: "permission.group.page" },
  { code: "compare:view", labelKey: "nav.compare", groupKey: "permission.group.page" },
  { code: "orders:create", labelKey: "nav.orders", groupKey: "permission.group.action" },
  { code: "foodProcurement:view", labelKey: "nav.foodProcurement", groupKey: "permission.group.page" },
  { code: "food:view", labelKey: "nav.food", groupKey: "permission.group.page" },
  { code: "foodInquiries:view", labelKey: "nav.foodInquiries", groupKey: "permission.group.page" },
  { code: "foodQuotes:view", labelKey: "nav.foodQuotes", groupKey: "permission.group.page" },
  { code: "foodCompare:view", labelKey: "nav.foodCompare", groupKey: "permission.group.page" },
  { code: "foodOrders:view", labelKey: "nav.foodOrders", groupKey: "permission.group.page" },
  { code: "delivery:manage", labelKey: "nav.delivery", groupKey: "permission.group.action" },
  { code: "settlements:archive", labelKey: "nav.settlements", groupKey: "permission.group.action" },
  { code: "crewServices:view", labelKey: "nav.crewServices", groupKey: "permission.group.page" },
  { code: "admin:registrations", labelKey: "nav.registrations", groupKey: "permission.group.admin" },
  { code: "admin:roles", labelKey: "permission.rolePermission", groupKey: "permission.group.admin" },
  { code: "admin:users", labelKey: "permission.userRolePermission", groupKey: "permission.group.admin" }
];

const fallbackMenus: PermissionMenuNode[] = menuItems.map((item) => ({
  key: item.key,
  labelKey: item.labelKey,
  route: item.route,
  icon: item.icon,
  sortOrder: item.sortOrder,
  requiredPermission: fallbackPermissions.find((permission) => permission.labelKey === item.labelKey)?.code,
  children: item.children?.map((child) => ({
    key: child.key,
    labelKey: child.labelKey,
    route: child.route,
    icon: child.icon,
    sortOrder: child.sortOrder,
    requiredPermission: fallbackPermissions.find((permission) => permission.labelKey === child.labelKey)?.code
  }))
}));

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
  if ("rows" in payload) return payload.rows;
  return payload;
}

function readString(source: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "string" && value.trim()) return value.trim();
    if (typeof value === "number") return String(value);
  }
  return "";
}

function readNumber(source: Record<string, unknown>, keys: string[]): number | undefined {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "number" && Number.isFinite(value)) return value;
    if (typeof value === "string" && value.trim()) {
      const parsed = Number(value.trim());
      if (Number.isFinite(parsed)) return parsed;
    }
  }
  return undefined;
}

function sortByMenuOrder<T extends { sortOrder?: number }>(items: T[]): T[] {
  return [...items]
    .map((item, index) => ({ item, index }))
    .sort((left, right) => {
      const leftOrder = left.item.sortOrder ?? Number.MAX_SAFE_INTEGER;
      const rightOrder = right.item.sortOrder ?? Number.MAX_SAFE_INTEGER;
      return leftOrder - rightOrder || left.index - right.index;
    })
    .map(({ item }) => item);
}

function readArray(source: Record<string, unknown>, keys: string[]): string[] {
  for (const key of keys) {
    const value = source[key];
    if (!Array.isArray(value)) continue;
    return value
      .map((item) => {
        if (typeof item === "string" || typeof item === "number") return String(item);
        if (isRecord(item)) return readString(item, ["roleCode", "permissionCode", "code", "key", "name"]);
        return "";
      })
      .filter(Boolean);
  }
  return [];
}

function readDelimitedStringArray(source: Record<string, unknown>, keys: string[]): string[] {
  for (const key of keys) {
    const value = source[key];
    if (typeof value !== "string") continue;
    return value
      .split(/[,\s]+/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [];
}

async function requestJson(endpoint: string, init: RequestInit = { method: "GET" }): Promise<unknown> {
  const session = getAuthSession();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  headers.set("Content-Type", "application/json");
  if (session?.token) headers.set("Authorization", `Bearer ${session.token}`);
  const response = await fetch(endpoint, {
    ...init,
    headers
  });
  const payload = await readJson(response);
  if (!response.ok) {
    const message = isRecord(payload) ? readString(payload, ["message", "error", "code"]) : "";
    throw new ApiError(message || `HTTP_${response.status}`, response.status, payload);
  }
  return unwrapPayload(payload);
}

async function requestArray<T>(endpoint: string, fallback: T[], normalize: (item: unknown) => T | null): Promise<T[]> {
  try {
    const payload = await requestJson(endpoint);
    if (!Array.isArray(payload)) return fallback;
    const normalized = payload.map(normalize).filter(Boolean) as T[];
    return normalized.length ? normalized : fallback;
  } catch {
    return fallback;
  }
}

function normalizeMenuNode(item: unknown): PermissionMenuNode | null {
  if (!isRecord(item)) return null;
  const key = readString(item, ["key", "code", "menuCode", "id"]);
  const route = readString(item, ["route", "routePath", "path", "url"]);
  const labelKey = readString(item, ["labelKey", "i18nKey"]);
  const label = readString(item, ["label", "menuName", "name", "nameCn", "title"]);
  const sortOrder = readNumber(item, ["sortOrder", "orderNo", "displayOrder", "sequence", "seq"]);
  const requiredPermission = readString(item, ["requiredPermission", "permissionCode", "permissionKey"]);
  if (!key && !route) return null;
  const childrenValue = item.children;
  const children = Array.isArray(childrenValue) ? sortByMenuOrder(childrenValue.map(normalizeMenuNode).filter(Boolean) as PermissionMenuNode[]) : undefined;
  return {
    menuId: readNumber(item, ["menuId", "id"]),
    key: key || route,
    label,
    labelKey: labelKey || menuLabelKeysByCode[key] || menuLabelKeysByRoute[route],
    route,
    icon: readString(item, ["icon", "iconText"]),
    sortOrder,
    requiredPermission,
    enabled: item.enabled === false ? false : true,
    children
  };
}

function normalizeVisibleRole(role: string): UserRole | "" {
  const roleMap: Record<string, UserRole> = {
    PLATFORM_ADMIN: "admin",
    SHIP_AGENT: "purchaser",
    SUPPLIER: "supplier",
    BARGE_AGENT: "operator"
  };
  const normalized = roleMap[role] ?? role.toLowerCase();
  return roles.includes(normalized as UserRole) ? normalized as UserRole : "";
}

const menuLabelKeysByCode: Record<string, string> = {
  DASHBOARD: "nav.dashboard",
  BASIC_MANAGEMENT: "nav.basicManagement",
  MATERIAL_PROCUREMENT: "nav.materialProcurement",
  MATERIAL_PROCUREMENT_GROUP: "nav.materialProcurement",
  materialProcurement: "nav.materialProcurement",
  materialProcurementGroup: "nav.materialProcurement",
  PROCUREMENT_MATERIAL: "nav.materialProcurement",
  FOOD_PROCUREMENT: "nav.foodProcurement",
  FOOD_PROCUREMENT_GROUP: "nav.foodProcurement",
  FOOD_PROCUREMENT_MANAGEMENT: "nav.foodProcurement",
  foodProcurement: "nav.foodProcurement",
  foodProcurementGroup: "nav.foodProcurement",
  ADMIN_MENUS: "nav.menuManagement",
  MENU_MANAGEMENT: "nav.menuManagement",
  DATA_DICTIONARY: "nav.dataDictionary",
  DICTIONARIES: "nav.dataDictionary",
  PROCUREMENT_MATERIALS: "nav.requests",
  MATERIALS: "nav.requests",
  STANDARD_LIBRARY_IMPA: "nav.impa",
  SUPPLIERS: "nav.suppliers",
  SUPPLIER_PRODUCTS: "nav.supplierProducts",
  SHOP_PRODUCTS: "nav.supplierProducts",
  STORE_MANAGEMENT: "nav.supplierProducts",
  SHOP_MANAGEMENT: "nav.supplierProducts",
  PROCUREMENT_FOOD: "nav.food",
  FOOD: "nav.food",
  FOOD_INQUIRIES: "nav.foodInquiries",
  FOOD_QUOTES: "nav.foodQuotes",
  FOOD_COMPARISON: "nav.foodCompare",
  FOOD_COMPARE: "nav.foodCompare",
  FOOD_ORDERS: "nav.foodOrders",
  INQUIRIES: "nav.inquiries",
  INQUIRY: "nav.inquiries",
  INQUIRY_MANAGEMENT: "nav.inquiries",
  QUOTES: "nav.quotes",
  COMPARE: "nav.compare",
  COMPARISON: "nav.compare",
  ORDERS: "nav.orders",
  SUPPLIER_ORDERS: "nav.supplierOrders",
  SUPPLIER_ORDER_MANAGEMENT: "nav.supplierOrders",
  CREW_SERVICES: "nav.crewServices",
  CREW_SERVICE: "nav.crewServices",
  CREW_MANAGEMENT: "nav.crewServices",
  DELIVERY: "nav.delivery",
  BARGE_MANAGEMENT: "nav.delivery",
  SETTLEMENT: "nav.settlements",
  SETTLEMENTS: "nav.settlements",
  SETTLEMENT_MANAGEMENT: "nav.settlements",
  ADMIN_REGISTRATIONS: "nav.registrations",
  ADMIN_PERMISSION: "nav.permissions",
  ADMIN_PERMISSIONS: "nav.permissions"
};

const menuLabelKeysByRoute: Record<string, string> = {
  "/dashboard": "nav.dashboard",
  "/procurement/materials": "nav.requests",
  "/procurement/requests": "nav.requests",
  "/standard-library/impa": "nav.impa",
  "/suppliers": "nav.suppliers",
  "/supplier-products": "nav.supplierProducts",
  "/shop/products": "nav.supplierProducts",
  "/procurement/food": "nav.food",
  "/food/inquiries": "nav.foodInquiries",
  "/food/quotes": "nav.foodQuotes",
  "/food/comparison": "nav.foodCompare",
  "/food/orders": "nav.foodOrders",
  "/inquiries": "nav.inquiries",
  "/procurement/inquiries": "nav.inquiries",
  "/quotes": "nav.quotes",
  "/comparison": "nav.compare",
  "/compare": "nav.compare",
  "/procurement/requests/RFQ-240604/compare": "nav.compare",
  "/orders/new": "nav.orders",
  "/supplier/orders": "nav.supplierOrders",
  "/crew-services": "nav.crewServices",
  "/delivery-tasks": "nav.delivery",
  "/settlement": "nav.settlements",
  "/settlements": "nav.settlements",
  "/admin/registrations": "nav.registrations",
  "/admin/permissions": "nav.permissions",
  "/admin/menus": "nav.menuManagement",
  "/admin/dictionaries": "nav.dataDictionary"
};

function resolveWorkbenchMenuLabelKey(node: PermissionMenuNode) {
  if (node.labelKey) return node.labelKey;
  if (menuLabelKeysByCode[node.key]) return menuLabelKeysByCode[node.key];
  if (menuLabelKeysByRoute[node.route || ""]) return menuLabelKeysByRoute[node.route || ""];
  return `nav.${node.key}`;
}

function normalizeWorkbenchMenu(item: unknown): WorkbenchMenuItem | null {
  const node = normalizeMenuNode(item);
  if (!node) return null;
  const source = isRecord(item) ? item : {};
  const rawRoles = [
    ...readArray(source, ["roles", "roleCodes", "allowedRoles"]),
    ...readDelimitedStringArray(source, ["visibleRoles"])
  ];
  const itemRoles = rawRoles.map(normalizeVisibleRole).filter((role): role is UserRole => Boolean(role));
  const children = (node.children ?? [])
    .map((child) => normalizeWorkbenchMenu(child))
    .filter(Boolean) as WorkbenchMenuItem[];
  if (!node.route && !children.length) return null;
  return {
    key: node.key,
    label: undefined,
    labelKey: resolveWorkbenchMenuLabelKey(node),
    route: node.route || children[0]?.route,
    icon: node.icon || node.key.slice(0, 2).toUpperCase(),
    sortOrder: node.sortOrder,
    roles: itemRoles.length ? itemRoles : roles,
    children: children.length ? sortByMenuOrder(children) : undefined
  };
}

const basicManagementChildKeys = new Set([
  "STANDARD_LIBRARY_IMPA",
  "SUPPLIERS",
  "ADMIN_REGISTRATIONS",
  "ADMIN_PERMISSION",
  "ADMIN_PERMISSIONS",
  "ADMIN_MENUS",
  "MENU_MANAGEMENT",
  "DATA_DICTIONARY",
  "impa",
  "suppliers",
  "registrations",
  "permissions",
  "menuManagement",
  "dataDictionary"
]);

const materialProcurementChildKeys = new Set([
  "PROCUREMENT_MATERIALS",
  "MATERIALS",
  "INQUIRIES",
  "INQUIRY",
  "INQUIRY_MANAGEMENT",
  "QUOTES",
  "COMPARE",
  "COMPARISON",
  "ORDERS",
  "SUPPLIER_ORDERS",
  "SUPPLIER_ORDER_MANAGEMENT",
  "requests",
  "inquiries",
  "quotes",
  "compare",
  "orders",
  "supplierOrders"
]);

const foodProcurementChildKeys = new Set([
  "PROCUREMENT_FOOD",
  "FOOD",
  "FOOD_INQUIRIES",
  "FOOD_QUOTES",
  "FOOD_COMPARISON",
  "FOOD_COMPARE",
  "FOOD_ORDERS",
  "food",
  "foodInquiries",
  "foodQuotes",
  "foodCompare",
  "foodOrders"
]);

const hiddenWorkbenchMenuKeys = new Set<string>();
const hiddenWorkbenchMenuRoutes = new Set<string>();

function filterHiddenWorkbenchMenus(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  return items
    .filter((item) => !hiddenWorkbenchMenuKeys.has(item.key) && !hiddenWorkbenchMenuRoutes.has(item.route || ""))
    .map((item) => ({ ...item, children: item.children ? filterHiddenWorkbenchMenus(item.children) : undefined }))
    .filter((item) => item.route || item.children?.length);
}

function mergeRolesFromChildren(children: WorkbenchMenuItem[]): UserRole[] {
  const roleSet = new Set<UserRole>();
  children.forEach((child) => child.roles.forEach((role) => roleSet.add(role)));
  return roles.filter((role) => roleSet.has(role));
}

function groupWorkbenchChildren(
  items: WorkbenchMenuItem[],
  parentKeys: string[],
  childKeys: Set<string>,
  parent: Pick<WorkbenchMenuItem, "key" | "labelKey" | "icon" | "sortOrder">
) {
  const hasTree = items.some((item) => parentKeys.includes(item.key));
  if (hasTree) return items;
  const children = sortByMenuOrder(items.filter((item) => childKeys.has(item.key)));
  if (!children.length) return items;
  const rest = items.filter((item) => !childKeys.has(item.key));
  return [
    ...rest,
    {
      ...parent,
      roles: mergeRolesFromChildren(children),
      route: children[0]?.route,
      children: children.map((child, index) => ({ ...child, sortOrder: index * 10 }))
    }
  ];
}

function groupBasicManagementMenus(items: WorkbenchMenuItem[]) {
  items = filterHiddenWorkbenchMenus(items);
  items = groupWorkbenchChildren(items, ["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup"], materialProcurementChildKeys, {
    key: "materialProcurement",
    labelKey: "nav.materialProcurement",
    icon: "MP",
    sortOrder: 30
  });
  items = groupWorkbenchChildren(items, ["FOOD_PROCUREMENT", "FOOD_PROCUREMENT_GROUP", "FOOD_PROCUREMENT_MANAGEMENT", "foodProcurement", "foodProcurementGroup"], foodProcurementChildKeys, {
    key: "foodProcurement",
    labelKey: "nav.foodProcurement",
    icon: "FP",
    sortOrder: 40
  });
  const hasBasicTree = items.some((item) => item.key === "BASIC_MANAGEMENT" || item.key === "basicManagement");
  if (hasBasicTree) {
    return sortByMenuOrder(items.filter((item) => !item.children || item.children.length > 0));
  }
  const basicChildren = sortByMenuOrder(items.filter((item) => basicManagementChildKeys.has(item.key)));
  if (!basicChildren.length) return sortByMenuOrder(items);
  const rest = items.filter((item) => !basicManagementChildKeys.has(item.key));
  return sortByMenuOrder([
    ...rest,
    {
      key: "basicManagement",
      labelKey: "nav.basicManagement",
      icon: "BM",
      sortOrder: 110,
      roles: mergeRolesFromChildren(basicChildren),
      route: basicChildren[0]?.route,
      children: basicChildren.map((child, index) => ({ ...child, sortOrder: index * 10 }))
    }
  ]);
}

function groupBasicManagementPermissionMenus(items: PermissionMenuNode[]) {
  items = groupPermissionChildren(items, ["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup"], materialProcurementChildKeys, {
    key: "materialProcurement",
    labelKey: "nav.materialProcurement",
    icon: "MP",
    sortOrder: 30
  });
  items = groupPermissionChildren(items, ["FOOD_PROCUREMENT", "FOOD_PROCUREMENT_GROUP", "FOOD_PROCUREMENT_MANAGEMENT", "foodProcurement", "foodProcurementGroup"], foodProcurementChildKeys, {
    key: "foodProcurement",
    labelKey: "nav.foodProcurement",
    icon: "FP",
    sortOrder: 40
  });
  const hasBasicTree = items.some((item) => item.key === "BASIC_MANAGEMENT" || item.key === "basicManagement");
  if (hasBasicTree) {
    return sortByMenuOrder(items.filter((item) => !item.children || item.children.length > 0));
  }
  const basicChildren = sortByMenuOrder(items.filter((item) => basicManagementChildKeys.has(item.key)));
  if (!basicChildren.length) return sortByMenuOrder(items);
  const rest = items.filter((item) => !basicManagementChildKeys.has(item.key));
  return sortByMenuOrder([
    ...rest,
    {
      key: "basicManagement",
      labelKey: "nav.basicManagement",
      icon: "BM",
      sortOrder: 110,
      enabled: true,
      children: basicChildren.map((child, index) => ({ ...child, sortOrder: index * 10 }))
    }
  ]);
}

function groupPermissionChildren(
  items: PermissionMenuNode[],
  parentKeys: string[],
  childKeys: Set<string>,
  parent: Pick<PermissionMenuNode, "key" | "labelKey" | "icon" | "sortOrder">
) {
  const hasTree = items.some((item) => parentKeys.includes(item.key));
  if (hasTree) return items;
  const children = sortByMenuOrder(items.filter((item) => childKeys.has(item.key)));
  if (!children.length) return items;
  const rest = items.filter((item) => !childKeys.has(item.key));
  return [
    ...rest,
    {
      ...parent,
      enabled: true,
      children: children.map((child, index) => ({ ...child, sortOrder: index * 10 }))
    }
  ];
}

function normalizeRole(item: unknown): PermissionRole | null {
  if (!isRecord(item)) return null;
  const code = readString(item, ["code", "roleCode", "key", "id"]);
  if (!code) return null;
  const userCountRaw = item.userCount ?? item.usersCount;
  return {
    code,
    name: readString(item, ["name", "nameCn", "label"]),
    nameKey: readString(item, ["nameKey", "labelKey", "i18nKey"]),
    description: readString(item, ["description", "remark"]),
    userCount: typeof userCountRaw === "number" ? userCountRaw : undefined,
    status: readString(item, ["status"]) === "warning" ? "warning" : "active"
  };
}

function normalizePermission(item: unknown): PermissionPoint | null {
  if (!isRecord(item)) return null;
  const code = readString(item, ["code", "permissionCode", "key", "id"]);
  if (!code) return null;
  return {
    code,
    label: readString(item, ["label", "name", "nameCn", "title"]),
    labelKey: readString(item, ["labelKey", "i18nKey"]),
    groupKey: readString(item, ["groupKey", "moduleKey"]) || "permission.group.action"
  };
}

function normalizeUser(item: unknown): AdminUser | null {
  if (!isRecord(item)) return null;
  const id = readString(item, ["id", "userId", "code"]);
  if (!id) return null;
  return {
    id,
    account: readString(item, ["account", "username", "loginName"]) || id,
    name: readString(item, ["name", "displayName", "realName"]) || id,
    roleCodes: readArray(item, ["roleCodes", "roles"]),
    status: readString(item, ["status"]) || "active"
  };
}

function normalizeRegistration(item: unknown): AdminRegistration | null {
  if (!isRecord(item)) return null;
  const id = readString(item, ["userId", "id", "registrationId", "code"]);
  if (!id) return null;
  const filesValue = item.qualifications ?? item.qualificationFiles ?? item.files ?? item.attachments;
  const qualificationFileItems = Array.isArray(filesValue)
    ? filesValue
        .map((file): AdminQualificationFile | null => {
          if (typeof file === "string") {
            return { fileId: "", fileType: "", fileName: file, status: "" };
          }
          if (!isRecord(file)) return null;
          const fileId = readString(file, ["fileId", "id", "fileCode"]);
          const fileName = readString(file, ["fileName", "name", "originalName", "url", "fileUrl"]);
          if (!fileId && !fileName) return null;
          return {
            fileId,
            fileType: readString(file, ["fileType", "type"]),
            fileName: fileName || fileId,
            status: readString(file, ["status"]),
            url: readString(file, ["url", "fileUrl", "downloadUrl"])
          };
        })
        .filter(Boolean) as AdminQualificationFile[]
    : [];
  const status = readString(item, ["companyStatus", "accountStatus", "status", "reviewStatus"]);
  const normalizedStatus = status === "ACTIVE" || status === "APPROVED" || status === "approved"
    ? "approved"
    : status === "REJECTED" || status === "rejected"
      ? "rejected"
      : "pending";
  return {
    id,
    userId: readString(item, ["userId"]),
    companyId: readString(item, ["companyId"]),
    username: readString(item, ["username", "account"]),
    companyType: readString(item, ["companyType", "enterpriseType", "type"]),
    companyName: readString(item, ["companyName", "enterpriseName", "name"]),
    contactName: readString(item, ["contactName", "contact", "contactPerson"]),
    phone: readString(item, ["contactPhone", "phone", "mobile"]),
    email: readString(item, ["contactEmail", "email"]),
    qualificationFiles: qualificationFileItems.map((file) => file.fileName).filter(Boolean),
    qualificationFileItems,
    submittedAt: readString(item, ["submittedAt", "createdAt", "applyTime"]),
    status: normalizedStatus,
    accountStatus: readString(item, ["accountStatus"]),
    companyStatus: readString(item, ["companyStatus"]),
    rejectReason: readString(item, ["rejectReason", "reason"])
  };
}

export async function getAuthMenus(): Promise<WorkbenchMenuItem[]> {
  try {
    const payload = await requestJson("/api/auth/menus");
    if (!Array.isArray(payload)) return groupBasicManagementMenus(menuItems);
    return groupBasicManagementMenus(payload.map(normalizeWorkbenchMenu).filter(Boolean) as WorkbenchMenuItem[]);
  } catch (error) {
    if (error instanceof ApiError && (error.status === 401 || error.status === 403)) return [];
    return groupBasicManagementMenus(menuItems);
  }
}

export async function getAuthPermissions(): Promise<string[]> {
  try {
    const payload = await requestJson("/api/auth/permissions");
    if (Array.isArray(payload)) {
      return payload
        .map((item) => {
          if (typeof item === "string") return item;
          if (isRecord(item)) return readString(item, ["code", "permissionCode", "key"]);
          return "";
        })
        .filter(Boolean);
    }
    return isRecord(payload) ? readArray(payload, ["permissions", "permissionCodes", "codes"]) : [];
  } catch {
    return fallbackPermissions.map((item) => item.code);
  }
}

export async function getAdminRoles(): Promise<PermissionRole[]> {
  return requestArray("/api/admin/roles", fallbackRoles, normalizeRole);
}

export async function getAdminUsers(): Promise<AdminUser[]> {
  return requestArray("/api/admin/users", fallbackUsers, normalizeUser);
}

export async function getAdminMenus(): Promise<PermissionMenuNode[]> {
  const menus = await requestArray("/api/admin/menus", fallbackMenus, normalizeMenuNode);
  return groupBasicManagementPermissionMenus(menus);
}

export async function getAdminMenuPermissions(): Promise<PermissionMenuNode[]> {
  const menus = await requestArray("/api/admin/menu-permissions", fallbackMenus, normalizeMenuNode);
  return groupBasicManagementPermissionMenus(menus);
}

export async function getManageableAdminMenus(): Promise<PermissionMenuNode[]> {
  const menus = await requestArray("/api/admin/permissions/menus", fallbackMenus, normalizeMenuNode);
  return groupBasicManagementPermissionMenus(menus);
}

export async function getAdminPermissions(): Promise<PermissionPoint[]> {
  return requestArray("/api/admin/permissions", fallbackPermissions, normalizePermission);
}

export async function getAdminRegistrations(query: AdminRegistrationQuery = {}): Promise<AdminRegistration[]> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("keyword", query.keyword.trim());
  if (query.status) params.set("status", query.status);
  if (query.page) params.set("page", String(query.page));
  if (query.pageSize) params.set("pageSize", String(query.pageSize));
  const endpoint = params.size ? `/api/admin/registrations?${params.toString()}` : "/api/admin/registrations";
  const payload = await requestJson(endpoint);
  if (!Array.isArray(payload)) return [];
  return payload.map(normalizeRegistration).filter(Boolean) as AdminRegistration[];
}

export async function getRolePermissions(roleCode: string): Promise<string[]> {
  try {
    const payload = await requestJson(`/api/admin/roles/${encodeURIComponent(roleCode)}/permissions`);
    if (Array.isArray(payload)) {
      const codes = payload
        .map((item) => {
          if (typeof item === "string") return item;
          if (isRecord(item)) return readString(item, ["code", "permissionCode", "key"]);
          return "";
        })
        .filter(Boolean);
      return codes.length ? codes : fallbackPermissions.map((item) => item.code);
    }
    if (isRecord(payload)) {
      const codes = readArray(payload, ["permissions", "permissionCodes", "codes"]);
      return codes.length ? codes : fallbackPermissions.map((item) => item.code);
    }
    return [];
  } catch {
    return [];
  }
}

export async function getRoleMenuPermissions(roleCode: string): Promise<string[]> {
  try {
    const payload = await requestJson(`/api/admin/roles/${encodeURIComponent(roleCode)}/menu-permissions`);
    if (isRecord(payload)) return readArray(payload, ["menuPermissionKeys", "menuCodes", "codes"]);
    if (Array.isArray(payload)) return payload.map((item) => String(item)).filter(Boolean);
    return [];
  } catch {
    return [];
  }
}

export async function saveRolePermissions(roleCode: string, permissionCodes: string[]): Promise<void> {
  await requestJson(`/api/admin/roles/${encodeURIComponent(roleCode)}/permissions`, {
    method: "PUT",
    body: JSON.stringify({ permissionCodes })
  });
}

export async function saveRoleMenuPermissions(roleCode: string, menuPermissionKeys: string[]): Promise<void> {
  await requestJson(`/api/admin/roles/${encodeURIComponent(roleCode)}/menu-permissions`, {
    method: "PUT",
    body: JSON.stringify({ menuPermissionKeys })
  });
}

export async function saveAdminMenuSortOrders(items: Array<{ menuCode: string; sortOrder: number }>): Promise<void> {
  await requestJson("/api/admin/permissions/menus/sort-order", {
    method: "PUT",
    body: JSON.stringify({ items })
  });
}

export async function assignUserRoles(userId: string, roleCodes: string[]): Promise<void> {
  try {
    await requestJson(`/api/admin/users/${encodeURIComponent(userId)}/roles`, {
      method: "PATCH",
      body: JSON.stringify({ roleCodes })
    });
  } catch {
    // Backend is optional for the interactive shell stage.
  }
}

export async function approveRegistration(registrationId: string): Promise<void> {
  await requestJson(`/api/admin/registrations/${encodeURIComponent(registrationId)}/approve`, {
    method: "POST"
  });
}

export async function rejectRegistration(registrationId: string, reason: string): Promise<void> {
  await requestJson(`/api/admin/registrations/${encodeURIComponent(registrationId)}/reject`, {
    method: "POST",
    body: JSON.stringify({ reason })
  });
}
