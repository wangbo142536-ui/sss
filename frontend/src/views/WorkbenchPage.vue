<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AnimatedTabs from "@/components/AnimatedTabs.vue";
import AttributeSummary from "@/components/AttributeSummary.vue";
import ConfirmDialog from "@/components/ConfirmDialog.vue";
import DataTable from "@/components/DataTable.vue";
import DetailDrawer from "@/components/DetailDrawer.vue";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import FilterToolbar from "@/components/FilterToolbar.vue";
import IconButton from "@/components/IconButton.vue";
import ImagePreviewModal from "@/components/ImagePreviewModal.vue";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import SkuThumbnail from "@/components/SkuThumbnail.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import {
  crewServiceRows,
  foodComparisonRows,
  foodInquiries,
  foodOrderRows,
  foodQuotes,
  inquiries,
  metrics,
  quoteRows,
  requestRows,
  suppliers,
  settlementRows
} from "@/data/mockWorkbench";
import { t, useI18n } from "@/i18n";
import { ApiError, clearAuthSession, getAuthSession, uploadQualificationFile } from "@/services/authService";
import {
  createCompanyMember,
  getCompanyMembers,
  getCompanyRoles,
  resetCompanyMemberPassword,
  updateCompanyMemberRoles,
  updateCompanyMemberStatus
} from "@/services/companyMemberService";
import {
  createCompanyQualification,
  deleteCompanyQualification,
  getCompanyProfile,
  listCompanyQualifications,
  updateCompanyProfile,
  updateCompanyQualification
} from "@/services/companyService";
import {
  assignUserRoles,
  approveRegistration,
  getAdminRegistrations,
  getAdminMenuPermissions,
  getAdminPermissions,
  getAdminRoles,
  getAdminUsers,
  getManageableAdminMenus,
  getRoleMenuPermissions,
  rejectRegistration,
  saveAdminMenuSortOrders,
  saveRoleMenuPermissions,
} from "@/services/permissionService";
import { getMaterialDemandComparison, listMaterialDemands } from "@/services/procurementMaterialService";
import {
  confirmSupplierPurchaseOrder,
  createPurchaseOrderFromDemand,
  getPurchaseOrderDetail,
  listPurchaseOrders,
  listSupplierPurchaseOrders,
  rejectSupplierPurchaseOrder
} from "@/services/purchaseOrderService";
import {
  batchUpsertShopSkus,
  deleteShopSku,
  listShopSkus,
  previewShopSkuImport,
  resolveShopSkuException,
  updateShopSkuShelfStatus
} from "@/services/shopService";
import { getImpaStandardCategories, getImpaStandardItems } from "@/services/standardLibraryService";
import type { CompanyMember, CompanyMemberStatus, CompanyRole } from "@/services/companyMemberService";
import type { AdminRegistration, AdminUser, PermissionMenuNode, PermissionPoint, PermissionRole } from "@/services/permissionService";
import type { PurchaseOrderDetail, PurchaseOrderSummary, PurchaseSupplierOrder } from "@/services/purchaseOrderService";
import type { MaterialComparisonCandidate, MaterialComparisonItem, MaterialComparisonStrategy, MaterialDemandComparisonResponse, MaterialDemandSummary } from "@/types/procurementMaterials";
import type { ImpaStandardItem, StandardCategoryNode } from "@/types/standardLibrary";
import type { SkuAttribute, SkuImage, StatusVariant, SupplierSku, TableColumn } from "@/types/workbench";

type FilterField = {
  key: string;
  label: string;
  type?: "text" | "select" | "date";
  placeholder?: string;
  options?: Array<{ label: string; value: string }>;
};

type ProcurementFlowRow = {
  code?: string;
  inquiryNo?: string;
  subject?: string;
  supplier: string;
  status: string;
  validUntil: string;
};

type ProcurementOrderRow = {
  code: string;
  sourceNo: string;
  vesselName: string;
  supplier: string;
  amount: string;
  status: string;
  orderDate: string;
  deliveryDate: string;
};

type SettlementRow = {
  code: string;
  type: "material" | "food";
  sourceNo: string;
  vesselName: string;
  supplier: string;
  amount: string;
  status: string;
  applyDate: string;
  settlementDate: string;
};

type ShopEditableSpec = {
  id: string;
  key: string;
  name: string;
  value: string;
  unit: string;
};

type ShopCategoryCandidate = {
  categoryCode: string;
  categoryName: string;
  segmentCode: string;
  segmentName: string;
  matchedKeyword: string;
  reason: string;
  itemCount: number;
};

type ShopSkuImageData = {
  fileId: string;
  imageUrl: string;
  thumbnailUrl: string;
  primary: boolean;
  sortOrder: number;
};

type ShopPreviewAction = "" | "INSERT" | "UPDATE" | "BLOCKED" | "DUPLICATE";

type ShopExistingSnapshot = {
  skuId?: string | number;
  supplierSkuCode: string;
  productName: string;
  productType: string;
  category: string;
  categoryCode: string;
  categoryName: string;
  platformCode: string;
  impaCode: string;
  stock: number;
  leadTimeDays: number;
  deliveryArea: string;
  imageUrl: string;
  images: ShopSkuImageData[];
  monthlySales: number;
  price: number;
  currency: string;
  brand: string;
  unit: string;
  packing: string;
  listingStatus: string;
};

type ShopSkuRow = {
  id: string;
  skuId?: string | number;
  existingSkuId?: string | number;
  previewRowId?: string | number;
  isPreview?: boolean;
  previewAction: ShopPreviewAction;
  existingSnapshot?: ShopExistingSnapshot;
  productType: string;
  category: string;
  categoryCode: string;
  categoryName: string;
  platformCode: string;
  impaCode?: string;
  supplierSkuCode: string;
  productName: string;
  specs: string[];
  specItems: ShopEditableSpec[];
  stock: number;
  leadTime: string;
  leadTimeDays: number;
  deliveryArea: string;
  thumbnail: string;
  imageFileId: string;
  imageUrl: string;
  images: ShopSkuImageData[];
  monthlySales: number;
  price: number;
  currency: string;
  brand: string;
  unit: string;
  packing: string;
  barcode: string;
  listingStatus: string;
  codingStatus: string;
  exceptionReason: string;
  importBatch: string | number;
  importRowNo: number;
  categoryCandidates: ShopCategoryCandidate[];
};

type ShopProfileForm = {
  shopName: string;
  creditCode: string;
  logoFileId: string;
  logoUrl: string;
  description: string;
  mainCategories: string[];
  servicePorts: string[];
  deliveryAreas: string[];
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  companyType: string;
  status: string;
};

type ShopImportPreviewSummary = {
  batchId: string | number;
  status: string;
  totalCount: number;
  successCount: number;
  exceptionCount: number;
};

type CompanyQualificationRow = {
  id: string;
  qualificationId?: string | number;
  fileId: string;
  fileName: string;
  fileUrl: string;
  imagePreviewUrl: string;
  name: string;
  type: string;
  typeCode: string;
  status: string;
  description: string;
  uploadedAt: string;
  updatedAt: string;
};

type CompareSupplyEditableKey = "vessel" | "port" | "date";

type CompareSupplierCard = {
  key: string;
  supplier: string;
  skuCount: number;
  amount: string;
};

type CompareStrategyCard = {
  key: string;
  label: string;
  matchSummary: string;
  total: string;
  tone: "green" | "blue";
  suppliers: CompareSupplierCard[];
};

type CompareSkuRow = SupplierSku & {
  supplierKey: string;
  supplierName: string;
  demandItemId: string;
  lowestPrice?: number;
  matchType?: string;
  reason?: string;
};

const route = useRoute();
const router = useRouter();
const { language } = useI18n();
const pageKey = computed(() => String(route.meta.pageKey || "dashboard"));
const procurementFlowPageKeys = new Set([
  "inquiries",
  "quotes",
  "orders",
  "foodInquiries",
  "foodQuotes",
  "foodComparisonList",
  "foodOrders",
  "crewServices"
]);
const selectedStrategy = ref("LOWEST_MIXED");
const selectedCompareSupplier = ref<string | null>(null);
const compareWorkspaceRef = ref<HTMLElement | null>(null);
const showCompareBackTop = ref(false);
const compareData = ref<MaterialDemandComparisonResponse | null>(null);
const compareLoading = ref(false);
const compareError = ref("");
const compareSupplyForm = ref<Record<CompareSupplyEditableKey, string>>({
  vessel: t("compare.supply.vesselValue"),
  port: t("compare.supply.portValue"),
  date: t("compare.supply.dateValue")
});
const compareSkuKeyword = ref("");
const comparePreferenceFilters = ref<string[]>([]);
const replacementSku = ref<SupplierSku | null>(null);
const purchaseOrderDialogOpen = ref(false);
const purchaseOrderCreating = ref(false);
const purchaseOrderError = ref("");
const purchaseOrderNotice = ref("");
const purchaseOrderForm = ref({
  supplyPort: "",
  vesselEta: "",
  requiredDeliveryTime: "",
  defaultPackagingMethod: "UNIFIED_PACKAGING",
  buyerRemark: ""
});
const purchaseOrders = ref<PurchaseOrderSummary[]>([]);
const supplierPurchaseOrders = ref<PurchaseOrderSummary[]>([]);
const purchaseOrderLoading = ref(false);
const supplierPurchaseOrderLoading = ref(false);
const purchaseOrderErrorKey = ref("");
const supplierPurchaseOrderErrorKey = ref("");
const purchaseOrderKeyword = ref("");
const purchaseOrderStatus = ref("");
const purchaseOrderSupplier = ref("");
const purchaseOrderCreatedFrom = ref("");
const purchaseOrderCreatedTo = ref("");
const purchaseOrderDeliveryFrom = ref("");
const purchaseOrderDeliveryTo = ref("");
const purchaseOrderDetail = ref<PurchaseOrderDetail | null>(null);
const purchaseOrderDetailLoading = ref(false);
const purchaseOrderDetailError = ref("");
const supplierActionOrder = ref<PurchaseSupplierOrder | null>(null);
const supplierActionType = ref<"confirm" | "reject" | "">("");
const supplierActionSaving = ref(false);
const supplierConfirmForm = ref({
  expectedReadyAt: "",
  discountType: "",
  discountValue: "",
  packagingMethod: "SUPPLIER_PACKAGING",
  supplierRemark: ""
});
const supplierRejectReason = ref("");
const activeAdminTab = ref("users");
const loading = ref(false);
const confirmOpen = ref(false);
const drawerOpen = ref(false);
const drawerTitle = ref("");
const drawerSubtitle = ref("");
const drawerAttributes = ref<SkuAttribute[]>([]);
const drawerFileLinks = ref<Array<{ key: string; name: string; url: string }>>([]);
const shopLogoInput = ref<HTMLInputElement | null>(null);
const shopLogoPreview = ref("");
const shopLogoUploading = ref(false);
const shopImportInput = ref<HTMLInputElement | null>(null);
const shopSkuImageInput = ref<HTMLInputElement | null>(null);
const selectedShopSkuImageRowId = ref("");
const shopSkuImageUploading = ref(false);
const shopImportOverlayVisible = ref(false);
const shopImportProgress = ref(0);
const shopImportStageIndex = ref(0);
const shopImportFileName = ref("");
const shopImportPending = ref(false);
const shopProfileLoading = ref(false);
const companyQualificationLoading = ref(false);
const shopSkuListLoading = ref(false);
const shopLoading = ref(false);
const shopSaving = ref(false);
const shopImporting = ref(false);
const shopImportPreview = ref<ShopImportPreviewSummary | null>(null);
const shopPersistedSkuRows = ref<ShopSkuRow[]>([]);
const shopPreviewSkuRows = ref<ShopSkuRow[]>([]);
const shopSkuRows = ref<ShopSkuRow[]>([]);
const shopDirtySkuIds = ref<Set<string>>(new Set());
const expandedShopSkuId = ref("");
const shopManagementTab = ref<"products" | "qualifications">("products");
const shopListFullscreen = ref(false);
const shopProfileForm = ref<ShopProfileForm>({
  shopName: "",
  creditCode: "",
  logoFileId: "",
  logoUrl: "",
  description: "",
  mainCategories: [],
  servicePorts: [],
  deliveryAreas: [],
  contactName: "",
  contactPhone: "",
  contactEmail: "",
  companyType: "",
  status: "ACTIVE"
});
const shopProductKeyword = ref("");
const shopProductTypeFilter = ref("");
const shopProductCodeStatusFilter = ref("");
const shopProductShelfStatusFilter = ref("");
const shopSkuPage = ref(1);
const shopSkuPageSize = 50;
const shopSkuTotal = ref(0);
const shopNoticeKey = ref("");
const shopNoticeMessage = ref("");
const shopErrorMessage = ref("");
const companyQualificationRows = ref<CompanyQualificationRow[]>([]);
const companyQualificationSaving = ref(false);
const companyQualificationUploading = ref(false);
const companyQualificationFileInput = ref<HTMLInputElement | null>(null);
const selectedCompanyQualificationRow = ref<CompanyQualificationRow | null>(null);
const protectedImageObjectUrls = new Set<string>();
const authenticatedImageUrlCache = new Map<string, string>();
type ShopSkuPageCacheEntry = {
  page: number;
  total: number;
  rows: ShopSkuRow[];
};
const shopSkuPageCache = new Map<string, ShopSkuPageCacheEntry>();
let shopImportTimer: number | undefined;
const previewOpen = ref(false);
const previewTitle = ref("");
const previewImages = ref<SkuImage[]>([]);
const previewAttributes = ref<SkuAttribute[]>([]);
const impaCategories = ref<StandardCategoryNode[]>([]);
const selectedImpaCategoryCode = ref("");
const impaKeyword = ref("");
const impaItems = ref<ImpaStandardItem[]>([]);
const impaCategoriesLoading = ref(false);
const impaItemsLoading = ref(false);
const expandedImpaCode = ref("");
const permissionLoading = ref(false);
const permissionSaving = ref(false);
const menuOrderSaving = ref(false);
const menuOrderErrorKey = ref("");
const permissionRoles = ref<PermissionRole[]>([]);
const permissionMenus = ref<PermissionMenuNode[]>([]);
const menuOrderMenus = ref<PermissionMenuNode[]>([]);
const permissionPoints = ref<PermissionPoint[]>([]);
const adminUsers = ref<AdminUser[]>([]);
const selectedPermissionRoleCode = ref("admin");
const selectedPermissionCodes = ref<string[]>([]);
const selectedAdminUserId = ref("");
const selectedAdminUserRoleCode = ref("admin");
type StaticPermissionUser = {
  id: string;
  username: string;
  name: string;
  phone: string;
  email: string;
  roles: string[];
  status: "ACTIVE" | "DISABLED";
  lastLogin: string;
};

const permissionUserKeyword = ref("");
const permissionUserStatusFilter = ref("");
const permissionUserRoleFilter = ref("");
const selectedStaticPermissionUserId = ref("u-admin");
const expandedStaticPermissionUserId = ref("");
const staticPermissionSaving = ref(false);
const staticPermissionNoticeKey = ref("");
const staticUserPanelMode = ref<"create" | "edit" | "reset" | "delete" | "">("");
const staticPermissionCodes = ref<string[]>([
  "dashboard",
  "materialProcurement",
  "materials",
  "inquiries",
  "quotes",
  "comparison",
  "orders",
  "foodProcurement",
  "food",
  "foodInquiries",
  "foodQuotes",
  "foodComparison",
  "foodOrders",
  "impa",
  "suppliers",
  "delivery",
  "settlement",
  "crewServices",
  "registrations",
  "permissions",
  "menuManagement"
]);
const staticPermissionUsers = ref<StaticPermissionUser[]>([
  {
    id: "u-admin",
    username: "company_admin",
    name: "王博",
    phone: "13800000001",
    email: "admin@shipsupply.local",
    roles: ["企业管理员"],
    status: "ACTIVE",
    lastLogin: "2026-06-07 09:42"
  },
  {
    id: "u-buyer",
    username: "buyer01",
    name: "李采购",
    phone: "13800000002",
    email: "buyer01@shipsupply.local",
    roles: ["采购员"],
    status: "ACTIVE",
    lastLogin: "2026-06-06 18:20"
  },
  {
    id: "u-finance",
    username: "finance01",
    name: "陈财务",
    phone: "13800000003",
    email: "finance01@shipsupply.local",
    roles: ["财务"],
    status: "ACTIVE",
    lastLogin: "2026-06-05 16:05"
  },
  {
    id: "u-disabled",
    username: "ops_stop",
    name: "赵运营",
    phone: "13800000004",
    email: "ops.stop@shipsupply.local",
    roles: ["运营"],
    status: "DISABLED",
    lastLogin: "2026-05-30 11:16"
  }
]);
const registrations = ref<AdminRegistration[]>([]);
const registrationLoading = ref(false);
const registrationActionLoading = ref(false);
const registrationActionId = ref("");
const registrationErrorKey = ref("");
const registrationKeyword = ref("");
const registrationStatusFilter = ref("PENDING_REVIEW");
const expandedRegistrationId = ref("");
const rejectDialogOpen = ref(false);
const rejectReason = ref("");
const rejectReasonError = ref(false);
const selectedRegistrationId = ref("");
const companyMembers = ref<CompanyMember[]>([]);
const companyRoles = ref<CompanyRole[]>([]);
const companyMemberLoading = ref(false);
const companyMemberSaving = ref(false);
const companyMemberErrorKey = ref("");
const companyMemberNoticeKey = ref("");
const companyMemberKeyword = ref("");
const companyMemberStatusFilter = ref("");
const companyMemberRoleFilter = ref("");
const memberDrawerOpen = ref(false);
const memberDrawerMode = ref<"create" | "roles" | "detail">("create");
const selectedCompanyMemberId = ref("");
const memberForm = ref({
  username: "",
  name: "",
  phone: "",
  email: "",
  password: "Temp@123456",
  roleCodes: [] as string[]
});
const memberFormErrors = ref<Record<string, string>>({});
const memberConfirmOpen = ref(false);
const memberConfirmAction = ref<"enable" | "disable" | "reset">("disable");
const today = new Date();
const currentMonthStart = new Date(today.getFullYear(), today.getMonth(), 1);
const calendarMonth = ref(new Date(currentMonthStart));
const selectedCalendarDate = ref(formatDateKey(today));
const calendarReminderStatuses = ["red", "yellow", "blue"] as const;
const dashboardRequestFilters = ref({
  requestNo: "",
  item: "",
  vessel: "",
  status: ""
});
const dashboardVesselQuery = ref("BLUE PORT");
const selectedDashboardVesselName = ref("BLUE PORT");

const dashboardVessels = [
  {
    name: "SEA SOAR",
    x: 23,
    y: 61,
    rotation: -24,
    speed: "10.8 kn",
    course: "035°",
    distance: "5.4 NM",
    statusKey: "dashboard.shipPosition.status.supply"
  },
  {
    name: "BLUE PORT",
    x: 56,
    y: 43,
    rotation: 18,
    speed: "8.6 kn",
    course: "082°",
    distance: "2.1 NM",
    statusKey: "dashboard.shipPosition.status.approaching"
  },
  {
    name: "ZHONG WAI YUN 6",
    x: 74,
    y: 67,
    rotation: -8,
    speed: "6.2 kn",
    course: "116°",
    distance: "7.8 NM",
    statusKey: "dashboard.shipPosition.status.anchored"
  },
  {
    name: "PACIFIC TRADER",
    x: 39,
    y: 28,
    rotation: 34,
    speed: "12.4 kn",
    course: "061°",
    distance: "9.3 NM",
    statusKey: "dashboard.shipPosition.status.inbound"
  }
];

const activeImpaCategory = computed(() => impaCategories.value.find((category) => category.code === selectedImpaCategoryCode.value));
const selectedPermissionRole = computed(() => permissionRoles.value.find((role) => role.code === selectedPermissionRoleCode.value));
const selectedAdminUser = computed(() => adminUsers.value.find((user) => user.id === selectedAdminUserId.value));
const selectedStaticPermissionUser = computed(() => staticPermissionUsers.value.find((user) => user.id === selectedStaticPermissionUserId.value) || staticPermissionUsers.value[0]);
const staticPermissionRoleOptions = computed(() => Array.from(new Set(staticPermissionUsers.value.flatMap((user) => user.roles))));
const permissionUserColumns = computed<TableColumn[]>(() => [
  { key: "username", label: t("permission.accountUsername"), width: "122px" },
  { key: "name", label: t("permission.accountName"), width: "96px" },
  { key: "phone", label: t("permission.accountPhone"), width: "118px" },
  { key: "email", label: t("permission.accountEmail") },
  { key: "roles", label: t("permission.accountRoles"), width: "128px" },
  { key: "status", label: t("field.status"), width: "86px", align: "center" },
  { key: "lastLogin", label: t("permission.lastLogin"), width: "142px" },
  { key: "operation", label: t("common.operation"), width: "132px", align: "center" }
]);
const filteredPermissionUsers = computed(() => {
  const keyword = permissionUserKeyword.value.trim().toLowerCase();
  return staticPermissionUsers.value.filter((user) => {
    const matchesKeyword =
      !keyword ||
      [user.username, user.name, user.phone, user.email].some((value) => value.toLowerCase().includes(keyword));
    const matchesStatus = !permissionUserStatusFilter.value || user.status === permissionUserStatusFilter.value;
    const matchesRole = !permissionUserRoleFilter.value || user.roles.includes(permissionUserRoleFilter.value);
    return matchesKeyword && matchesStatus && matchesRole;
  });
});
const staticPermissionGroups = computed(() => [
  {
    key: "core",
    name: t("permission.group.core"),
    items: [
      { code: "dashboard", label: t("nav.dashboard") },
      { code: "impa", label: t("nav.impa") }
    ]
  },
  {
    key: "procurement",
    name: t("permission.group.procurement"),
    items: [
      { code: "materialProcurement", label: t("nav.materialProcurement") },
      { code: "materials", label: t("nav.requests") },
      { code: "inquiries", label: t("nav.inquiries") },
      { code: "quotes", label: t("nav.quotes") },
      { code: "comparison", label: t("nav.compare") },
      { code: "orders", label: t("nav.orders") },
      { code: "foodProcurement", label: t("nav.foodProcurement") },
      { code: "food", label: t("nav.food") },
      { code: "foodInquiries", label: t("nav.foodInquiries") },
      { code: "foodQuotes", label: t("nav.foodQuotes") },
      { code: "foodComparison", label: t("nav.foodCompare") },
      { code: "foodOrders", label: t("nav.foodOrders") },
      { code: "suppliers", label: t("nav.suppliers") },
      { code: "supplierProducts", label: t("nav.supplierProducts") }
    ]
  },
  {
    key: "operation",
    name: t("permission.group.operation"),
    items: [
      { code: "delivery", label: t("nav.delivery") },
      { code: "settlement", label: t("nav.settlements") },
      { code: "crewServices", label: t("nav.crewServices") },
      { code: "registrations", label: t("nav.registrations") },
      { code: "permissions", label: t("nav.permissions") },
      { code: "menuManagement", label: t("nav.menuManagement") }
    ]
  }
]);
const selectedRegistration = computed(() => registrations.value.find((item) => item.id === selectedRegistrationId.value));
const selectedCompanyMember = computed(() => companyMembers.value.find((member) => member.id === selectedCompanyMemberId.value));
const materialDemands = ref<MaterialDemandSummary[]>([]);
const materialDemandLoading = ref(false);
const materialDemandErrorKey = ref("");
const materialDemandKeyword = ref("");
const highlightedDemandId = computed(() => String(route.query.demandId || ""));
const materialDemandRows = computed(() => materialDemands.value as unknown as Record<string, unknown>[]);
const inquiryDemandRows = ref<MaterialDemandSummary[]>([]);
const inquiryDemandLoading = ref(false);
const inquiryDemandErrorKey = ref("");
const inquiryDemandKeyword = ref("");
const inquiryDemandStatus = ref("");
const inquiryDemandDateFrom = ref("");
const inquiryDemandDateTo = ref("");
const inquiryDemandHasFilters = computed(() =>
  Boolean(inquiryDemandKeyword.value.trim() || inquiryDemandStatus.value.trim() || inquiryDemandDateFrom.value.trim() || inquiryDemandDateTo.value.trim())
);
const inquiryDemandTableRows = computed(() => inquiryDemandRows.value as unknown as Record<string, unknown>[]);
const comparisonDemandRows = ref<MaterialDemandSummary[]>([]);
const comparisonDemandLoading = ref(false);
const comparisonDemandErrorKey = ref("");
const comparisonDemandKeyword = ref("");
const comparisonDemandStatus = ref("");
const comparisonDemandDateFrom = ref("");
const comparisonDemandDateTo = ref("");
const comparisonDemandHasFilters = computed(() =>
  Boolean(comparisonDemandKeyword.value.trim() || comparisonDemandStatus.value.trim() || comparisonDemandDateFrom.value.trim() || comparisonDemandDateTo.value.trim())
);
const comparisonDemandTableRows = computed(() => comparisonDemandRows.value as unknown as Record<string, unknown>[]);
type MenuManagementRow = {
  id: string;
  menuCode: string;
  menuName: string;
  level: number;
  parentCode: string;
  sortOrder: number;
  enabled: boolean;
};
const menuManagementRows = ref<MenuManagementRow[]>([]);
const menuManagementLoading = ref(false);
const menuManagementNoticeKey = ref("");
const menuManagementColumns = computed<TableColumn[]>(() => [
  { key: "menuName", label: t("menuManagement.menuName") },
  { key: "level", label: t("menuManagement.level"), width: "86px", align: "center" },
  { key: "parentCode", label: t("menuManagement.parent"), width: "170px" },
  { key: "sortOrder", label: t("menuManagement.sortOrder"), width: "106px", align: "center" },
  { key: "enabled", label: t("menuManagement.visible"), width: "112px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "112px", align: "center" }
]);
const menuManagementParentOptions = computed(() => menuManagementRows.value.filter((row) => row.level === 1));
const menuManagementTableRows = computed(() => menuManagementRows.value as unknown as Record<string, unknown>[]);

function formatDateKey(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

const calendarWeekdays = computed(() => [
  t("dashboard.calendar.weekdays.sun"),
  t("dashboard.calendar.weekdays.mon"),
  t("dashboard.calendar.weekdays.tue"),
  t("dashboard.calendar.weekdays.wed"),
  t("dashboard.calendar.weekdays.thu"),
  t("dashboard.calendar.weekdays.fri"),
  t("dashboard.calendar.weekdays.sat")
]);

const calendarMonthLabel = computed(() =>
  new Intl.DateTimeFormat(language.value === "zh-CN" ? "zh-CN" : "en-US", {
    year: "numeric",
    month: "long"
  }).format(calendarMonth.value)
);

const canGoPreviousCalendarMonth = computed(() => calendarMonth.value > currentMonthStart);

const calendarDays = computed(() => {
  const year = calendarMonth.value.getFullYear();
  const month = calendarMonth.value.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDay = new Date(year, month, 1).getDay();
  const cells: Array<{
    key: string;
    day?: number;
    dateKey?: string;
    isToday?: boolean;
    isPast?: boolean;
    isSelectable?: boolean;
    reminders?: typeof calendarReminderStatuses;
  }> = [];

  for (let index = 0; index < firstDay; index += 1) {
    cells.push({ key: `blank-${index}` });
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = new Date(year, month, day);
    const dateKey = formatDateKey(date);
    const isPast = dateKey < formatDateKey(today);
    cells.push({
      key: dateKey,
      day,
      dateKey,
      isToday: dateKey === formatDateKey(today),
      isPast,
      isSelectable: !isPast,
      reminders: calendarReminderStatuses
    });
  }

  return cells;
});

const dashboardRequestRows = computed(() => {
  const requestNo = dashboardRequestFilters.value.requestNo.trim().toLowerCase();
  const item = dashboardRequestFilters.value.item.trim().toLowerCase();
  const vessel = dashboardRequestFilters.value.vessel.trim().toLowerCase();
  const status = dashboardRequestFilters.value.status.trim().toLowerCase();

  return requestRows.filter((row) => {
    const source = row as Record<string, unknown>;
    const requestNoText = String(source.requestNo ?? "").toLowerCase();
    const itemText = String(source.itemName ?? source.item ?? source.type ?? "").toLowerCase();
    const vesselText = String(source.vessel ?? "").toLowerCase();
    const statusText = String(source.status ?? "").toLowerCase();

    return (
      (!requestNo || requestNoText.includes(requestNo)) &&
      (!item || itemText.includes(item)) &&
      (!vessel || vesselText.includes(vessel)) &&
      (!status || statusText.includes(status))
    );
  });
});

const selectedDashboardVessel = computed(
  () => dashboardVessels.find((vessel) => vessel.name === selectedDashboardVesselName.value) || dashboardVessels[1]
);

const dashboardWeatherItems = computed(() => [
  {
    key: "wind",
    label: t("dashboard.weather.wind"),
    value: t("dashboard.weather.windValue")
  },
  {
    key: "visibility",
    label: t("dashboard.weather.visibility"),
    value: t("dashboard.weather.visibilityValue")
  },
  {
    key: "wave",
    label: t("dashboard.weather.wave"),
    value: t("dashboard.weather.waveValue")
  },
  {
    key: "tide",
    label: t("dashboard.weather.tide"),
    value: t("dashboard.weather.tideValue")
  }
]);

const queryDashboardVessel = () => {
  const keyword = dashboardVesselQuery.value.trim().toLowerCase();
  const matched = dashboardVessels.find((vessel) => vessel.name.toLowerCase().includes(keyword));
  selectedDashboardVesselName.value = matched?.name || "BLUE PORT";
};

const selectCalendarDate = (dateKey?: string, isSelectable?: boolean) => {
  if (!dateKey || !isSelectable) return;
  selectedCalendarDate.value = dateKey;
};

const changeCalendarMonth = (offset: number) => {
  const nextMonth = new Date(calendarMonth.value.getFullYear(), calendarMonth.value.getMonth() + offset, 1);
  if (nextMonth < currentMonthStart) return;
  calendarMonth.value = nextMonth;
};

const resetDashboardRequestFilters = () => {
  dashboardRequestFilters.value = {
    requestNo: "",
    item: "",
    vessel: "",
    status: ""
  };
};

const registrationStatusFilters = computed(() => [
  { value: "PENDING_REVIEW", label: t("registration.filter.pending") },
  { value: "ACTIVE", label: t("registration.filter.approved") },
  { value: "REJECTED", label: t("registration.filter.rejected") }
]);

const statusOptions = computed(() => [
  { label: t("status.pending"), value: "pending" },
  { label: t("status.inquiry"), value: "inquiry" },
  { label: t("status.quoted"), value: "quoted" },
  { label: t("status.active"), value: "active" },
  { label: t("status.warning"), value: "warning" }
]);

const filterFields = computed<FilterField[]>(() => {
  const status = { key: "status", label: t("filter.status"), type: "select" as const, options: statusOptions.value };

  if (pageKey.value === "impa") {
    return [];
  }

  if (pageKey.value === "suppliers") {
    return [
      { key: "supplier", label: t("filter.supplier"), placeholder: t("filter.placeholderSupplier") },
      { key: "port", label: t("filter.port"), type: "select", options: [{ label: "Zhoushan", value: "zhoushan" }, { label: "Ningbo", value: "ningbo" }] },
      { key: "category", label: t("filter.category"), placeholder: t("filter.placeholderKeyword") },
      { key: "qualification", label: t("filter.qualification"), type: "select", options: statusOptions.value }
    ];
  }

  if (pageKey.value === "supplierProducts") {
    return [
      { key: "skuCode", label: t("filter.skuCode"), placeholder: t("filter.placeholderSku") },
      { key: "impaCode", label: t("filter.impaCode"), placeholder: t("filter.placeholderImpa") },
      { key: "supplier", label: t("filter.supplier"), placeholder: t("filter.placeholderSupplier") },
      { key: "mappingStatus", label: t("filter.mappingStatus"), type: "select", options: statusOptions.value },
      { key: "stockStatus", label: t("filter.stockStatus"), type: "select", options: statusOptions.value }
    ];
  }

  if (pageKey.value === "requests") {
    return [
      { key: "requestNo", label: t("filter.requestNo"), placeholder: t("filter.placeholderRequest") },
      { key: "vessel", label: t("filter.vessel"), placeholder: t("filter.placeholderVessel") },
      { key: "port", label: t("filter.port"), type: "select", options: [{ label: "Zhoushan", value: "zhoushan" }, { label: "Ningbo", value: "ningbo" }] },
      status,
      { key: "timeRange", label: t("filter.timeRange"), type: "date" }
    ];
  }

  if (procurementFlowPageKeys.has(pageKey.value)) {
    return [
      { key: "inquiryNo", label: t("filter.inquiryNo"), placeholder: t("filter.placeholderInquiry") },
      { key: "supplier", label: t("filter.supplier"), placeholder: t("filter.placeholderSupplier") },
      status,
      { key: "timeRange", label: t("filter.timeRange"), type: "date" }
    ];
  }

  if (pageKey.value === "settlements") {
    return [
      { key: "keyword", label: t("filter.keyword"), placeholder: t("page.settlements.searchPlaceholder") },
      { key: "type", label: t("table.settlementType"), type: "select", options: [{ label: t("settlement.type.material"), value: "material" }, { label: t("settlement.type.food"), value: "food" }] },
      status,
      { key: "timeRange", label: t("filter.timeRange"), type: "date" }
    ];
  }

  if (pageKey.value === "delivery") {
    return [
      { key: "orderNo", label: t("filter.orderNo"), placeholder: t("filter.placeholderOrder") },
      { key: "vessel", label: t("filter.vessel"), placeholder: t("filter.placeholderVessel") },
      { key: "port", label: t("filter.port"), type: "select", options: [{ label: "Zhoushan", value: "zhoushan" }, { label: "Shanghai", value: "shanghai" }] },
      status,
      { key: "timeRange", label: t("filter.timeRange"), type: "date" }
    ];
  }

  if (pageKey.value === "permissions") {
    return [
      { key: "role", label: t("filter.role"), type: "select", options: ["admin", "purchaser", "supplier", "operator", "finance"].map((role) => ({ label: t(`role.${role}`), value: role })) },
      { key: "permission", label: t("filter.permission"), placeholder: t("filter.placeholderKeyword") },
      status
    ];
  }

  return [];
});

const primaryAction = computed(() => {
  if (pageKey.value === "suppliers" || pageKey.value === "supplierProducts" || pageKey.value === "permissions" || pageKey.value === "companyMembers") {
    return { icon: "Plus" as const, label: t("action.add") };
  }
  if (pageKey.value === "compare") {
    return { icon: "Send" as const, label: t("common.submit") };
  }
  if (pageKey.value === "requests" || pageKey.value === "inquiries") {
    return { icon: "Upload" as const, label: t("action.import") };
  }
  return undefined;
});

const skuColumns = computed<TableColumn[]>(() => [
  { key: "thumbnail", label: t("table.thumbnail"), width: "64px", align: "center" },
  { key: "name", label: t("field.item") },
  { key: "impaCode", label: t("field.impaCode"), width: "116px" },
  { key: "attributes", label: t("table.attrs") },
  { key: "price", label: t("field.price"), width: "112px", align: "right" },
  { key: "stock", label: t("field.stock"), width: "82px", align: "right" },
  { key: "operation", label: t("common.operation"), width: "116px", align: "center" }
]);

const shopSkuColumns = computed<TableColumn[]>(() => [
  { key: "image", label: t("page.supplierProducts.field.image"), width: "86px", align: "center" },
  { key: "category", label: t("page.supplierProducts.field.category"), width: "142px" },
  { key: "platformCode", label: t("page.supplierProducts.field.platformCode"), width: "120px" },
  { key: "productName", label: t("page.supplierProducts.field.productName"), width: "220px" },
  { key: "specs", label: t("page.supplierProducts.field.specs"), width: "92px", align: "center" },
  { key: "stock", label: t("page.supplierProducts.field.stock"), width: "86px", align: "right" },
  { key: "leadTime", label: t("page.supplierProducts.field.leadTime"), width: "96px" },
  { key: "deliveryArea", label: t("page.supplierProducts.field.deliveryArea"), width: "150px" },
  { key: "price", label: t("page.supplierProducts.field.unitPrice"), width: "136px", align: "right" },
  { key: "brand", label: t("page.supplierProducts.field.brand"), width: "108px" },
  { key: "unit", label: t("page.supplierProducts.field.unit"), width: "82px" },
  { key: "packing", label: t("page.supplierProducts.field.packing"), width: "118px" },
  { key: "operation", label: t("common.operation"), width: "150px", align: "center" }
]);

const companyQualificationColumns = computed<TableColumn[]>(() => [
  { key: "type", label: t("page.supplierProducts.qualification.type"), width: "140px" },
  { key: "name", label: t("page.supplierProducts.qualification.name"), width: "180px" },
  { key: "image", label: t("page.supplierProducts.qualification.image"), width: "96px", align: "center" },
  { key: "updatedAt", label: t("page.supplierProducts.qualification.updatedAt"), width: "140px" },
  { key: "operation", label: t("common.operation"), width: "124px", align: "center" }
]);

const isPlainRecord = (value: unknown): value is Record<string, unknown> => Boolean(value) && typeof value === "object" && !Array.isArray(value);

const unwrapShopPayload = (payload: unknown): unknown => {
  if (!isPlainRecord(payload)) return payload;
  if ("data" in payload) return payload.data;
  if ("result" in payload) return payload.result;
  return payload;
};

const readShopString = (source: Record<string, unknown>, key: string): string => {
  const value = source[key];
  if (typeof value === "string") return value;
  if (typeof value === "number") return String(value);
  return "";
};

const readShopNumber = (source: Record<string, unknown>, key: string): number => {
  const value = source[key];
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value);
  return 0;
};

const readShopStringArray = (source: Record<string, unknown>, key: string): string[] => {
  const value = source[key];
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : [];
};

const normalizeShopImageUrl = (value: string) => {
  const text = value.trim();
  if (!text) return "";
  if (/^(https?:|data:|blob:)/i.test(text)) return text;
  return text.startsWith("/") ? text : `/${text}`;
};

const buildFileUrlFromId = (fileId: string | number) => `/api/files/${encodeURIComponent(String(fileId))}`;

const revokeProtectedImageObjectUrls = () => {
  protectedImageObjectUrls.forEach((url) => URL.revokeObjectURL(url));
  protectedImageObjectUrls.clear();
  authenticatedImageUrlCache.clear();
};

const resolveAuthenticatedImageUrl = async (value: string) => {
  const url = normalizeShopImageUrl(value);
  if (!url || /^(data:|blob:)/i.test(url)) return url;
  if (!url.startsWith("/api/files/")) return url;
  const cachedUrl = authenticatedImageUrlCache.get(url);
  if (cachedUrl) return cachedUrl;
  const session = getAuthSession();
  if (!session?.token) return url;

  try {
    const response = await fetch(url, {
      headers: {
        Authorization: `Bearer ${session.token}`
      }
    });
    if (!response.ok) return url;
    const objectUrl = URL.createObjectURL(await response.blob());
    protectedImageObjectUrls.add(objectUrl);
    authenticatedImageUrlCache.set(url, objectUrl);
    return objectUrl;
  } catch {
    return url;
  }
};

const readShopImageUrl = (source: Record<string, unknown>) => {
  const direct = readShopString(source, "thumbnailUrl") || readShopString(source, "imageUrl") || readShopString(source, "fileUrl");
  if (direct) return normalizeShopImageUrl(direct);
  const imageFileId = readShopString(source, "imageFileId") || readShopString(source, "fileId");
  if (imageFileId) return buildFileUrlFromId(imageFileId);

  const images = Array.isArray(source.images) ? source.images.filter(isPlainRecord) : [];
  const primary = images.find((item) => item.primary === true) || images[0];
  if (!primary) return "";

  const nested = readShopString(primary, "thumbnailUrl") || readShopString(primary, "imageUrl");
  return nested ? normalizeShopImageUrl(nested) : "";
};

const readShopSkuImages = (source: Record<string, unknown>): ShopSkuImageData[] => {
  const images = Array.isArray(source.images) ? source.images.filter(isPlainRecord) : [];
  const normalized = images
    .map((item, index) => {
      const fileId = readShopString(item, "fileId") || readShopString(item, "imageFileId");
      const imageUrl = normalizeShopImageUrl(readShopString(item, "imageUrl") || readShopString(item, "url") || (fileId ? buildFileUrlFromId(fileId) : ""));
      const thumbnailUrl = normalizeShopImageUrl(readShopString(item, "thumbnailUrl") || imageUrl);
      if (!fileId && !imageUrl && !thumbnailUrl) return null;
      return {
        fileId,
        imageUrl,
        thumbnailUrl,
        primary: item.primary === true || index === 0,
        sortOrder: readShopNumber(item, "sortOrder") || index
      };
    })
    .filter((item): item is ShopSkuImageData => Boolean(item));

  if (normalized.length) return normalized;

  const fileId = readShopString(source, "imageFileId") || readShopString(source, "fileId");
  const imageUrl = normalizeShopImageUrl(readShopString(source, "imageUrl") || readShopString(source, "thumbnailUrl") || (fileId ? buildFileUrlFromId(fileId) : ""));
  const thumbnailUrl = normalizeShopImageUrl(readShopString(source, "thumbnailUrl") || imageUrl);
  return fileId || imageUrl || thumbnailUrl
    ? [{ fileId, imageUrl, thumbnailUrl, primary: true, sortOrder: 0 }]
    : [];
};

const readShopItems = (payload: unknown): unknown[] => {
  const unwrapped = unwrapShopPayload(payload);
  if (Array.isArray(unwrapped)) return unwrapped;
  if (!isPlainRecord(unwrapped)) return [];
  const items = unwrapped.items;
  return Array.isArray(items) ? items : [];
};

const readShopRawColumn = (source: Record<string, unknown>, candidates: string[]) => {
  const rawColumns = isPlainRecord(source.rawColumns) ? source.rawColumns : {};
  const entries = Object.entries(rawColumns);
  for (const candidate of candidates) {
    const normalizedCandidate = candidate.toLowerCase().replace(/[\s._\-:/\\()（）&]+/g, "");
    const found = entries.find(([key]) => key.toLowerCase().replace(/[\s._\-:/\\()（）&]+/g, "") === normalizedCandidate);
    if (found) return typeof found[1] === "string" || typeof found[1] === "number" ? String(found[1]).trim() : "";
  }
  return "";
};

const readShopImportRawName = (source: Record<string, unknown>) =>
  readShopString(source, "rawName") ||
  readShopString(source, "originalName") ||
  readShopString(source, "rawNameSpec") ||
  readShopString(source, "description") ||
  readShopRawColumn(source, ["Name of Commodity & Specification", "Name of Commodity Specification", "DESCRIPTION", "Description"]);

const normalizeShopNameForCompare = (value: string) => value.toLowerCase().replace(/\s+/g, " ").trim();

const looksLikeRawShopDescription = (value: string) =>
  /[\r\n]/.test(value) ||
  /\b(material|packing|barcode|stock|finish|handle|specification)\s*[:：]/i.test(value) ||
  value.length > 120;

const readBackendCleanShopProductName = (source: Record<string, unknown>) => {
  const productName = readShopString(source, "productName");
  if (productName) {
    const rawName = readShopImportRawName(source);
    if ((!rawName || normalizeShopNameForCompare(productName) !== normalizeShopNameForCompare(rawName)) && !looksLikeRawShopDescription(productName)) {
      return productName;
    }
  }

  const cleanName = readShopString(source, "cleanName");
  return cleanName && !looksLikeRawShopDescription(cleanName) ? cleanName : "";
};

const createShopSpecItem = (value: Partial<ShopEditableSpec> = {}): ShopEditableSpec => ({
  id: value.id || `spec-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  key: value.key || value.name || "specification",
  name: value.name || value.key || t("page.supplierProducts.field.specs"),
  value: value.value || "",
  unit: value.unit || ""
});

const formatShopSpecItem = (item: ShopEditableSpec) => {
  const name = item.name.trim();
  const value = item.value.trim();
  if (!name && !value) return "";
  if (!name) return value;
  return [name, value].filter(Boolean).join(": ");
};

const normalizeShopSpecItems = (source: Record<string, unknown>) => {
  const specifications = Array.isArray(source.specifications) ? source.specifications : [];
  const items = specifications
    .map((item, index) => {
      if (!isPlainRecord(item)) return null;
      const key = readShopString(item, "key") || readShopString(item, "name") || `specification_${index + 1}`;
      const name = readShopString(item, "name") || key;
      const value = readShopString(item, "value");
      const unit = readShopString(item, "unit");
      if (!name && !value && !unit) return null;
      return createShopSpecItem({ id: `spec-${index}-${key}`, key, name, value, unit });
    })
    .filter((item): item is ShopEditableSpec => Boolean(item));

  const parsedAttributes = isPlainRecord(source.parsedAttributes) ? source.parsedAttributes : {};
  Object.entries(parsedAttributes).forEach(([key, value]) => {
    if (items.some((item) => item.key === key)) return;
    if (typeof value !== "string" && typeof value !== "number") return;
    const text = String(value).trim();
    if (!text) return;
    items.push(createShopSpecItem({ key, name: key, value: text }));
  });

  const attributeSummary = readShopString(source, "attributeSummary");
  const specificationSummary = readShopString(source, "specificationSummary");
  const summary = attributeSummary || specificationSummary;
  if (!items.length && summary) {
    items.push(createShopSpecItem({ key: "summary", name: t("page.supplierProducts.field.specs"), value: summary }));
  }
  return items;
};

const normalizeShopCategoryCandidates = (source: Record<string, unknown>): ShopCategoryCandidate[] => {
  const candidates = Array.isArray(source.categoryCandidates) ? source.categoryCandidates : [];
  return candidates
    .filter(isPlainRecord)
    .map((item) => ({
      categoryCode: readShopString(item, "categoryCode"),
      categoryName: readShopString(item, "categoryName"),
      segmentCode: readShopString(item, "segmentCode"),
      segmentName: readShopString(item, "segmentName"),
      matchedKeyword: readShopString(item, "matchedKeyword"),
      reason: readShopString(item, "reason"),
      itemCount: readShopNumber(item, "itemCount")
    }))
    .filter((item) => item.categoryCode || item.categoryName);
};

const normalizeShopExistingSnapshot = (value: unknown): ShopExistingSnapshot | undefined => {
  if (!isPlainRecord(value)) return undefined;
  const skuId = value.skuId ?? value.id;
  const images = readShopSkuImages(value);
  const imageUrl = readShopImageUrl(value) || images[0]?.thumbnailUrl || images[0]?.imageUrl || "";
  const categoryCode = readShopString(value, "categoryCode");
  const categoryName = readShopString(value, "categoryName");
  return {
    skuId: typeof skuId === "string" || typeof skuId === "number" ? skuId : undefined,
    supplierSkuCode: readShopString(value, "supplierSkuCode"),
    productName: readShopString(value, "productName") || readShopString(value, "cleanName"),
    productType: readShopString(value, "productType"),
    category: categoryName || categoryCode,
    categoryCode,
    categoryName,
    platformCode: readShopString(value, "platformCode"),
    impaCode: readShopString(value, "impaCode"),
    stock: readShopNumber(value, "stockQty"),
    leadTimeDays: readShopNumber(value, "leadTimeDays"),
    deliveryArea: readShopString(value, "deliveryArea"),
    imageUrl,
    images,
    monthlySales: readShopNumber(value, "monthlySales"),
    price: readShopNumber(value, "unitPrice"),
    currency: readShopString(value, "currency") || "CNY",
    brand: readShopString(value, "brand"),
    unit: readShopString(value, "unit") || readShopString(value, "stockUnit"),
    packing: readShopString(value, "packageSpec"),
    listingStatus: readShopString(value, "shelfStatus")
  };
};

const syncShopRowSpecs = (row: ShopSkuRow) => {
  row.specs = row.specItems.map(formatShopSpecItem).filter(Boolean);
};

const normalizeEnterpriseNameForDisplay = (value: string) => {
  if (language.value !== "zh-CN") return value;
  return value.replace(/\s*(shop|store|\u5e97\u94fa)$/i, "").trim();
};

const normalizeShopProfile = (payload: unknown): ShopProfileForm => {
  const unwrapped = unwrapShopPayload(payload);
  const source = isPlainRecord(unwrapped) ? unwrapped : {};
  const logoFileId = readShopString(source, "logoFileId");
  const logoUrl = readShopString(source, "logoUrl") || (logoFileId ? buildFileUrlFromId(logoFileId) : "");
  return {
    shopName: normalizeEnterpriseNameForDisplay(
      readShopString(source, "companyName") || readShopString(source, "shopName") || readShopString(source, "enterpriseName")
    ),
    creditCode:
      readShopString(source, "unifiedSocialCreditCode") ||
      readShopString(source, "socialCreditCode") ||
      readShopString(source, "creditCode") ||
      readShopString(source, "businessLicenseNo"),
    logoFileId,
    logoUrl: logoUrl ? normalizeShopImageUrl(logoUrl) : "",
    description: readShopString(source, "description"),
    mainCategories: readShopStringArray(source, "mainCategories"),
    servicePorts: readShopStringArray(source, "servicePorts"),
    deliveryAreas: readShopStringArray(source, "deliveryAreas"),
    contactName: readShopString(source, "contactName"),
    contactPhone: readShopString(source, "contactPhone"),
    contactEmail: readShopString(source, "contactEmail"),
    companyType: readShopString(source, "companyType"),
    status: readShopString(source, "status") || "ACTIVE"
  };
};

const normalizeShopSku = (value: unknown, options: { preview?: boolean; batchId?: string | number } = {}): ShopSkuRow | null => {
  if (!isPlainRecord(value)) return null;
  const skuId = value.skuId ?? value.id;
  const existingSnapshot = normalizeShopExistingSnapshot(value.existingSnapshot);
  const existingSkuId = value.existingSkuId ?? existingSnapshot?.skuId;
  const previewActionText = readShopString(value, "previewAction").toUpperCase();
  const previewAction = ["INSERT", "UPDATE", "BLOCKED", "DUPLICATE"].includes(previewActionText) ? previewActionText as ShopPreviewAction : "";
  const previewRowId = value.previewRowId ?? value.rowId;
  const specItems = normalizeShopSpecItems(value);
  const specs = specItems.map(formatShopSpecItem).filter(Boolean);
  const images = readShopSkuImages(value);
  const imageUrl = readShopImageUrl(value) || images[0]?.thumbnailUrl || images[0]?.imageUrl || "";
  const imageFileId = readShopString(value, "imageFileId") || readShopString(value, "fileId") || images[0]?.fileId || "";
  const rowNo = readShopNumber(value, "importRowNumber") || readShopNumber(value, "rowNo");
  const rowId = options.preview
    ? `preview-${options.batchId || "batch"}-${previewRowId || rowNo || readShopString(value, "supplierSkuCode") || Math.random()}`
    : String(skuId || readShopString(value, "supplierSkuCode") || readShopString(value, "platformCode") || Math.random());
  const categoryCode = readShopString(value, "categoryCode");
  const categoryName = readShopString(value, "categoryName");
  const leadTimeDays = readShopNumber(value, "leadTimeDays");
  const categoryCandidates = normalizeShopCategoryCandidates(value);

  return {
    id: rowId,
    skuId: typeof skuId === "string" || typeof skuId === "number" ? skuId : undefined,
    existingSkuId: typeof existingSkuId === "string" || typeof existingSkuId === "number" ? existingSkuId : undefined,
    previewRowId: typeof previewRowId === "string" || typeof previewRowId === "number" ? previewRowId : undefined,
    isPreview: options.preview,
    previewAction,
    existingSnapshot,
    productType: readShopString(value, "productType") || "MATERIAL",
    category: categoryName || categoryCode,
    categoryCode,
    categoryName,
    platformCode: readShopString(value, "platformCode"),
    impaCode: readShopString(value, "impaCode"),
    supplierSkuCode: readShopString(value, "supplierSkuCode"),
    productName:
      readBackendCleanShopProductName(value) ||
      t("page.supplierProducts.waitingManual"),
    specs,
    specItems,
    stock: readShopNumber(value, "stockQty"),
    leadTime: leadTimeDays ? t("page.supplierProducts.leadTimeDays", { days: leadTimeDays }) : "",
    leadTimeDays,
    deliveryArea: readShopString(value, "deliveryArea"),
    thumbnail: imageUrl,
    imageFileId,
    imageUrl,
    images,
    monthlySales: readShopNumber(value, "monthlySales"),
    price: readShopNumber(value, "unitPrice"),
    currency: readShopString(value, "currency") || "CNY",
    brand: readShopString(value, "brand"),
    unit: readShopString(value, "unit") || readShopString(value, "stockUnit"),
    packing: readShopString(value, "packageSpec"),
    barcode: readShopString(value, "barcode"),
    listingStatus: previewAction === "UPDATE" ? existingSnapshot?.listingStatus || readShopString(value, "shelfStatus") || "OFF_SHELF" : readShopString(value, "shelfStatus") || "OFF_SHELF",
    codingStatus: readShopString(value, "codeStatus"),
    exceptionReason: readShopString(value, "exceptionReason"),
    importBatch: options.batchId || readShopString(value, "importBatchId") || readShopNumber(value, "importBatchId"),
    importRowNo: rowNo,
    categoryCandidates
  };
};

const hydrateShopSkuImages = async (rows: ShopSkuRow[]) =>
  Promise.all(
    rows.map(async (row) => ({
      ...row,
      thumbnail: await resolveAuthenticatedImageUrl(row.thumbnail)
    }))
  );

const normalizeCompanyQualification = (value: unknown): CompanyQualificationRow | null => {
  if (!isPlainRecord(value)) return null;
  const qualificationId = value.qualificationId ?? value.id;
  const fileId = readShopString(value, "fileId");
  const fileName = readShopString(value, "fileName");
  const fileUrl = readShopString(value, "fileUrl") || readShopString(value, "imageUrl") || readShopString(value, "url") || (fileId ? buildFileUrlFromId(fileId) : "");
  const title = readShopString(value, "title");
  const fallbackName = fileName || fileId || "-";
  const rowId = String(qualificationId || readShopString(value, "fileId") || title || fileName || Math.random());
  const typeCode = readShopString(value, "qualificationType") || "GENERAL";
  return {
    id: rowId,
    qualificationId: typeof qualificationId === "string" || typeof qualificationId === "number" ? qualificationId : undefined,
    fileId,
    fileName,
    fileUrl: fileUrl ? normalizeShopImageUrl(fileUrl) : "",
    imagePreviewUrl: "",
    name: title || fallbackName,
    type: typeCode === "GENERAL" ? t("page.supplierProducts.qualification.defaultType") : typeCode,
    typeCode,
    status: readShopString(value, "status") || "-",
    description: readShopString(value, "description"),
    uploadedAt: readShopString(value, "uploadedAt"),
    updatedAt: readShopString(value, "updatedAt") || readShopString(value, "uploadedAt")
  };
};

const filteredShopProductRows = computed(() => {
  const keyword = shopProductKeyword.value.trim().toLowerCase();
  return shopSkuRows.value.filter((row) => {
    const keywordMatched =
      !keyword ||
      [row.platformCode, row.impaCode, row.supplierSkuCode, row.productName, row.brand, row.barcode, row.exceptionReason, ...row.specs]
        .join(" ")
        .toLowerCase()
        .includes(keyword);
    const typeMatched = !shopProductTypeFilter.value || row.productType === shopProductTypeFilter.value;
    const codeStatusMatched =
      !shopProductCodeStatusFilter.value ||
      getShopCodeStatusFilterValues(shopProductCodeStatusFilter.value).includes(row.codingStatus);
    const shelfStatusMatched = !shopProductShelfStatusFilter.value || row.listingStatus === shopProductShelfStatusFilter.value;
    return keywordMatched && typeMatched && codeStatusMatched && shelfStatusMatched;
  });
});

const SHOP_MATCHED_CODE_STATUSES = ["CODE_MATCHED", "SPEC_MATCHED", "MATCHED", "CODE_MATCH", "SPEC_MATCH", "SPECIFICATION_MATCH"];

const isShopMatchedCodeStatus = (status: string) => SHOP_MATCHED_CODE_STATUSES.includes(status);

const shopExceptionRows = computed(() => shopSkuRows.value.filter((row) => isShopManualReviewStatus(row.codingStatus)));
const shopBlockedPreviewRows = computed(() =>
  shopPreviewSkuRows.value.filter((row) => row.previewAction === "BLOCKED" || row.previewAction === "DUPLICATE")
);

const shopMetrics = computed(() => [
  { label: t("page.supplierProducts.storeSkuCount"), value: String(hasPendingShopImport.value ? shopPreviewSkuRows.value.length : shopSkuTotal.value || shopSkuRows.value.length) },
  { label: t("page.supplierProducts.storeExceptionCount"), value: String(shopExceptionRows.value.length) },
  { label: t("page.supplierProducts.storeScope"), value: shopProfileForm.value.servicePorts.join(" / ") || t("common.notFilled") }
]);

const hasPendingShopImport = computed(() => Boolean(shopImportPreview.value?.batchId && shopPreviewSkuRows.value.length));
const hasDirtyShopSkuRows = computed(() => shopDirtySkuIds.value.size > 0);
const unsavedShopSkuRows = computed(() => shopSkuRows.value.filter((row) => !row.skuId));
const savedShopSkuRows = computed(() => shopSkuRows.value.filter((row) => row.skuId && !row.isPreview));
const hasSavedShopSkuRows = computed(() => savedShopSkuRows.value.length > 0);
const canConfirmShopImport = computed(() => shopPreviewSkuRows.value.length > 0 || unsavedShopSkuRows.value.length > 0 || hasDirtyShopSkuRows.value);
const shopSkuTotalPages = computed(() => Math.max(1, Math.ceil(shopSkuTotal.value / shopSkuPageSize)));
const shopSkuPageStart = computed(() => (shopSkuTotal.value ? (shopSkuPage.value - 1) * shopSkuPageSize + 1 : 0));
const shopSkuPageEnd = computed(() => Math.min(shopSkuPage.value * shopSkuPageSize, shopSkuTotal.value));

const markShopSkuDirty = (row: ShopSkuRow) => {
  shopDirtySkuIds.value = new Set(shopDirtySkuIds.value).add(row.id);
};

const getShopCurrencySymbol = (currency: string) => {
  const normalized = currency.trim().toUpperCase();
  if (normalized === "USD") return "$";
  return "¥";
};

const getShopCodeStatusFilterValues = (status: string) => {
  if (status === "MATCHED_SUCCESS") return SHOP_MATCHED_CODE_STATUSES;
  return [status];
};

const getShopCategoryOptions = computed(() => {
  const options = impaCategories.value.map((category) => ({
    value: category.code,
    label: language.value === "zh-CN" ? category.nameCn || category.nameEn || category.code : category.nameEn || category.nameCn || category.code
  }));
  const existing = shopSkuRows.value
    .map((row) => ({
      value: row.categoryCode || row.category,
      label: row.categoryName || row.categoryCode || row.category
    }))
    .filter((item) => item.value && !options.some((option) => option.value === item.value));
  return [...options, ...existing];
});

const getShopCategoryOptionsForRow = (row: ShopSkuRow) => {
  const options = [...getShopCategoryOptions.value];
  row.categoryCandidates.forEach((candidate) => {
    const value = candidate.categoryCode || candidate.categoryName;
    if (!value || options.some((option) => option.value === value)) return;
    const suffix = candidate.segmentName || candidate.segmentCode || candidate.matchedKeyword;
    options.push({
      value,
      label: [candidate.categoryName || value, suffix].filter(Boolean).join(" / ")
    });
  });
  return options;
};

const updateShopRowCategory = (row: ShopSkuRow, categoryCode: string) => {
  const option = getShopCategoryOptionsForRow(row).find((item) => item.value === categoryCode);
  row.categoryCode = categoryCode;
  row.categoryName = option?.label || row.categoryName || categoryCode;
  row.category = option?.label || categoryCode;
  markShopSkuDirty(row);
};

const getShopSpecsSummary = (row: ShopSkuRow) =>
  row.specItems.length ? t("page.supplierProducts.specCount", { count: row.specItems.length }) : t("page.supplierProducts.noSpecs");

const addShopSpec = (row: ShopSkuRow) => {
  row.specItems.push(createShopSpecItem());
  syncShopRowSpecs(row);
  markShopSkuDirty(row);
};

const removeShopSpec = (row: ShopSkuRow, index: number) => {
  row.specItems.splice(index, 1);
  syncShopRowSpecs(row);
  markShopSkuDirty(row);
};

const updateShopSpec = (row: ShopSkuRow) => {
  syncShopRowSpecs(row);
  markShopSkuDirty(row);
};

const buildShopSkuImagePayload = (row: ShopSkuRow) => {
  const images = row.images.length
    ? row.images
    : row.imageFileId || row.imageUrl
      ? [{ fileId: row.imageFileId, imageUrl: row.imageUrl, thumbnailUrl: row.imageUrl, primary: true, sortOrder: 0 }]
      : [];
  return images
    .map((image, index) => ({
      fileId: image.fileId || "",
      imageUrl: normalizeShopImageUrl(image.imageUrl || image.thumbnailUrl || (image.fileId ? buildFileUrlFromId(image.fileId) : "")),
      thumbnailUrl: normalizeShopImageUrl(image.thumbnailUrl || image.imageUrl || (image.fileId ? buildFileUrlFromId(image.fileId) : "")),
      primary: image.primary || index === 0,
      sortOrder: image.sortOrder ?? index
    }))
    .filter((image) => image.fileId || image.imageUrl || image.thumbnailUrl);
};

const buildShopSkuSavePayload = (row: ShopSkuRow) => ({
  productType: row.productType || "MATERIAL",
  categoryCode: row.categoryCode || row.category,
  categoryName: row.categoryName || row.category,
  platformCode: row.platformCode,
  impaCode: row.impaCode || row.platformCode,
  supplierSkuCode: row.supplierSkuCode,
  productName: row.productName,
  specifications: row.specItems
    .map((item) => ({
      key: item.key || item.name || "specification",
      name: item.name || item.key || t("page.supplierProducts.field.specs"),
      value: item.value,
      unit: null
    }))
    .filter((item) => item.name || item.value || item.unit),
  attributeSummary: row.specs.join(" / "),
  stockQty: row.stock,
  stockUnit: row.unit,
  leadTimeDays: row.leadTimeDays,
  deliveryArea: row.deliveryArea,
  imageFileId: row.imageFileId,
  imageUrl: row.imageUrl,
  thumbnailUrl: buildShopSkuImagePayload(row)[0]?.thumbnailUrl || row.imageUrl,
  monthlySales: row.monthlySales,
  unitPrice: row.price,
  currency: row.currency || "CNY",
  brand: row.brand,
  unit: row.unit,
  packageSpec: row.packing,
  barcode: row.barcode,
  shelfStatus: row.listingStatus,
  codeStatus: row.codingStatus,
  exceptionReason: row.exceptionReason,
  images: buildShopSkuImagePayload(row),
  importRowNumber: row.importRowNo,
  importBatchId: row.importBatch || shopImportPreview.value?.batchId || undefined,
  previewRowId: row.previewRowId
});

const getShopPreviewActionLabel = (action: ShopPreviewAction) => {
  const keys: Record<Exclude<ShopPreviewAction, "">, string> = {
    INSERT: "page.supplierProducts.previewActionInsert",
    UPDATE: "page.supplierProducts.previewActionUpdate",
    BLOCKED: "page.supplierProducts.previewActionBlocked",
    DUPLICATE: "page.supplierProducts.previewActionDuplicate"
  };
  return action ? t(keys[action]) : "";
};

const getShopPreviewActionHint = (row: ShopSkuRow) => {
  if (row.previewAction === "UPDATE") return t("page.supplierProducts.previewActionUpdateHint");
  if (row.previewAction === "BLOCKED") return t("page.supplierProducts.previewActionBlockedHint");
  if (row.previewAction === "DUPLICATE") return t("page.supplierProducts.previewActionDuplicateHint");
  return "";
};

const formatShopDisplayValue = (value: unknown) => {
  if (value === undefined || value === null || value === "") return "-";
  if (typeof value === "number") return Number.isFinite(value) ? String(value) : "-";
  if (typeof value === "string") return value.trim() || "-";
  return String(value);
};

const formatShopMoneyValue = (price: number, currency: string) => {
  if (!price) return "-";
  return `${getShopCurrencySymbol(currency)} ${price}`;
};

const getShopExistingChangeRows = (row: ShopSkuRow) => {
  const snapshot = row.existingSnapshot;
  if (!snapshot) return [];
  return [
    { key: "productName", label: t("page.supplierProducts.field.productName"), oldValue: snapshot.productName, newValue: row.productName },
    { key: "category", label: t("page.supplierProducts.field.category"), oldValue: snapshot.categoryName || snapshot.categoryCode, newValue: row.categoryName || row.categoryCode },
    { key: "platformCode", label: t("page.supplierProducts.field.platformCode"), oldValue: snapshot.platformCode || snapshot.impaCode, newValue: row.platformCode || row.impaCode },
    { key: "stock", label: t("page.supplierProducts.field.stock"), oldValue: snapshot.stock, newValue: row.stock },
    { key: "leadTime", label: t("page.supplierProducts.field.leadTime"), oldValue: snapshot.leadTimeDays ? t("page.supplierProducts.leadTimeDays", { days: snapshot.leadTimeDays }) : "", newValue: row.leadTimeDays ? t("page.supplierProducts.leadTimeDays", { days: row.leadTimeDays }) : "" },
    { key: "deliveryArea", label: t("page.supplierProducts.field.deliveryArea"), oldValue: snapshot.deliveryArea, newValue: row.deliveryArea },
    { key: "price", label: t("page.supplierProducts.field.unitPrice"), oldValue: formatShopMoneyValue(snapshot.price, snapshot.currency), newValue: formatShopMoneyValue(row.price, row.currency) },
    { key: "brand", label: t("page.supplierProducts.field.brand"), oldValue: snapshot.brand, newValue: row.brand },
    { key: "unit", label: t("page.supplierProducts.field.unit"), oldValue: snapshot.unit, newValue: row.unit },
    { key: "packing", label: t("page.supplierProducts.field.packing"), oldValue: snapshot.packing, newValue: row.packing },
    { key: "shelfStatus", label: t("page.supplierProducts.field.listingStatus"), oldValue: getShopShelfStatusLabel(snapshot.listingStatus), newValue: getShopShelfStatusLabel(row.listingStatus) }
  ].filter((item) => formatShopDisplayValue(item.oldValue) !== formatShopDisplayValue(item.newValue));
};

const shopImportStages = computed(() => [
  t("page.supplierProducts.importStageUpload"),
  t("page.supplierProducts.importStagePreview"),
  t("page.supplierProducts.importStageImpa"),
  t("page.supplierProducts.importStageException")
]);

const shopImportProgressStyle = computed(() => ({ "--progress": `${shopImportProgress.value}%` }));

const shopCompanyName = computed(() => {
  const session = getAuthSession();
  const companyName = session?.company?.companyName || session?.company?.name || "";
  return normalizeEnterpriseNameForDisplay(companyName.trim()) || t("page.supplierProducts.storeFallbackCompany");
});

const shopCreditCode = computed(() => {
  const session = getAuthSession();
  const companyRecord = (session?.company || {}) as Record<string, unknown>;
  return (
    shopProfileForm.value.creditCode ||
    readShopString(companyRecord, "unifiedSocialCreditCode") ||
    readShopString(companyRecord, "socialCreditCode") ||
    readShopString(companyRecord, "creditCode") ||
    readShopString(companyRecord, "businessLicenseNo")
  );
});

const shopStoreName = computed(() => normalizeEnterpriseNameForDisplay(shopProfileForm.value.shopName) || shopCompanyName.value);

const openCompanyQualificationCreate = () => {
  selectedCompanyQualificationRow.value = null;
  openCompanyQualificationFilePicker();
  shopNoticeKey.value = "";
};

const openCompanyQualificationEdit = (row: CompanyQualificationRow) => {
  selectedCompanyQualificationRow.value = row;
  openCompanyQualificationFilePicker();
  shopNoticeKey.value = "";
};

const shopCodingVariant = (status: ShopSkuRow["codingStatus"]): StatusVariant => {
  if (isShopMatchedCodeStatus(status)) return "success";
  if (status === "PENDING_EXCEPTION") return "warning";
  if (status === "EXCEPTION") return "danger";
  return "neutral";
};

const isShopManualReviewStatus = (status: string) =>
  ["PENDING_EXCEPTION", "MANUAL_REQUIRED", "MANUAL_REVIEW", "PENDING_MANUAL", "REVIEW_REQUIRED", "EXCEPTION"].includes(status);

const getShopSkuRowClass = (row: ShopSkuRow) => {
  const needsManualReview = isShopManualReviewStatus(row.codingStatus);
  return {
    "is-manual-review": needsManualReview,
    "is-preview-update": row.previewAction === "UPDATE" && !needsManualReview,
    "is-preview-blocked": row.previewAction === "BLOCKED" || row.previewAction === "DUPLICATE"
  };
};

const shopShelfVariant = (status: string): StatusVariant => (status === "ON_SHELF" ? "success" : status === "OFF_SHELF" ? "neutral" : "info");

const getShopCodeStatusLabel = (status: string) => {
  const keyByStatus: Record<string, string> = {
    CODE_MATCHED: "page.supplierProducts.codeMatched",
    MATCHED: "page.supplierProducts.codeMatched",
    CODE_MATCH: "page.supplierProducts.codeMatched",
    SPEC_MATCH: "page.supplierProducts.codeMatched",
    SPEC_MATCHED: "page.supplierProducts.codeMatched",
    SPECIFICATION_MATCH: "page.supplierProducts.codeMatched",
    PENDING_EXCEPTION: "page.supplierProducts.codePending",
    EXCEPTION: "page.supplierProducts.codeException"
  };
  return status ? t(keyByStatus[status] || "page.supplierProducts.codeUnknown") : "-";
};

const getShopShelfStatusLabel = (status: string) => {
  if (status === "ON_SHELF") return t("page.supplierProducts.statusOnShelf");
  if (status === "OFF_SHELF") return t("page.supplierProducts.statusOffShelf");
  return status || "-";
};

const showShopPendingNotice = () => {
  shopNoticeKey.value = "page.supplierProducts.actionNeedsForm";
};

const getShopErrorText = (error: unknown, fallbackKey = "page.supplierProducts.requestFailed") => {
  if (error instanceof ApiError && typeof error.message === "string" && error.message.trim()) return error.message;
  return t(fallbackKey);
};

const buildShopSkuQuery = (page = shopSkuPage.value) => ({
  productType: shopProductTypeFilter.value,
  codeStatus: "",
  shelfStatus: shopProductShelfStatusFilter.value,
  keyword: shopProductKeyword.value.trim(),
  page,
  size: shopSkuPageSize
});

const getShopSkuPageCacheKey = (query: ReturnType<typeof buildShopSkuQuery>) => JSON.stringify(query);

const applyShopSkuPageCacheEntry = (entry: ShopSkuPageCacheEntry) => {
  shopSkuTotal.value = entry.total;
  shopSkuPage.value = entry.page;
  shopPersistedSkuRows.value = entry.rows;
  shopSkuRows.value = shopPreviewSkuRows.value.length ? shopPreviewSkuRows.value : shopPersistedSkuRows.value;
  shopDirtySkuIds.value = new Set([...shopDirtySkuIds.value].filter((id) => shopPreviewSkuRows.value.some((row) => row.id === id)));
};

const fetchShopSkuPage = async (query: ReturnType<typeof buildShopSkuQuery>): Promise<ShopSkuPageCacheEntry> => {
  const payload = await listShopSkus(query);
  const unwrapped = unwrapShopPayload(payload);
  const source = isPlainRecord(unwrapped) ? unwrapped : {};
  const rows = readShopItems(payload).map((item) => normalizeShopSku(item)).filter((item): item is ShopSkuRow => Boolean(item));
  const total = readShopNumber(source, "total") || rows.length;
  const responsePage = readShopNumber(source, "page") || query.page;
  return {
    page: responsePage,
    total,
    rows: await hydrateShopSkuImages(rows)
  };
};

const fetchAllSavedShopSkuRows = async (): Promise<ShopSkuRow[]> => {
  const rows: ShopSkuRow[] = [];
  const size = 100;
  let page = 1;
  let total = shopSkuTotal.value || 0;

  do {
    const payload = await listShopSkus({
      ...buildShopSkuQuery(page),
      page,
      size
    });
    const unwrapped = unwrapShopPayload(payload);
    const source = isPlainRecord(unwrapped) ? unwrapped : {};
    const items = readShopItems(payload)
      .map((item) => normalizeShopSku(item))
      .filter((item): item is ShopSkuRow => Boolean(item?.skuId && !item.isPreview));
    rows.push(...items);
    total = readShopNumber(source, "total") || total || rows.length;
    if (!items.length || rows.length >= total) break;
    page += 1;
  } while (page <= Math.ceil(Math.max(total, rows.length) / size) + 1);

  return rows;
};

const runShopShelfUpdates = async (rows: ShopSkuRow[], shelfStatus: "ON_SHELF" | "OFF_SHELF") => {
  const chunkSize = 8;
  for (let start = 0; start < rows.length; start += chunkSize) {
    await Promise.all(
      rows.slice(start, start + chunkSize).map((row) =>
        updateShopSkuShelfStatus(row.skuId as string | number, {
          shelfStatus
        })
      )
    );
  }
};

const prefetchShopSkuPage = async (page: number) => {
  if (hasPendingShopImport.value || page < 1 || (shopSkuTotal.value > 0 && page > shopSkuTotalPages.value)) return;
  const query = buildShopSkuQuery(page);
  const cacheKey = getShopSkuPageCacheKey(query);
  if (shopSkuPageCache.has(cacheKey)) return;
  try {
    shopSkuPageCache.set(cacheKey, await fetchShopSkuPage(query));
  } catch {
    // Background prefetch is only an interaction optimization.
  }
};

const loadShopProfile = async () => {
  shopProfileLoading.value = true;
  try {
    const payload = await getCompanyProfile();
    shopProfileForm.value = normalizeShopProfile(payload);
    shopLogoPreview.value = await resolveAuthenticatedImageUrl(shopProfileForm.value.logoUrl);
  } finally {
    shopProfileLoading.value = false;
  }
};

const loadCompanyQualifications = async () => {
  companyQualificationLoading.value = true;
  try {
    const payload = await listCompanyQualifications({ status: "SUBMITTED" });
    const rows = readShopItems(payload).map((item) => normalizeCompanyQualification(item)).filter((item): item is CompanyQualificationRow => Boolean(item));
    companyQualificationRows.value = await Promise.all(
      rows.map(async (row) => ({
        ...row,
        imagePreviewUrl: await resolveAuthenticatedImageUrl(row.fileUrl)
      }))
    );
  } finally {
    companyQualificationLoading.value = false;
  }
};

const loadShopSkuList = async () => {
  const query = buildShopSkuQuery();
  const cacheKey = getShopSkuPageCacheKey(query);
  const cachedPage = shopSkuPageCache.get(cacheKey);
  if (cachedPage) {
    applyShopSkuPageCacheEntry(cachedPage);
    void prefetchShopSkuPage(shopSkuPage.value + 1);
    return;
  }
  shopSkuListLoading.value = true;
  try {
    const entry = await fetchShopSkuPage(query);
    shopSkuPageCache.set(cacheKey, entry);
    applyShopSkuPageCacheEntry(entry);
    if (!entry.rows.length && shopSkuTotal.value > 0 && shopSkuPage.value > shopSkuTotalPages.value) {
      shopSkuPage.value = shopSkuTotalPages.value;
      return loadShopSkuList();
    }
    void prefetchShopSkuPage(shopSkuPage.value + 1);
  } finally {
    shopSkuListLoading.value = false;
  }
};

const loadShopWorkspace = async () => {
  if (pageKey.value !== "supplierProducts") return;
  shopLoading.value = true;
  shopErrorMessage.value = "";
  shopNoticeMessage.value = "";
  shopSkuPageCache.clear();
  try {
    await Promise.all([loadShopProfile(), loadCompanyQualifications(), loadShopSkuList()]);
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error);
    companyQualificationRows.value = [];
    shopPersistedSkuRows.value = [];
    shopPreviewSkuRows.value = [];
    shopSkuRows.value = [];
  } finally {
    shopLoading.value = false;
  }
};

const buildCompanyProfilePayload = (overrides: Record<string, unknown> = {}) => {
  const form = shopProfileForm.value;
  return {
    companyName: form.shopName,
    unifiedSocialCreditCode: form.creditCode,
    logoFileId: form.logoFileId,
    logoUrl: form.logoUrl,
    contactName: form.contactName,
    contactPhone: form.contactPhone,
    contactEmail: form.contactEmail,
    companyType: form.companyType,
    status: form.status || "ACTIVE",
    ...overrides
  };
};

const saveShopProfile = async () => {
  shopSaving.value = true;
  shopErrorMessage.value = "";
  shopNoticeMessage.value = "";
  try {
    await updateCompanyProfile(buildCompanyProfilePayload());
    shopNoticeKey.value = "page.supplierProducts.profileSaved";
    await loadShopProfile();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.profileSaveFailed");
  } finally {
    shopSaving.value = false;
  }
};

const removeCompanyQualification = async (row: CompanyQualificationRow) => {
  if (!row.qualificationId) return;
  if (typeof window !== "undefined" && !window.confirm(t("page.supplierProducts.qualification.deleteConfirm"))) return;

  companyQualificationSaving.value = true;
  shopErrorMessage.value = "";
  try {
    await deleteCompanyQualification(row.qualificationId);
    shopNoticeKey.value = "page.supplierProducts.qualification.deleted";
    await loadCompanyQualifications();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.qualification.deleteFailed");
  } finally {
    companyQualificationSaving.value = false;
  }
};

const searchShopSkus = async () => {
  shopErrorMessage.value = "";
  if (hasPendingShopImport.value) return;
  shopSkuPage.value = 1;
  shopSkuPageCache.clear();
  try {
    await loadShopSkuList();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error);
    shopSkuRows.value = [];
  }
};

const refreshShopSkuList = async () => {
  shopErrorMessage.value = "";
  shopSkuPageCache.clear();
  try {
    await loadShopSkuList();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error);
    shopSkuRows.value = [];
  }
};

const goShopSkuPage = async (page: number) => {
  if (hasPendingShopImport.value || shopSkuListLoading.value) return;
  const nextPage = Math.min(Math.max(1, page), shopSkuTotalPages.value);
  if (nextPage === shopSkuPage.value) return;
  shopSkuPage.value = nextPage;
  shopErrorMessage.value = "";
  try {
    await loadShopSkuList();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error);
    shopSkuRows.value = [];
  }
};

const clearShopImportTimer = () => {
  if (shopImportTimer !== undefined) {
    window.clearTimeout(shopImportTimer);
    shopImportTimer = undefined;
  }
};

const openShopLogoPicker = () => {
  if (shopLogoUploading.value || shopSaving.value) return;
  shopLogoInput.value?.click();
};

const handleShopLogoChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    shopLogoPreview.value = typeof reader.result === "string" ? reader.result : "";
  };
  reader.readAsDataURL(file);

  shopLogoUploading.value = true;
  shopErrorMessage.value = "";
  shopNoticeKey.value = "";
  try {
    const uploaded = await uploadQualificationFile(file);
    const fileId = String(uploaded.fileId || uploaded.id || "");
    const fileUrl = uploaded.url ? normalizeShopImageUrl(uploaded.url) : fileId ? buildFileUrlFromId(fileId) : "";
    shopProfileForm.value.logoFileId = fileId;
    shopProfileForm.value.logoUrl = fileUrl;
    await updateCompanyProfile(buildCompanyProfilePayload({ logoFileId: fileId, logoUrl: fileUrl }));
    await loadShopProfile();
    shopNoticeKey.value = "page.supplierProducts.logoUploadReady";
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.logoUploadFailed");
  } finally {
    shopLogoUploading.value = false;
    input.value = "";
  }
};

const openCompanyQualificationFilePicker = () => {
  if (companyQualificationUploading.value || companyQualificationSaving.value) return;
  companyQualificationFileInput.value?.click();
};

const handleCompanyQualificationFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  const targetRow = selectedCompanyQualificationRow.value;

  companyQualificationUploading.value = true;
  companyQualificationSaving.value = true;
  shopErrorMessage.value = "";
  shopNoticeKey.value = "";
  try {
    const uploaded = await uploadQualificationFile(file);
    const fileId = String(uploaded.fileId || uploaded.id || "");
    const fileUrl = uploaded.url ? normalizeShopImageUrl(uploaded.url) : fileId ? buildFileUrlFromId(fileId) : "";
    const fileName = uploaded.name || file.name;
    if (targetRow?.qualificationId) {
      await updateCompanyQualification(targetRow.qualificationId, {
        fileId,
        fileName,
        fileUrl,
        qualificationType: targetRow.typeCode || "GENERAL",
        title: targetRow.name === "-" ? fileName : targetRow.name,
        description: targetRow.description,
        status: targetRow.status === "-" ? "SUBMITTED" : targetRow.status
      });
      shopNoticeKey.value = "page.supplierProducts.qualification.updated";
    } else {
      await createCompanyQualification({
        fileId,
        fileName,
        fileUrl,
        qualificationType: "GENERAL",
        title: fileName,
        description: "",
        status: "SUBMITTED"
      });
      shopNoticeKey.value = "page.supplierProducts.qualification.created";
    }
    await loadCompanyQualifications();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.qualification.uploadFailed");
  } finally {
    companyQualificationUploading.value = false;
    companyQualificationSaving.value = false;
    selectedCompanyQualificationRow.value = null;
    input.value = "";
  }
};

const openShopImportPicker = () => {
  if (shopImportOverlayVisible.value) return;
  shopImportInput.value?.click();
};

const openShopSkuImagePicker = (row: ShopSkuRow) => {
  selectedShopSkuImageRowId.value = row.id;
  shopSkuImageInput.value?.click();
};

const handleShopSkuImageChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  const row = shopSkuRows.value.find((item) => item.id === selectedShopSkuImageRowId.value);
  if (!file || !row) {
    input.value = "";
    return;
  }
  shopSkuImageUploading.value = true;
  shopErrorMessage.value = "";
  try {
    const uploaded = await uploadQualificationFile(file);
    const fileId = String(uploaded.fileId || uploaded.id || "");
    const fileUrl = uploaded.url ? normalizeShopImageUrl(uploaded.url) : fileId ? buildFileUrlFromId(fileId) : "";
    if (!fileId && !fileUrl) throw new Error(t("page.supplierProducts.imageUploadFailed"));
    row.imageFileId = fileId;
    row.imageUrl = fileUrl;
    row.images = fileId || fileUrl ? [{ fileId, imageUrl: fileUrl, thumbnailUrl: fileUrl, primary: true, sortOrder: 0 }] : [];
    row.thumbnail = await resolveAuthenticatedImageUrl(fileUrl || buildFileUrlFromId(fileId));
    markShopSkuDirty(row);
    shopNoticeKey.value = "page.supplierProducts.imageUploadReady";
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.imageUploadFailed");
  } finally {
    shopSkuImageUploading.value = false;
    selectedShopSkuImageRowId.value = "";
    input.value = "";
  }
};

const handleShopImportFile = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  clearShopImportTimer();
  shopImportFileName.value = file.name;
  shopImportOverlayVisible.value = true;
  shopImportPending.value = false;
  shopImporting.value = true;
  shopImportProgress.value = 8;
  shopImportStageIndex.value = 0;
  shopImportPreview.value = null;
  shopPreviewSkuRows.value = [];
  shopDirtySkuIds.value = new Set();
  shopSkuPageCache.clear();
  shopNoticeKey.value = "";
  shopErrorMessage.value = "";
  shopNoticeMessage.value = "";

  const steps = [
    { delay: 360, percent: 28, stage: 0 },
    { delay: 520, percent: 52, stage: 1 },
    { delay: 620, percent: 76, stage: 2 },
    { delay: 620, percent: 88, stage: 3 }
  ];
  let cursor = 0;
  const runStep = () => {
    const step = steps[cursor++];
    if (!step) return;
    shopImportProgress.value = step.percent;
    shopImportStageIndex.value = step.stage;
    shopImportTimer = window.setTimeout(runStep, step.delay);
  };
  shopImportTimer = window.setTimeout(runStep, 320);

  try {
    const formData = new FormData();
    formData.append("file", file);
    const payload = unwrapShopPayload(await previewShopSkuImport(formData));
    const source = isPlainRecord(payload) ? payload : {};
    const backendItems = readShopItems(source);
    shopImportPreview.value = {
      batchId: readShopString(source, "batchId") || readShopNumber(source, "batchId"),
      status: readShopString(source, "status"),
      totalCount: readShopNumber(source, "totalCount"),
      successCount: readShopNumber(source, "successCount"),
      exceptionCount: readShopNumber(source, "exceptionCount")
    };
    const batchId = shopImportPreview.value.batchId;
    const previewRows = backendItems.map((item) => normalizeShopSku(item, { preview: true, batchId })).filter((item): item is ShopSkuRow => Boolean(item));
    shopPreviewSkuRows.value = await hydrateShopSkuImages(previewRows);
    shopSkuRows.value = shopPreviewSkuRows.value;
    shopImportProgress.value = 100;
    shopImportStageIndex.value = 3;
    shopImportPending.value = false;
    shopProductCodeStatusFilter.value = "";
    shopNoticeKey.value = "page.supplierProducts.importPreviewReady";
  } catch (error) {
    shopImportPending.value = true;
    shopProductCodeStatusFilter.value = "PENDING_EXCEPTION";
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.importFailed");
  } finally {
    shopImporting.value = false;
    clearShopImportTimer();
    shopImportTimer = window.setTimeout(() => {
      shopImportOverlayVisible.value = false;
      shopImportTimer = undefined;
    }, 900);
    input.value = "";
  }
};

const resetShopFilters = () => {
  shopProductKeyword.value = "";
  shopProductTypeFilter.value = "";
  shopProductCodeStatusFilter.value = "";
  shopProductShelfStatusFilter.value = "";
  shopSkuPage.value = 1;
  if (!hasPendingShopImport.value) void searchShopSkus();
};

const getShopSkuSaveKey = (row: Pick<ShopSkuRow, "supplierSkuCode" | "importRowNo">) => `${row.importRowNo || ""}::${row.supplierSkuCode || ""}`;

const getShopPreviewBlockReason = (row: ShopSkuRow) => {
  if (row.previewAction === "BLOCKED") {
    return row.exceptionReason === "SUPPLIER_SKU_CODE_REQUIRED"
      ? t("page.supplierProducts.previewBlockMissingSku")
      : row.exceptionReason || t("page.supplierProducts.previewActionBlocked");
  }
  if (row.previewAction === "DUPLICATE") {
    return row.exceptionReason === "DUPLICATE_SUPPLIER_SKU_CODE"
      ? t("page.supplierProducts.previewBlockDuplicateSku")
      : row.exceptionReason || t("page.supplierProducts.previewActionDuplicate");
  }
  return "";
};

const formatShopPreviewBlockMessage = (rows: ShopSkuRow[]) => {
  const details = rows
    .slice(0, 5)
    .map((row) =>
      t("page.supplierProducts.previewBlockRow", {
        row: row.importRowNo || "-",
        sku: row.supplierSkuCode || "-",
        reason: getShopPreviewBlockReason(row)
      })
    )
    .join("\n");
  return [t("page.supplierProducts.previewBlockSave"), details].filter(Boolean).join("\n");
};

const readShopBatchFailureResults = (source: Record<string, unknown>) => {
  const rowResults = Array.isArray(source.rowResults) ? source.rowResults.filter(isPlainRecord) : [];
  return rowResults
    .filter((item) => {
      const status = readShopString(item, "status").toUpperCase();
      return Boolean(readShopString(item, "errorCode") || readShopString(item, "message") || status === "FAILED" || status === "ERROR");
    })
    .map((item) => ({
      importRowNumber: readShopNumber(item, "importRowNumber") || readShopNumber(item, "rowNo"),
      supplierSkuCode: readShopString(item, "supplierSkuCode"),
      message: readShopString(item, "message") || readShopString(item, "errorCode") || t("page.supplierProducts.batchSaveUnknownError")
    }));
};

const formatShopBatchSaveSummary = (source: Record<string, unknown>) => {
  const total = readShopNumber(source, "totalCount");
  const success = readShopNumber(source, "successCount");
  const inserted = readShopNumber(source, "insertedCount");
  const updated = readShopNumber(source, "updatedCount");
  const failed = readShopNumber(source, "failedCount");
  const messageKey = failed > 0 ? "page.supplierProducts.batchSavePartial" : "page.supplierProducts.batchSaveSuccess";
  return t(messageKey, { total, success, inserted, updated, failed });
};

const formatShopBatchFailureText = (failures: ReturnType<typeof readShopBatchFailureResults>) =>
  failures
    .slice(0, 5)
    .map((item) =>
      t("page.supplierProducts.batchSaveFailureRow", {
        row: item.importRowNumber || "-",
        sku: item.supplierSkuCode || "-",
        reason: item.message
      })
    )
    .join("\n");

const confirmShopImportPreview = async () => {
  if (!unsavedShopSkuRows.value.length && !hasDirtyShopSkuRows.value) return;
  if (shopBlockedPreviewRows.value.length) {
    shopNoticeKey.value = "";
    shopErrorMessage.value = formatShopPreviewBlockMessage(shopBlockedPreviewRows.value);
    return;
  }
  shopSaving.value = true;
  shopErrorMessage.value = "";
  shopNoticeMessage.value = "";
  try {
    const dirtyPersistedRows = shopPersistedSkuRows.value.filter((row) => shopDirtySkuIds.value.has(row.id) && row.skuId && !row.isPreview);
    const rowsToSave = shopPreviewSkuRows.value.length
      ? [...dirtyPersistedRows, ...shopPreviewSkuRows.value]
      : [...dirtyPersistedRows, ...unsavedShopSkuRows.value];
    const payload = unwrapShopPayload(await batchUpsertShopSkus({
      importBatchId: shopImportPreview.value?.batchId || undefined,
      items: rowsToSave.map((row) => ({
        skuId: row.skuId || row.existingSkuId || undefined,
        importBatchId: row.importBatch || shopImportPreview.value?.batchId || undefined,
        importRowNumber: row.importRowNo || undefined,
        supplierSkuCode: row.supplierSkuCode,
        sku: buildShopSkuSavePayload(row)
      }))
    }));
    const source = isPlainRecord(payload) ? payload : {};
    const failures = readShopBatchFailureResults(source);
    const failedKeys = new Set(failures.map((item) => `${item.importRowNumber || ""}::${item.supplierSkuCode || ""}`));
    const savedRows = await hydrateShopSkuImages(
      readShopItems(source).map((item) => normalizeShopSku(item)).filter((item): item is ShopSkuRow => Boolean(item))
    );
    const savedSkuIds = new Set(savedRows.map((row) => String(row.skuId || "")).filter(Boolean));
    const savedSupplierCodes = new Set(savedRows.map((row) => row.supplierSkuCode).filter(Boolean));
    const remainingPersistedRows = shopPersistedSkuRows.value.filter((row) => {
      if (row.skuId && savedSkuIds.has(String(row.skuId))) return false;
      if (!row.skuId && row.supplierSkuCode && savedSupplierCodes.has(row.supplierSkuCode)) return false;
      return true;
    });
    shopPersistedSkuRows.value = [...savedRows, ...remainingPersistedRows];
    shopSkuPageCache.clear();
    shopPreviewSkuRows.value = failures.length
      ? shopPreviewSkuRows.value.filter((row) => failedKeys.has(getShopSkuSaveKey(row)))
      : [];
    shopSkuRows.value = shopPreviewSkuRows.value.length ? shopPreviewSkuRows.value : shopPersistedSkuRows.value;
    shopDirtySkuIds.value = new Set(
      [...shopDirtySkuIds.value].filter((id) => shopSkuRows.value.some((row) => row.id === id && !savedSkuIds.has(String(row.skuId || ""))))
    );
    shopNoticeKey.value = "";
    shopNoticeMessage.value = formatShopBatchSaveSummary(source);
    shopImportPreview.value = failures.length ? shopImportPreview.value : null;
    shopProductCodeStatusFilter.value = "PENDING_EXCEPTION";
    if (failures.length) {
      shopErrorMessage.value = formatShopBatchFailureText(failures);
    } else {
      expandedShopSkuId.value = "";
      await searchShopSkus();
    }
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.skuSaveFailed");
  } finally {
    shopSaving.value = false;
  }
};

const removeShopSku = async (row: ShopSkuRow) => {
  if (!row.skuId || row.isPreview) return;
  shopErrorMessage.value = "";
  try {
    await deleteShopSku(row.skuId);
    shopNoticeKey.value = "page.supplierProducts.skuDeleted";
    await searchShopSkus();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.skuDeleteFailed");
  }
};

const toggleShopShelfStatus = async (row: ShopSkuRow) => {
  if (!row.skuId || row.isPreview) return;
  shopErrorMessage.value = "";
  try {
    await updateShopSkuShelfStatus(row.skuId, {
      shelfStatus: row.listingStatus === "ON_SHELF" ? "OFF_SHELF" : "ON_SHELF"
    });
    shopNoticeKey.value = "page.supplierProducts.shelfStatusSaved";
    await searchShopSkus();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.shelfStatusFailed");
  }
};

const updateAllShopShelfStatus = async (shelfStatus: "ON_SHELF" | "OFF_SHELF") => {
  const currentRows = savedShopSkuRows.value;
  if (!currentRows.length && !shopSkuTotal.value) {
    shopNoticeKey.value = "page.supplierProducts.noSavedSkuForShelf";
    return;
  }
  shopSaving.value = true;
  shopErrorMessage.value = "";
  try {
    const rows = hasPendingShopImport.value ? currentRows : await fetchAllSavedShopSkuRows();
    if (!rows.length) {
      shopNoticeKey.value = "page.supplierProducts.noSavedSkuForShelf";
      return;
    }
    await runShopShelfUpdates(rows, shelfStatus);
    shopNoticeKey.value = shelfStatus === "ON_SHELF" ? "page.supplierProducts.bulkOnShelfSaved" : "page.supplierProducts.bulkOffShelfSaved";
    shopSkuPageCache.clear();
    await searchShopSkus();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.shelfStatusFailed");
  } finally {
    shopSaving.value = false;
  }
};

const resolveShopException = async (row: ShopSkuRow) => {
  if (!row.skuId || row.isPreview) return;
  shopErrorMessage.value = "";
  try {
    await resolveShopSkuException(row.skuId, {
      action: row.platformCode || row.impaCode ? "SELECT_CANDIDATE" : "IGNORE",
      platformCode: row.platformCode,
      impaCode: row.impaCode,
      reason: t("page.supplierProducts.manualConfirmReason")
    });
    shopNoticeKey.value = "page.supplierProducts.exceptionResolved";
    await searchShopSkus();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.exceptionResolveFailed");
  }
};

const toggleShopListFullscreen = () => {
  shopListFullscreen.value = !shopListFullscreen.value;
};

const handleShopFullscreenKeydown = (event: KeyboardEvent) => {
  if (event.key === "Escape" && shopListFullscreen.value) {
    shopListFullscreen.value = false;
  }
};

onUnmounted(() => {
  clearShopImportTimer();
  revokeProtectedImageObjectUrls();
  if (typeof window !== "undefined") window.removeEventListener("keydown", handleShopFullscreenKeydown);
});

const compareSkuColumns = computed<TableColumn[]>(() => [
  { key: "impaCode", label: t("field.impaCode"), width: "108px" },
  { key: "thumbnail", label: t("table.thumbnail"), width: "58px", align: "center" },
  { key: "name", label: t("field.item") },
  { key: "attributes", label: t("table.attrs") },
  { key: "price", label: t("field.price"), width: "102px", align: "right" },
  { key: "stock", label: t("field.stock"), width: "78px", align: "right" },
  { key: "operation", label: t("common.operation"), width: "136px", align: "center" }
]);

const purchaseOrderColumns = computed<TableColumn[]>(() => [
  { key: "purchaseOrderNo", label: t("purchaseOrder.field.purchaseOrderNo"), width: "156px" },
  { key: "sourceNo", label: t("purchaseOrder.field.sourceNo"), width: "158px" },
  { key: "vesselName", label: t("purchaseOrder.field.vesselName"), width: "136px" },
  { key: "supplierCount", label: t("purchaseOrder.field.supplierCount"), width: "92px", align: "right" },
  { key: "itemCount", label: t("purchaseOrder.field.itemCount"), width: "82px", align: "right" },
  { key: "totalAmount", label: t("purchaseOrder.field.totalAmount"), width: "118px", align: "right" },
  { key: "status", label: t("field.status"), width: "126px" },
  { key: "createdAt", label: t("purchaseOrder.field.createdAt"), width: "132px" },
  { key: "requiredDeliveryTime", label: t("purchaseOrder.field.requiredDeliveryTime"), width: "132px" },
  { key: "operation", label: t("common.operation"), width: "82px", align: "center" }
]);

const supplierOrderColumns = computed<TableColumn[]>(() => [
  { key: "purchaseOrderNo", label: t("purchaseOrder.field.purchaseOrderNo"), width: "156px" },
  { key: "sourceNo", label: t("purchaseOrder.field.sourceNo"), width: "158px" },
  { key: "vesselName", label: t("purchaseOrder.field.vesselName"), width: "136px" },
  { key: "itemCount", label: t("purchaseOrder.field.itemCount"), width: "82px", align: "right" },
  { key: "totalAmount", label: t("purchaseOrder.field.totalAmount"), width: "118px", align: "right" },
  { key: "status", label: t("field.status"), width: "126px" },
  { key: "createdAt", label: t("purchaseOrder.field.createdAt"), width: "132px" },
  { key: "operation", label: t("common.operation"), width: "82px", align: "center" }
]);

const purchaseSupplierColumns = computed<TableColumn[]>(() => [
  { key: "supplierName", label: t("field.supplier") },
  { key: "status", label: t("field.status"), width: "126px" },
  { key: "itemCount", label: t("purchaseOrder.field.itemCount"), width: "82px", align: "right" },
  { key: "finalAmount", label: t("purchaseOrder.field.finalAmount"), width: "118px", align: "right" },
  { key: "packagingMethod", label: t("purchaseOrder.field.packagingMethod"), width: "132px" },
  { key: "expectedReadyAt", label: t("purchaseOrder.field.expectedReadyAt"), width: "140px" },
  { key: "operation", label: t("common.operation"), width: "130px", align: "center" }
]);

const purchaseItemColumns = computed<TableColumn[]>(() => [
  { key: "supplierSkuCode", label: t("purchaseOrder.field.supplierSkuCode"), width: "126px" },
  { key: "impaCode", label: t("field.impaCode"), width: "108px" },
  { key: "productName", label: t("field.item") },
  { key: "specification", label: t("attr.spec"), width: "190px" },
  { key: "quantity", label: t("purchaseOrder.field.quantity"), width: "86px", align: "right" },
  { key: "unit", label: t("purchaseOrder.field.unit"), width: "72px" },
  { key: "unitPrice", label: t("field.price"), width: "96px", align: "right" },
  { key: "amount", label: t("field.amount"), width: "108px", align: "right" },
  { key: "flags", label: t("purchaseOrder.field.flags"), width: "140px" }
]);

const supplierColumns = computed<TableColumn[]>(() => [
  { key: "name", label: t("field.supplier") },
  { key: "port", label: t("field.port") },
  { key: "score", label: t("field.score"), width: "90px" },
  { key: "status", label: t("field.status"), width: "112px" },
  { key: "operation", label: t("common.operation"), width: "116px", align: "center" }
]);

const impaItemColumns = computed<TableColumn[]>(() => [
  { key: "impaCode", label: t("field.impaCode"), width: "118px" },
  { key: "nameCn", label: t("field.item") },
  { key: "specification", label: t("attr.spec") },
  { key: "unit", label: t("field.unit"), width: "86px" },
  { key: "segmentCode", label: t("field.segmentCode"), width: "108px" },
  { key: "operation", label: t("common.operation"), width: "96px", align: "center" }
]);

const registrationColumns = computed<TableColumn[]>(() => [
  { key: "companyType", label: t("registration.companyType"), width: "118px" },
  { key: "companyName", label: t("registration.companyName"), width: "240px" },
  { key: "contactName", label: t("registration.contactName"), width: "98px" },
  { key: "phone", label: t("registration.phone"), width: "126px" },
  { key: "email", label: t("registration.email"), width: "160px" },
  { key: "qualificationFiles", label: t("registration.qualificationFiles"), width: "128px" },
  { key: "submittedAt", label: t("registration.submittedAt"), width: "150px" },
  { key: "operation", label: t("common.operation"), width: "122px", align: "center" }
]);

const companyMemberColumns = computed<TableColumn[]>(() => [
  { key: "username", label: t("companyMembers.field.account"), width: "132px" },
  { key: "name", label: t("companyMembers.field.name"), width: "116px" },
  { key: "contact", label: t("companyMembers.field.contact") },
  { key: "roleCodes", label: t("companyMembers.field.roles") },
  { key: "status", label: t("field.status"), width: "112px" },
  { key: "lastLoginAt", label: t("companyMembers.field.lastLoginAt"), width: "150px" },
  { key: "source", label: t("companyMembers.field.source"), width: "118px" },
  { key: "operation", label: t("common.operation"), width: "172px", align: "center" }
]);

const requestColumns = computed<TableColumn[]>(() => [
  { key: "demandNo", label: t("page.materialDemand.demandNo"), width: "150px" },
  { key: "applicationNo", label: t("page.materialDemand.applicationNo"), width: "132px" },
  { key: "vesselName", label: t("page.materialDemand.vesselName"), width: "142px" },
  { key: "inquiryDate", label: t("page.materialDemand.inquiryDate"), width: "118px" },
  { key: "skuCount", label: t("page.materialDemand.skuCount"), width: "88px", align: "right" },
  { key: "exactCount", label: t("page.materialDemand.exactCount"), width: "92px", align: "right" },
  { key: "similarCount", label: t("page.materialDemand.similarCount"), width: "92px", align: "right" },
  { key: "unmatchedCount", label: t("page.materialDemand.unmatchedCount"), width: "92px", align: "right" },
  { key: "status", label: t("field.status"), width: "96px" },
  { key: "operation", label: t("common.operation"), width: "70px", align: "center" }
]);

const inquiryDemandColumns = computed<TableColumn[]>(() => [
  { key: "demandNo", label: t("page.materialDemand.demandNo"), width: "150px" },
  { key: "applicationNo", label: t("page.materialDemand.applicationNo"), width: "132px" },
  { key: "vesselName", label: t("page.materialDemand.vesselName"), width: "132px" },
  { key: "inquiryDate", label: t("page.materialDemand.inquiryDate"), width: "112px" },
  { key: "sourceFileName", label: t("page.materialDemand.sourceFileName"), width: "168px" },
  { key: "skuCount", label: t("page.materialDemand.skuCount"), width: "82px", align: "right" },
  { key: "exactCount", label: t("page.materialDemand.exactCount"), width: "88px", align: "right" },
  { key: "similarCount", label: t("page.materialDemand.similarCount"), width: "88px", align: "right" },
  { key: "unmatchedCount", label: t("page.materialDemand.unmatchedCount"), width: "88px", align: "right" },
  { key: "status", label: t("field.status"), width: "92px" },
  { key: "updatedAt", label: t("field.updatedAt"), width: "136px" },
  { key: "operation", label: t("common.operation"), width: "70px", align: "center" }
]);

const dashboardRequestColumns = computed<TableColumn[]>(() => [
  { key: "requestNo", label: t("table.requestNo"), width: "118px" },
  { key: "type", label: t("field.item"), width: "70px" },
  { key: "vessel", label: t("field.vessel"), width: "136px" },
  { key: "supplyTime", label: t("field.supplyTime"), width: "104px" },
  { key: "status", label: t("field.status"), width: "86px" },
  { key: "amount", label: t("field.amount"), width: "78px", align: "right" },
  { key: "operation", label: t("common.operation"), width: "54px", align: "center" }
]);

const procurementFlowColumns = computed<TableColumn[]>(() => [
  { key: "code", label: t("table.documentNo"), width: "150px" },
  { key: "subject", label: t("table.subject") },
  { key: "supplier", label: t("field.supplier"), width: "180px" },
  { key: "status", label: t("field.status"), width: "112px" },
  { key: "validUntil", label: t("table.validUntil"), width: "150px" },
  { key: "operation", label: t("common.operation"), width: "92px", align: "center" }
]);

const procurementOrderColumns = computed<TableColumn[]>(() => [
  { key: "code", label: t("table.purchaseNo"), width: "138px" },
  { key: "sourceNo", label: t("table.sourceNo"), width: "138px" },
  { key: "vesselName", label: t("table.vesselName"), width: "140px" },
  { key: "supplier", label: t("field.supplier"), width: "170px" },
  { key: "amount", label: t("table.purchaseAmount"), width: "110px", align: "right" },
  { key: "status", label: t("table.purchaseStatus"), width: "112px" },
  { key: "orderDate", label: t("table.orderDate"), width: "116px" },
  { key: "deliveryDate", label: t("table.deliveryDate"), width: "116px" },
  { key: "operation", label: t("common.operation"), width: "82px", align: "center" }
]);

const isProcurementOrderPage = computed(() => pageKey.value === "orders" || pageKey.value === "foodOrders");

const procurementFlowRows = computed<ProcurementFlowRow[]>(() => {
  const normalizedInquiries = inquiries.map((row) => ({
    code: "code" in row ? String(row.code) : String(row.inquiryNo || ""),
    subject: "subject" in row ? String(row.subject) : String(row.inquiryNo || ""),
    supplier: String(row.supplier),
    status: String(row.status),
    validUntil: String(row.validUntil)
  }));
  const maps: Record<string, ProcurementFlowRow[]> = {
    inquiries: normalizedInquiries,
    quotes: quoteRows,
    foodInquiries,
    foodQuotes,
    foodComparisonList: foodComparisonRows,
    crewServices: crewServiceRows
  };
  return maps[pageKey.value] ?? [];
});

const procurementOrderRows = computed<ProcurementOrderRow[]>(() => (pageKey.value === "foodOrders" ? foodOrderRows : []));

const formatCompareMoney = (amount?: number, currency?: string) => {
  if (amount == null || !Number.isFinite(amount)) return "-";
  return `${currency || "CNY"} ${amount.toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
};

const normalizeCompareText = (value?: string | number | null) => {
  if (value == null) return "";
  const text = String(value).trim();
  return text && text !== "undefined" && text !== "null" ? text : "";
};

const compareSupplierKey = (strategy: MaterialComparisonStrategy, supplier: { supplierId?: number; companyId?: number; supplierName?: string }, index: number) =>
  `${strategy.strategyType}:${supplier.companyId ?? supplier.supplierId ?? (normalizeCompareText(supplier.supplierName) || index)}`;

const sameCompareSupplier = (candidate: MaterialComparisonCandidate, supplier: { companyId?: number; supplierName?: string }) => {
  if (candidate.companyId != null && supplier.companyId != null) return candidate.companyId === supplier.companyId;
  return normalizeCompareText(candidate.supplierName) === normalizeCompareText(supplier.supplierName);
};

const candidateSupplierKey = (strategyKey: string, candidate: MaterialComparisonCandidate, index: number) => {
  const strategy = compareData.value?.strategies.find((item) => item.strategyType === strategyKey);
  const supplierIndex = strategy?.suppliers.findIndex((supplier) => sameCompareSupplier(candidate, supplier)) ?? -1;
  if (strategy && supplierIndex >= 0) return compareSupplierKey(strategy, strategy.suppliers[supplierIndex], supplierIndex);
  return `${strategyKey}:${candidate.companyId ?? (normalizeCompareText(candidate.supplierName) || index)}`;
};

const readCompareSpecText = (specifications: unknown) => {
  if (typeof specifications === "string") return specifications;
  if (!Array.isArray(specifications)) return "";
  return specifications
    .map((item) => {
      if (item && typeof item === "object" && !Array.isArray(item)) {
        const record = item as Record<string, unknown>;
        const name = normalizeCompareText(record.name as string) || normalizeCompareText(record.key as string);
        const value = normalizeCompareText(record.value as string);
        const unit = normalizeCompareText(record.unit as string);
        return [name, value, unit].filter(Boolean).join(": ");
      }
      return normalizeCompareText(item as string);
    })
    .filter(Boolean)
    .join(" / ");
};

const compareItemLowestPrice = (item: MaterialComparisonItem) => {
  const prices = item.candidates.map((candidate) => candidate.unitPrice).filter((price): price is number => price != null && Number.isFinite(price));
  return prices.length ? Math.min(...prices) : undefined;
};

const compareCandidateToRow = (item: MaterialComparisonItem, candidate: MaterialComparisonCandidate, index: number, strategyKey: string): CompareSkuRow => {
  const specText = normalizeCompareText(candidate.attributeSummary) || readCompareSpecText(candidate.specifications) || normalizeCompareText(item.specification);
  const supplierName = normalizeCompareText(candidate.supplierName) || "-";
  const image = normalizeCompareText(candidate.thumbnailUrl) || normalizeCompareText(candidate.imageUrl);
  const attributes: SkuAttribute[] = [
    { key: "spec", labelKey: "attr.spec", value: specText || "-" },
    { key: "supplier", labelKey: "field.supplier", value: supplierName },
    { key: "packing", labelKey: "attr.packing", value: normalizeCompareText(candidate.packageSpec) || "-" },
    { key: "match", labelKey: "page.materials.supplierMatch", value: normalizeCompareText(candidate.matchType) || normalizeCompareText(candidate.reason) || "-" }
  ];
  return {
    id: `${item.demandItemId ?? item.rowNo ?? index}-${candidate.skuId ?? candidate.supplierSkuCode ?? index}-${strategyKey}`,
    supplierKey: candidateSupplierKey(strategyKey, candidate, index),
    supplierName,
    demandItemId: String(item.demandItemId ?? item.rowNo ?? index),
    lowestPrice: compareItemLowestPrice(item),
    matchType: candidate.matchType,
    reason: candidate.reason,
    thumbnail: image,
    images: image ? [{ src: image, alt: normalizeCompareText(candidate.productName) || normalizeCompareText(item.productName) || supplierName }] : [],
    name: normalizeCompareText(candidate.productName) || normalizeCompareText(item.productName) || "-",
    itemNo: normalizeCompareText(candidate.supplierSkuCode) || String(candidate.skuId ?? "-"),
    impaCode: normalizeCompareText(candidate.impaCode) || normalizeCompareText(candidate.platformCode) || normalizeCompareText(item.impaCode) || normalizeCompareText(item.platformCode) || "-",
    supplier: supplierName,
    price: candidate.unitPrice ?? 0,
    currency: normalizeCompareText(candidate.currencySymbol) || normalizeCompareText(candidate.currency) || "CNY",
    stock: candidate.stockQty ?? 0,
    status: candidate.unitPrice != null ? "success" : "warning",
    attributes
  };
};

const compareStrategyCards = computed<CompareStrategyCard[]>(() =>
  (compareData.value?.strategies ?? []).map((strategy) => ({
    key: strategy.strategyType,
    label: strategy.strategyName || (strategy.strategyType === "SINGLE_SUPPLIER" ? t("compare.singleSupplier") : t("compare.lowestMixed")),
    matchSummary: `${strategy.matchedCount}/${strategy.totalCount}`,
    total: formatCompareMoney(strategy.totalAmount, strategy.currency),
    tone: strategy.strategyType === "SINGLE_SUPPLIER" ? "blue" : "green",
    suppliers: strategy.suppliers.map((supplier, supplierIndex) => ({
      key: compareSupplierKey(strategy, supplier, supplierIndex),
      supplier: normalizeCompareText(supplier.supplierName) || "-",
      skuCount: supplier.matchedCount ?? supplier.skuCount ?? 0,
      amount: formatCompareMoney(supplier.totalAmount ?? supplier.amount, supplier.currency || strategy.currency)
    }))
  }))
);

const compareSupplyEditableFields = computed<Array<{ key: CompareSupplyEditableKey; label: string; inputType: string }>>(() => [
  { key: "vessel", label: t("compare.supply.vessel"), inputType: "text" },
  { key: "port", label: t("compare.supply.port"), inputType: "text" },
  { key: "date", label: t("compare.supply.date"), inputType: "date" }
]);

const compareWeatherInfo = computed(() => ({
  label: t("compare.supply.weather"),
  value: normalizeCompareText(compareData.value?.supplyInfo?.weather) || normalizeCompareText(compareData.value?.supplyInfo?.weatherInfo) || t("compare.supply.weatherValue")
}));

const activeCompareStrategy = computed(() => compareStrategyCards.value.find((strategy) => strategy.key === selectedStrategy.value) ?? compareStrategyCards.value[0] ?? null);

const activeCompareSupplierKeys = computed(() =>
  selectedCompareSupplier.value ? [selectedCompareSupplier.value] : activeCompareStrategy.value?.suppliers.map((supplier) => supplier.key) ?? []
);

const compareSkuRows = computed<CompareSkuRow[]>(() => {
  if (!compareData.value || !activeCompareStrategy.value) return [];
  const strategyKey = activeCompareStrategy.value.key;
  return compareData.value.items.flatMap((item, itemIndex) => {
    let candidates: MaterialComparisonCandidate[] = [];
    if (selectedCompareSupplier.value) {
      candidates = item.candidates.filter((candidate, candidateIndex) => candidateSupplierKey(strategyKey, candidate, candidateIndex) === selectedCompareSupplier.value);
    } else if (strategyKey === "SINGLE_SUPPLIER") {
      candidates = item.singleSupplierCandidate ? [item.singleSupplierCandidate] : [];
    } else {
      candidates = item.lowestCandidate ? [item.lowestCandidate] : [];
    }
    return candidates.map((candidate, candidateIndex) => compareCandidateToRow(item, candidate, itemIndex * 1000 + candidateIndex, strategyKey));
  });
});

const compareFilteredSkus = computed(() =>
  compareSkuRows.value.filter((row) => {
    const supplierMatches = activeCompareSupplierKeys.value.length === 0 || activeCompareSupplierKeys.value.includes(row.supplierKey);
    const keyword = compareSkuKeyword.value.trim().toLowerCase();
    const keywordMatches =
      !keyword ||
      [row.impaCode, row.itemNo, row.name, row.supplier, ...row.attributes.map((attribute) => attribute.value)].some((value) => value.toLowerCase().includes(keyword));
    const priceMatches = !comparePreferenceFilters.value.includes("priceLow") || row.lowestPrice == null || row.price <= row.lowestPrice;
    return supplierMatches && keywordMatches && priceMatches;
  })
);

const purchaseOrderRows = computed(() =>
  purchaseOrders.value.map((row) => ({
    ...row,
    sourceNo: [row.demandNo, row.applicationNo].filter(Boolean).join(" / ") || "-"
  })) as unknown as Record<string, unknown>[]
);

const supplierPurchaseOrderRows = computed(() =>
  supplierPurchaseOrders.value.map((row) => ({
    ...row,
    sourceNo: [row.demandNo, row.applicationNo].filter(Boolean).join(" / ") || "-"
  })) as unknown as Record<string, unknown>[]
);

const purchaseOrderIdFromRoute = computed(() => String(route.params.orderId || "").trim());
const isPurchaseOrderDetailPage = computed(() => pageKey.value === "orders" && Boolean(purchaseOrderIdFromRoute.value));
const purchaseOrderHasFilters = computed(() =>
  Boolean(
    purchaseOrderKeyword.value.trim() ||
      purchaseOrderStatus.value.trim() ||
      purchaseOrderSupplier.value.trim() ||
      purchaseOrderCreatedFrom.value.trim() ||
      purchaseOrderCreatedTo.value.trim() ||
      purchaseOrderDeliveryFrom.value.trim() ||
      purchaseOrderDeliveryTo.value.trim()
  )
);
const currentRoles = computed(() => getAuthSession()?.roles ?? []);
const isSupplierOnlyUser = computed(() => currentRoles.value.includes("supplier") && !currentRoles.value.some((role) => role === "admin" || role === "purchaser"));
const purchaseOrderWorkspaceMode = computed<"buyer" | "supplier">(() => (isSupplierOnlyUser.value ? "supplier" : "buyer"));
const activePurchaseOrderRows = computed(() => (purchaseOrderWorkspaceMode.value === "supplier" ? supplierPurchaseOrderRows.value : purchaseOrderRows.value));
const activePurchaseOrderLoading = computed(() => (purchaseOrderWorkspaceMode.value === "supplier" ? supplierPurchaseOrderLoading.value : purchaseOrderLoading.value));
const activePurchaseOrderErrorKey = computed(() => (purchaseOrderWorkspaceMode.value === "supplier" ? supplierPurchaseOrderErrorKey.value : purchaseOrderErrorKey.value));

const selectedCompareOrderableRows = computed(() => compareSkuRows.value.filter((row) => row.price > 0));
const selectedCompareExcludedCount = computed(() => Math.max((compareData.value?.items.length ?? 0) - selectedCompareOrderableRows.value.length, 0));
const selectedCompareSupplierCount = computed(() => new Set(selectedCompareOrderableRows.value.map((row) => row.supplierKey)).size);
const selectedCompareOrderAmount = computed(() => selectedCompareOrderableRows.value.reduce((sum, row) => sum + (Number.isFinite(row.price) ? row.price : 0), 0));
const selectedCompareCurrency = computed(() => selectedCompareOrderableRows.value[0]?.currency || "CNY");
const selectedCompareSupplierSummaries = computed(() => {
  const map = new Map<string, { supplier: string; count: number; amount: number; currency: string }>();
  selectedCompareOrderableRows.value.forEach((row) => {
    const key = row.supplierKey || row.supplier;
    const current = map.get(key) ?? { supplier: row.supplier || "-", count: 0, amount: 0, currency: row.currency || "CNY" };
    current.count += 1;
    current.amount += Number.isFinite(row.price) ? row.price : 0;
    map.set(key, current);
  });
  return [...map.values()];
});

const purchaseDetailSupplierRows = computed(() => (purchaseOrderDetail.value?.supplierOrders ?? []) as unknown as Record<string, unknown>[]);
const purchaseDetailItemRows = computed(() => (purchaseOrderDetail.value?.supplierOrders ?? []).flatMap((supplier) => supplier.items) as unknown as Record<string, unknown>[]);
const purchaseDetailEvents = computed(() => purchaseOrderDetail.value?.events ?? []);

const formatPurchaseMoney = (amount?: number, currency?: string) => {
  if (amount == null || !Number.isFinite(amount)) return "-";
  return `${currency || "CNY"} ${amount.toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
};

const purchaseStatusLabel = (status?: string | null) => {
  const value = String(status || "").toUpperCase();
  const key = `purchaseOrder.status.${value}`;
  const label = t(key);
  return label === key ? status || "-" : label;
};

const purchaseStatusVariant = (status?: string | null): StatusVariant => {
  const value = String(status || "").toUpperCase();
  if (value === "PREPARING" || value === "COMPLETED") return "success";
  if (value === "PENDING_SUPPLIER_CONFIRM" || value === "PARTIALLY_CONFIRMED") return "warning";
  if (value === "REJECTED" || value === "PARTIALLY_REJECTED" || value === "CANCELED") return "danger";
  return "info";
};

const packagingMethodLabel = (value?: string | null) => {
  const text = String(value || "").toUpperCase();
  if (!text) return "-";
  const key = `purchaseOrder.packaging.${text}`;
  const label = t(key);
  return label === key ? value || "-" : label;
};

const getPurchaseErrorText = (error: unknown, fallbackKey: string) => {
  if (error instanceof ApiError) {
    const message = error.message || "";
    const keys: Record<string, string> = {
      PURCHASE_ORDER_NO_ORDERABLE_ITEMS: "purchaseOrder.error.noOrderableItems",
      PURCHASE_ORDER_SKU_CONFLICT: "purchaseOrder.error.skuConflict",
      DEMAND_ID_REQUIRED: "purchaseOrder.error.demandRequired",
      EXPECTED_READY_AT_REQUIRED: "purchaseOrder.error.expectedReadyAtRequired",
      REJECT_REASON_REQUIRED: "purchaseOrder.error.rejectReasonRequired",
      PURCHASE_ORDER_NOT_FOUND: "purchaseOrder.error.notFound",
      SUPPLIER_ORDER_NOT_FOUND: "purchaseOrder.error.supplierOrderNotFound"
    };
    return keys[message] ? t(keys[message]) : message || t(fallbackKey);
  }
  return error instanceof Error && error.message ? error.message : t(fallbackKey);
};

const selectCompareStrategy = (key: string) => {
  selectedStrategy.value = key;
  selectedCompareSupplier.value = null;
};

const selectCompareSupplier = (strategyKey: string, supplierKey: string) => {
  const shouldClear = selectedStrategy.value === strategyKey && selectedCompareSupplier.value === supplierKey;
  selectedStrategy.value = strategyKey;
  selectedCompareSupplier.value = shouldClear ? null : supplierKey;
};

const toggleComparePreferenceFilter = (value: string) => {
  comparePreferenceFilters.value = comparePreferenceFilters.value.includes(value)
    ? comparePreferenceFilters.value.filter((item) => item !== value)
    : [...comparePreferenceFilters.value, value];
};

const openReplacementDialog = (sku: SupplierSku) => {
  replacementSku.value = sku;
};

const handleCompareScroll = () => {
  showCompareBackTop.value = (compareWorkspaceRef.value?.scrollTop ?? 0) > 120;
};

const scrollCompareToTop = () => {
  const target = compareWorkspaceRef.value;
  if (!target) return;
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  target.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
};

const compareDemandId = computed(() => String(route.params.requestId || "").trim());

const loadCompareWorkspace = async () => {
  if (pageKey.value !== "compare") return;
  const currentDemandId = compareDemandId.value;
  compareError.value = "";
  compareData.value = null;
  selectedCompareSupplier.value = null;
  replacementSku.value = null;
  comparePreferenceFilters.value = comparePreferenceFilters.value.filter((item) => item === "priceLow");
  if (!currentDemandId) {
    compareError.value = t("compare.missingDemandId");
    return;
  }
  compareLoading.value = true;
  try {
    const response = await getMaterialDemandComparison(currentDemandId);
    compareData.value = response;
    const supplyInfo = response.supplyInfo;
    compareSupplyForm.value = {
      vessel: normalizeCompareText(supplyInfo?.vesselName) || normalizeCompareText(response.demand?.vesselName) || "",
      port: normalizeCompareText(supplyInfo?.supplyPort) || normalizeCompareText(supplyInfo?.port) || t("compare.supply.portPending"),
      date: normalizeCompareText(supplyInfo?.supplyDate) || normalizeCompareText(supplyInfo?.inquiryDate) || normalizeCompareText(response.demand?.inquiryDate) || ""
    };
    selectedStrategy.value = response.strategies[0]?.strategyType || "LOWEST_MIXED";
  } catch (error) {
    if (error instanceof ApiError && error.status) {
      compareError.value = `${error.status} ${error.message || t("compare.loadFailed")}`;
    } else if (error instanceof Error && error.message) {
      compareError.value = error.message;
    } else {
      compareError.value = t("compare.loadFailed");
    }
  } finally {
    compareLoading.value = false;
  }
};

const openPurchaseOrderDialog = () => {
  purchaseOrderNotice.value = "";
  purchaseOrderError.value = "";
  if (!compareDemandId.value) {
    purchaseOrderError.value = t("page.materials.saveBeforeCompare");
    return;
  }
  if (!selectedCompareOrderableRows.value.length) {
    purchaseOrderError.value = t("purchaseOrder.error.noOrderableItems");
    return;
  }
  purchaseOrderForm.value = {
    supplyPort: compareSupplyForm.value.port || "",
    vesselEta: compareSupplyForm.value.date || "",
    requiredDeliveryTime: compareSupplyForm.value.date || "",
    defaultPackagingMethod: "UNIFIED_PACKAGING",
    buyerRemark: ""
  };
  purchaseOrderDialogOpen.value = true;
};

const submitPurchaseOrder = async () => {
  if (!compareDemandId.value || !activeCompareStrategy.value) {
    purchaseOrderError.value = t("page.materials.saveBeforeCompare");
    return;
  }
  if (!selectedCompareOrderableRows.value.length) {
    purchaseOrderError.value = t("purchaseOrder.error.noOrderableItems");
    return;
  }
  purchaseOrderCreating.value = true;
  purchaseOrderError.value = "";
  purchaseOrderNotice.value = "";
  try {
    const response = await createPurchaseOrderFromDemand(compareDemandId.value, {
      demandId: Number(compareDemandId.value),
      strategyType: activeCompareStrategy.value.key,
      supplyPort: purchaseOrderForm.value.supplyPort,
      vesselEta: purchaseOrderForm.value.vesselEta,
      requiredDeliveryTime: purchaseOrderForm.value.requiredDeliveryTime,
      defaultPackagingMethod: purchaseOrderForm.value.defaultPackagingMethod,
      buyerRemark: purchaseOrderForm.value.buyerRemark
    });
    purchaseOrderDialogOpen.value = false;
    purchaseOrderNotice.value = response.idempotent ? t("purchaseOrder.notice.idempotent") : t("purchaseOrder.notice.created");
    router.push(response.redirectTo || `/orders/${response.purchaseOrderId}`);
  } catch (error) {
    purchaseOrderError.value = getPurchaseErrorText(error, "purchaseOrder.error.createFailed");
  } finally {
    purchaseOrderCreating.value = false;
  }
};

const ensureCompareLoaded = () => {
  if (pageKey.value === "compare") {
    void loadCompareWorkspace();
  }
};

const loadPurchaseOrders = async () => {
  purchaseOrderLoading.value = true;
  purchaseOrderErrorKey.value = "";
  try {
    const response = await listPurchaseOrders({
      keyword: purchaseOrderKeyword.value,
      status: purchaseOrderStatus.value,
      supplier: purchaseOrderSupplier.value,
      createdFrom: purchaseOrderCreatedFrom.value,
      createdTo: purchaseOrderCreatedTo.value,
      deliveryFrom: purchaseOrderDeliveryFrom.value,
      deliveryTo: purchaseOrderDeliveryTo.value,
      page: 1,
      size: 50
    });
    purchaseOrders.value = response.items;
  } catch {
    purchaseOrders.value = [];
    purchaseOrderErrorKey.value = "purchaseOrder.error.loadFailed";
  } finally {
    purchaseOrderLoading.value = false;
  }
};

const loadSupplierPurchaseOrders = async () => {
  supplierPurchaseOrderLoading.value = true;
  supplierPurchaseOrderErrorKey.value = "";
  try {
    const response = await listSupplierPurchaseOrders({
      keyword: purchaseOrderKeyword.value,
      status: purchaseOrderStatus.value,
      createdFrom: purchaseOrderCreatedFrom.value,
      createdTo: purchaseOrderCreatedTo.value,
      page: 1,
      size: 50
    });
    supplierPurchaseOrders.value = response.items;
  } catch {
    supplierPurchaseOrders.value = [];
    supplierPurchaseOrderErrorKey.value = "purchaseOrder.error.supplierLoadFailed";
  } finally {
    supplierPurchaseOrderLoading.value = false;
  }
};

const loadPurchaseOrderDetail = async () => {
  if (!purchaseOrderIdFromRoute.value) return;
  purchaseOrderDetailLoading.value = true;
  purchaseOrderDetailError.value = "";
  try {
    purchaseOrderDetail.value = await getPurchaseOrderDetail(purchaseOrderIdFromRoute.value);
  } catch (error) {
    purchaseOrderDetail.value = null;
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.detailLoadFailed");
  } finally {
    purchaseOrderDetailLoading.value = false;
  }
};

const ensurePurchaseOrdersLoaded = () => {
  if (pageKey.value !== "orders") return;
  if (isPurchaseOrderDetailPage.value) {
    void loadPurchaseOrderDetail();
    return;
  }
  if (purchaseOrderWorkspaceMode.value === "supplier") {
    if (!supplierPurchaseOrderLoading.value && !supplierPurchaseOrders.value.length && !supplierPurchaseOrderErrorKey.value) {
      void loadSupplierPurchaseOrders();
    }
    return;
  }
  if (!purchaseOrderLoading.value && !purchaseOrders.value.length && !purchaseOrderErrorKey.value) {
    void loadPurchaseOrders();
  }
};

const reloadPurchaseOrderWorkspace = () => {
  if (purchaseOrderWorkspaceMode.value === "supplier") {
    void loadSupplierPurchaseOrders();
  } else {
    void loadPurchaseOrders();
  }
};

const resetPurchaseOrderSearch = async () => {
  purchaseOrderKeyword.value = "";
  purchaseOrderStatus.value = "";
  purchaseOrderSupplier.value = "";
  purchaseOrderCreatedFrom.value = "";
  purchaseOrderCreatedTo.value = "";
  purchaseOrderDeliveryFrom.value = "";
  purchaseOrderDeliveryTo.value = "";
  if (purchaseOrderWorkspaceMode.value === "supplier") {
    await loadSupplierPurchaseOrders();
  } else {
    await loadPurchaseOrders();
  }
};

const openPurchaseOrderDetail = (row: PurchaseOrderSummary | Record<string, unknown>) => {
  const orderId = "purchaseOrderId" in row ? row.purchaseOrderId : undefined;
  if (!orderId) return;
  router.push(`/orders/${orderId}`);
};

const openSupplierActionDialog = (supplierOrder: PurchaseSupplierOrder, type: "confirm" | "reject") => {
  supplierActionOrder.value = supplierOrder;
  supplierActionType.value = type;
  supplierRejectReason.value = "";
  supplierConfirmForm.value = {
    expectedReadyAt: supplierOrder.expectedReadyAt || "",
    discountType: supplierOrder.discountType || "",
    discountValue: supplierOrder.discountValue == null ? "" : String(supplierOrder.discountValue),
    packagingMethod: supplierOrder.packagingMethod || "SUPPLIER_PACKAGING",
    supplierRemark: supplierOrder.supplierRemark || ""
  };
};

const closeSupplierActionDialog = () => {
  supplierActionOrder.value = null;
  supplierActionType.value = "";
  supplierRejectReason.value = "";
};

const submitSupplierAction = async () => {
  const supplierOrder = supplierActionOrder.value;
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  if (!supplierOrder || !orderId) return;
  supplierActionSaving.value = true;
  purchaseOrderDetailError.value = "";
  try {
    if (supplierActionType.value === "confirm") {
      if (!supplierConfirmForm.value.expectedReadyAt) {
        purchaseOrderDetailError.value = t("purchaseOrder.error.expectedReadyAtRequired");
        return;
      }
      purchaseOrderDetail.value = await confirmSupplierPurchaseOrder(orderId, supplierOrder.supplierOrderId, {
        expectedReadyAt: supplierConfirmForm.value.expectedReadyAt,
        discountType: supplierConfirmForm.value.discountType || undefined,
        discountValue: supplierConfirmForm.value.discountValue === "" ? undefined : Number(supplierConfirmForm.value.discountValue),
        packagingMethod: supplierConfirmForm.value.packagingMethod,
        supplierRemark: supplierConfirmForm.value.supplierRemark
      });
    } else {
      const reason = supplierRejectReason.value.trim();
      if (!reason) {
        purchaseOrderDetailError.value = t("purchaseOrder.error.rejectReasonRequired");
        return;
      }
      purchaseOrderDetail.value = await rejectSupplierPurchaseOrder(orderId, supplierOrder.supplierOrderId, { rejectReason: reason });
    }
    closeSupplierActionDialog();
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
  } finally {
    supplierActionSaving.value = false;
  }
};

const settlementColumns = computed<TableColumn[]>(() => [
  { key: "code", label: t("table.settlementNo"), width: "140px" },
  { key: "type", label: t("table.settlementType"), width: "112px" },
  { key: "sourceNo", label: t("table.sourcePurchaseNo"), width: "138px" },
  { key: "vesselName", label: t("table.vesselName"), width: "140px" },
  { key: "supplier", label: t("table.serviceProvider"), width: "180px" },
  { key: "amount", label: t("table.settlementAmount"), width: "116px", align: "right" },
  { key: "status", label: t("field.status"), width: "112px" },
  { key: "applyDate", label: t("table.applyDate"), width: "116px" },
  { key: "settlementDate", label: t("table.settlementDate"), width: "116px" },
  { key: "operation", label: t("common.operation"), width: "82px", align: "center" }
]);

const procurementFlowTitleKey = computed(() => {
  const keys: Record<string, string> = {
    inquiries: "nav.inquiries",
    quotes: "nav.quotes",
    comparisonList: "nav.compare",
    orders: "nav.orders",
    foodInquiries: "nav.foodInquiries",
    foodQuotes: "nav.foodQuotes",
    foodComparisonList: "nav.foodCompare",
    foodOrders: "nav.foodOrders",
    crewServices: "nav.crewServices"
  };
  return keys[pageKey.value] ?? "nav.inquiries";
});

const procurementFlowSubtitleKey = computed(() => {
  if (pageKey.value === "crewServices") return "page.crewServices.staticList";
  return pageKey.value.startsWith("food") ? "page.procurementFlow.foodStaticList" : "page.procurementFlow.materialStaticList";
});

const openProcurementFlowRow = (row: ProcurementFlowRow) => {
  if (pageKey.value === "foodComparisonList") {
    router.push("/procurement/requests/RFQ-240604/compare");
    return;
  }
  openSimpleDetail(String(row.code || row.inquiryNo || "-"), String(row.subject || row.supplier || ""));
};

const openSkuPreview = (sku: SupplierSku) => {
  previewTitle.value = `${sku.name} / ${sku.itemNo}`;
  previewImages.value = sku.images;
  previewAttributes.value = sku.attributes;
  previewOpen.value = true;
};

const openImagePreview = (title: string, src: string, alt: string, attributes: SkuAttribute[] = []) => {
  if (!src) {
    shopErrorMessage.value = t("page.supplierProducts.noImage");
    return;
  }
  previewTitle.value = title || t("action.previewImage");
  previewImages.value = [{ src, alt: alt || title || t("action.previewImage") }];
  previewAttributes.value = attributes;
  previewOpen.value = true;
};

const openShopSkuImagePreview = (row: ShopSkuRow) => {
  openImagePreview(row.productName || row.supplierSkuCode || t("page.supplierProducts.skuList"), row.thumbnail, row.productName || row.supplierSkuCode);
};

const openCompanyQualificationImagePreview = (row: CompanyQualificationRow) => {
  openImagePreview(row.name || row.fileName || t("page.supplierProducts.qualification.title"), row.imagePreviewUrl || row.fileUrl, row.name || row.fileName);
};

const getShopSkuRowId = (row: ShopSkuRow) => row.id;

const toggleShopSkuRow = (row: ShopSkuRow) => {
  const rowId = getShopSkuRowId(row);
  expandedShopSkuId.value = expandedShopSkuId.value === rowId ? "" : rowId;
};

const openAttributes = (sku: SupplierSku) => {
  drawerTitle.value = sku.name;
  drawerSubtitle.value = `${sku.impaCode} / ${sku.itemNo}`;
  drawerAttributes.value = sku.attributes;
  drawerFileLinks.value = [];
  drawerOpen.value = true;
};

const openSimpleDetail = (title: string, subtitle = "") => {
  drawerTitle.value = title;
  drawerSubtitle.value = subtitle;
  drawerAttributes.value = [];
  drawerFileLinks.value = [];
  drawerOpen.value = true;
};

const runAction = () => {
  loading.value = true;
  window.setTimeout(() => {
    loading.value = false;
    confirmOpen.value = true;
  }, 420);
};

const keepInquiryStaticNotice = () => {
  // The inquiry page is intentionally static until a real inquiry API is available.
};

const formatEmpty = (value?: string | null) => value || "-";

const formatDemandDateTime = (value?: string | null) => {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
};

const demandStatusLabel = (status?: string | null) => {
  const value = String(status || "").toUpperCase();
  const keys: Record<string, string> = {
    SAVED: "page.materialDemand.statusSaved",
    DRAFT: "page.materialDemand.statusDraft",
    SUBMITTED: "page.materialDemand.statusSubmitted",
    PENDING_REVIEW: "page.materialDemand.statusPending",
    COMPLETED: "page.materialDemand.statusCompleted",
    CANCELLED: "page.materialDemand.statusCancelled"
  };
  return keys[value] ? t(keys[value]) : status || "-";
};

const demandStatusVariant = (status?: string | null): StatusVariant => {
  const value = String(status || "").toUpperCase();
  if (value === "SAVED" || value === "COMPLETED") return "success";
  if (value === "SUBMITTED" || value === "PENDING_REVIEW" || value === "DRAFT") return "warning";
  if (value === "CANCELLED") return "danger";
  return "info";
};

const getImpaCategoryLabel = (categoryCode: string) => {
  const category = impaCategories.value.find((item) => item.code === categoryCode);
  return category ? `${category.code} / ${category.nameCn}` : formatEmpty(categoryCode);
};

const toggleImpaDetail = (item: ImpaStandardItem) => {
  expandedImpaCode.value = expandedImpaCode.value === item.impaCode ? "" : item.impaCode;
};

const loadImpaItems = async (categoryCode: string, keyword = impaKeyword.value) => {
  if (!categoryCode && !keyword.trim()) {
    impaItems.value = [];
    expandedImpaCode.value = "";
    return;
  }

  impaItemsLoading.value = true;
  try {
    impaItems.value = await getImpaStandardItems({ categoryCode, keyword, limit: 50 });
    expandedImpaCode.value = "";
  } finally {
    impaItemsLoading.value = false;
  }
};

const selectImpaCategory = async (category: StandardCategoryNode) => {
  selectedImpaCategoryCode.value = category.code;
  await loadImpaItems(category.code);
};

const searchImpaItems = async () => {
  await loadImpaItems(selectedImpaCategoryCode.value);
};

const clearImpaSearch = async () => {
  impaKeyword.value = "";
  await loadImpaItems(selectedImpaCategoryCode.value, "");
};

const loadImpaCategories = async (loadItems = true) => {
  impaCategoriesLoading.value = true;
  try {
    const categories = await getImpaStandardCategories();
    impaCategories.value = categories;
    if (!loadItems) return;
    const activeCategory = categories.find((category) => category.code === selectedImpaCategoryCode.value) ?? categories[0];
    if (activeCategory) {
      await selectImpaCategory(activeCategory);
      return;
    }
    selectedImpaCategoryCode.value = "";
    impaItems.value = [];
  } finally {
    impaCategoriesLoading.value = false;
  }
};

const getPermissionLabel = (item: PermissionRole | PermissionMenuNode | PermissionPoint) => {
  if ("nameKey" in item && item.nameKey) return t(item.nameKey);
  if ("labelKey" in item && item.labelKey) return t(item.labelKey);
  if ("name" in item && item.name) return item.name;
  if ("label" in item && item.label) return item.label;
  return "code" in item ? item.code : item.key;
};

const isTechnicalPermissionText = (value?: string) => {
  const text = value?.trim();
  if (!text) return true;
  return (
    text.startsWith("/") ||
    text.startsWith("nav.") ||
    text.startsWith("permission.") ||
    /^[A-Z0-9_:-]+$/.test(text)
  );
};

const translatedPermissionLabel = (key?: string) => {
  if (!key) return "";
  const translated = t(key);
  return translated !== key && !isTechnicalPermissionText(translated) ? translated : "";
};

const getPermissionMenuLabel = (menu: PermissionMenuNode) => {
  const labelFromKey = translatedPermissionLabel(menu.labelKey);
  if (labelFromKey) return labelFromKey;
  if (menu.label && !isTechnicalPermissionText(menu.label)) return menu.label;
  const labelFromRoute = translatedPermissionLabel(`nav.${menu.key}`);
  return labelFromRoute || t("permission.unnamedMenu");
};

const permissionMenuOptions = computed(() => {
  const flatten = (menus: PermissionMenuNode[]): Array<{ key: string; code: string; label: string; level: number }> =>
    menus.flatMap((menu) => {
      const current = menu.key
        ? [{ key: menu.key, code: menu.key, label: getPermissionMenuLabel(menu), level: 0 }]
        : [];
      const children = (menu.children ?? [])
        .filter((child) => child.key)
        .map((child) => ({ key: child.key, code: child.key, label: getPermissionMenuLabel(child), level: 1 }));
      return [...current, ...children];
    });

  const seen = new Set<string>();
  return flatten(permissionMenus.value).filter((item) => {
    if (seen.has(item.code)) return false;
    seen.add(item.code);
    return true;
  });
});

const menuOrderRows = computed(() => {
  const flatten = (menus: PermissionMenuNode[], level = 0): Array<Record<string, unknown> & { key: string; menuCode: string; name: string; sortOrder: number; status: string; level: number }> =>
    menus.flatMap((menu) => [
      {
        key: menu.key,
        menuCode: menu.key,
        name: getPermissionMenuLabel(menu),
        sortOrder: menu.sortOrder ?? 0,
        status: "enabled",
        level
      },
      ...flatten(menu.children ?? [], level + 1)
    ]);
  return flatten(menuOrderMenus.value);
});

const menuOrderColumns = computed<TableColumn[]>(() => [
  { key: "name", label: t("permission.menuName") },
  { key: "menuCode", label: t("permission.menuCode"), width: "170px" },
  { key: "sortOrder", label: t("permission.sortOrder"), width: "106px", align: "center" },
  { key: "status", label: t("field.status"), width: "92px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "112px", align: "center" }
]);

const formatMenuSortOrder = (value: unknown) => String(Number(value) || 0).padStart(2, "0");

const renumberMenuOrders = (items: PermissionMenuNode[]) =>
  items.map((item, index) => ({
    ...item,
    sortOrder: index * 10
  }));

const moveMenuOrder = (menuCode: string, direction: -1 | 1) => {
  const index = menuOrderMenus.value.findIndex((menu) => menu.key === menuCode);
  const targetIndex = index + direction;
  if (index < 0 || targetIndex < 0 || targetIndex >= menuOrderMenus.value.length) return;
  const next = [...menuOrderMenus.value];
  const [item] = next.splice(index, 1);
  next.splice(targetIndex, 0, item);
  menuOrderMenus.value = renumberMenuOrders(next);
  menuOrderErrorKey.value = "";
};

const flattenMenuManagementRows = (menus: PermissionMenuNode[], parentCode = "", level = 1): MenuManagementRow[] =>
  menus.flatMap((menu) => [
    {
      id: `${parentCode || "root"}-${menu.key}`,
      menuCode: menu.key,
      menuName: getPermissionMenuLabel(menu),
      level,
      parentCode,
      sortOrder: menu.sortOrder ?? 0,
      enabled: menu.enabled !== false
    },
    ...flattenMenuManagementRows(menu.children ?? [], menu.key, level + 1)
  ]);

const loadMenuManagementRows = async () => {
  menuManagementLoading.value = true;
  menuManagementNoticeKey.value = "";
  try {
    const menus = await getManageableAdminMenus();
    menuManagementRows.value = flattenMenuManagementRows(menus);
  } finally {
    menuManagementLoading.value = false;
  }
};

const ensureMenuManagementLoaded = () => {
  if (pageKey.value === "menuManagement" && !menuManagementRows.value.length && !menuManagementLoading.value) {
    void loadMenuManagementRows();
  }
};

const updateMenuManagementRow = (menuCode: string, patch: Partial<MenuManagementRow>) => {
  menuManagementRows.value = menuManagementRows.value.map((row) => (row.menuCode === menuCode ? { ...row, ...patch } : row));
  menuManagementNoticeKey.value = "";
};

const saveMenuManagementDraft = () => {
  menuManagementNoticeKey.value = "menuManagement.pendingApi";
};

const isPermissionChecked = (code: string) => selectedPermissionCodes.value.includes(code);

const togglePermissionCode = (code: string) => {
  selectedPermissionCodes.value = isPermissionChecked(code)
    ? selectedPermissionCodes.value.filter((item) => item !== code)
    : [...selectedPermissionCodes.value, code];
};

const isStaticPermissionChecked = (code: string) => staticPermissionCodes.value.includes(code);

const toggleStaticPermissionCode = (code: string) => {
  staticPermissionCodes.value = isStaticPermissionChecked(code)
    ? staticPermissionCodes.value.filter((item) => item !== code)
    : [...staticPermissionCodes.value, code];
  staticPermissionNoticeKey.value = "";
};

const selectStaticPermissionUser = (row: Record<string, unknown>) => {
  const nextId = String(row.id || "");
  selectedStaticPermissionUserId.value = nextId;
  expandedStaticPermissionUserId.value = expandedStaticPermissionUserId.value === nextId ? "" : nextId;
  staticPermissionNoticeKey.value = "";
};

const openStaticUserPanel = (mode: "create" | "edit" | "reset" | "delete") => {
  staticUserPanelMode.value = mode;
  staticPermissionNoticeKey.value = "permission.staticPreviewOnly";
};

const selectAllStaticPermissions = () => {
  staticPermissionCodes.value = staticPermissionGroups.value.flatMap((group) => group.items.map((item) => item.code));
  staticPermissionNoticeKey.value = "";
};

const clearStaticPermissions = () => {
  staticPermissionCodes.value = [];
  staticPermissionNoticeKey.value = "";
};

const restoreDefaultStaticPermissions = () => {
  staticPermissionCodes.value = ["dashboard", "materials", "requests", "impa"];
  staticPermissionNoticeKey.value = "permission.staticPreviewOnly";
};

const saveStaticUserPermissions = () => {
  staticPermissionSaving.value = true;
  staticPermissionNoticeKey.value = "permission.staticPreviewOnly";
  window.setTimeout(() => {
    staticPermissionSaving.value = false;
  }, 450);
};

const loadSelectedRolePermissions = async (roleCode = selectedPermissionRoleCode.value) => {
  selectedPermissionRoleCode.value = roleCode;
  const codes = await getRoleMenuPermissions(roleCode);
  selectedPermissionCodes.value = codes;
};

const loadPermissionWorkspace = async () => {
  permissionLoading.value = true;
  try {
    const [roles, menus, manageableMenus, permissions, users] = await Promise.all([
      getAdminRoles(),
      getAdminMenuPermissions(),
      getManageableAdminMenus(),
      getAdminPermissions(),
      getAdminUsers()
    ]);
    permissionRoles.value = roles;
    permissionMenus.value = menus;
    menuOrderMenus.value = manageableMenus;
    permissionPoints.value = permissions;
    selectedPermissionRoleCode.value = roles[0]?.code || selectedPermissionRoleCode.value;
    selectedAdminUserId.value = users[0]?.id || selectedAdminUserId.value;
    selectedAdminUserRoleCode.value = users[0]?.roleCodes[0] || roles[0]?.code || selectedAdminUserRoleCode.value;
    adminUsers.value = users;
    await loadSelectedRolePermissions(selectedPermissionRoleCode.value);
  } finally {
    permissionLoading.value = false;
  }
};

const ensurePermissionsLoaded = () => {
  if (pageKey.value === "permissions" && !permissionRoles.value.length && !permissionLoading.value) {
    void loadPermissionWorkspace();
  }
};

const loadMaterialDemands = async () => {
  materialDemandLoading.value = true;
  materialDemandErrorKey.value = "";
  try {
    const response = await listMaterialDemands({
      keyword: materialDemandKeyword.value,
      page: 1,
      size: 50
    });
    materialDemands.value = response.items;
  } catch {
    materialDemands.value = [];
    materialDemandErrorKey.value = "page.materialDemand.requestFailed";
  } finally {
    materialDemandLoading.value = false;
  }
};

const loadInquiryDemands = async () => {
  inquiryDemandLoading.value = true;
  inquiryDemandErrorKey.value = "";
  try {
    const response = await listMaterialDemands({
      keyword: inquiryDemandKeyword.value,
      status: inquiryDemandStatus.value,
      dateFrom: inquiryDemandDateFrom.value,
      dateTo: inquiryDemandDateTo.value,
      page: 1,
      size: 50
    });
    inquiryDemandRows.value = response.items;
  } catch {
    inquiryDemandRows.value = [];
    inquiryDemandErrorKey.value = "page.inquiries.loadFailed";
  } finally {
    inquiryDemandLoading.value = false;
  }
};

const loadComparisonDemands = async () => {
  comparisonDemandLoading.value = true;
  comparisonDemandErrorKey.value = "";
  try {
    const response = await listMaterialDemands({
      keyword: comparisonDemandKeyword.value,
      status: comparisonDemandStatus.value,
      dateFrom: comparisonDemandDateFrom.value,
      dateTo: comparisonDemandDateTo.value,
      page: 1,
      size: 50
    });
    comparisonDemandRows.value = response.items;
  } catch {
    comparisonDemandRows.value = [];
    comparisonDemandErrorKey.value = "page.compareManagement.loadFailed";
  } finally {
    comparisonDemandLoading.value = false;
  }
};

const ensureMaterialDemandsLoaded = () => {
  if (pageKey.value === "requests" && !materialDemandLoading.value && (!materialDemands.value.length || highlightedDemandId.value)) {
    void loadMaterialDemands();
  }
};

const ensureInquiryDemandsLoaded = () => {
  if (pageKey.value === "inquiries" && !inquiryDemandLoading.value && !inquiryDemandRows.value.length && !inquiryDemandErrorKey.value) {
    void loadInquiryDemands();
  }
};

const ensureComparisonDemandsLoaded = () => {
  if (pageKey.value === "comparisonList" && !comparisonDemandLoading.value && !comparisonDemandRows.value.length && !comparisonDemandErrorKey.value) {
    void loadComparisonDemands();
  }
};

const resetMaterialDemandSearch = async () => {
  materialDemandKeyword.value = "";
  await loadMaterialDemands();
};

const resetInquiryDemandSearch = async () => {
  inquiryDemandKeyword.value = "";
  inquiryDemandStatus.value = "";
  inquiryDemandDateFrom.value = "";
  inquiryDemandDateTo.value = "";
  await loadInquiryDemands();
};

const resetComparisonDemandSearch = async () => {
  comparisonDemandKeyword.value = "";
  comparisonDemandStatus.value = "";
  comparisonDemandDateFrom.value = "";
  comparisonDemandDateTo.value = "";
  await loadComparisonDemands();
};

const openInquiryDemand = (row: MaterialDemandSummary | Record<string, unknown>) => {
  const demandId = "demandId" in row ? row.demandId : undefined;
  if (!demandId) return;
  router.push({ path: "/procurement/materials", query: { demandId: String(demandId) } });
};

const openComparisonDemand = (row: MaterialDemandSummary | Record<string, unknown>) => {
  const demandId = "demandId" in row ? row.demandId : undefined;
  if (!demandId) return;
  router.push(`/procurement/requests/${demandId}/compare`);
};

const saveSelectedRolePermissions = async () => {
  if (!selectedPermissionRoleCode.value) return;
  permissionSaving.value = true;
  try {
    await saveRoleMenuPermissions(selectedPermissionRoleCode.value, selectedPermissionCodes.value);
    permissionMenus.value = await getAdminMenuPermissions();
    await loadSelectedRolePermissions(selectedPermissionRoleCode.value);
    confirmOpen.value = true;
  } finally {
    permissionSaving.value = false;
  }
};

const saveMenuSortOrders = async () => {
  menuOrderSaving.value = true;
  menuOrderErrorKey.value = "";
  try {
    await saveAdminMenuSortOrders(menuOrderMenus.value.map((menu) => ({ menuCode: menu.key, sortOrder: menu.sortOrder ?? 0 })));
    menuOrderMenus.value = await getManageableAdminMenus();
    permissionMenus.value = await getAdminMenuPermissions();
    confirmOpen.value = true;
  } catch {
    menuOrderErrorKey.value = "permission.menuOrderSaveFailed";
  } finally {
    menuOrderSaving.value = false;
  }
};

const assignSelectedUserRole = async () => {
  if (!selectedAdminUserId.value || !selectedAdminUserRoleCode.value) return;
  permissionSaving.value = true;
  try {
    await assignUserRoles(selectedAdminUserId.value, [selectedAdminUserRoleCode.value]);
    adminUsers.value = adminUsers.value.map((user) =>
      user.id === selectedAdminUserId.value ? { ...user, roleCodes: [selectedAdminUserRoleCode.value] } : user
    );
  } finally {
    permissionSaving.value = false;
  }
};

const loadRegistrations = async () => {
  registrationLoading.value = true;
  registrationErrorKey.value = "";
  try {
    registrations.value = await getAdminRegistrations({
      keyword: registrationKeyword.value,
      status: registrationStatusFilter.value,
      page: 1,
      pageSize: 50
    });
    if (!registrations.value.some((item) => item.id === expandedRegistrationId.value)) {
      expandedRegistrationId.value = "";
    }
  } catch (error) {
    registrations.value = [];
    registrationErrorKey.value = getRegistrationErrorKey(error);
  } finally {
    registrationLoading.value = false;
  }
};

const searchRegistrations = async () => {
  await loadRegistrations();
};

const selectRegistrationStatus = async (status: string) => {
  if (registrationStatusFilter.value === status && !registrationLoading.value) return;
  registrationStatusFilter.value = status;
  await loadRegistrations();
};

const ensureRegistrationsLoaded = () => {
  if (pageKey.value === "registrations" && !registrations.value.length && !registrationLoading.value && !registrationErrorKey.value) {
    void loadRegistrations();
  }
};

const getRegistrationErrorKey = (error: unknown) => {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      clearAuthSession();
      return "registration.error.sessionExpired";
    }
    if (error.status === 403) return "registration.error.forbidden";
  }
  return "registration.error.requestFailed";
};

const getRegistrationStatusKey = (status: AdminRegistration["status"]) => `registration.status.${status}`;

const getRegistrationStatusVariant = (status: AdminRegistration["status"]): StatusVariant => {
  if (status === "approved") return "success";
  if (status === "rejected") return "danger";
  return "warning";
};

const getRegistrationCompanyTypeLabel = (companyType: string) => {
  const keyByType: Record<string, string> = {
    SHIP_AGENT: "page.register.companyTypeShipAgent",
    SUPPLIER: "page.register.companyTypeSupplier",
    BARGE_AGENT: "page.register.companyTypeBargeAgent"
  };
  return t(keyByType[companyType] || "page.register.companyType");
};

const openRegistrationFile = async (file: { name: string; url: string }) => {
  registrationErrorKey.value = "";
  try {
    const session = getAuthSession();
    const headers = new Headers();
    if (session?.token) headers.set("Authorization", `Bearer ${session.token}`);
    const response = await fetch(file.url, { headers });
    if (!response.ok) {
      throw new ApiError(`HTTP_${response.status}`, response.status);
    }
    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    const opened = window.open(objectUrl, "_blank", "noopener,noreferrer");
    if (!opened) {
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = file.name;
      anchor.click();
    }
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  } catch (error) {
    registrationErrorKey.value = getRegistrationErrorKey(error) === "registration.error.requestFailed"
      ? "registration.error.fileOpenFailed"
      : getRegistrationErrorKey(error);
  }
};

const getRegistrationFileLinks = (registration: AdminRegistration) =>
  registration.qualificationFileItems
    .map((file, index) => ({
      key: file.fileId || `${registration.id}-${index}`,
      name: file.fileName || file.fileId || t("registration.qualificationFiles"),
      url: file.url || (file.fileId ? `/api/files/${encodeURIComponent(file.fileId)}` : "")
    }))
    .filter((file) => file.url);

const toggleRegistrationDetail = (registration: AdminRegistration) => {
  expandedRegistrationId.value = expandedRegistrationId.value === registration.id ? "" : registration.id;
};

const approveSelectedRegistration = async (registration: AdminRegistration) => {
  registrationErrorKey.value = "";
  registrationActionLoading.value = true;
  registrationActionId.value = registration.id;
  try {
    await approveRegistration(registration.id);
    await loadRegistrations();
  } catch (error) {
    registrationErrorKey.value = getRegistrationErrorKey(error);
  } finally {
    registrationActionLoading.value = false;
    registrationActionId.value = "";
  }
};

const openRejectDialog = (registration: AdminRegistration) => {
  selectedRegistrationId.value = registration.id;
  rejectReason.value = "";
  rejectReasonError.value = false;
  rejectDialogOpen.value = true;
};

const submitRejectRegistration = async () => {
  const reason = rejectReason.value.trim();
  rejectReasonError.value = !reason;
  if (!selectedRegistration.value || !reason) return;

  registrationErrorKey.value = "";
  registrationActionLoading.value = true;
  registrationActionId.value = selectedRegistration.value.id;
  try {
    await rejectRegistration(selectedRegistration.value.id, reason);
    rejectDialogOpen.value = false;
    await loadRegistrations();
  } catch (error) {
    registrationErrorKey.value = getRegistrationErrorKey(error);
  } finally {
    registrationActionLoading.value = false;
    registrationActionId.value = "";
  }
};

const getCompanyMemberStatusKey = (status: CompanyMemberStatus) => `companyMembers.status.${status}`;

const getCompanyMemberStatusVariant = (status: CompanyMemberStatus): StatusVariant => {
  if (status === "ACTIVE") return "success";
  if (status === "DISABLED" || status === "LEFT") return "danger";
  if (status === "PENDING" || status === "INVITED" || status === "PASSWORD_RESET_REQUIRED") return "warning";
  return "info";
};

const getCompanyRoleLabel = (roleCode: string) => {
  const role = companyRoles.value.find((item) => item.code === roleCode);
  return role?.name || roleCode;
};

const getCompanyMemberErrorKey = (error: unknown, fallbackKey = "companyMembers.error.requestFailed") => {
  if (!(error instanceof ApiError)) return fallbackKey;

  if (error.status === 401) return "companyMembers.error.sessionExpired";
  if (error.status === 403) return "companyMembers.error.forbidden";
  if (error.status === 409) return "companyMembers.error.accountExists";
  if (error.status === 400) return "companyMembers.error.invalidRole";
  if (error.status === 404) return "companyMembers.error.notFound";
  return fallbackKey;
};

const handleCompanyMemberError = (error: unknown, fallbackKey = "companyMembers.error.requestFailed") => {
  if (error instanceof ApiError && error.status === 401) {
    clearAuthSession();
  }
  companyMemberErrorKey.value = getCompanyMemberErrorKey(error, fallbackKey);
};

const loadCompanyMemberWorkspace = async () => {
  companyMemberLoading.value = true;
  companyMemberErrorKey.value = "";
  try {
    const [roles, members] = await Promise.all([
      getCompanyRoles(),
      getCompanyMembers({
        keyword: companyMemberKeyword.value,
        status: companyMemberStatusFilter.value,
        roleCode: companyMemberRoleFilter.value
      })
    ]);
    companyRoles.value = roles;
    companyMembers.value = members;
  } catch (error) {
    companyMembers.value = [];
    handleCompanyMemberError(error);
  } finally {
    companyMemberLoading.value = false;
  }
};

const ensureCompanyMembersLoaded = () => {
  if (pageKey.value === "companyMembers" && !companyMemberLoading.value && (!companyMembers.value.length || !companyRoles.value.length)) {
    void loadCompanyMemberWorkspace();
  }
};

const searchCompanyMembers = () => {
  void loadCompanyMemberWorkspace();
};

const resetCompanyMemberFilters = () => {
  companyMemberKeyword.value = "";
  companyMemberStatusFilter.value = "";
  companyMemberRoleFilter.value = "";
  void loadCompanyMemberWorkspace();
};

const openCreateCompanyMember = () => {
  memberDrawerMode.value = "create";
  selectedCompanyMemberId.value = "";
  memberForm.value = {
    username: "",
    name: "",
    phone: "",
    email: "",
    password: "Temp@123456",
    roleCodes: companyRoles.value[0]?.code ? [companyRoles.value[0].code] : []
  };
  memberFormErrors.value = {};
  memberDrawerOpen.value = true;
};

const openCompanyMemberRoles = (member: CompanyMember) => {
  memberDrawerMode.value = "roles";
  selectedCompanyMemberId.value = member.id;
  memberForm.value = {
    username: member.username,
    name: member.name,
    phone: member.phone,
    email: member.email,
    password: "",
    roleCodes: [...member.roleCodes]
  };
  memberFormErrors.value = {};
  memberDrawerOpen.value = true;
};

const openCompanyMemberDetail = (member: CompanyMember) => {
  memberDrawerMode.value = "detail";
  selectedCompanyMemberId.value = member.id;
  memberForm.value = {
    username: member.username,
    name: member.name,
    phone: member.phone,
    email: member.email,
    password: "",
    roleCodes: [...member.roleCodes]
  };
  memberFormErrors.value = {};
  memberDrawerOpen.value = true;
};

const validateCompanyMemberForm = () => {
  const errors: Record<string, string> = {};
  if (memberDrawerMode.value === "create") {
    if (!memberForm.value.username.trim()) errors.username = "companyMembers.error.accountRequired";
    if (!memberForm.value.phone.trim()) errors.phone = "companyMembers.error.phoneRequired";
    if (!memberForm.value.password.trim()) errors.password = "companyMembers.error.passwordRequired";
  }
  if (!memberForm.value.roleCodes.length) errors.roleCodes = "companyMembers.error.roleRequired";
  memberFormErrors.value = errors;
  return Object.keys(errors).length === 0;
};

const submitCompanyMemberDrawer = async () => {
  if (memberDrawerMode.value === "detail") {
    memberDrawerOpen.value = false;
    return;
  }
  if (!validateCompanyMemberForm()) return;

  companyMemberSaving.value = true;
  companyMemberErrorKey.value = "";
  try {
    if (memberDrawerMode.value === "create") {
      await createCompanyMember({
        username: memberForm.value.username.trim(),
        name: memberForm.value.name.trim() || undefined,
        phone: memberForm.value.phone.trim(),
        email: memberForm.value.email.trim() || undefined,
        password: memberForm.value.password,
        roleCodes: memberForm.value.roleCodes
      });
      companyMemberNoticeKey.value = "companyMembers.notice.created";
    } else if (selectedCompanyMember.value) {
      await updateCompanyMemberRoles(selectedCompanyMember.value.id, memberForm.value.roleCodes);
      companyMemberNoticeKey.value = "companyMembers.notice.rolesSaved";
    }
    memberDrawerOpen.value = false;
    await loadCompanyMemberWorkspace();
  } catch (error) {
    handleCompanyMemberError(error);
  } finally {
    companyMemberSaving.value = false;
  }
};

const openCompanyMemberConfirm = (member: CompanyMember, action: "enable" | "disable" | "reset") => {
  selectedCompanyMemberId.value = member.id;
  memberConfirmAction.value = action;
  memberConfirmOpen.value = true;
};

const companyMemberConfirmMessage = computed(() => {
  if (!selectedCompanyMember.value) return "";
  return t(`companyMembers.confirm.${memberConfirmAction.value}`, { account: selectedCompanyMember.value.username });
});

const submitCompanyMemberConfirm = async () => {
  if (!selectedCompanyMember.value) return;
  companyMemberSaving.value = true;
  companyMemberErrorKey.value = "";
  try {
    if (memberConfirmAction.value === "reset") {
      await resetCompanyMemberPassword(selectedCompanyMember.value.id, "New@123456");
      companyMemberNoticeKey.value = "companyMembers.notice.passwordReset";
    } else {
      await updateCompanyMemberStatus(selectedCompanyMember.value.id, memberConfirmAction.value === "enable" ? "ACTIVE" : "DISABLED");
      companyMemberNoticeKey.value = memberConfirmAction.value === "enable" ? "companyMembers.notice.enabled" : "companyMembers.notice.disabled";
    }
    memberConfirmOpen.value = false;
    await loadCompanyMemberWorkspace();
  } catch (error) {
    handleCompanyMemberError(error);
  } finally {
    companyMemberSaving.value = false;
  }
};

const ensureImpaLoaded = () => {
  if (pageKey.value === "impa" && !impaCategories.value.length && !impaCategoriesLoading.value) {
    void loadImpaCategories();
  }
};

const ensureShopLoaded = () => {
  if (pageKey.value === "supplierProducts" && !shopLoading.value && !shopSkuRows.value.length) {
    void loadShopWorkspace();
  }
  if (pageKey.value === "supplierProducts" && !impaCategories.value.length && !impaCategoriesLoading.value) {
    void loadImpaCategories(false);
  }
};

onMounted(() => {
  if (typeof window !== "undefined") window.addEventListener("keydown", handleShopFullscreenKeydown);
  ensureImpaLoaded();
  ensurePermissionsLoaded();
  ensureRegistrationsLoaded();
  ensureCompanyMembersLoaded();
  ensureMaterialDemandsLoaded();
  ensureInquiryDemandsLoaded();
  ensureComparisonDemandsLoaded();
  ensureMenuManagementLoaded();
  ensureShopLoaded();
  ensureCompareLoaded();
  ensurePurchaseOrdersLoaded();
});
watch(pageKey, () => {
  shopListFullscreen.value = false;
  ensureImpaLoaded();
  ensurePermissionsLoaded();
  ensureRegistrationsLoaded();
  ensureCompanyMembersLoaded();
  ensureMaterialDemandsLoaded();
  ensureInquiryDemandsLoaded();
  ensureComparisonDemandsLoaded();
  ensureMenuManagementLoaded();
  ensureShopLoaded();
  ensureCompareLoaded();
  ensurePurchaseOrdersLoaded();
});
watch(compareDemandId, () => {
  ensureCompareLoaded();
});
watch(purchaseOrderIdFromRoute, () => {
  ensurePurchaseOrdersLoaded();
});
watch(selectedAdminUserId, () => {
  selectedAdminUserRoleCode.value = selectedAdminUser.value?.roleCodes[0] || permissionRoles.value[0]?.code || "admin";
});
</script>

<template>
  <WorkbenchLayout>
    <LoadingOverlay :active="loading" :label="t('common.loadingSaving')">
      <section v-if="pageKey === 'dashboard'" class="dashboard-workspace">
        <div class="dashboard-metrics-grid">
          <article v-for="metric in metrics" :key="metric.labelKey" :class="['metric-card', 'dashboard-metric-card', `is-${metric.status}`]">
            <span>{{ t(metric.labelKey) }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ t(metric.noteKey) }}</small>
          </article>
        </div>
        <div class="dashboard-body-grid">
          <div class="dashboard-main-column">
            <ExpandablePanel :title="t('nav.requests')" class="dashboard-request-panel">
              <div class="dashboard-request-toolbar list-search-toolbar">
                <label class="dashboard-request-filter single-filter-field">
                  <span>{{ t("filter.requestNo") }}</span>
                  <input v-model="dashboardRequestFilters.requestNo" type="search" :placeholder="t('filter.placeholderRequest')" />
                </label>
                <label class="dashboard-request-filter single-filter-field">
                  <span>{{ t("field.item") }}</span>
                  <input v-model="dashboardRequestFilters.item" type="search" :placeholder="t('dashboard.filters.itemPlaceholder')" />
                </label>
                <label class="dashboard-request-filter single-filter-field">
                  <span>{{ t("field.vessel") }}</span>
                  <input v-model="dashboardRequestFilters.vessel" type="search" :placeholder="t('filter.placeholderVessel')" />
                </label>
                <label class="dashboard-request-filter single-filter-field">
                  <span>{{ t("field.status") }}</span>
                  <input v-model="dashboardRequestFilters.status" type="search" :placeholder="t('dashboard.filters.statusPlaceholder')" />
                </label>
                <div class="dashboard-request-actions">
                  <IconButton icon="RefreshCw" :label="t('action.reset')" @click="resetDashboardRequestFilters" />
                  <IconButton icon="Plus" :label="t('action.add')" variant="primary" @click="router.push('/procurement/materials')" />
                </div>
              </div>
              <DataTable class="dashboard-request-table" :columns="dashboardRequestColumns" :rows="dashboardRequestRows" @row-click="(row) => openSimpleDetail(String(row.requestNo), String(row.status))">
                <template #cell-status="{ row }">
                  <StatusBadge :label="t(`status.${row.status}`)" variant="info" />
                </template>
                <template #cell-operation="{ row }">
                  <div class="icon-action-row">
                    <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openSimpleDetail(String(row.requestNo), String(row.status))" />
                  </div>
                </template>
              </DataTable>
            </ExpandablePanel>
            <article class="work-card dashboard-ais-card">
              <div class="dashboard-module-head">
                <h2>{{ t("dashboard.shipPosition.title") }}</h2>
                <div class="dashboard-ship-search">
                  <input
                    v-model="dashboardVesselQuery"
                    type="search"
                    :placeholder="t('dashboard.shipPosition.placeholder')"
                    @keyup.enter="queryDashboardVessel"
                  />
                  <button type="button" class="primary-button" @click="queryDashboardVessel">
                    {{ t("dashboard.shipPosition.query") }}
                  </button>
                </div>
              </div>
              <div class="dashboard-ais-map" :aria-label="t('dashboard.shipPosition.mapLabel')">
                <div class="dashboard-ais-topline">
                  <strong>{{ t("dashboard.shipPosition.port") }}</strong>
                  <span>{{ t("dashboard.shipPosition.simulation") }}</span>
                  <time datetime="2026-06-08T10:30:00">2026-06-08 10:30</time>
                </div>
                <svg class="dashboard-ais-chart" viewBox="0 0 1000 360" role="img" :aria-label="t('dashboard.shipPosition.mapLabel')">
                  <defs>
                    <linearGradient id="aisWater" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stop-color="#eef9ff" />
                      <stop offset="100%" stop-color="#dff1fb" />
                    </linearGradient>
                    <pattern id="aisGrid" width="50" height="50" patternUnits="userSpaceOnUse">
                      <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#b9d8f2" stroke-width="1" opacity="0.56" />
                    </pattern>
                  </defs>
                  <rect width="1000" height="360" rx="18" fill="url(#aisWater)" />
                  <rect width="1000" height="360" rx="18" fill="url(#aisGrid)" />
                  <path d="M50 250 C150 196 214 216 314 172 C433 120 524 142 626 94 C746 38 842 62 952 28" fill="none" stroke="#6aaad6" stroke-width="3" stroke-dasharray="10 12" opacity="0.85" />
                  <path d="M94 312 C215 274 312 302 420 256 C550 200 640 236 758 182 C846 142 902 158 962 116" fill="none" stroke="#7ebbdc" stroke-width="2" stroke-dasharray="8 10" opacity="0.7" />
                  <path d="M0 52 C98 28 150 64 250 36 C382 0 506 20 640 0 L1000 0 L1000 88 C846 120 736 104 612 136 C480 170 350 126 214 164 C112 194 52 174 0 198 Z" fill="#c8e8f8" opacity="0.78" />
                  <path d="M0 322 C88 290 196 306 300 280 C430 248 570 278 698 230 C804 190 914 210 1000 178 L1000 360 L0 360 Z" fill="#d4edf8" opacity="0.95" />
                  <path d="M642 138 C700 114 758 128 812 104 C850 88 902 94 934 72" fill="none" stroke="#4e9dcc" stroke-width="10" stroke-linecap="round" opacity="0.28" />
                  <text x="74" y="88" class="dashboard-ais-zone">{{ t("dashboard.shipPosition.outerAnchorage") }}</text>
                  <text x="690" y="148" class="dashboard-ais-zone">{{ t("dashboard.shipPosition.zhoushanPortArea") }}</text>
                  <text x="764" y="284" class="dashboard-ais-zone">{{ t("dashboard.shipPosition.berthPoint") }}</text>
                </svg>
                <button
                  v-for="vessel in dashboardVessels"
                  :key="vessel.name"
                  type="button"
                  :class="['dashboard-vessel-marker', { 'is-active': vessel.name === selectedDashboardVessel.name }]"
                  :style="{ left: `${vessel.x}%`, top: `${vessel.y}%` }"
                  @click="selectedDashboardVesselName = vessel.name"
                >
                  <span class="dashboard-vessel-arrow" :style="{ transform: `rotate(${vessel.rotation}deg)` }" aria-hidden="true"></span>
                  <strong>{{ vessel.name }}</strong>
                </button>
                <div class="dashboard-vessel-info">
                  <span>{{ selectedDashboardVessel.name }}</span>
                  <dl>
                    <div>
                      <dt>{{ t("dashboard.shipPosition.speed") }}</dt>
                      <dd>{{ selectedDashboardVessel.speed }}</dd>
                    </div>
                    <div>
                      <dt>{{ t("dashboard.shipPosition.course") }}</dt>
                      <dd>{{ selectedDashboardVessel.course }}</dd>
                    </div>
                    <div>
                      <dt>{{ t("dashboard.shipPosition.distance") }}</dt>
                      <dd>{{ selectedDashboardVessel.distance }}</dd>
                    </div>
                  </dl>
                  <small>{{ t(selectedDashboardVessel.statusKey) }}</small>
                </div>
              </div>
            </article>
          </div>
          <aside class="dashboard-side-column">
            <article class="work-card dashboard-calendar-card">
              <div class="dashboard-calendar-head">
                <h2>{{ t("dashboard.calendar.title") }}</h2>
                <div class="dashboard-calendar-actions">
                  <button
                    type="button"
                    :disabled="!canGoPreviousCalendarMonth"
                    :aria-label="t('dashboard.calendar.previousMonth')"
                    :title="t('dashboard.calendar.previousMonth')"
                    @click="changeCalendarMonth(-1)"
                  >
                    &lt;
                  </button>
                  <strong>{{ calendarMonthLabel }}</strong>
                  <button
                    type="button"
                    :aria-label="t('dashboard.calendar.nextMonth')"
                    :title="t('dashboard.calendar.nextMonth')"
                    @click="changeCalendarMonth(1)"
                  >
                    &gt;
                  </button>
                </div>
              </div>
              <div class="dashboard-calendar-grid" role="grid" :aria-label="t('dashboard.calendar.title')">
                <span v-for="weekday in calendarWeekdays" :key="weekday" class="dashboard-calendar-weekday">{{ weekday }}</span>
                <button
                  v-for="day in calendarDays"
                  :key="day.key"
                  type="button"
                  :class="{
                    'is-empty': !day.day,
                    'is-today': day.isToday,
                    'is-past': day.isPast,
                    'is-selected': day.dateKey === selectedCalendarDate
                  }"
                  :disabled="!day.isSelectable"
                  @click="selectCalendarDate(day.dateKey, day.isSelectable)"
                >
                  <span v-if="day.day" class="dashboard-calendar-dots" aria-hidden="true">
                    <i v-for="reminder in day.reminders" :key="reminder" :class="`is-${reminder}`"></i>
                  </span>
                  <span>{{ day.day || "" }}</span>
                </button>
              </div>
              <div class="dashboard-calendar-reminders">
                <div v-for="reminder in calendarReminderStatuses" :key="reminder" class="dashboard-calendar-reminder">
                  <span :class="['dashboard-reminder-dot', `is-${reminder}`]" aria-hidden="true"></span>
                  <strong>{{ t(`dashboard.calendar.reminders.${reminder}.title`) }}</strong>
                  <small>{{ t(`dashboard.calendar.reminders.${reminder}.content`) }}</small>
                </div>
              </div>
            </article>
            <article class="work-card dashboard-weather-card">
              <div class="dashboard-module-head">
                <h2>{{ t("dashboard.weather.title") }}</h2>
                <span>{{ t("dashboard.weather.updatedAt") }}</span>
              </div>
              <div class="dashboard-weather-list">
                <div v-for="item in dashboardWeatherItems" :key="item.key" class="dashboard-weather-item">
                  <strong>{{ item.label }}</strong>
                  <span>{{ item.value }}</span>
                </div>
              </div>
              <div class="dashboard-berth-window">
                <strong>{{ t("dashboard.weather.berthWindow") }}</strong>
                <span>{{ t("dashboard.weather.berthWindowValue") }}</span>
              </div>
            </article>
          </aside>
        </div>
      </section>

      <ExpandablePanel v-else-if="pageKey === 'impa'" :show-header="false" :show-expand="false" class="impa-library-panel">
        <div class="impa-library-workspace">
          <div class="library-toolbar list-search-toolbar">
            <label class="library-search list-search-field">
              <span class="list-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input
                v-model="impaKeyword"
                type="search"
                :placeholder="t('filter.placeholderKeyword')"
                @keyup.enter="searchImpaItems"
              />
            </label>
            <div class="toolbar-icon-actions">
              <IconButton icon="Search" :label="t('action.search')" variant="primary" :loading="impaItemsLoading" @click="searchImpaItems" />
              <IconButton icon="RefreshCw" :label="t('action.refresh')" @click="loadImpaItems(selectedImpaCategoryCode)" />
            </div>
          </div>
          <div class="library-shell">
            <nav class="library-tree">
              <button
                v-for="category in impaCategories"
                :key="category.code"
                type="button"
                :class="{ active: category.code === selectedImpaCategoryCode }"
                @click="selectImpaCategory(category)"
              >
                <strong>{{ category.code }}</strong>
                <span>{{ category.nameCn }}</span>
                <em>{{ category.itemCount }}</em>
              </button>
              <div v-if="impaCategoriesLoading || impaCategories.length === 0" class="library-empty">
                {{ impaCategoriesLoading ? t("common.loading") : t("common.empty") }}
              </div>
            </nav>
            <section class="library-detail">
              <DataTable
                :columns="impaItemColumns"
                :rows="impaItems"
                :loading="impaItemsLoading"
                row-key="impaCode"
                :expanded-row-key="expandedImpaCode"
                @row-click="toggleImpaDetail"
              >
                <template #cell-specification="{ value }">
                  {{ value || "-" }}
                </template>
                <template #cell-unit="{ value }">
                  {{ value || "-" }}
                </template>
                <template #cell-operation="{ row }">
                  <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="toggleImpaDetail(row)" />
                </template>
                <template #expanded-row="{ row }">
                  <div class="impa-row-detail">
                    <div>
                      <span>{{ t("field.impaCode") }}</span>
                      <strong>{{ row.impaCode }}</strong>
                    </div>
                    <div>
                      <span>{{ t("field.item") }}</span>
                      <strong>{{ row.nameCn }}</strong>
                    </div>
                    <div>
                      <span>{{ t("attr.spec") }}</span>
                      <strong>{{ formatEmpty(row.specification) }}</strong>
                    </div>
                    <div>
                      <span>{{ t("field.unit") }}</span>
                      <strong>{{ formatEmpty(row.unit) }}</strong>
                    </div>
                    <div>
                      <span>{{ t("field.categoryCode") }}</span>
                      <strong>{{ getImpaCategoryLabel(row.categoryCode) }}</strong>
                    </div>
                    <div>
                      <span>{{ t("field.segmentCode") }}</span>
                      <strong>{{ row.segmentCode }}</strong>
                    </div>
                    <div>
                      <span>{{ t("field.englishName") }}</span>
                      <strong>-</strong>
                    </div>
                    <div>
                      <span>{{ t("field.alias") }}</span>
                      <strong>-</strong>
                    </div>
                    <div>
                      <span>{{ t("field.skuCoverage") }}</span>
                      <strong>-</strong>
                    </div>
                  </div>
                </template>
              </DataTable>
            </section>
          </div>
        </div>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'registrations'" :show-header="false" :show-expand="false" class="registration-admin-panel">
        <section class="registration-workspace">
          <form class="registration-toolbar list-search-toolbar" @submit.prevent="searchRegistrations">
            <label class="registration-search-field list-search-field">
              <span>{{ t("filter.keyword") }}</span>
              <span class="list-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input
                v-model="registrationKeyword"
                type="search"
                :placeholder="t('registration.searchPlaceholder')"
                :disabled="registrationLoading"
              />
            </label>
            <div class="registration-status-filter" role="group" :aria-label="t('registration.statusFilter')">
              <button
                v-for="filter in registrationStatusFilters"
                :key="filter.value"
                type="button"
                :class="[`status-${filter.value.toLowerCase()}`, { active: registrationStatusFilter === filter.value }]"
                :disabled="registrationLoading"
                @click="selectRegistrationStatus(filter.value)"
              >
                {{ filter.label }}
              </button>
            </div>
            <div class="toolbar-icon-actions">
              <IconButton icon="Search" :label="t('action.search')" variant="primary" :loading="registrationLoading" type="submit" />
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="registrationLoading" @click="loadRegistrations" />
            </div>
          </form>
          <p v-if="registrationErrorKey" class="registration-error" role="alert">{{ t(registrationErrorKey) }}</p>
          <DataTable
            :columns="registrationColumns"
            :rows="registrations"
            :loading="registrationLoading"
            :expanded-row-key="expandedRegistrationId"
            row-key="id"
            row-interactive
            @row-click="toggleRegistrationDetail"
          >
            <template #cell-companyType="{ value }">
              {{ getRegistrationCompanyTypeLabel(String(value)) }}
            </template>
            <template #cell-companyName="{ value }">
              <span class="registration-company-name" :title="String(value || '')">{{ value || "-" }}</span>
            </template>
            <template #cell-qualificationFiles="{ row }">
              {{ row.qualificationFiles.length }}
            </template>
            <template #cell-operation="{ row }">
              <div class="icon-action-row">
                <IconButton
                  icon="Check"
                  :label="t('registration.approve')"
                  variant="primary"
                  :disabled="row.status !== 'pending' || registrationActionLoading"
                  :loading="registrationActionLoading && registrationActionId === row.id"
                  @click.stop="approveSelectedRegistration(row)"
                />
                <IconButton
                  icon="X"
                  :label="t('registration.reject')"
                  variant="danger"
                  :disabled="row.status !== 'pending' || registrationActionLoading"
                  @click.stop="openRejectDialog(row)"
                />
              </div>
            </template>
            <template #expanded-row="{ row }">
              <section class="registration-inline-detail">
                <div class="registration-detail-grid">
                  <div>
                    <span>{{ t("registration.account") }}</span>
                    <strong>{{ formatEmpty(row.username) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.companyType") }}</span>
                    <strong>{{ getRegistrationCompanyTypeLabel(row.companyType) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.companyName") }}</span>
                    <strong>{{ formatEmpty(row.companyName) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.contactName") }}</span>
                    <strong>{{ formatEmpty(row.contactName) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.phone") }}</span>
                    <strong>{{ formatEmpty(row.phone) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.email") }}</span>
                    <strong>{{ formatEmpty(row.email) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.accountStatus") }}</span>
                    <strong>{{ formatEmpty(row.accountStatus) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.companyStatus") }}</span>
                    <strong>{{ formatEmpty(row.companyStatus) }}</strong>
                  </div>
                  <div>
                    <span>{{ t("registration.rejectReason") }}</span>
                    <strong>{{ formatEmpty(row.rejectReason) }}</strong>
                  </div>
                </div>
                <div class="registration-inline-files">
                  <span>{{ t("registration.qualificationFiles") }}</span>
                  <div v-if="getRegistrationFileLinks(row).length" class="registration-file-actions">
                    <button
                      v-for="file in getRegistrationFileLinks(row)"
                      :key="file.key"
                      type="button"
                      @click.stop="openRegistrationFile(file)"
                    >
                      {{ file.name }}
                    </button>
                  </div>
                  <strong v-else>{{ t("common.empty") }}</strong>
                </div>
              </section>
            </template>
          </DataTable>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'companyMembers'" :show-header="false" :show-expand="false" class="company-members-panel">
        <LoadingOverlay :active="companyMemberLoading" :label="t('common.loading')">
          <section class="company-members-workspace">
            <div class="company-members-toolbar list-search-toolbar">
              <div class="company-members-filters">
                <label class="member-filter-field member-filter-keyword list-search-field">
                  <span>{{ t("filter.keyword") }}</span>
                  <span class="list-search-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24">
                      <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                    </svg>
                  </span>
                  <input v-model="companyMemberKeyword" type="search" :placeholder="t('companyMembers.filter.keywordPlaceholder')" @keyup.enter="searchCompanyMembers" />
                </label>
                <label class="member-filter-field single-filter-field">
                  <span>{{ t("filter.status") }}</span>
                  <select v-model="companyMemberStatusFilter">
                    <option value="">{{ t("common.all") }}</option>
                    <option value="PENDING">{{ t("companyMembers.status.PENDING") }}</option>
                    <option value="INVITED">{{ t("companyMembers.status.INVITED") }}</option>
                    <option value="ACTIVE">{{ t("companyMembers.status.ACTIVE") }}</option>
                    <option value="DISABLED">{{ t("companyMembers.status.DISABLED") }}</option>
                    <option value="LEFT">{{ t("companyMembers.status.LEFT") }}</option>
                    <option value="PASSWORD_RESET_REQUIRED">{{ t("companyMembers.status.PASSWORD_RESET_REQUIRED") }}</option>
                  </select>
                </label>
                <label class="member-filter-field single-filter-field">
                  <span>{{ t("companyMembers.field.roles") }}</span>
                  <select v-model="companyMemberRoleFilter">
                    <option value="">{{ t("common.all") }}</option>
                    <option v-for="role in companyRoles" :key="role.code" :value="role.code">
                      {{ role.name }}
                    </option>
                  </select>
                </label>
              </div>
              <div class="toolbar-icon-actions">
                <IconButton icon="Search" :label="t('action.search')" :loading="companyMemberLoading" @click="searchCompanyMembers" />
                <IconButton icon="X" :label="t('common.reset')" @click="resetCompanyMemberFilters" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="companyMemberLoading" @click="loadCompanyMemberWorkspace" />
                <IconButton icon="Plus" :label="t('companyMembers.action.create')" variant="primary" @click="openCreateCompanyMember" />
              </div>
            </div>

            <div v-if="companyMemberErrorKey" class="member-state-banner is-error">
              <span>{{ t(companyMemberErrorKey) }}</span>
              <button v-if="companyMemberErrorKey === 'companyMembers.error.sessionExpired'" type="button" @click="router.push('/login')">
                {{ t("page.onboarding.relogin") }}
              </button>
            </div>
            <div v-else-if="companyMemberNoticeKey" class="member-state-banner">
              <span>{{ t(companyMemberNoticeKey) }}</span>
            </div>

            <DataTable :columns="companyMemberColumns" :rows="companyMembers" :loading="companyMemberLoading" row-key="id">
              <template #cell-contact="{ row }">
                <div class="member-contact-cell">
                  <span>{{ row.phone || "-" }}</span>
                  <small>{{ row.email || "-" }}</small>
                </div>
              </template>
              <template #cell-roleCodes="{ row }">
                <div class="member-role-chips">
                  <span v-for="roleCode in row.roleCodes" :key="roleCode">{{ getCompanyRoleLabel(roleCode) }}</span>
                  <em v-if="row.roleCodes.length === 0">-</em>
                </div>
              </template>
              <template #cell-status="{ row }">
                <StatusBadge :label="t(getCompanyMemberStatusKey(row.status))" :variant="getCompanyMemberStatusVariant(row.status)" />
              </template>
              <template #cell-lastLoginAt="{ value }">
                {{ value || "-" }}
              </template>
              <template #cell-source="{ value }">
                {{ value || "-" }}
              </template>
              <template #cell-operation="{ row }">
                <div class="icon-action-row">
                  <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openCompanyMemberDetail(row)" />
                  <IconButton icon="Pencil" :label="t('companyMembers.action.assignRoles')" @click.stop="openCompanyMemberRoles(row)" />
                  <IconButton
                    :icon="row.status === 'DISABLED' ? 'Check' : 'Ban'"
                    :label="row.status === 'DISABLED' ? t('companyMembers.action.enable') : t('companyMembers.action.disable')"
                    :variant="row.status === 'DISABLED' ? 'secondary' : 'danger'"
                    :disabled="row.isOwner"
                    @click.stop="openCompanyMemberConfirm(row, row.status === 'DISABLED' ? 'enable' : 'disable')"
                  />
                  <IconButton icon="RefreshCw" :label="t('companyMembers.action.resetPassword')" :disabled="row.isOwner" @click.stop="openCompanyMemberConfirm(row, 'reset')" />
                </div>
              </template>
            </DataTable>
          </section>
        </LoadingOverlay>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'suppliers'" :show-header="false">
        <FilterToolbar :fields="filterFields" :primary-action="primaryAction" @search="runAction" @reset="runAction" @refresh="runAction" @export="runAction" @primary="runAction" />
        <DataTable :columns="supplierColumns" :rows="suppliers" @row-click="(row) => openSimpleDetail(String(row.name), String(row.port))">
          <template #cell-status="{ row }">
            <StatusBadge :label="t(`status.${row.status}`)" :variant="row.status === 'warning' ? 'warning' : 'success'" />
          </template>
          <template #cell-operation="{ row }">
            <div class="icon-action-row">
              <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openSimpleDetail(String(row.name), String(row.port))" />
              <IconButton icon="Image" :label="t('action.viewProduct')" @click.stop="router.push(`/suppliers/${row.id}/products`)" />
              <IconButton icon="MoreHorizontal" :label="t('action.more')" @click.stop="openSimpleDetail(String(row.name), t('action.more'))" />
            </div>
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'supplierProducts'" :show-header="false">
        <section class="shop-products-workspace">
          <div class="shop-store-card" :aria-busy="shopProfileLoading">
            <div class="shop-store-head">
              <div class="shop-title-field">
                <h2>
                  <span>{{ shopStoreName }}</span>
                  <small v-if="shopCreditCode">&#65288;{{ shopCreditCode }}&#65289;</small>
                </h2>
              </div>
              <div class="toolbar-icon-actions">
                <IconButton icon="Save" :label="t('common.save')" :loading="shopSaving || shopLogoUploading" @click="saveShopProfile" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="shopLoading" @click="loadShopWorkspace" />
              </div>
            </div>
            <div class="shop-store-body">
              <button type="button" class="shop-store-logo" :aria-label="t('page.supplierProducts.logoUpload')" :title="t('page.supplierProducts.logoUpload')" @click="openShopLogoPicker">
                <img v-if="shopLogoPreview" :src="shopLogoPreview" :alt="t('page.supplierProducts.logoAlt')" />
                <span v-else aria-hidden="true">LOGO</span>
              </button>
              <input ref="shopLogoInput" type="file" accept="image/*" hidden @change="handleShopLogoChange" />
              <div class="shop-store-main">
                <div class="shop-profile-fields">
                  <label>
                    <span>{{ t("page.supplierProducts.field.contactName") }}</span>
                    <input v-model="shopProfileForm.contactName" />
                  </label>
                  <label>
                    <span>{{ t("page.supplierProducts.field.contactPhone") }}</span>
                    <input v-model="shopProfileForm.contactPhone" />
                  </label>
                  <label>
                    <span>{{ t("page.supplierProducts.field.contactEmail") }}</span>
                    <input v-model="shopProfileForm.contactEmail" />
                  </label>
                </div>
                <div class="shop-store-metrics">
                  <div v-for="metric in shopMetrics" :key="metric.label" class="shop-store-metric">
                    <span>{{ metric.label }}</span>
                    <strong>{{ metric.value }}</strong>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="shopProfileLoading" class="shop-panel-loading" role="status" aria-live="polite">
              <span class="loading-spinner" aria-hidden="true"></span>
              <strong>{{ t("common.loading") }}</strong>
            </div>
          </div>

          <p v-if="shopErrorMessage" class="permission-static-notice is-error">{{ shopErrorMessage }}</p>
          <p v-if="shopNoticeMessage" class="permission-static-notice">{{ shopNoticeMessage }}</p>
          <p v-if="shopNoticeKey" class="permission-static-notice">{{ t(shopNoticeKey) }}</p>

          <div :class="['shop-list-card', { 'is-section-fullscreen': shopListFullscreen }]" :aria-busy="shopSkuListLoading || companyQualificationLoading">
            <div class="shop-management-bar">
              <div class="shop-management-tabs" role="tablist">
                <button type="button" :class="{ active: shopManagementTab === 'products' }" role="tab" :aria-selected="shopManagementTab === 'products'" @click="shopManagementTab = 'products'">
                  {{ t("page.supplierProducts.skuList") }}
                </button>
                <button type="button" :class="{ active: shopManagementTab === 'qualifications' }" role="tab" :aria-selected="shopManagementTab === 'qualifications'" @click="shopManagementTab = 'qualifications'">
                  {{ t("page.supplierProducts.qualification.title") }}
                </button>
              </div>
              <div v-if="shopManagementTab === 'products'" class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('page.supplierProducts.actionAddSku')" @click="showShopPendingNotice" />
                <IconButton class="shop-bulk-shelf-button is-on" icon="Check" :label="t('page.supplierProducts.actionAllOnShelf')" :disabled="!hasSavedShopSkuRows || shopSaving" @click="updateAllShopShelfStatus('ON_SHELF')" />
                <IconButton class="shop-bulk-shelf-button is-off" icon="Ban" :label="t('page.supplierProducts.actionAllOffShelf')" :disabled="!hasSavedShopSkuRows || shopSaving" @click="updateAllShopShelfStatus('OFF_SHELF')" />
                <IconButton icon="Upload" :label="t('page.supplierProducts.importButton')" @click="openShopImportPicker" />
                <IconButton icon="Save" :label="t('common.save')" :disabled="!canConfirmShopImport" :loading="shopSaving" @click="confirmShopImportPreview" />
                <IconButton :icon="shopListFullscreen ? 'Minimize2' : 'Maximize2'" :label="shopListFullscreen ? t('page.supplierProducts.exitSectionFullscreen') : t('page.supplierProducts.enterSectionFullscreen')" @click="toggleShopListFullscreen" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="shopSkuListLoading" @click="refreshShopSkuList" />
              </div>
              <div v-else class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('page.supplierProducts.qualification.add')" @click="openCompanyQualificationCreate" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="companyQualificationLoading" @click="loadCompanyQualifications" />
              </div>
            </div>
            <input ref="shopImportInput" type="file" accept=".xlsx,.xls,.csv" hidden @change="handleShopImportFile" />
            <input ref="shopSkuImageInput" type="file" accept="image/*" hidden @change="handleShopSkuImageChange" />
            <input ref="companyQualificationFileInput" type="file" accept="image/*" hidden @change="handleCompanyQualificationFileChange" />
            <div v-show="shopManagementTab === 'products'" class="shop-management-pane">
            <div class="shop-list-toolbar">
              <label class="shop-filter-field shop-filter-field--search">
                <span>{{ t("filter.keyword") }}</span>
                <input v-model="shopProductKeyword" :placeholder="t('page.supplierProducts.searchPlaceholder')" />
              </label>
              <label class="shop-filter-field">
                <span>{{ t("page.supplierProducts.filterType") }}</span>
                <select v-model="shopProductTypeFilter">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="MATERIAL">{{ t("page.supplierProducts.typeMaterial") }}</option>
                  <option value="FOOD">{{ t("page.supplierProducts.typeFood") }}</option>
                </select>
              </label>
              <label class="shop-filter-field">
                <span>{{ t("page.supplierProducts.filterCodeStatus") }}</span>
                <select v-model="shopProductCodeStatusFilter">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="MATCHED_SUCCESS">{{ t("page.supplierProducts.codeMatched") }}</option>
                  <option value="PENDING_EXCEPTION">{{ t("page.supplierProducts.codePending") }}</option>
                  <option value="EXCEPTION">{{ t("page.supplierProducts.codeException") }}</option>
                </select>
              </label>
              <label class="shop-filter-field">
                <span>{{ t("page.supplierProducts.field.listingStatus") }}</span>
                <select v-model="shopProductShelfStatusFilter">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="ON_SHELF">{{ t("page.supplierProducts.statusOnShelf") }}</option>
                  <option value="OFF_SHELF">{{ t("page.supplierProducts.statusOffShelf") }}</option>
                </select>
              </label>
              <div class="toolbar-icon-actions">
                <IconButton icon="Search" :label="t('common.search')" :loading="shopSkuListLoading" @click="searchShopSkus" />
                <IconButton icon="X" :label="t('common.reset')" @click="resetShopFilters" />
              </div>
            </div>
            <DataTable
              :columns="shopSkuColumns"
              :rows="filteredShopProductRows"
              :loading="shopSkuListLoading"
              row-key="id"
              :expanded-row-key="expandedShopSkuId"
              :row-class="getShopSkuRowClass"
              row-interactive
              :empty-label="t('page.supplierProducts.emptyImport')"
              @row-click="toggleShopSkuRow"
            >
              <template #cell-image="{ row }">
                <div class="shop-image-editor" @click.stop>
                  <SkuThumbnail :src="row.thumbnail" :alt="row.productName" @preview="openShopSkuImagePreview(row)" />
                  <button type="button" :disabled="shopSkuImageUploading" :title="t('page.supplierProducts.imageReplace')" @click.stop="openShopSkuImagePicker(row)">
                    {{ t("page.supplierProducts.imageReplaceShort") }}
                  </button>
                </div>
              </template>
              <template #cell-category="{ row }">
                <select class="shop-edit-control" :value="row.categoryCode || row.category" @click.stop @change="updateShopRowCategory(row, ($event.target as HTMLSelectElement).value)">
                  <option value="">{{ t("common.notFilled") }}</option>
                  <option v-for="option in getShopCategoryOptionsForRow(row)" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </template>
              <template #cell-platformCode="{ row }">
                <input v-model="row.platformCode" class="shop-edit-control" :placeholder="t('page.supplierProducts.waitingCode')" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-productName="{ row }">
                <div class="shop-product-name-cell">
                  <input v-model="row.productName" class="shop-edit-control shop-edit-control--name" @click.stop @input="markShopSkuDirty(row)" />
                  <span v-if="row.previewAction" :class="['shop-preview-action-pill', `is-${row.previewAction.toLowerCase()}`]">
                    {{ getShopPreviewActionLabel(row.previewAction) }}
                  </span>
                </div>
              </template>
              <template #cell-specs="{ row }">
                <button type="button" class="shop-spec-summary-button" @click.stop="expandedShopSkuId = expandedShopSkuId === row.id ? '' : row.id">
                  {{ getShopSpecsSummary(row) }}
                </button>
              </template>
              <template #cell-stock="{ row }">
                <input v-model.number="row.stock" class="shop-edit-control shop-edit-control--number" type="number" min="0" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-leadTime="{ row }">
                <input v-model.number="row.leadTimeDays" class="shop-edit-control shop-edit-control--number" type="number" min="0" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-deliveryArea="{ row }">
                <input v-model="row.deliveryArea" class="shop-edit-control" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-price="{ row }">
                <div class="shop-price-editor" @click.stop>
                  <select v-model="row.currency" @change="markShopSkuDirty(row)" :aria-label="t('page.supplierProducts.field.currency')">
                    <option value="CNY">{{ getShopCurrencySymbol("CNY") }}</option>
                    <option value="RMB">{{ getShopCurrencySymbol("RMB") }}</option>
                    <option value="USD">$</option>
                  </select>
                  <span>{{ getShopCurrencySymbol(row.currency) }}</span>
                  <input v-model.number="row.price" type="number" min="0" step="0.01" @input="markShopSkuDirty(row)" />
                </div>
              </template>
              <template #cell-brand="{ row }">
                <input v-model="row.brand" class="shop-edit-control" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-unit="{ row }">
                <input v-model="row.unit" class="shop-edit-control" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-packing="{ row }">
                <input v-model="row.packing" class="shop-edit-control" @click.stop @input="markShopSkuDirty(row)" />
              </template>
              <template #cell-operation="{ row }">
                <div class="icon-action-row">
                  <button v-if="row.skuId && !row.isPreview" type="button" :class="['shop-row-shelf-action', row.listingStatus === 'ON_SHELF' ? 'is-off' : 'is-on']" @click.stop="toggleShopShelfStatus(row)">
                    {{ row.listingStatus === "ON_SHELF" ? t("page.supplierProducts.actionOffShelf") : t("page.supplierProducts.actionOnShelf") }}
                  </button>
                  <IconButton v-if="row.skuId && !row.isPreview" icon="Check" :label="t('page.supplierProducts.actionCandidate')" :disabled="isShopMatchedCodeStatus(row.codingStatus)" @click.stop="resolveShopException(row)" />
                  <IconButton v-if="row.skuId && !row.isPreview" icon="Trash2" :label="t('action.delete')" variant="danger" @click.stop="removeShopSku(row)" />
                </div>
              </template>
              <template #expanded-row="{ row, expanded }">
                <div v-if="expanded" class="shop-sku-expanded">
                  <dl>
                    <div v-if="row.previewAction">
                      <dt>{{ t("page.supplierProducts.previewAction") }}</dt>
                      <dd>
                        <span :class="['shop-preview-action-pill', `is-${row.previewAction.toLowerCase()}`]">
                          {{ getShopPreviewActionLabel(row.previewAction) }}
                        </span>
                        <small v-if="getShopPreviewActionHint(row)">{{ getShopPreviewActionHint(row) }}</small>
                      </dd>
                    </div>
                    <div>
                      <dt>{{ t("page.supplierProducts.field.platformCode") }}</dt>
                      <dd>{{ row.platformCode || row.impaCode || "-" }}</dd>
                    </div>
                    <div>
                      <dt>{{ t("page.supplierProducts.field.supplierSkuCode") }}</dt>
                      <dd>
                        <input v-model="row.supplierSkuCode" class="shop-edit-control" @click.stop @input="markShopSkuDirty(row)" />
                      </dd>
                    </div>
                    <div>
                      <dt>{{ t("page.supplierProducts.field.monthlySales") }}</dt>
                      <dd>
                        <input v-model.number="row.monthlySales" class="shop-edit-control shop-edit-control--number" type="number" min="0" @click.stop @input="markShopSkuDirty(row)" />
                      </dd>
                    </div>
                    <div>
                      <dt>{{ t("page.supplierProducts.field.importRowNo") }}</dt>
                      <dd>{{ row.importRowNo || "-" }}</dd>
                    </div>
                    <div>
                      <dt>{{ t("page.supplierProducts.field.productType") }}</dt>
                      <dd>
                        <select v-model="row.productType" class="shop-edit-control" @click.stop @change="markShopSkuDirty(row)">
                          <option value="MATERIAL">{{ t("page.supplierProducts.typeMaterial") }}</option>
                          <option value="FOOD">{{ t("page.supplierProducts.typeFood") }}</option>
                        </select>
                      </dd>
                    </div>
                    <div v-if="getShopPreviewBlockReason(row)" class="shop-sku-expanded__notice">
                      <dt>{{ t("page.supplierProducts.previewBlockReason") }}</dt>
                      <dd>{{ getShopPreviewBlockReason(row) }}</dd>
                    </div>
                    <div v-if="getShopExistingChangeRows(row).length" class="shop-sku-expanded__changes">
                      <dt>{{ t("page.supplierProducts.changeCompare") }}</dt>
                      <dd>
                        <div class="shop-change-list">
                          <div v-for="item in getShopExistingChangeRows(row)" :key="item.key" class="shop-change-item">
                            <span>{{ item.label }}</span>
                            <strong>{{ formatShopDisplayValue(item.oldValue) }}</strong>
                            <i>{{ t("page.supplierProducts.changeArrow") }}</i>
                            <strong>{{ formatShopDisplayValue(item.newValue) }}</strong>
                          </div>
                        </div>
                      </dd>
                    </div>
                    <div class="shop-sku-expanded__specs">
                      <dt class="shop-spec-title">
                        <span>{{ t("page.supplierProducts.field.specs") }}</span>
                        <button type="button" class="shop-spec-add" @click.stop="addShopSpec(row)">{{ t("page.supplierProducts.addSpec") }}</button>
                      </dt>
                      <dd>
                        <div class="shop-spec-editor">
                          <div v-for="(spec, specIndex) in row.specItems" :key="spec.id" class="shop-spec-editor-row">
                            <input v-model="spec.name" :placeholder="t('page.supplierProducts.specName')" @click.stop @input="updateShopSpec(row)" />
                            <input v-model="spec.value" :placeholder="t('page.supplierProducts.specValue')" @click.stop @input="updateShopSpec(row)" />
                            <IconButton icon="Trash2" :label="t('page.supplierProducts.removeSpec')" variant="danger" @click.stop="removeShopSpec(row, specIndex)" />
                          </div>
                        </div>
                      </dd>
                    </div>
                  </dl>
                </div>
              </template>
            </DataTable>
            <div v-if="!hasPendingShopImport && shopSkuTotal > shopSkuPageSize" class="shop-pagination">
              <span>
                {{ t("page.supplierProducts.paginationSummary", { start: shopSkuPageStart, end: shopSkuPageEnd, total: shopSkuTotal }) }}
              </span>
              <div class="shop-pagination__actions">
                <IconButton
                  icon="ChevronLeft"
                  :label="t('page.supplierProducts.prevPage')"
                  :disabled="shopSkuPage <= 1 || shopSkuListLoading"
                  @click="goShopSkuPage(shopSkuPage - 1)"
                />
                <strong>{{ t("page.supplierProducts.pageIndicator", { page: shopSkuPage, total: shopSkuTotalPages }) }}</strong>
                <IconButton
                  icon="ChevronRight"
                  :label="t('page.supplierProducts.nextPage')"
                  :disabled="shopSkuPage >= shopSkuTotalPages || shopSkuListLoading"
                  @click="goShopSkuPage(shopSkuPage + 1)"
                />
              </div>
            </div>
            <div v-if="shopSkuListLoading" class="shop-panel-loading" role="status" aria-live="polite">
              <span class="loading-spinner" aria-hidden="true"></span>
              <strong>{{ t("common.loading") }}</strong>
            </div>
            </div>
            <div v-show="shopManagementTab === 'qualifications'" class="shop-management-pane">
            <DataTable :columns="companyQualificationColumns" :rows="companyQualificationRows" row-key="id" :empty-label="t('page.supplierProducts.qualification.empty')">
              <template #cell-name="{ row }">
                <a v-if="row.fileUrl" class="text-action" :href="row.fileUrl" target="_blank" rel="noreferrer">{{ row.name }}</a>
                <span v-else>{{ row.name }}</span>
              </template>
              <template #cell-image="{ row }">
                <SkuThumbnail :src="row.imagePreviewUrl || row.fileUrl" :alt="row.name || row.fileName || t('page.supplierProducts.qualification.image')" @preview="openCompanyQualificationImagePreview(row)" />
              </template>
              <template #cell-type="{ value }">{{ value || "-" }}</template>
              <template #cell-updatedAt="{ value }">{{ value || "-" }}</template>
              <template #cell-operation="{ row }">
                <div class="icon-action-row">
                  <IconButton icon="Upload" :label="t('page.supplierProducts.qualification.replaceImage')" @click.stop="openCompanyQualificationEdit(row)" />
                  <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" @click.stop="removeCompanyQualification(row)" />
                </div>
              </template>
            </DataTable>
            <div v-if="companyQualificationLoading" class="shop-panel-loading" role="status" aria-live="polite">
              <span class="loading-spinner" aria-hidden="true"></span>
              <strong>{{ t("common.loading") }}</strong>
            </div>
            </div>
          </div>

          <Teleport to="body">
            <div v-if="shopImportOverlayVisible" class="shop-import-backdrop" role="status" aria-live="polite">
              <section class="shop-import-progress-panel" :class="{ 'is-pending': shopImportPending }">
                <div class="shop-import-progress-head">
                  <span>{{ t("page.supplierProducts.importProgressKicker") }}</span>
                  <h2>{{ shopImportPending ? t("page.supplierProducts.importFailedTitle") : t("page.supplierProducts.importProgressTitle") }}</h2>
                  <p>{{ shopImportFileName }}</p>
                </div>
                <ol class="shop-import-progress-stages">
                  <li v-for="(stage, index) in shopImportStages" :key="stage" :class="{ active: index === shopImportStageIndex, done: index < shopImportStageIndex || shopImportProgress === 100 }">
                    <i>{{ index + 1 }}</i>
                    <span>{{ stage }}</span>
                  </li>
                </ol>
                <div class="shop-ship-progress" :style="shopImportProgressStyle">
                  <div class="shop-ship-progress-track">
                    <div class="shop-ship-progress-fill"></div>
                    <div class="shop-ship-runner" aria-hidden="true">
                      <svg viewBox="0 0 64 40">
                        <path class="ship-flag" d="M35 5v13M36 7h15l-4 5 4 5H36" />
                        <path class="ship-body" d="M8 21h45l-6 10H16L8 21Z" />
                        <path class="ship-cabin" d="M23 13h18l5 8H18l5-8Z" />
                        <path class="ship-wave" d="M6 34c5-3 9-3 14 0s9 3 14 0 9-3 14 0 8 3 12 0" />
                      </svg>
                    </div>
                  </div>
                </div>
                <p class="shop-import-progress-note">{{ shopImportPending ? (shopErrorMessage || t("page.supplierProducts.importFailed")) : t("page.supplierProducts.importProgressHint") }}</p>
              </section>
            </div>
          </Teleport>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'requests'" :show-header="false">
        <section class="filter-toolbar" aria-label="material demand filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>{{ t("page.materialDemand.keyword") }}</span>
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="materialDemandKeyword" :placeholder="t('page.materialDemand.searchPlaceholder')" @keydown.enter.prevent="loadMaterialDemands" />
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" :label="t('common.search')" @click="loadMaterialDemands" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetMaterialDemandSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="materialDemandLoading" @click="loadMaterialDemands" />
            <IconButton icon="Plus" :label="t('action.add')" variant="primary" @click="router.push('/procurement/materials')" />
          </div>
        </section>
        <p v-if="materialDemandErrorKey" class="inline-error">{{ t(materialDemandErrorKey) }}</p>
        <DataTable
          :columns="requestColumns"
          :rows="materialDemandRows"
          :loading="materialDemandLoading"
          row-key="demandId"
          row-interactive
          @row-click="(row) => router.push(`/procurement/materials/${row.demandId}`)"
        >
          <template #cell-status="{ row }">
            <StatusBadge :label="String(row.status || '-')" variant="info" />
          </template>
          <template #cell-applicationNo="{ value }">{{ value || "-" }}</template>
          <template #cell-vesselName="{ value }">{{ value || "-" }}</template>
          <template #cell-inquiryDate="{ value }">{{ value || "-" }}</template>
          <template #cell-demandNo="{ row, value }">
            <strong :class="{ 'is-highlight-demand': highlightedDemandId === String(row.demandId) }">{{ value || "-" }}</strong>
          </template>
          <template #cell-operation="{ row }">
            <div class="icon-action-row">
              <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="router.push(`/procurement/materials/${row.demandId}`)" />
            </div>
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'inquiries'" :show-header="false" class="inquiry-management-panel">
        <section class="filter-toolbar" aria-label="inquiry filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>{{ t("page.inquiries.keyword") }}</span>
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="inquiryDemandKeyword" :placeholder="t('page.inquiries.searchPlaceholder')" @keydown.enter.prevent="loadInquiryDemands" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.inquiries.dateFrom") }}</span>
              <input v-model="inquiryDemandDateFrom" type="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.inquiries.dateTo") }}</span>
              <input v-model="inquiryDemandDateTo" type="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.status") }}</span>
              <select v-model="inquiryDemandStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="SAVED">{{ t("page.materialDemand.statusSaved") }}</option>
                <option value="SUBMITTED">{{ t("page.materialDemand.statusSubmitted") }}</option>
                <option value="COMPLETED">{{ t("page.materialDemand.statusCompleted") }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" :label="t('common.search')" @click="loadInquiryDemands" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetInquiryDemandSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="inquiryDemandLoading" @click="loadInquiryDemands" />
            <IconButton icon="Plus" :label="t('action.add')" variant="primary" @click="router.push('/procurement/materials')" />
          </div>
        </section>
        <div v-if="inquiryDemandErrorKey" class="inline-error inline-error--action">
          <span>{{ t(inquiryDemandErrorKey) }}</span>
          <button type="button" @click="loadInquiryDemands">{{ t("page.inquiries.retry") }}</button>
        </div>
        <DataTable
          :columns="inquiryDemandColumns"
          :rows="inquiryDemandTableRows"
          :loading="inquiryDemandLoading"
          :empty-label="inquiryDemandHasFilters ? t('page.inquiries.emptyFiltered') : t('page.inquiries.empty')"
          row-key="demandId"
          row-interactive
          @row-click="(row) => openInquiryDemand(row)"
        >
          <template #cell-demandNo="{ value }">
            <strong>{{ value || "-" }}</strong>
          </template>
          <template #cell-applicationNo="{ value }">{{ value || "-" }}</template>
          <template #cell-vesselName="{ value }">{{ value || "-" }}</template>
          <template #cell-inquiryDate="{ value }">{{ value || "-" }}</template>
          <template #cell-sourceFileName="{ value }">{{ value || "-" }}</template>
          <template #cell-updatedAt="{ value }">{{ formatDemandDateTime(String(value || "")) }}</template>
          <template #cell-status="{ row }">
            <StatusBadge :label="demandStatusLabel(String(row.status || ''))" :variant="demandStatusVariant(String(row.status || ''))" />
          </template>
          <template #cell-operation="{ row }">
            <div class="icon-action-row">
              <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openInquiryDemand(row)" />
            </div>
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'comparisonList'" :show-header="false" class="inquiry-management-panel">
        <section class="filter-toolbar" aria-label="comparison filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>{{ t("page.compareManagement.keyword") }}</span>
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="comparisonDemandKeyword" :placeholder="t('page.compareManagement.searchPlaceholder')" @keydown.enter.prevent="loadComparisonDemands" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.compareManagement.dateFrom") }}</span>
              <input v-model="comparisonDemandDateFrom" type="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.compareManagement.dateTo") }}</span>
              <input v-model="comparisonDemandDateTo" type="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.status") }}</span>
              <select v-model="comparisonDemandStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="SAVED">{{ t("page.materialDemand.statusSaved") }}</option>
                <option value="SUBMITTED">{{ t("page.materialDemand.statusSubmitted") }}</option>
                <option value="COMPLETED">{{ t("page.materialDemand.statusCompleted") }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" :label="t('common.search')" @click="loadComparisonDemands" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetComparisonDemandSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="comparisonDemandLoading" @click="loadComparisonDemands" />
          </div>
        </section>
        <div v-if="comparisonDemandErrorKey" class="inline-error inline-error--action">
          <span>{{ t(comparisonDemandErrorKey) }}</span>
          <button type="button" @click="loadComparisonDemands">{{ t("page.compareManagement.retry") }}</button>
        </div>
        <DataTable
          :columns="inquiryDemandColumns"
          :rows="comparisonDemandTableRows"
          :loading="comparisonDemandLoading"
          :empty-label="comparisonDemandHasFilters ? t('page.compareManagement.emptyFiltered') : t('page.compareManagement.empty')"
          row-key="demandId"
          row-interactive
          @row-click="(row) => openComparisonDemand(row)"
        >
          <template #cell-demandNo="{ value }">
            <strong>{{ value || "-" }}</strong>
          </template>
          <template #cell-applicationNo="{ value }">{{ value || "-" }}</template>
          <template #cell-vesselName="{ value }">{{ value || "-" }}</template>
          <template #cell-inquiryDate="{ value }">{{ value || "-" }}</template>
          <template #cell-sourceFileName="{ value }">{{ value || "-" }}</template>
          <template #cell-updatedAt="{ value }">{{ formatDemandDateTime(String(value || "")) }}</template>
          <template #cell-status="{ row }">
            <StatusBadge :label="demandStatusLabel(String(row.status || ''))" :variant="demandStatusVariant(String(row.status || ''))" />
          </template>
          <template #cell-operation="{ row }">
            <div class="icon-action-row">
              <IconButton icon="Eye" :label="t('action.compare')" @click.stop="openComparisonDemand(row)" />
            </div>
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="procurementFlowPageKeys.has(pageKey)" :show-header="false" class="inquiry-management-panel">
        <div class="inquiry-management-heading">
          <div>
            <strong>{{ t(procurementFlowTitleKey) }}</strong>
            <span>{{ t(procurementFlowSubtitleKey) }}</span>
          </div>
          <p>{{ t("page.inquiries.staticNotice") }}</p>
        </div>
        <FilterToolbar :fields="filterFields" @search="keepInquiryStaticNotice" @reset="keepInquiryStaticNotice" @refresh="keepInquiryStaticNotice" @export="keepInquiryStaticNotice" />
        <DataTable v-if="isProcurementOrderPage" :columns="procurementOrderColumns" :rows="procurementOrderRows" row-key="code" row-interactive @row-click="(row) => openSimpleDetail(String((row as ProcurementOrderRow).code), String((row as ProcurementOrderRow).supplier))">
          <template #cell-amount="{ value }">CNY {{ value }}</template>
          <template #cell-status="{ row }">
            <StatusBadge :label="t(`status.${row.status}`)" :variant="row.status === 'active' ? 'success' : row.status === 'warning' ? 'warning' : 'info'" />
          </template>
          <template #cell-operation="{ row }">
            <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openSimpleDetail(String((row as ProcurementOrderRow).code), String((row as ProcurementOrderRow).supplier))" />
          </template>
        </DataTable>
        <DataTable v-else :columns="procurementFlowColumns" :rows="procurementFlowRows" row-key="code" row-interactive @row-click="(row) => openProcurementFlowRow(row as ProcurementFlowRow)">
          <template #cell-code="{ row, value }">{{ value || row.inquiryNo || "-" }}</template>
          <template #cell-status="{ row }">
            <StatusBadge :label="t(`status.${row.status}`)" :variant="row.status === 'quoted' || row.status === 'compared' || row.status === 'active' ? 'success' : row.status === 'warning' ? 'warning' : 'info'" />
          </template>
          <template #cell-operation="{ row }">
            <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openProcurementFlowRow(row as ProcurementFlowRow)" />
          </template>
        </DataTable>
      </ExpandablePanel>

      <section v-else-if="pageKey === 'compare'" ref="compareWorkspaceRef" class="compare-workspace" @scroll="handleCompareScroll">
        <article class="compare-supply-card">
          <div class="compare-supply-title">
            <strong>{{ t("compare.supply.title") }}</strong>
          </div>
          <dl>
            <div v-for="item in compareSupplyEditableFields" :key="item.key" class="is-editable">
              <dt>{{ item.label }}</dt>
              <dd>
                <input v-model="compareSupplyForm[item.key]" :type="item.inputType" />
              </dd>
            </div>
            <div>
              <dt>{{ compareWeatherInfo.label }}</dt>
              <dd>{{ compareWeatherInfo.value }}</dd>
            </div>
          </dl>
        </article>

        <article class="strategy-panel compare-detail-panel">
          <div v-if="compareError" class="compare-state-message is-error">
            <span>{{ t("compare.loadFailed") }}：{{ compareError }}</span>
            <button type="button" @click="loadCompareWorkspace">{{ t("action.refresh") }}</button>
          </div>
          <div v-else-if="compareLoading" class="compare-state-message">
            {{ t("common.loading") }}
          </div>
          <div v-else-if="!compareStrategyCards.length" class="compare-state-message">
            {{ t("compare.noComparisonData") }}
          </div>
          <div class="compare-bid-area">
            <div class="strategy-choice-group compare-strategy-board" role="group" :aria-label="t('compare.strategyChoice')">
              <template v-for="(strategy, index) in compareStrategyCards" :key="strategy.key">
                <article :class="['strategy-choice', `is-${strategy.tone}`, { active: selectedStrategy === strategy.key }]">
                  <button type="button" class="strategy-choice-main" @click="selectCompareStrategy(strategy.key)">
                    <span>{{ strategy.label }}</span>
                    <div class="strategy-total-stack">
                      <strong>{{ strategy.matchSummary }}</strong>
                      <em>{{ strategy.total }}</em>
                    </div>
                  </button>
                  <ul class="strategy-supplier-list">
                    <li v-for="supplier in strategy.suppliers" :key="supplier.key">
                      <button
                        type="button"
                        :class="{ active: selectedStrategy === strategy.key && selectedCompareSupplier === supplier.key }"
                        @click="selectCompareSupplier(strategy.key, supplier.key)"
                      >
                        <span>{{ supplier.supplier }}</span>
                        <span class="strategy-supplier-meta">
                          <em>{{ t("compare.supplierAmount.skuCount", { count: supplier.skuCount }) }}</em>
                          <strong>{{ supplier.amount }}</strong>
                        </span>
                      </button>
                    </li>
                  </ul>
                </article>
                <span v-if="index === 0" class="strategy-vs">{{ t("compare.vs") }}</span>
              </template>
            </div>
          </div>

          <div class="compare-list-toolbar list-search-toolbar">
            <label class="list-search-field">
              <span>{{ t("compare.skuSearch") }}</span>
              <span class="list-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="compareSkuKeyword" type="search" :placeholder="t('compare.skuSearchPlaceholder')" />
            </label>
            <div class="compare-preference-filters" role="group" :aria-label="t('compare.preferenceFilter')">
              <button
                type="button"
                :class="{ active: comparePreferenceFilters.includes('priceLow') }"
                @click="toggleComparePreferenceFilter('priceLow')"
              >
                {{ t("compare.priceLow") }}
              </button>
              <button
                type="button"
                :class="{ active: comparePreferenceFilters.includes('qualityFirst') }"
                :disabled="true"
                :title="t('compare.qualityPending')"
              >
                {{ t("compare.qualityFirst") }}
              </button>
            </div>
            <div class="toolbar-icon-actions">
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :disabled="compareLoading" @click="loadCompareWorkspace" />
              <IconButton icon="Send" :label="t('purchaseOrder.action.confirmOrder')" variant="primary" :disabled="compareLoading || !selectedCompareOrderableRows.length" @click="openPurchaseOrderDialog" />
            </div>
          </div>
          <p v-if="purchaseOrderNotice" class="inline-success">{{ purchaseOrderNotice }}</p>
          <p v-if="purchaseOrderError" class="inline-error">{{ purchaseOrderError }}</p>

          <DataTable :columns="compareSkuColumns" :rows="compareFilteredSkus" :loading="compareLoading" :empty-label="compareError ? t('compare.loadFailed') : t('compare.noCandidates')">
            <template #cell-impaCode="{ value }">
              <strong class="compare-impa-code">{{ value }}</strong>
            </template>
            <template #cell-thumbnail="{ row }">
              <SkuThumbnail :src="row.thumbnail" :alt="row.name" @preview="openSkuPreview(row)" />
            </template>
            <template #cell-attributes="{ row }">
              <AttributeSummary :attributes="row.attributes" @detail="openAttributes(row)" />
            </template>
            <template #cell-price="{ row }">
              {{ row.price > 0 ? `${row.currency} ${row.price.toLocaleString()}` : "-" }}
              <small class="price-source">{{ row.id === "SKU-150203" ? t("status.formalQuote") : t("status.dailyPrice") }}</small>
            </template>
            <template #cell-operation="{ row }">
              <button class="replace-product-button" type="button" :aria-label="t('compare.replaceProduct')" :title="t('compare.replaceProduct')" @click.stop="openReplacementDialog(row)">
                {{ t("compare.replaceShort") }}
              </button>
            </template>
          </DataTable>
        </article>
        <IconButton
          v-if="showCompareBackTop"
          class="compare-back-top-button"
          icon="ArrowUp"
          :label="t('action.backToTop')"
          @click="scrollCompareToTop"
        />
      </section>

      <ExpandablePanel v-else-if="pageKey === 'orders'" :show-header="false" class="inquiry-management-panel purchase-orders-panel">
        <template v-if="isPurchaseOrderDetailPage">
          <div class="purchase-order-detail-toolbar">
            <button type="button" class="ghost-button" @click="router.push('/orders')">{{ t("purchaseOrder.action.backToList") }}</button>
            <div class="toolbar-icon-actions">
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="purchaseOrderDetailLoading" @click="loadPurchaseOrderDetail" />
            </div>
          </div>
          <div v-if="purchaseOrderDetailError" class="inline-error inline-error--action">
            <span>{{ purchaseOrderDetailError }}</span>
            <button type="button" @click="loadPurchaseOrderDetail">{{ t("purchaseOrder.action.retry") }}</button>
          </div>
          <div v-if="purchaseOrderDetailLoading" class="compare-state-message">{{ t("common.loading") }}</div>
          <section v-else-if="purchaseOrderDetail" class="purchase-order-detail">
            <article class="purchase-order-summary-card">
              <div>
                <span>{{ t("purchaseOrder.field.purchaseOrderNo") }}</span>
                <strong>{{ purchaseOrderDetail.order.purchaseOrderNo || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.sourceNo") }}</span>
                <strong>{{ [purchaseOrderDetail.order.demandNo, purchaseOrderDetail.order.applicationNo].filter(Boolean).join(" / ") || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.vesselName") }}</span>
                <strong>{{ purchaseOrderDetail.order.vesselName || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.supplyPort") }}</span>
                <strong>{{ purchaseOrderDetail.order.supplyPort || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.vesselEta") }}</span>
                <strong>{{ purchaseOrderDetail.order.vesselEta || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.requiredDeliveryTime") }}</span>
                <strong>{{ purchaseOrderDetail.order.requiredDeliveryTime || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.strategy") }}</span>
                <strong>{{ purchaseOrderDetail.order.strategyName || purchaseOrderDetail.order.strategyType || "-" }}</strong>
              </div>
              <div>
                <span>{{ t("purchaseOrder.field.totalAmount") }}</span>
                <strong>{{ formatPurchaseMoney(purchaseOrderDetail.order.totalAmount, purchaseOrderDetail.order.currency) }}</strong>
              </div>
              <div>
                <span>{{ t("field.status") }}</span>
                <StatusBadge :label="purchaseStatusLabel(purchaseOrderDetail.order.status)" :variant="purchaseStatusVariant(purchaseOrderDetail.order.status)" />
              </div>
            </article>

            <article class="purchase-order-section">
              <h3>{{ t("purchaseOrder.section.supplierConfirm") }}</h3>
              <DataTable :columns="purchaseSupplierColumns" :rows="purchaseDetailSupplierRows" row-key="supplierOrderId" :show-index="false">
                <template #cell-status="{ row }">
                  <StatusBadge :label="purchaseStatusLabel(String(row.status || ''))" :variant="purchaseStatusVariant(String(row.status || ''))" />
                </template>
                <template #cell-finalAmount="{ row }">{{ formatPurchaseMoney(Number(row.finalAmount ?? row.subtotalAmount), String(row.currency || "CNY")) }}</template>
                <template #cell-packagingMethod="{ value }">{{ packagingMethodLabel(String(value || "")) }}</template>
                <template #cell-expectedReadyAt="{ value }">{{ value || "-" }}</template>
                <template #cell-operation="{ row }">
                  <div class="table-action-buttons">
                    <button v-if="currentRoles.includes('supplier') && String(row.status || '').toUpperCase() === 'PENDING_SUPPLIER_CONFIRM'" type="button" class="mini-action-button is-success" @click.stop="openSupplierActionDialog(row as unknown as PurchaseSupplierOrder, 'confirm')">
                      {{ t("purchaseOrder.action.supplierConfirm") }}
                    </button>
                    <button v-if="currentRoles.includes('supplier') && String(row.status || '').toUpperCase() === 'PENDING_SUPPLIER_CONFIRM'" type="button" class="mini-action-button is-danger" @click.stop="openSupplierActionDialog(row as unknown as PurchaseSupplierOrder, 'reject')">
                      {{ t("purchaseOrder.action.supplierReject") }}
                    </button>
                  </div>
                </template>
              </DataTable>
            </article>

            <article class="purchase-order-section">
              <h3>{{ t("purchaseOrder.section.items") }}</h3>
              <DataTable :columns="purchaseItemColumns" :rows="purchaseDetailItemRows" row-key="itemId">
                <template #cell-unitPrice="{ row }">{{ formatPurchaseMoney(Number(row.unitPrice), String(row.currency || "CNY")) }}</template>
                <template #cell-amount="{ row }">{{ formatPurchaseMoney(Number(row.amount), String(row.currency || "CNY")) }}</template>
                <template #cell-flags="{ row }">
                  <div class="flag-stack">
                    <StatusBadge v-if="row.unitMismatchFlag" :label="t('purchaseOrder.flag.unitMismatch')" variant="warning" />
                    <StatusBadge v-if="row.quantityFallbackFlag" :label="t('purchaseOrder.flag.quantityFallback')" variant="warning" />
                    <span v-if="!row.unitMismatchFlag && !row.quantityFallbackFlag">-</span>
                  </div>
                </template>
              </DataTable>
            </article>

            <article class="purchase-order-section">
              <h3>{{ t("purchaseOrder.section.timeline") }}</h3>
              <div class="purchase-order-timeline">
                <div v-for="event in purchaseDetailEvents" :key="event.eventId || event.createdAt">
                  <strong>{{ event.eventMessage || event.eventType || "-" }}</strong>
                  <span>{{ formatDemandDateTime(event.createdAt || "") }}</span>
                </div>
                <p v-if="!purchaseDetailEvents.length">{{ t("purchaseOrder.emptyTimeline") }}</p>
              </div>
            </article>
          </section>
        </template>
        <template v-else>
          <section class="filter-toolbar" aria-label="purchase order filters">
            <div class="filter-fields">
              <label class="filter-field filter-field--search">
                <span>{{ t("purchaseOrder.filter.keyword") }}</span>
                <span class="filter-search-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24">
                    <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                  </svg>
                </span>
                <input v-model="purchaseOrderKeyword" :placeholder="t('purchaseOrder.filter.keywordPlaceholder')" @keydown.enter.prevent="reloadPurchaseOrderWorkspace" />
              </label>
              <label class="filter-field">
                <span>{{ t("filter.status") }}</span>
                <select v-model="purchaseOrderStatus">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="PENDING_SUPPLIER_CONFIRM">{{ t("purchaseOrder.status.PENDING_SUPPLIER_CONFIRM") }}</option>
                  <option value="PARTIALLY_CONFIRMED">{{ t("purchaseOrder.status.PARTIALLY_CONFIRMED") }}</option>
                  <option value="PREPARING">{{ t("purchaseOrder.status.PREPARING") }}</option>
                  <option value="PARTIALLY_REJECTED">{{ t("purchaseOrder.status.PARTIALLY_REJECTED") }}</option>
                  <option value="REJECTED">{{ t("purchaseOrder.status.REJECTED") }}</option>
                  <option value="CANCELED">{{ t("purchaseOrder.status.CANCELED") }}</option>
                </select>
              </label>
              <label v-if="purchaseOrderWorkspaceMode === 'buyer'" class="filter-field">
                <span>{{ t("filter.supplier") }}</span>
                <input v-model="purchaseOrderSupplier" :placeholder="t('filter.placeholderSupplier')" />
              </label>
              <label class="filter-field">
                <span>{{ t("purchaseOrder.filter.createdFrom") }}</span>
                <input v-model="purchaseOrderCreatedFrom" type="date" />
              </label>
              <label class="filter-field">
                <span>{{ t("purchaseOrder.filter.createdTo") }}</span>
                <input v-model="purchaseOrderCreatedTo" type="date" />
              </label>
              <label v-if="purchaseOrderWorkspaceMode === 'buyer'" class="filter-field">
                <span>{{ t("purchaseOrder.filter.deliveryFrom") }}</span>
                <input v-model="purchaseOrderDeliveryFrom" type="date" />
              </label>
              <label v-if="purchaseOrderWorkspaceMode === 'buyer'" class="filter-field">
                <span>{{ t("purchaseOrder.filter.deliveryTo") }}</span>
                <input v-model="purchaseOrderDeliveryTo" type="date" />
              </label>
            </div>
            <div class="toolbar-icon-actions">
              <IconButton icon="Search" :label="t('common.search')" @click="reloadPurchaseOrderWorkspace" />
              <IconButton icon="X" :label="t('common.reset')" @click="resetPurchaseOrderSearch" />
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="activePurchaseOrderLoading" @click="reloadPurchaseOrderWorkspace" />
            </div>
          </section>
          <div v-if="activePurchaseOrderErrorKey" class="inline-error inline-error--action">
            <span>{{ t(activePurchaseOrderErrorKey) }}</span>
            <button type="button" @click="reloadPurchaseOrderWorkspace">{{ t("purchaseOrder.action.retry") }}</button>
          </div>
          <DataTable
            :columns="purchaseOrderWorkspaceMode === 'supplier' ? supplierOrderColumns : purchaseOrderColumns"
            :rows="activePurchaseOrderRows"
            :loading="activePurchaseOrderLoading"
            :empty-label="purchaseOrderHasFilters ? t('purchaseOrder.emptyFiltered') : t('purchaseOrder.empty')"
            row-key="purchaseOrderId"
            row-interactive
            @row-click="(row) => openPurchaseOrderDetail(row)"
          >
            <template #cell-purchaseOrderNo="{ value }">
              <strong>{{ value || "-" }}</strong>
            </template>
            <template #cell-totalAmount="{ row }">{{ formatPurchaseMoney(Number(row.totalAmount), String(row.currency || "CNY")) }}</template>
            <template #cell-status="{ row }">
              <StatusBadge :label="purchaseStatusLabel(String(row.status || ''))" :variant="purchaseStatusVariant(String(row.status || ''))" />
            </template>
            <template #cell-createdAt="{ value }">{{ formatDemandDateTime(String(value || "")) }}</template>
            <template #cell-requiredDeliveryTime="{ value }">{{ value || "-" }}</template>
            <template #cell-operation="{ row }">
              <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openPurchaseOrderDetail(row)" />
            </template>
          </DataTable>
        </template>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'settlements'" :show-header="false" class="inquiry-management-panel">
        <FilterToolbar :fields="filterFields" @search="runAction" @reset="runAction" @refresh="runAction" @export="runAction" />
        <p class="static-list-note">{{ t("page.settlements.staticNotice") }}</p>
        <DataTable :columns="settlementColumns" :rows="settlementRows" row-key="code" row-interactive @row-click="(row) => openSimpleDetail(String((row as SettlementRow).code), String((row as SettlementRow).supplier))">
          <template #cell-type="{ value }">{{ t(`settlement.type.${value}`) }}</template>
          <template #cell-amount="{ value }">CNY {{ value }}</template>
          <template #cell-status="{ row }">
            <StatusBadge :label="t(`status.${row.status}`)" :variant="row.status === 'archived' ? 'success' : row.status === 'pending' ? 'warning' : 'info'" />
          </template>
          <template #cell-operation="{ row }">
            <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openSimpleDetail(String((row as SettlementRow).code), String((row as SettlementRow).supplier))" />
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'delivery'" :show-header="false">
        <FilterToolbar :fields="filterFields" @search="runAction" @reset="runAction" @refresh="runAction" @export="runAction" />
        <section class="work-grid">
          <article class="work-card span-2">
            <h2>{{ t("nav.delivery") }}</h2>
            <div class="timeline">
              <div><strong>{{ t("status.ordered") }}</strong><span>2026-06-04 16:00</span></div>
              <div><strong>{{ t("status.delivered") }}</strong><span>2026-06-05 08:30</span></div>
              <div><strong>{{ t("status.archived") }}</strong><span>{{ t("status.pending") }}</span></div>
            </div>
          </article>
          <article class="work-card">
            <h2>{{ t("field.amount") }}</h2>
            <strong class="amount">CNY 18,420</strong>
            <IconButton icon="Check" :label="t('action.archive')" variant="primary" @click="runAction" />
          </article>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'menuManagement'" :show-header="false" :show-expand="false" class="menu-management-panel">
        <div class="menu-management-toolbar list-search-toolbar">
          <div class="menu-management-title">
            <strong>{{ t("nav.menuManagement") }}</strong>
            <span>{{ t("menuManagement.description") }}</span>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="RefreshCw" :label="t('action.refresh')" @click="loadMenuManagementRows" />
            <IconButton icon="Save" :label="t('common.save')" :loading="menuManagementLoading" @click="saveMenuManagementDraft" />
          </div>
        </div>
        <p v-if="menuManagementNoticeKey" class="permission-static-notice">{{ t(menuManagementNoticeKey) }}</p>
        <DataTable :columns="menuManagementColumns" :rows="menuManagementTableRows" :loading="menuManagementLoading" row-key="id">
          <template #cell-menuName="{ row, value }">
            <span :class="['menu-management-name', { 'is-child': Number(row.level) > 1 }]">{{ value }}</span>
          </template>
          <template #cell-level="{ value }">
            <StatusBadge :label="Number(value) === 1 ? t('menuManagement.levelParent') : t('menuManagement.levelChild')" :variant="Number(value) === 1 ? 'info' : 'neutral'" />
          </template>
          <template #cell-parentCode="{ row, value }">
            <select
              class="menu-management-select"
              :value="String(value || '')"
              :disabled="Number(row.level) === 1"
              @change="updateMenuManagementRow(String(row.menuCode), { parentCode: ($event.target as HTMLSelectElement).value, level: ($event.target as HTMLSelectElement).value ? 2 : 1 })"
            >
              <option value="">{{ t("menuManagement.noParent") }}</option>
              <option v-for="parent in menuManagementParentOptions" :key="parent.menuCode" :value="parent.menuCode">
                {{ parent.menuName }}
              </option>
            </select>
          </template>
          <template #cell-sortOrder="{ row, value }">
            <input
              class="menu-management-order-input"
              type="number"
              min="0"
              step="10"
              :value="Number(value)"
              @input="updateMenuManagementRow(String(row.menuCode), { sortOrder: Number(($event.target as HTMLInputElement).value) })"
            />
          </template>
          <template #cell-enabled="{ row, value }">
            <label class="menu-management-switch">
              <input
                type="checkbox"
                :checked="Boolean(value)"
                @change="updateMenuManagementRow(String(row.menuCode), { enabled: ($event.target as HTMLInputElement).checked })"
              />
              <span>{{ Boolean(value) ? t("menuManagement.visibleOn") : t("menuManagement.visibleOff") }}</span>
            </label>
          </template>
          <template #cell-operation="{ row }">
            <div class="icon-action-row">
              <IconButton icon="ArrowUp" :label="t('permission.moveUp')" @click.stop="updateMenuManagementRow(String(row.menuCode), { sortOrder: Math.max(0, Number(row.sortOrder) - 10) })" />
              <IconButton icon="ArrowDown" :label="t('permission.moveDown')" @click.stop="updateMenuManagementRow(String(row.menuCode), { sortOrder: Number(row.sortOrder) + 10 })" />
            </div>
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'permissions'" :show-header="false" :show-expand="false" class="permission-admin-panel">
        <LoadingOverlay :active="permissionLoading" :label="t('common.loading')">
          <section class="permission-admin-workspace">
            <section class="permission-card permission-main">
              <div class="permission-main-toolbar">
                <AnimatedTabs
                  v-model="activeAdminTab"
                  :tabs="[
                    { key: 'users', label: t('permission.userAssignment') },
                    { key: 'menuPermissions', label: t('permission.roleMenuPermissions') },
                    { key: 'menuOrder', label: t('permission.menuOrder') }
                  ]"
                />
                <div class="toolbar-icon-actions">
                  <IconButton
                    v-if="activeAdminTab === 'menuPermissions'"
                    icon="Save"
                    :label="t('action.savePermissions')"
                    variant="primary"
                    :loading="permissionSaving"
                    @click="saveSelectedRolePermissions"
                  />
                  <IconButton
                    v-else-if="activeAdminTab === 'menuOrder'"
                    icon="Save"
                    :label="t('permission.saveMenuOrder')"
                    variant="primary"
                    :loading="menuOrderSaving"
                    @click="saveMenuSortOrders"
                  />
                </div>
              </div>
              <section v-if="activeAdminTab === 'users'" class="permission-user-preview">
                <div class="permission-user-toolbar">
                  <label class="permission-filter-field permission-filter-field--wide">
                    <span>{{ t("permission.accountKeyword") }}</span>
                    <input v-model="permissionUserKeyword" type="search" :placeholder="t('permission.accountKeywordPlaceholder')" />
                  </label>
                  <label class="permission-filter-field">
                    <span>{{ t("field.status") }}</span>
                    <select v-model="permissionUserStatusFilter">
                      <option value="">{{ t("common.all") }}</option>
                      <option value="ACTIVE">{{ t("status.active") }}</option>
                      <option value="DISABLED">{{ t("status.disabled") }}</option>
                    </select>
                  </label>
                  <label class="permission-filter-field">
                    <span>{{ t("permission.accountRoles") }}</span>
                    <select v-model="permissionUserRoleFilter">
                      <option value="">{{ t("common.all") }}</option>
                      <option v-for="role in staticPermissionRoleOptions" :key="role" :value="role">{{ role }}</option>
                    </select>
                  </label>
                  <div class="toolbar-icon-actions">
                    <IconButton icon="Plus" :label="t('permission.addAccount')" @click="openStaticUserPanel('create')" />
                    <IconButton icon="RefreshCw" :label="t('action.refresh')" @click="staticPermissionNoticeKey = 'permission.staticPreviewOnly'" />
                    <IconButton icon="Download" :label="t('action.export')" disabled />
                    <IconButton icon="MoreHorizontal" :label="t('action.more')" disabled />
                  </div>
                </div>

                <section class="permission-user-table-panel">
                  <div class="permission-subpanel-title">
                    <strong>{{ t("permission.enterpriseAccounts") }}</strong>
                    <StatusBadge :label="t('permission.staticPreviewBadge')" variant="info" />
                  </div>
                  <DataTable
                    :columns="permissionUserColumns"
                    :rows="filteredPermissionUsers as unknown as Record<string, unknown>[]"
                    row-key="id"
                    row-interactive
                    :expanded-row-key="expandedStaticPermissionUserId"
                    @row-click="selectStaticPermissionUser"
                  >
                    <template #cell-roles="{ value }">
                      <span>{{ Array.isArray(value) ? value.join(', ') : value }}</span>
                    </template>
                    <template #cell-status="{ value }">
                      <StatusBadge :label="String(value) === 'ACTIVE' ? t('status.active') : t('status.disabled')" :variant="String(value) === 'ACTIVE' ? 'success' : 'neutral'" />
                    </template>
                    <template #cell-operation>
                      <div class="icon-action-row">
                        <IconButton icon="Pencil" :label="t('action.edit')" @click.stop="openStaticUserPanel('edit')" />
                        <IconButton icon="Ban" :label="t('permission.disableOrEnable')" @click.stop="openStaticUserPanel('reset')" />
                        <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" @click.stop="openStaticUserPanel('delete')" />
                      </div>
                    </template>
                    <template #expanded-row="{ row, expanded }">
                      <section v-if="expanded" class="permission-user-expanded">
                        <div class="permission-user-expanded-info">
                          <div class="permission-user-profile">
                            <strong>{{ row.name || "-" }}</strong>
                            <span>{{ row.username || "-" }}</span>
                            <StatusBadge :label="String(row.status) === 'ACTIVE' ? t('status.active') : t('status.disabled')" :variant="String(row.status) === 'ACTIVE' ? 'success' : 'neutral'" />
                          </div>
                          <dl class="permission-user-meta">
                            <div>
                              <dt>{{ t("permission.accountPhone") }}</dt>
                              <dd>{{ row.phone || "-" }}</dd>
                            </div>
                            <div>
                              <dt>{{ t("permission.accountEmail") }}</dt>
                              <dd>{{ row.email || "-" }}</dd>
                            </div>
                            <div>
                              <dt>{{ t("permission.accountRoles") }}</dt>
                              <dd>{{ Array.isArray(row.roles) ? row.roles.join(", ") : row.roles || "-" }}</dd>
                            </div>
                            <div>
                              <dt>{{ t("permission.lastLogin") }}</dt>
                              <dd>{{ row.lastLogin || "-" }}</dd>
                            </div>
                          </dl>
                          <div v-if="staticUserPanelMode" class="permission-static-panel">
                            {{ t("permission.staticPanelHint") }}
                          </div>
                        </div>

                        <div class="permission-user-expanded-permissions">
                          <div class="permission-subpanel-title">
                            <strong>{{ t("permission.menuPermissions") }}</strong>
                            <div class="toolbar-icon-actions">
                              <button type="button" class="text-action" @click.stop="selectAllStaticPermissions">{{ t("permission.selectAll") }}</button>
                              <button type="button" class="text-action" @click.stop="clearStaticPermissions">{{ t("permission.clearAll") }}</button>
                              <button type="button" class="text-action" @click.stop="restoreDefaultStaticPermissions">{{ t("permission.restoreDefault") }}</button>
                              <IconButton icon="Save" :label="t('action.savePermissions')" :loading="staticPermissionSaving" @click.stop="saveStaticUserPermissions" />
                            </div>
                          </div>
                          <p v-if="staticPermissionNoticeKey" class="permission-static-notice">{{ t(staticPermissionNoticeKey) }}</p>
                          <div class="permission-menu-groups">
                            <section v-for="group in staticPermissionGroups" :key="group.key" class="permission-menu-group">
                              <strong>{{ group.name }}</strong>
                              <div class="permission-check-list">
                                <label v-for="item in group.items" :key="item.code" class="permission-check-row" @click.stop>
                                  <input type="checkbox" :checked="isStaticPermissionChecked(item.code)" @change="toggleStaticPermissionCode(item.code)" />
                                  <span>{{ item.label }}</span>
                                </label>
                              </div>
                            </section>
                          </div>
                        </div>
                      </section>
                    </template>
                  </DataTable>
                </section>
              </section>

              <div v-else-if="activeAdminTab === 'menuPermissions'" class="permission-selected-role">
                <strong>{{ selectedPermissionRole ? getPermissionLabel(selectedPermissionRole) : "-" }}</strong>
                <select v-model="selectedPermissionRoleCode" class="permission-inline-select" @change="loadSelectedRolePermissions(selectedPermissionRoleCode)">
                  <option v-for="role in permissionRoles" :key="role.code" :value="role.code">
                    {{ getPermissionLabel(role) }}
                  </option>
                </select>
              </div>
              <div v-if="activeAdminTab === 'menuPermissions'" class="permission-config-grid">
                <section class="permission-subpanel">
                  <div class="permission-subpanel-title">
                    <strong>{{ t("permission.menuPermissions") }}</strong>
                  </div>
                  <div class="permission-check-list">
                    <label v-for="item in permissionMenuOptions" :key="item.code" :class="['permission-check-row', { 'is-child': item.level > 0 }]">
                      <input
                        type="checkbox"
                        :checked="isPermissionChecked(item.code)"
                        @change="togglePermissionCode(item.code)"
                      />
                      <span>{{ item.label }}</span>
                    </label>
                    <p v-if="!permissionMenuOptions.length" class="permission-empty">{{ t("common.empty") }}</p>
                  </div>
                </section>
              </div>
              <section v-else-if="activeAdminTab === 'menuOrder'" class="permission-order-panel">
                <p v-if="menuOrderErrorKey" class="inline-error">{{ t(menuOrderErrorKey) }}</p>
                <DataTable :columns="menuOrderColumns" :rows="menuOrderRows" row-key="menuCode">
                  <template #cell-name="{ row, value }">
                    <span :class="{ 'permission-order-child': Number(row.level) > 0 }">{{ value }}</span>
                  </template>
                  <template #cell-sortOrder="{ value }">
                    <strong>{{ formatMenuSortOrder(value) }}</strong>
                  </template>
                  <template #cell-status>
                    <StatusBadge :label="t('status.active')" variant="success" />
                  </template>
                  <template #cell-operation="{ row }">
                    <div class="icon-action-row">
                      <IconButton icon="ArrowUp" :label="t('permission.moveUp')" :disabled="Number(row.level) > 0 || menuOrderSaving" @click.stop="moveMenuOrder(String(row.menuCode), -1)" />
                      <IconButton icon="ArrowDown" :label="t('permission.moveDown')" :disabled="Number(row.level) > 0 || menuOrderSaving" @click.stop="moveMenuOrder(String(row.menuCode), 1)" />
                    </div>
                  </template>
                </DataTable>
              </section>
            </section>
          </section>
        </LoadingOverlay>
      </ExpandablePanel>
    </LoadingOverlay>

    <DetailDrawer :open="drawerOpen" :title="drawerTitle" :subtitle="drawerSubtitle" width="wide" @close="drawerOpen = false">
      <div v-if="drawerAttributes.length" class="attribute-detail-list">
        <div v-for="attribute in drawerAttributes" :key="attribute.key">
          <span>{{ t(attribute.labelKey) }}</span>
          <strong>{{ attribute.value }}</strong>
        </div>
      </div>
      <div v-if="drawerFileLinks.length" class="drawer-file-list">
        <strong>{{ t("registration.qualificationFiles") }}</strong>
        <button v-for="file in drawerFileLinks" :key="file.key" type="button" @click="openRegistrationFile(file)">
          {{ file.name }}
        </button>
      </div>
      <p v-if="!drawerAttributes.length && !drawerFileLinks.length">{{ t("common.detail") }} / {{ drawerSubtitle }}</p>
    </DetailDrawer>

    <DetailDrawer
      :open="memberDrawerOpen"
      :title="t(`companyMembers.drawer.${memberDrawerMode}`)"
      :subtitle="selectedCompanyMember?.username || t('companyMembers.drawer.subtitle')"
      width="wide"
      @close="memberDrawerOpen = false"
    >
      <div v-if="memberDrawerMode === 'detail' && selectedCompanyMember" class="member-detail-grid">
        <div>
          <span>{{ t("companyMembers.field.account") }}</span>
          <strong>{{ selectedCompanyMember.username }}</strong>
        </div>
        <div>
          <span>{{ t("companyMembers.field.name") }}</span>
          <strong>{{ selectedCompanyMember.name }}</strong>
        </div>
        <div>
          <span>{{ t("companyMembers.field.phone") }}</span>
          <strong>{{ selectedCompanyMember.phone || "-" }}</strong>
        </div>
        <div>
          <span>{{ t("companyMembers.field.email") }}</span>
          <strong>{{ selectedCompanyMember.email || "-" }}</strong>
        </div>
        <div>
          <span>{{ t("registration.companyName") }}</span>
          <strong>{{ selectedCompanyMember.companyName || "-" }}</strong>
        </div>
        <div>
          <span>{{ t("registration.companyType") }}</span>
          <strong>{{ selectedCompanyMember.companyType || "-" }}</strong>
        </div>
        <div>
          <span>{{ t("companyMembers.field.roles") }}</span>
          <strong>{{ selectedCompanyMember.roleCodes.map(getCompanyRoleLabel).join(", ") || "-" }}</strong>
        </div>
        <div>
          <span>{{ t("field.status") }}</span>
          <strong>{{ t(getCompanyMemberStatusKey(selectedCompanyMember.status)) }}</strong>
        </div>
        <div>
          <span>{{ t("companyMembers.field.source") }}</span>
          <strong>{{ selectedCompanyMember.source || "-" }}</strong>
        </div>
        <div>
          <span>{{ t("companyMembers.field.lastLoginAt") }}</span>
          <strong>{{ selectedCompanyMember.lastLoginAt || "-" }}</strong>
        </div>
      </div>
      <form v-else class="member-drawer-form" @submit.prevent="submitCompanyMemberDrawer">
        <label v-if="memberDrawerMode === 'create'" :class="{ 'has-error': memberFormErrors.username }">
          <span>{{ t("companyMembers.field.account") }}</span>
          <input v-model="memberForm.username" type="text" :placeholder="t('companyMembers.placeholder.account')" />
          <small v-if="memberFormErrors.username">{{ t(memberFormErrors.username) }}</small>
        </label>
        <label v-if="memberDrawerMode === 'create'">
          <span>{{ t("companyMembers.field.name") }}</span>
          <input v-model="memberForm.name" type="text" :placeholder="t('companyMembers.placeholder.name')" />
        </label>
        <label v-if="memberDrawerMode === 'create'" :class="{ 'has-error': memberFormErrors.phone }">
          <span>{{ t("companyMembers.field.phone") }}</span>
          <input v-model="memberForm.phone" type="tel" :placeholder="t('companyMembers.placeholder.phone')" />
          <small v-if="memberFormErrors.phone">{{ t(memberFormErrors.phone) }}</small>
        </label>
        <label v-if="memberDrawerMode === 'create'">
          <span>{{ t("companyMembers.field.email") }}</span>
          <input v-model="memberForm.email" type="email" :placeholder="t('companyMembers.placeholder.email')" />
        </label>
        <label v-if="memberDrawerMode === 'create'" :class="{ 'has-error': memberFormErrors.password }">
          <span>{{ t("companyMembers.field.password") }}</span>
          <input v-model="memberForm.password" type="text" :placeholder="t('companyMembers.placeholder.password')" />
          <small v-if="memberFormErrors.password">{{ t(memberFormErrors.password) }}</small>
        </label>
        <section class="member-role-select" :class="{ 'has-error': memberFormErrors.roleCodes }">
          <strong>{{ t("companyMembers.field.roles") }}</strong>
          <label v-for="role in companyRoles" :key="role.code" class="member-check-row">
            <input v-model="memberForm.roleCodes" type="checkbox" :value="role.code" />
            <span>{{ role.name }}</span>
            <em>{{ role.code }}</em>
          </label>
          <small v-if="memberFormErrors.roleCodes">{{ t(memberFormErrors.roleCodes) }}</small>
        </section>
        <footer class="member-drawer-actions">
          <IconButton icon="X" :label="t('common.cancel')" @click="memberDrawerOpen = false" />
          <IconButton icon="Save" :label="t('common.save')" variant="primary" :loading="companyMemberSaving" type="submit" />
        </footer>
      </form>
    </DetailDrawer>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="purchaseOrderDialogOpen" class="modal-backdrop" role="presentation" @click="purchaseOrderDialogOpen = false">
          <section class="purchase-order-confirm-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("purchaseOrder.dialog.title") }}</strong>
                <span>{{ t("purchaseOrder.dialog.subtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="purchaseOrderDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body">
              <section class="purchase-order-dialog-summary">
                <div>
                  <span>{{ t("purchaseOrder.field.strategy") }}</span>
                  <strong>{{ activeCompareStrategy?.label || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("purchaseOrder.dialog.supplierCount") }}</span>
                  <strong>{{ selectedCompareSupplierCount }}</strong>
                </div>
                <div>
                  <span>{{ t("purchaseOrder.dialog.orderableCount") }}</span>
                  <strong>{{ selectedCompareOrderableRows.length }}</strong>
                </div>
                <div>
                  <span>{{ t("purchaseOrder.dialog.excludedCount") }}</span>
                  <strong>{{ selectedCompareExcludedCount }}</strong>
                </div>
                <div>
                  <span>{{ t("purchaseOrder.field.totalAmount") }}</span>
                  <strong>{{ activeCompareStrategy?.total || formatPurchaseMoney(selectedCompareOrderAmount, selectedCompareCurrency) }}</strong>
                </div>
              </section>
              <section v-if="activeCompareStrategy?.suppliers.length" class="purchase-order-supplier-summary">
                <strong>{{ t("purchaseOrder.dialog.supplierSubtotal") }}</strong>
                <div>
                  <span v-for="supplier in activeCompareStrategy.suppliers" :key="supplier.key">
                    {{ supplier.supplier }} · {{ t("compare.supplierAmount.skuCount", { count: supplier.skuCount }) }} · {{ supplier.amount }}
                  </span>
                </div>
              </section>
              <section class="purchase-order-form-grid">
                <label>
                  <span>{{ t("purchaseOrder.field.supplyPort") }}</span>
                  <input v-model="purchaseOrderForm.supplyPort" type="text" :placeholder="t('purchaseOrder.placeholder.supplyPort')" />
                </label>
                <label>
                  <span>{{ t("purchaseOrder.field.vesselEta") }}</span>
                  <input v-model="purchaseOrderForm.vesselEta" type="datetime-local" />
                </label>
                <label>
                  <span>{{ t("purchaseOrder.field.requiredDeliveryTime") }}</span>
                  <input v-model="purchaseOrderForm.requiredDeliveryTime" type="datetime-local" />
                </label>
                <label>
                  <span>{{ t("purchaseOrder.field.defaultPackagingMethod") }}</span>
                  <select v-model="purchaseOrderForm.defaultPackagingMethod">
                    <option value="UNIFIED_PACKAGING">{{ t("purchaseOrder.packaging.UNIFIED_PACKAGING") }}</option>
                    <option value="SUPPLIER_PACKAGING">{{ t("purchaseOrder.packaging.SUPPLIER_PACKAGING") }}</option>
                  </select>
                </label>
                <label class="span-2">
                  <span>{{ t("purchaseOrder.field.buyerRemark") }}</span>
                  <textarea v-model="purchaseOrderForm.buyerRemark" rows="3" :placeholder="t('purchaseOrder.placeholder.buyerRemark')"></textarea>
                </label>
              </section>
              <p v-if="purchaseOrderError" class="inline-error">{{ purchaseOrderError }}</p>
            </div>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="purchaseOrderDialogOpen = false" />
              <IconButton icon="Send" :label="t('purchaseOrder.action.confirmCreate')" variant="primary" :loading="purchaseOrderCreating" @click="submitPurchaseOrder" />
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="supplierActionOrder" class="modal-backdrop" role="presentation" @click="closeSupplierActionDialog">
          <section class="purchase-order-confirm-dialog supplier-action-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ supplierActionType === "confirm" ? t("purchaseOrder.dialog.confirmTitle") : t("purchaseOrder.dialog.rejectTitle") }}</strong>
                <span>{{ supplierActionOrder.supplierOrderNo || supplierActionOrder.supplierName || "-" }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="closeSupplierActionDialog" />
            </header>
            <div class="purchase-order-dialog-body">
              <section v-if="supplierActionType === 'confirm'" class="purchase-order-form-grid">
                <label>
                  <span>{{ t("purchaseOrder.field.expectedReadyAt") }}</span>
                  <input v-model="supplierConfirmForm.expectedReadyAt" type="datetime-local" />
                </label>
                <label>
                  <span>{{ t("purchaseOrder.field.packagingMethod") }}</span>
                  <select v-model="supplierConfirmForm.packagingMethod">
                    <option value="SUPPLIER_PACKAGING">{{ t("purchaseOrder.packaging.SUPPLIER_PACKAGING") }}</option>
                    <option value="UNIFIED_PACKAGING">{{ t("purchaseOrder.packaging.UNIFIED_PACKAGING") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("purchaseOrder.field.discountType") }}</span>
                  <select v-model="supplierConfirmForm.discountType">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option value="AMOUNT">{{ t("purchaseOrder.discount.AMOUNT") }}</option>
                    <option value="PERCENT">{{ t("purchaseOrder.discount.PERCENT") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("purchaseOrder.field.discountValue") }}</span>
                  <input v-model="supplierConfirmForm.discountValue" type="number" min="0" step="0.01" />
                </label>
                <label class="span-2">
                  <span>{{ t("purchaseOrder.field.supplierRemark") }}</span>
                  <textarea v-model="supplierConfirmForm.supplierRemark" rows="3"></textarea>
                </label>
              </section>
              <label v-else class="reject-reason-field">
                <span>{{ t("purchaseOrder.field.rejectReason") }}</span>
                <textarea v-model="supplierRejectReason" rows="4" :placeholder="t('purchaseOrder.placeholder.rejectReason')"></textarea>
              </label>
              <p v-if="purchaseOrderDetailError" class="inline-error">{{ purchaseOrderDetailError }}</p>
            </div>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="closeSupplierActionDialog" />
              <IconButton
                :icon="supplierActionType === 'confirm' ? 'Check' : 'Ban'"
                :label="supplierActionType === 'confirm' ? t('purchaseOrder.action.supplierConfirm') : t('purchaseOrder.action.supplierReject')"
                :variant="supplierActionType === 'confirm' ? 'primary' : 'danger'"
                :loading="supplierActionSaving"
                @click="submitSupplierAction"
              />
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="replacementSku" class="modal-backdrop" role="presentation" @click="replacementSku = null">
          <section class="sku-replacement-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("compare.replaceDialogTitle") }}</strong>
                <span>{{ t("compare.replaceDialogSubtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="replacementSku = null" />
            </header>
            <div class="sku-replacement-body">
              <div class="sku-replacement-impa">
                <span>{{ t("field.impaCode") }}</span>
                <strong>{{ replacementSku.impaCode }}</strong>
              </div>
              <div class="sku-replacement-state">
                <strong>{{ t("compare.skuCandidatePendingTitle") }}</strong>
                <p>{{ t("compare.skuCandidatePendingText") }}</p>
              </div>
            </div>
            <footer>
              <button class="ghost-button" type="button" @click="replacementSku = null">{{ t("common.close") }}</button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <ImagePreviewModal :open="previewOpen" :title="previewTitle" :images="previewImages" :attributes="previewAttributes" @close="previewOpen = false" />

    <ConfirmDialog
      :open="confirmOpen"
      :title="t('common.confirm')"
      :message="t('compare.priceFrozen')"
      @close="confirmOpen = false"
      @confirm="confirmOpen = false"
    />

    <ConfirmDialog
      :open="memberConfirmOpen"
      :title="t(`companyMembers.action.${memberConfirmAction}`)"
      :message="companyMemberConfirmMessage"
      :confirm-label="t('common.confirm')"
      :danger="memberConfirmAction === 'disable'"
      @close="memberConfirmOpen = false"
      @confirm="submitCompanyMemberConfirm"
    />

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="rejectDialogOpen" class="modal-backdrop" role="presentation" @click="rejectDialogOpen = false">
          <section class="reject-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <strong>{{ t("registration.reject") }}</strong>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="rejectDialogOpen = false" />
            </header>
            <label class="reject-reason-field" :class="{ 'has-error': rejectReasonError }">
              <span>{{ t("registration.rejectReason") }}</span>
              <textarea v-model="rejectReason" :placeholder="t('registration.rejectReasonPlaceholder')" rows="4"></textarea>
              <small v-if="rejectReasonError">{{ t("registration.rejectRequired") }}</small>
            </label>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="rejectDialogOpen = false" />
              <IconButton icon="Check" :label="t('registration.reject')" variant="danger" :loading="registrationActionLoading" @click="submitRejectRegistration" />
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </WorkbenchLayout>
</template>
