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
  supplierServiceTypes: string[];
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
  { code: "supply-chain-finance:view", labelKey: "nav.supplyChainFinance", groupKey: "permission.group.page" },
  { code: "crewServices:view", labelKey: "nav.crewServices", groupKey: "permission.group.page" },
  { code: "admin:registrations", labelKey: "nav.registrations", groupKey: "permission.group.admin" },
  { code: "admin:roles", labelKey: "permission.rolePermission", groupKey: "permission.group.admin" },
  { code: "admin:users", labelKey: "permission.userRolePermission", groupKey: "permission.group.admin" }
];

const toPermissionMenuNode = (item: WorkbenchMenuItem): PermissionMenuNode => ({
  key: item.key,
  labelKey: item.labelKey,
  route: item.route,
  icon: item.icon,
  sortOrder: item.sortOrder,
  requiredPermission: fallbackPermissions.find((permission) => permission.labelKey === item.labelKey)?.code,
  children: item.children?.map(toPermissionMenuNode)
});

const fallbackMenus: PermissionMenuNode[] = menuItems.map(toPermissionMenuNode);

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
  DASHBOARD_GOVERNMENT: "nav.dashboardGovernment",
  dashboardGovernment: "nav.dashboardGovernment",
  BASIC_MANAGEMENT: "nav.basicManagement",
  PROCUREMENT_SERVICES: "nav.procurementServices",
  procurementServices: "nav.procurementServices",
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
  FOOD_SUPPLIER_ORDERS: "nav.foodSupplierOrders",
  FOOD_SETTLEMENTS: "nav.foodSettlements",
  FOOD_SUPPLIER_SETTLEMENTS: "nav.foodSupplierSettlements",
  FOOD_EVALUATIONS: "nav.foodEvaluations",
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
  DELIVERY_TASKS: "nav.delivery",
  TRAFFIC_SERVICE: "nav.delivery",
  BARGE_SERVICES: "nav.bargeServices",
  bargeServices: "nav.bargeServices",
  TRAFFIC_BOAT: "nav.trafficBoat",
  TRAFFIC_ROUTE_PLANNING: "nav.trafficRoutes",
  BARGE_MANAGEMENT: "nav.delivery",
  SETTLEMENT: "nav.settlements",
  SETTLEMENTS: "nav.settlements",
  SETTLEMENT_MANAGEMENT: "nav.settlements",
  SUPPLY_CHAIN_FINANCE: "nav.supplyChainFinance",
  supplyChainFinance: "nav.supplyChainFinance",
  REGULATORY_SERVICES: "nav.regulatoryServices",
  regulatoryServices: "nav.regulatoryServices",
  QUALITY_SUPERVISION: "nav.qualitySupervision",
  qualitySupervision: "nav.qualitySupervision",
  TAX_SERVICES: "nav.taxServices",
  taxServices: "nav.taxServices",
  FINANCIAL_SERVICES: "nav.financialServices",
  financialServices: "nav.financialServices",
  WEATHER_SERVICES: "nav.weatherServices",
  weatherServices: "nav.weatherServices",
  VESSEL_DYNAMICS_SERVICES: "nav.vesselDynamicsServices",
  vesselDynamicsServices: "nav.vesselDynamicsServices",
  PLATFORM_OPERATIONS: "nav.platformOperations",
  platformOperations: "nav.platformOperations",
  SUPPLIER_DATA_ANALYSIS: "nav.dataAnalysis",
  DATA_ANALYSIS: "nav.dataAnalysis",
  supplierDataAnalysis: "nav.dataAnalysis",
  CUSTOMS_SERVICES: "nav.customsServices",
  CUSTOMS_SERVICES_GROUP: "nav.customsServices",
  customsServices: "nav.customsServices",
  CUSTOMS_DECLARATION: "nav.customsDeclarations",
  CUSTOMS_DECLARATIONS: "nav.customsDeclarations",
  customsDeclarations: "nav.customsDeclarations",
  PORT_SHIPPING_SERVICES: "nav.portShippingServices",
  portShippingServices: "nav.portShippingServices",
  BORDER_INSPECTION_SERVICES: "nav.borderInspectionServices",
  borderInspectionServices: "nav.borderInspectionServices",
  MARITIME_SERVICES: "nav.maritimeServices",
  maritimeServices: "nav.maritimeServices",
  COMPANY_MEMBERS: "nav.companyMembers",
  companyMembers: "nav.companyMembers",
  ADMIN_REGISTRATIONS: "nav.registrations",
  ADMIN_PERMISSION: "nav.permissions",
  ADMIN_PERMISSIONS: "nav.permissions"
};

const menuLabelKeysByRoute: Record<string, string> = {
  "/dashboard": "nav.dashboard",
  "/dashboard-government": "nav.dashboardGovernment",
  "/company/members": "nav.companyMembers",
  "/procurement/materials": "nav.requests",
  "/procurement/requests": "nav.requests",
  "/standard-library/impa": "nav.impa",
  "/suppliers": "nav.suppliers",
  "/supplier-products": "nav.supplierProducts",
  "/shop/products": "nav.supplierProducts",
  "/customs-services/declarations": "nav.customsDeclarations",
  "/procurement/food": "nav.food",
  "/food/inquiries": "nav.foodInquiries",
  "/food/quotes": "nav.foodQuotes",
  "/food/comparison": "nav.foodCompare",
  "/food/orders": "nav.foodOrders",
  "/supplier/food/orders": "nav.foodSupplierOrders",
  "/food/settlements": "nav.foodSettlements",
  "/supplier/food/settlements": "nav.foodSupplierSettlements",
  "/food/evaluations": "nav.foodEvaluations",
  "/inquiries": "nav.inquiries",
  "/procurement/inquiries": "nav.inquiries",
  "/quotes": "nav.quotes",
  "/comparison": "nav.compare",
  "/compare": "nav.compare",
  "/procurement/requests/RFQ-240604/compare": "nav.compare",
  "/orders/new": "nav.orders",
  "/supplier/orders": "nav.supplierOrders",
  "/crew-services": "nav.crewServices",
  "/customs-services": "nav.customsServices",
  "/port-shipping-services": "nav.portShippingServices",
  "/border-inspection-services": "nav.borderInspectionServices",
  "/maritime-services": "nav.maritimeServices",
  "/tax-services": "nav.taxServices",
  "/financial-services": "nav.financialServices",
  "/weather-services": "nav.weatherServices",
  "/vessel-dynamics-services": "nav.vesselDynamicsServices",
  "/platform-operations/data-analysis": "nav.dataAnalysis",
  "/basic-services": "nav.basicManagement",
  "/delivery-tasks": "nav.delivery",
  "/transport/services": "nav.transportManagement",
  "/traffic-boat": "nav.trafficBoat",
  "/traffic-routes": "nav.trafficRoutes",
  "/settlement": "nav.settlements",
  "/settlements": "nav.settlements",
  "/supply-chain-finance": "nav.supplyChainFinance",
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
  "SUPPLIER_PRODUCTS",
  "SHOP_PRODUCTS",
  "STORE_MANAGEMENT",
  "SHOP_MANAGEMENT",
  "ADMIN_REGISTRATIONS",
  "ADMIN_PERMISSION",
  "ADMIN_PERMISSIONS",
  "ADMIN_MENUS",
  "MENU_MANAGEMENT",
  "DATA_DICTIONARY",
  "impa",
  "suppliers",
  "supplierProducts",
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
  "supplierOrders",
  "settlements",
  "supplierSettlements",
  "evaluations"
]);

const foodProcurementChildKeys = new Set([
  "PROCUREMENT_FOOD",
  "FOOD",
  "FOOD_INQUIRIES",
  "FOOD_QUOTES",
  "FOOD_COMPARISON",
  "FOOD_COMPARE",
  "FOOD_ORDERS",
  "FOOD_SUPPLIER_ORDERS",
  "FOOD_SETTLEMENTS",
  "FOOD_SUPPLIER_SETTLEMENTS",
  "FOOD_EVALUATIONS",
  "food",
  "foodInquiries",
  "foodQuotes",
  "foodCompare",
  "foodOrders",
  "foodSupplierOrders",
  "foodSettlements",
  "foodSupplierSettlements",
  "foodEvaluations"
]);

const hiddenWorkbenchMenuKeys = new Set<string>([
  "delivery", "DELIVERY", "DELIVERY_TASKS", "supplyChainFinance", "SUPPLY_CHAIN_FINANCE"
]);
const hiddenWorkbenchMenuRoutes = new Set<string>([
  "/delivery-tasks", "/transport/services", "/settlement", "/supply-chain-finance"
]);

const developmentFoodMenuTemplates: WorkbenchMenuItem[] = [
  { key: "food", labelKey: "nav.food", route: "/procurement/food", icon: "FD", sortOrder: 0, roles },
  { key: "foodInquiries", labelKey: "nav.foodInquiries", route: "/food/inquiries", icon: "IQ", sortOrder: 10, roles },
  { key: "foodQuotes", labelKey: "nav.foodQuotes", route: "/food/quotes", icon: "QT", sortOrder: 20, roles },
  { key: "foodCompare", labelKey: "nav.foodCompare", route: "/food/comparison", icon: "CP", sortOrder: 30, roles },
  { key: "foodOrders", labelKey: "nav.foodOrders", route: "/food/orders", icon: "PO", sortOrder: 40, roles },
  { key: "foodSupplierOrders", labelKey: "nav.foodSupplierOrders", route: "/supplier/food/orders", icon: "SO", sortOrder: 50, roles },
  { key: "foodSettlements", labelKey: "nav.foodSettlements", route: "/food/settlements", icon: "ST", sortOrder: 60, roles },
  { key: "foodSupplierSettlements", labelKey: "nav.foodSupplierSettlements", route: "/supplier/food/settlements", icon: "SS", sortOrder: 70, roles },
  { key: "foodEvaluations", labelKey: "nav.foodEvaluations", route: "/food/evaluations", icon: "EV", sortOrder: 80, roles }
];

const foodProcurementParentKeys = new Set([
  "FOOD_PROCUREMENT",
  "FOOD_PROCUREMENT_GROUP",
  "FOOD_PROCUREMENT_MANAGEMENT",
  "foodProcurement",
  "foodProcurementGroup"
]);

function developmentFoodParent(existing?: WorkbenchMenuItem): WorkbenchMenuItem {
  const existingChildren = existing?.children || [];
  const knownRoutes = new Set(developmentFoodMenuTemplates.map((item) => item.route));
  const children = developmentFoodMenuTemplates.map((template) => ({
    ...existingChildren.find((item) => item.route === template.route),
    ...template,
    roles: [...roles]
  }));
  const additionalChildren = existingChildren
    .filter((item) => !knownRoutes.has(item.route))
    .map((item) => ({ ...item, roles: [...roles] }));
  return {
    ...existing,
    key: "foodProcurement",
    labelKey: "nav.foodProcurement",
    icon: existing?.icon || "FP",
    sortOrder: existing?.sortOrder ?? 10,
    roles: [...roles],
    route: undefined,
    children: sortByMenuOrder([...children, ...additionalChildren])
  };
}

export function ensureDevelopmentFoodMenus(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  let foundFoodParent = false;
  const visit = (nodes: WorkbenchMenuItem[]): WorkbenchMenuItem[] => nodes.map((item) => {
    if (foodProcurementParentKeys.has(item.key)) {
      foundFoodParent = true;
      return developmentFoodParent(item);
    }
    return item.children ? { ...item, children: visit(item.children) } : item;
  });
  const normalized = visit(items);
  if (foundFoodParent) return normalized;
  const procurementIndex = normalized.findIndex((item) => procurementServiceParentKeys.has(item.key));
  if (procurementIndex < 0) return [...normalized, developmentFoodParent()];
  return normalized.map((item, index) => index === procurementIndex
    ? { ...item, roles: [...roles], children: sortByMenuOrder([...(item.children || []), developmentFoodParent()]) }
    : item);
}

function ensureSettlementMenus(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  const templates: WorkbenchMenuItem[] = [
    { key: "settlements", labelKey: "nav.settlements", route: "/settlements", icon: "ST", sortOrder: 60, roles: ["admin", "purchaser"] },
    { key: "supplierSettlements", labelKey: "nav.supplierSettlements", route: "/supplier/settlements", icon: "SS", sortOrder: 70, roles: ["admin", "supplier"] },
    { key: "evaluations", labelKey: "nav.evaluations", route: "/evaluations", icon: "EV", sortOrder: 80, roles: ["admin", "purchaser"] }
  ];
  const withoutRootSettlements = items.filter((item) => !templates.some((template) => template.route === item.route));
  const hasMaterialTree = withoutRootSettlements.some((item) => ["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup"].includes(item.key));
  if (!hasMaterialTree) return [...withoutRootSettlements, ...templates];
  return withoutRootSettlements.map((item) => {
    if (!["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup"].includes(item.key)) return item;
    const children = [...(item.children || [])];
    templates.forEach((template) => {
      if (!children.some((child) => child.route === template.route)) children.push(template);
    });
    return { ...item, children: sortByMenuOrder(children) };
  });
}

function filterHiddenWorkbenchMenus(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  return items
    .filter((item) => !hiddenWorkbenchMenuKeys.has(item.key) && !hiddenWorkbenchMenuRoutes.has(item.route || ""))
    .map((item) => ({ ...item, children: item.children ? filterHiddenWorkbenchMenus(item.children) : undefined }))
    .filter((item) => item.route || item.children?.length);
}

function filterHiddenPermissionMenus(items: PermissionMenuNode[]): PermissionMenuNode[] {
  return items
    .filter((item) => !hiddenWorkbenchMenuKeys.has(item.key) && !hiddenWorkbenchMenuRoutes.has(item.route || ""))
    .map((item) => ({ ...item, children: item.children ? filterHiddenPermissionMenus(item.children) : undefined }))
    .filter((item) => item.route || item.children?.length || procurementServiceParentKeys.has(item.key) || item.key === "basicManagement" || item.key === "BASIC_MANAGEMENT");
}

function hasWorkbenchMenuRoute(items: WorkbenchMenuItem[], route: string): boolean {
  return items.some((item) => item.route === route || Boolean(item.children?.length && hasWorkbenchMenuRoute(item.children, route)));
}

function allowPurchaserTrafficMenu(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  return items.map((item) => {
    const children = item.children ? allowPurchaserTrafficMenu(item.children) : undefined;
    const isTrafficEntry =
      item.route === "/traffic-boat" ||
      item.route === "/traffic-boat/my-services" ||
      item.route === "/traffic-routes" ||
      children?.some((child) => child.route === "/traffic-boat" || child.route === "/traffic-boat/my-services" || child.route === "/traffic-routes");
    return isTrafficEntry
      ? { ...item, roles: Array.from(new Set([...item.roles, "purchaser" as UserRole])), children }
      : { ...item, children };
  });
}

function appendTrafficMyServicesMenu(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  return items.map((item) => {
    const children = item.children ? appendTrafficMyServicesMenu(item.children) : undefined;
    const ownsTrafficBoat = Boolean(children?.some((child) => child.route === "/traffic-boat"));
    if (!ownsTrafficBoat) return { ...item, children };
    const nextChildren = [...(children || [])];
    if (!nextChildren.some((child) => child.route === "/traffic-boat/my-services")) {
      nextChildren.push({ key: "trafficBoatMyServices", labelKey: "nav.trafficBoatMyServices", route: "/traffic-boat/my-services", icon: "MS", sortOrder: 5, roles: ["admin", "purchaser", "operator", "supplier"] as UserRole[] });
    }
    if (!nextChildren.some((child) => child.route === "/traffic-boat/settlements")) {
      nextChildren.push({ key: "bargeSettlements", labelKey: "nav.settlements", route: "/traffic-boat/settlements", icon: "ST", sortOrder: 10, roles: ["admin", "operator", "supplier"] as UserRole[] });
    }
    return {
      ...item,
      children: nextChildren.sort((first, second) => Number(first.sortOrder || 0) - Number(second.sortOrder || 0))
    };
  });
}

function ensureTrafficBoatMenu(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  if (hasWorkbenchMenuRoute(items, "/traffic-boat") || hasWorkbenchMenuRoute(items, "/traffic-routes")) return appendTrafficMyServicesMenu(allowPurchaserTrafficMenu(items));
  return [
    ...items,
    {
      key: "trafficService",
      labelKey: "nav.delivery",
      route: "/traffic-boat",
      icon: "TS",
      sortOrder: 90,
      roles: ["admin", "purchaser", "operator", "supplier"],
      children: [
        {
          key: "trafficBoat",
          labelKey: "nav.trafficBoat",
          route: "/traffic-boat",
          icon: "TB",
          sortOrder: 0,
          roles: ["admin", "purchaser", "operator", "supplier"]
        },
        {
          key: "trafficBoatMyServices",
          labelKey: "nav.trafficBoatMyServices",
          route: "/traffic-boat/my-services",
          icon: "MS",
          sortOrder: 5,
          roles: ["admin", "purchaser", "operator", "supplier"]
        },
        {
          key: "bargeSettlements",
          labelKey: "nav.settlements",
          route: "/traffic-boat/settlements",
          icon: "ST",
          sortOrder: 10,
          roles: ["admin", "operator", "supplier"]
        }
      ]
    }
  ];
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

const procurementServiceParentKeys = new Set(["PROCUREMENT_SERVICES", "procurementServices"]);
const procurementServiceChildKeys = new Set(["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup", "FOOD_PROCUREMENT", "FOOD_PROCUREMENT_GROUP", "FOOD_PROCUREMENT_MANAGEMENT", "foodProcurement", "foodProcurementGroup"]);

const fixedRootMenuTemplates: WorkbenchMenuItem[] = [
  {
    key: "regulatoryServices",
    labelKey: "nav.regulatoryServices",
    icon: "RS",
    sortOrder: 40,
    roles,
    children: [
      { key: "qualitySupervision", labelKey: "nav.qualitySupervision", route: "/regulatory/quality", icon: "QS", sortOrder: 0, roles }
    ]
  },
  { key: "portShippingServices", labelKey: "nav.portShippingServices", route: "/port-shipping-services", icon: "PH", sortOrder: 35, roles },
  { key: "borderInspectionServices", labelKey: "nav.borderInspectionServices", route: "/border-inspection-services", icon: "BI", sortOrder: 45, roles },
  { key: "maritimeServices", labelKey: "nav.maritimeServices", route: "/maritime-services", icon: "MS", sortOrder: 50, roles },
  { key: "taxServices", labelKey: "nav.taxServices", route: "/tax-services", icon: "TX", sortOrder: 60, roles },
  { key: "financialServices", labelKey: "nav.financialServices", route: "/financial-services", icon: "FN", sortOrder: 70, roles },
  { key: "weatherServices", labelKey: "nav.weatherServices", route: "/weather-services", icon: "WT", sortOrder: 80, roles },
  { key: "vesselDynamicsServices", labelKey: "nav.vesselDynamicsServices", route: "/vessel-dynamics-services", icon: "VD", sortOrder: 90, roles },
  {
    key: "platformOperations",
    labelKey: "nav.platformOperations",
    icon: "OP",
    sortOrder: 95,
    roles: ["admin", "supplier"],
    children: [
      { key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: "DA", sortOrder: 0, roles: ["admin", "supplier"] }
    ]
  }
];

function groupProcurementServiceMenus(items: WorkbenchMenuItem[]) {
  const existing = items.find((item) => procurementServiceParentKeys.has(item.key));
  const children = sortByMenuOrder(items.filter((item) => procurementServiceChildKeys.has(item.key)));
  const rest = items.filter((item) => !procurementServiceParentKeys.has(item.key) && !procurementServiceChildKeys.has(item.key));
  const mergedChildren = sortByMenuOrder([...(existing?.children || []), ...children]).map((child, index) => ({ ...child, sortOrder: index * 10 }));
  if (!mergedChildren.length && !existing) return items;
  return [
    ...rest,
    {
      ...(existing || {}),
      key: "procurementServices",
      labelKey: "nav.procurementServices",
      icon: "PS",
      sortOrder: 10,
      roles: mergeRolesFromChildren(mergedChildren.length ? mergedChildren : children),
      children: mergedChildren
    }
  ];
}

function ensureDashboardGovernmentMenu(items: WorkbenchMenuItem[]) {
  const dashboardChildren: WorkbenchMenuItem[] = [
    { key: "dashboardGovernment", labelKey: "nav.dashboardGovernment", route: "/dashboard-government", icon: "DG", sortOrder: 0, roles },
    { key: "dashboard", labelKey: "nav.dashboard", route: "/dashboard", icon: "DB", sortOrder: 5, roles }
  ];
  const hasDashboardGroup = items.some((item) => item.key === "dashboardGroup");
  if (hasDashboardGroup) return items;
  const dashboardItem = items.find((item) => item.route === "/dashboard" || item.key === "DASHBOARD" || item.key === "dashboard");
  const govItem = items.find((item) => item.route === "/dashboard-government" || item.key === "DASHBOARD_GOVERNMENT" || item.key === "dashboardGovernment");
  const rest = items.filter((item) => item !== dashboardItem && item !== govItem);
  if (!dashboardItem && !govItem) return items;
  const mergedChildren = sortByMenuOrder([
    govItem ? { ...govItem, key: "dashboardGovernment", labelKey: "nav.dashboardGovernment", route: "/dashboard-government", icon: govItem.icon || "DG", sortOrder: 0 } : dashboardChildren[0],
    dashboardItem ? { ...dashboardItem, key: "dashboard", labelKey: "nav.dashboard", route: "/dashboard", icon: dashboardItem.icon || "DB", sortOrder: 5 } : dashboardChildren[1]
  ]);
  return [
    ...rest,
    {
      key: "dashboardGroup",
      labelKey: "nav.dashboard",
      icon: "DB",
      sortOrder: 0,
      roles: mergeRolesFromChildren(mergedChildren),
      children: mergedChildren
    }
  ];
}

function normalizeBargeRootMenu(items: WorkbenchMenuItem[]) {
  return items.map((item) => {
    if (item.key === "platformOperations" || item.key === "PLATFORM_OPERATIONS") {
      const analyticsChild = item.children?.find((child) => child.route === "/platform-operations/data-analysis" || ["SUPPLIER_DATA_ANALYSIS", "DATA_ANALYSIS", "supplierDataAnalysis"].includes(child.key));
      const children = analyticsChild
        ? [{ ...analyticsChild, key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: analyticsChild.icon || "DA", sortOrder: 0, roles: ["admin", "supplier"] as UserRole[] }]
        : [{ key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: "DA", sortOrder: 0, roles: ["admin", "supplier"] as UserRole[] }];
      return { ...item, key: "platformOperations", labelKey: "nav.platformOperations", route: undefined, icon: "OP", sortOrder: 95, roles: ["admin", "supplier"] as UserRole[], children };
    }
    if (item.key === "trafficService" || item.key === "TRAFFIC_SERVICE") {
      return { ...item, key: "trafficService", labelKey: "nav.bargeServices", icon: "BS", sortOrder: 20 };
    }
    if (item.key === "regulatoryServices" || item.key === "REGULATORY_SERVICES") {
      const children = item.children?.length
        ? item.children
        : [{ key: "qualitySupervision", labelKey: "nav.qualitySupervision", route: "/regulatory/quality", icon: "QS", sortOrder: 0, roles }];
      return { ...item, key: "regulatoryServices", labelKey: "nav.regulatoryServices", route: undefined, icon: "RS", sortOrder: 40, children };
    }
    return item;
  });
}

function ensureFixedRootMenus(items: WorkbenchMenuItem[]) {
  return sortByMenuOrder([
    ...ensureDashboardGovernmentMenu(items),
    ...fixedRootMenuTemplates.filter((template) => !items.some((item) => item.key === template.key))
  ]);
}

function mergeBasicChildrenIntoExistingWorkbenchTree(items: WorkbenchMenuItem[]) {
  const externalChildren = sortByMenuOrder(items.filter((item) => basicManagementChildKeys.has(item.key)));
  const rest = items.filter((item) => !basicManagementChildKeys.has(item.key));
  if (!externalChildren.length) return rest;
  return rest.map((item) => {
    if (item.key !== "BASIC_MANAGEMENT" && item.key !== "basicManagement") return item;
    const children = sortByMenuOrder([...(item.children || []), ...externalChildren]).map((child, index) => ({ ...child, sortOrder: index * 10 }));
    return { ...item, key: "basicManagement", labelKey: "nav.basicManagement", icon: "BSV", sortOrder: 100, children };
  });
}

function groupBasicManagementMenus(items: WorkbenchMenuItem[]) {
  items = filterHiddenWorkbenchMenus(items);
  items = ensureSettlementMenus(items);
  items = ensureTrafficBoatMenu(items);
  items = groupWorkbenchChildren(items, ["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup"], materialProcurementChildKeys, {
    key: "materialProcurement",
    labelKey: "nav.materialProcurement",
    icon: "MP",
    sortOrder: 0
  });
  items = groupWorkbenchChildren(items, ["FOOD_PROCUREMENT", "FOOD_PROCUREMENT_GROUP", "FOOD_PROCUREMENT_MANAGEMENT", "foodProcurement", "foodProcurementGroup"], foodProcurementChildKeys, {
    key: "foodProcurement",
    labelKey: "nav.foodProcurement",
    icon: "FP",
    sortOrder: 10
  });
  items = ensureDevelopmentFoodMenus(items);
  items = groupProcurementServiceMenus(items);
  items = normalizeBargeRootMenu(items);
  const hasBasicTree = items.some((item) => item.key === "BASIC_MANAGEMENT" || item.key === "basicManagement");
  if (hasBasicTree) {
    return ensureFixedRootMenus(mergeBasicChildrenIntoExistingWorkbenchTree(items).filter((item) => !item.children || item.children.length > 0));
  }
  const basicChildren = sortByMenuOrder(items.filter((item) => basicManagementChildKeys.has(item.key)));
  if (!basicChildren.length) return ensureFixedRootMenus(items);
  const rest = items.filter((item) => !basicManagementChildKeys.has(item.key));
  return ensureFixedRootMenus([
    ...rest,
    {
      key: "basicManagement",
      labelKey: "nav.basicManagement",
      icon: "BSV",
      sortOrder: 100,
      roles: mergeRolesFromChildren(basicChildren),
      route: basicChildren[0]?.route,
      children: basicChildren.map((child, index) => ({ ...child, sortOrder: index * 10 }))
    }
  ]);
}

const fixedPermissionRootMenuTemplates: PermissionMenuNode[] = [
  { key: "regulatoryServices", labelKey: "nav.regulatoryServices", icon: "RS", sortOrder: 40, enabled: true },
  { key: "portShippingServices", labelKey: "nav.portShippingServices", route: "/port-shipping-services", icon: "PH", sortOrder: 35, enabled: true },
  { key: "borderInspectionServices", labelKey: "nav.borderInspectionServices", route: "/border-inspection-services", icon: "BI", sortOrder: 45, enabled: true },
  { key: "maritimeServices", labelKey: "nav.maritimeServices", route: "/maritime-services", icon: "MS", sortOrder: 50, enabled: true },
  { key: "taxServices", labelKey: "nav.taxServices", route: "/tax-services", icon: "TX", sortOrder: 60, enabled: true },
  { key: "financialServices", labelKey: "nav.financialServices", route: "/financial-services", icon: "FN", sortOrder: 70, enabled: true },
  { key: "weatherServices", labelKey: "nav.weatherServices", route: "/weather-services", icon: "WT", sortOrder: 80, enabled: true },
  { key: "vesselDynamicsServices", labelKey: "nav.vesselDynamicsServices", route: "/vessel-dynamics-services", icon: "VD", sortOrder: 90, enabled: true },
  {
    key: "platformOperations",
    labelKey: "nav.platformOperations",
    icon: "OP",
    sortOrder: 95,
    enabled: true,
    children: [
      { key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: "DA", sortOrder: 0, enabled: true }
    ]
  }
];

function groupProcurementServicePermissionMenus(items: PermissionMenuNode[]) {
  const existing = items.find((item) => procurementServiceParentKeys.has(item.key));
  const children = sortByMenuOrder(items.filter((item) => procurementServiceChildKeys.has(item.key)));
  const rest = items.filter((item) => !procurementServiceParentKeys.has(item.key) && !procurementServiceChildKeys.has(item.key));
  const mergedChildren = sortByMenuOrder([...(existing?.children || []), ...children]).map((child, index) => ({ ...child, sortOrder: index * 10 }));
  if (!mergedChildren.length && !existing) return items;
  return [
    ...rest,
    {
      ...(existing || {}),
      key: "procurementServices",
      labelKey: "nav.procurementServices",
      icon: "PS",
      sortOrder: 10,
      enabled: true,
      children: mergedChildren
    }
  ];
}

function normalizeBargeRootPermissionMenu(items: PermissionMenuNode[]) {
  return items.map((item) => {
    if (item.key === "platformOperations" || item.key === "PLATFORM_OPERATIONS") {
      const analyticsChild = item.children?.find((child) => child.route === "/platform-operations/data-analysis" || ["SUPPLIER_DATA_ANALYSIS", "DATA_ANALYSIS", "supplierDataAnalysis"].includes(child.key));
      return {
        ...item,
        key: "platformOperations",
        labelKey: "nav.platformOperations",
        route: undefined,
        icon: "OP",
        sortOrder: 95,
        enabled: true,
        children: [analyticsChild
          ? { ...analyticsChild, key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: analyticsChild.icon || "DA", sortOrder: 0, enabled: true }
          : { key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: "DA", sortOrder: 0, enabled: true }]
      };
    }
    return item.key === "trafficService" || item.key === "TRAFFIC_SERVICE"
      ? { ...item, key: "trafficService", labelKey: "nav.bargeServices", icon: "BS", sortOrder: 20 }
      : item;
  });
}

function ensureFixedPermissionRootMenus(items: PermissionMenuNode[]) {
  return sortByMenuOrder([
    ...items,
    ...fixedPermissionRootMenuTemplates.filter((template) => !items.some((item) => item.key === template.key))
  ]);
}

function mergeBasicChildrenIntoExistingPermissionTree(items: PermissionMenuNode[]) {
  const externalChildren = sortByMenuOrder(items.filter((item) => basicManagementChildKeys.has(item.key)));
  const rest = items.filter((item) => !basicManagementChildKeys.has(item.key));
  if (!externalChildren.length) return rest;
  return rest.map((item) => {
    if (item.key !== "BASIC_MANAGEMENT" && item.key !== "basicManagement") return item;
    const children = sortByMenuOrder([...(item.children || []), ...externalChildren]).map((child, index) => ({ ...child, sortOrder: index * 10 }));
    return { ...item, key: "basicManagement", labelKey: "nav.basicManagement", icon: "BSV", sortOrder: 100, children };
  });
}

function groupBasicManagementPermissionMenus(items: PermissionMenuNode[]) {
  items = filterHiddenPermissionMenus(items);
  items = groupPermissionChildren(items, ["MATERIAL_PROCUREMENT", "MATERIAL_PROCUREMENT_GROUP", "materialProcurement", "materialProcurementGroup"], materialProcurementChildKeys, {
    key: "materialProcurement",
    labelKey: "nav.materialProcurement",
    icon: "MP",
    sortOrder: 0
  });
  items = groupPermissionChildren(items, ["FOOD_PROCUREMENT", "FOOD_PROCUREMENT_GROUP", "FOOD_PROCUREMENT_MANAGEMENT", "foodProcurement", "foodProcurementGroup"], foodProcurementChildKeys, {
    key: "foodProcurement",
    labelKey: "nav.foodProcurement",
    icon: "FP",
    sortOrder: 10
  });
  items = groupProcurementServicePermissionMenus(items);
  items = normalizeBargeRootPermissionMenu(items);
  const hasBasicTree = items.some((item) => item.key === "BASIC_MANAGEMENT" || item.key === "basicManagement");
  if (hasBasicTree) {
    return ensureFixedPermissionRootMenus(mergeBasicChildrenIntoExistingPermissionTree(items).filter((item) => !item.children || item.children.length > 0));
  }
  const basicChildren = sortByMenuOrder(items.filter((item) => basicManagementChildKeys.has(item.key)));
  if (!basicChildren.length) return ensureFixedPermissionRootMenus(items);
  const rest = items.filter((item) => !basicManagementChildKeys.has(item.key));
  return ensureFixedPermissionRootMenus([
    ...rest,
    {
      key: "basicManagement",
      labelKey: "nav.basicManagement",
      icon: "BSV",
      sortOrder: 100,
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
    supplierServiceTypes: readArray(item, ["supplierServiceTypes", "serviceTypes"]),
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
