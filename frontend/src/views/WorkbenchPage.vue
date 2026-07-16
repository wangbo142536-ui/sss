<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
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
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import dashboardShipSprite from "@/assets/dashboard-ship-sprite.png";
import govMapScene from "@/assets/gov-dashboard/code-assets/map-scene.png";
import govRefundScene from "@/assets/gov-dashboard/code-assets/icon/oe7C.png";
import govCardOrdersIcon from "@/assets/gov-dashboard/code-assets/icon/dingdanzhixing.svg";
import govCardSupplierIcon from "@/assets/gov-dashboard/code-assets/icon/gongyingshang.svg";
import govCardAmountIcon from "@/assets/gov-dashboard/code-assets/icon/jiaoyijine.svg";
import govCardTaxIcon from "@/assets/gov-dashboard/code-assets/icon/tuisui.svg";
import {
  crewServiceRows,
  foodComparisonRows,
  foodInquiries,
  foodOrderRows,
  foodQuotes,
  inquiries,
  metrics,
  quoteRows,
  requestRows
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
  createCompanyContact,
  createCompanyQualification,
  createCompanyVessel,
  deleteCompanyContact,
  deleteCompanyQualification,
  deleteCompanyVessel,
  getCompanyProfile,
  getCompanyValueAddedServices,
  listCompanyContacts,
  listCompanyQualifications,
  listCompanyVessels,
  updateCompanyContact,
  updateCompanyProfile,
  updateCompanyQualification,
  updateCompanyValueAddedServices,
  updateCompanyVessel
} from "@/services/companyService";
import {
  deleteDictionaryItem,
  deleteDictionaryType,
  listDictionaryItems,
  listDictionaryTypes,
  saveDictionaryItem,
  saveDictionaryType
} from "@/services/dataDictionaryService";
import { formatSupplyPortDisplay } from "@/utils/portHierarchy";
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
import {
  discardMaterialDemand,
  exportMaterialQuoteTemplate,
  getMaterialDemandDetail,
  getMaterialDemandComparison,
  importMaterialQuoteQuantities,
  listMaterialDemandItemSupplierCandidates,
  listMaterialDemands,
  saveMaterialDemand,
  saveMaterialComparisonQuotes
} from "@/services/procurementMaterialService";
import {
  confirmSupplierPurchaseOrder,
  createPurchaseOrderFromDemand,
  discardPurchaseOrder,
  getPurchaseOrderDetail,
  getSupplierPurchaseOrderDetail,
  listPurchaseOrders,
  listSupplierPurchaseOrders,
  markSupplierOrderReady,
  markSupplierOrderSupplied,
  markSupplierOrderWaitingSupply,
  remindPurchaseOrderSuppliers,
  rejectSupplierPurchaseOrder,
  saveSupplierCustomsDocuments,
  updatePurchaseOrderDeliveryInfo
} from "@/services/purchaseOrderService";
import {
  bookTrafficShuttle,
  cancelTrafficServiceRequest,
  confirmTrafficServiceOrder,
  closeTrafficShuttle,
  completeTrafficShuttle,
  createTrafficBoatPrice,
  createTrafficServiceRequest,
  createTrafficShuttle,
  createTrafficServiceOrder,
  discardTrafficServiceOrder,
  addTrafficRouteStop,
  createTrafficRoute,
  getTrafficServiceRequest,
  getTrafficRoute,
  listSupplierTrafficServiceRequests,
  listSupplierTrafficShuttles,
  listTrafficAnchorages,
  listTrafficBoatPrices,
  listTrafficRoutes,
  listTrafficServiceRequests,
  listTrafficServiceOrders,
  listTrafficShuttles,
  removeTrafficRouteStop,
  selectTrafficServiceQuote,
  submitTrafficServiceQuote,
  startTrafficShuttle,
  updateTrafficBoatPrice,
  updateTrafficBoatPrices,
  updateTrafficRouteStatus,
  updateTrafficShuttleBookingExecution,
  updateTrafficServiceOrder,
  updateTrafficShuttle,
  withdrawTrafficServiceQuote
} from "@/services/trafficService";
import {
  batchUpsertShopSkus,
  deleteShopSku,
  listShopSuppliers,
  listShopSkus,
  previewShopSkuImport,
  resolveShopSkuException,
  updateShopSkuShelfStatus
} from "@/services/shopService";
import { getImpaStandardCategories, getImpaStandardItems } from "@/services/standardLibraryService";
import { createSettlementBatch, deleteSettlement, listSettlements, paySettlement, settleSettlement, submitSettlementInvoice, updateSettlement } from "@/services/settlementService";
import { deleteFulfillmentAttachment, listBargeShuttleAttachments, listPurchaseFulfillmentAttachments, saveBargeNodeAttachments, saveSupplierFulfillmentAttachments } from "@/services/fulfillmentService";
import { listServiceEvaluations, reviewServiceEvaluation, submitServiceEvaluation } from "@/services/evaluationService";
import type { CompanyMember, CompanyMemberStatus, CompanyRole } from "@/services/companyMemberService";
import type { DictionaryItem, DictionaryItemPayload, DictionaryType, DictionaryTypePayload } from "@/services/dataDictionaryService";
import type { AdminRegistration, AdminUser, PermissionMenuNode, PermissionPoint, PermissionRole } from "@/services/permissionService";
import type { PurchaseOrderCreatePayload, PurchaseOrderDetail, PurchaseOrderEvent, PurchaseOrderItem, PurchaseOrderSummary, PurchaseSupplierOrder } from "@/services/purchaseOrderService";
import type { SettlementOrder, SettlementScope } from "@/services/settlementService";
import type { BusinessAttachmentPayload, FulfillmentAttachment } from "@/services/fulfillmentService";
import type { ServiceEvaluation } from "@/services/evaluationService";
import type { TrafficAnchorage, TrafficBoatPrice, TrafficBoatPricePayload, TrafficRouteDetail, TrafficRoutePlan, TrafficRoutePlanPayload, TrafficRouteStop, TrafficServiceOrder, TrafficServiceOrderPayload, TrafficServiceQuotePayload, TrafficServiceRequest, TrafficServiceRequestDetail, TrafficServiceRequestPayload, TrafficShuttleBooking, TrafficShuttleNode, TrafficShuttlePayload, TrafficShuttleService } from "@/services/trafficService";
import type { MaterialComparisonCandidate, MaterialComparisonItem, MaterialComparisonStrategy, MaterialDemandComparisonResponse, MaterialDemandDetail, MaterialDemandSummary, MaterialDemandTrafficService } from "@/types/procurementMaterials";
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

type SupplyChainFinanceStatus = "pending" | "prepaying" | "reviewing" | "funded" | "repaying" | "settled";

type SupplyChainFinanceRow = {
  purchaseOrderNo: string;
  vesselName: string;
  supplierName: string;
  orderAmount: number;
  prepaymentAmount: number;
  financeAmount: number;
  prepaymentRate: string;
  loanTerm: string;
  annualRate: string;
  status: SupplyChainFinanceStatus;
  riskLevel: "low" | "medium";
};

type SupplierInfoRow = {
  id: string;
  companyId?: number;
  name: string;
  port: string;
  category?: string;
  score: string;
  status: string;
  qualificationStatus?: string;
  skuCount?: number;
  contactName?: string;
  contactPhone?: string;
  updatedAt?: string;
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

type CompanyContactRow = {
  id: string;
  contactId?: string | number;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  isEditing?: boolean;
  isNew?: boolean;
  draft: {
    contactName: string;
    contactPhone: string;
    contactEmail: string;
  };
};

type CompanyVesselRow = {
  id: string;
  vesselId?: string | number;
  vesselName: string;
  vesselType: string;
  buildDate: string;
  nextMaintenanceDate: string;
  capacity: string;
  status: string;
  remark: string;
  createdAt: string;
  updatedAt: string;
  isEditing?: boolean;
  isNew?: boolean;
  draft: {
    vesselName: string;
    vesselType: string;
    buildDate: string;
    nextMaintenanceDate: string;
    capacity: string;
    remark: string;
  };
};

type CompareSupplyEditableKey = "vessel" | "inquiryNo" | "materialType" | "port" | "date" | "recipientCompany" | "handlerContact";
type CompareSupplyFormKey = CompareSupplyEditableKey | "currency" | "supplyMode";

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
  costTotal: string;
  fixedFeeTotal: string;
  quoteTotal: string;
  profitTotal: string;
  tone: "green" | "blue";
  suppliers: CompareSupplierCard[];
  enabled: boolean;
  disabledReason?: string;
};

type CompareSkuRow = SupplierSku & {
  supplierKey: string;
  supplierName: string;
  demandItemId: string;
  displayNo?: number;
  unmatched?: boolean;
  sourceSkuCode: string;
  sourceSkuName: string;
  matchedProductCode: string;
  rowNo?: number;
  quantity?: string;
  pricingQuantity?: number;
  quantityMissingFlag?: boolean;
  quantityInvalidFlag?: boolean;
  unit?: string;
  remarks?: string;
  platformUnit?: string;
  selectedUnit?: string;
  unitPriceUsd?: number;
  subtotalUsd?: number;
  actualQuotePrice?: number;
  actualQuoteSubtotal?: number;
  quoteMarkupPercent?: number;
  unitPriceOptions?: NonNullable<MaterialComparisonCandidate["unitPriceOptions"]>;
  unitMismatchFlag?: boolean;
  quantityFallbackFlag?: boolean;
  subtotal: number;
  candidate: MaterialComparisonCandidate;
  lowestPrice?: number;
  matchType?: string;
  reason?: string;
};

const route = useRoute();
const router = useRouter();
const { language } = useI18n();
const pageKey = computed(() => String(route.meta.pageKey || "dashboard"));
const orderVesselSearchPlaceholder = "单号、船舶检索";
const servicePendingPageKeys = new Set([
  "customsServices",
  "portShippingServices",
  "borderInspectionServices",
  "maritimeServices",
  "crewServices",
  "taxServices",
  "financialServices",
  "weatherServices",
  "vesselDynamicsServices",
  "basicManagement"
]);
const servicePendingMeta: Record<string, { labelKey: string; icon: string; title: string; subtitle: string; metric: string }> = {
  customsServices: {
    labelKey: "nav.customsServices",
    icon: "CS",
    title: "海关数据与单证通道正在接入",
    subtitle: "后续将承载报关单证、查验回执、税费状态和口岸协同节点。",
    metric: "关务链路"
  },
  portShippingServices: {
    labelKey: "nav.portShippingServices",
    icon: "PH",
    title: "港航协同服务正在接入",
    subtitle: "后续将覆盖泊位、港区、靠离泊窗口、港航资源和作业计划协同。",
    metric: "港航资源"
  },
  borderInspectionServices: {
    labelKey: "nav.borderInspectionServices",
    icon: "BI",
    title: "边检查验协同正在接入",
    subtitle: "后续将汇聚船员上下船、证件核验、查验预约和通关状态。",
    metric: "边检节点"
  },
  maritimeServices: {
    labelKey: "nav.maritimeServices",
    icon: "MS",
    title: "海事监管服务正在接入",
    subtitle: "后续将承载海事申报、作业许可、安全监管和异常预警协同。",
    metric: "海事事项"
  },
  crewServices: {
    labelKey: "nav.crewServices",
    icon: "USE",
    title: "船员服务正在接入",
    subtitle: "后续将支持换班、登轮、证件、体检和本地接送服务流程。",
    metric: "船员事项"
  },
  taxServices: {
    labelKey: "nav.taxServices",
    icon: "TX",
    title: "税务服务正在接入",
    subtitle: "后续将连接退税、开票、税额核验和结算票据协同。",
    metric: "税务链路"
  },
  financialServices: {
    labelKey: "nav.financialServices",
    icon: "FN",
    title: "金融服务正在接入",
    subtitle: "后续将支持预付款、保理、授信和供应链资金状态管理。",
    metric: "资金服务"
  },
  weatherServices: {
    labelKey: "nav.weatherServices",
    icon: "WT",
    title: "气象服务正在接入",
    subtitle: "后续将汇聚风浪、能见度、潮汐窗口和靠泊风险提示。",
    metric: "气象窗口"
  },
  vesselDynamicsServices: {
    labelKey: "nav.vesselDynamicsServices",
    icon: "VD",
    title: "船舶动态服务正在接入",
    subtitle: "后续将展示船位、靠泊计划、锚地动态和服务窗口联动。",
    metric: "船舶动态"
  },
  basicManagement: {
    labelKey: "nav.basicManagement",
    icon: "BSV",
    title: "基础服务正在接入",
    subtitle: "后续将沉淀企业、标准库、字典、权限和基础配置能力。",
    metric: "基础能力"
  }
};
const currentPendingService = computed(() => servicePendingMeta[pageKey.value] || servicePendingMeta.customsServices);
const procurementFlowPageKeys = new Set([
  "quotes",
  "foodInquiries",
  "foodQuotes",
  "foodComparisonList",
  "foodOrders"
]);
const selectedStrategy = ref("LOWEST_MIXED");
const selectedCompareSupplier = ref<string | null>(null);
const fallbackDeliveryContactId = "__fallback_delivery_contact__";
const fallbackDeliveryContact = {
  contactName: "王经理",
  contactPhone: "15300800761",
  contactEmail: ""
};
const compareWorkspaceRef = ref<HTMLElement | null>(null);
const showCompareBackTop = ref(false);
const compareData = ref<MaterialDemandComparisonResponse | null>(null);
const compareLoading = ref(false);
const compareError = ref("");
const compareSupplyForm = ref<Record<CompareSupplyFormKey, string>>({
  vessel: t("compare.supply.vesselValue"),
  inquiryNo: "",
  materialType: "",
  currency: "CNY",
  port: t("compare.supply.portValue"),
  date: t("compare.supply.dateValue"),
  recipientCompany: "",
  handlerContact: "",
  supplyMode: "SEA"
});
const compareSkuKeyword = ref("");
const comparePreferenceFilters = ref<string[]>([]);
const selectedCompareRowIds = ref<string[]>([]);
const compareQuantityInputs = ref<Record<string, string>>({});
const compareUnitSelections = ref<Record<string, string>>({});
const compareQuoteMarkupPercent = ref(10);
const compareQuoteMarkupInputs = ref<Record<string, string>>({});
const compareQuoteActualPrices = ref<Record<string, string>>({});
const compareRemarkInputs = ref<Record<string, string>>({});
const compareSelectionInitialized = ref(false);
const compareDisplayCurrency = ref<"CNY" | "USD">("CNY");
const compareFixedFeeInputs = ref({
  shuttle: "",
  freight: "",
  customs: "",
  crane: "",
  other: ""
});
const compareTrafficDialogOpen = ref(false);
const compareTrafficError = ref("");
const compareTrafficShuttleRows = ref<TrafficShuttleService[]>([]);
const compareTrafficShuttleLoading = ref(false);
const compareSupplierDialogOpen = ref(false);
const compareSupplierLoading = ref(false);
const compareSupplierError = ref("");
const compareSupplierValueServiceMap = ref<Record<string, { freightPrice: string; customsPrice: string; cranePrice: string; total: number }>>({});
const compareFixedProvider = ref<{ type: "BARGE" | "SUPPLIER"; id: string; name: string }>({
  type: "BARGE",
  id: "",
  name: ""
});
const compareTrafficServiceForm = ref<MaterialDemandTrafficService>({
  shuttleNo: "",
  trafficVesselName: "",
  seaArea: "NORTH",
  anchorageCode: "",
  anchorageName: "",
  useTime: "",
  serviceType: "GOODS",
  passengerType: "NORMAL",
  passengerCount: undefined,
  cargoType: "CARGO",
  returnTrip: false,
  allowShare: false,
  remark: "",
  cargos: []
});
const compareQuoteSaving = ref(false);
const compareQuoteExporting = ref(false);
const compareQuoteImporting = ref(false);
const compareQuoteImportInput = ref<HTMLInputElement | null>(null);
const compareQuoteNotice = ref("");
const compareQuoteError = ref("");
const expandedCompareRowId = ref("");
const compareRowReplacements = ref<Record<string, MaterialComparisonCandidate>>({});
const replacementSku = ref<CompareSkuRow | null>(null);
const replacementCandidates = ref<MaterialComparisonCandidate[]>([]);
const replacementSearchKeyword = ref("");
const replacementLoading = ref(false);
const replacementError = ref("");
const purchaseOrderDialogOpen = ref(false);
const purchaseOrderCreating = ref(false);
const purchaseOrderError = ref("");
const purchaseOrderNotice = ref("");
const purchaseOrderSelectedItemsSnapshot = ref<SelectedPurchaseItem[]>([]);
const purchaseOrderForm = ref({
  supplyPort: "",
  vesselEta: "",
  requiredDeliveryTime: "",
  deliveryContactId: "",
  deliveryContactName: "",
  deliveryContactPhone: "",
  deliveryContactEmail: "",
  defaultPackagingMethod: "UNIFIED_PACKAGING",
  buyerRemark: "",
  customsServiceMode: "PACKAGE",
  craneServiceMode: "PACKAGE",
  trafficBoatEnabled: false
});
const purchaseOrderFormErrors = ref({
  supplyPort: "",
  requiredDeliveryTime: "",
  deliveryContactName: "",
  deliveryContactPhone: "",
  defaultPackagingMethod: ""
});
const purchaseOrders = ref<PurchaseOrderSummary[]>([]);
const supplierPurchaseOrders = ref<PurchaseOrderSummary[]>([]);
const purchaseOrderLoading = ref(false);
const supplierPurchaseOrderLoading = ref(false);
const purchaseOrderErrorKey = ref("");
const supplierPurchaseOrderErrorKey = ref("");
const purchaseOrderKeyword = ref("");
const purchaseOrderStatus = ref("");
const purchaseOrderCreatedFrom = ref("");
const purchaseOrderCreatedTo = ref("");
const purchaseOrderDetail = ref<PurchaseOrderDetail | null>(null);
const purchaseOrderDetailLoading = ref(false);
const purchaseOrderDetailError = ref("");
const skipNextPurchaseOrderDetailReload = ref(false);
const purchaseOrderDetailSaving = ref(false);
const purchaseOrderDetailNotice = ref("");
const purchaseOrderDetailReminding = ref(false);
const purchaseOrderSourceTrafficService = ref<MaterialDemandTrafficService | null>(null);
const purchaseOrderDetailTab = ref<"details" | "settlement">("settlement");
const purchaseOrderDetailPanelRef = ref<HTMLElement | null>(null);
const showPurchaseOrderDetailBackTop = ref(false);
const purchaseSettlementSelectedRowIds = ref<string[]>([]);
const purchaseSettlementActualInputs = ref<Record<string, string>>({});
const persistedSettlementRows = ref<SettlementOrder[]>([]);
const settlementManagementRows = ref<SettlementOrder[]>([]);
const settlementManagementLoading = ref(false);
const settlementManagementError = ref("");
const settlementManagementKeyword = ref("");
const settlementManagementStatus = ref("");
const settlementSaving = ref(false);
const settlementInvoiceDrafts = ref<Record<number, { actualAmount: string; attachments: BusinessAttachmentPayload[] }>>({});
const settlementInvoiceUploadingId = ref(0);
const settlementEditingId = ref(0);
const fulfillmentAttachments = ref<FulfillmentAttachment[]>([]);
const fulfillmentDrawerOpen = ref(false);
const fulfillmentDrawerTitle = ref("");
const fulfillmentDrawerItems = ref<FulfillmentAttachment[]>([]);
const fulfillmentUploading = ref(false);
const managedShuttleReadonly = ref(false);
const supplierFulfillmentUploading = ref(false);
const managedShuttleNodeContext = ref<{ row: TrafficShuttleService; index: number } | null>(null);
const managedShuttleActiveBookingId = ref(0);
const managedShuttleExecutionSaving = ref(false);
const managedShuttleExecutionDrafts = ref<Record<number, {
  cargoWeight: string;
  cargoVolume: string;
  palletCount: string;
  anchorageLongitude: string;
  anchorageLatitude: string;
  craneCount: number;
}>>({});
const evaluationRows = ref<ServiceEvaluation[]>([]);
const evaluationLoading = ref(false);
const evaluationError = ref("");
const evaluationEditing = ref<ServiceEvaluation | null>(null);
const evaluationKeyword = ref("");
const evaluationStatus = ref("");
const evaluationForm = ref({ rating: 5, logisticsRating: 5, content: "", attachments: [] as BusinessAttachmentPayload[], reviewRemark: "" });
const evaluationSaving = ref(false);
const evaluationUploading = ref(false);
const purchaseOrderDetailForm = ref({
  supplyPort: "",
  vesselEta: "",
  requiredDeliveryTime: "",
  deliveryContactName: "",
  deliveryContactPhone: "",
  deliveryContactEmail: "",
  buyerRemark: ""
});
let purchaseOrderDetailScrollPanel: HTMLElement | null = null;
const getPurchaseOrderDetailScrollPanel = () =>
  purchaseOrderDetailPanelRef.value?.closest(".expandable-panel__body") as HTMLElement | null;
const updatePurchaseOrderDetailBackTopVisibility = () => {
  const scrollPanel = getPurchaseOrderDetailScrollPanel();
  showPurchaseOrderDetailBackTop.value =
    purchaseOrderDetailTab.value === "details" && (scrollPanel?.scrollTop ?? 0) > 120;
};
const bindPurchaseOrderDetailScrollPanel = () => {
  const nextPanel = getPurchaseOrderDetailScrollPanel();
  if (purchaseOrderDetailScrollPanel === nextPanel) {
    updatePurchaseOrderDetailBackTopVisibility();
    return;
  }
  purchaseOrderDetailScrollPanel?.removeEventListener("scroll", updatePurchaseOrderDetailBackTopVisibility);
  purchaseOrderDetailScrollPanel = nextPanel;
  purchaseOrderDetailScrollPanel?.addEventListener("scroll", updatePurchaseOrderDetailBackTopVisibility, { passive: true });
  updatePurchaseOrderDetailBackTopVisibility();
};
const scrollPurchaseOrderDetailToTop = () => {
  const scrollPanel = getPurchaseOrderDetailScrollPanel();
  scrollPanel?.scrollTo({ top: 0, behavior: "smooth" });
  showPurchaseOrderDetailBackTop.value = false;
};
const setPurchaseOrderDetailTab = async (tab: "details" | "settlement") => {
  purchaseOrderDetailTab.value = tab;
  showPurchaseOrderDetailBackTop.value = false;
  await nextTick();
  bindPurchaseOrderDetailScrollPanel();
  const scrollPanel = getPurchaseOrderDetailScrollPanel();
  if (scrollPanel) {
    scrollPanel.scrollTo({ top: 0, behavior: "smooth" });
  }
};
const purchaseTrafficServiceRows = ref<TrafficServiceOrder[]>([]);
const supplierActionOrder = ref<PurchaseSupplierOrder | null>(null);
const supplierActionType = ref<"confirm" | "reject" | "">("");
const supplierActionSaving = ref(false);
const supplierDetailDialogRow = ref<Record<string, unknown> | null>(null);
const supplierConfirmForm = ref({
  expectedReadyAt: "",
  supplierRemark: ""
});
const supplierRejectReason = ref("");
const discardDialogOpen = ref(false);
const discardTarget = ref<{ type: "demand" | "purchaseOrder"; id: number | string; refresh: () => void | Promise<void> } | null>(null);
const discardSaving = ref(false);
const discardError = ref("");
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
const companyContactLoading = ref(false);
const companyVesselLoading = ref(false);
const companyValueAddedServiceLoading = ref(false);
const supplierInfoLoading = ref(false);
const shopSkuListLoading = ref(false);
const shopLoading = ref(false);
const shopSaving = ref(false);
const shopImporting = ref(false);
const shopImportPreview = ref<ShopImportPreviewSummary | null>(null);
const shopPersistedSkuRows = ref<ShopSkuRow[]>([]);
const shopPreviewSkuRows = ref<ShopSkuRow[]>([]);
const shopSkuRows = ref<ShopSkuRow[]>([]);
const supplierInfoRows = ref<SupplierInfoRow[]>([]);
const shopDirtySkuIds = ref<Set<string>>(new Set());
const expandedShopSkuId = ref("");
const shopManagementTab = ref<"products" | "qualifications" | "contacts" | "vessels" | "valueAddedServices" | "trafficService">("products");
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
const supplierInfoErrorMessage = ref("");
const supplierInfoKeyword = ref("");
const supplierInfoPort = ref("");
const supplierInfoCategory = ref("");
const supplierInfoStatus = ref("");
const companyQualificationRows = ref<CompanyQualificationRow[]>([]);
const companyContactRows = ref<CompanyContactRow[]>([]);
const companyVesselRows = ref<CompanyVesselRow[]>([]);
const companyValueAddedServiceForm = ref({
  freightPrice: "",
  customsPrice: "",
  cranePrice: "",
  remark: ""
});
const companyQualificationSaving = ref(false);
const companyContactSaving = ref(false);
const companyVesselSaving = ref(false);
const companyValueAddedServiceSaving = ref(false);
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
let liveFilterTimer: number | undefined;
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
    course: "035掳",
    distance: "5.4 NM",
    statusKey: "dashboard.shipPosition.status.supply"
  },
  {
    name: "BLUE PORT",
    x: 56,
    y: 43,
    rotation: 18,
    speed: "8.6 kn",
    course: "082掳",
    distance: "2.1 NM",
    statusKey: "dashboard.shipPosition.status.approaching"
  },
  {
    name: "ZHONG WAI YUN 6",
    x: 74,
    y: 67,
    rotation: -8,
    speed: "6.2 kn",
    course: "116掳",
    distance: "7.8 NM",
    statusKey: "dashboard.shipPosition.status.anchored"
  },
  {
    name: "PACIFIC TRADER",
    x: 39,
    y: 28,
    rotation: 34,
    speed: "12.4 kn",
    course: "061掳",
    distance: "9.3 NM",
    statusKey: "dashboard.shipPosition.status.inbound"
  }
];
const dashboardLoading = ref(false);
const dashboardLoaded = ref(false);
const dashboardError = ref("");
const dashboardShipSpriteUrl = dashboardShipSprite;
const selectedDashboardPurchaseOrderId = ref(0);
const dashboardVesselScroller = ref<HTMLElement | null>(null);
const dashboardMetricAnimationProgress = ref(1);
let dashboardMetricAnimationFrame = 0;
const parseDashboardMetricNumber = (value: string | number) => {
  if (typeof value === "number") return value;
  const normalized = String(value).replace(/[^\d.-]/g, "");
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
};
const formatAnimatedNumber = (value: string | number, progress: number) => {
  const target = parseDashboardMetricNumber(value);
  const current = target * progress;
  const source = String(value);
  const decimals = source.includes(".") ? Math.min(2, source.split(".")[1]?.replace(/[^\d]/g, "").length || 0) : 0;
  return current.toLocaleString("zh-CN", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  });
};
const formatAnimatedMetricValue = (value: string | number) => {
  const animated = formatAnimatedNumber(value, dashboardMetricAnimationProgress.value);
  return String(value).includes("¥") ? `¥ ${animated}` : animated;
};
const startDashboardMetricAnimation = () => {
  if (typeof window === "undefined") return;
  if (!["dashboard", "dashboardGovernment"].includes(pageKey.value)) return;
  window.cancelAnimationFrame(dashboardMetricAnimationFrame);
  if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) {
    dashboardMetricAnimationProgress.value = 1;
    return;
  }
  dashboardMetricAnimationProgress.value = 0;
  const startedAt = performance.now();
  const duration = 980;
  const tick = (now: number) => {
    const raw = Math.min(1, (now - startedAt) / duration);
    dashboardMetricAnimationProgress.value = 1 - Math.pow(1 - raw, 3);
    if (raw < 1) {
      dashboardMetricAnimationFrame = window.requestAnimationFrame(tick);
    }
  };
  dashboardMetricAnimationFrame = window.requestAnimationFrame(tick);
};
const dashboardTodayLabel = computed(() =>
  new Intl.DateTimeFormat(language.value === "zh-CN" ? "zh-CN" : "en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short"
  }).format(today)
);
const isDashboardToday = (value?: string | null) => {
  if (!value) return false;
  return String(value).slice(0, 10) === formatDateKey(today);
};
const governmentDashboardStats = [
  { label: "交易金额", value: "8,426.80", unit: "万", trend: "+843.60 万", rate: "11.13%", icon: govCardAmountIcon },
  { label: "退税金额", value: "1,268.40", unit: "万", trend: "+126.80 万", rate: "11.12%", icon: govCardTaxIcon },
  { label: "订单执行", value: "128", unit: "单", trend: "+12 单", rate: "10.34%", icon: govCardOrdersIcon },
  { label: "服务商", value: "340", unit: "家", trend: "+9 家", rate: "2.72%", icon: govCardSupplierIcon }
];
const governmentMapSceneUrl = govMapScene;
const governmentDashboardMapPoints = [
  { city: "湛江市", x: 15, y: 35, type: "port" },
  { city: "海口市", x: 24, y: 73, type: "node" },
  { city: "三亚市", x: 32, y: 88, type: "node" },
  { city: "茂名市", x: 50, y: 17, type: "node" },
  { city: "阳江市", x: 70, y: 15, type: "port" },
  { city: "实时运力", x: 76, y: 47, type: "card" }
];
const governmentDashboardRoutes = [
  { from: [15, 35], to: [70, 15] },
  { from: [24, 73], to: [50, 17] },
  { from: [32, 88], to: [76, 47] },
  { from: [15, 35], to: [76, 47] },
  { from: [50, 17], to: [70, 15] }
];
const governmentDashboardShips = [
  { x: 38, y: 40, angle: -18 },
  { x: 48, y: 28, angle: -10 },
  { x: 60, y: 42, angle: -22 },
  { x: 72, y: 31, angle: -16 },
  { x: 83, y: 63, angle: 14 },
  { x: 55, y: 73, angle: 18 }
];
const governmentSupplierRankings = [
  ["供货商A", "26", "1,685.40"],
  ["供货商B", "18", "1,256.30"],
  ["供货商C", "15", "1,002.60"],
  ["供货商D", "12", "845.20"],
  ["供货商E", "10", "632.80"]
];
const governmentBargeRankings = [
  ["交通艇A", "36", "18,560"],
  ["交通艇B", "28", "14,230"],
  ["交通艇C", "24", "12,340"],
  ["交通艇D", "20", "9,860"],
  ["交通艇E", "18", "8,420"]
];
const governmentRankMode = ref<"supplier" | "barge">("supplier");
const governmentMonthlyBaseValues = [
  [108, 8200],
  [116, 7900],
  [94, 7600],
  [122, 8900],
  [101, 8350],
  [98, 8100],
  [102, 8450],
  [111, 9200],
  [82, 6900],
  [97, 7650],
  [104, 8020],
  [128, 8426.8]
];
const governmentMonthlyOrders = computed(() =>
  governmentMonthlyBaseValues.map(([orderCount, amount], index) => {
    const date = new Date(today.getFullYear(), today.getMonth() - 11 + index, 1);
    return {
      label: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      shortLabel: `${date.getMonth() + 1}月`,
      orderCount,
      amount
    };
  })
);
const governmentMaxAmount = computed(() => Math.max(...governmentMonthlyOrders.value.map((item) => item.amount)));
const governmentAmountCeiling = computed(() => Math.ceil(governmentMaxAmount.value / 1000) * 1000);
const governmentAmountTicks = computed(() => [governmentAmountCeiling.value, governmentAmountCeiling.value / 2, 0]);
const formatGovernmentAmountLabel = (value: number) =>
  value >= 1000 ? `${(value / 1000).toFixed(value % 1000 === 0 ? 0 : 1)}k` : String(value);
const formatGovernmentMonthlyAmount = (value: number) =>
  value.toLocaleString("zh-CN", { minimumFractionDigits: value % 1 === 0 ? 0 : 1, maximumFractionDigits: 1 });
const getGovernmentAmountBarHeight = (amount: number) =>
  `${Math.max(16, (amount / Math.max(1, governmentAmountCeiling.value)) * 100)}%`;
const dashboardActiveCompareRows = computed(() =>
  comparisonDemandRows.value.filter((row) => {
    const status = String(row.status || "").toUpperCase();
    return status === "COMPARING";
  })
);
const dashboardCompletedPurchaseStatuses = new Set(["SUPPLIED", "COMPLETED", "FINISHED"]);
const isDashboardExecutionPurchaseOrder = (row: PurchaseOrderSummary | Record<string, unknown>) => {
  const status = String((row as PurchaseOrderSummary).status || "").toUpperCase();
  return !dashboardCompletedPurchaseStatuses.has(status);
};
const dashboardExecutionOrders = computed(() =>
  sortNewestFirst(
    purchaseOrders.value.filter(isDashboardExecutionPurchaseOrder),
    ["requiredDeliveryTime", "vesselEta", "createdAt", "updatedAt"],
    ["purchaseOrderId"]
  )
);
const dashboardTodayExecutionOrders = computed(() => {
  const rows = dashboardExecutionOrders.value.filter((row) =>
    isDashboardToday((row as PurchaseOrderSummary).requiredDeliveryTime) ||
    isDashboardToday((row as PurchaseOrderSummary).vesselEta) ||
    isDashboardToday((row as PurchaseOrderSummary).createdAt)
  ) as PurchaseOrderSummary[];
  return rows.length ? rows : (dashboardExecutionOrders.value.slice(0, 8) as PurchaseOrderSummary[]);
});
const dashboardPendingSettlementRows = computed(() =>
  settlementManagementRows.value.filter((row) => String(row.status || "").toUpperCase() === "SETTLED")
);
const dashboardSettlementTotalAmount = computed(() =>
  settlementManagementRows.value.reduce((total, row) => {
    const actualAmount = Number(row.actualAmount || 0);
    const quotedAmount = Number(row.quotedAmount || 0);
    return total + (actualAmount > 0 ? actualAmount : quotedAmount);
  }, 0)
);
const dashboardPrimaryOrder = computed(() => {
  const source = dashboardShipSourceRows.value;
  return source.find((row) => row.purchaseOrderId === selectedDashboardPurchaseOrderId.value) || source[0] || (dashboardExecutionOrders.value[0] as PurchaseOrderSummary | undefined);
});
const dashboardMetricCards = computed(() => [
  {
    key: "amount",
    title: "订单金额",
    subtitle: "",
    value: formatPurchaseMoneyValue(dashboardSettlementTotalAmount.value),
    unit: "",
    note: "结算单金额合计",
    trend: "none",
    icon: "¥",
    path: "/settlements"
  },
  {
    key: "quotes",
    title: "需求报价",
    subtitle: "",
    value: dashboardActiveCompareRows.value.length,
    unit: "份",
    delta: Math.max(1, Math.min(9, Math.round(dashboardActiveCompareRows.value.length / 5) || 2)),
    trend: "down",
    icon: "IQ",
    path: "/comparison"
  },
  {
    key: "orders",
    title: "执行订单",
    subtitle: "",
    value: dashboardExecutionOrders.value.length,
    unit: "单",
    delta: Math.max(1, Math.min(9, Math.round(dashboardExecutionOrders.value.length / 4) || 4)),
    trend: "up",
    icon: "PO",
    path: "/orders"
  },
  {
    key: "settlements",
    title: "供应结算",
    subtitle: "",
    value: dashboardPendingSettlementRows.value.length,
    unit: "单",
    delta: Math.max(1, Math.min(6, Math.round(dashboardPendingSettlementRows.value.length / 6) || 1)),
    trend: "down",
    icon: "ST",
    path: "/settlements"
  }
]);
const dashboardWeatherSummary = computed(() => ({
  temperature: "18°",
  condition: "多云转晴",
  wind: "东北风 4-5级",
  visibility: "12nm",
  wave: "0.6-1.0m",
  distance: "台风 “玛娃” 距离 162nm"
}));
const navigateDashboardMetric = (path: string) => {
  router.push(path);
};
const scrollDashboardVessels = (direction: "prev" | "next") => {
  const scroller = dashboardVesselScroller.value;
  if (!scroller) return;
  scroller.scrollBy({
    left: (direction === "next" ? 1 : -1) * Math.max(280, scroller.clientWidth * 0.75),
    behavior: "smooth"
  });
};
const dashboardSupportWarnings = computed(() => [
  { title: "供应评价", detail: "服务商需要评价", count: 2 },
  { title: "供应提醒", detail: "存在物料缺口风险", count: Math.max(1, dashboardTodayExecutionOrders.value.length) },
  { title: "结算风险", detail: "应收账款即将逾期", count: Math.min(9, dashboardPendingSettlementRows.value.length) },
  { title: "船舶动态", detail: "ETA 发生变化", count: 4 }
]);
const dashboardStageTime = (row: PurchaseOrderSummary | undefined, index: number) => {
  if (!row) return "待开始";
  if (index === 0) return row.createdAt ? formatDateTimeParts(row.createdAt).date || "--" : "--";
  if (index === 1) return row.updatedAt ? formatDateTimeParts(row.updatedAt).date || "--" : "--";
  if (index === 2) return row.vesselEta ? formatDateTimeParts(row.vesselEta).date || "--" : "--";
  return "待开始";
};
const dashboardShipImagePosition = (index: number) => {
  const positions = ["0% 50%", "33.333% 50%", "66.666% 50%", "100% 50%"];
  return positions[index % positions.length];
};
const dashboardProgressRate = (done: number, total: number) =>
  total > 0 ? Math.max(0, Math.min(100, Math.round((done / total) * 100))) : 0;
const displaySupplyPort = (value?: string | null) => formatSupplyPortDisplay(value) || String(value ?? "").trim();
const dashboardShipSourceRows = computed(() =>
  sortNewestFirst(dashboardExecutionOrders.value, ["createdAt", "updatedAt", "requiredDeliveryTime", "vesselEta"], ["purchaseOrderId"]) as PurchaseOrderSummary[]
);
const dashboardShipCards = computed(() => {
  const source = dashboardShipSourceRows.value;
  return source.map((row, index) => {
    const supplierCount = Math.max(1, Number(row.supplierCount || row.totalSupplierCount || 1));
    const totalSkuCount = Math.max(0, Number(row.totalSkuCount || row.itemCount || 0));
    const purchasedSkuCount = Math.min(
      totalSkuCount || Number(row.purchasedSkuCount || row.itemCount || 0),
      Math.max(0, Number(row.purchasedSkuCount || row.itemCount || 0))
    );
    const readySupplierCount = Math.min(supplierCount, Math.max(0, Number(row.readySupplierCount || 0)));
    const etaParts = formatDateTimeParts(row.vesselEta || row.requiredDeliveryTime || "");
    return {
      row,
      key: row.purchaseOrderId || index,
      vesselName: row.vesselName || ["OCEANIC GLORY", "SEA HARMONY", "MAERSK HORIZON", "GLOBAL SPIRIT"][index % 4],
      chip: index === 2 ? "今日靠泊" : index === 3 ? "即将靠泊" : "在锚地",
      distance: index === 2 ? "距离 2.6h" : index === 3 ? "距离 3.6h" : `距离 ${index === 0 ? "4.2" : "7.1"}h`,
      supplyPort: displaySupplyPort(row.supplyPort) || "-",
      vesselEta: formatDemandDateTime(row.vesselEta || row.requiredDeliveryTime || "") || etaParts.time || "-",
      serviceText: `${purchasedSkuCount}/${totalSkuCount || purchasedSkuCount}`,
      serviceRate: dashboardProgressRate(purchasedSkuCount, totalSkuCount || purchasedSkuCount),
      supplierText: `${readySupplierCount}/${supplierCount}`,
      supplierRate: dashboardProgressRate(readySupplierCount, supplierCount),
      status: purchaseOrderQueryStatusLabel(row),
      imagePosition: dashboardShipImagePosition(index)
    };
  });
});
const dashboardPrimaryOrderMeta = computed(() => {
  const row = dashboardPrimaryOrder.value;
  if (!row) {
    return {
      orderNo: "-",
      status: "暂无执行",
      vesselName: "-",
      imo: "IMO -",
      berthTime: "-",
      anchorage: "-",
      sku: "-",
      service: "-",
      amount: "¥ 0.00",
      quantity: "-",
      supplier: "-",
      contact: "-"
    };
  }
  const supplierContact = [row.supplierContactName, row.supplierContactPhone].filter(Boolean).join(" / ");
  return {
    orderNo: row.purchaseOrderNo || "-",
    status: purchaseOrderQueryStatusLabel(row),
    vesselName: row.vesselName || "-",
    imo: "IMO 9698765",
    berthTime: formatDemandDateTime(row.vesselEta || row.requiredDeliveryTime || "") || "-",
    anchorage: displaySupplyPort(row.supplyPort) || "-",
    sku: row.itemCount || 0,
    service: row.materialType || "供应服务",
    amount: formatPurchaseMoneyValue(purchaseOrderQueryAmount(row)),
    quantity: row.supplierCount || "-",
    supplier: row.buyerCompanyName || row.recipientCompany || "-",
    contact: supplierContact || row.supplierContactName || row.supplierContactPhone || "-"
  };
});

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
      { code: "supplyChainFinance", label: t("nav.supplyChainFinance") },
      { code: "crewServices", label: t("nav.crewServices") },
      { code: "registrations", label: t("nav.registrations") },
      { code: "permissions", label: t("nav.permissions") },
      { code: "menuManagement", label: t("nav.menuManagement") }
    ]
  }
]);
const selectedRegistration = computed(() => registrations.value.find((item) => item.id === selectedRegistrationId.value));
const selectedCompanyMember = computed(() => companyMembers.value.find((member) => member.id === selectedCompanyMemberId.value));
const sortNewestFirst = <T>(rows: T[], dateKeys: string[], idKeys: string[] = []) =>
  [...rows].sort((first, second) => {
    const firstRow = first as Record<string, unknown>;
    const secondRow = second as Record<string, unknown>;
    const dateValue = (row: Record<string, unknown>) => {
      const text = dateKeys.map((key) => String(row[key] || "").trim()).find(Boolean) || "";
      const parsed = Date.parse(text);
      return Number.isFinite(parsed) ? parsed : 0;
    };
    const dateDifference = dateValue(secondRow) - dateValue(firstRow);
    if (dateDifference) return dateDifference;
    const idValue = (row: Record<string, unknown>) => {
      const value = idKeys.map((key) => Number(row[key])).find((item) => Number.isFinite(item));
      return value ?? 0;
    };
    return idValue(secondRow) - idValue(firstRow);
  });
const materialDemands = ref<MaterialDemandSummary[]>([]);
const materialDemandLoading = ref(false);
const materialDemandErrorKey = ref("");
const materialDemandKeyword = ref("");
const highlightedDemandId = computed(() => String(route.query.demandId || ""));
const materialDemandRows = computed(() => sortNewestFirst(materialDemands.value, ["updatedAt", "inquiryDate", "createdAt"], ["demandId"]) as unknown as Record<string, unknown>[]);
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
const inquiryDemandTableRows = computed(() =>
  sortNewestFirst(inquiryDemandRows.value, ["updatedAt", "inquiryDate", "createdAt"], ["demandId"]).map((row) => ({
    ...row,
    matchSummary: `${row.exactCount ?? 0} / ${row.similarCount ?? 0} / ${row.unmatchedCount ?? 0}（${row.skuCount ?? 0}）`
  })) as unknown as Record<string, unknown>[]
);
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
const comparisonDemandTableRows = computed(() => sortNewestFirst(comparisonDemandRows.value, ["updatedAt", "inquiryDate", "createdAt"], ["demandId"]) as unknown as Record<string, unknown>[]);
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
const dictionaryTypes = ref<DictionaryType[]>([]);
const dictionaryItems = ref<DictionaryItem[]>([]);
const dictionaryLoading = ref(false);
const dictionarySaving = ref(false);
const dictionaryErrorKey = ref("");
const dictionaryNotice = ref("");
const dictionaryTypeKeyword = ref("");
const dictionaryItemKeyword = ref("");
const selectedDictionaryTypeCode = ref("");
const editingDictionaryTypeCode = ref("");
const editingDictionaryItemId = ref<number | null>(null);
const dictionaryTypeForm = ref({
  typeCode: "",
  typeName: "",
  description: "",
  sortOrder: 0,
  enabled: true
});
const dictionaryItemForm = ref({
  typeCode: "",
  itemCode: "",
  itemName: "",
  itemValue: "",
  itemNameEn: "",
  description: "",
  sortOrder: 0,
  enabled: true
});
const dictionaryTypeColumns = computed<TableColumn[]>(() => [
  { key: "typeCode", label: t("dataDictionary.typeCode"), width: "34%" },
  { key: "typeName", label: t("dataDictionary.typeName"), width: "34%" },
  { key: "sortOrder", label: t("dataDictionary.sortOrder"), width: "16%", align: "right" },
  { key: "enabled", label: t("field.status"), width: "16%", align: "center" }
]);
const dictionaryItemColumns = computed<TableColumn[]>(() => [
  { key: "itemCode", label: t("dataDictionary.itemCode"), width: "22%" },
  { key: "itemName", label: t("dataDictionary.itemName"), width: "18%" },
  { key: "itemValue", label: t("dataDictionary.itemValue"), width: "15%" },
  { key: "itemNameEn", label: t("dataDictionary.itemNameEn"), width: "20%" },
  { key: "description", label: t("dataDictionary.descriptionField"), width: "8%" },
  { key: "sortOrder", label: t("dataDictionary.sortOrder"), width: "7%", align: "right" },
  { key: "enabled", label: t("field.status"), width: "10%", align: "center" }
]);
const dictionaryTypeRows = computed(() => dictionaryTypes.value as unknown as Record<string, unknown>[]);
const dictionaryItemRows = computed(() => dictionaryItems.value as unknown as Record<string, unknown>[]);

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

const loadDashboardData = async () => {
  dashboardLoading.value = true;
  dashboardError.value = "";
  try {
    const [compareResult, purchaseResult, settlementResult] = await Promise.allSettled([
      listMaterialDemands({ page: 1, size: 100 }),
      listPurchaseOrders({ page: 1, size: 100 }),
      listSettlements("BUYER")
    ]);
    if (compareResult.status === "fulfilled") {
      comparisonDemandRows.value = compareResult.value.items;
    }
    if (purchaseResult.status === "fulfilled") {
      purchaseOrders.value = purchaseResult.value.items;
    }
    if (settlementResult.status === "fulfilled") {
      settlementManagementRows.value = settlementResult.value;
    }
    dashboardLoaded.value = true;
    if ([compareResult, purchaseResult, settlementResult].every((result) => result.status === "rejected")) {
      dashboardError.value = "今日看板数据加载失败";
    }
  } finally {
    dashboardLoading.value = false;
  }
};

const ensureDashboardLoaded = () => {
  if (pageKey.value === "dashboard" && !dashboardLoading.value && !dashboardLoaded.value) {
    void loadDashboardData();
  }
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
  if (pageKey.value === "supplierProducts") {
    return { icon: "Plus" as const, label: t("page.supplierProducts.contact.add") };
  }
  if (pageKey.value === "suppliers" || pageKey.value === "permissions" || pageKey.value === "companyMembers") {
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

const companyContactColumns = computed<TableColumn[]>(() => [
  { key: "contactName", label: t("page.supplierProducts.contact.contactName"), width: "180px" },
  { key: "contactPhone", label: t("page.supplierProducts.contact.contactPhone"), width: "180px" },
  { key: "contactEmail", label: t("page.supplierProducts.contact.contactEmail"), width: "220px" },
  { key: "updatedAt", label: t("page.supplierProducts.contact.updatedAt"), width: "140px" },
  { key: "operation", label: t("common.operation"), width: "156px", align: "center" }
]);

const companyVesselColumns = computed<TableColumn[]>(() => [
  { key: "vesselName", label: t("page.supplierProducts.vessel.vesselName"), width: "180px" },
  { key: "vesselType", label: t("page.supplierProducts.vessel.vesselType"), width: "130px" },
  { key: "buildDate", label: t("page.supplierProducts.vessel.buildDate"), width: "130px" },
  { key: "nextMaintenanceDate", label: t("page.supplierProducts.vessel.nextMaintenanceDate"), width: "150px" },
  { key: "capacity", label: t("page.supplierProducts.vessel.capacity"), width: "120px" },
  { key: "remark", label: t("field.remark"), width: "180px" },
  { key: "operation", label: t("common.operation"), width: "156px", align: "center" }
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
    const normalizedCandidate = candidate.toLowerCase().replace(/[\s._\-:/\\()锛堬級&]+/g, "");
    const found = entries.find(([key]) => key.toLowerCase().replace(/[\s._\-:/\\()锛堬級&]+/g, "") === normalizedCandidate);
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
    deliveryArea: normalizeQuestionMojibakeText(readShopString(value, "deliveryArea")),
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

const normalizeQuestionMojibakeText = (value: string, fallback = "") => {
  const text = value.trim();
  if (!text) return fallback;
  const suffix = text.match(/^\?+([A-Za-z])$/)?.[1]?.toUpperCase();
  if (suffix) return `联系人${suffix}`;
  return /\?{2,}/.test(text) ? fallback : text;
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
    contactName: normalizeQuestionMojibakeText(readShopString(source, "contactName")),
    contactPhone: normalizeQuestionMojibakeText(readShopString(source, "contactPhone")),
    contactEmail: normalizeQuestionMojibakeText(readShopString(source, "contactEmail")),
    companyType: readShopString(source, "companyType"),
    status: readShopString(source, "status") || "ACTIVE"
  };
};

const normalizeSupplierInfo = (value: unknown): SupplierInfoRow | null => {
  if (!isPlainRecord(value)) return null;
  const companyId = readShopNumber(value, "companyId");
  const id = readShopString(value, "id") || (companyId ? `SUP-${companyId}` : "");
  const name = normalizeQuestionMojibakeText(readShopString(value, "name") || readShopString(value, "companyName"));
  if (!id || !name) return null;
  return {
    id,
    companyId: companyId || undefined,
    name,
    port: normalizeQuestionMojibakeText(readShopString(value, "port")) || "--",
    category: normalizeQuestionMojibakeText(readShopString(value, "category")) || "--",
    score: readShopString(value, "score") || String(readShopNumber(value, "score") || 88),
    status: readShopString(value, "status") || "active",
    qualificationStatus: readShopString(value, "qualificationStatus"),
    skuCount: readShopNumber(value, "skuCount"),
    contactName: normalizeQuestionMojibakeText(readShopString(value, "contactName")),
    contactPhone: normalizeQuestionMojibakeText(readShopString(value, "contactPhone")),
    updatedAt: readShopString(value, "updatedAt")
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
    deliveryArea: normalizeQuestionMojibakeText(readShopString(value, "deliveryArea")),
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

const normalizeCompanyContact = (value: unknown): CompanyContactRow | null => {
  if (!isPlainRecord(value)) return null;
  const contactId = value.contactId ?? value.id;
  const contactName = normalizeQuestionMojibakeText(readShopString(value, "contactName"));
  const contactPhone = normalizeQuestionMojibakeText(readShopString(value, "contactPhone"));
  const contactEmail = normalizeQuestionMojibakeText(readShopString(value, "contactEmail"));
  const rowId = String(contactId || contactName || contactPhone || Math.random());
  return {
    id: rowId,
    contactId: typeof contactId === "string" || typeof contactId === "number" ? contactId : undefined,
    contactName,
    contactPhone,
    contactEmail,
    status: readShopString(value, "status") || "ACTIVE",
    createdAt: readShopString(value, "createdAt"),
    updatedAt: readShopString(value, "updatedAt") || readShopString(value, "createdAt"),
    isEditing: false,
    isNew: false,
    draft: {
      contactName,
      contactPhone,
      contactEmail
    }
  };
};

const normalizeCompanyVessel = (value: unknown): CompanyVesselRow | null => {
  if (!isPlainRecord(value)) return null;
  const vesselId = value.vesselId ?? value.id;
  const vesselName = normalizeQuestionMojibakeText(readShopString(value, "vesselName"));
  const vesselType = normalizeQuestionMojibakeText(readShopString(value, "vesselType"));
  const buildDate = readShopString(value, "buildDate");
  const nextMaintenanceDate = readShopString(value, "nextMaintenanceDate");
  const capacity = normalizeQuestionMojibakeText(readShopString(value, "capacity"));
  const remark = normalizeQuestionMojibakeText(readShopString(value, "remark"));
  const rowId = String(vesselId || vesselName || Math.random());
  return {
    id: rowId,
    vesselId: typeof vesselId === "string" || typeof vesselId === "number" ? vesselId : undefined,
    vesselName,
    vesselType,
    buildDate,
    nextMaintenanceDate,
    capacity,
    status: readShopString(value, "status") || "ACTIVE",
    remark,
    createdAt: readShopString(value, "createdAt"),
    updatedAt: readShopString(value, "updatedAt") || readShopString(value, "createdAt"),
    isEditing: false,
    isNew: false,
    draft: {
      vesselName,
      vesselType,
      buildDate,
      nextMaintenanceDate,
      capacity,
      remark
    }
  };
};

const normalizeCompanyValueAddedService = (payload: unknown) => {
  const unwrapped = unwrapShopPayload(payload);
  const source = isPlainRecord(unwrapped) ? unwrapped : {};
  return {
    freightPrice: normalizeCompareFeeInput(readShopNumber(source, "freightPrice")),
    customsPrice: normalizeCompareFeeInput(readShopNumber(source, "customsPrice")),
    cranePrice: normalizeCompareFeeInput(readShopNumber(source, "cranePrice")),
    remark: normalizeQuestionMojibakeText(readShopString(source, "remark"))
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
const activeCompanyContacts = computed(() => companyContactRows.value.filter((row) => row.status !== "DELETED" && row.contactName && row.contactPhone));
const activeCompanyVessels = computed(() => companyVesselRows.value.filter((row) => row.status !== "DELETED" && row.vesselName));
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

const isValidContactEmail = (value: string) => !value.trim() || /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value.trim());

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

const buildSupplierInfoQuery = () => ({
  keyword: supplierInfoKeyword.value.trim(),
  port: supplierInfoPort.value.trim(),
  category: supplierInfoCategory.value.trim(),
  status: supplierInfoStatus.value,
  page: 1,
  size: 100
});

const loadSupplierInfoRows = async () => {
  if (pageKey.value !== "suppliers") return;
  supplierInfoLoading.value = true;
  supplierInfoErrorMessage.value = "";
  try {
    const payload = await listShopSuppliers(buildSupplierInfoQuery());
    supplierInfoRows.value = readShopItems(payload).map((item) => normalizeSupplierInfo(item)).filter((item): item is SupplierInfoRow => Boolean(item));
  } catch (error) {
    supplierInfoErrorMessage.value = error instanceof ApiError && error.message ? error.message : "供货商信息查询失败";
    supplierInfoRows.value = [];
  } finally {
    supplierInfoLoading.value = false;
  }
};

const resetSupplierInfoFilters = () => {
  supplierInfoKeyword.value = "";
  supplierInfoPort.value = "";
  supplierInfoCategory.value = "";
  supplierInfoStatus.value = "";
  void loadSupplierInfoRows();
};

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

const loadCompanyContacts = async () => {
  companyContactLoading.value = true;
  try {
    const payload = await listCompanyContacts({ status: "ACTIVE" });
    companyContactRows.value = readShopItems(payload).map((item) => normalizeCompanyContact(item)).filter((item): item is CompanyContactRow => Boolean(item));
  } finally {
    companyContactLoading.value = false;
  }
};

const loadCompanyVessels = async () => {
  companyVesselLoading.value = true;
  try {
    const payload = await listCompanyVessels({ status: "ACTIVE" });
    companyVesselRows.value = readShopItems(payload).map((item) => normalizeCompanyVessel(item)).filter((item): item is CompanyVesselRow => Boolean(item));
  } finally {
    companyVesselLoading.value = false;
  }
};

const loadCompanyValueAddedServices = async () => {
  companyValueAddedServiceLoading.value = true;
  try {
    const payload = await getCompanyValueAddedServices();
    companyValueAddedServiceForm.value = normalizeCompanyValueAddedService(payload);
  } finally {
    companyValueAddedServiceLoading.value = false;
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
    await Promise.all([loadShopProfile(), loadCompanyQualifications(), loadCompanyContacts(), loadCompanyVessels(), loadCompanyValueAddedServices(), loadShopSkuList()]);
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error);
    companyQualificationRows.value = [];
    companyContactRows.value = [];
    companyVesselRows.value = [];
    companyValueAddedServiceForm.value = {
      freightPrice: "",
      customsPrice: "",
      cranePrice: "",
      remark: ""
    };
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

const addCompanyContact = () => {
  if (companyContactRows.value.some((row) => row.isNew)) return;
  companyContactRows.value = [
    {
      id: `new-contact-${Date.now()}`,
      contactName: "",
      contactPhone: "",
      contactEmail: "",
      status: "ACTIVE",
      createdAt: "",
      updatedAt: "",
      isEditing: true,
      isNew: true,
      draft: {
        contactName: "",
        contactPhone: "",
        contactEmail: ""
      }
    },
    ...companyContactRows.value
  ];
};

const openCompanyContactCreateFromAnyTab = () => {
  shopManagementTab.value = "contacts";
  addCompanyContact();
};

const editCompanyContact = (row: CompanyContactRow) => {
  row.isEditing = true;
  row.draft = {
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    contactEmail: row.contactEmail
  };
};

const cancelCompanyContactEdit = (row: CompanyContactRow) => {
  if (row.isNew) {
    companyContactRows.value = companyContactRows.value.filter((item) => item.id !== row.id);
    return;
  }
  row.isEditing = false;
  row.draft = {
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    contactEmail: row.contactEmail
  };
};

const saveCompanyContact = async (row: CompanyContactRow) => {
  const draft = row.draft || row;
  const contactName = draft.contactName.trim();
  const contactPhone = draft.contactPhone.trim();
  const contactEmail = draft.contactEmail.trim();
  if (!contactName) {
    shopErrorMessage.value = t("page.supplierProducts.contact.nameRequired");
    return;
  }
  if (!contactPhone) {
    shopErrorMessage.value = t("page.supplierProducts.contact.phoneRequired");
    return;
  }
  if (!isValidContactEmail(contactEmail)) {
    shopErrorMessage.value = t("page.supplierProducts.contact.emailInvalid");
    return;
  }
  companyContactSaving.value = true;
  shopErrorMessage.value = "";
  shopNoticeKey.value = "";
  try {
    const payload = {
      contactName,
      contactPhone,
      contactEmail,
      status: "ACTIVE"
    };
    if (row.contactId) {
      await updateCompanyContact(row.contactId, payload);
      shopNoticeKey.value = "page.supplierProducts.contact.updated";
    } else {
      await createCompanyContact(payload);
      shopNoticeKey.value = "page.supplierProducts.contact.created";
    }
    await loadCompanyContacts();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, row.contactId ? "page.supplierProducts.contact.updateFailed" : "page.supplierProducts.contact.createFailed");
  } finally {
    companyContactSaving.value = false;
  }
};

const removeCompanyContact = async (row: CompanyContactRow) => {
  if (row.isNew) {
    companyContactRows.value = companyContactRows.value.filter((item) => item.id !== row.id);
    return;
  }
  if (!row.contactId) return;
  if (typeof window !== "undefined" && !window.confirm(t("page.supplierProducts.contact.deleteConfirm"))) return;

  companyContactSaving.value = true;
  shopErrorMessage.value = "";
  try {
    await deleteCompanyContact(row.contactId);
    shopNoticeKey.value = "page.supplierProducts.contact.deleted";
    await loadCompanyContacts();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.contact.deleteFailed");
  } finally {
    companyContactSaving.value = false;
  }
};

const addCompanyVessel = () => {
  if (companyVesselRows.value.some((row) => row.isNew)) return;
  companyVesselRows.value = [
    {
      id: `new-vessel-${Date.now()}`,
      vesselName: "",
      vesselType: "",
      buildDate: "",
      nextMaintenanceDate: "",
      capacity: "",
      status: "ACTIVE",
      remark: "",
      createdAt: "",
      updatedAt: "",
      isEditing: true,
      isNew: true,
      draft: {
        vesselName: "",
        vesselType: "",
        buildDate: "",
        nextMaintenanceDate: "",
        capacity: "",
        remark: ""
      }
    },
    ...companyVesselRows.value
  ];
};

const editCompanyVessel = (row: CompanyVesselRow) => {
  row.isEditing = true;
  row.draft = {
    vesselName: row.vesselName,
    vesselType: row.vesselType,
    buildDate: row.buildDate,
    nextMaintenanceDate: row.nextMaintenanceDate,
    capacity: row.capacity,
    remark: row.remark
  };
};

const cancelCompanyVesselEdit = (row: CompanyVesselRow) => {
  if (row.isNew) {
    companyVesselRows.value = companyVesselRows.value.filter((item) => item.id !== row.id);
    return;
  }
  row.isEditing = false;
  row.draft = {
    vesselName: row.vesselName,
    vesselType: row.vesselType,
    buildDate: row.buildDate,
    nextMaintenanceDate: row.nextMaintenanceDate,
    capacity: row.capacity,
    remark: row.remark
  };
};

const saveCompanyVessel = async (row: CompanyVesselRow) => {
  const draft = row.draft || row;
  const vesselName = draft.vesselName.trim();
  if (!vesselName) {
    shopErrorMessage.value = t("page.supplierProducts.vessel.nameRequired");
    return;
  }
  companyVesselSaving.value = true;
  shopErrorMessage.value = "";
  shopNoticeKey.value = "";
  try {
    const payload = {
      vesselName,
      vesselType: draft.vesselType.trim(),
      buildDate: draft.buildDate.trim(),
      nextMaintenanceDate: draft.nextMaintenanceDate.trim(),
      capacity: draft.capacity.trim(),
      remark: draft.remark.trim(),
      status: "ACTIVE"
    };
    if (row.vesselId) {
      await updateCompanyVessel(row.vesselId, payload);
      shopNoticeKey.value = "page.supplierProducts.vessel.updated";
    } else {
      await createCompanyVessel(payload);
      shopNoticeKey.value = "page.supplierProducts.vessel.created";
    }
    await loadCompanyVessels();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, row.vesselId ? "page.supplierProducts.vessel.updateFailed" : "page.supplierProducts.vessel.createFailed");
  } finally {
    companyVesselSaving.value = false;
  }
};

const removeCompanyVessel = async (row: CompanyVesselRow) => {
  if (row.isNew) {
    companyVesselRows.value = companyVesselRows.value.filter((item) => item.id !== row.id);
    return;
  }
  if (!row.vesselId) return;
  if (typeof window !== "undefined" && !window.confirm(t("page.supplierProducts.vessel.deleteConfirm"))) return;

  companyVesselSaving.value = true;
  shopErrorMessage.value = "";
  try {
    await deleteCompanyVessel(row.vesselId);
    shopNoticeKey.value = "page.supplierProducts.vessel.deleted";
    await loadCompanyVessels();
  } catch (error) {
    shopErrorMessage.value = getShopErrorText(error, "page.supplierProducts.vessel.deleteFailed");
  } finally {
    companyVesselSaving.value = false;
  }
};

const saveCompanyValueAddedServices = async () => {
  companyValueAddedServiceSaving.value = true;
  shopErrorMessage.value = "";
  shopNoticeKey.value = "";
  try {
    await updateCompanyValueAddedServices({
      freightPrice: parseCompareNonNegative(companyValueAddedServiceForm.value.freightPrice) ?? 0,
      customsPrice: parseCompareNonNegative(companyValueAddedServiceForm.value.customsPrice) ?? 0,
      cranePrice: parseCompareNonNegative(companyValueAddedServiceForm.value.cranePrice) ?? 0,
      remark: companyValueAddedServiceForm.value.remark.trim()
    });
    shopNoticeMessage.value = "增值服务费用已保存";
    await loadCompanyValueAddedServices();
  } catch (error) {
    shopErrorMessage.value = error instanceof Error && error.message ? error.message : "增值服务费用保存失败";
  } finally {
    companyValueAddedServiceSaving.value = false;
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
  if (event.key === "Escape" && activeTrafficShuttleNodeKey.value) {
    activeTrafficShuttleNodeKey.value = "";
    activeTrafficShuttleComparePickerKey.value = "";
    return;
  }
  if (event.key === "Escape" && shopListFullscreen.value) {
    shopListFullscreen.value = false;
  }
};

onUnmounted(() => {
  clearShopImportTimer();
  if (liveFilterTimer) window.clearTimeout(liveFilterTimer);
  purchaseOrderDetailScrollPanel?.removeEventListener("scroll", updatePurchaseOrderDetailBackTopVisibility);
  revokeProtectedImageObjectUrls();
  if (typeof window !== "undefined") {
    window.removeEventListener("keydown", handleShopFullscreenKeydown);
    window.cancelAnimationFrame(dashboardMetricAnimationFrame);
  }
});

const compareSkuColumns = computed<TableColumn[]>(() => [
  { key: "selection", label: t("table.index"), width: "72px", align: "center" },
  { key: "sourceSkuCode", label: t("compare.impaCnCode"), width: "20%" },
  { key: "quantity", label: t("compare.quantity"), width: "10%", align: "right" },
  { key: "matchedProductCode", label: t("compare.candidateCode"), width: "20%" },
  { key: "platformUnit", label: t("compare.candidateUnit"), width: "10%" },
  { key: "price", label: t("compare.unitPrice"), width: "10%", align: "center" },
  { key: "subtotal", label: t("compare.subtotal"), width: "10%", align: "center" },
  { key: "quoteMarkup", label: t("compare.profitPercent"), width: "10%", align: "center" },
  { key: "actualQuote", label: t("compare.quotePrice"), width: "10%", align: "center" }
]);

const purchaseOrderColumns = computed<TableColumn[]>(() => [
  { key: "sourceNo", label: t("purchaseOrder.field.sourceNo"), width: "158px" },
  { key: "vesselName", label: t("purchaseOrder.field.vesselName"), width: "136px" },
  { key: "itemCount", label: t("purchaseOrder.field.itemCount"), width: "82px", align: "right" },
  { key: "totalAmount", label: t("purchaseOrder.field.totalAmount"), width: "118px", align: "right" },
  { key: "status", label: t("field.status"), width: "126px" },
  { key: "createdAt", label: t("purchaseOrder.field.createdAt"), width: "132px" },
  { key: "operation", label: t("common.operation"), width: "112px", align: "center" }
]);

const supplierOrderColumns = computed<TableColumn[]>(() => [
  { key: "sourceNo", label: t("purchaseOrder.field.sourceNo"), width: "158px" },
  { key: "vesselName", label: t("purchaseOrder.field.vesselName"), width: "136px" },
  { key: "supplyPort", label: t("purchaseOrder.field.supplyPort"), width: "118px" },
  { key: "vesselEta", label: t("purchaseOrder.field.vesselEta"), width: "136px" },
  { key: "itemCount", label: t("purchaseOrder.field.itemCount"), width: "82px", align: "right" },
  { key: "totalAmount", label: t("purchaseOrder.field.totalAmount"), width: "118px", align: "right" },
  { key: "expectedReadyAt", label: t("purchaseOrder.field.expectedReadyAt"), width: "140px" },
  { key: "packagingMethod", label: t("purchaseOrder.field.packagingMethod"), width: "132px" },
  { key: "status", label: t("field.status"), width: "126px" },
  { key: "createdAt", label: t("purchaseOrder.field.createdAt"), width: "132px" },
  { key: "operation", label: t("common.operation"), width: "82px", align: "center" }
]);

const purchaseSupplierColumns = computed<TableColumn[]>(() => [
  { key: "supplierName", label: t("field.supplier") },
  { key: "packagingMethod", label: t("purchaseOrder.field.packagingMethod"), width: "132px" },
  { key: "status", label: t("field.status"), width: "126px" },
  { key: "itemCount", label: t("purchaseOrder.field.itemCount"), width: "82px", align: "right" },
  { key: "finalAmount", label: t("purchaseOrder.field.finalAmount"), width: "118px", align: "right" },
  { key: "expectedReadyAt", label: t("purchaseOrder.field.expectedReadyAt"), width: "140px" },
  { key: "operation", label: t("common.operation"), width: "88px", align: "center" }
]);

const purchaseItemColumns = computed<TableColumn[]>(() => [
  { key: "supplierSkuCode", label: t("purchaseOrder.field.supplierSkuCode"), width: "126px" },
  { key: "impaCode", label: t("field.impaCode"), width: "108px" },
  { key: "productName", label: t("field.item"), width: "240px" },
  { key: "specification", label: t("attr.spec"), width: "190px" },
  { key: "quantity", label: t("purchaseOrder.field.quantity"), width: "86px", align: "right" },
  { key: "unit", label: t("purchaseOrder.field.unit"), width: "72px" },
  { key: "unitPrice", label: t("field.price"), width: "96px", align: "right" },
  { key: "amount", label: t("field.amount"), width: "108px", align: "right" },
  { key: "flags", label: t("purchaseOrder.field.remark"), width: "140px" }
]);

const supplierColumns = computed<TableColumn[]>(() => [
  { key: "name", label: t("field.supplier"), width: "32%" },
  { key: "port", label: t("field.port"), width: "32%" },
  { key: "score", label: t("field.score"), width: "90px" },
  { key: "status", label: t("field.status"), width: "112px" },
  { key: "operation", label: t("common.operation"), width: "132px", align: "center" }
]);

const impaItemColumns = computed<TableColumn[]>(() => [
  { key: "impaCode", label: t("field.impaCode"), width: "118px" },
  { key: "cnCode", label: t("field.cnCode"), width: "118px" },
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
  { key: "operation", label: t("common.operation"), width: "108px", align: "center" }
]);

const inquiryDemandColumns = computed<TableColumn[]>(() => [
  { key: "demandNo", label: t("page.materialDemand.demandNo"), width: "150px" },
  { key: "vesselName", label: t("page.materialDemand.vesselName"), width: "132px" },
  { key: "inquiryDate", label: t("page.materialDemand.inquiryDate"), width: "112px" },
  { key: "sourceFileName", label: t("page.materialDemand.sourceFileName"), width: "168px" },
  { key: "matchSummary", label: t("page.materialDemand.matchSummary"), width: "132px", align: "center" },
  { key: "status", label: t("field.status"), width: "92px" },
  { key: "updatedAt", label: t("field.updatedAt"), width: "136px" },
  { key: "operation", label: t("common.operation"), width: "108px", align: "center" }
]);

const comparisonDemandColumns = computed<TableColumn[]>(() => [
  { key: "demandNo", label: t("page.materialDemand.demandNo"), width: "150px" },
  { key: "applicationNo", label: t("page.materialDemand.applicationNo"), width: "132px" },
  { key: "vesselName", label: t("page.materialDemand.vesselName"), width: "132px" },
  { key: "inquiryDate", label: t("page.materialDemand.inquiryDate"), width: "112px" },
  { key: "skuCount", label: t("page.materialDemand.skuCount"), width: "82px", align: "right" },
  { key: "status", label: t("field.status"), width: "92px" },
  { key: "operation", label: t("common.operation"), width: "108px", align: "center" }
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
  return sortNewestFirst(maps[pageKey.value] ?? [], ["updatedAt", "createdAt", "orderDate", "validUntil"], ["id", "code"]);
});

const procurementOrderRows = computed<ProcurementOrderRow[]>(() =>
  sortNewestFirst(pageKey.value === "foodOrders" ? foodOrderRows : [], ["updatedAt", "createdAt", "orderDate", "deliveryDate"], ["id", "orderNo"])
);

const displayCurrencySymbol = (currency?: string | null) => {
  const value = normalizeCompareText(currency).toUpperCase();
  if (!value || value === "CNY" || value === "RMB" || value === "¥") return "¥";
  if (value === "USD" || value === "$") return "$";
  if (value === "EUR" || value === "€") return "€";
  if (value === "HKD") return "HK$";
  return normalizeCompareText(currency) || "¥";
};

const formatDisplayMoney = (amount?: number, currency?: string | null) => {
  if (amount == null || !Number.isFinite(amount)) return "-";
  return `${displayCurrencySymbol(currency)} ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
};

const dualMoneyLines = (cny?: number, usd?: number) => ({
  cny: formatDisplayMoney(cny, "CNY"),
  usd: formatDisplayMoney(usd, "USD")
});

const CNY_PER_USD = 7;

const ceilMoneyToCents = (value?: number | null) => {
  if (value == null || !Number.isFinite(value)) return undefined;
  return Math.ceil((value - Number.EPSILON) * 100) / 100;
};

const cnyToDisplayCurrencyAmount = (cny?: number, usd?: number): number | undefined => {
  if (cny != null && Number.isFinite(cny)) return cny;
  return usd != null && Number.isFinite(usd) ? usd : undefined;
};

const displayCurrencyAmountToCnyValue = (value?: number | null): number | undefined => {
  if (value == null || !Number.isFinite(value)) return undefined;
  return ceilMoneyToCents(value);
};

const displayCurrencyAmountToCny = (value?: string | number | null): number | undefined => {
  const parsed = parseCompareQuantity(value);
  if (parsed == null) return undefined;
  return displayCurrencyAmountToCnyValue(parsed);
};

const formatCompareMoney = (cny?: number, usd?: number) =>
  formatDisplayMoney(cnyToDisplayCurrencyAmount(cny, usd), compareDisplayCurrency.value);

const compareDisplayCurrencySymbol = computed(() => (compareDisplayCurrency.value === "USD" ? "$" : "¥"));

const compareFixedFeeTotal = computed(() =>
  ["freight", "customs", "crane", "other"].reduce((sum, key) => sum + (displayCurrencyAmountToCny(compareFixedFeeInputs.value[key as "freight" | "customs" | "crane" | "other"]) ?? 0), 0)
);

const compareFixedFeeTotalUsd = computed(() => compareFixedFeeTotal.value);

const normalizeCompareFeeInput = (value?: number | null) => {
  if (value == null || !Number.isFinite(value) || value <= 0) return "";
  return String(Math.round(value * 100) / 100);
};

const normalizeCompareFeeDisplayInput = (cnyValue?: number | null) => {
  const value = cnyValue == null ? undefined : cnyValue;
  return normalizeCompareFeeInput(value);
};

const compareTrafficLockedShuttleLabel = (trafficService?: MaterialDemandTrafficService) => {
  if (!trafficService) return "";
  if (trafficService.fixedProviderType === "SUPPLIER") return normalizeCompareText(trafficService.fixedProviderName);
  if (trafficService.fixedProviderName && !trafficService.shuttleNo && !trafficService.trafficVesselName) return normalizeCompareText(trafficService.fixedProviderName);
  return [trafficService.shuttleNo, trafficService.trafficVesselName].filter(Boolean).join(" / ");
};

const compareProviderLabel = computed(() => (compareSupplyForm.value.supplyMode === "LAND" ? "供货商" : "驳船"));
const compareProviderPlaceholder = computed(() => (compareSupplyForm.value.supplyMode === "LAND" ? "选择供货商" : "选择驳船服务"));

const compareSupplierProviderOptions = computed(() => {
  const source = compareData.value;
  const map = new Map<string, { id: string; companyId?: number; supplierName: string; skuCount: number; amount?: number; amountUsd?: number }>();
  const addSupplier = (supplier: { companyId?: number; supplierId?: number; supplierName?: string; skuCount?: number; matchedCount?: number; totalAmount?: number; totalAmountUsd?: number; amount?: number }) => {
    const supplierName = normalizeCompareText(supplier.supplierName);
    if (!supplierName) return;
    const id = String(supplier.companyId ?? supplier.supplierId ?? supplierName);
    const current = map.get(id);
    map.set(id, {
      id,
      companyId: supplier.companyId,
      supplierName,
      skuCount: Math.max(current?.skuCount ?? 0, supplier.skuCount ?? supplier.matchedCount ?? 0),
      amount: supplier.totalAmount ?? supplier.amount ?? current?.amount,
      amountUsd: supplier.totalAmountUsd ?? current?.amountUsd
    });
  };
  source?.strategies.forEach((strategy) => strategy.suppliers.forEach(addSupplier));
  source?.items.forEach((item) => {
    item.candidates.forEach((candidate) =>
      addSupplier({
        companyId: candidate.companyId,
        supplierName: candidate.supplierName,
        skuCount: 1,
        totalAmount: candidate.lineAmount,
        totalAmountUsd: candidate.lineAmountUsd
      })
    );
  });
  return [...map.values()].sort((a, b) => a.supplierName.localeCompare(b.supplierName, "zh-Hans-CN"));
});

const openCompareFixedProviderDialog = () => {
  if (compareSupplyForm.value.supplyMode === "LAND") {
    compareSupplierDialogOpen.value = true;
    compareSupplierError.value = "";
    void loadCompareSupplierValueServices();
    return;
  }
  void openCompareTrafficDialog();
};

const applyCompareSupplyModeFromTrafficService = (trafficService?: MaterialDemandTrafficService) => {
  const mode = trafficService?.supplyMode === "LAND" || trafficService?.fixedProviderType === "SUPPLIER" ? "LAND" : "SEA";
  compareSupplyForm.value.supplyMode = mode;
  compareFixedProvider.value = {
    type: mode === "LAND" ? "SUPPLIER" : "BARGE",
    id: normalizeCompareText(trafficService?.fixedProviderId),
    name: normalizeCompareText(trafficService?.fixedProviderName) || compareTrafficLockedShuttleLabel(trafficService)
  };
};

const compareSupplierFeeValues = (optionId: string) =>
  compareSupplierValueServiceMap.value[optionId] || {
    freightPrice: "",
    customsPrice: "",
    cranePrice: "",
    total: 0
  };

const compareSupplierFeeLabel = (optionId: string, key: "freightPrice" | "customsPrice" | "cranePrice") => {
  const cny = parseCompareNonNegative(compareSupplierFeeValues(optionId)[key]) ?? 0;
  return formatCompareMoney(cny, cny / CNY_PER_USD);
};

const compareSupplierFeeTotalLabel = (optionId: string) => {
  const cny = compareSupplierFeeValues(optionId).total;
  return formatCompareMoney(cny, cny / CNY_PER_USD);
};

const compareSupplyModeLabel = computed(() => (compareSupplyForm.value.supplyMode === "LAND" ? "陆运" : "海运"));
const compareFixedProviderDisplayName = computed(() => normalizeCompareText(compareFixedFeeInputs.value.shuttle) || compareFixedProvider.value.name || "-");
const compareFixedFeeDisplayItems = computed(() => [
  { key: "freight", label: "运费", value: compareFixedFeeInputs.value.freight },
  { key: "customs", label: "报关费", value: compareFixedFeeInputs.value.customs },
  { key: "crane", label: "吊机费", value: compareFixedFeeInputs.value.crane },
  { key: "other", label: "其他费用", value: compareFixedFeeInputs.value.other }
]);
const compareFixedFeeDisplayLabel = (value?: string | number | null) => {
  const cny = displayCurrencyAmountToCny(value) ?? 0;
  return formatCompareMoney(cny, cny / CNY_PER_USD);
};
const purchaseOrderLinkedBarge = computed(() => {
  const fixedId = normalizeCompareText(compareTrafficServiceForm.value.fixedProviderId);
  const shuttleNo = normalizeCompareText(compareTrafficServiceForm.value.shuttleNo);
  const vesselName = normalizeCompareText(compareTrafficServiceForm.value.trafficVesselName);
  return compareTrafficShuttleRows.value.find((row) => {
    const rowId = normalizeCompareText(row.shuttleId);
    const rowNo = normalizeCompareText(row.shuttleNo);
    const rowVessel = normalizeCompareText(row.trafficVesselName);
    return (fixedId && rowId === fixedId) || (shuttleNo && rowNo === shuttleNo) || (vesselName && rowVessel === vesselName);
  }) || null;
});
const purchaseOrderBargeStartTimeValue = computed(() =>
  normalizeCompareText(purchaseOrderLinkedBarge.value?.startTime) ||
  normalizeCompareText(compareTrafficServiceForm.value.startTime) ||
  normalizeCompareText(compareTrafficServiceForm.value.useTime)
);
const purchaseOrderBargeReturnTimeValue = computed(() =>
  normalizeCompareText(purchaseOrderLinkedBarge.value?.returnTime) || normalizeCompareText(compareTrafficServiceForm.value.returnTime)
);
const purchaseOrderBargeDeparturePointLabel = computed(() =>
  normalizeCompareText(purchaseOrderLinkedBarge.value?.departurePoint) || normalizeCompareText(compareTrafficServiceForm.value.departurePoint) || "-"
);
const purchaseOrderBargeDestinationPointLabel = computed(() =>
  normalizeCompareText(purchaseOrderLinkedBarge.value?.destinationPoint) ||
  normalizeCompareText(purchaseOrderLinkedBarge.value?.anchorageName) ||
  normalizeCompareText(compareTrafficServiceForm.value.destinationPoint) ||
  normalizeCompareText(compareTrafficServiceForm.value.anchorageName) ||
  selectedCompareTrafficAnchorageName() ||
  "-"
);
const purchaseOrderBargeDateLabel = computed(() =>
  formatDateTimeParts(purchaseOrderBargeStartTimeValue.value || purchaseOrderForm.value.vesselEta || compareSupplyForm.value.date || "").date
);
const purchaseOrderBargeOriginTimeLabel = computed(() =>
  formatDateTimeParts(purchaseOrderBargeStartTimeValue.value || purchaseOrderForm.value.vesselEta || compareSupplyForm.value.date || "").time || "-"
);
const purchaseOrderBargeDestinationTimeLabel = computed(() =>
  formatDateTimeParts(purchaseOrderBargeReturnTimeValue.value).time || "-"
);
const purchaseOrderBargeBasePrice = computed(() => {
  const direct = Number(compareTrafficServiceForm.value.basePrice);
  if (Number.isFinite(direct) && direct > 0) return direct;
  return displayCurrencyAmountToCny(compareFixedFeeInputs.value.freight) ?? 0;
});
const purchaseOrderBargeSharedPrice = computed(() => {
  const direct = Number(compareTrafficServiceForm.value.sharedPrice);
  if (Number.isFinite(direct) && direct > 0) return direct;
  return displayCurrencyAmountToCny(compareFixedFeeInputs.value.freight) ?? 0;
});
const purchaseOrderBargeSelectedFeeItems = computed(() =>
  compareFixedFeeDisplayItems.value
    .filter((fee) => ["freight", "customs", "crane"].includes(fee.key))
    .map((fee) => ({ ...fee, label: fee.key === "freight" ? "运费" : fee.label }))
);
const purchaseOrderBargeNodeRows = computed(() => {
  const nodes = purchaseOrderLinkedBarge.value?.serviceNodes?.length
    ? purchaseOrderLinkedBarge.value.serviceNodes
    : compareTrafficServiceForm.value.serviceNodes || [];
  return nodes.length ? nodes : defaultTrafficShuttleNodes();
});

const loadCompareSupplierValueServices = async () => {
  const options = compareSupplierProviderOptions.value;
  if (!options.length) return;
  compareSupplierLoading.value = true;
  compareSupplierError.value = "";
  try {
    const entries = await Promise.all(
      options.map(async (option) => {
        const payload = option.companyId == null ? null : await getCompanyValueAddedServices(option.companyId);
        const services = normalizeCompanyValueAddedService(payload);
        const freight = parseCompareNonNegative(services.freightPrice) ?? 0;
        const customs = parseCompareNonNegative(services.customsPrice) ?? 0;
        const crane = parseCompareNonNegative(services.cranePrice) ?? 0;
        return [
          option.id,
          {
            freightPrice: services.freightPrice,
            customsPrice: services.customsPrice,
            cranePrice: services.cranePrice,
            total: freight + customs + crane
          }
        ] as const;
      })
    );
    compareSupplierValueServiceMap.value = Object.fromEntries(entries);
  } catch (error) {
    compareSupplierError.value = error instanceof Error && error.message ? error.message : "供货商增值服务费用读取失败";
  } finally {
    compareSupplierLoading.value = false;
  }
};

const handleCompareSupplyModeChange = () => {
  compareFixedProvider.value = {
    type: compareSupplyForm.value.supplyMode === "LAND" ? "SUPPLIER" : "BARGE",
    id: "",
    name: ""
  };
  compareFixedFeeInputs.value.shuttle = compareSupplyForm.value.supplyMode === "SEA"
    ? [compareTrafficServiceForm.value.shuttleNo, compareTrafficServiceForm.value.trafficVesselName].filter(Boolean).join(" / ")
    : "";
};

const selectCompareSupplierProvider = async (option: { id: string; companyId?: number; supplierName: string }) => {
  compareSupplierLoading.value = true;
  compareSupplierError.value = "";
  try {
    if (!compareSupplierValueServiceMap.value[option.id]) {
      await loadCompareSupplierValueServices();
    }
    const services = compareSupplierFeeValues(option.id);
    compareFixedProvider.value = {
      type: "SUPPLIER",
      id: option.id,
      name: option.supplierName
    };
    compareFixedFeeInputs.value.shuttle = option.supplierName;
    compareFixedFeeInputs.value.freight = normalizeCompareFeeDisplayInput(parseCompareNonNegative(services.freightPrice));
    compareFixedFeeInputs.value.customs = normalizeCompareFeeDisplayInput(parseCompareNonNegative(services.customsPrice));
    compareFixedFeeInputs.value.crane = normalizeCompareFeeDisplayInput(parseCompareNonNegative(services.cranePrice));
    compareSupplierDialogOpen.value = false;
  } catch (error) {
    compareSupplierError.value = error instanceof Error && error.message ? error.message : "供货商增值服务费用读取失败";
  } finally {
    compareSupplierLoading.value = false;
  }
};

const compareMoneyInputValue = (cny?: number, usd?: number) => {
  const value = cnyToDisplayCurrencyAmount(cny, usd);
  const rounded = ceilMoneyToCents(value);
  return rounded == null || !Number.isFinite(rounded) ? "" : rounded.toFixed(2);
};

const normalizeCompareText = (value?: string | number | null) => {
  if (value == null) return "";
  const text = String(value).trim();
  return text && text !== "undefined" && text !== "null" ? text : "";
};

const mojibakePattern = /[ÃÂÄÅÆÇÈÉÖÜÑãäåæçèéêìíîïðñòóôõöøùúûüýÿ\u0080-\u009f]/;

const repairMojibakeText = (value?: string | number | null) => {
  const text = normalizeCompareText(value);
  if (!text || !mojibakePattern.test(text)) return text;
  try {
    const bytes = new Uint8Array(Array.from(text, (char) => char.charCodeAt(0) & 0xff));
    const decoded = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    return /[\u4e00-\u9fff]/.test(decoded) ? decoded : text;
  } catch {
    return text;
  }
};

const displayOrderText = (value?: unknown) =>
  repairMojibakeText(typeof value === "string" || typeof value === "number" ? value : null) || "-";

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

const replacementCandidateSpecText = (candidate: MaterialComparisonCandidate) =>
  normalizeCompareText(candidate.attributeSummary) || normalizeCompareText(candidate.specification) || readCompareSpecText(candidate.specifications) || normalizeCompareText(candidate.packageSpec) || "-";

const compareItemLowestPrice = (item: MaterialComparisonItem) => {
  const prices = item.candidates.map((candidate) => candidate.unitPrice).filter((price): price is number => price != null && Number.isFinite(price));
  return prices.length ? Math.min(...prices) : undefined;
};

const compareDemandItemKey = (item: MaterialComparisonItem, fallback: number | string) => String(item.demandItemId ?? item.rowNo ?? fallback);

const parseCompareQuantity = (value?: string | number | null) => {
  const text = normalizeCompareText(value);
  if (!text) return undefined;
  const normalized = text.replace(/,/g, "");
  const quantity = Number(normalized);
  return Number.isFinite(quantity) && quantity > 0 ? quantity : undefined;
};

const parseCompareNonNegative = (value?: string | number | null) => {
  const text = normalizeCompareText(value);
  if (!text) return undefined;
  const normalized = text.replace(/,/g, "");
  const number = Number(normalized);
  return Number.isFinite(number) && number >= 0 ? number : undefined;
};

const isCompareRowOrderable = (row: CompareSkuRow) => row.price > 0 && row.pricingQuantity != null && Number.isFinite(row.pricingQuantity) && row.pricingQuantity > 0;

const compareCandidateToRow = (item: MaterialComparisonItem, candidate: MaterialComparisonCandidate, candidateIndex: number, strategyKey: string): CompareSkuRow => {
  const specText = normalizeCompareText(candidate.attributeSummary) || readCompareSpecText(candidate.specifications) || normalizeCompareText(item.specification);
  const supplierName = normalizeCompareText(candidate.supplierName) || "-";
  const image = normalizeCompareText(candidate.thumbnailUrl) || normalizeCompareText(candidate.imageUrl);
  const demandLineKey = compareDemandItemKey(item, candidateIndex);
  const unitOptions = candidate.unitPriceOptions?.filter((option) => option.unit && option.unitPrice != null) ?? [];
  const defaultUnitOption = unitOptions.find((option) => option.defaultSelected) ?? unitOptions[0];
  const selectedUnit = normalizeCompareText(compareUnitSelections.value[demandLineKey]) || normalizeCompareText(candidate.selectedUnit) || normalizeCompareText(defaultUnitOption?.unit) || normalizeCompareText(candidate.stockUnit) || normalizeCompareText(candidate.unit);
  const selectedUnitOption = unitOptions.find((option) => normalizeCompareText(option.unit) === selectedUnit) ?? defaultUnitOption;
  const price = selectedUnitOption?.unitPrice ?? candidate.unitPrice ?? 0;
  const unitPriceUsd = selectedUnitOption?.unitPriceUsd ?? candidate.unitPriceUsd;
  const sourceQuantity = normalizeCompareText(item.quantity);
  const quantityText = compareQuantityInputs.value[demandLineKey] ?? sourceQuantity;
  const remarks = compareRemarkInputs.value[demandLineKey] ?? normalizeCompareText(item.remarks);
  const pricingQuantity = parseCompareQuantity(quantityText);
  const quantityMissing = !sourceQuantity;
  const rowMarkup = parseCompareNonNegative(compareQuoteMarkupInputs.value[demandLineKey]);
  const markupPercent = rowMarkup ?? compareQuoteMarkupPercent.value;
  const actualQuoteText = compareQuoteActualPrices.value[demandLineKey] ?? "";
  const actualQuotePrice = ceilMoneyToCents(parseCompareQuantity(actualQuoteText) ?? (price > 0 ? price * (1 + markupPercent / 100) : undefined));
  const actualQuoteSubtotal = actualQuotePrice != null && pricingQuantity ? ceilMoneyToCents(actualQuotePrice * pricingQuantity) : undefined;
  const rowId = `${strategyKey}:${selectedCompareSupplier.value || "strategy"}:${demandLineKey}:${candidateIndex}`;
  const attributes: SkuAttribute[] = [
    { key: "spec", labelKey: "attr.spec", value: specText || "-" },
    { key: "packing", labelKey: "attr.packing", value: normalizeCompareText(candidate.packageSpec) || "-" },
    { key: "match", labelKey: "page.materials.supplierMatch", value: normalizeCompareText(candidate.matchType) || normalizeCompareText(candidate.reason) || "-" }
  ];
  return {
    id: rowId,
    supplierKey: candidateSupplierKey(strategyKey, candidate, candidateIndex),
    supplierName,
    demandItemId: demandLineKey,
    sourceSkuCode: normalizeCompareText(item.sourceSkuCode) || normalizeCompareText(item.impaCode) || normalizeCompareText(item.platformCode) || "-",
    sourceSkuName: normalizeCompareText(item.sourceSkuName) || normalizeCompareText(item.productName) || normalizeCompareText(item.description) || "-",
    matchedProductCode: normalizeCompareText(candidate.impaCode) || normalizeCompareText(candidate.platformCode) || normalizeCompareText(item.impaCode) || normalizeCompareText(item.platformCode) || "-",
    rowNo: item.rowNo,
    quantity: quantityText,
    remarks,
    pricingQuantity,
    unit: normalizeCompareText(item.unit),
    platformUnit: selectedUnit || "-",
    selectedUnit,
    unitPriceUsd,
    unitPriceOptions: unitOptions,
    unitMismatchFlag: Boolean(item.unit && selectedUnit && normalizeCompareText(item.unit).toUpperCase() !== selectedUnit.toUpperCase()),
    quantityMissingFlag: quantityMissing,
    quantityInvalidFlag: !pricingQuantity,
    quantityFallbackFlag: Boolean(item.pricingQuantityNote && !quantityText),
    subtotal: actualQuoteSubtotal ?? 0,
    subtotalUsd: actualQuoteSubtotal,
    actualQuotePrice,
    actualQuoteSubtotal,
    quoteMarkupPercent: markupPercent,
    candidate,
    lowestPrice: compareItemLowestPrice(item),
    matchType: candidate.matchType,
    reason: candidate.reason,
    thumbnail: image,
    images: image ? [{ src: image, alt: normalizeCompareText(candidate.productName) || normalizeCompareText(item.productName) || supplierName }] : [],
    name: normalizeCompareText(candidate.productName) || normalizeCompareText(item.productName) || "-",
    itemNo: normalizeCompareText(candidate.supplierSkuCode) || String(candidate.skuId ?? "-"),
    impaCode: normalizeCompareText(candidate.impaCode) || normalizeCompareText(candidate.platformCode) || normalizeCompareText(item.impaCode) || normalizeCompareText(item.platformCode) || "-",
    supplier: supplierName,
    price,
    currency: normalizeCompareText(candidate.currencySymbol) || normalizeCompareText(candidate.currency) || "CNY",
    stock: candidate.stockQty ?? 0,
    status: candidate.unitPrice != null ? "success" : "warning",
    attributes
  };
};

const compareUnmatchedItemToRow = (item: MaterialComparisonItem, itemIndex: number, strategyKey: string): CompareSkuRow => {
  const demandLineKey = compareDemandItemKey(item, itemIndex);
  const quantityText = compareQuantityInputs.value[demandLineKey] ?? normalizeCompareText(item.quantity);
  const remarks = compareRemarkInputs.value[demandLineKey] ?? normalizeCompareText(item.remarks);
  return {
    id: `${strategyKey}:unmatched:${demandLineKey}`,
    supplierKey: `${strategyKey}:unmatched`,
    supplierName: "-",
    demandItemId: demandLineKey,
    unmatched: true,
    sourceSkuCode: normalizeCompareText(item.sourceSkuCode) || normalizeCompareText(item.impaCode) || normalizeCompareText(item.platformCode) || "-",
    sourceSkuName: normalizeCompareText(item.sourceSkuName) || normalizeCompareText(item.productName) || normalizeCompareText(item.description) || "-",
    matchedProductCode: "-",
    rowNo: item.rowNo,
    quantity: quantityText,
    remarks,
    pricingQuantity: parseCompareQuantity(quantityText),
    unit: normalizeCompareText(item.unit),
    platformUnit: "-",
    selectedUnit: "",
    unitPriceOptions: [],
    quantityMissingFlag: !normalizeCompareText(item.quantity),
    quantityInvalidFlag: !parseCompareQuantity(quantityText),
    quantityFallbackFlag: Boolean(item.pricingQuantityNote && !quantityText),
    subtotal: 0,
    actualQuotePrice: undefined,
    actualQuoteSubtotal: undefined,
    quoteMarkupPercent: compareQuoteMarkupPercent.value,
    candidate: {} as MaterialComparisonCandidate,
    lowestPrice: undefined,
    matchType: "UNMATCHED",
    reason: item.emptyReason,
    thumbnail: "",
    images: [],
    name: "-",
    itemNo: "-",
    impaCode: normalizeCompareText(item.impaCode) || normalizeCompareText(item.platformCode) || "-",
    supplier: "-",
    price: 0,
    currency: "CNY",
    stock: 0,
    status: "danger",
    attributes: [
      { key: "spec", labelKey: "attr.spec", value: normalizeCompareText(item.specification) || "-" },
      { key: "reason", labelKey: "page.materials.reason", value: normalizeCompareText(item.emptyReason) || "-" }
    ]
  };
};

const compareRowsForStrategy = (strategyKey: string): CompareSkuRow[] => {
  if (!compareData.value) return [];
  return compareData.value.items.flatMap((item, itemIndex) => {
    const candidate = strategyKey === "SINGLE_SUPPLIER" ? item.singleSupplierCandidate : item.lowestCandidate;
    return candidate ? [compareCandidateToRow(item, candidate, itemIndex * 1000, strategyKey)] : [];
  });
};

const demandItemIdFromCompareRowId = (rowId: string) => {
  const parts = rowId.split(":");
  return parts.length >= 2 ? parts[parts.length - 2] : "";
};

const selectedCompareDemandItemIds = computed(() =>
  new Set(selectedCompareRowIds.value.map(demandItemIdFromCompareRowId).filter(Boolean))
);

const compareStrategyCards = computed<CompareStrategyCard[]>(() =>
  (compareData.value?.strategies ?? []).map((strategy) => {
    const rows = compareRowsForStrategy(strategy.strategyType);
    const selectedIds = selectedCompareDemandItemIds.value;
    const rowsForTotals = selectedIds.size ? rows.filter((row) => selectedIds.has(String(row.demandItemId)) && isCompareRowOrderable(row)) : rows;
    const costAmount = rowsForTotals.reduce((sum, row) => {
      const quantity = row.pricingQuantity && Number.isFinite(row.pricingQuantity) ? row.pricingQuantity : 0;
      const price = Number.isFinite(row.price) ? row.price : 0;
      return sum + price * quantity;
    }, 0);
    const costAmountUsd = rowsForTotals.reduce((sum, row) => {
      const quantity = row.pricingQuantity && Number.isFinite(row.pricingQuantity) ? row.pricingQuantity : 0;
      const price = row.unitPriceUsd != null && Number.isFinite(row.unitPriceUsd) ? row.unitPriceUsd : 0;
      return sum + price * quantity;
    }, 0);
    const quoteAmount = rowsForTotals.reduce((sum, row) => sum + (Number.isFinite(row.subtotal) ? row.subtotal : 0), 0);
    const quoteAmountUsd = rowsForTotals.reduce((sum, row) => sum + (row.subtotalUsd != null && Number.isFinite(row.subtotalUsd) ? row.subtotalUsd : 0), 0);
    const fixedFeeAmount = compareFixedFeeTotal.value;
    const fixedFeeAmountUsd = compareFixedFeeTotalUsd.value;
    const profitAmount = Math.max(0, quoteAmount - costAmount - fixedFeeAmount);
    const profitAmountUsd = Math.max(0, quoteAmountUsd - costAmountUsd - fixedFeeAmountUsd);
    return {
      key: strategy.strategyType,
      label: strategy.strategyName || (strategy.strategyType === "SINGLE_SUPPLIER" ? t("compare.singleSupplier") : t("compare.lowestMixed")),
      matchSummary: `${rowsForTotals.length}/${strategy.totalCount}`,
      costTotal: formatCompareMoney(costAmount, costAmountUsd),
      fixedFeeTotal: formatCompareMoney(fixedFeeAmount, fixedFeeAmountUsd),
      quoteTotal: formatCompareMoney(quoteAmount, quoteAmountUsd),
      profitTotal: formatCompareMoney(profitAmount, profitAmountUsd),
      tone: strategy.strategyType === "SINGLE_SUPPLIER" ? "blue" : "green",
      enabled: strategy.enabled !== false,
      disabledReason: strategy.disabledReason,
      suppliers: strategy.suppliers.map((supplier, supplierIndex) => {
        const supplierRows = rowsForTotals.filter((row) => sameCompareSupplier(row.candidate, supplier));
        const supplierCostAmount = supplierRows.reduce((sum, row) => {
          const quantity = row.pricingQuantity && Number.isFinite(row.pricingQuantity) ? row.pricingQuantity : 0;
          const price = Number.isFinite(row.price) ? row.price : 0;
          return sum + price * quantity;
        }, 0);
        const supplierCostAmountUsd = supplierRows.reduce((sum, row) => {
          const quantity = row.pricingQuantity && Number.isFinite(row.pricingQuantity) ? row.pricingQuantity : 0;
          const price = row.unitPriceUsd != null && Number.isFinite(row.unitPriceUsd) ? row.unitPriceUsd : 0;
          return sum + price * quantity;
        }, 0);
        return {
          key: compareSupplierKey(strategy, supplier, supplierIndex),
          supplier: normalizeCompareText(supplier.supplierName) || "-",
          skuCount: supplierRows.length,
          amount: formatCompareMoney(
            supplierCostAmount,
            supplierCostAmountUsd
          )
        };
      })
    };
  })
);

const compareSupplyEditableFields = computed<Array<{ key: CompareSupplyEditableKey; label: string; inputType: string }>>(() => [
  { key: "vessel", label: t("compare.supply.vessel"), inputType: "text" },
  { key: "inquiryNo", label: t("filter.inquiryNo"), inputType: "text" },
  { key: "materialType", label: t("page.materials.materialType"), inputType: "text" },
  { key: "port", label: t("compare.supply.port"), inputType: "text" },
  { key: "date", label: t("compare.supply.date"), inputType: "datetime" }
]);

const compareWeatherInfo = computed(() => ({
  label: t("compare.supply.weather"),
  value:
    normalizeCompareText(compareData.value?.supplyInfo?.weather) ||
    normalizeCompareText(compareData.value?.supplyInfo?.weatherInfo) ||
    normalizeCompareText(compareData.value?.supplyInfo?.weatherText) ||
    t("compare.supply.weatherValue")
}));

const compareWeatherTone = computed(() => {
  const value = compareWeatherInfo.value.value;
  return value.includes("八级") || value.includes("雾") ? "danger" : "normal";
});

const activeCompareStrategy = computed(() => compareStrategyCards.value.find((strategy) => strategy.key === selectedStrategy.value) ?? compareStrategyCards.value[0] ?? null);

const activeCompareSupplierKeys = computed(() =>
  selectedCompareSupplier.value ? [selectedCompareSupplier.value] : activeCompareStrategy.value?.suppliers.map((supplier) => supplier.key) ?? []
);

const compareSkuRows = computed<CompareSkuRow[]>(() => {
  if (!compareData.value || !activeCompareStrategy.value) return [];
  const strategyKey = activeCompareStrategy.value.key;
  if (selectedCompareSupplier.value) {
    return compareRowsForStrategy(strategyKey).filter((row) => row.supplierKey === selectedCompareSupplier.value);
  }
  return compareData.value.items.flatMap((item, itemIndex) => {
    let candidates: MaterialComparisonCandidate[] = [];
    const replacement = item.demandItemId != null ? compareRowReplacements.value[String(item.demandItemId)] : undefined;
    if (strategyKey === "SINGLE_SUPPLIER") {
      candidates = item.singleSupplierCandidate ? [item.singleSupplierCandidate] : [];
    } else {
      candidates = item.lowestCandidate ? [item.lowestCandidate] : [];
    }
    if (replacement && !selectedCompareSupplier.value) candidates = [replacement];
    if (!candidates.length) return [compareUnmatchedItemToRow(item, itemIndex, strategyKey)];
    return candidates.map((candidate, candidateIndex) => compareCandidateToRow(item, candidate, itemIndex * 1000 + candidateIndex, strategyKey));
  });
});

const compareFilteredSkus = computed<CompareSkuRow[]>(() =>
  compareSkuRows.value.filter((row) => {
    const supplierMatches = row.unmatched
      ? !selectedCompareSupplier.value
      : activeCompareSupplierKeys.value.length === 0 || activeCompareSupplierKeys.value.includes(row.supplierKey);
    const keyword = compareSkuKeyword.value.trim().toLowerCase();
    const keywordMatches =
      !keyword ||
      [row.impaCode, row.itemNo, row.name, row.supplier, ...row.attributes.map((attribute) => attribute.value)].some((value) => value.toLowerCase().includes(keyword));
    const priceMatches = !comparePreferenceFilters.value.includes("priceLow") || row.lowestPrice == null || row.price <= row.lowestPrice;
    return supplierMatches && keywordMatches && priceMatches;
  }).map((row, index) => ({ ...row, displayNo: index + 1 }))
);

const compareSkuRowClass = (row: Record<string, unknown>) => {
  const compareRow = row as unknown as CompareSkuRow;
  const isOrderable = isCompareRowOrderable(compareRow);
  const isSelected = selectedCompareRowIds.value.includes(compareRow.id);
  return {
    "is-compare-unmatched": Boolean(row.unmatched),
    "is-compare-deselected": isOrderable && !isSelected
  };
};

const filteredReplacementCandidates = computed(() => {
  const keyword = replacementSearchKeyword.value.trim().toLowerCase();
  const rows = replacementCandidates.value.slice(0, 50);
  if (!keyword) return rows;
  return rows.filter((candidate) =>
    [
      candidate.productName,
      candidate.supplierName,
      candidate.impaCode,
      candidate.platformCode,
      candidate.supplierSkuCode,
      candidate.skuId
    ]
      .map((value) => normalizeCompareText(value))
      .some((value) => value.toLowerCase().includes(keyword))
  );
});

const purchaseOrderRows = computed(() =>
  purchaseOrders.value.map((row) => ({
    ...row,
    sourceNo: row.inquiryNo || row.demandNo || "-",
    supplierQuoteRatio: `${row.quotedSupplierCount ?? row.supplierCount ?? 0}/${row.totalSupplierCount ?? row.supplierCount ?? 0}`
  })) as unknown as Record<string, unknown>[]
);

const supplierPurchaseOrderRows = computed(() =>
  supplierPurchaseOrders.value.map((row) => ({
    ...row,
    sourceNo: row.inquiryNo || row.demandNo || "-",
    supplyMeta: [displaySupplyPort(row.supplyPort), row.vesselEta ? formatDemandDateTime(row.vesselEta) : ""].filter(Boolean).join(" / ") || "-"
  })) as unknown as Record<string, unknown>[]
);

const demandMatchRate = (row: MaterialDemandSummary | Record<string, unknown>) => {
  const demand = row as MaterialDemandSummary;
  const total = Number(demand.skuCount || 0);
  const matched = Number(demand.exactCount || 0) + Number(demand.similarCount || 0);
  return total > 0 ? Math.max(0, Math.min(100, Math.round((matched / total) * 100))) : 0;
};

const demandDisplayNo = (row: MaterialDemandSummary | Record<string, unknown>) => {
  const demand = row as MaterialDemandSummary;
  return demand.inquiryNo || demand.demandNo || demand.applicationNo || "-";
};

const purchaseOrderQueryStageLabels = ["确认中", "备货中", "运输中", "待补给", "补给中", "补给完成"];
const purchaseOrderStatusStageIndex = (statusValue?: string | null) => {
  const status = String(statusValue || "").toUpperCase();
  if (!status || status === "PENDING_SUPPLIER_CONFIRM") return 0;
  if (["PARTIALLY_CONFIRMED", "PREPARING"].includes(status)) return 1;
  if (["PARTIALLY_READY", "READY_TO_DELIVER", "IN_TRANSIT"].includes(status)) return 2;
  if (["WAITING_SUPPLY", "WAITING_SERVICE"].includes(status)) return 3;
  if (["PARTIALLY_SUPPLIED", "SUPPLYING", "IN_SERVICE", "IN_PROGRESS"].includes(status)) return 4;
  if (["SUPPLIED", "COMPLETED"].includes(status)) return 5;
  return 0;
};
const purchaseOrderQueryStageIndex = (row: PurchaseOrderSummary | Record<string, unknown>) => {
  const order = row as PurchaseOrderSummary;
  const statusStage = purchaseOrderStatusStageIndex(order.status);
  const supplierStage = Math.max(0, Math.min(purchaseOrderQueryStageLabels.length - 1, Number(order.supplierStageIndex || 0)));
  return Math.max(statusStage, supplierStage);
};

const purchaseOrderQueryStageState = (row: PurchaseOrderSummary | Record<string, unknown>, index: number) => {
  const activeIndex = purchaseOrderQueryStageIndex(row);
  if (index < activeIndex || (activeIndex === purchaseOrderQueryStageLabels.length - 1 && index === activeIndex)) return "done";
  if (index === activeIndex) return "active";
  return "todo";
};
const purchaseOrderQueryStatusLabel = (row: PurchaseOrderSummary | Record<string, unknown>) =>
  purchaseOrderWorkspaceMode.value === "supplier"
    ? supplierPurchaseStatusLabel(String((row as PurchaseOrderSummary).status || ""))
    : purchaseOrderQueryStageLabels[purchaseOrderQueryStageIndex(row)] || purchaseOrderQueryStageLabels[0];

const purchaseOrderQueryAmount = (row: PurchaseOrderSummary | Record<string, unknown>) => {
  const order = row as PurchaseOrderSummary;
  if (purchaseOrderWorkspaceMode.value === "supplier") {
    return Number(order.supplierFinalAmount ?? order.supplierSubtotalAmount ?? order.totalAmount ?? 0);
  }
  return Number(order.totalAmount || 0)
    + Number(order.fixedFreightFee || 0)
    + Number(order.fixedCustomsFee || 0)
    + Number(order.fixedCraneFee || 0)
    + Number(order.fixedOtherFee || 0);
};

const purchaseOrderQueryDate = (row: PurchaseOrderSummary | Record<string, unknown>) => {
  const order = row as PurchaseOrderSummary;
  return order.vesselEta || order.expectedReadyAt || order.createdAt || "";
};

const purchaseOrderIdFromRoute = computed(() => String(route.params.orderId || "").trim());
const supplierOrderIdFromRoute = computed(() => {
  const value = Number(route.query.supplierOrderId);
  return Number.isFinite(value) && value > 0 ? value : 0;
});
const isSupplierOrdersPage = computed(() => pageKey.value === "supplierOrders");
const isPurchaseOrdersPage = computed(() => pageKey.value === "orders" || isSupplierOrdersPage.value);
const isPurchaseOrderDetailPage = computed(() => isPurchaseOrdersPage.value && Boolean(purchaseOrderIdFromRoute.value));
const purchaseOrderHasFilters = computed(() =>
  Boolean(
    purchaseOrderKeyword.value.trim() ||
      purchaseOrderStatus.value.trim() ||
      purchaseOrderCreatedFrom.value.trim() ||
      purchaseOrderCreatedTo.value.trim()
  )
);
const currentRoles = computed(() => getAuthSession()?.roles ?? []);
const isSupplierOnlyUser = computed(() => currentRoles.value.includes("supplier") && !currentRoles.value.some((role) => role === "admin" || role === "purchaser"));
const isBuyerSupplierOrderPreview = computed(() => isSupplierOrdersPage.value && supplierOrderIdFromRoute.value > 0 && !isSupplierOnlyUser.value);
const purchaseOrderWorkspaceMode = computed<"buyer" | "supplier">(() =>
  route.query.buyerView === "1" ? "buyer" : (isSupplierOrdersPage.value || isSupplierOnlyUser.value ? "supplier" : "buyer")
);
const activePurchaseOrderRows = computed(() =>
  sortNewestFirst(
    purchaseOrderWorkspaceMode.value === "supplier" ? supplierPurchaseOrderRows.value : purchaseOrderRows.value,
    ["createdAt", "updatedAt", "vesselEta"],
    ["purchaseOrderId", "supplierOrderId"]
  )
);
const activePurchaseOrderLoading = computed(() => (purchaseOrderWorkspaceMode.value === "supplier" ? supplierPurchaseOrderLoading.value : purchaseOrderLoading.value));
const activePurchaseOrderErrorKey = computed(() => (purchaseOrderWorkspaceMode.value === "supplier" ? supplierPurchaseOrderErrorKey.value : purchaseOrderErrorKey.value));
const trafficAnchorages = ref<TrafficAnchorage[]>([]);
const trafficServiceRows = ref<TrafficServiceOrder[]>([]);
const trafficServiceLoading = ref(false);
const trafficServiceErrorKey = ref("");
const trafficServiceNoticeKey = ref("");
const trafficServiceKeyword = ref("");
const trafficServiceStatus = ref("");
const trafficServiceSeaArea = ref("");
const trafficServiceDialogOpen = ref(false);
const trafficServiceSaving = ref(false);
const selectedTrafficServiceId = ref<number | null>(null);
const transportMarketplaceTab = ref<"requests" | "shuttles">("requests");
const trafficBoatWorkspaceTab = ref<"hall" | "requests">("hall");
const trafficBoatMyServiceTab = ref<"shuttles" | "orders">("shuttles");
const trafficRequestRows = ref<TrafficServiceRequest[]>([]);
const trafficRequestDetail = ref<TrafficServiceRequestDetail | null>(null);
const trafficRequestLoading = ref(false);
const trafficRequestSaving = ref(false);
const trafficRequestKeyword = ref("");
const trafficRequestStatus = ref("");
const trafficRequestSeaArea = ref("");
const trafficRequestDialogOpen = ref(false);
const trafficQuoteDialogOpen = ref(false);
const trafficQuoteRequestId = ref<number | null>(null);
const trafficShuttleRows = ref<TrafficShuttleService[]>([]);
const trafficShuttleLoading = ref(false);
const trafficShuttleSaving = ref(false);
const trafficShuttleDialogOpen = ref(false);
const editingTrafficShuttleId = ref<number | null>(null);
const trafficShuttleKeyword = ref("");
const trafficShuttleStatus = ref("");
const trafficShuttleSeaArea = ref("");
const trafficShuttleAnchorageCode = ref("");
const trafficShuttleServiceDate = ref("");
const trafficShuttleStartTimeFrom = ref("");
const trafficShuttleStartTimeTo = ref("");
const activeTrafficShuttleNodeKey = ref("");
const activeTrafficShuttleComparePickerKey = ref("");
const trafficShuttleInquiryLoading = ref(false);
const trafficShuttleInquiryOptions = ref<
  Array<{
    demandId: number;
    inquiryNo: string;
    status: string;
    vesselName: string;
    vesselImo: string;
    anchorageTime: string;
    anchoragePosition: string;
    serviceContent: string;
    cargoWeight: string;
    cargoVolume: string;
    palletCount: string;
  }>
>([]);
type TrafficShuttleVesselSlot = {
  compareNo: string;
  vesselName: string;
  vesselImo: string;
  anchorageTime: string;
  anchoragePosition: string;
  anchorageLongitude: string;
  anchorageLatitude: string;
  cargoWeight: string;
  cargoVolume: string;
  palletCount: string;
};
const emptyTrafficShuttleVesselSlot = (): TrafficShuttleVesselSlot => ({
  compareNo: "",
  vesselName: "",
  vesselImo: "",
  anchorageTime: "",
  anchoragePosition: "",
  anchorageLongitude: "",
  anchorageLatitude: "",
  cargoWeight: "",
  cargoVolume: "",
  palletCount: ""
});
const trafficShuttleNodeDrafts = ref<
  Record<
    string,
    {
      activeVesselSlot: 1 | 2;
      vesselSlotOne: TrafficShuttleVesselSlot;
      vesselSlotTwo: TrafficShuttleVesselSlot;
      compareNo: string;
      vesselName: string;
      vesselImo: string;
      anchorageTime: string;
      anchoragePosition: string;
      anchorageLongitude: string;
      anchorageLatitude: string;
      cargoWeight: string;
      cargoVolume: string;
      palletCount: string;
      allowShare: boolean;
      customsService: boolean;
      craneService: boolean;
      craneCount: number;
    }
  >
>({});
const trafficShuttleBookingSavingKey = ref("");
const trafficShuttleNodeReservations = ref<Record<string, {
  share: boolean;
  customs: boolean;
  crane: boolean;
  craneCount: number;
  compareNo: string;
  vesselName: string;
  vesselImo: string;
  anchorageTime: string;
  anchoragePosition: string;
  anchorageLongitude: string;
  anchorageLatitude: string;
  cargoWeight: string;
  cargoVolume: string;
  palletCount: string;
  freightFee: number;
  customsFee: number;
  craneFee: number;
}>>({});
const trafficRequestForm = ref<TrafficServiceRequestPayload>({
  seaArea: "NORTH",
  anchorageCode: "",
  useTime: "",
  serviceType: "GOODS",
  passengerType: "NORMAL",
  cargoType: "CARGO",
  returnTrip: false,
  allowShare: true,
  remark: "",
  publish: true,
  cargos: []
});
const trafficQuoteForm = ref<TrafficServiceQuotePayload>({
  quoteAmount: undefined,
  currency: "CNY",
  availableStartTime: "",
  availableReturnTime: "",
  trafficVesselId: undefined,
  trafficVesselName: "",
  contactName: "",
  contactPhone: "",
  message: ""
});
const defaultTrafficShuttleNodes = (): TrafficShuttleNode[] => [
  { nodeName: "节点一", startTime: "09:00", endTime: "11:00" },
  { nodeName: "节点二", startTime: "12:00", endTime: "15:00" },
  { nodeName: "节点三", startTime: "16:00", endTime: "18:00" }
];
const defaultTrafficShuttleDeparturePoint = (seaArea?: string | null) =>
  String(seaArea || "").toUpperCase() === "SOUTH" ? "小干岛" : "西码头";
const trafficShuttleForm = ref<TrafficShuttlePayload>({
  seaArea: "NORTH",
  anchorageCode: "",
  departurePoint: defaultTrafficShuttleDeparturePoint("NORTH"),
  destinationPoint: "",
  startTime: "",
  returnTime: "",
  basePrice: undefined,
  sharedPrice: undefined,
  customsPrice: undefined,
  cranePrice: undefined,
  passengerCapacity: undefined,
  cargoCapacityKg: undefined,
  cargoCapacityCbm: undefined,
  trafficVesselId: undefined,
  trafficVesselName: "",
  status: "DRAFT",
  remark: "",
  serviceNodes: defaultTrafficShuttleNodes()
});
const trafficServiceForm = ref<TrafficServiceOrderPayload>({
  seaArea: "NORTH",
  anchorageCode: "",
  feeType: "FREIGHT",
  useTime: "",
  serviceType: "PERSONNEL",
  passengerType: "NORMAL",
  passengerCount: undefined,
  cargoType: "CARGO",
  returnTrip: false,
  allowShare: false,
  basePrice: undefined,
  sharedPrice: undefined,
  remark: "",
  businessContactId: undefined,
  businessContactName: "",
  businessContactPhone: "",
  acceptedAt: "",
  trafficVesselId: undefined,
  trafficVesselName: "",
  handlerContactId: undefined,
  handlerName: "",
  handlerPhone: "",
  supplierMessage: "",
  departureTime: "",
  arrivalTime: "",
  returnStartTime: "",
  returnEndTime: "",
  signPhotoUrl: "",
  pickupPhotoUrl: "",
  returnArrivalPhotoUrl: "",
  cargos: []
});
const trafficBoatRows = ref<TrafficBoatPrice[]>([]);
const trafficBoatLoading = ref(false);
const trafficBoatErrorKey = ref("");
const trafficBoatNoticeKey = ref("");
const trafficBoatKeyword = ref("");
const trafficBoatSeaArea = ref("");
const trafficBoatSavingCode = ref("");
const trafficBoatSavingAll = ref(false);
const trafficRouteRows = ref<TrafficRoutePlan[]>([]);
const trafficRouteDetail = ref<TrafficRouteDetail | null>(null);
const trafficRouteLoading = ref(false);
const trafficRouteSaving = ref(false);
const trafficRouteErrorKey = ref("");
const trafficRouteNoticeKey = ref("");
const trafficRouteKeyword = ref("");
const trafficRouteStatus = ref("");
const trafficRouteSeaArea = ref("");
const trafficRouteServiceDate = ref("");
const trafficRouteDialogOpen = ref(false);
const trafficRouteForm = ref<TrafficRoutePlanPayload>({
  routeName: "",
  serviceDate: new Date().toISOString().slice(0, 10),
  seaArea: "",
  supplierCompanyName: "",
  trafficVesselName: "",
  plannedDepartureTime: "",
  plannedFinishTime: "",
  allowShare: true,
  estimatedCost: undefined,
  remark: ""
});
const trafficServiceVisualRows = computed(() => sortNewestFirst(trafficServiceRows.value, ["createdAt", "useTime", "departureTime"], ["serviceOrderId"]));
const trafficServiceTableRows = computed(() => trafficServiceVisualRows.value as unknown as Record<string, unknown>[]);
const trafficRequestTableRows = computed(() => trafficRequestRows.value as unknown as Record<string, unknown>[]);
const trafficQuoteTableRows = computed(() => (trafficRequestDetail.value?.quotes || []) as unknown as Record<string, unknown>[]);
const trafficShuttleVisualRows = computed(() => sortNewestFirst(trafficShuttleRows.value, ["createdAt", "startTime"], ["shuttleId"]));
const trafficShuttleTableRows = computed(() => trafficShuttleVisualRows.value as unknown as Record<string, unknown>[]);
const trafficBoatTableRows = computed(() => trafficBoatRows.value as unknown as Record<string, unknown>[]);
const trafficRouteTableRows = computed(() => trafficRouteRows.value as unknown as Record<string, unknown>[]);
const trafficRouteStopTableRows = computed(() => (trafficRouteDetail.value?.stops || []) as unknown as Record<string, unknown>[]);
const trafficRouteCandidateRows = computed(() =>
  trafficServiceRows.value.filter((row) => !["DISCARDED", "COMPLETED"].includes((row.status || "").toUpperCase())) as unknown as Record<string, unknown>[]
);
const selectedTrafficRoute = computed(() => trafficRouteDetail.value?.route || trafficRouteRows.value[0] || null);
const selectedTrafficService = computed(() => trafficServiceRows.value.find((row) => row.serviceOrderId === selectedTrafficServiceId.value) ?? null);
const selectedTrafficRequest = computed(() => trafficRequestDetail.value?.request || null);
const trafficServiceDetailId = computed(() => {
  const value = Number(route.params.serviceOrderId || route.query.serviceOrderId);
  return Number.isFinite(value) && value > 0 ? value : 0;
});
const isTrafficServiceDetailPage = computed(() => pageKey.value === "trafficBoat" && trafficServiceDetailId.value > 0);
const trafficServiceDetailEditable = computed(() => String(selectedTrafficService.value?.status || "").toUpperCase() === "PENDING_CONFIRM");
const trafficServiceDetailPrice = computed(() => {
  const row = selectedTrafficService.value;
  if (!row) return 0;
  return Number(row.allowShare ? (row.sharedPrice ?? row.basePrice ?? 0) : (row.basePrice ?? row.sharedPrice ?? 0));
});
const filteredTrafficAnchorages = computed(() => {
  const area = trafficServiceForm.value.seaArea || trafficServiceSeaArea.value;
  return trafficAnchorages.value.filter((item) => !area || item.seaArea === area);
});
const selectTrafficBusinessContact = (contactId: string) => {
  const contact = activeCompanyContacts.value.find((item) => String(item.contactId || item.id) === contactId);
  trafficServiceForm.value.businessContactId = contact?.contactId ? Number(contact.contactId) : undefined;
  trafficServiceForm.value.businessContactName = contact?.contactName || "";
  trafficServiceForm.value.businessContactPhone = contact?.contactPhone || "";
};
const selectTrafficHandlerContact = (contactId: string) => {
  const contact = activeCompanyContacts.value.find((item) => String(item.contactId || item.id) === contactId);
  trafficServiceForm.value.handlerContactId = contact?.contactId ? Number(contact.contactId) : undefined;
  trafficServiceForm.value.handlerName = contact?.contactName || "";
  trafficServiceForm.value.handlerPhone = contact?.contactPhone || "";
};
const selectTrafficVessel = (vesselId: string) => {
  const vessel = activeCompanyVessels.value.find((item) => String(item.vesselId || item.id) === vesselId);
  trafficServiceForm.value.trafficVesselId = vessel?.vesselId ? Number(vessel.vesselId) : undefined;
  trafficServiceForm.value.trafficVesselName = vessel?.vesselName || "";
};
const selectTrafficShuttleVessel = (vesselId: string) => {
  const vessel = activeCompanyVessels.value.find((item) => String(item.vesselId || item.id) === vesselId);
  trafficShuttleForm.value.trafficVesselId = vessel?.vesselId ? Number(vessel.vesselId) : undefined;
  trafficShuttleForm.value.trafficVesselName = vessel?.vesselName || "";
};
const updateTrafficShuttleSeaArea = () => {
  trafficShuttleForm.value.anchorageCode = "";
  trafficShuttleForm.value.departurePoint = defaultTrafficShuttleDeparturePoint(trafficShuttleForm.value.seaArea);
};
const compareTrafficAnchorages = computed(() => {
  return trafficAnchorages.value;
});
const compareTrafficShuttleVisualRows = computed(() => compareTrafficShuttleRows.value);
const trafficRequestAnchorages = computed(() => {
  const area = trafficRequestForm.value.seaArea || "";
  return trafficAnchorages.value.filter((item) => !area || item.seaArea === area);
});
const trafficShuttleAnchorages = computed(() => {
  const area = trafficShuttleForm.value.seaArea || "";
  return trafficAnchorages.value.filter((item) => !area || item.seaArea === area);
});
const trafficShuttleFilterAnchorages = computed(() => {
  const area = trafficShuttleSeaArea.value || "";
  return trafficAnchorages.value.filter((item) => !area || item.seaArea === area);
});
const trafficServiceColumns = computed<TableColumn[]>(() => [
  { key: "serviceNo", label: t("trafficService.field.serviceNo"), width: "150px" },
  { key: "feeType", label: t("trafficService.field.feeType"), width: "96px", align: "center" },
  { key: "seaArea", label: t("trafficService.field.seaArea"), width: "110px" },
  { key: "anchorageName", label: t("trafficService.field.anchorage"), width: "220px" },
  { key: "useTime", label: t("trafficService.field.useTime"), width: "150px" },
  { key: "serviceType", label: t("trafficService.field.serviceType"), width: "110px" },
  { key: "basePrice", label: t("trafficService.field.basePrice"), width: "120px", align: "right" },
  { key: "sharedPrice", label: t("trafficService.field.sharedPrice"), width: "120px", align: "right" },
  { key: "status", label: t("field.status"), width: "100px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "132px", align: "center" }
]);
const trafficRequestColumns = computed<TableColumn[]>(() => [
  { key: "requestNo", label: t("trafficMarketplace.field.requestNo"), width: "160px" },
  { key: "seaArea", label: t("trafficService.field.seaArea"), width: "110px" },
  { key: "anchorageName", label: t("trafficService.field.anchorage"), width: "180px" },
  { key: "useTime", label: t("trafficService.field.useTime"), width: "150px" },
  { key: "serviceType", label: t("trafficService.field.serviceType"), width: "110px" },
  { key: "allowShare", label: t("trafficService.field.allowShare"), width: "86px", align: "center" },
  { key: "status", label: t("field.status"), width: "110px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "156px", align: "center" }
]);
const trafficQuoteColumns = computed<TableColumn[]>(() => [
  { key: "supplierCompanyName", label: t("trafficService.field.supplier"), width: "180px" },
  { key: "quoteAmount", label: t("trafficMarketplace.field.quoteAmount"), width: "120px", align: "right" },
  { key: "availableStartTime", label: t("trafficMarketplace.field.availableStartTime"), width: "150px" },
  { key: "trafficVesselName", label: t("trafficService.field.trafficVessel"), width: "130px" },
  { key: "contactName", label: t("trafficMarketplace.field.contact"), width: "120px" },
  { key: "status", label: t("field.status"), width: "110px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "120px", align: "center" }
]);
const trafficShuttleColumns = computed<TableColumn[]>(() => [
  { key: "shuttleNo", label: t("trafficMarketplace.field.shuttleNo"), width: "150px" },
  { key: "supplierCompanyName", label: t("trafficService.field.supplier"), width: "160px" },
  { key: "anchorageName", label: t("trafficService.field.anchorage"), width: "160px" },
  { key: "route", label: t("trafficMarketplace.field.route"), width: "180px" },
  { key: "trafficVesselName", label: t("trafficService.field.trafficVessel"), width: "130px" },
  { key: "startTime", label: t("trafficMarketplace.field.startTime"), width: "150px" },
  { key: "returnTime", label: t("trafficMarketplace.field.returnTime"), width: "150px" },
  { key: "sharedPrice", label: "驳船价格", width: "120px", align: "right" },
  { key: "status", label: t("field.status"), width: "100px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "132px", align: "center" }
]);
const trafficShuttleManagementColumns = computed<TableColumn[]>(() =>
  trafficShuttleColumns.value.filter((column) => !["supplierCompanyName", "anchorageName"].includes(String(column.key)))
);
const trafficBoatColumns = computed<TableColumn[]>(() => [
  { key: "seaArea", label: t("trafficService.field.seaArea"), width: "110px" },
  { key: "anchorageName", label: t("trafficService.field.anchorage"), width: "240px" },
  { key: "basePrice", label: t("trafficService.field.basePrice"), width: "140px", align: "right" },
  { key: "sharedPrice", label: t("trafficService.field.sharedPrice"), width: "140px", align: "right" },
  { key: "enabled", label: t("field.status"), width: "96px", align: "center" },
  { key: "remark", label: t("field.remark") },
  { key: "operation", label: t("common.operation"), width: "96px", align: "center" }
]);
const trafficBoatEnterpriseColumns = computed<TableColumn[]>(() => trafficBoatColumns.value.filter((column) => column.key !== "operation"));
const trafficRouteColumns = computed<TableColumn[]>(() => [
  { key: "routeNo", label: t("trafficRoute.field.routeNo"), width: "150px" },
  { key: "routeName", label: t("trafficRoute.field.routeName"), width: "180px" },
  { key: "serviceDate", label: t("trafficRoute.field.serviceDate"), width: "120px" },
  { key: "seaArea", label: t("trafficService.field.seaArea"), width: "110px" },
  { key: "supplierCompanyName", label: t("trafficRoute.field.supplier"), width: "160px" },
  { key: "orderCount", label: t("trafficRoute.field.orderCount"), width: "92px", align: "center" },
  { key: "totalIncome", label: t("trafficRoute.field.totalIncome"), width: "120px", align: "right" },
  { key: "estimatedProfit", label: t("trafficRoute.field.estimatedProfit"), width: "120px", align: "right" },
  { key: "status", label: t("field.status"), width: "100px", align: "center" }
]);
const trafficRouteStopColumns = computed<TableColumn[]>(() => [
  { key: "stopSequence", label: t("trafficRoute.field.stop"), width: "80px", align: "center" },
  { key: "serviceNo", label: t("trafficService.field.serviceNo"), width: "150px" },
  { key: "anchorageName", label: t("trafficService.field.anchorage"), width: "200px" },
  { key: "plannedServiceTime", label: t("trafficRoute.field.plannedTime"), width: "150px" },
  { key: "contactName", label: t("trafficRoute.field.contact"), width: "130px" },
  { key: "amount", label: t("trafficRoute.field.amount"), width: "120px", align: "right" },
  { key: "status", label: t("field.status"), width: "100px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "88px", align: "center" }
]);
const trafficRouteCandidateColumns = computed<TableColumn[]>(() => [
  { key: "serviceNo", label: t("trafficService.field.serviceNo"), width: "150px" },
  { key: "anchorageName", label: t("trafficService.field.anchorage"), width: "180px" },
  { key: "useTime", label: t("trafficService.field.useTime"), width: "150px" },
  { key: "supplierCompanyName", label: t("trafficRoute.field.supplier"), width: "150px" },
  { key: "basePrice", label: t("trafficRoute.field.amount"), width: "120px", align: "right" },
  { key: "operation", label: t("common.operation"), width: "96px", align: "center" }
]);

const getTrafficBoatDefaultPrice = (code: string, salt = 0) => {
  const text = code || "ANCHORAGE";
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) {
    hash = (hash * 31 + text.charCodeAt(index) + salt) % 3000;
  }
  return 1000 + Math.abs(hash % 3000);
};

const normalizeTrafficBoatRow = (row: TrafficBoatPrice): TrafficBoatPrice => ({
  ...row,
  basePrice:
    Number.isFinite(Number(row.basePrice)) && Number(row.basePrice) >= 1000 && Number(row.basePrice) <= 3999
      ? Number(row.basePrice)
      : getTrafficBoatDefaultPrice(row.anchorageCode),
  sharedPrice:
    Number.isFinite(Number(row.sharedPrice)) && Number(row.sharedPrice) >= 1000 && Number(row.sharedPrice) <= 3999
      ? Number(row.sharedPrice)
      : getTrafficBoatDefaultPrice(row.anchorageCode, 137),
  enabled: true
});
const supplierDetailSelectedOrder = computed(() => {
  const rows = purchaseOrderDetail.value?.supplierOrders ?? [];
  if (!supplierOrderIdFromRoute.value) return rows[0] ?? null;
  return rows.find((row) => Number(row.supplierOrderId) === supplierOrderIdFromRoute.value) ?? rows[0] ?? null;
});
const supplierDetailPendingOrder = computed(() => {
  const row = supplierDetailSelectedOrder.value;
  return row && String(row.status || "").toUpperCase() === "PENDING_SUPPLIER_CONFIRM" ? row : null;
});
const supplierDetailPreparingOrder = computed(() => {
  const row = supplierDetailSelectedOrder.value;
  return row && String(row.status || "").toUpperCase() === "PREPARING" ? row : null;
});
const supplierDetailShippingOrder = computed(() => {
  const row = supplierDetailSelectedOrder.value;
  if (!row) return null;
  const status = String(row.status || "").toUpperCase();
  return purchaseSupplierShipmentActionStatuses.has(status) ? row : null;
});
const supplierDetailTransportOrder = computed(() => {
  const row = supplierDetailSelectedOrder.value;
  if (!row) return null;
  const status = String(row.status || "").toUpperCase();
  return purchaseSupplierTransportActionStatuses.has(status) ? row : null;
});
const supplierDetailCurrentOrder = computed(() => supplierDetailSelectedOrder.value);
const purchaseOrderDetailStatus = computed(() =>
  purchaseOrderWorkspaceMode.value === "supplier"
    ? String(supplierDetailCurrentOrder.value?.status || purchaseOrderDetail.value?.order.status || "")
    : String(purchaseOrderDetail.value?.order.status || "")
);
type SelectedPurchaseItem = NonNullable<PurchaseOrderCreatePayload["selectedItems"]>[number] & {
  rowId: string;
  supplierKey: string;
  supplierName: string;
  currency: string;
  amount: number;
  amountUsd?: number;
  costAmount: number;
  costAmountUsd?: number;
};

const buildSelectedPurchaseItem = (row: CompareSkuRow): SelectedPurchaseItem | null => {
  const skuId = Number(row.candidate.skuId);
  const unitPrice = Number(row.price);
  const pricingQuantity = Number(row.pricingQuantity);
  if (!selectedCompareRowIds.value.includes(row.id)) return null;
  if (!Number.isFinite(skuId) || skuId <= 0) return null;
  if (!Number.isFinite(unitPrice) || unitPrice <= 0) return null;
  if (!Number.isFinite(pricingQuantity) || pricingQuantity <= 0) return null;
  const costAmount = unitPrice * pricingQuantity;
  const costAmountUsd = row.unitPriceUsd != null && Number.isFinite(row.unitPriceUsd) ? row.unitPriceUsd * pricingQuantity : undefined;
  const amount = Number.isFinite(row.subtotal) ? row.subtotal : costAmount;
  const selectedUnit = normalizeCompareText(row.selectedUnit) || normalizeCompareText(row.platformUnit) || normalizeCompareText(row.unit);
  return {
    rowId: row.id,
    supplierKey: row.supplierKey,
    demandItemId: Number(row.demandItemId),
    skuId,
    supplierCompanyId: row.candidate.companyId,
    supplierName: row.supplierName,
    supplierSkuCode: row.candidate.supplierSkuCode,
    platformCode: row.candidate.platformCode,
    impaCode: row.candidate.impaCode,
    productName: row.candidate.productName || row.name,
    specification: normalizeCompareText(row.candidate.attributeSummary) || row.attributes[0]?.value,
    quantity: row.quantity,
    unit: selectedUnit || row.unit,
    selectedUnit,
    pricingQuantity,
    unitPrice,
    unitPriceUsd: row.unitPriceUsd,
    actualQuotePrice: row.actualQuotePrice,
    actualQuoteAmount: amount,
    quoteMarkupPercent: row.quoteMarkupPercent,
    currency: row.candidate.currency || row.currency || "CNY",
    amount,
    amountUsd: row.subtotalUsd,
    costAmount,
    costAmountUsd,
    unitMismatchFlag: row.unitMismatchFlag,
    quantityFallbackFlag: row.quantityFallbackFlag
  };
};

const selectedComparePurchaseItems = computed<SelectedPurchaseItem[]>(() =>
  compareSkuRows.value.map((row) => buildSelectedPurchaseItem(row)).filter((item): item is SelectedPurchaseItem => Boolean(item))
);
const activePurchaseOrderItems = computed(() =>
  purchaseOrderDialogOpen.value ? purchaseOrderSelectedItemsSnapshot.value : selectedComparePurchaseItems.value
);
const selectedCompareOrderableRows = computed(() =>
  compareSkuRows.value.filter((row) => selectedComparePurchaseItems.value.some((item) => item.rowId === row.id))
);
const selectedCompareExcludedCount = computed(() => Math.max((compareData.value?.items.length ?? 0) - activePurchaseOrderItems.value.length, 0));
const selectedCompareTotalSkuCount = computed(() => compareData.value?.items.length ?? 0);
const selectedCompareSupplierCount = computed(() => new Set(activePurchaseOrderItems.value.map((item) => item.supplierKey)).size);
const selectedCompareOrderAmount = computed(() => activePurchaseOrderItems.value.reduce((sum, item) => sum + (Number.isFinite(item.amount) ? item.amount : 0), 0));
const selectedCompareOrderAmountUsd = computed(() => activePurchaseOrderItems.value.reduce((sum, item) => sum + (item.amountUsd != null && Number.isFinite(item.amountUsd) ? item.amountUsd : 0), 0));
const selectedCompareOrderGrandAmount = computed(() => selectedCompareOrderAmount.value + compareFixedFeeTotal.value);
const selectedCompareOrderGrandAmountUsd = computed(() => selectedCompareOrderAmountUsd.value + compareFixedFeeTotalUsd.value);
const selectedCompareCostAmount = computed(() => activePurchaseOrderItems.value.reduce((sum, item) => sum + (Number.isFinite(item.costAmount) ? item.costAmount : 0), 0));
const selectedCompareCostAmountUsd = computed(() => activePurchaseOrderItems.value.reduce((sum, item) => sum + (item.costAmountUsd != null && Number.isFinite(item.costAmountUsd) ? item.costAmountUsd : 0), 0));
const selectedCompareProfitAmount = computed(() => selectedCompareOrderAmount.value - selectedCompareCostAmount.value - compareFixedFeeTotal.value);
const selectedCompareProfitAmountUsd = computed(() => selectedCompareOrderAmountUsd.value - selectedCompareCostAmountUsd.value - compareFixedFeeTotalUsd.value);
const selectedCompareSupplierSummaries = computed(() => {
  const map = new Map<string, { supplier: string; count: number; amount: number; amountUsd: number; currency: string }>();
  activePurchaseOrderItems.value.forEach((item) => {
    const key = item.supplierKey || item.supplierName;
    const current = map.get(key) ?? { supplier: item.supplierName || "-", count: 0, amount: 0, amountUsd: 0, currency: item.currency || "CNY" };
    current.count += 1;
    current.amount += Number.isFinite(item.amount) ? item.amount : 0;
    current.amountUsd += item.amountUsd != null && Number.isFinite(item.amountUsd) ? item.amountUsd : 0;
    map.set(key, current);
  });
  return [...map.values()];
});
const shouldAwardAllPurchaseSuppliers = computed(() =>
  activeCompareStrategy.value?.key === "LOWEST_MIXED" || selectedCompareSupplierCount.value > 1
);
const purchaseSupplierAwardLabel = (index: number) => (shouldAwardAllPurchaseSuppliers.value || index === 0 ? "中标" : "入选");
const isPurchaseSupplierWinner = (index: number) => shouldAwardAllPurchaseSuppliers.value || index === 0;
const selectedCompareOrderNotices = computed(() => {
  const notices: string[] = [];
  const quantityFallbackCount = activePurchaseOrderItems.value.filter((item) => item.quantityFallbackFlag).length;
  if (quantityFallbackCount) notices.push(t("purchaseOrder.dialog.pricingQuantityFallback", { count: quantityFallbackCount }));
  return notices;
});

const compareOrderedStatuses = new Set(["ORDERED", "SUPPLIED", "PARTIALLY_SUPPLIED", "COMPLETED"]);
const compareDemandStatus = computed(() => normalizeCompareText(compareData.value?.demand?.status).toUpperCase());
const isCompareOrderedState = computed(() => Boolean(compareData.value?.isOrdered || compareData.value?.existingPurchaseOrderId || compareOrderedStatuses.has(compareDemandStatus.value)));
const isCompareReadonly = computed(() => Boolean(compareData.value?.isDiscarded || isCompareOrderedState.value));
const compareStatusLabel = computed(() => {
  if (compareData.value?.isDiscarded) return t("page.materialDemand.statusDiscarded");
  if (isCompareOrderedState.value) return t("page.materialDemand.statusOrdered");
  return t("compare.statusPending");
});
const compareStatusVariant = computed<StatusVariant>(() => {
  if (compareData.value?.isDiscarded) return "danger";
  if (isCompareOrderedState.value) return "success";
  return "warning";
});

const compareAllVisibleSelected = computed(() => {
  const ids = compareFilteredSkus.value.filter(isCompareRowOrderable).map((row) => row.id);
  return ids.length > 0 && ids.every((id) => selectedCompareRowIds.value.includes(id));
});

const syncCompareSelectedRows = () => {
  const orderableIds = compareSkuRows.value.filter(isCompareRowOrderable).map((row) => row.id);
  selectedCompareRowIds.value = orderableIds;
};

const toggleCompareRowSelection = (row: CompareSkuRow) => {
  if (isCompareReadonly.value || !isCompareRowOrderable(row)) return;
  selectedCompareRowIds.value = selectedCompareRowIds.value.includes(row.id)
    ? selectedCompareRowIds.value.filter((id) => id !== row.id)
    : [...selectedCompareRowIds.value, row.id];
};

const toggleAllVisibleCompareRows = () => {
  if (isCompareReadonly.value) return;
  const ids = compareFilteredSkus.value.filter(isCompareRowOrderable).map((row) => row.id);
  if (!ids.length) return;
  selectedCompareRowIds.value = compareAllVisibleSelected.value
    ? selectedCompareRowIds.value.filter((id) => !ids.includes(id))
    : Array.from(new Set([...selectedCompareRowIds.value, ...ids]));
};

const updateCompareQuantity = (row: CompareSkuRow, value: string) => {
  compareQuantityInputs.value = {
    ...compareQuantityInputs.value,
    [row.demandItemId]: value
  };
  if (!parseCompareQuantity(value)) {
    selectedCompareRowIds.value = selectedCompareRowIds.value.filter((id) => id !== row.id);
  }
};

const updateCompareRemark = (row: CompareSkuRow, value: string) => {
  if (isCompareReadonly.value || !row.demandItemId) return;
  compareRemarkInputs.value = {
    ...compareRemarkInputs.value,
    [row.demandItemId]: value
  };
};

const updateCompareUnit = (row: CompareSkuRow, value: string) => {
  if (isCompareReadonly.value || !row.demandItemId) return;
  compareUnitSelections.value = {
    ...compareUnitSelections.value,
    [row.demandItemId]: value
  };
  expandedCompareRowId.value = "";
  window.setTimeout(syncCompareSelectedRows, 0);
};

const updateCompareActualQuote = (row: CompareSkuRow, value: string) => {
  if (isCompareReadonly.value || !row.demandItemId) return;
  const cnyValue = displayCurrencyAmountToCny(value);
  compareQuoteActualPrices.value = {
    ...compareQuoteActualPrices.value,
    [row.demandItemId]: cnyValue == null ? value : String(cnyValue)
  };
};

const updateCompareGlobalMarkup = (value: string) => {
  const markup = parseCompareNonNegative(value);
  compareQuoteMarkupPercent.value = markup ?? 0;
  compareQuoteMarkupInputs.value = {};
  compareQuoteActualPrices.value = {};
};

const updateCompareMarkup = (row: CompareSkuRow, value: string) => {
  if (isCompareReadonly.value || !row.demandItemId) return;
  compareQuoteMarkupInputs.value = {
    ...compareQuoteMarkupInputs.value,
    [row.demandItemId]: value
  };
  const { [row.demandItemId]: _removed, ...rest } = compareQuoteActualPrices.value;
  compareQuoteActualPrices.value = rest;
};

const quotePayloadItems = () =>
  compareSkuRows.value
    .map((row) => {
      const selected = selectedCompareRowIds.value.includes(row.id) && isCompareRowOrderable(row) && row.actualQuotePrice != null && Number.isFinite(row.actualQuotePrice) && row.actualQuotePrice > 0;
      return {
      demandItemId: row.demandItemId,
      skuId: selected ? row.candidate.skuId : undefined,
      selectedUnit: selected ? row.selectedUnit : undefined,
      quantity: row.quantity,
      remarks: row.remarks ?? "",
      unitPrice: selected ? row.price : undefined,
      unitPriceUsd: selected ? row.unitPriceUsd : undefined,
      actualQuotePrice: selected ? ceilMoneyToCents(row.actualQuotePrice) : undefined,
      quoteMarkupPercent: selected ? row.quoteMarkupPercent : undefined,
      currency: selected ? (compareSupplyForm.value.currency.trim() || compareData.value?.demand?.currency || row.currency || "CNY") : undefined
      };
    });

const splitCompareHandlerContact = () => {
  const text = compareSupplyForm.value.handlerContact.trim();
  if (!text) return { handlerName: "", handlerEmail: "" };
  const parts = text.split(/[\/,，;；\s]+/).map((part) => part.trim()).filter(Boolean);
  const emailIndex = parts.findIndex((part) => part.includes("@"));
  return {
    handlerName: parts.filter((_, index) => index !== emailIndex).join(" "),
    handlerEmail: emailIndex >= 0 ? parts[emailIndex] : ""
  };
};

const mergeCompareEditedItems = (items: MaterialDemandDetail["items"]) =>
  items.map((item, index) => {
    const itemRecord = item as MaterialDemandDetail["items"][number] & { itemId?: number };
    const key = String(itemRecord.itemId ?? item.sourceRowNumber ?? item.sequence ?? index);
    const quantity = compareQuantityInputs.value[key];
    return {
      ...item,
      quantity: quantity == null ? item.quantity : quantity
    };
  });

const saveCompareMainInfo = async () => {
  if (!compareDemandId.value || !compareData.value?.demand) return;
  const detail = await getMaterialDemandDetail(compareDemandId.value);
  const handler = splitCompareHandlerContact();
  const compareSupplyPort = displaySupplyPort(compareSupplyForm.value.port.trim() || detail.demand.supplyPortName || detail.demand.supplyPortCode);
  await saveMaterialDemand({
    demandId: detail.demand.demandId,
    demandNo: detail.demand.demandNo,
    applicationNo: detail.demand.applicationNo,
    inquiryNo: compareSupplyForm.value.inquiryNo.trim() || detail.demand.inquiryNo,
    materialType: compareSupplyForm.value.materialType.trim() || detail.demand.materialType,
    currency: compareSupplyForm.value.currency.trim() || detail.demand.currency,
    recipientCompany: compareSupplyForm.value.recipientCompany.trim() || detail.demand.recipientCompany,
    handlerName: handler.handlerName || detail.demand.handlerName,
    handlerEmail: handler.handlerEmail || detail.demand.handlerEmail,
    vesselName: compareSupplyForm.value.vessel.trim() || detail.demand.vesselName,
    supplyPortCode: detail.demand.supplyPortCode,
    supplyPortName: compareSupplyPort || detail.demand.supplyPortName,
    vesselEta: compareSupplyForm.value.date.trim() || detail.demand.vesselEta,
    inquiryDate: detail.demand.inquiryDate,
    sourceFileName: detail.demand.sourceFileName,
    sourceFileId: detail.demand.sourceFileId,
    documentType: detail.demand.documentType,
    headerRowIndex: detail.demand.headerRowIndex,
    trafficService: normalizeCompareTrafficServicePayload(),
    items: mergeCompareEditedItems(detail.items)
  });
};

const saveCompareQuotePrices = async (includeTrafficService = true, reloadAfterSave = false) => {
  if (!compareDemandId.value || isCompareReadonly.value) return false;
  compareQuoteError.value = "";
  compareQuoteNotice.value = "";
  const items = quotePayloadItems();
  if (!items.length) {
    compareQuoteError.value = t("compare.noQuoteItems");
    return false;
  }
  compareQuoteSaving.value = true;
  try {
    const response = await saveMaterialComparisonQuotes(compareDemandId.value, {
      strategyType: activeCompareStrategy.value?.key,
      markupPercent: compareQuoteMarkupPercent.value,
      fixedFreightFee: displayCurrencyAmountToCny(compareFixedFeeInputs.value.freight) ?? 0,
      fixedCustomsFee: displayCurrencyAmountToCny(compareFixedFeeInputs.value.customs) ?? 0,
      fixedCraneFee: displayCurrencyAmountToCny(compareFixedFeeInputs.value.crane) ?? 0,
      fixedOtherFee: displayCurrencyAmountToCny(compareFixedFeeInputs.value.other) ?? 0,
      trafficService: includeTrafficService ? normalizeCompareTrafficServicePayload() : undefined,
      items
    });
    compareQuoteNotice.value = t("compare.quoteSaved", { count: response.savedCount ?? items.filter((item) => item.actualQuotePrice != null).length });
    if (reloadAfterSave) {
      await loadCompareWorkspace();
    }
    return true;
  } catch (error) {
    compareQuoteError.value = error instanceof Error && error.message ? error.message : t("compare.quoteSaveFailed");
    return false;
  } finally {
    compareQuoteSaving.value = false;
  }
};

const exportCompareQuotePrices = async () => {
  if (!compareDemandId.value) return;
  compareQuoteExporting.value = true;
  compareQuoteError.value = "";
  try {
    if (!isCompareReadonly.value && selectedCompareOrderableRows.value.length) {
      const saved = await saveCompareQuotePrices();
      if (!saved) return;
    }
    await exportMaterialQuoteTemplate(compareDemandId.value);
  } catch (error) {
    compareQuoteError.value = error instanceof Error && error.message ? error.message : t("compare.quoteExportFailed");
  } finally {
    compareQuoteExporting.value = false;
  }
};

const openCompareQuoteImport = () => {
  if (compareLoading.value || compareQuoteImporting.value || isCompareReadonly.value) return;
  compareQuoteImportInput.value?.click();
};

const handleCompareQuoteImport = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file || !compareDemandId.value || isCompareReadonly.value) return;
  compareQuoteImporting.value = true;
  compareQuoteError.value = "";
  compareQuoteNotice.value = "";
  try {
    const response = await importMaterialQuoteQuantities(compareDemandId.value, file);
    const quantityByDemandItemId = new Map(
      response.items
        .filter((item) => item.demandItemId != null)
        .map((item) => [String(item.demandItemId), normalizeCompareText(item.quantity)])
    );
    if (!quantityByDemandItemId.size) {
      compareQuoteError.value = "未识别到可回填的明细数量。";
      return;
    }
    const nextQuantities = { ...compareQuantityInputs.value };
    quantityByDemandItemId.forEach((quantity, demandItemId) => {
      nextQuantities[demandItemId] = quantity || "0";
    });
    compareQuantityInputs.value = nextQuantities;

    const importedDemandItemIds = new Set(quantityByDemandItemId.keys());
    const nextSelected = new Set(
      selectedCompareRowIds.value.filter((rowId) => {
        const demandItemId = demandItemIdFromCompareRowId(rowId);
        return !demandItemId || !importedDemandItemIds.has(demandItemId);
      })
    );
    compareSkuRows.value.forEach((row) => {
      if (!importedDemandItemIds.has(String(row.demandItemId))) return;
      const quantity = quantityByDemandItemId.get(String(row.demandItemId));
      if (parseCompareQuantity(quantity) && isCompareRowOrderable(row)) {
        nextSelected.add(row.id);
      }
    });
    selectedCompareRowIds.value = Array.from(nextSelected);
    compareQuoteNotice.value = `已导入 ${quantityByDemandItemId.size} 条明细数量，数量为 0 的明细已取消选择。`;
  } catch (error) {
    compareQuoteError.value = error instanceof Error && error.message ? error.message : "导入失败，请确认文件为导出的 xlsx 文件。";
  } finally {
    compareQuoteImporting.value = false;
  }
};

watch(
  () => compareSkuRows.value.map((row) => row.id).join("|"),
  () => {
    if (!compareSkuRows.value.length) return;
    const current = new Set(compareSkuRows.value.map((row) => row.id));
    selectedCompareRowIds.value = selectedCompareRowIds.value.filter((id) => current.has(id));
    if (!compareSelectionInitialized.value && !selectedCompareRowIds.value.length && !isCompareReadonly.value) {
      syncCompareSelectedRows();
      compareSelectionInitialized.value = true;
    }
  }
);

const purchaseDetailSupplierRows = computed(() => (purchaseOrderDetail.value?.supplierOrders ?? []) as unknown as Record<string, unknown>[]);
const purchaseSupplierPendingStatuses = new Set(["PENDING_SUPPLIER_CONFIRM"]);
const purchaseSupplierReadyStatuses = new Set(["READY_TO_DELIVER", "PARTIALLY_READY", "IN_TRANSIT", "WAITING_SUPPLY"]);
const purchaseSupplierShipmentActionStatuses = new Set(["READY_TO_DELIVER", "PARTIALLY_READY"]);
const purchaseSupplierTransportActionStatuses = new Set(["IN_TRANSIT"]);
const purchaseSupplierTransportStatuses = new Set(["IN_TRANSIT", "WAITING_SUPPLY"]);
const purchaseSupplierWaitingSupplyStatuses = new Set(["WAITING_SUPPLY"]);
const purchaseSupplierCompletedStatuses = new Set(["SUPPLIED", "PARTIALLY_SUPPLIED", "COMPLETED"]);
const purchaseSupplierRejectedStatuses = new Set(["REJECTED", "PARTIALLY_REJECTED", "CANCELED", "DISCARDED"]);
const purchaseExecutionStageDefinitions = [
  { key: "confirming", label: "确认中", note: "供货商接单确认" },
  { key: "stocking", label: "备货中", note: "供应商备货与资料准备" },
  { key: "transporting", label: "运输中", note: "供应商更新货物运输" },
  { key: "waitingSupply", label: "待补给", note: "驳船等待靠泊补给" },
  { key: "supplying", label: "补给中", note: "驳船服务商执行补给" },
  { key: "completed", label: "补给完成", note: "订单履约完成" }
] as const;
type PurchaseExecutionStageKey = (typeof purchaseExecutionStageDefinitions)[number]["key"];
const formatExecutionTimeText = (value?: string | null) => {
  const text = String(value || "").trim();
  if (!text) return "--";
  const parts = formatDateTimeParts(text);
  return [parts.date, parts.time].filter(Boolean).join(" ") || text.replace("T", " ").slice(0, 16);
};
const getPurchaseSupplierExecutionTime = (row: PurchaseSupplierOrder) => {
  const status = String(row.status || "").toUpperCase();
  if (purchaseSupplierCompletedStatuses.has(status)) return formatExecutionTimeText(row.suppliedAt || row.readyAt || row.confirmedAt || row.expectedReadyAt);
  if (purchaseSupplierTransportStatuses.has(status)) return formatExecutionTimeText(row.suppliedAt || row.readyAt || row.confirmedAt || row.expectedReadyAt);
  if (purchaseSupplierReadyStatuses.has(status)) return formatExecutionTimeText(row.readyAt || row.confirmedAt || row.expectedReadyAt);
  if (!purchaseSupplierPendingStatuses.has(status)) return formatExecutionTimeText(row.confirmedAt || row.expectedReadyAt);
  return formatExecutionTimeText(row.expectedReadyAt);
};
const purchaseDetailSupplierCards = computed(() =>
  (purchaseOrderDetail.value?.supplierOrders ?? []).map((row) => {
    const status = String(row.status || "").toUpperCase();
    const isPending = purchaseSupplierPendingStatuses.has(status);
    const isRejected = purchaseSupplierRejectedStatuses.has(status);
    const isDone = purchaseSupplierCompletedStatuses.has(status);
    const isWaitingSupply = purchaseSupplierWaitingSupplyStatuses.has(status);
    const isReady = purchaseSupplierReadyStatuses.has(status);
    return {
      ...row,
      amount: Number(row.finalAmount ?? row.subtotalAmount ?? 0),
      cardTone: isRejected ? "danger" : isPending ? "pending" : "active",
      flowStatusText: isRejected ? purchaseStatusLabel(status) : isPending ? "待确认" : isDone ? "补给完成" : isWaitingSupply ? "待补给" : isReady ? "运输中" : "备货中",
      flowStatusVariant: (isRejected ? "danger" : isPending ? "info" : "success") as StatusVariant,
      executionTime: getPurchaseSupplierExecutionTime(row),
      attachmentCount: fulfillmentAttachments.value.filter((item) => item.providerType === "SUPPLIER" && item.supplierOrderId === row.supplierOrderId).length,
      supplyRole: purchaseTrafficServiceRows.value.length ? "负责备货" : "承担补给管理"
    };
  })
);
const purchaseDetailItemRows = computed(() => {
  if (purchaseOrderWorkspaceMode.value === "supplier" && supplierDetailCurrentOrder.value) {
    return (supplierDetailCurrentOrder.value.items ?? []) as unknown as Record<string, unknown>[];
  }
  return (purchaseOrderDetail.value?.supplierOrders ?? []).flatMap((supplier) => supplier.items) as unknown as Record<string, unknown>[];
});
const supplierDetailDialogItems = computed(() => {
  const row = supplierDetailDialogRow.value as PurchaseSupplierOrder | null;
  return (row?.items ?? []) as unknown as Record<string, unknown>[];
});
const purchaseDetailEvents = computed(() => purchaseOrderDetail.value?.events ?? []);
const purchaseTrafficServiceTableRows = computed(() => purchaseTrafficServiceRows.value as unknown as Record<string, unknown>[]);
const purchaseDetailPrimaryTrafficService = computed(() => purchaseTrafficServiceRows.value.find((row) => Number(row.bookingId || 0) > 0) ?? null);
const purchaseDetailOrderSupplyMode = computed(() =>
  String(purchaseOrderDetail.value?.order.supplyMode || purchaseOrderSourceTrafficService.value?.supplyMode || "").toUpperCase()
);
const purchaseDetailOrderProviderType = computed(() =>
  String(purchaseOrderDetail.value?.order.fixedProviderType || purchaseOrderSourceTrafficService.value?.fixedProviderType || "").toUpperCase()
);
const purchaseDetailHasBargeSupply = computed(() =>
  purchaseDetailOrderSupplyMode.value === "SEA" || purchaseDetailOrderProviderType.value === "BARGE" || Boolean(purchaseDetailPrimaryTrafficService.value?.bookingId)
);
const purchaseDetailSupplyModeLabel = computed(() => (purchaseDetailHasBargeSupply.value ? "海运" : "陆运"));
const purchaseDetailSupplyModeIcon = computed(() => (purchaseDetailHasBargeSupply.value ? "⛴" : "🚚"));
const latestPurchaseTimestamp = (values: Array<string | undefined | null>) => {
  const sortedValues = values
    .map((value) => String(value || "").trim())
    .filter(Boolean)
    .sort();
  return sortedValues[sortedValues.length - 1] || "";
};
const purchaseDetailActiveSupplierOrders = computed(() =>
  (purchaseOrderDetail.value?.supplierOrders ?? []).filter(
    (supplier) => !purchaseSupplierRejectedStatuses.has(String(supplier.status || "").toUpperCase())
  )
);
const purchaseTrafficWaitingCompletedStatuses = new Set(["IN_TRANSIT", "IN_PROGRESS", "IN_SERVICE", "SUPPLYING", "COMPLETED"]);
const purchaseTrafficSupplyingCompletedStatuses = new Set(["COMPLETED"]);
const purchaseDetailExecutionProgress = computed<Record<PurchaseExecutionStageKey, { completed: number; total: number }>>(() => {
  const suppliers = purchaseDetailActiveSupplierOrders.value;
  const supplierStatuses = suppliers.map((supplier) => String(supplier.status || "").toUpperCase());
  const supplierTotal = suppliers.length;
  const supplierConfirmed = supplierStatuses.filter((status) => !purchaseSupplierPendingStatuses.has(status)).length;
  const supplierStocked = supplierStatuses.filter((status) => purchaseSupplierReadyStatuses.has(status)).length;
  const supplierTransported = supplierStatuses.filter((status) => purchaseSupplierTransportStatuses.has(status) || purchaseSupplierCompletedStatuses.has(status)).length;
  const supplierWaitingSupply = supplierStatuses.filter(
    (status) => purchaseSupplierWaitingSupplyStatuses.has(status) || purchaseSupplierCompletedStatuses.has(status)
  ).length;
  const supplierCompleted = supplierStatuses.filter((status) => purchaseSupplierCompletedStatuses.has(status)).length;

  if (!purchaseDetailHasBargeSupply.value) {
    return {
      confirming: { completed: supplierConfirmed, total: supplierTotal },
      stocking: { completed: supplierStocked, total: supplierTotal },
      transporting: { completed: supplierTransported, total: supplierTotal },
      waitingSupply: { completed: supplierWaitingSupply, total: supplierTotal },
      supplying: { completed: supplierCompleted, total: supplierTotal },
      completed: { completed: supplierCompleted, total: supplierTotal }
    };
  }

  const trafficServices = purchaseDetailPrimaryTrafficService.value ? [purchaseDetailPrimaryTrafficService.value] : [];
  const trafficStatuses = trafficServices.map((service) => String(service.status || "").toUpperCase());
  const trafficTotal = trafficServices.length;
  const trafficWaitingCompleted = trafficStatuses.filter((status) => purchaseTrafficWaitingCompletedStatuses.has(status)).length;
  const trafficSupplyingCompleted = trafficStatuses.filter((status) => purchaseTrafficSupplyingCompletedStatuses.has(status)).length;
  return {
    confirming: { completed: supplierConfirmed, total: supplierTotal },
    stocking: { completed: supplierStocked, total: supplierTotal },
    transporting: { completed: supplierTransported, total: supplierTotal },
    waitingSupply: { completed: trafficWaitingCompleted, total: trafficTotal },
    supplying: { completed: trafficSupplyingCompleted, total: trafficTotal },
    completed: { completed: trafficSupplyingCompleted, total: trafficTotal }
  };
});
const purchaseDetailExecutionStageKey = computed<PurchaseExecutionStageKey>(() => {
  const orderStatus = String(purchaseOrderDetail.value?.order.status || "").toUpperCase();
  if (!orderStatus || orderStatus === "PENDING_SUPPLIER_CONFIRM") return "confirming";
  if (["PARTIALLY_CONFIRMED", "PREPARING"].includes(orderStatus)) return "stocking";
  if (["PARTIALLY_READY", "READY_TO_DELIVER", "IN_TRANSIT"].includes(orderStatus)) return "transporting";
  if (["WAITING_SUPPLY", "WAITING_SERVICE"].includes(orderStatus)) return "waitingSupply";
  if (["SUPPLYING", "IN_SERVICE", "IN_PROGRESS"].includes(orderStatus)) return "supplying";
  if (!purchaseDetailHasBargeSupply.value) {
    return ["PARTIALLY_SUPPLIED", "SUPPLIED", "COMPLETED"].includes(orderStatus) ? "completed" : "transporting";
  }
  const trafficStatus = String(purchaseDetailPrimaryTrafficService.value?.status || "").toUpperCase();
  if (trafficStatus === "COMPLETED") return "completed";
  if (["SUPPLYING", "IN_SERVICE", "IN_PROGRESS", "IN_TRANSIT"].includes(trafficStatus)) return "supplying";
  return "waitingSupply";
});
const purchaseDetailExecutionStageIndex = computed(() =>
  Math.max(0, purchaseExecutionStageDefinitions.findIndex((stage) => stage.key === purchaseDetailExecutionStageKey.value))
);
const purchaseDetailExecutionStageTimes = computed<Record<PurchaseExecutionStageKey, string>>(() => {
  const suppliers = purchaseOrderDetail.value?.supplierOrders ?? [];
  const traffic = purchaseDetailPrimaryTrafficService.value;
  const supplierWaitingSupplyTimes = suppliers.map((supplier) => supplier.suppliedAt || supplier.readyAt || supplier.confirmedAt);
  const trafficWaitingTime = latestPurchaseTimestamp([traffic?.useTime, traffic?.departureTime, traffic?.createdAt]);
  const trafficWorkingTime = latestPurchaseTimestamp([traffic?.updatedAt, traffic?.arrivalTime, traffic?.returnEndTime, traffic?.departureTime, traffic?.useTime]);
  const trafficCompletedTime = String(traffic?.status || "").toUpperCase() === "COMPLETED" ? trafficWorkingTime : "";
  return {
    confirming: latestPurchaseTimestamp(suppliers.map((supplier) => supplier.confirmedAt)),
    stocking: latestPurchaseTimestamp(suppliers.map((supplier) => supplier.readyAt)),
    transporting: latestPurchaseTimestamp(suppliers.map((supplier) => supplier.suppliedAt || supplier.readyAt || supplier.confirmedAt)),
    waitingSupply: purchaseDetailHasBargeSupply.value ? trafficWaitingTime : latestPurchaseTimestamp(supplierWaitingSupplyTimes),
    supplying: purchaseDetailHasBargeSupply.value ? trafficWorkingTime : latestPurchaseTimestamp(suppliers.map((supplier) => supplier.suppliedAt)),
    completed: purchaseDetailHasBargeSupply.value ? trafficCompletedTime : latestPurchaseTimestamp(suppliers.map((supplier) => supplier.suppliedAt))
  };
});
const purchaseDetailExecutionStages = computed(() =>
  purchaseExecutionStageDefinitions.map((stage, index) => {
    const progress = purchaseDetailExecutionProgress.value[stage.key];
    const isComplete = progress.total > 0 && progress.completed >= progress.total;
    const dateTime = isComplete ? purchaseDetailExecutionStageTimes.value[stage.key] : "";
    return {
      ...stage,
      state: isComplete ? "done" : index === purchaseDetailExecutionStageIndex.value ? "active" : "todo",
      progressText: `${progress.completed}/${progress.total}`,
      dateText: dateTime ? formatDateTimeParts(dateTime).date || dateTime.slice(0, 10) : "--"
    };
  })
);
const purchaseDetailVisibleExecutionStages = computed(() =>
  purchaseDetailExecutionStages.value.length
    ? purchaseDetailExecutionStages.value
    : purchaseExecutionStageDefinitions.map((stage, index) => ({
        ...stage,
        state: index === 0 ? "active" : "todo",
        progressText: "0/0",
        dateText: "--"
      }))
);
const purchaseDetailBargeExecutionCard = computed(() => {
  const row = purchaseDetailPrimaryTrafficService.value;
  if (!purchaseDetailHasBargeSupply.value || !row) return null;
  const provider = displayOrderText(row.trafficVesselName || row.supplierCompanyName || row.serviceNo || "驳船服务");
  const normalizedStatus = String(row.status || "").toUpperCase();
  const state = normalizedStatus === "COMPLETED"
    ? "done"
    : ["IN_PROGRESS", "IN_TRANSIT", "EXECUTING", "SUPPLYING", "IN_SERVICE"].includes(normalizedStatus)
      ? "active"
      : "todo";
  const statusText = trafficServiceStatusLabel(normalizedStatus);
  const statusVariant = trafficServiceStatusVariant(normalizedStatus);
  const time = formatExecutionTimeText(
    (row as { updatedAt?: string }).updatedAt || row.returnEndTime || row.arrivalTime || row.returnStartTime || row.departureTime || row.useTime || row.createdAt
  );
  const bookingId = Number(row.bookingId || 0);
  const trafficServiceOrderId = Number(row.serviceOrderId || 0);
  const matchedAttachments = fulfillmentAttachments.value.filter((item) => {
    if (item.providerType !== "BARGE") return false;
    if (bookingId && item.bookingId != null) return Number(item.bookingId || 0) === bookingId;
    if (trafficServiceOrderId && item.trafficServiceOrderId != null) return Number(item.trafficServiceOrderId || 0) === trafficServiceOrderId;
    return false;
  });
  return {
    provider,
    state,
    node: statusText,
    time,
    statusText,
    statusVariant,
    attachments: matchedAttachments,
    attachmentCount: matchedAttachments.length
  };
});
const purchaseDetailExecutionSummary = computed(() => {
  const suppliers = purchaseOrderDetail.value?.supplierOrders ?? [];
  const confirmed = suppliers.filter((supplier) => !purchaseSupplierPendingStatuses.has(String(supplier.status || "").toUpperCase())).length;
  const ready = suppliers.filter((supplier) => purchaseSupplierReadyStatuses.has(String(supplier.status || "").toUpperCase())).length;
  const supplied = suppliers.filter((supplier) => purchaseSupplierCompletedStatuses.has(String(supplier.status || "").toUpperCase())).length;
  return [
    { label: "供货商确认", value: `${confirmed}/${suppliers.length || 0}` },
    { label: "备货完成", value: `${ready}/${suppliers.length || 0}` },
    { label: "补给完成", value: `${supplied}/${suppliers.length || 0}` }
  ];
});
const purchaseDetailFixedFeeItems = computed(() => {
  const order = purchaseOrderDetail.value?.order;
  if (!order) return [];
  const booking = purchaseDetailPrimaryTrafficService.value;
  if (booking?.bookingId) {
    return [
      { key: "freight", label: "运费", value: Number(booking.bookingFreightFee ?? 0) },
      { key: "customs", label: "报关费", value: Number(booking.bookingCustomsFee ?? 0) },
      { key: "crane", label: "吊机费", value: Number(booking.bookingCraneFee ?? 0) }
    ].filter((item) => Number.isFinite(item.value) && item.value > 0);
  }
  return [
    { key: "freight", label: "运费", value: Number(order.fixedFreightFee ?? 0) },
    { key: "customs", label: "报关费", value: Number(order.fixedCustomsFee ?? 0) },
    { key: "crane", label: "吊机费", value: Number(order.fixedCraneFee ?? 0) },
    { key: "other", label: "其他费用", value: Number(order.fixedOtherFee ?? 0) }
  ].filter((item) => Number.isFinite(item.value) && item.value > 0);
});
const purchaseSettlementRows = computed(() => {
  const materialType = purchaseOrderDetail.value?.order.materialType || "物料供应";
  const supplierRows: Array<{ id: string; settlementId?: number; sourceType: "SUPPLIER" | "BARGE"; sourceId?: number; provider: string; type: string; quoteAmount: number; actualAmount: number; settlementStatus: string; rawStatus: string; invoiceAttachments: BusinessAttachmentPayload[] }> = (purchaseOrderDetail.value?.supplierOrders ?? []).map((supplier) => {
    const amount = Number(supplier.finalAmount ?? supplier.subtotalAmount ?? 0);
    const persisted = persistedSettlementRows.value.find((row) => row.settlementType === "SUPPLIER" && row.supplierOrderId === supplier.supplierOrderId);
    return {
      id: `supplier-${supplier.supplierOrderId}`,
      settlementId: persisted?.settlementId,
      sourceType: "SUPPLIER" as const,
      sourceId: Number(supplier.supplierOrderId || 0),
      provider: displayOrderText(supplier.supplierName),
      type: materialType,
      quoteAmount: Number.isFinite(amount) ? amount : 0,
      actualAmount: Number(persisted?.actualAmount || 0),
      settlementStatus: persisted ? settlementStatusLabel(persisted.status) : "未生成",
      rawStatus: String(persisted?.status || ""),
      invoiceAttachments: persisted?.invoiceAttachments || []
    };
  });
  const bargeSettlement = persistedSettlementRows.value.find((row) => row.settlementType === "BARGE" && row.purchaseOrderId === purchaseOrderDetail.value?.order.purchaseOrderId);
  if (bargeSettlement) {
    supplierRows.push({
      id: "fixed-fee",
      settlementId: bargeSettlement.settlementId,
      sourceType: "BARGE" as const,
      sourceId: bargeSettlement.trafficServiceOrderId,
      provider: bargeSettlement.providerName || "驳船服务",
      type: "补给费用",
      quoteAmount: Number(bargeSettlement.quotedAmount || 0),
      actualAmount: Number(bargeSettlement.actualAmount || 0),
      settlementStatus: settlementStatusLabel(bargeSettlement.status),
      rawStatus: bargeSettlement.status,
      invoiceAttachments: bargeSettlement.invoiceAttachments || []
    });
  }
  return supplierRows;
});
const purchaseSettlementAllSelected = computed(
  () => {
    const available = purchaseSettlementRows.value.filter((row) => String(row.rawStatus).toUpperCase() === "PENDING_SETTLEMENT");
    return available.length > 0 && available.every((row) => purchaseSettlementSelectedRowIds.value.includes(row.id));
  }
);
const purchaseSettlementTotal = computed(() =>
  purchaseSettlementRows.value.reduce((sum, row) => sum + Number(purchaseSettlementActualInputs.value[row.id] ?? row.quoteAmount ?? 0), 0)
);
const getPurchaseSettlementActualValue = (row: { id: string; quoteAmount: number }) =>
  purchaseSettlementActualInputs.value[row.id] ?? String(row.quoteAmount || "");
const setPurchaseSettlementActualValue = (rowId: string, value: string) => {
  purchaseSettlementActualInputs.value = {
    ...purchaseSettlementActualInputs.value,
    [rowId]: value
  };
};
const togglePurchaseSettlementRow = (rowId: string, checked: boolean) => {
  const selected = new Set(purchaseSettlementSelectedRowIds.value);
  if (checked) selected.add(rowId);
  else selected.delete(rowId);
  purchaseSettlementSelectedRowIds.value = Array.from(selected);
};
const toggleAllPurchaseSettlementRows = (checked: boolean) => {
  purchaseSettlementSelectedRowIds.value = checked ? purchaseSettlementRows.value.filter((row) => String(row.rawStatus).toUpperCase() === "PENDING_SETTLEMENT").map((row) => row.id) : [];
};
const submitPurchaseSettlementRows = async (rowId?: string) => {
  const targetIds = rowId ? [rowId] : purchaseSettlementSelectedRowIds.value;
  const purchaseOrderId = Number(purchaseOrderDetail.value?.order.purchaseOrderId || 0);
  if (!targetIds.length || !purchaseOrderId || settlementSaving.value) return;
  const targets = purchaseSettlementRows.value.filter((row) => targetIds.includes(row.id) && String(row.rawStatus).toUpperCase() === "PENDING_SETTLEMENT" && row.settlementId);
  if (!targets.length) return;
  settlementSaving.value = true;
  try {
    await Promise.all(targets.map((row) => settleSettlement(Number(row.settlementId))));
    persistedSettlementRows.value = (await listSettlements("BUYER")).filter((row) => row.purchaseOrderId === purchaseOrderId);
    purchaseSettlementSelectedRowIds.value = purchaseSettlementSelectedRowIds.value.filter((id) => !targetIds.includes(id));
    purchaseOrderDetailNotice.value = `已完成 ${targets.length} 条结算。`;
  } catch (error) {
    purchaseOrderDetailNotice.value = error instanceof ApiError ? error.message : "结算处理失败，请稍后重试。";
  } finally {
    settlementSaving.value = false;
  }
};

const settlementManagementScope = computed<SettlementScope>(() =>
  pageKey.value === "supplierSettlements" ? "SUPPLIER" : pageKey.value === "bargeSettlements" ? "BARGE" : "BUYER"
);
const isEvaluationPage = computed(() => ["evaluations", "regulatoryReviews", "qualitySupervision"].includes(pageKey.value));
const isRegulatoryEvaluationPage = computed(() => ["regulatoryReviews", "qualitySupervision"].includes(pageKey.value));
const supplierDetailSettlementRows = computed(() => {
  const orderId = Number(purchaseOrderDetail.value?.order.purchaseOrderId || 0);
  const visibleSupplierIds = new Set((purchaseOrderDetail.value?.supplierOrders || []).map((row) => Number(row.supplierOrderId)));
  return persistedSettlementRows.value.filter((row) =>
    row.purchaseOrderId === orderId
    && row.settlementType === "SUPPLIER"
    && (!row.supplierOrderId || visibleSupplierIds.has(Number(row.supplierOrderId)))
  );
});
const filteredSettlementManagementRows = computed(() => {
  const keyword = settlementManagementKeyword.value.trim().toLowerCase();
  const status = settlementManagementStatus.value.trim().toUpperCase();
  const scopedRows = settlementManagementScope.value === "BUYER"
    ? settlementManagementRows.value.filter((row) => String(row.status || "").toUpperCase() !== "PENDING_INVOICE")
    : settlementManagementRows.value;
  const statusRows = status ? scopedRows.filter((row) => String(row.status || "").toUpperCase() === status) : scopedRows;
  const rows = keyword ? statusRows.filter((row) =>
    [row.settlementNo, row.purchaseOrderNo, row.providerName, row.buyerCompanyName, row.vesselName].some((value) => String(value || "").toLowerCase().includes(keyword))
  ) : statusRows;
  return sortNewestFirst(rows, ["updatedAt", "createdAt"], ["settlementId"]);
});
const evaluationLogisticsRating = (row: ServiceEvaluation | null | undefined) =>
  Number((row as unknown as { logisticsRating?: number })?.logisticsRating || row?.rating || 0);
const evaluationStatusLabel = (status: string) => {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "PENDING_EVALUATION") return "待评价";
  if (normalized === "PENDING_REVIEW") return "待审查";
  if (normalized === "APPROVED") return "已完成";
  if (normalized === "REJECTED") return "已驳回";
  return "待评价";
};
const evaluationStatusVariant = (status: string): StatusVariant => {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "APPROVED") return "success";
  if (normalized === "REJECTED") return "danger";
  if (normalized === "PENDING_REVIEW") return "info";
  return "warning";
};
const isEvaluationReadonly = computed(() =>
  isRegulatoryEvaluationPage.value || String(evaluationEditing.value?.status || "").toUpperCase() === "PENDING_REVIEW"
);
const filteredEvaluationRows = computed(() => {
  const keyword = evaluationKeyword.value.trim().toLowerCase();
  const status = evaluationStatus.value.trim().toUpperCase();
  const scopedRows = isRegulatoryEvaluationPage.value
    ? evaluationRows.value.filter((row) => ["PENDING_REVIEW", "APPROVED"].includes(String(row.status || "").toUpperCase()))
    : evaluationRows.value;
  const statusRows = status ? scopedRows.filter((row) => String(row.status || "").toUpperCase() === status) : scopedRows;
  if (!keyword) return statusRows;
  return statusRows.filter((row) =>
    [row.providerName, row.purchaseOrderNo, row.serviceType, row.content].some((value) => String(value || "").toLowerCase().includes(keyword))
  );
});
const sortedEvaluationRows = computed(() => sortNewestFirst(filteredEvaluationRows.value, ["updatedAt", "createdAt"], ["evaluationId"]));
const settlementManagementStatusOptions = computed(() => {
  const values = settlementManagementScope.value === "BUYER"
    ? ["PENDING_SETTLEMENT", "SETTLED", "PAID"]
    : ["PENDING_INVOICE", "PENDING_SETTLEMENT", "SETTLED", "PAID"];
  return values.map((value) => ({ value, label: settlementStatusLabel(value) }));
});
const evaluationStatusOptions = computed(() => {
  const values = isRegulatoryEvaluationPage.value
    ? ["PENDING_REVIEW", "APPROVED"]
    : ["PENDING_EVALUATION", "PENDING_REVIEW", "APPROVED", "REJECTED"];
  return values.map((value) => ({ value, label: evaluationStatusLabel(value) }));
});
const resetEvaluationSearch = () => {
  evaluationKeyword.value = "";
  evaluationStatus.value = "";
};
const resetSettlementManagementSearch = () => {
  settlementManagementKeyword.value = "";
  settlementManagementStatus.value = "";
};
const settlementManagementColumns = computed<TableColumn[]>(() => [
  { key: "settlementNo", label: "结算单号", width: "170px" },
  { key: "purchaseOrderNo", label: "采购单号", width: "170px" },
  { key: "providerName", label: "服务商" },
  { key: "materialType", label: "类型", width: "120px", align: "center" },
  { key: "quotedAmount", label: "报价金额", width: "130px", align: "right" },
  { key: "actualAmount", label: "实际金额", width: "130px", align: "right" },
  { key: "status", label: "状态", width: "100px", align: "center" },
  { key: "operation", label: "操作", width: "100px", align: "center" }
]);
const loadSettlementManagement = async () => {
  settlementManagementLoading.value = true;
  settlementManagementError.value = "";
  try {
    settlementManagementRows.value = await listSettlements(settlementManagementScope.value, {
      keyword: settlementManagementKeyword.value,
      status: settlementManagementStatus.value,
      page: 1,
      size: 100
    });
  } catch {
    settlementManagementRows.value = [];
    settlementManagementError.value = "结算数据加载失败";
  } finally {
    settlementManagementLoading.value = false;
  }
};
const removeSettlementManagementRow = async (row: SettlementOrder) => {
  await deleteSettlement(row.settlementId);
  await loadSettlementManagement();
};
const saveSettlementManagementRow = async (row: SettlementOrder) => {
  await updateSettlement(row.settlementId, Number(row.actualAmount || 0));
  await loadSettlementManagement();
};
const settlementStatusLabel = (status: string) => {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "PENDING_INVOICE") return "待供发票";
  if (normalized === "PENDING_SETTLEMENT") return "待结算";
  if (normalized === "SETTLED") return "待付款";
  if (normalized === "PAID") return "已付款";
  return "待供发票";
};
const settlementStatusVariant = (status: string): StatusVariant => {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "PAID") return "success";
  if (normalized === "SETTLED") return "info";
  if (normalized === "PENDING_SETTLEMENT") return "warning";
  if (normalized === "PENDING_INVOICE") return "warning";
  return "neutral";
};
const settlementInvoiceDraft = (row: SettlementOrder) => {
  if (!settlementInvoiceDrafts.value[row.settlementId]) {
    settlementInvoiceDrafts.value[row.settlementId] = {
      actualAmount: Number(row.actualAmount || 0) > 0 ? String(row.actualAmount) : "",
      attachments: [...(row.invoiceAttachments || [])]
    };
  }
  return settlementInvoiceDrafts.value[row.settlementId];
};
const settlementEditingRow = computed(() =>
  [...settlementManagementRows.value, ...persistedSettlementRows.value].find((row) => row.settlementId === settlementEditingId.value) || null
);
const businessAttachmentUrl = (item: BusinessAttachmentPayload | FulfillmentAttachment) => {
  const rawUrl = String(item.fileUrl || "").trim();
  if (rawUrl) return normalizeShopImageUrl(rawUrl);
  const fileId = String(item.fileId || "").trim();
  return fileId ? buildFileUrlFromId(fileId) : "";
};
const uploadBusinessFile = async (file: File): Promise<BusinessAttachmentPayload> => {
  const uploaded = await uploadQualificationFile(file);
  const fileId = String(uploaded.fileId || uploaded.id || "");
  const fileUrl = uploaded.url ? normalizeShopImageUrl(uploaded.url) : fileId ? buildFileUrlFromId(fileId) : "";
  return { fileId, fileName: uploaded.name || file.name, fileUrl };
};
const uploadSettlementInvoiceFile = async (row: SettlementOrder, event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  if (!files.length) return;
  settlementInvoiceUploadingId.value = row.settlementId;
  try {
    for (const file of files) settlementInvoiceDraft(row).attachments.push(await uploadBusinessFile(file));
  } finally {
    settlementInvoiceUploadingId.value = 0;
    input.value = "";
  }
};
const submitSettlementInvoiceRow = async (row: SettlementOrder) => {
  const draft = settlementInvoiceDraft(row);
  if (!draft.actualAmount || !draft.attachments.length) {
    settlementManagementError.value = "请填写实际金额并上传发票附件。";
    return;
  }
  settlementSaving.value = true;
  try {
    await submitSettlementInvoice(row.settlementId, Number(draft.actualAmount), draft.attachments);
    settlementEditingId.value = 0;
    if (isPurchaseOrderDetailPage.value) await loadPurchaseOrderDetail();
    else await loadSettlementManagement();
  } finally {
    settlementSaving.value = false;
  }
};
const settleSettlementRow = async (row: SettlementOrder) => {
  settlementSaving.value = true;
  try {
    await settleSettlement(row.settlementId);
    await loadSettlementManagement();
  } finally {
    settlementSaving.value = false;
  }
};
const paySettlementRow = async (row: SettlementOrder) => {
  settlementSaving.value = true;
  try {
    await paySettlement(row.settlementId);
    await loadSettlementManagement();
  } finally {
    settlementSaving.value = false;
  }
};
const openFulfillmentDrawer = (title: string, items: FulfillmentAttachment[]) => {
  managedShuttleNodeContext.value = null;
  fulfillmentDrawerTitle.value = title;
  fulfillmentDrawerItems.value = items;
  fulfillmentDrawerOpen.value = true;
};
const previewBusinessAttachments = async (title: string, items: Array<BusinessAttachmentPayload | FulfillmentAttachment>) => {
  const images = await Promise.all(
    items.map(async (item) => {
      const src = businessAttachmentUrl(item);
      if (!src) return null;
      return { src: await resolveAuthenticatedImageUrl(src), alt: item.fileName || title };
    })
  );
  previewTitle.value = title;
  previewImages.value = images.filter((item): item is { src: string; alt: string } => Boolean(item?.src));
  previewAttributes.value = [];
  previewOpen.value = true;
};
const evaluationScope = computed(() => isRegulatoryEvaluationPage.value ? "REGULATORY" as const : "BUYER" as const);
const loadEvaluations = async () => {
  evaluationLoading.value = true;
  evaluationError.value = "";
  try {
    evaluationRows.value = await listServiceEvaluations(evaluationScope.value, {
      keyword: evaluationKeyword.value,
      status: evaluationStatus.value,
      page: 1,
      size: 100
    });
  } catch {
    evaluationRows.value = [];
    evaluationError.value = "评价数据加载失败";
  } finally {
    evaluationLoading.value = false;
  }
};
const openEvaluationEditor = (row: ServiceEvaluation) => {
  evaluationEditing.value = row;
  evaluationForm.value = { rating: row.rating || 5, logisticsRating: evaluationLogisticsRating(row) || 5, content: row.content || "", attachments: [...(row.attachments || [])], reviewRemark: row.reviewRemark || "" };
};
const uploadEvaluationFile = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  evaluationUploading.value = true;
  try {
    evaluationForm.value.attachments.push(await uploadBusinessFile(file));
  } finally {
    evaluationUploading.value = false;
    input.value = "";
  }
};
const submitEvaluation = async () => {
  if (!evaluationEditing.value) return;
  evaluationSaving.value = true;
  try {
    await submitServiceEvaluation(evaluationEditing.value.evaluationId, evaluationForm.value.rating, evaluationForm.value.content, evaluationForm.value.attachments, evaluationForm.value.logisticsRating);
    evaluationEditing.value = null;
    await loadEvaluations();
  } finally {
    evaluationSaving.value = false;
  }
};
const reviewEvaluation = async (action: "approve" | "reject") => {
  if (!evaluationEditing.value) return;
  evaluationSaving.value = true;
  try {
    await reviewServiceEvaluation(evaluationEditing.value.evaluationId, action, evaluationForm.value.reviewRemark);
    evaluationEditing.value = null;
    await loadEvaluations();
  } finally {
    evaluationSaving.value = false;
  }
};
const purchaseDetailBargeNodeRows = computed(() => {
  const row = purchaseDetailPrimaryTrafficService.value;
  if (!row) return [];
  if (row.shuttleServiceNodes?.length) {
    return row.shuttleServiceNodes.map((node, index) => ({
      label: trafficShuttleNodeTimeLabel(node),
      state: index === Number(row.bookingNodeIndex ?? -1) ? "active" : "normal"
    }));
  }
  if (row.bookingNodeTime || row.bookingNodeName) {
    return [{ label: row.bookingNodeTime || row.bookingNodeName || "预约节点", state: "active" }];
  }
  const planTime = formatDateTimeParts(row.useTime || row.departureTime || "").time;
  const arrivalTime = formatDateTimeParts(row.arrivalTime || row.returnEndTime || "").time;
  return [
    { label: planTime || "待发", state: "active" },
    { label: "服务中", state: "normal" },
    { label: arrivalTime || "到达", state: "normal" }
  ];
});
const purchaseDetailBargeOriginLabel = computed(() =>
  displayOrderText(displaySupplyPort(purchaseDetailPrimaryTrafficService.value?.shuttleDeparturePoint || purchaseOrderDetailForm.value.supplyPort || purchaseOrderDetail.value?.order.supplyPort))
);
const purchaseDetailBargeDestinationLabel = computed(() =>
  displayOrderText(purchaseDetailPrimaryTrafficService.value?.shuttleDestinationPoint || purchaseDetailPrimaryTrafficService.value?.anchorageName || purchaseDetailPrimaryTrafficService.value?.anchorageCode)
);
const purchaseDetailBargeStartTimeLabel = computed(() =>
  formatDateTimeParts(purchaseDetailPrimaryTrafficService.value?.shuttleStartTime || purchaseDetailPrimaryTrafficService.value?.useTime || purchaseDetailPrimaryTrafficService.value?.departureTime || "").time || "-"
);
const purchaseDetailBargeEndTimeLabel = computed(() =>
  formatDateTimeParts(purchaseDetailPrimaryTrafficService.value?.shuttleReturnTime || purchaseDetailPrimaryTrafficService.value?.arrivalTime || purchaseDetailPrimaryTrafficService.value?.returnEndTime || "").time || "-"
);
const purchaseDetailBargeDateLabel = computed(() =>
  formatDateTimeParts(purchaseDetailPrimaryTrafficService.value?.shuttleStartTime || purchaseDetailPrimaryTrafficService.value?.useTime || purchaseDetailPrimaryTrafficService.value?.departureTime || "").date || "-"
);
const purchaseDetailBargeTitleLabel = computed(() => {
  const row = purchaseDetailPrimaryTrafficService.value as (TrafficServiceOrder & { vesselName?: string; serviceProviderName?: string }) | null;
  return [displayOrderText(row?.trafficVesselName || row?.vesselName || row?.serviceProviderName || "驳船"), purchaseDetailBargeDateLabel.value]
    .filter((value) => value && value !== "-")
    .join(" / ");
});
const purchaseDetailBargeReadonlyShuttle = computed<TrafficShuttleService | null>(() => {
  const row = purchaseDetailPrimaryTrafficService.value;
  if (!row) return null;
  const serviceNodes = row.shuttleServiceNodes?.length
    ? row.shuttleServiceNodes
    : purchaseDetailBargeNodeRows.value.map((node, index) => {
        const parts = String(node.label || "").split("-");
        return {
          nodeName: row.bookingNodeName || `节点${index + 1}`,
          startTime: parts[0] || node.label || "",
          endTime: parts[1] || ""
        };
      });
  const nodeIndex = Math.max(0, Number(row.bookingNodeIndex ?? 0));
  const cargos = row.cargos || [];
  const cargoWeight = cargos.reduce((sum, cargo) => sum + Number(cargo.weightKg || 0), 0);
  const cargoVolume = cargos.reduce((sum, cargo) => sum + Number(cargo.volumeCbm || 0), 0);
  const bookingAmount = Number(row.bookingAmount || row.bookingFreightFee || row.basePrice || row.sharedPrice || 0);
  return {
    shuttleId: Number(row.shuttleServiceId || 0),
    shuttleNo: row.shuttleNo || row.serviceNo || "",
    supplierCompanyId: row.supplierCompanyId,
    supplierCompanyName: row.supplierCompanyName,
    seaArea: row.seaArea,
    anchorageCode: row.anchorageCode,
    anchorageName: row.anchorageName,
    departurePoint: row.shuttleDeparturePoint || purchaseDetailBargeOriginLabel.value,
    destinationPoint: row.shuttleDestinationPoint || purchaseDetailBargeDestinationLabel.value,
    startTime: row.shuttleStartTime || row.useTime || row.departureTime || "",
    returnTime: row.shuttleReturnTime || row.arrivalTime || row.returnEndTime || "",
    basePrice: Number(row.basePrice || row.bookingFreightFee || bookingAmount || 0),
    sharedPrice: Number(row.sharedPrice || row.bookingFreightFee || bookingAmount || 0),
    customsPrice: Number(row.bookingCustomsFee || 0),
    cranePrice: Number(row.bookingCraneFee || 0),
    trafficVesselId: row.trafficVesselId,
    trafficVesselName: row.trafficVesselName,
    status: row.status,
    remark: row.remark,
    serviceNodes,
    bookings: [
      {
        bookingId: Number(row.bookingId || row.serviceOrderId || 0),
        bookingNo: row.serviceNo || "",
        shuttleServiceId: Number(row.shuttleServiceId || 0),
        purchaseOrderId: Number(row.purchaseOrderId || purchaseOrderDetail.value?.order.purchaseOrderId || 0),
        requestNo: purchaseOrderDetail.value?.order.inquiryNo || purchaseOrderDetail.value?.order.demandNo || row.serviceNo,
        nodeIndex,
        nodeName: row.bookingNodeName || serviceNodes[nodeIndex]?.nodeName,
        nodeTime: row.bookingNodeTime || trafficShuttleNodeTimeLabel(serviceNodes[nodeIndex] || {}),
        vesselName: purchaseOrderDetail.value?.order.vesselName || row.trafficVesselName || "",
        vesselImo: "",
        anchorageTime: purchaseOrderDetail.value?.order.vesselEta || row.useTime || row.departureTime || "",
        anchoragePosition: "",
        palletCount: "",
        trafficServiceOrderId: row.serviceOrderId,
        cargoWeightKg: cargoWeight || undefined,
        cargoVolumeCbm: cargoVolume || undefined,
        amount: bookingAmount,
        allowShare: Boolean(row.allowShare),
        customsService: Number(row.bookingCustomsFee || 0) > 0,
        craneService: Number(row.bookingCraneFee || 0) > 0,
        craneCount: Number(row.bookingCraneFee || 0) > 0 ? 1 : undefined,
        freightFee: Number(row.bookingFreightFee || bookingAmount || 0),
        customsFee: Number(row.bookingCustomsFee || 0),
        craneFee: Number(row.bookingCraneFee || 0),
        contactName: row.businessContactName || row.handlerName,
        contactPhone: row.businessContactPhone || row.handlerPhone,
        remark: row.remark,
        status: row.status,
        createdAt: row.createdAt
      }
    ],
    createdAt: row.createdAt
  };
});
const purchaseTrafficServiceColumns = computed<TableColumn[]>(() => [
  { key: "feeType", label: t("trafficService.field.feeType"), width: "72px", align: "center" },
  { key: "supplierCompanyName", label: t("trafficService.field.supplier"), width: "118px" },
  { key: "anchorageName", label: t("trafficService.field.destination"), width: "126px" },
  { key: "useTime", label: t("trafficService.field.planTime"), width: "128px" },
  { key: "freightAmount", label: t("trafficService.field.freightAmount"), width: "90px", align: "right" },
  { key: "status", label: t("field.status"), width: "88px", align: "center" },
  { key: "allowShare", label: t("trafficService.field.allowShare"), width: "72px", align: "center" },
  { key: "operation", label: t("common.operation"), width: "62px", align: "center" }
]);
const supplierCustomsFiles = ref<Array<{ fileId?: string; fileName: string; fileUrl?: string; error?: string }>>([]);
const supplierCustomsUploading = ref(false);
const supplierCustomsError = ref("");

const purchaseDetailCostAmount = computed(() =>
  purchaseDetailItemRows.value.reduce((sum, row) => sum + Number(row.amount ?? 0), 0)
);

const purchaseDetailQuoteAmount = computed(() =>
  purchaseDetailItemRows.value.reduce((sum, row) => {
    const quotePrice = Number(row.actualQuotePrice ?? 0);
    const quantity = Number(row.pricingQuantity ?? row.quantity ?? 0);
    const fallback = Number(row.amount ?? 0);
    return sum + (quotePrice > 0 && quantity > 0 ? quotePrice * quantity : fallback);
  }, 0)
);

const purchaseDetailFixedFeeAmount = computed(() => {
  const order = purchaseOrderDetail.value?.order;
  if (!order) return 0;
  return Number(order.fixedFreightFee ?? 0)
    + Number(order.fixedCustomsFee ?? 0)
    + Number(order.fixedCraneFee ?? 0)
    + Number(order.fixedOtherFee ?? 0);
});

const purchaseDetailProfitAmount = computed(() => purchaseDetailQuoteAmount.value - purchaseDetailCostAmount.value - purchaseDetailFixedFeeAmount.value);

const supplierVisibleItemTotal = computed(() =>
  purchaseDetailItemRows.value.reduce((sum, row) => sum + Number(row.amount ?? 0), 0)
);

const showSupplierCustomsUpload = computed(() => {
  if (purchaseOrderWorkspaceMode.value !== "supplier") return false;
  const order = purchaseOrderDetail.value?.supplierOrders?.[0];
  const status = String(order?.status || "").toUpperCase();
  return Boolean(order && ["PREPARING", "IN_TRANSIT", "READY_TO_DELIVER", "SUPPLIED"].includes(status));
});

watch(
  () => purchaseOrderDetail.value?.attachments,
  (attachments) => {
    if (purchaseOrderWorkspaceMode.value !== "supplier") return;
    supplierCustomsFiles.value = (attachments ?? [])
      .filter((item) => String(item.attachmentType || "").toUpperCase() === "CUSTOMS_DOCUMENT")
      .map((item) => ({
        fileId: item.fileId,
        fileName: item.fileName || item.fileId || item.fileUrl || "-",
        fileUrl: item.fileUrl || (item.fileId ? buildFileUrlFromId(item.fileId) : "")
      }));
  },
  { immediate: true }
);

const formatPurchaseMoney = (amount?: number, currency?: string) => {
  return formatDisplayMoney(amount, currency || "CNY");
};

const formatPurchaseMoneyValue = (amount?: number) => formatPurchaseMoney(amount, "CNY");

const purchaseOrderEventMessage = (event: PurchaseOrderEvent) => {
  const eventType = String(event.eventType || "").toUpperCase();
  if (eventType === "QC_REQUIRED") return t("purchaseOrder.notice.customsRequired");
  return event.eventMessage || event.eventType || "-";
};

const purchaseStatusLabel = (status?: string | null) => {
  const value = String(status || "").toUpperCase();
  if (value === "WAITING_SUPPLY" || value === "WAITING_SERVICE") return "待补给";
  if (value === "SUPPLYING" || value === "IN_SERVICE" || value === "IN_PROGRESS") return "补给中";
  const key = `purchaseOrder.status.${value}`;
  const label = t(key);
  return label === key ? status || "-" : label;
};

const supplierPurchaseStatusLabel = (status?: string | null) => {
  const value = String(status || "").toUpperCase();
  if (value === "READY_TO_DELIVER") return t("purchaseOrder.statusSupplier.READY_TO_DELIVER");
  if (value === "PARTIALLY_READY") return t("purchaseOrder.statusSupplier.PARTIALLY_READY");
  if (value === "WAITING_SUPPLY" || value === "WAITING_SERVICE") return "待补给";
  if (value === "SUPPLYING" || value === "IN_SERVICE" || value === "IN_PROGRESS") return "补给中";
  return purchaseStatusLabel(status);
};

const purchaseStatusVariant = (status?: string | null): StatusVariant => {
  const value = String(status || "").toUpperCase();
  if (value === "PREPARING" || value === "READY_TO_DELIVER" || value === "SUPPLIED" || value === "PARTIALLY_SUPPLIED" || value === "COMPLETED" || value === "SUPPLYING" || value === "IN_SERVICE" || value === "IN_PROGRESS") return "success";
  if (value === "PENDING_SUPPLIER_CONFIRM" || value === "PARTIALLY_CONFIRMED" || value === "PARTIALLY_READY" || value === "WAITING_SUPPLY" || value === "WAITING_SERVICE") return "warning";
  if (value === "REJECTED" || value === "PARTIALLY_REJECTED" || value === "CANCELED" || value === "DISCARDED") return "danger";
  return "info";
};

const packagingMethodLabel = (value?: string | null) => {
  const text = String(value || "").toUpperCase();
  if (!text) return t("purchaseOrder.packaging.DEFAULT");
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
      DELIVERY_IMAGE_REQUIRED: "purchaseOrder.error.deliveryImageRequired",
      PURCHASE_ORDER_NOT_FOUND: "purchaseOrder.error.notFound",
      SUPPLIER_ORDER_NOT_FOUND: "purchaseOrder.error.supplierOrderNotFound"
    };
    return keys[message] ? t(keys[message]) : message || t(fallbackKey);
  }
  return error instanceof Error && error.message ? error.message : t(fallbackKey);
};

const canDiscardStatus = (status?: string | null) => String(status || "").toUpperCase() !== "DISCARDED";

const openDiscardDialog = (target: { type: "demand" | "purchaseOrder"; id: number | string; refresh: () => void | Promise<void> }) => {
  discardTarget.value = target;
  discardError.value = "";
  discardDialogOpen.value = true;
};

const closeDiscardDialog = () => {
  if (discardSaving.value) return;
  discardDialogOpen.value = false;
  discardTarget.value = null;
  discardError.value = "";
};

const confirmDiscardDocument = async () => {
  if (!discardTarget.value) return;
  discardSaving.value = true;
  discardError.value = "";
  try {
    if (discardTarget.value.type === "purchaseOrder") {
      await discardPurchaseOrder(discardTarget.value.id);
    } else {
      await discardMaterialDemand(discardTarget.value.id);
    }
    const refresh = discardTarget.value.refresh;
    closeDiscardDialog();
    await refresh();
  } catch (error) {
    discardError.value = error instanceof Error && error.message ? error.message : t("common.operationFailed");
  } finally {
    discardSaving.value = false;
  }
};

const selectCompareStrategy = (key: string) => {
  const strategy = compareStrategyCards.value.find((item) => item.key === key);
  if (!strategy?.enabled || isCompareReadonly.value) return;
  selectedStrategy.value = key;
  selectedCompareSupplier.value = null;
  expandedCompareRowId.value = "";
  window.setTimeout(syncCompareSelectedRows, 0);
};

const selectCompareSupplier = (strategyKey: string, supplierKey: string) => {
  const strategy = compareStrategyCards.value.find((item) => item.key === strategyKey);
  if (!strategy?.enabled || isCompareReadonly.value) return;
  const shouldClear = selectedStrategy.value === strategyKey && selectedCompareSupplier.value === supplierKey;
  selectedStrategy.value = strategyKey;
  selectedCompareSupplier.value = shouldClear ? null : supplierKey;
  expandedCompareRowId.value = "";
  window.setTimeout(syncCompareSelectedRows, 0);
};

const toggleComparePreferenceFilter = (value: string) => {
  comparePreferenceFilters.value = comparePreferenceFilters.value.includes(value)
    ? comparePreferenceFilters.value.filter((item) => item !== value)
    : [...comparePreferenceFilters.value, value];
};

const openReplacementDialog = async (sku: CompareSkuRow) => {
  if (isCompareReadonly.value) return;
  replacementSku.value = sku;
  replacementCandidates.value = [];
  replacementSearchKeyword.value = "";
  replacementError.value = "";
  if (!compareDemandId.value || !sku.demandItemId) return;
  replacementLoading.value = true;
  try {
    replacementCandidates.value = await listMaterialDemandItemSupplierCandidates(compareDemandId.value, sku.demandItemId);
  } catch (error) {
    replacementError.value = error instanceof Error && error.message ? error.message : t("compare.skuCandidateLoadFailed");
  } finally {
    replacementLoading.value = false;
  }
};

const closeReplacementDialog = () => {
  if (replacementLoading.value) return;
  replacementSku.value = null;
  replacementCandidates.value = [];
  replacementSearchKeyword.value = "";
  replacementError.value = "";
};

const applyReplacementCandidate = (candidate: MaterialComparisonCandidate) => {
  if (!replacementSku.value?.demandItemId) return;
  compareRowReplacements.value = {
    ...compareRowReplacements.value,
    [replacementSku.value.demandItemId]: candidate
  };
  replacementSku.value = null;
  replacementCandidates.value = [];
  replacementSearchKeyword.value = "";
  window.setTimeout(syncCompareSelectedRows, 0);
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
  replacementCandidates.value = [];
  replacementSearchKeyword.value = "";
  compareRowReplacements.value = {};
  selectedCompareRowIds.value = [];
  compareSelectionInitialized.value = false;
  compareQuantityInputs.value = {};
  compareRemarkInputs.value = {};
  compareQuoteMarkupInputs.value = {};
  expandedCompareRowId.value = "";
  comparePreferenceFilters.value = comparePreferenceFilters.value.filter((item) => item === "priceLow");
  if (!currentDemandId) {
    compareError.value = t("compare.missingDemandId");
    return;
  }
  compareLoading.value = true;
  try {
    const response = await getMaterialDemandComparison(currentDemandId);
    compareData.value = response;
    applyCompareTrafficService(response.demand?.trafficService);
    applyCompareSupplyModeFromTrafficService(response.demand?.trafficService);
    compareDisplayCurrency.value = normalizeCompareText(response.demand?.currency).toUpperCase() === "USD" ? "USD" : "CNY";
    compareFixedFeeInputs.value = {
      shuttle: compareTrafficLockedShuttleLabel(response.demand?.trafficService),
      freight: normalizeCompareFeeDisplayInput(response.demand?.fixedFreightFee),
      customs: normalizeCompareFeeDisplayInput(response.demand?.fixedCustomsFee),
      crane: normalizeCompareFeeDisplayInput(response.demand?.fixedCraneFee),
      other: normalizeCompareFeeDisplayInput(response.demand?.fixedOtherFee)
    };
    compareQuantityInputs.value = Object.fromEntries(response.items.map((item, index) => [compareDemandItemKey(item, index), normalizeCompareText(item.quantity)]));
    compareRemarkInputs.value = Object.fromEntries(response.items.map((item, index) => [compareDemandItemKey(item, index), normalizeCompareText(item.remarks)]));
    const savedMarkup = response.items.find((item) => item.quoteMarkupPercent != null)?.quoteMarkupPercent;
    const defaultMarkup = savedMarkup ?? compareQuoteMarkupPercent.value;
    compareQuoteMarkupPercent.value = defaultMarkup;
    compareQuoteMarkupInputs.value = Object.fromEntries(
      response.items
        .map((item, index) => [compareDemandItemKey(item, index), item.quoteMarkupPercent == null || item.quoteMarkupPercent === defaultMarkup ? "" : String(item.quoteMarkupPercent)] as const)
        .filter(([, value]) => value)
    );
    compareQuoteActualPrices.value = Object.fromEntries(
      response.items
        .map((item, index) => {
          const key = compareDemandItemKey(item, index);
          const candidate = item.lowestCandidate ?? item.singleSupplierCandidate ?? item.candidates[0];
          const expected = candidate?.unitPrice == null ? undefined : ceilMoneyToCents(candidate.unitPrice * (1 + (item.quoteMarkupPercent ?? defaultMarkup) / 100));
          const saved = item.actualQuotePrice == null ? undefined : ceilMoneyToCents(item.actualQuotePrice);
          return [key, saved != null && expected != null && Math.abs(saved - expected) > 0.01 ? String(saved) : ""] as const;
        })
        .filter(([, value]) => value)
    );
    compareUnitSelections.value = {};
    selectedStrategy.value = response.strategies.find((strategy) => strategy.enabled !== false)?.strategyType || response.strategies[0]?.strategyType || "LOWEST_MIXED";
    const savedQuoteItemIds = new Set(response.items.filter((item) => item.actualQuotePrice != null).map((item, index) => compareDemandItemKey(item, index)));
    const rowsAfterLoad = compareSkuRows.value;
    selectedCompareRowIds.value = savedQuoteItemIds.size
      ? rowsAfterLoad.filter((row) => savedQuoteItemIds.has(String(row.demandItemId)) && isCompareRowOrderable(row)).map((row) => row.id)
      : rowsAfterLoad.filter(isCompareRowOrderable).map((row) => row.id);
    compareSelectionInitialized.value = true;
    const supplyInfo = response.supplyInfo;
    const handlerContact = [response.demand?.handlerName, response.demand?.handlerEmail].filter(Boolean).join(" / ");
    const supplyPortDisplay = displaySupplyPort(
      normalizeCompareText(supplyInfo?.supplyPort)
        || normalizeCompareText(supplyInfo?.port)
        || normalizeCompareText(response.demand?.supplyPortName)
        || normalizeCompareText(response.demand?.supplyPortCode)
    );
    compareSupplyForm.value = {
      vessel: normalizeCompareText(supplyInfo?.vesselName) || normalizeCompareText(response.demand?.vesselName) || "",
      inquiryNo: normalizeCompareText(response.demand?.inquiryNo) || normalizeCompareText(response.demand?.demandNo) || "",
      materialType: normalizeCompareText(response.demand?.materialType) || "",
      currency: normalizeCompareText(response.demand?.currency) || "CNY",
      port: supplyPortDisplay || t("compare.supply.portPending"),
      date: normalizeCompareText(supplyInfo?.supplyDate) || normalizeCompareText(supplyInfo?.inquiryDate) || normalizeCompareText(response.demand?.vesselEta) || normalizeCompareText(response.demand?.inquiryDate) || "",
      recipientCompany: normalizeCompareText(response.demand?.recipientCompany) || "",
      handlerContact,
      supplyMode: response.demand?.trafficService?.supplyMode === "LAND" || response.demand?.trafficService?.fixedProviderType === "SUPPLIER" ? "LAND" : "SEA"
    };
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

const clearPurchaseOrderFormError = (key: keyof typeof purchaseOrderFormErrors.value) => {
  if (!purchaseOrderFormErrors.value[key]) return;
  purchaseOrderFormErrors.value = { ...purchaseOrderFormErrors.value, [key]: "" };
};

const applyPurchaseOrderContact = (contact: CompanyContactRow | null) => {
  purchaseOrderForm.value.deliveryContactId = contact?.contactId ? String(contact.contactId) : "";
  purchaseOrderForm.value.deliveryContactName = contact?.contactName || "";
  purchaseOrderForm.value.deliveryContactPhone = contact?.contactPhone || "";
  purchaseOrderForm.value.deliveryContactEmail = contact?.contactEmail || "";
  purchaseOrderFormErrors.value = {
    ...purchaseOrderFormErrors.value,
    deliveryContactName: "",
    deliveryContactPhone: ""
  };
};

const handlePurchaseOrderContactChange = () => {
  const contactId = purchaseOrderForm.value.deliveryContactId;
  if (contactId === fallbackDeliveryContactId) {
    purchaseOrderForm.value.deliveryContactName = fallbackDeliveryContact.contactName;
    purchaseOrderForm.value.deliveryContactPhone = fallbackDeliveryContact.contactPhone;
    purchaseOrderForm.value.deliveryContactEmail = fallbackDeliveryContact.contactEmail;
    purchaseOrderFormErrors.value = {
      ...purchaseOrderFormErrors.value,
      deliveryContactName: "",
      deliveryContactPhone: ""
    };
    return;
  }
  const contact = activeCompanyContacts.value.find((item) => String(item.contactId || item.id) === contactId) || null;
  applyPurchaseOrderContact(contact);
};

const validatePurchaseOrderForm = () => {
  const errors = {
    supplyPort: "",
    requiredDeliveryTime: "",
    deliveryContactName: "",
    deliveryContactPhone: "",
    defaultPackagingMethod: ""
  };
  if (!purchaseOrderForm.value.requiredDeliveryTime.trim()) errors.requiredDeliveryTime = t("purchaseOrder.error.requiredDeliveryTimeRequired");
  if (!purchaseOrderForm.value.deliveryContactName.trim()) errors.deliveryContactName = t("purchaseOrder.error.deliveryContactNameRequired");
  if (!purchaseOrderForm.value.deliveryContactPhone.trim()) errors.deliveryContactPhone = t("purchaseOrder.error.deliveryContactPhoneRequired");
  if (!purchaseOrderForm.value.defaultPackagingMethod.trim()) errors.defaultPackagingMethod = t("purchaseOrder.error.defaultPackagingMethodRequired");
  purchaseOrderFormErrors.value = errors;
  return !Object.values(errors).some(Boolean);
};

const formatDateTimePickerDisplay = (value?: string | null) => {
  const text = normalizeCompareText(value);
  return text ? text.replace("T", " ").slice(0, 16) : "";
};

const openPurchaseOrderDialog = async () => {
  purchaseOrderNotice.value = "";
  purchaseOrderError.value = "";
  purchaseOrderFormErrors.value = {
    supplyPort: "",
    requiredDeliveryTime: "",
    deliveryContactName: "",
    deliveryContactPhone: "",
    defaultPackagingMethod: ""
  };
  if (!compareDemandId.value) {
    purchaseOrderError.value = t("page.materials.saveBeforeCompare");
    return;
  }
  if (isCompareReadonly.value) {
    purchaseOrderError.value = compareData.value?.isDiscarded ? t("compare.discardedReadonly") : t("compare.orderedReadonly");
    return;
  }
  if (!selectedComparePurchaseItems.value.length) {
    purchaseOrderError.value = t("purchaseOrder.error.noOrderableItems");
    return;
  }
  purchaseOrderSelectedItemsSnapshot.value = selectedComparePurchaseItems.value.map((item) => ({ ...item }));
  if (!companyContactRows.value.length) {
    try {
      await loadCompanyContacts();
    } catch {
      companyContactRows.value = [];
    }
  }
  try {
    await loadTrafficAnchorages();
  } catch {
    // The traffic boat section can still stay hidden when anchorage data is unavailable.
  }
  const defaultContact = activeCompanyContacts.value.find((item) => item.contactName === fallbackDeliveryContact.contactName) || null;
  purchaseOrderForm.value = {
    supplyPort: displaySupplyPort(compareSupplyForm.value.port) || "",
    vesselEta: compareSupplyForm.value.date || normalizeCompareText(compareData.value?.supplyInfo?.vesselEta) || "",
    requiredDeliveryTime: "",
    deliveryContactId: defaultContact?.contactId ? String(defaultContact.contactId) : fallbackDeliveryContactId,
    deliveryContactName: defaultContact?.contactName || fallbackDeliveryContact.contactName,
    deliveryContactPhone: defaultContact?.contactPhone || fallbackDeliveryContact.contactPhone,
    deliveryContactEmail: defaultContact?.contactEmail || fallbackDeliveryContact.contactEmail,
    defaultPackagingMethod: "UNIFIED_PACKAGING",
    buyerRemark: "",
    customsServiceMode: "PACKAGE",
    craneServiceMode: "PACKAGE",
    trafficBoatEnabled: false
  };
  if (compareSupplyForm.value.supplyMode === "SEA") {
    try {
      await loadCompareTrafficShuttles();
    } catch {
      // Keep the order dialog usable; saved traffic service snapshots still provide the route when available.
    }
  }
  purchaseOrderDialogOpen.value = true;
};

const submitPurchaseOrder = async () => {
  if (!compareDemandId.value || !activeCompareStrategy.value) {
    purchaseOrderError.value = t("page.materials.saveBeforeCompare");
    return;
  }
  const selectedPurchaseItems = purchaseOrderSelectedItemsSnapshot.value.length
    ? purchaseOrderSelectedItemsSnapshot.value
    : selectedComparePurchaseItems.value;
  if (!selectedPurchaseItems.length) {
    purchaseOrderError.value = t("purchaseOrder.error.noOrderableItems");
    return;
  }
  if (isCompareReadonly.value) {
    purchaseOrderError.value = compareData.value?.isDiscarded ? t("compare.discardedReadonly") : t("compare.orderedReadonly");
    return;
  }
  if (!validatePurchaseOrderForm()) {
    purchaseOrderError.value = t("purchaseOrder.error.requiredFields");
    return;
  }
  purchaseOrderCreating.value = true;
  purchaseOrderError.value = "";
  purchaseOrderNotice.value = "";
  try {
    const savedQuote = await saveCompareQuotePrices(true);
    if (!savedQuote) {
      purchaseOrderError.value = compareQuoteError.value || t("compare.quoteSaveFailed");
      return;
    }
    const selectedItems = selectedPurchaseItems.map((item) => ({
      demandItemId: item.demandItemId,
      skuId: item.skuId,
      supplierCompanyId: item.supplierCompanyId,
      supplierName: item.supplierName,
      supplierSkuCode: item.supplierSkuCode,
      platformCode: item.platformCode,
      impaCode: item.impaCode,
      productName: item.productName,
      specification: item.specification,
      quantity: item.quantity,
      unit: item.unit,
      selectedUnit: item.selectedUnit,
      pricingQuantity: item.pricingQuantity,
      unitPrice: item.unitPrice,
      unitPriceUsd: item.unitPriceUsd,
      currency: item.currency,
      amount: item.amount,
      amountUsd: item.amountUsd,
      unitMismatchFlag: item.unitMismatchFlag,
      quantityFallbackFlag: item.quantityFallbackFlag
    }));
    const response = await createPurchaseOrderFromDemand(compareDemandId.value, {
      demandId: Number(compareDemandId.value),
      strategyType: activeCompareStrategy.value.key,
      supplyPort: purchaseOrderForm.value.supplyPort,
      vesselEta: purchaseOrderForm.value.vesselEta,
      requiredDeliveryTime: purchaseOrderForm.value.requiredDeliveryTime,
      deliveryContactName: purchaseOrderForm.value.deliveryContactName,
      deliveryContactPhone: purchaseOrderForm.value.deliveryContactPhone,
      deliveryContactEmail: purchaseOrderForm.value.deliveryContactEmail,
      defaultPackagingMethod: purchaseOrderForm.value.defaultPackagingMethod,
      buyerRemark: purchaseOrderForm.value.buyerRemark,
      selectedItems
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
      createdFrom: purchaseOrderCreatedFrom.value,
      createdTo: purchaseOrderCreatedTo.value,
      page: 1,
      size: 20
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
      size: 20
    });
    supplierPurchaseOrders.value = response.items;
  } catch {
    supplierPurchaseOrders.value = [];
    supplierPurchaseOrderErrorKey.value = "purchaseOrder.error.supplierLoadFailed";
  } finally {
    supplierPurchaseOrderLoading.value = false;
  }
};

const syncPurchaseOrderDetailForm = () => {
  const order = purchaseOrderDetail.value?.order;
  purchaseOrderDetailForm.value = {
    supplyPort: displaySupplyPort(repairMojibakeText(order?.supplyPort)),
    vesselEta: order?.vesselEta || "",
    requiredDeliveryTime: order?.requiredDeliveryTime || "",
    deliveryContactName: repairMojibakeText(order?.deliveryContactName),
    deliveryContactPhone: order?.deliveryContactPhone || "",
    deliveryContactEmail: order?.deliveryContactEmail || "",
    buyerRemark: repairMojibakeText(order?.buyerRemark)
  };
};

const loadPurchaseOrderDetail = async () => {
  if (!purchaseOrderIdFromRoute.value) return;
  purchaseOrderDetailLoading.value = true;
  purchaseOrderDetailError.value = "";
  purchaseOrderDetailNotice.value = "";
  try {
    purchaseOrderDetail.value = purchaseOrderWorkspaceMode.value === "supplier" && !isBuyerSupplierOrderPreview.value
      ? await getSupplierPurchaseOrderDetail(purchaseOrderIdFromRoute.value)
      : await getPurchaseOrderDetail(purchaseOrderIdFromRoute.value);
    purchaseOrderSourceTrafficService.value = null;
    if (purchaseOrderDetail.value?.order.demandId && (!purchaseOrderDetail.value.order.supplyMode || !purchaseOrderDetail.value.order.fixedProviderType)) {
      try {
        const demandDetail = await getMaterialDemandDetail(purchaseOrderDetail.value.order.demandId);
        purchaseOrderSourceTrafficService.value = demandDetail.demand.trafficService || null;
      } catch {
        purchaseOrderSourceTrafficService.value = null;
      }
    }
    syncPurchaseOrderDetailForm();
    if (purchaseOrderWorkspaceMode.value === "buyer") {
      try {
        const [trafficResponse, settlements, attachments] = await Promise.all([
          listTrafficServiceOrders({ purchaseOrderId: purchaseOrderIdFromRoute.value, page: 1, size: 20 }),
          listSettlements("BUYER"),
          listPurchaseFulfillmentAttachments(purchaseOrderIdFromRoute.value)
        ]);
        purchaseTrafficServiceRows.value = trafficResponse.items;
        persistedSettlementRows.value = settlements.filter((row) => row.purchaseOrderId === Number(purchaseOrderIdFromRoute.value));
        fulfillmentAttachments.value = attachments;
      } catch {
        purchaseTrafficServiceRows.value = [];
        persistedSettlementRows.value = [];
        fulfillmentAttachments.value = [];
      }
    } else {
      try {
        const [trafficResponse, settlements, attachments] = await Promise.all([
          listTrafficServiceOrders({ purchaseOrderId: purchaseOrderIdFromRoute.value, page: 1, size: 20 }),
          listSettlements("SUPPLIER"),
          listPurchaseFulfillmentAttachments(purchaseOrderIdFromRoute.value)
        ]);
        purchaseTrafficServiceRows.value = trafficResponse.items;
        persistedSettlementRows.value = settlements.filter((row) => row.purchaseOrderId === Number(purchaseOrderIdFromRoute.value));
        fulfillmentAttachments.value = attachments;
      } catch {
        purchaseTrafficServiceRows.value = [];
        persistedSettlementRows.value = [];
        fulfillmentAttachments.value = [];
      }
    }
  } catch (error) {
    purchaseOrderDetail.value = null;
    purchaseOrderSourceTrafficService.value = null;
    purchaseTrafficServiceRows.value = [];
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.detailLoadFailed");
  } finally {
    purchaseOrderDetailLoading.value = false;
  }
};

const savePurchaseOrderDetailInfo = async () => {
  if (!purchaseOrderIdFromRoute.value || purchaseOrderWorkspaceMode.value !== "buyer") return;
  purchaseOrderDetailSaving.value = true;
  purchaseOrderDetailError.value = "";
  purchaseOrderDetailNotice.value = "";
  try {
    purchaseOrderDetail.value = await updatePurchaseOrderDeliveryInfo(purchaseOrderIdFromRoute.value, {
      supplyPort: purchaseOrderDetailForm.value.supplyPort,
      vesselEta: purchaseOrderDetailForm.value.vesselEta,
      requiredDeliveryTime: purchaseOrderDetailForm.value.requiredDeliveryTime,
      deliveryContactName: purchaseOrderDetailForm.value.deliveryContactName,
      deliveryContactPhone: purchaseOrderDetailForm.value.deliveryContactPhone,
      deliveryContactEmail: purchaseOrderDetailForm.value.deliveryContactEmail,
      buyerRemark: purchaseOrderDetailForm.value.buyerRemark
    });
    syncPurchaseOrderDetailForm();
    purchaseOrderDetailNotice.value = t("purchaseOrder.notice.deliveryInfoSaved");
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.saveDetailFailed");
  } finally {
    purchaseOrderDetailSaving.value = false;
  }
};

const remindPurchaseOrderDetailSuppliers = async () => {
  if (!purchaseOrderIdFromRoute.value || purchaseOrderWorkspaceMode.value !== "buyer") return;
  purchaseOrderDetailReminding.value = true;
  purchaseOrderDetailNotice.value = "";
  purchaseOrderDetailError.value = "";
  try {
    const response = await remindPurchaseOrderSuppliers(purchaseOrderIdFromRoute.value);
    await new Promise((resolve) => window.setTimeout(resolve, 1000));
    purchaseOrderDetailNotice.value = response.message || t("purchaseOrder.notice.remindSent");
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.remindFailed");
  } finally {
    purchaseOrderDetailReminding.value = false;
  }
};

const ensurePurchaseOrdersLoaded = () => {
  if (!isPurchaseOrdersPage.value) return;
  if (isPurchaseOrderDetailPage.value) {
    if (
      skipNextPurchaseOrderDetailReload.value &&
      Number(purchaseOrderDetail.value?.order.purchaseOrderId || 0) === Number(purchaseOrderIdFromRoute.value || 0)
    ) {
      skipNextPurchaseOrderDetailReload.value = false;
      return;
    }
    skipNextPurchaseOrderDetailReload.value = false;
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
  purchaseOrderCreatedFrom.value = "";
  purchaseOrderCreatedTo.value = "";
  if (purchaseOrderWorkspaceMode.value === "supplier") {
    await loadSupplierPurchaseOrders();
  } else {
    await loadPurchaseOrders();
  }
};

const seaAreaLabel = (value: string) => {
  if (String(value || "").toUpperCase() === "NORTH") return t("trafficService.seaArea.north");
  if (String(value || "").toUpperCase() === "SOUTH") return t("trafficService.seaArea.south");
  return value || "-";
};

const trafficFeeTypeLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "CUSTOMS") return t("trafficFeeType.customs");
  if (normalized === "CRANE") return t("trafficFeeType.crane");
  return t("trafficFeeType.freight");
};

const trafficServiceStatusLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "PENDING_CONFIRM") return t("trafficService.status.pendingConfirm");
  if (normalized === "WAITING_SERVICE" || normalized === "CONFIRMED") return t("trafficService.status.waitingService");
  if (normalized === "PLANNED") return t("trafficRoute.status.planned");
  if (["IN_PROGRESS", "IN_TRANSIT", "EXECUTING", "SUPPLYING", "IN_SERVICE"].includes(normalized)) return t("trafficRoute.status.inProgress");
  if (normalized === "COMPLETED") return t("trafficRoute.status.completed");
  if (normalized === "DISCARDED") return t("trafficService.status.discarded");
  return t("trafficService.status.active");
};

const trafficRequestStatusLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "DRAFT") return t("trafficMarketplace.status.draft");
  if (normalized === "PUBLISHED") return t("trafficMarketplace.status.published");
  if (normalized === "QUOTING") return t("trafficMarketplace.status.quoting");
  if (normalized === "AWARDED") return t("trafficMarketplace.status.awarded");
  if (normalized === "ORDER_CREATED") return t("trafficMarketplace.status.orderCreated");
  if (normalized === "CANCELLED") return t("trafficMarketplace.status.cancelled");
  return normalized || "-";
};

const trafficQuoteStatusLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "SUBMITTED" || normalized === "UPDATED") return t("trafficMarketplace.status.submitted");
  if (normalized === "SELECTED") return t("trafficMarketplace.status.selected");
  if (normalized === "REJECTED") return t("trafficMarketplace.status.rejected");
  if (normalized === "WITHDRAWN") return t("trafficMarketplace.status.withdrawn");
  return normalized || "-";
};

const trafficShuttleStatusLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "DRAFT") return t("trafficMarketplace.status.pendingPublish");
  if (normalized === "PUBLISHED") return "已发布";
  if (normalized === "FULL" || normalized === "BOOKING" || normalized === "RESERVED") return "预约中";
  if (["IN_PROGRESS", "IN_TRANSIT", "EXECUTING"].includes(normalized)) return "执行中";
  if (normalized === "CLOSED" || normalized === "CANCELLED") return t("trafficMarketplace.status.cancelled");
  if (normalized === "COMPLETED" || normalized === "FINISHED") return "已完成";
  return normalized || "-";
};

const trafficShuttleOperationalStatus = (row: TrafficShuttleService | Record<string, unknown>) => {
  const shuttle = row as TrafficShuttleService;
  const normalized = String(shuttle.status || "").toUpperCase();
  if (["COMPLETED", "FINISHED"].includes(normalized)) return { label: "已完成", variant: "success" as StatusVariant };
  if (["IN_PROGRESS", "IN_TRANSIT", "EXECUTING"].includes(normalized)) return { label: "执行中", variant: "info" as StatusVariant };
  if (["FULL", "BOOKING", "RESERVED"].includes(normalized) || (shuttle.bookings?.length ?? 0) > 0 || trafficShuttleBookedCount(row) > 0) {
    return { label: "预约中", variant: "warning" as StatusVariant };
  }
  if (normalized === "PUBLISHED") return { label: "已发布", variant: "info" as StatusVariant };
  if (normalized === "DRAFT") return { label: t("trafficMarketplace.status.pendingPublish"), variant: "neutral" as StatusVariant };
  if (["CLOSED", "CANCELLED"].includes(normalized)) return { label: t("trafficMarketplace.status.cancelled"), variant: "danger" as StatusVariant };
  return { label: trafficShuttleStatusLabel(normalized), variant: "neutral" as StatusVariant };
};

const trafficServiceStatusVariant = (value: string): StatusVariant => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "DISCARDED") return "danger";
  if (normalized === "PENDING_CONFIRM") return "warning";
  if (normalized === "WAITING_SERVICE" || normalized === "CONFIRMED") return "info";
  if (normalized === "PLANNED" || normalized === "IN_TRANSIT") return "info";
  return "success";
};

const trafficRouteStatusLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "READY") return t("trafficRoute.status.ready");
  if (normalized === "IN_PROGRESS") return t("trafficRoute.status.inProgress");
  if (normalized === "COMPLETED") return t("trafficRoute.status.completed");
  if (normalized === "DISCARDED") return t("trafficRoute.status.discarded");
  if (normalized === "PLANNED") return t("trafficRoute.status.planned");
  return t("trafficRoute.status.draft");
};

const trafficRouteStatusVariant = (value: string): StatusVariant => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "DISCARDED") return "danger";
  if (normalized === "COMPLETED") return "success";
  if (normalized === "READY" || normalized === "IN_PROGRESS") return "info";
  return "warning";
};

const trafficTypeLabel = (value: string) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "CARGO") return t("trafficService.cargoType.cargo");
  if (normalized === "SUPPLY") return t("trafficService.cargoType.supply");
  if (normalized === "PERSONNEL") return t("trafficService.serviceType.personnel");
  if (normalized === "GOODS") return t("trafficService.serviceType.goods");
  return value || "-";
};
const trafficShuttleRouteLabel = (row: TrafficShuttleService | Record<string, unknown>) => {
  const departure = String((row as TrafficShuttleService).departurePoint || "").trim();
  const destination = String((row as TrafficShuttleService).destinationPoint || (row as TrafficShuttleService).anchorageName || "").trim();
  return [departure, destination].filter(Boolean).join(" → ") || "-";
};
const trafficShuttleOriginLabel = (row: TrafficShuttleService | Record<string, unknown>) =>
  String((row as TrafficShuttleService).departurePoint || "").trim() || "-";
const trafficShuttleDestinationLabel = (row: TrafficShuttleService | Record<string, unknown>) =>
  String((row as TrafficShuttleService).destinationPoint || (row as TrafficShuttleService).anchorageName || "").trim() || "-";
const trafficShuttleServiceDateLabel = (row: TrafficShuttleService | Record<string, unknown>) =>
  formatDateTimeParts(String((row as TrafficShuttleService).startTime || "")).date;
const trafficShuttleStartTimeLabel = (row: TrafficShuttleService | Record<string, unknown>) =>
  formatDateTimeParts(String((row as TrafficShuttleService).startTime || "")).time || "-";
const trafficShuttleReturnTimeLabel = (row: TrafficShuttleService | Record<string, unknown>) =>
  formatDateTimeParts(String((row as TrafficShuttleService).returnTime || "")).time || "-";
const trafficShuttleNodeTimeLabel = (node: { startTime?: string | null; endTime?: string | null }) =>
  [node.startTime, node.endTime].filter(Boolean).join("-") || "--";
const trafficShuttleDateRange = () => {
  const date = trafficShuttleServiceDate.value.trim();
  return date ? { from: `${date} 00:00`, to: `${date} 23:59` } : { from: trafficShuttleStartTimeFrom.value, to: trafficShuttleStartTimeTo.value };
};
const trafficShuttleMoneyLabel = (value: unknown) => {
  const amount = Number(value);
  return Number.isFinite(amount) && amount > 0 ? formatPurchaseMoneyValue(amount) : "--";
};
const trafficShuttleConfiguredPrice = (row: TrafficShuttleService | Record<string, unknown>, type: "base" | "shared") => {
  const shuttle = row as TrafficShuttleService;
  const configured = trafficBoatRows.value.find(
    (item) =>
      item.anchorageCode === shuttle.anchorageCode &&
      (!shuttle.supplierCompanyId || !item.supplierCompanyId || Number(item.supplierCompanyId) === Number(shuttle.supplierCompanyId))
  );
  const directBasePrice = Number((shuttle as TrafficShuttleService & { basePrice?: number }).basePrice);
  const directSharedPrice = Number(shuttle.sharedPrice);
  const configuredBasePrice = Number(configured?.basePrice);
  const configuredSharedPrice = Number(configured?.sharedPrice);
  if (type === "base") {
    if (Number.isFinite(directBasePrice) && directBasePrice > 0) return directBasePrice;
    if (Number.isFinite(configuredBasePrice) && configuredBasePrice > 0) return configuredBasePrice;
    return Number.isFinite(directSharedPrice) && directSharedPrice > 0 ? directSharedPrice : 0;
  }
  if (Number.isFinite(directSharedPrice) && directSharedPrice > 0) return directSharedPrice;
  return Number.isFinite(configuredSharedPrice) && configuredSharedPrice > 0 ? configuredSharedPrice : 0;
};
const trafficShuttleBasePriceLabel = (row: TrafficShuttleService | Record<string, unknown>) => trafficShuttleMoneyLabel(trafficShuttleConfiguredPrice(row, "base"));
const trafficShuttleSharedPriceLabel = (row: TrafficShuttleService | Record<string, unknown>) => trafficShuttleMoneyLabel(trafficShuttleConfiguredPrice(row, "shared"));
const trafficShuttleFreightPrice = (row: TrafficShuttleService | Record<string, unknown>, index: number) =>
  trafficShuttleEffectiveAllowShare(row, index) ? trafficShuttleConfiguredPrice(row, "shared") : trafficShuttleConfiguredPrice(row, "base");
const trafficShuttleOtherFeeLabel = (row: TrafficShuttleService | Record<string, unknown>) => {
  const shuttle = row as TrafficShuttleService;
  const fees = [
    Number(shuttle.customsPrice || 0) > 0 ? `报关 ${formatPurchaseMoneyValue(Number(shuttle.customsPrice))}` : "",
    Number(shuttle.cranePrice || 0) > 0 ? `吊机 ${formatPurchaseMoneyValue(Number(shuttle.cranePrice))}` : ""
  ].filter(Boolean);
  return fees.length ? fees.join(" / ") : "--";
};
const trafficShuttleNodeRows = (row: TrafficShuttleService | Record<string, unknown>) => {
  const nodes = (row as TrafficShuttleService).serviceNodes || [];
  return nodes.length ? nodes : defaultTrafficShuttleNodes();
};
const trafficShuttleBookedCount = (row: TrafficShuttleService | Record<string, unknown>) =>
  Number((row as TrafficShuttleService).bookedPassengerCount || 0);
const trafficShuttleNodeEndDate = (row: TrafficShuttleService | Record<string, unknown>, node: { endTime?: string | null }) => {
  const baseDate = String((row as TrafficShuttleService).startTime || "").slice(0, 10);
  const endTime = String(node.endTime || "").trim();
  if (!baseDate || !endTime) return null;
  const parsed = new Date(`${baseDate}T${endTime.length === 5 ? `${endTime}:00` : endTime}`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};
const trafficShuttleNodeState = (row: TrafficShuttleService | Record<string, unknown>, node: { endTime?: string | null }) => {
  if (trafficShuttleBookedCount(row) > 0) return "joined";
  const endDate = trafficShuttleNodeEndDate(row, node);
  const routeDate = String((row as TrafficShuttleService).startTime || "").slice(0, 10);
  const today = formatDateKey(new Date());
  if (endDate && routeDate === today && endDate.getTime() < Date.now()) return "missed";
  return "available";
};
const trafficShuttleNodeVisualState = (row: TrafficShuttleService | Record<string, unknown>, node: { endTime?: string | null }, index: number) => {
  const key = trafficShuttleNodeKey(row, index);
  const localState = trafficShuttleNodeReservations.value[key];
  if (localState) return "joined";
  return trafficShuttleNodeState(row, node);
};
const trafficShuttleExecutionCardState = (row: TrafficShuttleService | Record<string, unknown>) => {
  const status = String((row as TrafficShuttleService).status || "").toUpperCase();
  if (["COMPLETED", "FINISHED"].includes(status)) return "completed";
  if (["IN_PROGRESS", "IN_TRANSIT", "EXECUTING"].includes(status)) return "executing";
  return "idle";
};
const trafficShuttleManagedNodeVisualState = (row: TrafficShuttleService | Record<string, unknown>, node: { endTime?: string | null }, index: number) => {
  const executionState = trafficShuttleExecutionCardState(row);
  const nodes = trafficShuttleNodeRows(row);
  if (executionState === "completed") return "done";
  if (executionState === "executing") {
    const activeIndex = Math.max(0, nodes.findIndex((item) => trafficShuttleNodeState(row, item) !== "missed"));
    if (index < activeIndex) return "done";
    if (index === activeIndex) return "active";
    return "todo";
  }
  return trafficShuttleNodeVisualState(row, node, index);
};
const trafficShuttleNodeShareStatus = (row: TrafficShuttleService | Record<string, unknown>, node: { endTime?: string | null }) => {
  const state = trafficShuttleNodeState(row, node);
  if (state === "joined") return t("trafficMarketplace.status.joinedShare");
  if (state === "missed") return t("trafficMarketplace.status.missedShare");
  return t("trafficMarketplace.status.availableShare");
};
const trafficShuttleNodeKey = (row: TrafficShuttleService | Record<string, unknown>, index: number) =>
  `${Number((row as TrafficShuttleService).shuttleId || 0)}-${index}`;
const trafficShuttleBookingNodeIndex = (row: TrafficShuttleService, booking: NonNullable<TrafficShuttleService["bookings"]>[number]) => {
  const direct = Number(booking.nodeIndex);
  const nodes = trafficShuttleNodeRows(row);
  if (Number.isInteger(direct) && direct >= 0 && direct < nodes.length) return direct;
  const bookingTime = String(booking.nodeTime || booking.remark || "").trim();
  if (bookingTime) {
    const matched = nodes.findIndex((node) => trafficShuttleNodeTimeLabel(node) === bookingTime);
    if (matched >= 0) return matched;
  }
  return 0;
};
const restoreTrafficShuttleReservations = (rows: TrafficShuttleService[]) => {
  const reservations: typeof trafficShuttleNodeReservations.value = {};
  const drafts: typeof trafficShuttleNodeDrafts.value = { ...trafficShuttleNodeDrafts.value };
  rows.forEach((row) => {
    (row.bookings || []).forEach((booking) => {
      const index = trafficShuttleBookingNodeIndex(row, booking);
      const key = trafficShuttleNodeKey(row, index);
      const anchoragePosition = booking.anchoragePosition || "";
      const positionParts = splitTrafficShuttleAnchoragePosition(anchoragePosition);
      const craneCount = Number(booking.craneCount || 0) > 0 ? Number(booking.craneCount) : 1;
      const reservation = {
        share: Boolean(booking.allowShare),
        customs: Boolean(booking.customsService),
        crane: Boolean(booking.craneService),
        craneCount,
        compareNo: booking.requestNo || "",
        vesselName: booking.vesselName || booking.cargoSummary || "",
        vesselImo: booking.vesselImo || "",
        anchorageTime: booking.anchorageTime || "",
        anchoragePosition,
        anchorageLongitude: positionParts.longitude,
        anchorageLatitude: positionParts.latitude,
        cargoWeight: booking.cargoWeightKg == null ? "" : String(booking.cargoWeightKg),
        cargoVolume: booking.cargoVolumeCbm == null ? "" : String(booking.cargoVolumeCbm),
        palletCount: booking.palletCount || "",
        freightFee: Number(booking.freightFee || 0),
        customsFee: Number(booking.customsFee || 0),
        craneFee: Number(booking.craneFee || 0)
      };
      reservations[key] = reservation;
      drafts[key] = {
        activeVesselSlot: 1,
        vesselSlotOne: {
          compareNo: reservation.compareNo,
          vesselName: reservation.vesselName,
          vesselImo: reservation.vesselImo,
          anchorageTime: reservation.anchorageTime,
          anchoragePosition: reservation.anchoragePosition,
          anchorageLongitude: reservation.anchorageLongitude,
          anchorageLatitude: reservation.anchorageLatitude,
          cargoWeight: reservation.cargoWeight,
          cargoVolume: reservation.cargoVolume,
          palletCount: reservation.palletCount
        },
        vesselSlotTwo: drafts[key]?.vesselSlotTwo || emptyTrafficShuttleVesselSlot(),
        compareNo: reservation.compareNo,
        vesselName: reservation.vesselName,
        vesselImo: reservation.vesselImo,
        anchorageTime: reservation.anchorageTime,
        anchoragePosition: reservation.anchoragePosition,
        anchorageLongitude: reservation.anchorageLongitude,
        anchorageLatitude: reservation.anchorageLatitude,
        cargoWeight: reservation.cargoWeight,
        cargoVolume: reservation.cargoVolume,
        palletCount: reservation.palletCount,
        allowShare: reservation.share,
        customsService: reservation.customs,
        craneService: reservation.crane,
        craneCount
      };
    });
  });
  trafficShuttleNodeReservations.value = reservations;
  trafficShuttleNodeDrafts.value = drafts;
};
const trafficShuttleNodeDisplayStatus = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const localState = trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)];
  if (localState?.share) return t("trafficMarketplace.status.availableShare");
  if (localState) return "已预约";
  return "";
};
const trafficShuttleNodeReservationTags = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const localState = trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)];
  if (!localState) return [];
  return [localState.share ? "拼船" : "", localState.customs ? "报关" : "", localState.crane ? "吊机" : ""].filter(Boolean);
};
const trafficShuttleRowReservationTags = (row: TrafficShuttleService | Record<string, unknown>) => {
  const shuttleId = Number((row as TrafficShuttleService).shuttleId || 0);
  const states = Object.entries(trafficShuttleNodeReservations.value)
    .filter(([key]) => key.startsWith(`${shuttleId}-`))
    .map(([, value]) => value);
  const tags = new Set<string>();
  states.forEach((state) => {
    if (state.share) tags.add("拼船");
    if (state.customs) tags.add("报关");
    if (state.crane) tags.add("吊机");
  });
  return Array.from(tags);
};
const trafficShuttleNodeDraft = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const key = trafficShuttleNodeKey(row, index);
  if (!trafficShuttleNodeDrafts.value[key]) {
    const vesselSlotOne = emptyTrafficShuttleVesselSlot();
    trafficShuttleNodeDrafts.value[key] = {
      activeVesselSlot: 1,
      vesselSlotOne,
      vesselSlotTwo: emptyTrafficShuttleVesselSlot(),
      compareNo: "",
      vesselName: "",
      vesselImo: "",
      anchorageTime: "",
      anchoragePosition: "",
      anchorageLongitude: "",
      anchorageLatitude: "",
      cargoWeight: "",
      cargoVolume: "",
      palletCount: "",
      allowShare: false,
      customsService: false,
      craneService: false,
      craneCount: 1
    };
  }
  return trafficShuttleNodeDrafts.value[key];
};
const trafficShuttleNodeSlotKey = (slot: 1 | 2) => (slot === 2 ? "vesselSlotTwo" : "vesselSlotOne");
const syncTrafficShuttleDraftFromSlot = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const draft = trafficShuttleNodeDraft(row, index);
  const slot = draft[trafficShuttleNodeSlotKey(draft.activeVesselSlot)];
  draft.compareNo = slot.compareNo;
  draft.vesselName = slot.vesselName;
  draft.vesselImo = slot.vesselImo;
  draft.anchorageTime = slot.anchorageTime;
  draft.anchoragePosition = slot.anchoragePosition;
  draft.anchorageLongitude = slot.anchorageLongitude;
  draft.anchorageLatitude = slot.anchorageLatitude;
  draft.cargoWeight = slot.cargoWeight;
  draft.cargoVolume = slot.cargoVolume;
  draft.palletCount = slot.palletCount;
};
const syncTrafficShuttleDraftToSlot = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const draft = trafficShuttleNodeDraft(row, index);
  const slot = draft[trafficShuttleNodeSlotKey(draft.activeVesselSlot)];
  slot.compareNo = draft.compareNo;
  slot.vesselName = draft.vesselName;
  slot.vesselImo = draft.vesselImo;
  slot.anchorageTime = draft.anchorageTime;
  slot.anchoragePosition = draft.anchoragePosition;
  slot.anchorageLongitude = draft.anchorageLongitude;
  slot.anchorageLatitude = draft.anchorageLatitude;
  slot.cargoWeight = draft.cargoWeight;
  slot.cargoVolume = draft.cargoVolume;
  slot.palletCount = draft.palletCount;
};
const updateTrafficShuttleAnchoragePosition = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const draft = trafficShuttleNodeDraft(row, index);
  draft.anchoragePosition = [draft.anchorageLongitude, draft.anchorageLatitude].filter(Boolean).join(" / ");
  syncTrafficShuttleDraftToSlot(row, index);
};
const setTrafficShuttleActiveVesselSlot = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2) => {
  const draft = trafficShuttleNodeDraft(row, index);
  draft.activeVesselSlot = slot;
  syncTrafficShuttleDraftFromSlot(row, index);
};
const trafficShuttleNodeVesselSlot = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2): TrafficShuttleVesselSlot => {
  const saved = slot === 1 ? trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)] : undefined;
  if (saved) {
    return {
      compareNo: saved.compareNo,
      vesselName: saved.vesselName,
      vesselImo: saved.vesselImo,
      anchorageTime: saved.anchorageTime,
      anchoragePosition: saved.anchoragePosition,
      anchorageLongitude: saved.anchorageLongitude,
      anchorageLatitude: saved.anchorageLatitude,
      cargoWeight: saved.cargoWeight,
      cargoVolume: saved.cargoVolume,
      palletCount: saved.palletCount
    };
  }
  const draft = trafficShuttleNodeDraft(row, index);
  return draft[trafficShuttleNodeSlotKey(slot)];
};
const trafficShuttleNodeVesselSlotHasData = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2) => {
  const data = trafficShuttleNodeVesselSlot(row, index, slot);
  return Boolean(data.compareNo || data.vesselName || data.vesselImo || data.anchorageTime || data.anchoragePosition || data.anchorageLongitude || data.anchorageLatitude || data.cargoWeight || data.cargoVolume || data.palletCount);
};
const trafficShuttleEffectiveAllowShare = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const saved = trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)];
  if (saved?.share) return true;
  return Boolean(trafficShuttleNodeDraft(row, index).allowShare && trafficShuttleNodeVesselSlotHasData(row, index, 2));
};
const trafficShuttleNodeVesselSlotLabel = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2) => {
  const data = trafficShuttleNodeVesselSlot(row, index, slot);
  return [data.vesselName || "--", data.vesselImo || "--"].join(" / ");
};
const trafficShuttleNodeVesselSlotValue = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2, key: "cargoWeight" | "cargoVolume" | "palletCount" | "anchorageTime" | "anchoragePosition") =>
  trafficShuttleNodeVesselSlot(row, index, slot)[key] || "--";
const splitTrafficShuttleAnchoragePosition = (value: string) => {
  const parts = String(value || "")
    .split(/[，,\/\s]+/)
    .map((part) => part.trim())
    .filter(Boolean);
  return {
    longitude: parts[0] || "",
    latitude: parts[1] || ""
  };
};
const trafficShuttleNodeVesselSlotPositionPart = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2, part: "longitude" | "latitude") => {
  const data = trafficShuttleNodeVesselSlot(row, index, slot);
  const split = splitTrafficShuttleAnchoragePosition(data.anchoragePosition || "");
  return (part === "longitude" ? data.anchorageLongitude || split.longitude : data.anchorageLatitude || split.latitude) || "--";
};
const trafficShuttleNodeVesselSlotFeeLabel = (row: TrafficShuttleService | Record<string, unknown>, index: number, slot: 1 | 2, key: "freightFee" | "customsFee" | "craneFee") => {
  if (!trafficShuttleNodeVesselSlotHasData(row, index, slot)) return "--";
  if (slot === 1 && trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)]) return trafficShuttleReservationFeeLabel(row, index, key);
  return trafficShuttleReservationFeeLabel(row, index, key);
};
const trafficShuttleNodeBookingVesselLabel = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const saved = trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)];
  const draft = trafficShuttleNodeDraft(row, index);
  return [saved?.vesselName || draft.vesselName || "--", saved?.vesselImo || draft.vesselImo || "--"].join(" / ");
};
const trafficShuttleNodeBookingValue = (row: TrafficShuttleService | Record<string, unknown>, index: number, key: "cargoWeight" | "cargoVolume" | "palletCount") =>
  trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)]?.[key] || trafficShuttleNodeDraft(row, index)[key] || "--";
const trafficShuttleNumberValue = (value: unknown) => {
  const text = String(value ?? "").replace(/,/g, "").trim();
  const matched = text.match(/-?\d+(\.\d+)?/);
  if (!matched) return undefined;
  const parsed = Number(matched[0]);
  return Number.isFinite(parsed) ? parsed : undefined;
};
const trafficShuttleCraneCount = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const draft = trafficShuttleNodeDraft(row, index);
  if (!draft.craneService) return 0;
  const count = Number(draft.craneCount || 1);
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 1;
};
const trafficShuttleReservationFees = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const saved = trafficShuttleNodeReservations.value[trafficShuttleNodeKey(row, index)];
  if (saved) {
    return {
      freightFee: saved.freightFee,
      customsFee: saved.customsFee,
      craneFee: saved.craneFee
    };
  }
  const draft = trafficShuttleNodeDraft(row, index);
  const shuttle = row as TrafficShuttleService;
  const freightFee = trafficShuttleFreightPrice(row, index);
  const customsFee = draft.customsService ? Number(shuttle.customsPrice || 0) : 0;
  const craneFee = draft.craneService ? Number(shuttle.cranePrice || 0) * trafficShuttleCraneCount(row, index) : 0;
  return { freightFee, customsFee, craneFee };
};
const trafficShuttleReservationFeeLabel = (row: TrafficShuttleService | Record<string, unknown>, index: number, key: "freightFee" | "customsFee" | "craneFee") => {
  const fees = trafficShuttleReservationFees(row, index);
  return trafficShuttleMoneyLabel(fees[key]);
};
const trafficShuttleFreightFeeTitle = (_row: TrafficShuttleService | Record<string, unknown>, _index: number) => "运费";
const materialDemandToTrafficInquiryOption = (demand: MaterialDemandSummary) => {
  const cargos = demand.trafficService?.cargos || [];
  const totalWeight = cargos.reduce((sum, cargo) => sum + Number(cargo.weightKg || 0), 0);
  const totalVolume = cargos.reduce((sum, cargo) => sum + Number(cargo.volumeCbm || 0), 0);
  const serviceParts = [demand.materialType, demand.trafficService?.serviceType ? trafficTypeLabel(demand.trafficService.serviceType) : "", demand.trafficService?.cargoType ? trafficTypeLabel(demand.trafficService.cargoType) : ""]
    .filter(Boolean)
    .join(" / ");
  return {
    demandId: Number(demand.demandId || 0),
    inquiryNo: demand.inquiryNo || demand.demandNo,
    status: "比价中",
    vesselName: demand.vesselName || "",
    vesselImo: "",
    anchorageTime: demand.trafficService?.useTime || demand.vesselEta || "",
    anchoragePosition: "",
    serviceContent: serviceParts || demand.sourceFileName || "",
    cargoWeight: totalWeight > 0 ? `${totalWeight} kg` : "",
    cargoVolume: totalVolume > 0 ? `${totalVolume} 方` : "",
    palletCount: ""
  };
};
const loadTrafficShuttleInquiryOptions = async () => {
  if (trafficShuttleInquiryLoading.value || trafficShuttleInquiryOptions.value.length) return;
  trafficShuttleInquiryLoading.value = true;
  try {
    const response = await listMaterialDemands({ status: "COMPARING", page: 1, size: 100 });
    trafficShuttleInquiryOptions.value = response.items.map(materialDemandToTrafficInquiryOption).filter((option) => option.inquiryNo);
  } catch {
    trafficShuttleInquiryOptions.value = [];
  } finally {
    trafficShuttleInquiryLoading.value = false;
  }
};
const openTrafficShuttleComparePicker = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  activeTrafficShuttleComparePickerKey.value = trafficShuttleNodeKey(row, index);
  void loadTrafficShuttleInquiryOptions();
};
const updateTrafficShuttleCompareKeyword = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const draft = trafficShuttleNodeDraft(row, index);
  draft[trafficShuttleNodeSlotKey(draft.activeVesselSlot)].compareNo = draft.compareNo;
  openTrafficShuttleComparePicker(row, index);
};
const trafficShuttleInquirySuggestions = (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const keyword = trafficShuttleNodeDraft(row, index).compareNo.trim().toLowerCase();
  return trafficShuttleInquiryOptions.value.filter((option) => {
    if (option.status !== "比价中") return false;
    if (!keyword) return true;
    return `${option.inquiryNo} ${option.vesselName} ${option.vesselImo}`.toLowerCase().includes(keyword);
  });
};
const selectTrafficShuttleInquiry = (row: TrafficShuttleService | Record<string, unknown>, index: number, option: (typeof trafficShuttleInquiryOptions.value)[number]) => {
  if (option.status !== "比价中") return;
  const draft = trafficShuttleNodeDraft(row, index);
  const slot = draft[trafficShuttleNodeSlotKey(draft.activeVesselSlot)];
  const anchoragePositionParts = splitTrafficShuttleAnchoragePosition(option.anchoragePosition);
  slot.compareNo = option.inquiryNo;
  slot.vesselName = option.vesselName;
  slot.vesselImo = option.vesselImo;
  slot.anchorageTime = option.anchorageTime;
  slot.anchoragePosition = option.anchoragePosition;
  slot.anchorageLongitude = anchoragePositionParts.longitude;
  slot.anchorageLatitude = anchoragePositionParts.latitude;
  slot.cargoWeight = option.cargoWeight;
  slot.cargoVolume = option.cargoVolume;
  slot.palletCount = option.palletCount;
  draft.compareNo = option.inquiryNo;
  draft.vesselName = option.vesselName;
  draft.vesselImo = option.vesselImo;
  draft.anchorageTime = option.anchorageTime;
  draft.anchoragePosition = option.anchoragePosition;
  draft.anchorageLongitude = anchoragePositionParts.longitude;
  draft.anchorageLatitude = anchoragePositionParts.latitude;
  draft.cargoWeight = option.cargoWeight;
  draft.cargoVolume = option.cargoVolume;
  draft.palletCount = option.palletCount;
  activeTrafficShuttleComparePickerKey.value = "";
};
const confirmTrafficShuttleNodeReservation = async (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const key = trafficShuttleNodeKey(row, index);
  const draft = trafficShuttleNodeDraft(row, index);
  const shuttle = row as TrafficShuttleService;
  const shuttleId = Number(shuttle.shuttleId || 0);
  if (!shuttleId || trafficShuttleBookingSavingKey.value) return;
  const craneCount = trafficShuttleCraneCount(row, index);
  const fees = trafficShuttleReservationFees(row, index);
  const fromCompareTrafficDialog = compareTrafficDialogOpen.value;
  trafficShuttleBookingSavingKey.value = key;
  try {
    const node = trafficShuttleNodeRows(row)[index] || {};
    const nodeTime = trafficShuttleNodeTimeLabel(node);
    const selectedDemand = trafficShuttleInquiryOptions.value.find((option) => option.inquiryNo === draft.compareNo.trim());
    const existingBooking = (shuttle.bookings || []).find((booking) => trafficShuttleBookingNodeIndex(shuttle, booking) === index && (draft.compareNo.trim() ? booking.requestNo === draft.compareNo.trim() : true));
    const booked = await bookTrafficShuttle(shuttleId, {
      bookingId: existingBooking?.bookingId || undefined,
      demandId: Number(compareDemandId.value || selectedDemand?.demandId || 0) || undefined,
      purchaseOrderId: Number(purchaseOrderIdFromRoute.value || 0) || undefined,
      requestNo: draft.compareNo.trim(),
      nodeIndex: index,
      nodeName: node.nodeName || "",
      nodeTime,
      vesselName: draft.vesselName,
      vesselImo: draft.vesselImo,
      anchorageTime: draft.anchorageTime,
      anchoragePosition: draft.anchoragePosition,
      palletCount: draft.palletCount,
      allowShare: draft.allowShare,
      customsService: draft.customsService,
      craneService: draft.craneService,
      craneCount,
      cargoSummary: draft.vesselName || draft.compareNo.trim(),
      cargoWeightKg: trafficShuttleNumberValue(draft.cargoWeight),
      cargoVolumeCbm: trafficShuttleNumberValue(draft.cargoVolume),
      remark: nodeTime
    });
    trafficShuttleNodeReservations.value = {
      ...trafficShuttleNodeReservations.value,
      [key]: {
        share: draft.allowShare,
        customs: draft.customsService,
        crane: draft.craneService,
        craneCount,
        compareNo: draft.compareNo,
        vesselName: draft.vesselName,
        vesselImo: draft.vesselImo,
        anchorageTime: draft.anchorageTime,
        anchoragePosition: draft.anchoragePosition,
        anchorageLongitude: draft.anchorageLongitude,
        anchorageLatitude: draft.anchorageLatitude,
        cargoWeight: draft.cargoWeight,
        cargoVolume: draft.cargoVolume,
        palletCount: draft.palletCount,
        freightFee: fees.freightFee,
        customsFee: fees.customsFee,
        craneFee: fees.craneFee
      }
    };
    if (fromCompareTrafficDialog) {
      compareTrafficServiceForm.value.trafficServiceOrderId = booked.trafficServiceOrderId;
      compareTrafficServiceForm.value.bookingId = booked.bookingId;
      compareTrafficServiceForm.value.shuttleNo = shuttle.shuttleNo || "";
      compareTrafficServiceForm.value.trafficVesselName = shuttle.trafficVesselName || "";
      compareTrafficServiceForm.value.departurePoint = shuttle.departurePoint || "";
      compareTrafficServiceForm.value.destinationPoint = shuttle.destinationPoint || shuttle.anchorageName || "";
      compareTrafficServiceForm.value.returnTime = shuttle.returnTime || "";
      compareTrafficServiceForm.value.serviceNodes = shuttle.serviceNodes || [];
      compareTrafficServiceForm.value.seaArea = shuttle.seaArea || compareTrafficServiceForm.value.seaArea;
      compareTrafficServiceForm.value.anchorageCode = shuttle.anchorageCode || compareTrafficServiceForm.value.anchorageCode;
      compareTrafficServiceForm.value.anchorageName = shuttle.anchorageName || selectedCompareTrafficAnchorageName();
      compareTrafficServiceForm.value.useTime = shuttle.startTime || compareTrafficServiceForm.value.useTime;
      compareTrafficServiceForm.value.allowShare = draft.allowShare;
      compareFixedFeeInputs.value.shuttle = [shuttle.shuttleNo, shuttle.trafficVesselName].filter(Boolean).join(" / ");
      compareFixedProvider.value = {
        type: "BARGE",
        id: String(shuttle.shuttleId || ""),
        name: compareFixedFeeInputs.value.shuttle
      };
      compareFixedFeeInputs.value.freight = normalizeCompareFeeDisplayInput(fees.freightFee);
      compareFixedFeeInputs.value.customs = normalizeCompareFeeDisplayInput(fees.customsFee);
      compareFixedFeeInputs.value.crane = normalizeCompareFeeDisplayInput(fees.craneFee);
      compareTrafficDialogOpen.value = false;
    }
    activeTrafficShuttleNodeKey.value = "";
    activeTrafficShuttleComparePickerKey.value = "";
    if (!fromCompareTrafficDialog) await loadTrafficShuttles(false);
  } finally {
    trafficShuttleBookingSavingKey.value = "";
  }
};
const trafficShuttleBookedCountLabel = (row: TrafficShuttleService | Record<string, unknown>) =>
  `${trafficShuttleBookedCount(row)}${t("trafficMarketplace.unit.person")}`;

const loadTrafficAnchorages = async () => {
  if (trafficAnchorages.value.length) return;
  trafficAnchorages.value = await listTrafficAnchorages();
};

const selectedCompareTrafficAnchorageName = () => {
  const selected = trafficAnchorages.value.find((item) => item.anchorageCode === compareTrafficServiceForm.value.anchorageCode);
  return selected?.anchorageName || compareTrafficServiceForm.value.anchorageName || compareTrafficServiceForm.value.anchorageCode || "";
};

const compareTrafficUseTimeRange = () => {
  const useTime = String(compareTrafficServiceForm.value.useTime || "").trim();
  const datePart = useTime.slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) return {};
  return {
    startTimeFrom: `${datePart} 00:00`,
    startTimeTo: `${datePart} 23:59`
  };
};

const applyCompareTrafficService = (value?: MaterialDemandTrafficService) => {
  compareTrafficServiceForm.value = {
    trafficServiceOrderId: value?.trafficServiceOrderId,
    bookingId: value?.bookingId,
    shuttleNo: value?.shuttleNo || "",
    trafficVesselName: value?.trafficVesselName || "",
    departurePoint: value?.departurePoint || "",
    destinationPoint: value?.destinationPoint || value?.anchorageName || "",
    startTime: value?.startTime || value?.useTime || "",
    returnTime: value?.returnTime || "",
    serviceNodes: value?.serviceNodes || [],
    seaArea: value?.seaArea || "NORTH",
    anchorageCode: value?.anchorageCode || "",
    anchorageName: value?.anchorageName || "",
    useTime: value?.useTime || "",
    serviceType: value?.serviceType || "GOODS",
    passengerType: value?.passengerType || "NORMAL",
    passengerCount: value?.passengerCount,
    cargoType: value?.cargoType || "CARGO",
    returnTrip: Boolean(value?.returnTrip),
    allowShare: Boolean(value?.allowShare),
    remark: value?.remark || "",
    cargos: []
  };
};

const loadCompareTrafficShuttles = async () => {
  compareTrafficShuttleLoading.value = true;
  compareTrafficError.value = "";
  try {
    const response = await listTrafficShuttles({
      status: "PUBLISHED",
      anchorageCode: compareTrafficServiceForm.value.anchorageCode || "",
      ...compareTrafficUseTimeRange(),
      page: 1,
      size: 50
    });
    const rows = sortNewestFirst(response.items, ["startTime", "createdAt"], ["shuttleId", "shuttleServiceId"]);
    compareTrafficShuttleRows.value = rows;
    restoreTrafficShuttleReservations(rows);
  } catch (error) {
    compareTrafficShuttleRows.value = [];
    compareTrafficError.value = error instanceof Error && error.message ? error.message : t("trafficMarketplace.error.loadFailed");
  } finally {
    compareTrafficShuttleLoading.value = false;
  }
};

const normalizeCompareTrafficServicePayload = (): MaterialDemandTrafficService | undefined => {
  const anchorageCode = String(compareTrafficServiceForm.value.anchorageCode || "").trim();
  const isLand = compareSupplyForm.value.supplyMode === "LAND";
  const providerName = normalizeCompareText(compareFixedFeeInputs.value.shuttle) || compareFixedProvider.value.name;
  return {
    trafficServiceOrderId: compareTrafficServiceForm.value.trafficServiceOrderId,
    bookingId: compareTrafficServiceForm.value.bookingId,
    supplyMode: isLand ? "LAND" : "SEA",
    fixedProviderType: isLand ? "SUPPLIER" : "BARGE",
    fixedProviderId: compareFixedProvider.value.id || "",
    fixedProviderName: providerName,
    shuttleNo: compareTrafficServiceForm.value.shuttleNo || "",
    trafficVesselName: compareTrafficServiceForm.value.trafficVesselName || "",
    departurePoint: compareTrafficServiceForm.value.departurePoint || "",
    destinationPoint: compareTrafficServiceForm.value.destinationPoint || "",
    startTime: compareTrafficServiceForm.value.startTime || compareTrafficServiceForm.value.useTime || "",
    returnTime: compareTrafficServiceForm.value.returnTime || "",
    serviceNodes: compareTrafficServiceForm.value.serviceNodes || [],
    seaArea: compareTrafficServiceForm.value.seaArea || "NORTH",
    anchorageCode,
    anchorageName: selectedCompareTrafficAnchorageName(),
    useTime: compareTrafficServiceForm.value.useTime || "",
    serviceType: compareTrafficServiceForm.value.serviceType || "GOODS",
    passengerType: compareTrafficServiceForm.value.passengerType || "NORMAL",
    passengerCount: compareTrafficServiceForm.value.passengerCount,
    cargoType: compareTrafficServiceForm.value.cargoType || "CARGO",
    returnTrip: Boolean(compareTrafficServiceForm.value.returnTrip),
    allowShare: Boolean(compareTrafficServiceForm.value.allowShare),
    remark: compareTrafficServiceForm.value.remark || "",
    cargos: []
  };
};

const openCompareTrafficDialog = async () => {
  if (isCompareReadonly.value) return;
  compareTrafficError.value = "";
  try {
    await loadTrafficAnchorages();
    activeTrafficShuttleNodeKey.value = "";
    activeTrafficShuttleComparePickerKey.value = "";
    compareTrafficDialogOpen.value = true;
    await loadCompareTrafficShuttles();
  } catch {
    compareTrafficError.value = t("trafficService.error.loadFailed");
  }
};

const loadTrafficServices = async () => {
  trafficServiceLoading.value = true;
  trafficServiceErrorKey.value = "";
  try {
    await loadTrafficAnchorages();
    const response = await listTrafficServiceOrders({
      keyword: trafficServiceKeyword.value,
      status: trafficServiceStatus.value,
      seaArea: trafficServiceSeaArea.value,
      page: 1,
      size: 50
    });
    trafficServiceRows.value = response.items;
    if (isTrafficServiceDetailPage.value) {
      const target = trafficServiceRows.value.find((row) => row.serviceOrderId === trafficServiceDetailId.value);
      if (target) setTrafficServiceForm(target);
    }
  } catch {
    trafficServiceRows.value = [];
    trafficServiceErrorKey.value = "trafficService.error.loadFailed";
  } finally {
    trafficServiceLoading.value = false;
  }
};

const loadTrafficRequests = async () => {
  trafficRequestLoading.value = true;
  trafficServiceErrorKey.value = "";
  try {
    await loadTrafficAnchorages();
    const response = pageKey.value === "trafficBoat"
      ? await listSupplierTrafficServiceRequests({
          keyword: trafficRequestKeyword.value,
          status: trafficRequestStatus.value,
          seaArea: trafficRequestSeaArea.value,
          page: 1,
          size: 50
        })
      : await listTrafficServiceRequests({
          keyword: trafficRequestKeyword.value,
          status: trafficRequestStatus.value,
          seaArea: trafficRequestSeaArea.value,
          page: 1,
          size: 50
        });
    trafficRequestRows.value = response.items;
    if (!trafficRequestDetail.value && response.items[0]?.requestId) {
      trafficRequestDetail.value = await getTrafficServiceRequest(response.items[0].requestId);
    }
  } catch {
    trafficRequestRows.value = [];
    trafficServiceErrorKey.value = "trafficMarketplace.error.loadFailed";
  } finally {
    trafficRequestLoading.value = false;
  }
};

const loadTrafficShuttles = async (supplierOnly = shopManagementTab.value === "trafficService") => {
  trafficShuttleLoading.value = true;
  trafficServiceErrorKey.value = "";
  try {
    await loadTrafficAnchorages();
    if (!trafficBoatRows.value.length && !trafficBoatLoading.value) {
      try {
        const priceRows = await listTrafficBoatPrices({});
        trafficBoatRows.value = priceRows.map(normalizeTrafficBoatRow);
      } catch {
        trafficBoatRows.value = [];
      }
    }
    const shuttleDateRange = trafficShuttleDateRange();
    const response = supplierOnly
      ? await listSupplierTrafficShuttles({
          keyword: trafficShuttleKeyword.value,
          status: trafficShuttleStatus.value,
          seaArea: trafficShuttleSeaArea.value,
          anchorageCode: trafficShuttleAnchorageCode.value,
          startTimeFrom: shuttleDateRange.from,
          startTimeTo: shuttleDateRange.to,
          page: 1,
          size: 50
        })
      : await listTrafficShuttles({
          keyword: trafficShuttleKeyword.value,
          status: trafficShuttleStatus.value,
          seaArea: trafficShuttleSeaArea.value,
          anchorageCode: trafficShuttleAnchorageCode.value,
          startTimeFrom: shuttleDateRange.from,
          startTimeTo: shuttleDateRange.to,
          page: 1,
          size: 50
        });
    trafficShuttleRows.value = response.items;
    restoreTrafficShuttleReservations(response.items);
  } catch {
    trafficShuttleRows.value = [];
    trafficServiceErrorKey.value = "trafficMarketplace.error.loadFailed";
  } finally {
    trafficShuttleLoading.value = false;
  }
};

const resetTrafficRequestSearch = async () => {
  trafficRequestKeyword.value = "";
  trafficRequestStatus.value = "";
  trafficRequestSeaArea.value = "";
  await loadTrafficRequests();
};

const openTrafficRequestDialog = () => {
  trafficRequestForm.value = {
    seaArea: "NORTH",
    anchorageCode: "",
    useTime: "",
    serviceType: "GOODS",
    passengerType: "NORMAL",
    cargoType: "CARGO",
    returnTrip: false,
    allowShare: true,
    remark: "",
    publish: true,
    cargos: []
  };
  trafficRequestDialogOpen.value = true;
};

const saveTrafficRequest = async () => {
  trafficRequestSaving.value = true;
  trafficServiceErrorKey.value = "";
  try {
    await createTrafficServiceRequest(trafficRequestForm.value);
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.requestCreated";
    trafficRequestDialogOpen.value = false;
    await loadTrafficRequests();
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.saveFailed";
  } finally {
    trafficRequestSaving.value = false;
  }
};

const openTrafficRequestDetail = async (row: TrafficServiceRequest | Record<string, unknown>) => {
  const id = Number((row as TrafficServiceRequest).requestId);
  if (!id) return;
  trafficRequestLoading.value = true;
  try {
    trafficRequestDetail.value = await getTrafficServiceRequest(id);
  } finally {
    trafficRequestLoading.value = false;
  }
};

const selectTrafficQuote = async (row: Record<string, unknown>) => {
  const requestId = selectedTrafficRequest.value?.requestId;
  const quoteId = Number(row.quoteId);
  if (!requestId || !quoteId) return;
  trafficRequestSaving.value = true;
  try {
    trafficRequestDetail.value = await selectTrafficServiceQuote(requestId, quoteId);
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.quoteSelected";
    await loadTrafficRequests();
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.selectFailed";
  } finally {
    trafficRequestSaving.value = false;
  }
};

const cancelTrafficRequest = async (row: TrafficServiceRequest | Record<string, unknown>) => {
  const requestId = Number((row as TrafficServiceRequest).requestId);
  if (!requestId) return;
  trafficRequestSaving.value = true;
  trafficServiceErrorKey.value = "";
  try {
    await cancelTrafficServiceRequest(requestId);
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.requestCancelled";
    trafficRequestDetail.value = null;
    await loadTrafficRequests();
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.cancelFailed";
  } finally {
    trafficRequestSaving.value = false;
  }
};

const openTrafficQuoteDialog = (row: TrafficServiceRequest | Record<string, unknown>) => {
  trafficQuoteRequestId.value = Number((row as TrafficServiceRequest).requestId);
  trafficQuoteForm.value = {
    quoteAmount: undefined,
    currency: "CNY",
    availableStartTime: (row as TrafficServiceRequest).useTime || "",
    availableReturnTime: "",
    trafficVesselId: undefined,
    trafficVesselName: "",
    contactName: "",
    contactPhone: "",
    message: ""
  };
  trafficQuoteDialogOpen.value = true;
};

const saveTrafficQuote = async () => {
  if (!trafficQuoteRequestId.value) return;
  trafficRequestSaving.value = true;
  trafficServiceErrorKey.value = "";
  try {
    await submitTrafficServiceQuote(trafficQuoteRequestId.value, trafficQuoteForm.value);
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.quoteSaved";
    trafficQuoteDialogOpen.value = false;
    await loadTrafficRequests();
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.quoteFailed";
  } finally {
    trafficRequestSaving.value = false;
  }
};

const withdrawQuote = async (row: Record<string, unknown>) => {
  const quoteId = Number(row.quoteId);
  if (!quoteId) return;
  trafficRequestSaving.value = true;
  try {
    await withdrawTrafficServiceQuote(quoteId);
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.quoteWithdrawn";
    if (trafficQuoteRequestId.value) trafficRequestDetail.value = await getTrafficServiceRequest(trafficQuoteRequestId.value);
    await loadTrafficRequests();
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.quoteFailed";
  } finally {
    trafficRequestSaving.value = false;
  }
};

const openTrafficShuttleDialog = async () => {
  if (!companyVesselRows.value.length && !companyVesselLoading.value) void loadCompanyVessels();
  if (!companyValueAddedServiceForm.value.customsPrice && !companyValueAddedServiceForm.value.cranePrice && !companyValueAddedServiceLoading.value) {
    try {
      await loadCompanyValueAddedServices();
    } catch {
      // The shuttle can still be drafted if enterprise value-added fees are unavailable.
    }
  }
  editingTrafficShuttleId.value = null;
  trafficShuttleForm.value = {
    seaArea: "NORTH",
    anchorageCode: "",
    departurePoint: defaultTrafficShuttleDeparturePoint("NORTH"),
    destinationPoint: "",
    startTime: "",
    returnTime: "",
    basePrice: undefined,
    sharedPrice: undefined,
    customsPrice: parseCompareNonNegative(companyValueAddedServiceForm.value.customsPrice),
    cranePrice: parseCompareNonNegative(companyValueAddedServiceForm.value.cranePrice),
    passengerCapacity: undefined,
    cargoCapacityKg: undefined,
    cargoCapacityCbm: undefined,
    trafficVesselId: undefined,
    trafficVesselName: "",
    status: "DRAFT",
    remark: "",
    serviceNodes: defaultTrafficShuttleNodes()
  };
  trafficShuttleDialogOpen.value = true;
};

const openTrafficShuttleEditDialog = (row: TrafficShuttleService | Record<string, unknown>) => {
  if (!companyVesselRows.value.length && !companyVesselLoading.value) void loadCompanyVessels();
  const shuttle = row as TrafficShuttleService;
  const shuttleId = Number(shuttle.shuttleId);
  if (!shuttleId) return;
  editingTrafficShuttleId.value = shuttleId;
  trafficShuttleForm.value = {
    seaArea: shuttle.seaArea || "NORTH",
    anchorageCode: shuttle.anchorageCode || "",
    departurePoint: shuttle.departurePoint || "",
    destinationPoint: shuttle.destinationPoint || "",
    startTime: shuttle.startTime || "",
    returnTime: shuttle.returnTime || "",
    basePrice: shuttle.basePrice ?? shuttle.sharedPrice,
    sharedPrice: shuttle.sharedPrice,
    customsPrice: shuttle.customsPrice,
    cranePrice: shuttle.cranePrice,
    passengerCapacity: shuttle.passengerCapacity,
    cargoCapacityKg: shuttle.cargoCapacityKg,
    cargoCapacityCbm: shuttle.cargoCapacityCbm,
    trafficVesselId: shuttle.trafficVesselId,
    trafficVesselName: shuttle.trafficVesselName || "",
    status: shuttle.status || "DRAFT",
    remark: shuttle.remark || "",
    serviceNodes: shuttle.serviceNodes?.length ? shuttle.serviceNodes.map((node) => ({ ...node })) : defaultTrafficShuttleNodes()
  };
  trafficShuttleDialogOpen.value = true;
};

const resetTrafficShuttleSearch = async (supplierOnly = shopManagementTab.value === "trafficService") => {
  trafficShuttleKeyword.value = "";
  trafficShuttleStatus.value = "";
  trafficShuttleSeaArea.value = "";
  trafficShuttleAnchorageCode.value = "";
  trafficShuttleServiceDate.value = "";
  trafficShuttleStartTimeFrom.value = "";
  trafficShuttleStartTimeTo.value = "";
  await loadTrafficShuttles(supplierOnly);
};

const addTrafficShuttleNode = () => {
  const nodes = trafficShuttleForm.value.serviceNodes || [];
  trafficShuttleForm.value.serviceNodes = [
    ...nodes,
    {
      nodeName: `节点${nodes.length + 1}`,
      startTime: "",
      endTime: ""
    }
  ];
};

const removeTrafficShuttleNode = (index: number) => {
  trafficShuttleForm.value.serviceNodes = (trafficShuttleForm.value.serviceNodes || []).filter((_, itemIndex) => itemIndex !== index);
};

const normalizeTrafficShuttlePriceInput = (value: unknown) => {
  const amount = Number(value);
  return Number.isFinite(amount) && amount >= 0 ? amount : undefined;
};

const trafficShuttleErrorText = (error: unknown, fallbackKey = "trafficMarketplace.error.shuttleFailed") => {
  const message = error instanceof ApiError ? error.message.trim() : "";
  const messageMap: Record<string, string> = {
    SEA_AREA_REQUIRED: "请选择海域。",
    ANCHORAGE_REQUIRED: "请选择目的锚地。",
    TRAFFIC_SHUTTLE_DEPARTURE_REQUIRED: "请填写出发点。",
    TRAFFIC_SHUTTLE_START_TIME_REQUIRED: "请选择启动时间。",
    AUTH_TOKEN_MISSING: "登录状态已失效，请重新登录后再保存。",
    COMPANY_NOT_ACTIVE: "当前企业状态不可操作，请确认账号已完成入驻审核。"
  };
  if (message) return messageMap[message] || message;
  return fallbackKey;
};

const validateTrafficShuttleForm = () => {
  const form = trafficShuttleForm.value;
  if (!String(form.seaArea || "").trim()) return "请选择海域。";
  if (!String(form.anchorageCode || "").trim()) return "请选择目的锚地。";
  if (!String(form.departurePoint || "").trim()) return "请填写出发点。";
  if (!String(form.startTime || "").trim()) return "请选择启动时间。";
  return "";
};

const saveTrafficShuttle = async (status?: "DRAFT" | "PUBLISHED") => {
  trafficShuttleSaving.value = true;
  trafficServiceErrorKey.value = "";
  try {
    const validationError = validateTrafficShuttleForm();
    if (validationError) {
      trafficServiceErrorKey.value = validationError;
      return;
    }
    const nextStatus = status || trafficShuttleForm.value.status || "DRAFT";
    const payload: TrafficShuttlePayload = {
      ...trafficShuttleForm.value,
      seaArea: String(trafficShuttleForm.value.seaArea || "").trim(),
      anchorageCode: String(trafficShuttleForm.value.anchorageCode || "").trim(),
      departurePoint: String(trafficShuttleForm.value.departurePoint || "").trim(),
      destinationPoint: String(trafficShuttleForm.value.destinationPoint || "").trim(),
      startTime: String(trafficShuttleForm.value.startTime || "").trim(),
      returnTime: String(trafficShuttleForm.value.returnTime || "").trim(),
      basePrice: normalizeTrafficShuttlePriceInput(trafficShuttleForm.value.basePrice),
      sharedPrice: normalizeTrafficShuttlePriceInput(trafficShuttleForm.value.sharedPrice),
      customsPrice: normalizeTrafficShuttlePriceInput(trafficShuttleForm.value.customsPrice),
      cranePrice: normalizeTrafficShuttlePriceInput(trafficShuttleForm.value.cranePrice),
      status: nextStatus
    };
    if (editingTrafficShuttleId.value) {
      await updateTrafficShuttle(editingTrafficShuttleId.value, payload);
    } else {
      await createTrafficShuttle(payload);
    }
    trafficServiceNoticeKey.value = nextStatus === "PUBLISHED" ? "trafficMarketplace.notice.shuttlePublished" : "trafficMarketplace.notice.shuttleSaved";
    editingTrafficShuttleId.value = null;
    trafficShuttleDialogOpen.value = false;
    await loadTrafficShuttles(true);
  } catch (error) {
    trafficServiceErrorKey.value = trafficShuttleErrorText(error);
  } finally {
    trafficShuttleSaving.value = false;
  }
};

const closeShuttle = async (row: TrafficShuttleService | Record<string, unknown>) => {
  const id = trafficShuttleOperationId(row);
  if (!id) return;
  trafficShuttleSaving.value = true;
  try {
    await closeTrafficShuttle(id);
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.shuttleClosed";
    await loadTrafficShuttles(true);
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.shuttleFailed";
  } finally {
    trafficShuttleSaving.value = false;
  }
};

const trafficShuttleOperationId = (row: TrafficShuttleService | Record<string, unknown>) => {
  const source = row as TrafficShuttleService & { shuttleServiceId?: number };
  const shuttleServiceId = Number(source.shuttleServiceId || 0);
  if (shuttleServiceId > 0) return shuttleServiceId;
  const shuttleId = Number(source.shuttleId || 0);
  return shuttleId > 0 ? shuttleId : 0;
};

const executeTrafficShuttle = async (row: TrafficShuttleService | Record<string, unknown>) => {
  const id = trafficShuttleOperationId(row);
  if (!id) {
    trafficServiceErrorKey.value = "未找到可执行的驳船班次，请刷新后重试。";
    return;
  }
  trafficShuttleSaving.value = true;
  try {
    await startTrafficShuttle(id);
    await loadTrafficShuttles(true);
  } catch (error) {
    trafficServiceErrorKey.value = error instanceof ApiError ? error.message : "trafficMarketplace.error.shuttleFailed";
  } finally {
    trafficShuttleSaving.value = false;
  }
};

const completeTrafficShuttleAction = async (row: TrafficShuttleService | Record<string, unknown>) => {
  const id = trafficShuttleOperationId(row);
  if (!id) {
    trafficServiceErrorKey.value = "未找到可完成的驳船班次，请刷新后重试。";
    return;
  }
  trafficShuttleSaving.value = true;
  try {
    await completeTrafficShuttle(id);
    await loadTrafficShuttles(true);
  } catch (error) {
    trafficServiceErrorKey.value = error instanceof ApiError && String(error.payload || "").includes("ATTACHMENT_REQUIRED") ? "完成前必须上传履约附件。" : "trafficMarketplace.error.shuttleFailed";
  } finally {
    trafficShuttleSaving.value = false;
  }
};

const openManagedShuttleNode = async (row: TrafficShuttleService | Record<string, unknown>, index: number) => {
  const shuttle = row as TrafficShuttleService;
  managedShuttleReadonly.value = false;
  managedShuttleNodeContext.value = { row: shuttle, index };
  const firstBooking = (shuttle.bookings || []).find((booking) => trafficShuttleBookingNodeIndex(shuttle, booking) === index);
  managedShuttleActiveBookingId.value = Number(firstBooking?.bookingId || 0);
  fulfillmentDrawerOpen.value = true;
  fulfillmentDrawerTitle.value = `${shuttle.trafficVesselName || "驳船"} · ${trafficShuttleNodeTimeLabel(trafficShuttleNodeRows(shuttle)[index])}`;
  try {
    const items = await listBargeShuttleAttachments(shuttle.shuttleId);
    fulfillmentDrawerItems.value = items.filter((item) => item.nodeIndex === index);
  } catch {
    fulfillmentDrawerItems.value = [];
  }
};

const openPurchaseBargeExecutionDrawer = () => {
  const shuttle = purchaseDetailBargeReadonlyShuttle.value;
  if (!shuttle) return;
  const nodeIndex = Math.max(0, Number(shuttle.bookings?.[0]?.nodeIndex ?? 0));
  managedShuttleReadonly.value = true;
  managedShuttleNodeContext.value = { row: shuttle, index: nodeIndex };
  managedShuttleActiveBookingId.value = Number(shuttle.bookings?.[0]?.bookingId || 0);
  fulfillmentDrawerOpen.value = true;
  fulfillmentDrawerTitle.value = `${shuttle.trafficVesselName || "驳船"} · ${trafficShuttleNodeTimeLabel(trafficShuttleNodeRows(shuttle)[nodeIndex] || {})}`;
  fulfillmentDrawerItems.value = purchaseDetailBargeExecutionCard.value?.attachments || [];
};

const managedShuttleNodeBookings = computed<TrafficShuttleBooking[]>(() => {
  const context = managedShuttleNodeContext.value;
  if (!context) return [];
  return (context.row.bookings || []).filter((booking) => trafficShuttleBookingNodeIndex(context.row, booking) === context.index);
});

const managedShuttleActiveBooking = computed(() =>
  managedShuttleNodeBookings.value.find((booking) => Number(booking.bookingId) === managedShuttleActiveBookingId.value)
    || managedShuttleNodeBookings.value[0]
    || null
);

const managedShuttleExecutionDraft = (booking: TrafficShuttleBooking) => {
  const id = Number(booking.bookingId);
  if (!managedShuttleExecutionDrafts.value[id]) {
    const position = splitTrafficShuttleAnchoragePosition(booking.anchoragePosition || "");
    managedShuttleExecutionDrafts.value[id] = {
      cargoWeight: booking.cargoWeightKg == null ? "" : String(booking.cargoWeightKg),
      cargoVolume: booking.cargoVolumeCbm == null ? "" : String(booking.cargoVolumeCbm),
      palletCount: booking.palletCount || "",
      anchorageLongitude: position.longitude,
      anchorageLatitude: position.latitude,
      craneCount: Math.max(1, Number(booking.craneCount || 1))
    };
  }
  return managedShuttleExecutionDrafts.value[id];
};

const managedShuttleActiveAttachments = computed(() => {
  const booking = managedShuttleActiveBooking.value;
  if (!booking) return fulfillmentDrawerItems.value;
  const bookingId = Number(booking.bookingId || 0);
  const trafficServiceOrderId = Number(booking.trafficServiceOrderId || 0);
  return fulfillmentDrawerItems.value.filter((item) => {
    if (bookingId && item.bookingId != null) return Number(item.bookingId || 0) === bookingId;
    if (trafficServiceOrderId && item.trafficServiceOrderId != null) return Number(item.trafficServiceOrderId || 0) === trafficServiceOrderId;
    return !bookingId && !trafficServiceOrderId;
  });
});

const selectManagedShuttleBooking = (booking: TrafficShuttleBooking) => {
  managedShuttleActiveBookingId.value = Number(booking.bookingId);
  managedShuttleExecutionDraft(booking);
};

const closeManagedShuttleDrawer = () => {
  fulfillmentDrawerOpen.value = false;
  managedShuttleNodeContext.value = null;
  managedShuttleActiveBookingId.value = 0;
  managedShuttleReadonly.value = false;
};

const saveManagedShuttleExecution = async () => {
  const context = managedShuttleNodeContext.value;
  const booking = managedShuttleActiveBooking.value;
  if (!context || !booking) return;
  const draft = managedShuttleExecutionDraft(booking);
  managedShuttleExecutionSaving.value = true;
  try {
    const updated = await updateTrafficShuttleBookingExecution(context.row.shuttleId, booking.bookingId, {
      cargoWeightKg: trafficShuttleNumberValue(draft.cargoWeight),
      cargoVolumeCbm: trafficShuttleNumberValue(draft.cargoVolume),
      palletCount: draft.palletCount,
      longitude: trafficShuttleNumberValue(draft.anchorageLongitude),
      latitude: trafficShuttleNumberValue(draft.anchorageLatitude),
      anchoragePosition: [draft.anchorageLongitude, draft.anchorageLatitude].filter(Boolean).join(","),
      craneCount: draft.craneCount
    });
    Object.assign(booking, updated);
  } catch (error) {
    trafficServiceErrorKey.value = error instanceof ApiError ? error.message : "履约信息保存失败";
  } finally {
    managedShuttleExecutionSaving.value = false;
  }
};

const uploadManagedShuttleNodeAttachment = async (event: Event) => {
  const context = managedShuttleNodeContext.value;
  const booking = managedShuttleActiveBooking.value;
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  if (!context || !booking || !files.length) return;
  fulfillmentUploading.value = true;
  try {
    const attachments: BusinessAttachmentPayload[] = [];
    for (const file of files) attachments.push(await uploadBusinessFile(file));
    const items = await saveBargeNodeAttachments(context.row.shuttleId, context.index, attachments, {
      bookingId: booking.bookingId,
      trafficServiceOrderId: booking.trafficServiceOrderId
    });
    fulfillmentDrawerItems.value = items.filter((item) => item.nodeIndex === context.index);
  } finally {
    fulfillmentUploading.value = false;
    input.value = "";
  }
};

const deleteManagedShuttleNodeAttachment = async (attachment: FulfillmentAttachment) => {
  const context = managedShuttleNodeContext.value;
  if (!context || !attachment.attachmentId || fulfillmentUploading.value) return;
  fulfillmentUploading.value = true;
  try {
    await deleteFulfillmentAttachment(attachment.attachmentId);
    fulfillmentDrawerItems.value = fulfillmentDrawerItems.value.filter((item) => item.attachmentId !== attachment.attachmentId);
  } catch (error) {
    trafficServiceErrorKey.value = error instanceof ApiError ? error.message : "附件删除失败";
  } finally {
    fulfillmentUploading.value = false;
  }
};

const bookShuttle = async (row: TrafficShuttleService | Record<string, unknown>) => {
  const shuttle = row as TrafficShuttleService;
  if (!shuttle.shuttleId) return;
  trafficShuttleSaving.value = true;
  try {
    await bookTrafficShuttle(shuttle.shuttleId, {
      passengerCount: 1,
      cargoSummary: shuttle.remark || "",
      contactName: "",
      contactPhone: "",
      remark: ""
    });
    trafficServiceNoticeKey.value = "trafficMarketplace.notice.shuttleBooked";
    await loadTrafficShuttles(false);
  } catch {
    trafficServiceErrorKey.value = "trafficMarketplace.error.shuttleFailed";
  } finally {
    trafficShuttleSaving.value = false;
  }
};

const resetTrafficServiceSearch = async () => {
  trafficServiceKeyword.value = "";
  trafficServiceStatus.value = "";
  trafficServiceSeaArea.value = "";
  await loadTrafficServices();
};

const loadTrafficRoutes = async () => {
  trafficRouteLoading.value = true;
  trafficRouteErrorKey.value = "";
  try {
    await Promise.all([loadTrafficAnchorages(), loadTrafficServices()]);
    const response = await listTrafficRoutes({
      keyword: trafficRouteKeyword.value,
      status: trafficRouteStatus.value,
      seaArea: trafficRouteSeaArea.value,
      serviceDate: trafficRouteServiceDate.value,
      page: 1,
      size: 50
    });
    trafficRouteRows.value = response.items;
    const currentRouteId = trafficRouteDetail.value?.route?.routePlanId;
    const target = currentRouteId ? response.items.find((item) => item.routePlanId === currentRouteId) : response.items[0];
    if (target?.routePlanId) {
      trafficRouteDetail.value = await getTrafficRoute(target.routePlanId);
    } else {
      trafficRouteDetail.value = null;
    }
  } catch {
    trafficRouteRows.value = [];
    trafficRouteDetail.value = null;
    trafficRouteErrorKey.value = "trafficRoute.error.loadFailed";
  } finally {
    trafficRouteLoading.value = false;
  }
};

const resetTrafficRouteSearch = async () => {
  trafficRouteKeyword.value = "";
  trafficRouteStatus.value = "";
  trafficRouteSeaArea.value = "";
  trafficRouteServiceDate.value = "";
  await loadTrafficRoutes();
};

const openTrafficRouteDialog = () => {
  trafficRouteForm.value = {
    routeName: "",
    serviceDate: trafficRouteServiceDate.value || new Date().toISOString().slice(0, 10),
    seaArea: trafficRouteSeaArea.value || "",
    supplierCompanyName: "",
    trafficVesselName: "",
    plannedDepartureTime: "",
    plannedFinishTime: "",
    allowShare: true,
    estimatedCost: undefined,
    remark: ""
  };
  trafficRouteDialogOpen.value = true;
};

const saveTrafficRoute = async () => {
  trafficRouteSaving.value = true;
  trafficRouteErrorKey.value = "";
  try {
    const detail = await createTrafficRoute(trafficRouteForm.value);
    trafficRouteDialogOpen.value = false;
    trafficRouteNoticeKey.value = "trafficRoute.notice.created";
    await loadTrafficRoutes();
    if (detail.route?.routePlanId) {
      trafficRouteDetail.value = await getTrafficRoute(detail.route.routePlanId);
    }
  } catch {
    trafficRouteErrorKey.value = "trafficRoute.error.saveFailed";
  } finally {
    trafficRouteSaving.value = false;
  }
};

const selectTrafficRoute = async (row: TrafficRoutePlan | Record<string, unknown>) => {
  const routeId = Number((row as TrafficRoutePlan).routePlanId);
  if (!Number.isFinite(routeId) || routeId <= 0) return;
  trafficRouteLoading.value = true;
  trafficRouteErrorKey.value = "";
  try {
    trafficRouteDetail.value = await getTrafficRoute(routeId);
  } catch {
    trafficRouteErrorKey.value = "trafficRoute.error.loadFailed";
  } finally {
    trafficRouteLoading.value = false;
  }
};

const addOrderToTrafficRoute = async (row: TrafficServiceOrder | Record<string, unknown>) => {
  const routeId = selectedTrafficRoute.value?.routePlanId;
  const orderId = Number((row as TrafficServiceOrder).serviceOrderId);
  if (!routeId || !Number.isFinite(orderId)) return;
  trafficRouteSaving.value = true;
  try {
    trafficRouteDetail.value = await addTrafficRouteStop(routeId, {
      trafficServiceOrderId: orderId,
      plannedServiceTime: (row as TrafficServiceOrder).useTime || undefined
    });
    await loadTrafficServices();
    const refreshed = await listTrafficRoutes({ page: 1, size: 50 });
    trafficRouteRows.value = refreshed.items;
  } finally {
    trafficRouteSaving.value = false;
  }
};

const removeOrderFromTrafficRoute = async (row: TrafficRouteStop | Record<string, unknown>) => {
  const routeId = selectedTrafficRoute.value?.routePlanId;
  const stopId = Number((row as TrafficRouteStop).routeStopId);
  if (!routeId || !Number.isFinite(stopId)) return;
  trafficRouteSaving.value = true;
  try {
    trafficRouteDetail.value = await removeTrafficRouteStop(routeId, stopId);
    await loadTrafficServices();
  } finally {
    trafficRouteSaving.value = false;
  }
};

const changeTrafficRouteStatus = async (action: "confirm" | "start" | "complete" | "discard") => {
  const routeId = selectedTrafficRoute.value?.routePlanId;
  if (!routeId) return;
  trafficRouteSaving.value = true;
  try {
    trafficRouteDetail.value = await updateTrafficRouteStatus(routeId, action);
    await loadTrafficServices();
    const refreshed = await listTrafficRoutes({ page: 1, size: 50 });
    trafficRouteRows.value = refreshed.items;
  } finally {
    trafficRouteSaving.value = false;
  }
};

const setTrafficServiceForm = (row?: TrafficServiceOrder | Record<string, unknown>) => {
  const typed = row as TrafficServiceOrder | undefined;
  if (!companyContactRows.value.length && !companyContactLoading.value) void loadCompanyContacts();
  if (!companyVesselRows.value.length && !companyVesselLoading.value) void loadCompanyVessels();
  selectedTrafficServiceId.value = typed?.serviceOrderId ?? null;
  trafficServiceForm.value = {
    seaArea: typed?.seaArea || "NORTH",
    anchorageCode: typed?.anchorageCode || "",
    feeType: typed?.feeType || "FREIGHT",
    useTime: typed?.useTime || "",
    serviceType: typed?.serviceType || "PERSONNEL",
    passengerType: typed?.passengerType || "NORMAL",
    passengerCount: typed?.passengerCount,
    cargoType: typed?.cargoType || "CARGO",
    returnTrip: Boolean(typed?.returnTrip),
    allowShare: Boolean(typed?.allowShare),
    basePrice: typed?.basePrice,
    sharedPrice: typed?.sharedPrice,
    remark: typed?.remark || "",
    businessContactId: typed?.businessContactId,
    businessContactName: typed?.businessContactName || "",
    businessContactPhone: typed?.businessContactPhone || "",
    acceptedAt: typed?.acceptedAt || "",
    trafficVesselId: typed?.trafficVesselId,
    trafficVesselName: typed?.trafficVesselName || "",
    handlerContactId: typed?.handlerContactId,
    handlerName: typed?.handlerName || "",
    handlerPhone: typed?.handlerPhone || "",
    supplierMessage: typed?.supplierMessage || "",
    departureTime: typed?.departureTime || "",
    arrivalTime: typed?.arrivalTime || "",
    returnStartTime: typed?.returnStartTime || "",
    returnEndTime: typed?.returnEndTime || "",
    signPhotoUrl: typed?.signPhotoUrl || "",
    pickupPhotoUrl: typed?.pickupPhotoUrl || "",
    returnArrivalPhotoUrl: typed?.returnArrivalPhotoUrl || "",
    cargos: typed?.cargos?.length ? typed.cargos.map((cargo) => ({ ...cargo })) : []
  };
};

const openTrafficServiceDialog = (row?: TrafficServiceOrder | Record<string, unknown>) => {
  setTrafficServiceForm(row);
  trafficServiceDialogOpen.value = true;
};

const openTrafficServiceDetail = (row: TrafficServiceOrder | Record<string, unknown>) => {
  const id = Number((row as TrafficServiceOrder).serviceOrderId);
  if (!Number.isFinite(id) || id <= 0) return;
  router.push(`/traffic-boat/${id}`);
};

const saveTrafficService = async () => {
  trafficServiceSaving.value = true;
  trafficServiceErrorKey.value = "";
  try {
    if (selectedTrafficServiceId.value) {
      await updateTrafficServiceOrder(selectedTrafficServiceId.value, trafficServiceForm.value);
      trafficServiceNoticeKey.value = "trafficService.notice.updated";
    } else {
      await createTrafficServiceOrder(trafficServiceForm.value);
      trafficServiceNoticeKey.value = "trafficService.notice.created";
    }
    if (!isTrafficServiceDetailPage.value) {
      trafficServiceDialogOpen.value = false;
    }
    await loadTrafficServices();
  } catch {
    trafficServiceErrorKey.value = "trafficService.error.saveFailed";
  } finally {
    trafficServiceSaving.value = false;
  }
};

const discardTrafficService = async (row: TrafficServiceOrder | Record<string, unknown>) => {
  const id = Number((row as TrafficServiceOrder).serviceOrderId);
  if (!id) return;
  trafficServiceLoading.value = true;
  try {
    await discardTrafficServiceOrder(id);
    trafficServiceNoticeKey.value = "trafficService.notice.discarded";
    await loadTrafficServices();
  } catch {
    trafficServiceErrorKey.value = "trafficService.error.discardFailed";
  } finally {
    trafficServiceLoading.value = false;
  }
};

const confirmTrafficService = async (row: TrafficServiceOrder | Record<string, unknown>) => {
  const id = Number((row as TrafficServiceOrder).serviceOrderId);
  if (!id) return;
  trafficServiceLoading.value = true;
  try {
    await confirmTrafficServiceOrder(id);
    trafficServiceNoticeKey.value = "trafficService.notice.confirmed";
    await loadTrafficServices();
  } catch {
    trafficServiceErrorKey.value = "trafficService.error.confirmFailed";
  } finally {
    trafficServiceLoading.value = false;
  }
};

const loadTrafficBoatPrices = async () => {
  trafficBoatLoading.value = true;
  trafficBoatErrorKey.value = "";
  try {
    const rows = await listTrafficBoatPrices({
      keyword: trafficBoatKeyword.value,
      seaArea: trafficBoatSeaArea.value
    });
    trafficBoatRows.value = rows.map(normalizeTrafficBoatRow);
  } catch {
    trafficBoatRows.value = [];
    trafficBoatErrorKey.value = "trafficService.error.loadFailed";
  } finally {
    trafficBoatLoading.value = false;
  }
};

const resetTrafficBoatSearch = async () => {
  trafficBoatKeyword.value = "";
  trafficBoatSeaArea.value = "";
  await loadTrafficBoatPrices();
};

const saveTrafficBoatPrice = async (row: TrafficBoatPrice | Record<string, unknown>) => {
  const typed = row as TrafficBoatPrice;
  trafficBoatSavingCode.value = typed.anchorageCode;
  trafficBoatErrorKey.value = "";
  const payload: TrafficBoatPricePayload = {
    anchorageCode: typed.anchorageCode,
    basePrice: typed.basePrice == null || Number.isNaN(Number(typed.basePrice)) ? undefined : Number(typed.basePrice),
    sharedPrice: typed.sharedPrice == null || Number.isNaN(Number(typed.sharedPrice)) ? undefined : Number(typed.sharedPrice),
    enabled: typed.enabled,
    remark: typed.remark || ""
  };
  try {
    if (typed.priceId) {
      await updateTrafficBoatPrice(typed.priceId, payload);
    } else {
      await createTrafficBoatPrice(payload);
    }
    trafficBoatNoticeKey.value = "trafficService.notice.priceSaved";
    await loadTrafficBoatPrices();
  } catch {
    trafficBoatErrorKey.value = "trafficService.error.saveFailed";
  } finally {
    trafficBoatSavingCode.value = "";
  }
};

const buildTrafficBoatPricePayload = (row: TrafficBoatPrice): TrafficBoatPricePayload => ({
  anchorageCode: row.anchorageCode,
  basePrice: row.basePrice == null || Number.isNaN(Number(row.basePrice)) ? undefined : Number(row.basePrice),
  sharedPrice: row.sharedPrice == null || Number.isNaN(Number(row.sharedPrice)) ? undefined : Number(row.sharedPrice),
  enabled: row.enabled,
  remark: row.remark || ""
});

const saveAllTrafficBoatPrices = async () => {
  trafficBoatSavingAll.value = true;
  trafficBoatErrorKey.value = "";
  try {
    const rows = await updateTrafficBoatPrices(trafficBoatRows.value.map(buildTrafficBoatPricePayload));
    trafficBoatRows.value = rows.map(normalizeTrafficBoatRow);
    trafficBoatNoticeKey.value = "trafficService.notice.pricesSaved";
  } catch {
    trafficBoatErrorKey.value = "trafficService.error.saveFailed";
  } finally {
    trafficBoatSavingAll.value = false;
  }
};

const ensureTrafficWorkspaceLoaded = () => {
  if (isTrafficServiceDetailPage.value && trafficServiceRows.value.length) {
    const target = trafficServiceRows.value.find((row) => row.serviceOrderId === trafficServiceDetailId.value);
    if (target) setTrafficServiceForm(target);
    return;
  }
  if (pageKey.value === "transportServices" && !trafficRequestLoading.value && !trafficRequestRows.value.length && !trafficServiceErrorKey.value) {
    void loadTrafficRequests();
    void loadTrafficShuttles(false);
  }
  if (pageKey.value === "trafficBoat" && !trafficShuttleLoading.value && !trafficShuttleRows.value.length && !trafficServiceErrorKey.value) {
    void loadTrafficShuttles(false);
  }
  if (pageKey.value === "trafficBoatMyServices" && !trafficShuttleLoading.value && !trafficShuttleRows.value.length && !trafficServiceErrorKey.value) {
    void loadTrafficShuttles(true);
  }
  if ((pageKey.value === "delivery" || pageKey.value === "trafficBoatMyServices") && !trafficServiceLoading.value && (!trafficServiceRows.value.length || isTrafficServiceDetailPage.value) && !trafficServiceErrorKey.value) {
    void loadTrafficServices();
  }
  if (pageKey.value === "trafficRoutes" && !trafficRouteLoading.value && !trafficRouteRows.value.length && !trafficRouteErrorKey.value) {
    void loadTrafficRoutes();
  }
  if (
    pageKey.value === "supplierProducts" &&
    shopManagementTab.value === "trafficService" &&
    !trafficBoatLoading.value &&
    !trafficBoatRows.value.length &&
    !trafficBoatErrorKey.value
  ) {
    void loadTrafficBoatPrices();
    void loadTrafficShuttles(true);
  }
};

const openPurchaseOrderDetail = (row: PurchaseOrderSummary | Record<string, unknown>) => {
  const orderId = "purchaseOrderId" in row ? row.purchaseOrderId : undefined;
  if (!orderId) return;
  router.push(purchaseOrderWorkspaceMode.value === "supplier" ? `/supplier/orders/${orderId}` : `/orders/${orderId}`);
};

const openSupplierOrderDetail = (row: Record<string, unknown>) => {
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  if (!orderId) return;
  const supplierOrderId = Number(row.supplierOrderId);
  const hasCurrentSupplier = (purchaseOrderDetail.value?.supplierOrders ?? []).some(
    (supplier) => Number(supplier.supplierOrderId) === supplierOrderId
  );
  skipNextPurchaseOrderDetailReload.value = hasCurrentSupplier;
  router.push({
    path: `/supplier/orders/${orderId}`,
    query: Number.isFinite(supplierOrderId) && supplierOrderId > 0 ? { supplierOrderId: String(supplierOrderId) } : undefined
  });
};

const openPurchaseTrafficServiceDetail = (row: Record<string, unknown>) => {
  const serviceOrderId = Number(row.serviceOrderId);
  if (!Number.isFinite(serviceOrderId) || serviceOrderId <= 0) return;
  router.push(`/traffic-boat/${serviceOrderId}`);
};

const openSupplierActionDialog = (supplierOrder: PurchaseSupplierOrder, type: "confirm" | "reject") => {
  supplierActionOrder.value = supplierOrder;
  supplierActionType.value = type;
  supplierRejectReason.value = "";
  supplierConfirmForm.value = {
    expectedReadyAt: supplierOrder.expectedReadyAt || "",
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

const supplyChainFinanceColumns = computed<TableColumn[]>(() => [
  { key: "purchaseOrderNo", label: t("page.supplyChainFinance.field.purchaseOrderNo"), width: "150px" },
  { key: "vesselName", label: t("page.supplyChainFinance.field.vesselName"), width: "136px" },
  { key: "supplierName", label: t("page.supplyChainFinance.field.supplierName"), width: "180px" },
  { key: "orderAmount", label: t("page.supplyChainFinance.field.orderAmount"), width: "122px", align: "right" },
  { key: "prepaymentAmount", label: t("page.supplyChainFinance.field.prepaymentAmount"), width: "122px", align: "right" },
  { key: "financeAmount", label: t("page.supplyChainFinance.field.financeAmount"), width: "122px", align: "right" },
  { key: "prepaymentRate", label: t("page.supplyChainFinance.field.prepaymentRate"), width: "90px", align: "right" },
  { key: "loanTerm", label: t("page.supplyChainFinance.field.loanTerm"), width: "90px" },
  { key: "annualRate", label: t("page.supplyChainFinance.field.annualRate"), width: "82px", align: "right" },
  { key: "status", label: t("page.supplyChainFinance.field.status"), width: "112px", align: "center" }
]);

const supplyChainFinanceMetrics = computed(() => [
  { label: t("page.supplyChainFinance.metric.financeableAmount"), value: formatDisplayMoney(3200000, "CNY"), note: t("page.supplyChainFinance.metric.financeableAmountNote") },
  { label: t("page.supplyChainFinance.metric.lockedPrepayment"), value: formatDisplayMoney(640000, "CNY"), note: t("page.supplyChainFinance.metric.lockedPrepaymentNote") },
  { label: t("page.supplyChainFinance.metric.applyingLoan"), value: formatDisplayMoney(1880000, "CNY"), note: t("page.supplyChainFinance.metric.applyingLoanNote") },
  { label: t("page.supplyChainFinance.metric.repaymentBalance"), value: formatDisplayMoney(680000, "CNY"), note: t("page.supplyChainFinance.metric.repaymentBalanceNote") }
]);

const supplyChainFinanceRows = computed<SupplyChainFinanceRow[]>(() => [
  {
    purchaseOrderNo: "PO-20260630-001",
    vesselName: "PACIFIC HARVEST",
    supplierName: "供货商A",
    orderAmount: 1000000,
    prepaymentAmount: 200000,
    financeAmount: 800000,
    prepaymentRate: "20%",
    loanTerm: "60 天",
    annualRate: "6.8%",
    status: "reviewing",
    riskLevel: "low"
  },
  {
    purchaseOrderNo: "PO-20260630-002",
    vesselName: "BLUE PORT",
    supplierName: "供货商B",
    orderAmount: 760000,
    prepaymentAmount: 152000,
    financeAmount: 608000,
    prepaymentRate: "20%",
    loanTerm: "45 天",
    annualRate: "6.5%",
    status: "prepaying",
    riskLevel: "medium"
  },
  {
    purchaseOrderNo: "PO-20260630-003",
    vesselName: "ZHONG WAI YUN 6",
    supplierName: "供货商C",
    orderAmount: 1440000,
    prepaymentAmount: 288000,
    financeAmount: 1152000,
    prepaymentRate: "20%",
    loanTerm: "90 天",
    annualRate: "7.2%",
    status: "funded",
    riskLevel: "low"
  }
]);

const supplyChainFinanceTimeline = computed(() => [
  { label: t("page.supplyChainFinance.timeline.orderConfirmed"), value: "2026-06-30 09:20" },
  { label: t("page.supplyChainFinance.timeline.prepaymentLocked"), value: formatDisplayMoney(200000, "CNY") },
  { label: t("page.supplyChainFinance.timeline.financeReview"), value: t("page.supplyChainFinance.statusReviewing") },
  { label: t("page.supplyChainFinance.timeline.fundedToSupplier"), value: formatDisplayMoney(800000, "CNY") },
  { label: t("page.supplyChainFinance.timeline.repaymentDue"), value: "2026-08-29" }
]);

const supplyChainFinanceStatusLabel = (status: SupplyChainFinanceStatus) => {
  const keys: Record<SupplyChainFinanceStatus, string> = {
    pending: "page.supplyChainFinance.statusPending",
    prepaying: "page.supplyChainFinance.statusPrepaying",
    reviewing: "page.supplyChainFinance.statusReviewing",
    funded: "page.supplyChainFinance.statusFunded",
    repaying: "page.supplyChainFinance.statusRepaying",
    settled: "page.supplyChainFinance.statusSettled"
  };
  return t(keys[status]);
};

const supplyChainFinanceStatusVariant = (status: SupplyChainFinanceStatus): StatusVariant => {
  if (status === "settled" || status === "funded") return "success";
  if (status === "reviewing" || status === "repaying") return "info";
  if (status === "prepaying") return "warning";
  return "neutral";
};

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
  if (pageKey.value === "supplierProducts") {
    openCompanyContactCreateFromAnyTab();
    return;
  }
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

const markSupplierReadyAction = async (supplierOrder: PurchaseSupplierOrder) => {
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  if (!orderId) return;
  supplierActionSaving.value = true;
  purchaseOrderDetailError.value = "";
  try {
    await saveSupplierCustomsDocumentsAction(supplierOrder, false);
    purchaseOrderDetail.value = await markSupplierOrderReady(orderId, supplierOrder.supplierOrderId);
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
  } finally {
    supplierActionSaving.value = false;
  }
};

const markSupplierSuppliedAction = async (supplierOrder: PurchaseSupplierOrder) => {
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  if (!orderId) return;
  supplierActionSaving.value = true;
  purchaseOrderDetailError.value = "";
  try {
    purchaseOrderDetail.value = await markSupplierOrderSupplied(orderId, supplierOrder.supplierOrderId, {
      deliveryRemark: supplierOrder.deliveryRemark || "补给完成"
    });
    purchaseOrderDetailNotice.value = "供货商补给状态已更新。";
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
  } finally {
    supplierActionSaving.value = false;
  }
};

const markSupplierWaitingSupplyAction = async (supplierOrder: PurchaseSupplierOrder) => {
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  if (!orderId) return;
  supplierActionSaving.value = true;
  purchaseOrderDetailError.value = "";
  try {
    purchaseOrderDetail.value = await markSupplierOrderWaitingSupply(orderId, supplierOrder.supplierOrderId);
    purchaseOrderDetailNotice.value = "运输完成，订单已进入待补给。";
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
  } finally {
    supplierActionSaving.value = false;
  }
};

const uploadSupplierFulfillmentAttachment = async (supplierOrder: PurchaseSupplierOrder, event: Event) => {
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  if (!orderId || !files.length) return;
  supplierFulfillmentUploading.value = true;
  try {
    const attachments: BusinessAttachmentPayload[] = [];
    for (const file of files) attachments.push(await uploadBusinessFile(file));
    fulfillmentAttachments.value = await saveSupplierFulfillmentAttachments(orderId, supplierOrder.supplierOrderId, attachments);
    purchaseOrderDetailNotice.value = `已上传 ${attachments.length} 个运输附件。`;
  } catch (error) {
    purchaseOrderDetailError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
  } finally {
    supplierFulfillmentUploading.value = false;
    input.value = "";
  }
};

const getPurchaseSupplierExecutionAction = (supplierOrder: PurchaseSupplierOrder) => {
  const status = String(supplierOrder.status || "").toUpperCase();
  if (purchaseSupplierPendingStatuses.has(status)) return { label: "确认", kind: "confirm" };
  if (!purchaseSupplierReadyStatuses.has(status) && !purchaseSupplierRejectedStatuses.has(status)) return { label: "备货", kind: "ready" };
  if (purchaseSupplierShipmentActionStatuses.has(status)) return { label: "发货", kind: "supplied" };
  if (purchaseSupplierTransportActionStatuses.has(status)) return { label: "运输完成", kind: "waitingSupply" };
  return null;
};

const runPurchaseSupplierExecutionAction = async (supplierOrder: PurchaseSupplierOrder) => {
  const action = getPurchaseSupplierExecutionAction(supplierOrder);
  if (!action) return;
  if (action.kind === "confirm") {
    openSupplierActionDialog(supplierOrder, "confirm");
    return;
  }
  if (action.kind === "ready") {
    await markSupplierReadyAction(supplierOrder);
    return;
  }
  if (action.kind === "waitingSupply") {
    await markSupplierWaitingSupplyAction(supplierOrder);
    return;
  }
  await markSupplierSuppliedAction(supplierOrder);
};

const purchaseDetailBargeActionLabel = computed(() => {
  const row = purchaseDetailPrimaryTrafficService.value;
  if (!row) return "";
  const status = String(row.status || "").toUpperCase();
  if (["PENDING_CONFIRM", "PENDING", "WAITING_CONFIRM"].includes(status)) return "确认驳船";
  if (purchaseDetailExecutionStageKey.value === "waitingSupply") return "更新补给";
  if (purchaseDetailExecutionStageKey.value === "supplying") return "补给完成";
  return "查看驳船";
});

const runPurchaseBargeExecutionAction = async () => {
  const row = purchaseDetailPrimaryTrafficService.value;
  if (!row) return;
  const status = String(row.status || "").toUpperCase();
  if (["PENDING_CONFIRM", "PENDING", "WAITING_CONFIRM"].includes(status)) {
    await confirmTrafficService(row);
    await loadPurchaseOrderDetail();
    return;
  }
  openPurchaseTrafficServiceDetail(row as unknown as Record<string, unknown>);
};

const saveSupplierCustomsDocumentsAction = async (supplierOrder?: PurchaseSupplierOrder | null, showNotice = true) => {
  const orderId = purchaseOrderDetail.value?.order.purchaseOrderId;
  const currentSupplierOrder = supplierOrder ?? supplierDetailCurrentOrder.value;
  if (!orderId || !currentSupplierOrder) return;
  supplierCustomsUploading.value = true;
  supplierCustomsError.value = "";
  try {
    purchaseOrderDetail.value = await saveSupplierCustomsDocuments(orderId, currentSupplierOrder.supplierOrderId, supplierCustomsFiles.value);
    if (showNotice) {
      purchaseOrderNotice.value = t("purchaseOrder.notice.customsSaved");
    }
  } catch (error) {
    supplierCustomsError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
    throw error;
  } finally {
    supplierCustomsUploading.value = false;
  }
};

const handleSupplierCustomsUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (!files.length) return;
  supplierCustomsUploading.value = true;
  supplierCustomsError.value = "";
  try {
    const uploadedFiles = [];
    for (const file of files) {
      const uploaded = await uploadQualificationFile(file);
      const fileId = String(uploaded.fileId || uploaded.id || "");
      uploadedFiles.push({
        fileId,
        fileName: uploaded.name || file.name,
        fileUrl: uploaded.url || (fileId ? buildFileUrlFromId(fileId) : "")
      });
    }
    supplierCustomsFiles.value = [...supplierCustomsFiles.value, ...uploadedFiles];
  } catch (error) {
    supplierCustomsError.value = getPurchaseErrorText(error, "purchaseOrder.error.supplierActionFailed");
  } finally {
    supplierCustomsUploading.value = false;
    input.value = "";
  }
};

const formatDateTimeParts = (value?: string | null) => {
  const text = formatDemandDateTime(value);
  if (text === "-") return { date: "-", time: "" };
  const [date, time = ""] = text.split(" ");
  return { date: date || "-", time };
};

const demandStatusLabel = (status?: string | null) => {
  const value = String(status || "").toUpperCase();
  const keys: Record<string, string> = {
    SAVED: "page.materialDemand.statusSaved",
    COMPARING: "page.materialDemand.statusComparing",
    ORDERED: "page.materialDemand.statusOrdered",
    DISCARDED: "page.materialDemand.statusDiscarded",
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
  if (value === "COMPLETED" || value === "ORDERED") return "success";
  if (value === "COMPARING" || value === "SUBMITTED" || value === "PENDING_REVIEW") return "info";
  if (value === "SAVED" || value === "DRAFT") return "warning";
  if (value === "CANCELLED" || value === "DISCARDED") return "danger";
  return "neutral";
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

const resetDictionaryTypeForm = () => {
  editingDictionaryTypeCode.value = "";
  dictionaryTypeForm.value = {
    typeCode: "",
    typeName: "",
    description: "",
    sortOrder: 0,
    enabled: true
  };
};

const resetDictionaryItemForm = () => {
  editingDictionaryItemId.value = null;
  dictionaryItemForm.value = {
    typeCode: selectedDictionaryTypeCode.value,
    itemCode: "",
    itemName: "",
    itemValue: "",
    itemNameEn: "",
    description: "",
    sortOrder: 0,
    enabled: true
  };
};

const loadDictionaryItems = async (typeCode = selectedDictionaryTypeCode.value) => {
  if (!typeCode) {
    dictionaryItems.value = [];
    return;
  }
  dictionaryLoading.value = true;
  dictionaryErrorKey.value = "";
  try {
    dictionaryItems.value = await listDictionaryItems({
      typeCode,
      keyword: dictionaryItemKeyword.value
    });
  } catch {
    dictionaryItems.value = [];
    dictionaryErrorKey.value = "dataDictionary.loadFailed";
  } finally {
    dictionaryLoading.value = false;
  }
};

const loadDictionaryTypes = async () => {
  dictionaryLoading.value = true;
  dictionaryErrorKey.value = "";
  try {
    const types = await listDictionaryTypes({ keyword: dictionaryTypeKeyword.value });
    dictionaryTypes.value = types;
    if (!selectedDictionaryTypeCode.value || !types.some((item) => item.typeCode === selectedDictionaryTypeCode.value)) {
      selectedDictionaryTypeCode.value = types[0]?.typeCode || "";
    }
    resetDictionaryItemForm();
    await loadDictionaryItems(selectedDictionaryTypeCode.value);
  } catch {
    dictionaryTypes.value = [];
    dictionaryItems.value = [];
    dictionaryErrorKey.value = "dataDictionary.loadFailed";
  } finally {
    dictionaryLoading.value = false;
  }
};

const ensureDictionaryLoaded = () => {
  if (pageKey.value === "dataDictionary" && !dictionaryLoading.value && !dictionaryTypes.value.length) {
    void loadDictionaryTypes();
  }
};

const selectDictionaryType = (row: Record<string, unknown>) => {
  const typeCode = String(row.typeCode || "");
  if (!typeCode || typeCode === selectedDictionaryTypeCode.value) return;
  selectedDictionaryTypeCode.value = typeCode;
  resetDictionaryItemForm();
  void loadDictionaryItems(typeCode);
};

const editDictionaryType = (row: Record<string, unknown>) => {
  editingDictionaryTypeCode.value = String(row.typeCode || "");
  dictionaryTypeForm.value = {
    typeCode: String(row.typeCode || ""),
    typeName: String(row.typeName || ""),
    description: String(row.description || ""),
    sortOrder: Number(row.sortOrder || 0),
    enabled: Boolean(row.enabled)
  };
  dictionaryNotice.value = "";
};

const editDictionaryItem = (row: Record<string, unknown>) => {
  editingDictionaryItemId.value = Number(row.id || 0) || null;
  dictionaryItemForm.value = {
    typeCode: String(row.typeCode || selectedDictionaryTypeCode.value),
    itemCode: String(row.itemCode || ""),
    itemName: String(row.itemName || ""),
    itemValue: String(row.itemValue || ""),
    itemNameEn: String(row.itemNameEn || ""),
    description: String(row.description || ""),
    sortOrder: Number(row.sortOrder || 0),
    enabled: Boolean(row.enabled)
  };
  dictionaryNotice.value = "";
};

const saveDictionaryTypeDraft = async () => {
  const form = dictionaryTypeForm.value;
  if (!editingDictionaryTypeCode.value && !form.typeCode.trim()) {
    dictionaryNotice.value = t("dataDictionary.required");
    return;
  }
  if (!form.typeName.trim()) {
    dictionaryNotice.value = t("dataDictionary.required");
    return;
  }
  dictionarySaving.value = true;
  dictionaryNotice.value = "";
  try {
    const payload: DictionaryTypePayload = {
      typeCode: form.typeCode.trim(),
      typeName: form.typeName.trim(),
      description: form.description.trim(),
      sortOrder: Number(form.sortOrder || 0),
      enabled: form.enabled
    };
    const saved = await saveDictionaryType(payload, editingDictionaryTypeCode.value || undefined);
    selectedDictionaryTypeCode.value = saved.typeCode;
    resetDictionaryTypeForm();
    await loadDictionaryTypes();
  } catch {
    dictionaryNotice.value = t("dataDictionary.saveFailed");
  } finally {
    dictionarySaving.value = false;
  }
};

const saveDictionaryItemDraft = async () => {
  const form = dictionaryItemForm.value;
  if (!selectedDictionaryTypeCode.value) {
    dictionaryNotice.value = t("dataDictionary.selectTypeFirst");
    return;
  }
  if (!form.itemCode.trim() || !form.itemName.trim()) {
    dictionaryNotice.value = t("dataDictionary.required");
    return;
  }
  dictionarySaving.value = true;
  dictionaryNotice.value = "";
  try {
    const payload: DictionaryItemPayload = {
      typeCode: selectedDictionaryTypeCode.value,
      itemCode: form.itemCode.trim(),
      itemName: form.itemName.trim(),
      itemValue: form.itemValue.trim(),
      itemNameEn: form.itemNameEn.trim(),
      description: form.description.trim(),
      sortOrder: Number(form.sortOrder || 0),
      enabled: form.enabled
    };
    await saveDictionaryItem(payload, editingDictionaryItemId.value || undefined);
    resetDictionaryItemForm();
    await loadDictionaryItems(selectedDictionaryTypeCode.value);
  } catch {
    dictionaryNotice.value = t("dataDictionary.saveFailed");
  } finally {
    dictionarySaving.value = false;
  }
};

const disableDictionaryType = async (row: Record<string, unknown>) => {
  const typeCode = String(row.typeCode || "");
  if (!typeCode) return;
  dictionarySaving.value = true;
  dictionaryNotice.value = "";
  try {
    await deleteDictionaryType(typeCode);
    if (selectedDictionaryTypeCode.value === typeCode) {
      selectedDictionaryTypeCode.value = "";
      dictionaryItems.value = [];
    }
    await loadDictionaryTypes();
  } catch {
    dictionaryNotice.value = t("dataDictionary.deleteFailed");
  } finally {
    dictionarySaving.value = false;
  }
};

const disableDictionaryItem = async (row: Record<string, unknown>) => {
  const itemId = Number(row.id || 0);
  if (!itemId) return;
  dictionarySaving.value = true;
  dictionaryNotice.value = "";
  try {
    await deleteDictionaryItem(itemId);
    await loadDictionaryItems(selectedDictionaryTypeCode.value);
  } catch {
    dictionaryNotice.value = t("dataDictionary.deleteFailed");
  } finally {
    dictionarySaving.value = false;
  }
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
      stage: "COMPARISON",
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

const ensureSettlementManagementLoaded = () => {
  if (["settlements", "supplierSettlements", "bargeSettlements"].includes(pageKey.value) && !settlementManagementLoading.value) {
    void loadSettlementManagement();
  }
};
const ensureEvaluationsLoaded = () => {
  if (isEvaluationPage.value && !evaluationLoading.value) void loadEvaluations();
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

const ensureSupplierInfoLoaded = () => {
  if (pageKey.value === "suppliers" && !supplierInfoLoading.value && !supplierInfoRows.value.length) {
    void loadSupplierInfoRows();
  }
};

const refreshCurrentPage = () => {
  startDashboardMetricAnimation();
  if (pageKey.value === "dashboard") {
    void loadDashboardData();
    return;
  }
  if (pageKey.value === "impa") {
    void loadImpaCategories();
    return;
  }
  if (pageKey.value === "suppliers") {
    void loadSupplierInfoRows();
    return;
  }
  if (pageKey.value === "supplierProducts") {
    void loadShopWorkspace();
    if (shopManagementTab.value === "trafficService") {
      void loadTrafficBoatPrices();
      void loadTrafficShuttles(true);
    }
    return;
  }
  if (pageKey.value === "requests") {
    void loadMaterialDemands();
    return;
  }
  if (pageKey.value === "inquiries") {
    void loadInquiryDemands();
    return;
  }
  if (pageKey.value === "comparisonList") {
    void loadComparisonDemands();
    return;
  }
  if (pageKey.value === "compare") {
    void loadCompareWorkspace();
    return;
  }
  if (isPurchaseOrdersPage.value) {
    if (isPurchaseOrderDetailPage.value) void loadPurchaseOrderDetail();
    else reloadPurchaseOrderWorkspace();
    return;
  }
  if (["settlements", "supplierSettlements", "bargeSettlements"].includes(pageKey.value)) {
    void loadSettlementManagement();
    return;
  }
  if (isEvaluationPage.value) {
    void loadEvaluations();
    return;
  }
  if (pageKey.value === "transportServices") {
    void loadTrafficRequests();
    void loadTrafficShuttles(false);
    return;
  }
  if (pageKey.value === "delivery") {
    void loadTrafficServices();
    return;
  }
  if (pageKey.value === "trafficBoat") {
    void loadTrafficRequests();
    void loadTrafficShuttles(false);
    return;
  }
  if (pageKey.value === "trafficBoatMyServices") {
    void loadTrafficShuttles(true);
    void loadTrafficServices();
    return;
  }
  if (pageKey.value === "trafficRoutes") {
    void loadTrafficRoutes();
    return;
  }
  if (pageKey.value === "menuManagement") {
    void loadMenuManagementRows();
    return;
  }
  if (pageKey.value === "dataDictionary") {
    void loadDictionaryTypes();
  }
};

onMounted(() => {
  if (typeof window !== "undefined") window.addEventListener("keydown", handleShopFullscreenKeydown);
  startDashboardMetricAnimation();
  ensureDashboardLoaded();
  ensureImpaLoaded();
  ensurePermissionsLoaded();
  ensureRegistrationsLoaded();
  ensureCompanyMembersLoaded();
  ensureMaterialDemandsLoaded();
  ensureInquiryDemandsLoaded();
  ensureComparisonDemandsLoaded();
  ensureSettlementManagementLoaded();
  ensureEvaluationsLoaded();
  ensureMenuManagementLoaded();
  ensureDictionaryLoaded();
  ensureSupplierInfoLoaded();
  ensureShopLoaded();
  ensureCompareLoaded();
  ensurePurchaseOrdersLoaded();
  ensureTrafficWorkspaceLoaded();
});
watch(pageKey, () => {
  shopListFullscreen.value = false;
  settlementManagementStatus.value = "";
  evaluationStatus.value = "";
  startDashboardMetricAnimation();
  ensureDashboardLoaded();
  ensureImpaLoaded();
  ensurePermissionsLoaded();
  ensureRegistrationsLoaded();
  ensureCompanyMembersLoaded();
  ensureMaterialDemandsLoaded();
  ensureInquiryDemandsLoaded();
  ensureComparisonDemandsLoaded();
  ensureSettlementManagementLoaded();
  ensureEvaluationsLoaded();
  ensureMenuManagementLoaded();
  ensureDictionaryLoaded();
  ensureSupplierInfoLoaded();
  ensureShopLoaded();
  ensureCompareLoaded();
  ensurePurchaseOrdersLoaded();
  ensureTrafficWorkspaceLoaded();
});
watch(
  () => route.query._menuRefresh,
  () => {
    refreshCurrentPage();
  }
);
const scheduleLiveFilterReload = (reload: () => void) => {
  if (liveFilterTimer) window.clearTimeout(liveFilterTimer);
  liveFilterTimer = window.setTimeout(reload, 120);
};
watch(
  () => [inquiryDemandStatus.value, inquiryDemandDateFrom.value, inquiryDemandDateTo.value],
  () => {
    if (pageKey.value === "inquiries") scheduleLiveFilterReload(() => void loadInquiryDemands());
  }
);
watch(
  () => [comparisonDemandStatus.value, comparisonDemandDateFrom.value, comparisonDemandDateTo.value],
  () => {
    if (pageKey.value === "comparisonList") scheduleLiveFilterReload(() => void loadComparisonDemands());
  }
);
watch(
  () => [
    purchaseOrderStatus.value,
    purchaseOrderCreatedFrom.value,
    purchaseOrderCreatedTo.value
  ],
  () => {
    if (isPurchaseOrdersPage.value && !isPurchaseOrderDetailPage.value) scheduleLiveFilterReload(reloadPurchaseOrderWorkspace);
  }
);
watch(
  () => [settlementManagementStatus.value],
  () => {
    if (["settlements", "supplierSettlements", "bargeSettlements"].includes(pageKey.value)) {
      scheduleLiveFilterReload(() => void loadSettlementManagement());
    }
  }
);
watch(
  () => [evaluationStatus.value],
  () => {
    if (isEvaluationPage.value) scheduleLiveFilterReload(() => void loadEvaluations());
  }
);
watch(compareDemandId, () => {
  ensureCompareLoaded();
});
watch(purchaseOrderIdFromRoute, () => {
  purchaseOrderDetailTab.value = "settlement";
  showPurchaseOrderDetailBackTop.value = false;
  ensurePurchaseOrdersLoaded();
});
watch([purchaseOrderDetail, purchaseOrderDetailTab], () => {
  void nextTick(bindPurchaseOrderDetailScrollPanel);
});
watch(trafficServiceDetailId, () => {
  ensureTrafficWorkspaceLoaded();
});
watch(shopManagementTab, () => {
  ensureTrafficWorkspaceLoaded();
});
watch(selectedAdminUserId, () => {
  selectedAdminUserRoleCode.value = selectedAdminUser.value?.roleCodes[0] || permissionRoles.value[0]?.code || "admin";
});

const workbenchGlobalLoading = computed(() => {
  if (loading.value) return true;
  if (pageKey.value === "dashboard") return dashboardLoading.value;
  if (pageKey.value === "requests") return materialDemandLoading.value;
  if (pageKey.value === "inquiries") return inquiryDemandLoading.value;
  if (pageKey.value === "comparisonList") return comparisonDemandLoading.value;
  if (pageKey.value === "comparison") return compareLoading.value || compareSupplierLoading.value || compareTrafficShuttleLoading.value || replacementLoading.value;
  if (isPurchaseOrdersPage.value) return false;
  if (["settlements", "supplierSettlements", "bargeSettlements"].includes(pageKey.value)) return settlementManagementLoading.value;
  if (isEvaluationPage.value) return evaluationLoading.value;
  if (pageKey.value === "supplierProducts") {
    return shopLoading.value
      || shopProfileLoading.value
      || shopSkuListLoading.value
      || companyQualificationLoading.value
      || companyContactLoading.value
      || companyVesselLoading.value
      || companyValueAddedServiceLoading.value
      || trafficBoatLoading.value
      || trafficShuttleLoading.value;
  }
  if (pageKey.value === "suppliers") return supplierInfoLoading.value;
  if (pageKey.value === "impa") return impaCategoriesLoading.value || impaItemsLoading.value;
  if (pageKey.value === "registrations") return registrationLoading.value;
  if (pageKey.value === "companyMembers") return companyMemberLoading.value;
  if (pageKey.value === "permissions") return permissionLoading.value;
  if (pageKey.value === "menuManagement") return menuManagementLoading.value;
  if (pageKey.value === "dataDictionary") return dictionaryLoading.value;
  if (pageKey.value === "trafficRoutes") return trafficRouteLoading.value || trafficServiceLoading.value;
  if (pageKey.value === "transportServices") return trafficRequestLoading.value || trafficShuttleLoading.value || trafficServiceLoading.value;
  if (pageKey.value === "trafficBoat") return trafficShuttleLoading.value;
  if (pageKey.value === "trafficBoatMyServices") return trafficShuttleLoading.value || trafficServiceLoading.value;
  if (pageKey.value === "delivery") return trafficServiceLoading.value;
  return false;
});
</script>

<template>
  <WorkbenchLayout>
    <LoadingOverlay :active="workbenchGlobalLoading">
      <section v-if="pageKey === 'dashboardGovernment'" class="gov-dashboard-workspace">
        <div class="gov-dashboard-shell gov-code-dashboard">
          <section class="gov-code-stat-grid" aria-label="平台核心指标">
            <article v-for="card in governmentDashboardStats" :key="card.label" class="gov-code-stat-card">
              <img :src="card.icon" alt="" aria-hidden="true" draggable="false" />
              <div>
                <span>{{ card.label }}</span>
                <p><strong class="metric-count-up">{{ formatAnimatedMetricValue(card.value) }}</strong><small>{{ card.unit }}</small></p>
                <footer>较昨日 <b>{{ card.trend }}</b><em>↑ {{ card.rate }}</em></footer>
              </div>
            </article>
          </section>

          <section class="gov-code-main-grid">
            <article class="gov-code-panel gov-code-map-panel">
              <header class="gov-code-panel-head">
                <div><i></i><h2>供应服务情况</h2></div>
              </header>
              <div class="gov-code-map-scene">
                <img :src="governmentMapSceneUrl" alt="琼州海峡供应服务运行地图" draggable="false" />
              </div>
            </article>

            <aside class="gov-code-rank-stack">
              <article class="gov-code-panel gov-code-rank-panel">
                <header class="gov-code-panel-head">
                  <div><i></i><h2>排名</h2></div>
                  <div class="gov-code-rank-tabs" role="tablist" aria-label="排名切换">
                    <button type="button" :class="{ active: governmentRankMode === 'supplier' }" @click="governmentRankMode = 'supplier'">供货</button>
                    <button type="button" :class="{ active: governmentRankMode === 'barge' }" @click="governmentRankMode = 'barge'">驳船</button>
                  </div>
                </header>
                <div class="gov-code-rank-table">
                  <div v-if="governmentRankMode === 'supplier'" class="gov-code-rank-row is-head"><span>排名</span><span>供货商</span><span>订单执行</span><span>交易金额</span></div>
                  <div v-else class="gov-code-rank-row is-head"><span>排名</span><span>交通艇</span><span>航次</span><span>运输量</span></div>
                  <div v-for="(row, index) in (governmentRankMode === 'supplier' ? governmentSupplierRankings : governmentBargeRankings)" :key="`${governmentRankMode}-${row[0]}`" class="gov-code-rank-row">
                    <b :class="{ 'is-top': index < 3 }">{{ index + 1 }}</b><strong>{{ row[0] }}</strong><span>{{ row[1] }}</span><em>{{ row[2] }}</em>
                  </div>
                </div>
              </article>

              <article class="gov-code-panel gov-code-ecosystem-panel">
                <header class="gov-code-panel-head"><div><i></i><h2>生态概况</h2></div></header>
                <div class="gov-code-ecosystem-body">
                  <div class="gov-code-eco-stat is-supplier"><span>供货商</span><strong>312<small>家</small></strong></div>
                  <div class="gov-code-eco-stat is-barge"><span>驳船商</span><strong>28<small>家</small></strong></div>
                </div>
              </article>
            </aside>
          </section>

          <section class="gov-code-bottom-grid">
            <article class="gov-code-panel gov-code-chart-panel">
              <header class="gov-code-panel-head">
                <div><i></i><h2>订单趋势</h2></div>
                <select aria-label="趋势周期"><option>近12个月</option></select>
              </header>
              <div class="gov-code-chart">
                <div class="gov-code-grid-lines" aria-hidden="true"></div>
                <div class="gov-code-chart-axis" aria-hidden="true">
                  <span v-for="tick in governmentAmountTicks" :key="tick">{{ formatGovernmentAmountLabel(tick) }}</span>
                </div>
                <div v-for="item in governmentMonthlyOrders" :key="item.label" class="gov-code-chart-column">
                  <b>{{ formatGovernmentMonthlyAmount(item.amount) }}</b>
                  <span :style="{ height: getGovernmentAmountBarHeight(item.amount) }"></span>
                  <em>{{ item.orderCount }}单</em>
                  <small :title="item.label">{{ item.shortLabel }}</small>
                </div>
              </div>
              <div class="gov-code-chart-legend"><span>交易金额（万）</span><span>订单执行量（单）</span></div>
            </article>

            <article class="gov-code-panel gov-code-refund-panel">
              <header class="gov-code-panel-head">
                <div><i></i><h2>退税金额（万元）</h2></div>
                <button type="button" class="gov-code-detail-link">查看明细 ›</button>
              </header>
              <div class="gov-code-refund-hero" :style="{ backgroundImage: `linear-gradient(90deg, rgba(238, 248, 255, 0.98) 0%, rgba(238, 248, 255, 0.82) 48%, rgba(238, 248, 255, 0.08) 100%), url(${govRefundScene})` }">
                <div><strong>1,268.40</strong><p>较昨日 +126.80 <b>↑ 11.12%</b></p></div>
              </div>
              <div class="gov-code-refund-breakdown">
                <span>本月累计<b>1,268.40</b><small>同比 +11.12%</small></span>
                <span>本年累计<b>6,842.30</b><small>同比 +9.45%</small></span>
                <span>可退税金额<b>532.10</b><small>待审核 26.30</small></span>
              </div>
            </article>

          </section>
        </div>
      </section>

      <section v-else-if="pageKey === 'dashboard'" class="dashboard-workspace dashboard-today-board">
        <div class="dashboard-board-shell">
          <div v-if="dashboardError" class="dashboard-data-alert">{{ dashboardError }}</div>

          <section class="dashboard-summary-grid" aria-label="今日汇总">
            <article
              v-for="card in dashboardMetricCards"
              :key="card.key"
              :class="['dashboard-summary-card', `is-${card.key}`]"
              tabindex="0"
              @click="navigateDashboardMetric(card.path)"
              @keydown.enter.prevent="navigateDashboardMetric(card.path)"
            >
              <span class="dashboard-summary-badge" aria-hidden="true"></span>
              <div class="dashboard-summary-copy">
                <strong>{{ card.title }}</strong>
                <div>
                  <em class="metric-count-up">{{ formatAnimatedMetricValue(card.value) }}</em>
                  <small v-if="card.unit">{{ card.unit }}</small>
                </div>
                <p v-if="card.trend !== 'none'">
                  较昨日
                  <b :class="card.trend === 'up' ? 'is-up' : 'is-down'">{{ card.trend === "up" ? "↑" : "↓" }} {{ card.delta }} {{ card.unit }}</b>
                </p>
                <p v-else>{{ card.note }}</p>
              </div>
            </article>
          </section>

          <div class="dashboard-command-divider" aria-hidden="true">
            <span></span>
            <i></i>
            <span></span>
          </div>

          <section class="dashboard-vessel-board">
            <div class="dashboard-section-title">
              <h2>需供船舶</h2>
              <div class="dashboard-vessel-tools">
                <select aria-label="锚区"><option>全部锚区</option></select>
                <select aria-label="状态"><option>全部状态</option></select>
                <input type="search" placeholder="搜索船名 / IMO / 港区" />
              </div>
            </div>
            <div class="dashboard-vessel-carousel">
              <button type="button" class="dashboard-vessel-nav is-prev" aria-label="上一组船舶" @click="scrollDashboardVessels('prev')">‹</button>
              <div ref="dashboardVesselScroller" class="dashboard-vessel-grid">
              <article
                v-for="ship in dashboardShipCards"
                :key="ship.key"
                :class="['dashboard-vessel-card', { 'is-selected': ship.row.purchaseOrderId === dashboardPrimaryOrder?.purchaseOrderId }]"
                tabindex="0"
                @click="selectedDashboardPurchaseOrderId = ship.row.purchaseOrderId"
                @keydown.enter.prevent="selectedDashboardPurchaseOrderId = ship.row.purchaseOrderId"
              >
                <div
                  class="dashboard-vessel-photo"
                  :style="{ backgroundImage: `url(${dashboardShipSpriteUrl})`, backgroundPosition: ship.imagePosition }"
                >
                  <span>{{ ship.chip }}</span>
                  <b>{{ ship.distance }}</b>
                  <div>
                    <strong>{{ ship.vesselName }}</strong>
                    <small>{{ ship.supplyPort }}</small>
                    <em>{{ ship.vesselEta }}</em>
                  </div>
                </div>
                <div class="dashboard-vessel-stats">
                  <div>
                    <span>SKU</span>
                    <strong>{{ ship.serviceText }}</strong>
                    <b>{{ ship.serviceRate }}%</b>
                    <i :style="{ width: `${ship.serviceRate}%` }"></i>
                  </div>
                  <div>
                    <span>备货情况</span>
                    <strong>{{ ship.supplierText }}</strong>
                    <b>{{ ship.supplierRate }}%</b>
                    <i :style="{ width: `${ship.supplierRate}%` }"></i>
                  </div>
                </div>
                <footer>
                  <strong><span>{{ ship.status }}</span></strong>
                  <button type="button" @click.stop="router.push(`/orders/${ship.row.purchaseOrderId}`)">→</button>
                </footer>
              </article>
              </div>
              <button type="button" class="dashboard-vessel-nav is-next" aria-label="下一组船舶" @click="scrollDashboardVessels('next')">›</button>
            </div>
            <div v-if="dashboardLoading" class="dashboard-loading-tip">正在加载真实业务数据...</div>
          </section>

          <section class="dashboard-operations-grid">
            <article class="dashboard-execution-card">
              <div class="dashboard-section-title">
                <h2>执行订单</h2>
                <button type="button" @click="dashboardPrimaryOrder?.purchaseOrderId ? router.push(`/orders/${dashboardPrimaryOrder.purchaseOrderId}`) : router.push('/orders')">查看订单详情</button>
              </div>
              <div class="dashboard-order-strip">
                <div class="dashboard-order-no">
                  <span>PO</span>
                  <strong>{{ dashboardPrimaryOrderMeta.orderNo }}</strong>
                </div>
                <div class="dashboard-order-supply"><span>联系人/电话</span><strong>{{ dashboardPrimaryOrderMeta.contact }}</strong></div>
                <StatusBadge class="dashboard-order-status" :label="dashboardPrimaryOrderMeta.status" variant="info" />
              </div>
              <div class="dashboard-progress-card">
                <h3>执行进度</h3>
                <div class="dashboard-progress-line">
                  <div
                    v-for="(label, index) in purchaseOrderQueryStageLabels"
                    :key="label"
                    :class="['dashboard-progress-node', `is-${dashboardPrimaryOrder ? purchaseOrderQueryStageState(dashboardPrimaryOrder, index) : 'todo'}`]"
                  >
                    <i>{{ index + 1 }}</i>
                    <strong>{{ label }}</strong>
                    <span>{{ dashboardStageTime(dashboardPrimaryOrder, index) }}</span>
                  </div>
                </div>
              </div>
            </article>

            <aside class="dashboard-support-card">
              <div class="dashboard-section-title">
                <h2>支持与预警</h2>
                <button type="button">查看全部</button>
              </div>
              <div class="dashboard-warning-list">
                <div v-for="item in dashboardSupportWarnings" :key="item.title">
                  <span aria-hidden="true"></span>
                  <strong>{{ item.title }}</strong>
                  <small>{{ item.detail }}</small>
                  <b>{{ item.count }}</b>
                </div>
              </div>
              <div class="dashboard-quick-actions">
                <button type="button" @click="router.push('/procurement/materials')">新建订单</button>
                <button type="button" @click="router.push('/traffic-boat/my-services')">驳船调度</button>
                <button type="button" @click="router.push('/orders')">物料跟踪</button>
                <button type="button" @click="router.push('/settlements')">对账结算</button>
              </div>
            </aside>
          </section>
        </div>
      </section>

      <section v-else-if="false" class="dashboard-workspace">
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
                <template #cell-cnCode="{ value }">
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
                      <span>{{ t("field.cnCode") }}</span>
                      <strong>{{ formatEmpty(row.cnCode) }}</strong>
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

      <ExpandablePanel v-else-if="pageKey === 'suppliers'" :show-header="false" class="basic-suppliers-panel">
        <section class="filter-toolbar">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>{{ t("filter.supplier") }}</span>
              <input v-model="supplierInfoKeyword" :placeholder="t('filter.placeholderSupplier')" @keyup.enter="loadSupplierInfoRows" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.port") }}</span>
              <input v-model="supplierInfoPort" placeholder="Zhoushan / Ningbo" @keyup.enter="loadSupplierInfoRows" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.category") }}</span>
              <input v-model="supplierInfoCategory" :placeholder="t('filter.placeholderKeyword')" @keyup.enter="loadSupplierInfoRows" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.qualification") }}</span>
              <select v-model="supplierInfoStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="active">{{ t("status.active") }}</option>
                <option value="warning">{{ t("status.warning") }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" :label="t('common.search')" :loading="supplierInfoLoading" @click="loadSupplierInfoRows" />
            <IconButton icon="X" :label="t('action.reset')" @click="resetSupplierInfoFilters" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="supplierInfoLoading" @click="loadSupplierInfoRows" />
            <IconButton icon="Download" :label="t('action.export')" @click="runAction" />
            <IconButton icon="Plus" :label="t('action.add')" @click="runAction" />
          </div>
        </section>
        <p v-if="supplierInfoErrorMessage" class="permission-static-notice is-error">{{ supplierInfoErrorMessage }}</p>
        <DataTable class="basic-suppliers-table" :columns="supplierColumns" :rows="supplierInfoRows" :loading="supplierInfoLoading" @row-click="(row) => openSimpleDetail(String(row.name), String(row.port))">
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

      <ExpandablePanel v-else-if="pageKey === 'supplierProducts'" :show-header="false" :show-expand="false" class="supplier-products-panel">
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

          <div :class="['shop-list-card', { 'is-section-fullscreen': shopListFullscreen }]" :aria-busy="shopSkuListLoading || companyQualificationLoading || companyContactLoading || companyVesselLoading || companyValueAddedServiceLoading || trafficBoatLoading">
            <div class="shop-management-bar">
              <div class="shop-management-tabs" role="tablist">
                <button type="button" :class="{ active: shopManagementTab === 'products' }" role="tab" :aria-selected="shopManagementTab === 'products'" @click="shopManagementTab = 'products'">
                  {{ t("page.supplierProducts.skuList") }}
                </button>
                <button type="button" :class="{ active: shopManagementTab === 'qualifications' }" role="tab" :aria-selected="shopManagementTab === 'qualifications'" @click="shopManagementTab = 'qualifications'">
                  {{ t("page.supplierProducts.qualification.title") }}
                </button>
                <button type="button" :class="{ active: shopManagementTab === 'contacts' }" role="tab" :aria-selected="shopManagementTab === 'contacts'" @click="shopManagementTab = 'contacts'">
                  {{ t("page.supplierProducts.contact.title") }}
                </button>
                <button type="button" :class="{ active: shopManagementTab === 'vessels' }" role="tab" :aria-selected="shopManagementTab === 'vessels'" @click="shopManagementTab = 'vessels'">
                  {{ t("page.supplierProducts.vessel.title") }}
                </button>
                <button type="button" :class="{ active: shopManagementTab === 'valueAddedServices' }" role="tab" :aria-selected="shopManagementTab === 'valueAddedServices'" @click="shopManagementTab = 'valueAddedServices'">
                  增值服务维护
                </button>
                <button type="button" :class="{ active: shopManagementTab === 'trafficService' }" role="tab" :aria-selected="shopManagementTab === 'trafficService'" @click="shopManagementTab = 'trafficService'">
                  {{ t("page.supplierProducts.trafficService.title") }}
                </button>
              </div>
              <div v-if="shopManagementTab === 'products'" class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('page.supplierProducts.contact.add')" @click="openCompanyContactCreateFromAnyTab" />
                <IconButton class="shop-bulk-shelf-button is-on" icon="Check" :label="t('page.supplierProducts.actionAllOnShelf')" :disabled="!hasSavedShopSkuRows || shopSaving" @click="updateAllShopShelfStatus('ON_SHELF')" />
                <IconButton class="shop-bulk-shelf-button is-off" icon="Ban" :label="t('page.supplierProducts.actionAllOffShelf')" :disabled="!hasSavedShopSkuRows || shopSaving" @click="updateAllShopShelfStatus('OFF_SHELF')" />
                <IconButton icon="Upload" :label="t('page.supplierProducts.importButton')" @click="openShopImportPicker" />
                <IconButton icon="Save" :label="t('common.save')" :disabled="!canConfirmShopImport" :loading="shopSaving" @click="confirmShopImportPreview" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="shopSkuListLoading" @click="refreshShopSkuList" />
                <IconButton :icon="shopListFullscreen ? 'Minimize2' : 'Maximize2'" :label="shopListFullscreen ? t('page.supplierProducts.exitSectionFullscreen') : t('page.supplierProducts.enterSectionFullscreen')" @click="toggleShopListFullscreen" />
              </div>
              <div v-else-if="shopManagementTab === 'qualifications'" class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('page.supplierProducts.qualification.add')" @click="openCompanyQualificationCreate" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="companyQualificationLoading" @click="loadCompanyQualifications" />
              </div>
              <div v-else-if="shopManagementTab === 'contacts'" class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('page.supplierProducts.contact.add')" :disabled="companyContactSaving" @click="addCompanyContact" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="companyContactLoading" @click="loadCompanyContacts" />
              </div>
              <div v-else-if="shopManagementTab === 'vessels'" class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('page.supplierProducts.vessel.add')" :disabled="companyVesselSaving" @click="addCompanyVessel" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="companyVesselLoading" @click="loadCompanyVessels" />
              </div>
              <div v-else-if="shopManagementTab === 'valueAddedServices'" class="toolbar-icon-actions">
                <IconButton icon="Save" :label="t('common.save')" :loading="companyValueAddedServiceSaving" @click="saveCompanyValueAddedServices" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="companyValueAddedServiceLoading" @click="loadCompanyValueAddedServices" />
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
            <div v-show="shopManagementTab === 'contacts'" class="shop-management-pane">
              <DataTable :columns="companyContactColumns" :rows="companyContactRows" row-key="id" :empty-label="t('page.supplierProducts.contact.empty')">
                <template #cell-contactName="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.contactName" class="shop-edit-control" :placeholder="t('page.supplierProducts.contact.namePlaceholder')" @click.stop />
                  <span v-else>{{ row.contactName || "-" }}</span>
                </template>
                <template #cell-contactPhone="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.contactPhone" class="shop-edit-control" :placeholder="t('page.supplierProducts.contact.phonePlaceholder')" @click.stop />
                  <span v-else>{{ row.contactPhone || "-" }}</span>
                </template>
                <template #cell-contactEmail="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.contactEmail" class="shop-edit-control" :placeholder="t('page.supplierProducts.contact.emailPlaceholder')" @click.stop />
                  <span v-else>{{ row.contactEmail || "-" }}</span>
                </template>
                <template #cell-updatedAt="{ value }">{{ value || "-" }}</template>
                <template #cell-operation="{ row }">
                  <div class="icon-action-row">
                    <IconButton v-if="row.isEditing" icon="Save" :label="t('common.save')" :loading="companyContactSaving" @click.stop="saveCompanyContact(row)" />
                    <IconButton v-if="row.isEditing" icon="X" :label="t('common.cancel')" :disabled="companyContactSaving" @click.stop="cancelCompanyContactEdit(row)" />
                    <IconButton v-if="!row.isEditing" icon="Pencil" :label="t('action.edit')" :disabled="companyContactSaving" @click.stop="editCompanyContact(row)" />
                    <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" :disabled="companyContactSaving" @click.stop="removeCompanyContact(row)" />
                  </div>
                </template>
              </DataTable>
              <div v-if="companyContactLoading" class="shop-panel-loading" role="status" aria-live="polite">
                <span class="loading-spinner" aria-hidden="true"></span>
                <strong>{{ t("common.loading") }}</strong>
              </div>
            </div>
            <div v-show="shopManagementTab === 'vessels'" class="shop-management-pane">
              <DataTable :columns="companyVesselColumns" :rows="companyVesselRows" row-key="id" :empty-label="t('page.supplierProducts.vessel.empty')">
                <template #cell-vesselName="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.vesselName" class="shop-edit-control" :placeholder="t('page.supplierProducts.vessel.namePlaceholder')" @click.stop />
                  <span v-else>{{ row.vesselName || "-" }}</span>
                </template>
                <template #cell-vesselType="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.vesselType" class="shop-edit-control" :placeholder="t('page.supplierProducts.vessel.typePlaceholder')" @click.stop />
                  <span v-else>{{ row.vesselType || "-" }}</span>
                </template>
                <template #cell-buildDate="{ row }">
                  <StableDateTimeInput v-if="row.isEditing" v-model="row.draft.buildDate" mode="date" />
                  <span v-else>{{ row.buildDate || "-" }}</span>
                </template>
                <template #cell-nextMaintenanceDate="{ row }">
                  <StableDateTimeInput v-if="row.isEditing" v-model="row.draft.nextMaintenanceDate" mode="date" />
                  <span v-else>{{ row.nextMaintenanceDate || "-" }}</span>
                </template>
                <template #cell-capacity="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.capacity" class="shop-edit-control" :placeholder="t('page.supplierProducts.vessel.capacityPlaceholder')" @click.stop />
                  <span v-else>{{ row.capacity || "-" }}</span>
                </template>
                <template #cell-remark="{ row }">
                  <input v-if="row.isEditing" v-model="row.draft.remark" class="shop-edit-control" :placeholder="t('field.remark')" @click.stop />
                  <span v-else>{{ row.remark || "-" }}</span>
                </template>
                <template #cell-operation="{ row }">
                  <div class="icon-action-row">
                    <IconButton v-if="row.isEditing" icon="Save" :label="t('common.save')" :loading="companyVesselSaving" @click.stop="saveCompanyVessel(row)" />
                    <IconButton v-if="row.isEditing" icon="X" :label="t('common.cancel')" :disabled="companyVesselSaving" @click.stop="cancelCompanyVesselEdit(row)" />
                    <IconButton v-if="!row.isEditing" icon="Pencil" :label="t('action.edit')" :disabled="companyVesselSaving" @click.stop="editCompanyVessel(row)" />
                    <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" :disabled="companyVesselSaving" @click.stop="removeCompanyVessel(row)" />
                  </div>
                </template>
              </DataTable>
              <div v-if="companyVesselLoading" class="shop-panel-loading" role="status" aria-live="polite">
                <span class="loading-spinner" aria-hidden="true"></span>
                <strong>{{ t("common.loading") }}</strong>
              </div>
            </div>
            <div v-show="shopManagementTab === 'valueAddedServices'" class="shop-management-pane company-value-service-pane">
              <section class="company-value-service-card">
                <div class="company-value-service-head">
                  <div>
                    <strong>增值服务费用</strong>
                    <span>驳船公司和供货商都从企业维护中读取这些费用。</span>
                  </div>
                </div>
                <div class="company-value-service-grid">
                  <label>
                    <span>运维</span>
                    <input v-model="companyValueAddedServiceForm.freightPrice" type="text" inputmode="decimal" placeholder="0.00" />
                  </label>
                  <label>
                    <span>报关费</span>
                    <input v-model="companyValueAddedServiceForm.customsPrice" type="text" inputmode="decimal" placeholder="0.00" />
                  </label>
                  <label>
                    <span>吊机费</span>
                    <input v-model="companyValueAddedServiceForm.cranePrice" type="text" inputmode="decimal" placeholder="0.00" />
                  </label>
                  <label class="company-value-service-remark">
                    <span>备注</span>
                    <textarea v-model="companyValueAddedServiceForm.remark" rows="3" placeholder="可填写费用说明"></textarea>
                  </label>
                </div>
              </section>
              <div v-if="companyValueAddedServiceLoading" class="shop-panel-loading" role="status" aria-live="polite">
                <span class="loading-spinner" aria-hidden="true"></span>
                <strong>{{ t("common.loading") }}</strong>
              </div>
            </div>
            <div v-show="shopManagementTab === 'trafficService'" class="shop-management-pane traffic-price-pane">
              <section class="shop-list-toolbar traffic-price-toolbar">
                <label class="shop-filter-field">
                  <span>{{ t("trafficService.field.seaArea") }}</span>
                  <select v-model="trafficBoatSeaArea">
                    <option value="">{{ t("common.all") }}</option>
                    <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                    <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                  </select>
                </label>
                <div class="toolbar-icon-actions">
                  <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficBoatPrices" />
                  <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficBoatSearch" />
                  <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficBoatLoading" @click="loadTrafficBoatPrices" />
                  <IconButton icon="Save" :label="t('common.save')" :loading="trafficBoatSavingAll" @click="saveAllTrafficBoatPrices" />
                </div>
              </section>
              <p v-if="trafficBoatNoticeKey" class="permission-static-notice">{{ t(trafficBoatNoticeKey) }}</p>
              <div v-if="trafficBoatErrorKey" class="inline-error inline-error--action">
                <span>{{ t(trafficBoatErrorKey) }}</span>
                <button type="button" @click="loadTrafficBoatPrices">{{ t("purchaseOrder.action.retry") }}</button>
              </div>
              <DataTable :columns="trafficBoatEnterpriseColumns" :rows="trafficBoatTableRows" :loading="trafficBoatLoading" row-key="anchorageCode" :empty-label="t('trafficService.emptyPrice')">
                <template #cell-seaArea="{ value }">{{ seaAreaLabel(String(value || '')) }}</template>
                <template #cell-basePrice="{ row }">
                  <input v-model.number="row.basePrice" class="traffic-price-input" type="number" min="0" step="0.01" />
                </template>
                <template #cell-sharedPrice="{ row }">
                  <input v-model.number="row.sharedPrice" class="traffic-price-input" type="number" min="0" step="0.01" />
                </template>
                <template #cell-enabled="{ row }">
                  <input v-model="row.enabled" type="checkbox" />
                </template>
                <template #cell-remark="{ row }">
                  <input v-model="row.remark" class="traffic-remark-input" :placeholder="t('field.remark')" />
                </template>
              </DataTable>
              <section v-if="false" class="traffic-shuttle-admin">
                <header class="traffic-shuttle-admin__head">
                  <div>
                    <strong>{{ t("trafficMarketplace.shuttleAdminTitle") }}</strong>
                    <span>{{ t("trafficMarketplace.shuttleAdminSubtitle") }}</span>
                  </div>
                  <div class="toolbar-icon-actions">
                    <IconButton icon="Plus" :label="t('trafficMarketplace.action.createShuttle')" variant="primary" @click="openTrafficShuttleDialog" />
                    <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficShuttleLoading" @click="loadTrafficShuttles(true)" />
                  </div>
                </header>
                <DataTable :columns="trafficShuttleColumns" :rows="trafficShuttleTableRows" :loading="trafficShuttleLoading" row-key="shuttleId" :empty-label="t('trafficMarketplace.emptyShuttles')">
                  <template #cell-route="{ row }">{{ trafficShuttleRouteLabel(row) }}</template>
                  <template #cell-startTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
                  <template #cell-returnTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
                  <template #cell-sharedPrice="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
                  <template #cell-status="{ value }"><StatusBadge :label="trafficShuttleStatusLabel(String(value || ''))" variant="info" /></template>
                  <template #cell-operation="{ row }">
                    <IconButton icon="X" :label="t('trafficMarketplace.action.closeShuttle')" variant="danger" :loading="trafficShuttleSaving" @click.stop="closeShuttle(row)" />
                  </template>
                </DataTable>
              </section>
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
              <span>检索</span>
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="materialDemandKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadMaterialDemands" />
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
              <span>检索</span>
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="inquiryDemandKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadInquiryDemands" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.inquiries.dateFrom") }}</span>
              <StableDateTimeInput v-model="inquiryDemandDateFrom" mode="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.inquiries.dateTo") }}</span>
              <StableDateTimeInput v-model="inquiryDemandDateTo" mode="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.status") }}</span>
              <select v-model="inquiryDemandStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="SAVED">{{ t("page.materialDemand.statusSaved") }}</option>
                <option value="COMPARING">{{ t("page.materialDemand.statusComparing") }}</option>
                <option value="ORDERED">{{ t("page.materialDemand.statusOrdered") }}</option>
                <option value="DISCARDED">{{ t("page.materialDemand.statusDiscarded") }}</option>
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
        <div v-if="inquiryDemandLoading" class="empty-state compact">{{ t("common.loading") }}</div>
        <div v-else-if="!inquiryDemandRows.length" class="empty-state compact">{{ inquiryDemandHasFilters ? t("page.inquiries.emptyFiltered") : t("page.inquiries.empty") }}</div>
        <section v-else class="procurement-query-card-list">
          <article v-for="row in inquiryDemandTableRows" :key="Number(row.demandId)" class="procurement-query-card is-inquiry" tabindex="0" @click="openInquiryDemand(row as unknown as MaterialDemandSummary)" @keydown.enter="openInquiryDemand(row as unknown as MaterialDemandSummary)">
            <header class="procurement-query-card__head">
              <div>
                <strong class="vessel-title"><span class="vessel-mini-icon" aria-hidden="true"></span>{{ row.vesselName || "-" }}</strong>
              </div>
              <StatusBadge :label="demandStatusLabel(String(row.status || ''))" :variant="demandStatusVariant(String(row.status || ''))" />
            </header>
            <section class="procurement-query-card__body">
              <div class="procurement-query-card__primary">
                <strong>{{ demandDisplayNo(row) }}</strong>
              </div>
              <div class="procurement-query-card__metrics">
                <div class="metric-sku"><span>SKU</span><strong>{{ row.skuCount || 0 }}</strong></div>
                <div class="metric-exact"><span>精准</span><strong>{{ row.exactCount || 0 }}</strong></div>
                <div class="metric-similar"><span>相似</span><strong>{{ row.similarCount || 0 }}</strong></div>
                <div class="metric-unmatched"><span>未匹配</span><strong>{{ row.unmatchedCount || 0 }}</strong></div>
              </div>
            </section>
            <footer class="procurement-query-card__footer">
              <span>询价日期 {{ row.inquiryDate || "-" }}</span>
              <div class="icon-action-row">
                <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openInquiryDemand(row)" />
                <IconButton icon="Trash2" :label="t('action.discard')" variant="danger" :disabled="!canDiscardStatus(String(row.status || ''))" @click.stop="openDiscardDialog({ type: 'demand', id: String(row.demandId), refresh: loadInquiryDemands })" />
              </div>
            </footer>
          </article>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'comparisonList'" :show-header="false" class="inquiry-management-panel">
        <section class="filter-toolbar" aria-label="comparison filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>检索</span>
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                </svg>
              </span>
              <input v-model="comparisonDemandKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadComparisonDemands" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.compareManagement.dateFrom") }}</span>
              <StableDateTimeInput v-model="comparisonDemandDateFrom" mode="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("page.compareManagement.dateTo") }}</span>
              <StableDateTimeInput v-model="comparisonDemandDateTo" mode="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("filter.status") }}</span>
              <select v-model="comparisonDemandStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="SAVED">{{ t("page.materialDemand.statusSaved") }}</option>
                <option value="COMPARING">{{ t("page.materialDemand.statusComparing") }}</option>
                <option value="ORDERED">{{ t("page.materialDemand.statusOrdered") }}</option>
                <option value="DISCARDED">{{ t("page.materialDemand.statusDiscarded") }}</option>
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
        <div v-if="comparisonDemandLoading" class="empty-state compact">{{ t("common.loading") }}</div>
        <div v-else-if="!comparisonDemandRows.length" class="empty-state compact">{{ comparisonDemandHasFilters ? t("page.compareManagement.emptyFiltered") : t("page.compareManagement.empty") }}</div>
        <section v-else class="procurement-query-card-list">
          <article v-for="row in comparisonDemandTableRows" :key="Number(row.demandId)" class="procurement-query-card is-comparison" tabindex="0" @click="openComparisonDemand(row as unknown as MaterialDemandSummary)" @keydown.enter="openComparisonDemand(row as unknown as MaterialDemandSummary)">
            <header class="procurement-query-card__head">
              <div>
                <strong class="vessel-title"><span class="vessel-mini-icon" aria-hidden="true"></span>{{ row.vesselName || "-" }}</strong>
              </div>
              <StatusBadge :label="demandStatusLabel(String(row.status || ''))" :variant="demandStatusVariant(String(row.status || ''))" />
            </header>
            <section class="procurement-query-card__body">
              <div class="procurement-query-card__primary">
                <strong>{{ demandDisplayNo(row) }}</strong>
              </div>
              <div class="procurement-query-card__metrics">
                <div class="metric-sku"><span>SKU</span><strong>{{ row.skuCount || 0 }}</strong></div>
                <div class="metric-exact"><span>精准</span><strong>{{ row.exactCount || 0 }}</strong></div>
                <div class="metric-similar"><span>相似</span><strong>{{ row.similarCount || 0 }}</strong></div>
                <div class="metric-unmatched"><span>未匹配</span><strong>{{ row.unmatchedCount || 0 }}</strong></div>
              </div>
            </section>
            <footer class="procurement-query-card__footer">
              <span>询价日期 {{ row.inquiryDate || "-" }}</span>
              <div class="icon-action-row">
                <IconButton icon="Eye" :label="t('action.compare')" @click.stop="openComparisonDemand(row)" />
                <IconButton icon="Trash2" :label="t('action.discard')" variant="danger" :disabled="!canDiscardStatus(String(row.status || ''))" @click.stop="openDiscardDialog({ type: 'demand', id: String(row.demandId), refresh: loadComparisonDemands })" />
              </div>
            </footer>
          </article>
        </section>
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
          <template #cell-amount="{ value }">楼 {{ value }}</template>
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
        <LoadingOverlay :active="compareLoading" :label="t('common.loading')">
        <article class="compare-supply-card">
          <div class="compare-supply-title">
            <strong>{{ t("compare.mainInfo") }}</strong>
            <StatusBadge :label="compareStatusLabel" :variant="compareStatusVariant" />
          </div>
          <dl>
            <div v-for="item in compareSupplyEditableFields" :key="item.key" class="is-editable">
              <dt>{{ item.label }}</dt>
              <dd>
                <StableDateTimeInput
                  v-if="item.inputType === 'datetime'"
                  v-model="compareSupplyForm[item.key]"
                  :placeholder="item.label"
                  :disabled="isCompareReadonly"
                />
                <input v-else v-model="compareSupplyForm[item.key]" :type="item.inputType" :disabled="isCompareReadonly" />
              </dd>
            </div>
            <div class="is-editable">
              <dt>补给方式</dt>
              <dd>
                <select v-model="compareSupplyForm.supplyMode" :disabled="isCompareReadonly" @change="handleCompareSupplyModeChange">
                  <option value="SEA">海运</option>
                  <option value="LAND">陆运</option>
                </select>
              </dd>
            </div>
            <div class="is-editable">
              <dt>{{ t("page.materials.recipientCompany") }}</dt>
              <dd>
                <input v-model="compareSupplyForm.recipientCompany" type="text" :disabled="isCompareReadonly" />
              </dd>
            </div>
            <div class="is-editable">
              <dt>{{ t("page.materials.handlerContact") }}</dt>
              <dd>
                <input v-model="compareSupplyForm.handlerContact" type="text" :disabled="isCompareReadonly" />
              </dd>
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
                <article :class="['strategy-choice', `is-${strategy.tone}`, { active: selectedStrategy === strategy.key, 'is-disabled': !strategy.enabled, 'is-readonly': isCompareReadonly }]">
                  <button type="button" class="strategy-choice-main" :disabled="!strategy.enabled || isCompareReadonly" @click="selectCompareStrategy(strategy.key)">
                    <span class="strategy-title-stack">
                      <strong>{{ strategy.label }}</strong>
                    </span>
                    <div class="strategy-count-stack">
                      <strong>{{ strategy.matchSummary }}</strong>
                    </div>
                    <div class="strategy-total-stack">
                      <em>{{ t("compare.costAmount") }} {{ strategy.costTotal }}</em>
                      <em>{{ t("compare.fixedFeeTotal") }} {{ strategy.fixedFeeTotal }}</em>
                      <em>{{ t("compare.quoteTotal") }} {{ strategy.quoteTotal }}</em>
                      <em>{{ t("compare.profitAmount") }} {{ strategy.profitTotal }}</em>
                    </div>
                  </button>
                  <ul class="strategy-supplier-list">
                    <li v-for="supplier in strategy.suppliers" :key="supplier.key">
                      <button
                        type="button"
                        :class="{ active: selectedStrategy === strategy.key && selectedCompareSupplier === supplier.key }"
                        :disabled="!strategy.enabled || isCompareReadonly"
                        @click="selectCompareSupplier(strategy.key, supplier.key)"
                      >
                        <span>{{ supplier.supplier }}</span>
                        <span class="strategy-supplier-meta">
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

          <section class="compare-fixed-fee-bar" :aria-label="t('compare.fixedFeeTitle')">
            <label>
              <span>{{ compareProviderLabel }}</span>
              <span class="fixed-fee-input-with-action">
                <input v-model="compareFixedFeeInputs.shuttle" type="text" :placeholder="compareProviderPlaceholder" readonly :disabled="isCompareReadonly" />
                <IconButton icon="Search" :label="compareProviderPlaceholder" :disabled="isCompareReadonly" @click="openCompareFixedProviderDialog" />
              </span>
            </label>
            <label>
              <span>{{ t("compare.fixedFee.freight") }}</span>
              <input v-model="compareFixedFeeInputs.freight" type="text" inputmode="decimal" :disabled="isCompareReadonly" />
            </label>
            <label>
              <span>{{ t("compare.fixedFee.customs") }}</span>
              <input v-model="compareFixedFeeInputs.customs" type="text" inputmode="decimal" :disabled="isCompareReadonly" />
            </label>
            <label>
              <span>{{ t("compare.fixedFee.crane") }}</span>
              <input v-model="compareFixedFeeInputs.crane" type="text" inputmode="decimal" :disabled="isCompareReadonly" />
            </label>
            <label>
              <span>{{ t("compare.fixedFee.other") }}</span>
              <input v-model="compareFixedFeeInputs.other" type="text" inputmode="decimal" :disabled="isCompareReadonly" />
            </label>
            <div>
              <span>{{ t("compare.fixedFeeTotal") }}</span>
              <strong>{{ formatCompareMoney(compareFixedFeeTotal, compareFixedFeeTotalUsd) }}</strong>
            </div>
          </section>

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
                :disabled="isCompareReadonly"
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
            <label class="compare-markup-field">
              <span>{{ t("compare.quoteMarkupPercent") }}</span>
              <input :value="compareQuoteMarkupPercent" type="text" inputmode="decimal" :disabled="isCompareReadonly" @input="updateCompareGlobalMarkup(($event.target as HTMLInputElement).value)" />
            </label>
            <div class="toolbar-icon-actions">
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :disabled="compareLoading" @click="loadCompareWorkspace" />
              <IconButton icon="Save" :label="t('compare.saveQuote')" :disabled="compareLoading || compareQuoteSaving || isCompareReadonly || !selectedCompareOrderableRows.length" @click="() => saveCompareQuotePrices()" />
              <IconButton icon="Download" :label="t('compare.exportQuote')" :disabled="compareLoading || compareQuoteExporting" @click="exportCompareQuotePrices" />
              <IconButton icon="Upload" label="导入" :disabled="compareLoading || compareQuoteImporting || isCompareReadonly" @click="openCompareQuoteImport" />
              <IconButton icon="Send" :label="t('purchaseOrder.action.confirmOrder')" variant="primary" :disabled="compareLoading || isCompareReadonly || !selectedCompareOrderableRows.length" @click="openPurchaseOrderDialog" />
              <IconButton v-if="compareData?.existingPurchaseOrderId" icon="Eye" :label="t('action.viewDetail')" @click="router.push(`/orders/${compareData.existingPurchaseOrderId}`)" />
              <input ref="compareQuoteImportInput" class="visually-hidden-input" type="file" accept=".xlsx" @change="handleCompareQuoteImport" />
            </div>
          </div>
          <p v-if="compareQuoteNotice" class="inline-success">{{ compareQuoteNotice }}</p>
          <p v-if="compareQuoteError" class="inline-error">{{ compareQuoteError }}</p>
          <p v-if="purchaseOrderNotice" class="inline-success">{{ purchaseOrderNotice }}</p>
          <p v-if="purchaseOrderError" class="inline-error">{{ purchaseOrderError }}</p>

          <DataTable
            :columns="compareSkuColumns"
            :rows="compareFilteredSkus"
            :loading="false"
            :empty-label="compareError ? t('compare.loadFailed') : t('compare.noCandidates')"
            :show-index="false"
            row-key="id"
            row-interactive
            :row-class="compareSkuRowClass"
            :expanded-row-key="expandedCompareRowId"
            @row-click="(row) => { expandedCompareRowId = expandedCompareRowId === String(row.id) ? '' : String(row.id); }"
          >
            <template #head-selection>
              <label class="compare-row-select compare-row-select--head" @click.stop>
                <input
                  type="checkbox"
                  :checked="compareAllVisibleSelected"
                  :disabled="isCompareReadonly || !compareFilteredSkus.some(isCompareRowOrderable)"
                  :aria-label="t('compare.selectAllVisible')"
                  @change="toggleAllVisibleCompareRows"
                />
                <span>{{ t("table.index") }}</span>
              </label>
            </template>
            <template #cell-selection="{ row }">
              <label class="compare-row-select" @click.stop>
                <input type="checkbox" :checked="selectedCompareRowIds.includes(row.id) && isCompareRowOrderable(row)" :disabled="isCompareReadonly || !isCompareRowOrderable(row)" @change.stop="toggleCompareRowSelection(row)" />
                <span>{{ row.displayNo || "-" }}</span>
              </label>
            </template>
            <template #cell-sourceSkuCode="{ row, value }">
              <span class="compare-two-line-cell">
                <strong>{{ value || "-" }}</strong>
                <small>{{ row.sourceSkuName || "-" }}</small>
              </span>
            </template>
            <template #cell-matchedProductCode="{ row }">
              <span class="compare-two-line-cell">
                <strong>{{ row.matchedProductCode || "-" }}</strong>
                <small>{{ row.name || "-" }}</small>
              </span>
            </template>
            <template #cell-platformUnit="{ row }">
              <span v-if="row.unmatched">-</span>
              <select
                v-else-if="row.unitPriceOptions?.length"
                class="compare-unit-select"
                :value="row.selectedUnit || row.platformUnit"
                :disabled="isCompareReadonly"
                @click.stop
                @change="updateCompareUnit(row, ($event.target as HTMLSelectElement).value)"
              >
                <option v-for="option in row.unitPriceOptions" :key="option.unit" :value="option.unit">
                  {{ option.unit }}
                </option>
              </select>
              <span v-else>{{ row.platformUnit || "-" }}</span>
            </template>
            <template #cell-quantity="{ row }">
              <span class="compare-two-line-cell compare-two-line-cell--right">
                <label class="compare-quantity-field" :class="{ 'is-invalid': row.quantityInvalidFlag }" @click.stop>
                  <input
                    :value="row.quantity"
                    type="text"
                    inputmode="decimal"
                    :placeholder="t('compare.quantityPlaceholder')"
                    :disabled="isCompareReadonly"
                    @input="updateCompareQuantity(row, ($event.target as HTMLInputElement).value)"
                  />
                </label>
                <small>{{ row.unit || "-" }}</small>
              </span>
            </template>
            <template #cell-price="{ row }">
              <span v-if="row.unmatched">-</span>
              <span v-else-if="row.price > 0" class="money-stack money-stack--single">
                <strong>{{ formatCompareMoney(row.price, row.unitPriceUsd) }}</strong>
              </span>
              <span v-else>-</span>
              <small v-if="!row.unmatched" class="price-source">{{ row.id === "SKU-150203" ? t("status.formalQuote") : t("status.dailyPrice") }}</small>
            </template>
            <template #cell-quoteMarkup="{ row }">
              <span v-if="row.unmatched">-</span>
              <label v-else class="compare-markup-inline-field" @click.stop>
                <input
                  :value="row.quoteMarkupPercent ?? ''"
                  type="text"
                  inputmode="decimal"
                  :disabled="isCompareReadonly || !isCompareRowOrderable(row)"
                  @input="updateCompareMarkup(row, ($event.target as HTMLInputElement).value)"
                />
              </label>
            </template>
            <template #cell-actualQuote="{ row }">
              <span v-if="row.unmatched">-</span>
              <label v-else class="compare-actual-quote-field" @click.stop>
                <span>{{ compareDisplayCurrencySymbol }}</span>
                <input
                  :value="compareMoneyInputValue(row.actualQuotePrice)"
                  type="number"
                  min="0.0001"
                  step="0.0001"
                  inputmode="decimal"
                  :placeholder="t('compare.actualQuotePrice')"
                  :disabled="isCompareReadonly || !isCompareRowOrderable(row)"
                  @input="updateCompareActualQuote(row, ($event.target as HTMLInputElement).value)"
                />
              </label>
            </template>
            <template #cell-subtotal="{ row }">
              <span v-if="row.unmatched">-</span>
              <span v-else-if="row.subtotal > 0" class="money-stack money-stack--single">
                <strong>{{ formatCompareMoney(row.subtotal, row.subtotalUsd) }}</strong>
              </span>
              <span v-else>-</span>
            </template>
            <template #cell-operation="{ row }">
              <button class="replace-product-button" type="button" :disabled="isCompareReadonly" :aria-label="t('compare.replaceProduct')" :title="t('compare.replaceProduct')" @click.stop="openReplacementDialog(row)">
                {{ t("compare.replaceShort") }}
              </button>
            </template>
            <template #expanded-row="{ row, expanded }">
              <section v-if="expanded" class="compare-row-detail" @click.stop>
                <section>
                  <h3>{{ t("compare.sourceSkuName") }}</h3>
                  <div class="sku-comparison-table">
                    <div class="sku-comparison-head">
                      <span>{{ t("page.materials.field") }}</span>
                      <span>{{ t("page.materials.sourceValue") }}</span>
                      <span>{{ t("page.materials.selectedCandidate") }}</span>
                    </div>
                    <div class="sku-comparison-row">
                      <span>{{ t("compare.sourceSkuCode") }}</span>
                      <strong>{{ row.sourceSkuCode || "-" }}</strong>
                      <em>{{ row.unmatched ? "-" : row.matchedProductCode || "-" }}</em>
                    </div>
                    <div class="sku-comparison-row">
                      <span>{{ t("compare.sourceSkuName") }}</span>
                      <strong>{{ row.sourceSkuName || "-" }}</strong>
                      <em>{{ row.unmatched ? "-" : row.name || "-" }}</em>
                    </div>
                    <div class="sku-comparison-row">
                      <span>{{ t("attr.spec") }}</span>
                      <strong>{{ row.quantity || "-" }} / {{ row.unit || "-" }}</strong>
                      <em>{{ row.unmatched ? "-" : row.attributes?.[0]?.value || "-" }}</em>
                    </div>
                  </div>
                </section>
                <section class="compare-row-remark-panel">
                  <h3>{{ t("compare.remarks") }}</h3>
                  <textarea
                    :value="row.remarks || ''"
                    rows="5"
                    :placeholder="t('compare.remarksPlaceholder')"
                    :disabled="isCompareReadonly"
                    @input="updateCompareRemark(row, ($event.target as HTMLTextAreaElement).value)"
                  ></textarea>
                </section>
              </section>
            </template>
          </DataTable>
        </article>
        </LoadingOverlay>
        <Teleport to="body">
          <IconButton
            v-if="showCompareBackTop"
            class="compare-back-top-button"
            icon="ArrowUp"
            :label="t('action.backToTop')"
            @click="scrollCompareToTop"
          />
        </Teleport>
      </section>

      <ExpandablePanel v-else-if="pageKey === 'orders' || pageKey === 'supplierOrders'" :show-header="false" class="inquiry-management-panel purchase-orders-panel">
        <template v-if="isPurchaseOrderDetailPage">
          <div v-if="purchaseOrderDetailError" class="inline-error inline-error--action">
            <span>{{ purchaseOrderDetailError }}</span>
            <button type="button" @click="loadPurchaseOrderDetail">{{ t("purchaseOrder.action.retry") }}</button>
          </div>
          <p v-if="purchaseOrderDetailNotice" class="permission-static-notice">{{ purchaseOrderDetailNotice }}</p>
          <div v-if="purchaseOrderDetailLoading" class="compare-state-message">{{ t("common.loading") }}</div>
          <section v-else-if="purchaseOrderDetail" ref="purchaseOrderDetailPanelRef" class="purchase-order-detail">
            <div v-if="purchaseOrderDetailReminding" class="purchase-order-remind-mask">
              {{ t("purchaseOrder.notice.remindSending") }}
            </div>
            <article class="purchase-order-basic-card">
              <header>
                <div class="purchase-order-basic-card__title">
                  <strong>{{ displayOrderText(purchaseOrderDetail.order.vesselName) }}</strong>
                  <small>{{ purchaseOrderDetail.order.purchaseOrderNo || "-" }}</small>
                </div>
                <div class="purchase-order-basic-card__right">
                  <div v-if="purchaseOrderWorkspaceMode === 'supplier'" class="purchase-order-basic-card__actions purchase-order-basic-card__actions--supplier">
                    <IconButton
                      v-if="supplierDetailPendingOrder"
                      icon="Check"
                      label="确认"
                      :disabled="supplierActionSaving"
                      :loading="supplierActionSaving"
                      @click="supplierDetailPendingOrder && openSupplierActionDialog(supplierDetailPendingOrder, 'confirm')"
                    />
                    <IconButton
                      v-if="supplierDetailPreparingOrder"
                      icon="Save"
                      label="备货"
                      :disabled="supplierActionSaving"
                      :loading="supplierActionSaving"
                      @click="supplierDetailPreparingOrder && markSupplierReadyAction(supplierDetailPreparingOrder)"
                    />
                    <IconButton
                      v-if="supplierDetailShippingOrder"
                      icon="Send"
                      label="发货"
                      :disabled="supplierActionSaving"
                      :loading="supplierActionSaving"
                      @click="supplierDetailShippingOrder && markSupplierSuppliedAction(supplierDetailShippingOrder)"
                    />
                    <IconButton
                      v-if="supplierDetailTransportOrder"
                      icon="Check"
                      label="运输完成"
                      :disabled="supplierActionSaving"
                      :loading="supplierActionSaving"
                      @click="supplierDetailTransportOrder && markSupplierWaitingSupplyAction(supplierDetailTransportOrder)"
                    />
                  </div>
                </div>
              </header>
              <dl v-if="purchaseOrderWorkspaceMode === 'buyer'">
                <div>
                  <dt>补给港口</dt>
                  <dd>{{ displayOrderText(displaySupplyPort(purchaseOrderDetail.order.supplyPort)) }}</dd>
                </div>
                <div>
                  <dt>预靠港时间</dt>
                  <dd>{{ purchaseOrderDetail.order.vesselEta || "-" }}</dd>
                </div>
                <div>
                  <dt>要求送达时间</dt>
                  <dd>{{ purchaseOrderDetail.order.requiredDeliveryTime || "-" }}</dd>
                </div>
                <div>
                  <dt>收报单位</dt>
                  <dd>{{ displayOrderText(purchaseOrderDetail.order.recipientCompany) }}</dd>
                </div>
                <div>
                  <dt>联系人</dt>
                  <dd>{{ displayOrderText(purchaseOrderDetail.order.deliveryContactName) }}</dd>
                </div>
                <div>
                  <dt>联系电话</dt>
                  <dd>{{ purchaseOrderDetail.order.deliveryContactPhone || "-" }}</dd>
                </div>
                <div>
                  <dt>联系邮箱</dt>
                  <dd>{{ purchaseOrderDetail.order.deliveryContactEmail || "-" }}</dd>
                </div>
                <div>
                  <dt>补给方式</dt>
                  <dd>{{ purchaseDetailSupplyModeIcon }} {{ purchaseDetailSupplyModeLabel }}</dd>
                </div>
              </dl>
            </article>
            <article class="purchase-order-execution-card" aria-label="采购订单执行状态">
              <div class="purchase-order-execution-rail">
                <div v-for="stage in purchaseDetailVisibleExecutionStages" :key="stage.key" :class="['purchase-order-execution-stage__item', `is-${stage.state}`]">
                  <span class="purchase-order-execution-stage__count">{{ stage.progressText }}</span>
                  <i></i>
                  <strong>{{ stage.label }}</strong>
                  <small>日期：{{ stage.dateText }}</small>
                </div>
              </div>
            </article>
            <section v-if="purchaseOrderWorkspaceMode === 'buyer'" class="purchase-order-execution-situation" aria-label="订单执行情况">
              <div class="purchase-order-execution-situation__header">
                <h3>执行情况</h3>
              </div>
              <div class="purchase-order-execution-situation__grid">
                <button
                  v-for="supplier in purchaseDetailSupplierCards"
                  :key="`supplier-execution-${supplier.supplierOrderId}`"
                  type="button"
                  class="purchase-order-execution-situation-card"
                  @click="openSupplierOrderDetail(supplier as unknown as Record<string, unknown>)"
                >
                  <span>供货商</span>
                  <strong>{{ displayOrderText(supplier.supplierName) }}</strong>
                  <StatusBadge :label="supplier.flowStatusText" :variant="supplier.flowStatusVariant" />
                  <button v-if="supplier.attachmentCount" type="button" class="fulfillment-attachment-badge" @click.stop="openFulfillmentDrawer(`${supplier.supplierName} · 运输附件`, fulfillmentAttachments.filter((item) => item.supplierOrderId === supplier.supplierOrderId))">
                    运输附件 {{ supplier.attachmentCount }}
                  </button>
                  <small>时间：{{ supplier.executionTime }}</small>
                </button>
                <button
                  v-if="purchaseDetailBargeExecutionCard"
                  type="button"
                  class="purchase-order-execution-situation-card is-barge"
                  @click="openPurchaseBargeExecutionDrawer"
                >
                  <span>驳船</span>
                  <strong>{{ purchaseDetailBargeExecutionCard.provider }}</strong>
                  <StatusBadge :label="purchaseDetailBargeExecutionCard.statusText" :variant="purchaseDetailBargeExecutionCard.statusVariant" />
                  <span v-if="purchaseDetailBargeExecutionCard.attachmentCount" class="fulfillment-attachment-badge">运输附件 {{ purchaseDetailBargeExecutionCard.attachmentCount }}</span>
                  <small>时间：{{ purchaseDetailBargeExecutionCard.time }}</small>
                </button>
              </div>
            </section>
            <nav class="purchase-order-detail-tabs purchase-order-detail-tabs--inline" aria-label="purchase order follow-up tabs">
              <button type="button" :class="{ active: purchaseOrderDetailTab === 'settlement' }" @click="setPurchaseOrderDetailTab('settlement')">订单结算</button>
              <button type="button" :class="{ active: purchaseOrderDetailTab === 'details' }" @click="setPurchaseOrderDetailTab('details')">订单明细</button>
            </nav>

            <article v-if="purchaseOrderDetailTab === 'details'" class="purchase-order-section">
              <div class="purchase-order-section-header">
                <h3>{{ t("purchaseOrder.section.items") }}</h3>
                <strong v-if="purchaseOrderWorkspaceMode === 'supplier'" class="purchase-order-section-total">
                  {{ t("purchaseOrder.field.total") }} {{ formatPurchaseMoneyValue(supplierVisibleItemTotal) }}
                </strong>
              </div>
              <DataTable :columns="purchaseItemColumns" :rows="purchaseDetailItemRows" row-key="itemId">
                <template #cell-productName="{ value }">
                  <span class="purchase-item-name" :title="String(value || '-')">{{ value || "-" }}</span>
                </template>
                <template #cell-unitPrice="{ row }">
                  <span class="money-stack money-stack--single">
                    <strong>{{ formatPurchaseMoneyValue(Number(row.unitPrice)) }}</strong>
                  </span>
                </template>
                <template #cell-amount="{ row }">
                  <span class="money-stack money-stack--single">
                    <strong>{{ formatPurchaseMoneyValue(Number(row.amount)) }}</strong>
                  </span>
                </template>
                <template #cell-flags="{ row }">
                  <div class="flag-stack">
                    <StatusBadge v-if="row.quantityFallbackFlag" :label="t('purchaseOrder.flag.quantityFallback')" variant="warning" />
                    <span v-if="!row.quantityFallbackFlag">-</span>
                  </div>
                </template>
              </DataTable>
            </article>

            <article v-if="showSupplierCustomsUpload && purchaseOrderDetailTab === 'details'" class="purchase-order-section supplier-qc-section">
              <h3>{{ t("purchaseOrder.section.customsDocuments") }}</h3>
              <p>{{ t("purchaseOrder.notice.customsDocuments") }}</p>
              <label class="supplier-customs-upload">
                <span>{{ supplierCustomsUploading ? t("common.loading") : t("purchaseOrder.action.uploadCustomsDocuments") }}</span>
                <input type="file" multiple @change="handleSupplierCustomsUpload" />
              </label>
              <p v-if="supplierCustomsError" class="qc-error">{{ supplierCustomsError }}</p>
              <div v-if="supplierCustomsFiles.length" class="supplier-customs-file-list">
                <a v-for="file in supplierCustomsFiles" :key="`${file.fileName}-${file.fileUrl}`" :href="file.fileUrl" target="_blank" rel="noreferrer">
                  {{ file.fileName }}
                </a>
              </div>
            </article>

            <article v-if="purchaseOrderWorkspaceMode === 'supplier' && purchaseOrderDetailTab === 'details' && supplierDetailShippingOrder" class="purchase-order-section supplier-qc-section supplier-transport-upload-section">
              <h3>运输附件</h3>
              <p>上传本次运输过程和到船交付证明，完成发货前至少保留一份附件。</p>
              <label class="supplier-customs-upload">
                <span>{{ supplierFulfillmentUploading ? t("common.loading") : "上传运输图片或文件" }}</span>
                <input type="file" multiple accept="image/*,.pdf,.doc,.docx,.xls,.xlsx" :disabled="supplierFulfillmentUploading" @change="uploadSupplierFulfillmentAttachment(supplierDetailShippingOrder, $event)" />
              </label>
              <div v-if="fulfillmentAttachments.filter((item) => item.supplierOrderId === supplierDetailShippingOrder?.supplierOrderId).length" class="supplier-customs-file-list">
                <button
                  v-for="file in fulfillmentAttachments.filter((item) => item.supplierOrderId === supplierDetailShippingOrder?.supplierOrderId)"
                  :key="file.attachmentId"
                  type="button"
                  @click="previewBusinessAttachments('运输附件', [file])"
                >
                  {{ file.fileName }}
                </button>
              </div>
            </article>

            <article v-if="purchaseOrderWorkspaceMode === 'supplier' && purchaseOrderDetailTab === 'settlement'" class="purchase-order-section purchase-order-settlement-section supplier-order-settlement-section">
              <div v-if="!supplierDetailSettlementRows.length" class="empty-state compact">暂无结算单</div>
              <section v-else class="settlement-card-list settlement-card-list--embedded">
                <article v-for="row in supplierDetailSettlementRows" :key="row.settlementId" class="settlement-card is-provider">
                  <header>
                    <div><strong class="settlement-card__no"><span class="settlement-money-icon" aria-hidden="true"></span>{{ row.settlementNo }}</strong><small>{{ row.purchaseOrderNo }}</small></div>
                    <StatusBadge :label="settlementStatusLabel(row.status)" :variant="settlementStatusVariant(row.status)" />
                  </header>
                  <dl>
                    <div><dt>船代公司</dt><dd>{{ row.buyerCompanyName || '-' }}</dd></div>
                    <div><dt>船舶名称</dt><dd>{{ row.vesselName || purchaseOrderDetail.order.vesselName || '-' }}</dd></div>
                    <div><dt>报价金额</dt><dd>{{ formatPurchaseMoneyValue(row.quotedAmount) }}</dd></div>
                    <div><dt>实际金额</dt><dd>{{ row.actualAmount ? formatPurchaseMoneyValue(row.actualAmount) : '--' }}</dd></div>
                  </dl>
                  <footer>
                    <IconButton icon="Eye" label="查看发票" @click="previewBusinessAttachments('发票附件', row.invoiceAttachments || [])" />
                    <IconButton v-if="String(row.status).toUpperCase() === 'PENDING_INVOICE'" icon="Pencil" label="编辑结算" @click="settlementEditingId = row.settlementId" />
                  </footer>
                </article>
              </section>
            </article>

            <article v-if="purchaseOrderWorkspaceMode === 'buyer' && purchaseOrderDetailTab === 'settlement'" class="purchase-order-section purchase-order-settlement-section">
              <div class="purchase-order-section-header">
                <h3>订单结算</h3>
                <div class="purchase-settlement-head-actions">
                  <strong>总计 {{ formatPurchaseMoneyValue(purchaseSettlementTotal) }}</strong>
                  <IconButton icon="Check" label="一键结算" :disabled="!purchaseSettlementSelectedRowIds.length || settlementSaving" :loading="settlementSaving" @click="submitPurchaseSettlementRows()" />
                </div>
              </div>
              <div class="purchase-order-settlement-table-wrap">
                <table class="purchase-order-settlement-table">
                  <thead>
                    <tr>
                      <th>
                        <input type="checkbox" :checked="purchaseSettlementAllSelected" @change="toggleAllPurchaseSettlementRows(($event.target as HTMLInputElement).checked)" />
                      </th>
                      <th>序号</th>
                      <th>服务商</th>
                      <th>类型</th>
                      <th>报价金额</th>
                      <th>实际金额</th>
                      <th>状态</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in purchaseSettlementRows" :key="row.id">
                      <td>
                        <input type="checkbox" :disabled="String(row.rawStatus).toUpperCase() !== 'PENDING_SETTLEMENT'" :checked="purchaseSettlementSelectedRowIds.includes(row.id)" @change="togglePurchaseSettlementRow(row.id, ($event.target as HTMLInputElement).checked)" />
                      </td>
                      <td>{{ index + 1 }}</td>
                      <td>{{ row.provider }}</td>
                      <td>{{ row.type }}</td>
                      <td>{{ formatPurchaseMoneyValue(row.quoteAmount) }}</td>
                      <td>
                        <strong>{{ row.actualAmount > 0 ? formatPurchaseMoneyValue(row.actualAmount) : "--" }}</strong>
                      </td>
                      <td>
                        <span :class="['purchase-order-settlement-status', row.settlementStatus === '已结算' ? 'is-settled' : 'is-pending']">{{ row.settlementStatus }}</span>
                      </td>
                      <td>
                        <div class="icon-action-row">
                          <IconButton icon="Eye" label="查看发票" @click="previewBusinessAttachments('发票附件', row.invoiceAttachments || [])" />
                          <IconButton v-if="String(row.rawStatus).toUpperCase() === 'PENDING_SETTLEMENT'" icon="Check" label="确认结算" variant="primary" @click="submitPurchaseSettlementRows(row.id)" />
                        </div>
                      </td>
                    </tr>
                    <tr v-if="!purchaseSettlementRows.length">
                      <td colspan="8">暂无结算数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>

          </section>
          <Teleport to="body">
            <IconButton
              v-if="showPurchaseOrderDetailBackTop"
              class="compare-back-top-button"
              icon="ArrowUp"
              :label="t('action.backToTop')"
              @click="scrollPurchaseOrderDetailToTop"
            />
          </Teleport>
        </template>
        <template v-else>
          <section class="filter-toolbar" aria-label="purchase order filters">
            <div class="filter-fields">
              <label class="filter-field filter-field--search">
                <span>检索</span>
                <span class="filter-search-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24">
                    <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                  </svg>
                </span>
                <input v-model="purchaseOrderKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="reloadPurchaseOrderWorkspace" />
              </label>
              <label class="filter-field">
                <span>{{ t("filter.status") }}</span>
                <select v-model="purchaseOrderStatus">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="PENDING_SUPPLIER_CONFIRM">{{ t("purchaseOrder.status.PENDING_SUPPLIER_CONFIRM") }}</option>
                  <option value="PARTIALLY_CONFIRMED">{{ t("purchaseOrder.status.PARTIALLY_CONFIRMED") }}</option>
                  <option value="PREPARING">{{ t("purchaseOrder.status.PREPARING") }}</option>
                  <option value="PARTIALLY_READY">{{ t("purchaseOrder.status.PARTIALLY_READY") }}</option>
                  <option value="READY">{{ t("purchaseOrder.status.READY") }}</option>
                  <option value="PARTIALLY_SUPPLIED">{{ t("purchaseOrder.status.PARTIALLY_SUPPLIED") }}</option>
                  <option value="SUPPLIED">{{ t("purchaseOrder.status.SUPPLIED") }}</option>
                  <option value="COMPLETED">{{ t("purchaseOrder.status.COMPLETED") }}</option>
                  <option value="PARTIALLY_REJECTED">{{ t("purchaseOrder.status.PARTIALLY_REJECTED") }}</option>
                  <option value="REJECTED">{{ t("purchaseOrder.status.REJECTED") }}</option>
                  <option value="CANCELED">{{ t("purchaseOrder.status.CANCELED") }}</option>
                  <option value="DISCARDED">{{ t("purchaseOrder.status.DISCARDED") }}</option>
                </select>
              </label>
              <label class="filter-field">
                <span>下单开始</span>
                <StableDateTimeInput v-model="purchaseOrderCreatedFrom" mode="date" />
              </label>
              <label class="filter-field">
                <span>下单结束</span>
                <StableDateTimeInput v-model="purchaseOrderCreatedTo" mode="date" />
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
          <div v-if="activePurchaseOrderLoading" class="empty-state compact">{{ t("common.loading") }}</div>
          <div v-else-if="!activePurchaseOrderRows.length" class="empty-state compact">{{ purchaseOrderHasFilters ? t("purchaseOrder.emptyFiltered") : t("purchaseOrder.empty") }}</div>
          <section v-else class="purchase-order-query-list">
            <article v-for="row in activePurchaseOrderRows" :key="String(row.purchaseOrderId)" class="purchase-order-query-card" tabindex="0" @click="openPurchaseOrderDetail(row)" @keydown.enter="openPurchaseOrderDetail(row)">
              <header class="purchase-order-query-card__head">
                <div class="purchase-order-query-card__vessel">
                  <strong class="vessel-title"><span class="vessel-mini-icon" aria-hidden="true"></span>{{ row.vesselName || "-" }}</strong>
                </div>
                <div class="purchase-order-query-card__identity">
                  <strong>{{ row.purchaseOrderNo || "-" }}</strong>
                </div>
                <div class="purchase-order-query-card__status">
                  <StatusBadge :label="purchaseOrderQueryStatusLabel(row)" :variant="purchaseStatusVariant(String(row.status || ''))" />
                </div>
              </header>
              <section class="purchase-order-query-timeline" aria-label="订单执行进度">
                <div v-for="(label, index) in purchaseOrderQueryStageLabels" :key="`${row.purchaseOrderId}-${label}`" :class="['purchase-order-query-stage', `is-${purchaseOrderQueryStageState(row, index)}`]">
                  <i></i><strong>{{ label }}</strong>
                </div>
              </section>
              <footer class="purchase-order-query-card__footer">
                <div><span>靠泊时间</span><strong>{{ formatDemandDateTime(purchaseOrderQueryDate(row)) || "-" }}</strong></div>
                <div><span>SKU</span><strong>{{ row.itemCount || 0 }}</strong></div>
                <div><span>{{ purchaseOrderWorkspaceMode === "supplier" ? "订单金额" : "采购金额" }}</span><strong>{{ formatPurchaseMoneyValue(purchaseOrderQueryAmount(row)) }}</strong></div>
                <div class="icon-action-row">
                  <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openPurchaseOrderDetail(row)" />
                  <IconButton v-if="purchaseOrderWorkspaceMode === 'buyer'" icon="Trash2" :label="t('action.discard')" variant="danger" :disabled="!canDiscardStatus(String(row.status || ''))" @click.stop="openDiscardDialog({ type: 'purchaseOrder', id: String(row.purchaseOrderId), refresh: reloadPurchaseOrderWorkspace })" />
                </div>
              </footer>
            </article>
          </section>
        </template>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'supplyChainFinance'" :show-header="false" class="inquiry-management-panel supply-chain-finance-panel">
        <div class="inquiry-management-heading">
          <div>
            <strong>{{ t("page.supplyChainFinance.title") }}</strong>
            <span>{{ t("page.supplyChainFinance.subtitle") }}</span>
          </div>
          <p>{{ t("page.supplyChainFinance.staticNotice") }}</p>
        </div>

        <div class="supply-finance-metrics">
          <article v-for="metric in supplyChainFinanceMetrics" :key="metric.label" class="metric-card supply-finance-metric-card">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ metric.note }}</small>
          </article>
        </div>

        <section class="supply-finance-layout">
          <article class="work-card supply-finance-split-card">
            <div class="supply-finance-section-head">
              <div>
                <h2>{{ t("page.supplyChainFinance.splitTitle") }}</h2>
                <p>{{ t("page.supplyChainFinance.splitSubtitle") }}</p>
              </div>
              <StatusBadge :label="t('page.supplyChainFinance.statusReviewing')" variant="info" />
            </div>

            <div class="supply-finance-total">
              <span>{{ t("page.supplyChainFinance.orderAmount") }}</span>
              <strong>{{ formatDisplayMoney(1000000, "CNY") }}</strong>
            </div>
            <div class="supply-finance-ratio" aria-hidden="true">
              <span class="is-prepayment" />
              <span class="is-loan" />
            </div>
            <div class="supply-finance-split-grid">
              <div>
                <span>{{ t("page.supplyChainFinance.prepayment") }}</span>
                <strong>{{ formatDisplayMoney(200000, "CNY") }}</strong>
                <small>{{ t("page.supplyChainFinance.prepaymentRate") }} 20%</small>
              </div>
              <div>
                <span>{{ t("page.supplyChainFinance.financeLoan") }}</span>
                <strong>{{ formatDisplayMoney(800000, "CNY") }}</strong>
                <small>{{ t("page.supplyChainFinance.loanRate") }} 80%</small>
              </div>
            </div>
          </article>

          <article class="work-card supply-finance-plan-card">
            <div class="supply-finance-section-head">
              <div>
                <h2>{{ t("page.supplyChainFinance.repaymentPlan") }}</h2>
                <p>{{ t("page.supplyChainFinance.riskLevel") }}：{{ t("page.supplyChainFinance.riskLow") }}</p>
              </div>
            </div>
            <div class="timeline supply-finance-timeline">
              <div v-for="item in supplyChainFinanceTimeline" :key="item.label">
                <strong>{{ item.label }}</strong>
                <span>{{ item.value }}</span>
              </div>
            </div>
          </article>
        </section>

        <section class="work-card supply-finance-table-card">
          <div class="supply-finance-section-head">
            <div>
              <h2>{{ t("page.supplyChainFinance.financeOrders") }}</h2>
              <p>{{ t("page.supplyChainFinance.orderAmount") }} = {{ t("page.supplyChainFinance.prepayment") }} + {{ t("page.supplyChainFinance.financeLoan") }}</p>
            </div>
          </div>
          <DataTable :columns="supplyChainFinanceColumns" :rows="supplyChainFinanceRows" row-key="purchaseOrderNo">
            <template #cell-orderAmount="{ value }">{{ formatDisplayMoney(Number(value), "CNY") }}</template>
            <template #cell-prepaymentAmount="{ value }">{{ formatDisplayMoney(Number(value), "CNY") }}</template>
            <template #cell-financeAmount="{ value }">{{ formatDisplayMoney(Number(value), "CNY") }}</template>
            <template #cell-status="{ row }">
              <StatusBadge
                :label="supplyChainFinanceStatusLabel(row.status as SupplyChainFinanceStatus)"
                :variant="supplyChainFinanceStatusVariant(row.status as SupplyChainFinanceStatus)"
              />
            </template>
          </DataTable>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="isEvaluationPage" :show-header="false" class="inquiry-management-panel evaluation-management-panel">
        <section class="filter-toolbar evaluation-filter-toolbar" aria-label="evaluation filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>检索</span>
              <input v-model="evaluationKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadEvaluations" />
            </label>
            <label class="filter-field">
              <span>状态</span>
              <select v-model="evaluationStatus">
                <option value="">{{ t("common.all") }}</option>
                <option v-for="option in evaluationStatusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" :label="t('common.search')" @click="loadEvaluations" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetEvaluationSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="evaluationLoading" @click="loadEvaluations" />
          </div>
        </section>
        <div v-if="evaluationError" class="inline-error">{{ evaluationError }}</div>
        <div v-if="evaluationLoading" class="empty-state compact">{{ t('common.loading') }}</div>
        <div v-else-if="!sortedEvaluationRows.length" class="empty-state compact">暂无评价任务</div>
        <section v-else class="evaluation-card-list">
          <article v-for="row in sortedEvaluationRows" :key="row.evaluationId" class="evaluation-card">
            <header>
              <div><strong>{{ row.providerName }}</strong></div>
              <StatusBadge :label="evaluationStatusLabel(row.status)" :variant="evaluationStatusVariant(row.status)" />
            </header>
            <div class="evaluation-card__order"><span>采购单</span><strong>{{ row.purchaseOrderNo || row.purchaseOrderId }}</strong></div>
            <div class="evaluation-score-rows">
              <div class="evaluation-score-line">
                <span>品质</span>
                <div class="evaluation-star-display" :aria-label="`品质${row.rating || 0}分`"><span v-for="point in 5" :key="point" :class="{ active: point <= Number(row.rating || 0) }">★</span></div>
              </div>
              <div class="evaluation-score-line">
                <span>物流</span>
                <div class="evaluation-star-display" :aria-label="`物流${evaluationLogisticsRating(row)}分`"><span v-for="point in 5" :key="point" :class="{ active: point <= evaluationLogisticsRating(row) }">★</span></div>
              </div>
            </div>
            <dl class="evaluation-card__content">
              <div><dd>{{ row.content || '尚未填写评价内容' }}</dd></div>
            </dl>
            <footer>
              <span>附件证明：{{ row.attachments?.length || 0 }} 个</span>
              <IconButton v-if="pageKey === 'evaluations' && ['PENDING_EVALUATION', 'REJECTED'].includes(row.status)" icon="Pencil" label="填写评价" @click="openEvaluationEditor(row)" />
              <IconButton v-if="pageKey === 'evaluations' && row.status === 'PENDING_REVIEW'" icon="Eye" label="查看明细" @click="openEvaluationEditor(row)" />
              <IconButton v-if="isRegulatoryEvaluationPage && row.status === 'PENDING_REVIEW'" icon="Eye" label="审查评价" @click="openEvaluationEditor(row)" />
              <IconButton v-if="isRegulatoryEvaluationPage && row.status === 'APPROVED'" icon="Eye" label="查看明细" @click="openEvaluationEditor(row)" />
              <IconButton v-if="row.attachments?.length" icon="Image" label="查看证明" @click="previewBusinessAttachments('评价证明', row.attachments)" />
            </footer>
          </article>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="['settlements', 'supplierSettlements', 'bargeSettlements'].includes(pageKey)" :show-header="false" class="inquiry-management-panel settlement-management-panel">
        <section class="filter-toolbar" aria-label="settlement filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>检索</span>
              <input v-model="settlementManagementKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadSettlementManagement" />
            </label>
            <label class="filter-field">
              <span>状态</span>
              <select v-model="settlementManagementStatus">
                <option value="">{{ t("common.all") }}</option>
                <option v-for="option in settlementManagementStatusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" :label="t('common.search')" @click="loadSettlementManagement" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetSettlementManagementSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="settlementManagementLoading" @click="loadSettlementManagement" />
          </div>
        </section>
        <div v-if="settlementManagementError" class="inline-error">{{ settlementManagementError }}</div>
        <div v-if="settlementManagementLoading" class="empty-state compact">{{ t("common.loading") }}</div>
        <div v-else-if="!filteredSettlementManagementRows.length" class="empty-state compact">暂无结算单</div>
        <section v-else :class="['settlement-card-list', `is-${settlementManagementScope.toLowerCase()}`]">
          <article
            v-for="row in filteredSettlementManagementRows"
            :key="row.settlementId"
            :class="['settlement-card', settlementManagementScope === 'BUYER' ? 'is-buyer' : 'is-provider', { 'is-barge': settlementManagementScope === 'BARGE' }]"
          >
            <header>
              <div>
                <strong class="settlement-card__no"><span class="settlement-money-icon" aria-hidden="true"></span>{{ row.settlementNo }}</strong>
              </div>
              <small class="settlement-card__purchase-no">{{ row.purchaseOrderNo || '-' }}</small>
              <div class="settlement-card__status-type">
                <StatusBadge :label="settlementStatusLabel(row.status)" :variant="settlementStatusVariant(row.status)" />
              </div>
            </header>
            <div class="settlement-card__content-row">
              <dl>
                <div>
                  <dt>{{ settlementManagementScope === 'BUYER' ? '服务商' : '船代公司' }}</dt>
                  <dd>{{ settlementManagementScope === 'BUYER' ? row.providerName : (row.buyerCompanyName || '-') }}</dd>
                </div>
                <div><dt>船舶名称</dt><dd>{{ row.vesselName || '-' }}</dd></div>
                <div><dt>报价金额</dt><dd>{{ formatPurchaseMoneyValue(Number(row.quotedAmount || 0)) }}</dd></div>
                <div><dt>实际金额</dt><dd>{{ Number(row.actualAmount || 0) > 0 ? formatPurchaseMoneyValue(Number(row.actualAmount)) : '--' }}</dd></div>
              </dl>
              <div class="icon-action-row settlement-card__inline-actions">
                <IconButton v-if="settlementManagementScope === 'BUYER' && String(row.status).toUpperCase() === 'PENDING_SETTLEMENT'" icon="Check" label="确认已结算" variant="primary" @click.stop="settleSettlementRow(row)" />
                <IconButton v-if="settlementManagementScope !== 'BUYER' && String(row.status).toUpperCase() === 'PENDING_INVOICE'" icon="Pencil" label="编辑结算" @click.stop="settlementEditingId = row.settlementId" />
                <IconButton icon="Eye" label="查看发票" @click.stop="previewBusinessAttachments('发票附件', row.invoiceAttachments || [])" />
                <IconButton v-if="settlementManagementScope === 'BUYER' && String(row.status).toUpperCase() === 'SETTLED'" icon="Check" label="付款" variant="primary" @click.stop="paySettlementRow(row)" />
              </div>
            </div>
          </article>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'trafficRoutes'" :show-header="false" class="inquiry-management-panel traffic-service-panel traffic-route-panel">
        <section class="filter-toolbar" aria-label="traffic route filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>{{ t("trafficRoute.filter.keyword") }}</span>
              <input v-model="trafficRouteKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficRoutes" />
            </label>
            <label class="filter-field">
              <span>{{ t("trafficRoute.filter.serviceDate") }}</span>
              <StableDateTimeInput v-model="trafficRouteServiceDate" mode="date" />
            </label>
            <label class="filter-field">
              <span>{{ t("trafficService.field.seaArea") }}</span>
              <select v-model="trafficRouteSeaArea">
                <option value="">{{ t("common.all") }}</option>
                <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span>{{ t("filter.status") }}</span>
              <select v-model="trafficRouteStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="DRAFT">{{ t("trafficRoute.status.draft") }}</option>
                <option value="READY">{{ t("trafficRoute.status.ready") }}</option>
                <option value="IN_PROGRESS">{{ t("trafficRoute.status.inProgress") }}</option>
                <option value="COMPLETED">{{ t("trafficRoute.status.completed") }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Plus" :label="t('trafficRoute.action.create')" variant="primary" @click="openTrafficRouteDialog" />
            <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficRoutes" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficRouteSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficRouteLoading" @click="loadTrafficRoutes" />
          </div>
        </section>
        <p v-if="trafficRouteNoticeKey" class="permission-static-notice">{{ t(trafficRouteNoticeKey) }}</p>
        <div v-if="trafficRouteErrorKey" class="inline-error inline-error--action">
          <span>{{ t(trafficRouteErrorKey) }}</span>
          <button type="button" @click="loadTrafficRoutes">{{ t("purchaseOrder.action.retry") }}</button>
        </div>
        <section class="traffic-route-workspace">
          <div class="traffic-route-main">
            <DataTable :columns="trafficRouteColumns" :rows="trafficRouteTableRows" :loading="trafficRouteLoading" row-key="routePlanId" row-interactive :empty-label="t('trafficRoute.empty')" @row-click="selectTrafficRoute">
              <template #cell-seaArea="{ value }">{{ seaAreaLabel(String(value || '')) }}</template>
              <template #cell-totalIncome="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
              <template #cell-estimatedProfit="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
              <template #cell-status="{ value }"><StatusBadge :label="trafficRouteStatusLabel(String(value || ''))" :variant="trafficRouteStatusVariant(String(value || ''))" /></template>
            </DataTable>
            <section class="traffic-route-map-card">
              <div>
                <strong>{{ t("trafficRoute.mapPlaceholder") }}</strong>
                <span>{{ t("trafficRoute.mapHint") }}</span>
              </div>
              <div class="traffic-route-map-line">
                <span v-for="stop in (trafficRouteDetail?.stops || [])" :key="stop.routeStopId">{{ stop.stopSequence }}</span>
              </div>
            </section>
            <section class="traffic-route-candidates">
              <header class="panel-section-title">
                <h3>{{ t("trafficRoute.emptyOrders") }}</h3>
              </header>
              <DataTable :columns="trafficRouteCandidateColumns" :rows="trafficRouteCandidateRows" :loading="trafficServiceLoading" row-key="serviceOrderId" :empty-label="t('trafficRoute.emptyOrders')">
                <template #cell-useTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
                <template #cell-basePrice="{ row }">{{ formatPurchaseMoneyValue(Number(row.allowShare ? row.sharedPrice || 0 : row.basePrice || 0)) }}</template>
                <template #cell-operation="{ row }">
                  <IconButton icon="Plus" :label="t('trafficRoute.action.addStop')" :disabled="!selectedTrafficRoute" @click.stop="addOrderToTrafficRoute(row)" />
                </template>
              </DataTable>
            </section>
          </div>
          <aside class="traffic-route-side">
            <header>
              <div>
                <span>{{ selectedTrafficRoute?.routeNo || "-" }}</span>
                <strong>{{ selectedTrafficRoute?.routeName || t("trafficRoute.empty") }}</strong>
              </div>
              <StatusBadge v-if="selectedTrafficRoute" :label="trafficRouteStatusLabel(selectedTrafficRoute.status)" :variant="trafficRouteStatusVariant(selectedTrafficRoute.status)" />
            </header>
            <div class="traffic-route-actions">
              <IconButton icon="Check" :label="t('trafficRoute.action.confirm')" :disabled="!selectedTrafficRoute" :loading="trafficRouteSaving" @click="changeTrafficRouteStatus('confirm')" />
              <IconButton icon="Send" :label="t('trafficRoute.action.start')" :disabled="!selectedTrafficRoute" :loading="trafficRouteSaving" @click="changeTrafficRouteStatus('start')" />
              <IconButton icon="Check" :label="t('trafficRoute.action.complete')" :disabled="!selectedTrafficRoute" :loading="trafficRouteSaving" @click="changeTrafficRouteStatus('complete')" />
              <IconButton icon="Trash2" :label="t('trafficRoute.action.discard')" variant="danger" :disabled="!selectedTrafficRoute" :loading="trafficRouteSaving" @click="changeTrafficRouteStatus('discard')" />
            </div>
            <DataTable :columns="trafficRouteStopColumns" :rows="trafficRouteStopTableRows" :loading="trafficRouteLoading" row-key="routeStopId" :empty-label="t('trafficRoute.emptyStops')">
              <template #cell-plannedServiceTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
              <template #cell-amount="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
              <template #cell-status="{ value }"><StatusBadge :label="trafficRouteStatusLabel(String(value || ''))" variant="info" /></template>
              <template #cell-operation="{ row }">
                <IconButton icon="X" :label="t('action.delete')" variant="danger" @click.stop="removeOrderFromTrafficRoute(row)" />
              </template>
            </DataTable>
          </aside>
        </section>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'transportServices'" :show-header="false" class="inquiry-management-panel traffic-service-panel">
        <div class="transport-market-tabs">
          <button type="button" :class="{ active: transportMarketplaceTab === 'requests' }" @click="transportMarketplaceTab = 'requests'">{{ t("trafficMarketplace.tab.requests") }}</button>
          <button type="button" :class="{ active: transportMarketplaceTab === 'shuttles' }" @click="transportMarketplaceTab = 'shuttles'; loadTrafficShuttles(false)">{{ t("trafficMarketplace.tab.shuttles") }}</button>
        </div>
        <template v-if="transportMarketplaceTab === 'requests'">
          <section class="filter-toolbar" aria-label="traffic request filters">
            <div class="filter-fields">
              <label class="filter-field filter-field--search">
                <span>{{ t("trafficService.filter.keyword") }}</span>
                <input v-model="trafficRequestKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficRequests" />
              </label>
              <label class="filter-field">
                <span>{{ t("trafficService.field.seaArea") }}</span>
                <select v-model="trafficRequestSeaArea">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                  <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                </select>
              </label>
              <label class="filter-field">
                <span>{{ t("field.status") }}</span>
                <select v-model="trafficRequestStatus">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="PUBLISHED">{{ t("trafficMarketplace.status.published") }}</option>
                  <option value="QUOTING">{{ t("trafficMarketplace.status.quoting") }}</option>
                  <option value="ORDER_CREATED">{{ t("trafficMarketplace.status.orderCreated") }}</option>
                </select>
              </label>
            </div>
            <div class="toolbar-icon-actions">
              <IconButton icon="Plus" :label="t('trafficMarketplace.action.createRequest')" variant="primary" @click="openTrafficRequestDialog" />
              <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficRequests" />
              <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficRequestSearch" />
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficRequestLoading" @click="loadTrafficRequests" />
            </div>
          </section>
          <p v-if="trafficServiceNoticeKey" class="permission-static-notice">{{ t(trafficServiceNoticeKey) }}</p>
          <div v-if="trafficServiceErrorKey" class="inline-error inline-error--action">
            <span>{{ t(trafficServiceErrorKey) }}</span>
            <button type="button" @click="loadTrafficRequests">{{ t("purchaseOrder.action.retry") }}</button>
          </div>
          <section class="transport-market-layout">
            <DataTable :columns="trafficRequestColumns" :rows="trafficRequestTableRows" :loading="trafficRequestLoading" row-key="requestId" row-interactive :empty-label="t('trafficMarketplace.emptyRequests')" @row-click="openTrafficRequestDetail">
              <template #cell-seaArea="{ value }">{{ seaAreaLabel(String(value || '')) }}</template>
              <template #cell-useTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
              <template #cell-serviceType="{ value }">{{ trafficTypeLabel(String(value || '')) }}</template>
              <template #cell-allowShare="{ value }">{{ value ? t("common.yes") : t("common.no") }}</template>
              <template #cell-status="{ value }"><StatusBadge :label="trafficRequestStatusLabel(String(value || ''))" variant="info" /></template>
              <template #cell-operation="{ row }">
                <div class="table-action-buttons table-action-buttons--center">
                  <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openTrafficRequestDetail(row)" />
                  <IconButton icon="X" :label="t('action.discard')" variant="danger" :loading="trafficRequestSaving" @click.stop="cancelTrafficRequest(row)" />
                </div>
              </template>
            </DataTable>
            <aside class="transport-market-detail">
              <header>
                <div>
                  <span>{{ selectedTrafficRequest?.requestNo || "-" }}</span>
                  <strong>{{ selectedTrafficRequest?.anchorageName || t("trafficMarketplace.emptyQuotes") }}</strong>
                </div>
                <StatusBadge v-if="selectedTrafficRequest" :label="trafficRequestStatusLabel(selectedTrafficRequest.status)" variant="info" />
              </header>
              <DataTable :columns="trafficQuoteColumns" :rows="trafficQuoteTableRows" :loading="trafficRequestLoading" row-key="quoteId" :empty-label="t('trafficMarketplace.emptyQuotes')">
                <template #cell-quoteAmount="{ row, value }">
                  <span :class="{ 'traffic-recommended-price': row.recommended }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</span>
                </template>
                <template #cell-availableStartTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
                <template #cell-status="{ value }"><StatusBadge :label="trafficQuoteStatusLabel(String(value || ''))" variant="info" /></template>
                <template #cell-operation="{ row }">
                  <IconButton v-if="['SUBMITTED', 'UPDATED'].includes(String(row.status || '').toUpperCase())" icon="Check" :label="t('trafficMarketplace.action.selectQuote')" variant="primary" :loading="trafficRequestSaving" @click.stop="selectTrafficQuote(row)" />
                </template>
              </DataTable>
            </aside>
          </section>
        </template>
        <template v-else>
          <section class="filter-toolbar" aria-label="traffic shuttle filters">
            <div class="filter-fields">
              <label class="filter-field filter-field--search">
                <span>{{ t("trafficService.filter.keyword") }}</span>
                <input v-model="trafficShuttleKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficShuttles(false)" />
              </label>
              <label class="filter-field">
                <span>{{ t("trafficService.field.seaArea") }}</span>
                <select v-model="trafficShuttleSeaArea">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                  <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                </select>
              </label>
            </div>
            <div class="toolbar-icon-actions">
              <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficShuttles(false)" />
              <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficShuttleSearch(false)" />
              <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficShuttleLoading" @click="loadTrafficShuttles(false)" />
            </div>
          </section>
          <DataTable :columns="trafficShuttleColumns" :rows="trafficShuttleTableRows" :loading="trafficShuttleLoading" row-key="shuttleId" :empty-label="t('trafficMarketplace.emptyShuttles')">
            <template #cell-route="{ row }">{{ trafficShuttleRouteLabel(row) }}</template>
            <template #cell-startTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
            <template #cell-returnTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
            <template #cell-sharedPrice="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
            <template #cell-status="{ value }"><StatusBadge :label="trafficShuttleStatusLabel(String(value || ''))" variant="info" /></template>
            <template #cell-operation="{ row }">
              <IconButton icon="Check" :label="t('trafficMarketplace.action.bookShuttle')" variant="primary" :loading="trafficShuttleSaving" @click.stop="bookShuttle(row)" />
            </template>
          </DataTable>
        </template>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'delivery'" :show-header="false" class="inquiry-management-panel traffic-service-panel">
        <section class="filter-toolbar" aria-label="traffic service filters">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>{{ t("trafficService.filter.keyword") }}</span>
              <input v-model="trafficServiceKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficServices" />
            </label>
            <label class="filter-field">
              <span>{{ t("trafficService.field.seaArea") }}</span>
              <select v-model="trafficServiceSeaArea">
                <option value="">{{ t("common.all") }}</option>
                <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span>{{ t("filter.status") }}</span>
              <select v-model="trafficServiceStatus">
                <option value="">{{ t("common.all") }}</option>
                <option value="ACTIVE">{{ t("trafficService.status.active") }}</option>
                <option value="DISCARDED">{{ t("trafficService.status.discarded") }}</option>
              </select>
            </label>
          </div>
          <div class="toolbar-icon-actions">
            <IconButton icon="Plus" :label="t('action.add')" variant="primary" @click="openTrafficServiceDialog()" />
            <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficServices" />
            <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficServiceSearch" />
            <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficServiceLoading" @click="loadTrafficServices" />
          </div>
        </section>
        <p v-if="trafficServiceNoticeKey" class="permission-static-notice">{{ t(trafficServiceNoticeKey) }}</p>
        <div v-if="trafficServiceErrorKey" class="inline-error inline-error--action">
          <span>{{ t(trafficServiceErrorKey) }}</span>
          <button type="button" @click="loadTrafficServices">{{ t("purchaseOrder.action.retry") }}</button>
        </div>
        <DataTable :columns="trafficServiceColumns" :rows="trafficServiceTableRows" :loading="trafficServiceLoading" row-key="serviceOrderId" :empty-label="t('trafficService.empty')">
          <template #cell-feeType="{ value }">{{ trafficFeeTypeLabel(String(value || '')) }}</template>
          <template #cell-seaArea="{ value }">{{ seaAreaLabel(String(value || '')) }}</template>
          <template #cell-useTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
          <template #cell-serviceType="{ value }">{{ trafficTypeLabel(String(value || '')) }}</template>
          <template #cell-basePrice="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
          <template #cell-sharedPrice="{ value }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</template>
          <template #cell-status="{ value }"><StatusBadge :label="trafficServiceStatusLabel(String(value || ''))" :variant="trafficServiceStatusVariant(String(value || ''))" /></template>
          <template #cell-operation="{ row }">
            <div class="table-action-buttons table-action-buttons--center">
              <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openTrafficServiceDialog(row)" />
              <IconButton icon="Trash2" :label="t('action.discard')" variant="danger" @click.stop="discardTrafficService(row)" />
            </div>
          </template>
        </DataTable>
      </ExpandablePanel>

      <ExpandablePanel v-else-if="pageKey === 'trafficBoat' || pageKey === 'trafficBoatMyServices'" :show-header="false" class="inquiry-management-panel traffic-service-panel">
        <template v-if="isTrafficServiceDetailPage">
          <div class="purchase-order-detail-toolbar traffic-service-detail-toolbar">
            <div class="purchase-order-detail-title">
              <h3>{{ selectedTrafficService?.serviceNo || t("trafficService.dialog.editTitle") }}</h3>
              <StatusBadge
                v-if="selectedTrafficService"
                :label="trafficServiceStatusLabel(selectedTrafficService.status)"
                :variant="trafficServiceStatusVariant(selectedTrafficService.status)"
              />
            </div>
            <div class="purchase-order-detail-actions">
              <IconButton icon="ChevronLeft" :label="t('action.back')" @click="router.push('/traffic-boat')" />
              <IconButton
                v-if="selectedTrafficService && String(selectedTrafficService.status || '').toUpperCase() === 'PENDING_CONFIRM'"
                icon="Check"
                :label="t('common.confirm')"
                variant="primary"
                :loading="trafficServiceLoading"
                @click="confirmTrafficService(selectedTrafficService)"
              />
              <IconButton
                v-if="trafficServiceDetailEditable"
                icon="Save"
                :label="t('common.save')"
                variant="primary"
                :loading="trafficServiceSaving"
                @click="saveTrafficService"
              />
            </div>
          </div>
          <div v-if="trafficServiceLoading" class="compare-state-message">{{ t("common.loading") }}</div>
          <div v-else-if="!selectedTrafficService" class="empty-state compact">{{ t("trafficService.empty") }}</div>
          <section v-else class="traffic-service-detail-page">
            <article class="purchase-order-section">
              <div class="purchase-order-section-header">
                <h3>{{ t("trafficService.section.basicInfo") }}</h3>
              </div>
              <div class="purchase-order-summary-card">
                <div>
                  <span>{{ t("trafficService.field.serviceNo") }}</span>
                  <strong>{{ selectedTrafficService.serviceNo || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.feeType") }}</span>
                  <strong>{{ trafficFeeTypeLabel(selectedTrafficService.feeType || "") }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.seaArea") }}</span>
                  <strong>{{ seaAreaLabel(selectedTrafficService.seaArea || "") }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.anchorage") }}</span>
                  <strong>{{ displayOrderText(selectedTrafficService.anchorageName) }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.useTime") }}</span>
                  <strong>{{ formatDemandDateTime(selectedTrafficService.useTime || "") || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.serviceType") }}</span>
                  <strong>{{ trafficTypeLabel(selectedTrafficService.serviceType || "") }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.allowShare") }}</span>
                  <strong>{{ selectedTrafficService.allowShare ? t("common.yes") : t("common.no") }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.price") }}</span>
                  <strong>{{ formatPurchaseMoneyValue(trafficServiceDetailPrice) }}</strong>
                </div>
              </div>
            </article>
            <article class="purchase-order-section">
              <div class="purchase-order-section-header">
                <h3>{{ t("trafficService.section.businessInfo") }}</h3>
              </div>
              <div class="purchase-order-summary-card">
                <div class="purchase-order-summary-card__wide">
                  <span>{{ t("trafficService.field.businessInfo") }}</span>
                  <textarea
                    v-if="trafficServiceDetailEditable"
                    v-model="trafficServiceForm.remark"
                    class="purchase-detail-edit-control purchase-detail-edit-control--textarea"
                    rows="2"
                    :placeholder="t('trafficService.field.businessInfo')"
                  ></textarea>
                  <strong v-else>{{ displayOrderText(selectedTrafficService.remark) }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.businessContact") }}</span>
                  <select v-if="trafficServiceDetailEditable" :value="trafficServiceForm.businessContactId || ''" class="purchase-detail-edit-control" @change="selectTrafficBusinessContact(($event.target as HTMLSelectElement).value)">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option v-for="contact in activeCompanyContacts" :key="contact.id" :value="String(contact.contactId || contact.id)">
                      {{ contact.contactName }} / {{ contact.contactPhone }}
                    </option>
                  </select>
                  <strong v-else>{{ [displayOrderText(selectedTrafficService.businessContactName), selectedTrafficService.businessContactPhone].filter(Boolean).join(" / ") || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.trafficVessel") }}</span>
                  <select v-if="trafficServiceDetailEditable" :value="trafficServiceForm.trafficVesselId || ''" class="purchase-detail-edit-control" @change="selectTrafficVessel(($event.target as HTMLSelectElement).value)">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option v-for="vessel in activeCompanyVessels" :key="vessel.id" :value="String(vessel.vesselId || vessel.id)">
                      {{ vessel.vesselName }}
                    </option>
                  </select>
                  <strong v-else>{{ displayOrderText(selectedTrafficService.trafficVesselName) }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.handler") }}</span>
                  <select v-if="trafficServiceDetailEditable" :value="trafficServiceForm.handlerContactId || ''" class="purchase-detail-edit-control" @change="selectTrafficHandlerContact(($event.target as HTMLSelectElement).value)">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option v-for="contact in activeCompanyContacts" :key="contact.id" :value="String(contact.contactId || contact.id)">
                      {{ contact.contactName }} / {{ contact.contactPhone }}
                    </option>
                  </select>
                  <strong v-else>{{ [displayOrderText(selectedTrafficService.handlerName), selectedTrafficService.handlerPhone].filter(Boolean).join(" / ") || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.departureTime") }}</span>
                  <StableDateTimeInput v-if="trafficServiceDetailEditable" v-model="trafficServiceForm.departureTime" mode="datetime" />
                  <strong v-else>{{ selectedTrafficService.departureTime || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("trafficService.field.arrivalTime") }}</span>
                  <StableDateTimeInput v-if="trafficServiceDetailEditable" v-model="trafficServiceForm.arrivalTime" mode="datetime" />
                  <strong v-else>{{ selectedTrafficService.arrivalTime || "-" }}</strong>
                </div>
              </div>
            </article>
          </section>
        </template>
        <template v-else>
        <template v-if="pageKey === 'trafficBoat'">
          <p v-if="trafficServiceNoticeKey" class="permission-static-notice">{{ t(trafficServiceNoticeKey) }}</p>
          <div v-if="trafficServiceErrorKey" class="inline-error inline-error--action">
            <span>{{ t(trafficServiceErrorKey) }}</span>
            <button type="button" @click="loadTrafficRequests">{{ t("purchaseOrder.action.retry") }}</button>
          </div>
          <section class="traffic-service-hall-stack">
            <article class="traffic-hall-section">
              <header class="traffic-hall-section__head">
                <div>
                  <strong>{{ t("trafficMarketplace.section.shuttleServices") }}</strong>
                  <span>{{ t("trafficMarketplace.section.shuttleServicesHint") }}</span>
                </div>
                <div class="toolbar-icon-actions">
                  <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficShuttles(false)" />
                  <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficShuttleSearch(false)" />
                  <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficShuttleLoading" @click="loadTrafficShuttles(false)" />
                </div>
              </header>
              <section class="filter-toolbar traffic-hall-filter" aria-label="traffic shuttle filters">
                <div class="filter-fields">
                  <label class="filter-field filter-field--search">
                    <span>{{ t("trafficService.filter.keyword") }}</span>
                    <input v-model="trafficShuttleKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficShuttles(false)" />
                  </label>
                  <label class="filter-field">
                    <span>{{ t("trafficService.field.seaArea") }}</span>
                    <select v-model="trafficShuttleSeaArea" @change="trafficShuttleAnchorageCode = ''">
                      <option value="">{{ t("common.all") }}</option>
                      <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                      <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                    </select>
                  </label>
                  <label class="filter-field">
                    <span>{{ t("trafficService.field.anchorage") }}</span>
                    <select v-model="trafficShuttleAnchorageCode">
                      <option value="">{{ t("common.all") }}</option>
                      <option v-for="anchorage in trafficShuttleFilterAnchorages" :key="anchorage.anchorageCode" :value="anchorage.anchorageCode">
                        {{ anchorage.anchorageName }}
                      </option>
                    </select>
                  </label>
                  <label class="filter-field">
                    <span>服务日期</span>
                    <StableDateTimeInput v-model="trafficShuttleServiceDate" mode="date" />
                  </label>
                  <label class="filter-field">
                    <span>{{ t("field.status") }}</span>
                    <select v-model="trafficShuttleStatus">
                      <option value="">{{ t("common.all") }}</option>
                      <option value="PUBLISHED">已发布</option>
                      <option value="FULL">预约中</option>
                      <option value="IN_PROGRESS">执行中</option>
                      <option value="COMPLETED">已完成</option>
                      <option value="CLOSED">{{ t("trafficMarketplace.status.cancelled") }}</option>
                    </select>
                  </label>
                </div>
              </section>
              <div v-if="trafficShuttleLoading" class="empty-state compact">{{ t("common.loading") }}</div>
              <div v-else-if="!trafficShuttleVisualRows.length" class="empty-state compact">{{ t("trafficMarketplace.emptyShuttles") }}</div>
              <section v-else class="traffic-shuttle-visual-list">
                <article
                  v-for="row in trafficShuttleVisualRows"
                  :key="row.shuttleId"
                  :class="['traffic-shuttle-visual-card', `is-${trafficShuttleExecutionCardState(row)}`]"
                >
                  <header class="traffic-shuttle-visual-card__head">
                    <div class="traffic-shuttle-visual-title">
                      <div class="traffic-shuttle-visual-name">
                        <strong>{{ row.trafficVesselName || "-" }}</strong>
                        <span>{{ row.shuttleNo || "-" }}</span>
                      </div>
                      <div class="traffic-shuttle-price-pair">
                        <span><b>单</b><em>{{ trafficShuttleBasePriceLabel(row) }}</em></span>
                        <span><b>拼</b><em>{{ trafficShuttleSharedPriceLabel(row) }}</em></span>
                        <small>增值服务：{{ trafficShuttleOtherFeeLabel(row) }}</small>
                      </div>
                    </div>
                    <div class="traffic-shuttle-card-status-corner">
                      <StatusBadge :label="trafficShuttleOperationalStatus(row).label" :variant="trafficShuttleOperationalStatus(row).variant" />
                    </div>
                  </header>
                  <section class="traffic-shuttle-diagram" aria-label="traffic shuttle route diagram">
                    <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--start">
                      <span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--pier" aria-label="departure vessel">⛴</span>
                      <div>
                        <strong>{{ trafficShuttleOriginLabel(row) }}</strong>
                        <em>{{ trafficShuttleStartTimeLabel(row) }}</em>
                      </div>
                    </div>
                    <div class="traffic-shuttle-node-track">
                      <div class="traffic-shuttle-dashed-line" aria-hidden="true"></div>
                      <div class="traffic-shuttle-node-strip">
                        <div
                          v-for="(node, index) in trafficShuttleNodeRows(row)"
                          :key="`${row.shuttleId}-${index}`"
                          class="traffic-shuttle-node-point"
                          :class="[`is-${trafficShuttleNodeVisualState(row, node, index)}`, { 'is-selected': activeTrafficShuttleNodeKey === trafficShuttleNodeKey(row, index) }]"
                          :aria-label="[node.nodeName || `${t('trafficMarketplace.field.serviceNode')}${index + 1}`, [node.startTime, node.endTime].filter(Boolean).join('-') || '-', trafficShuttleNodeShareStatus(row, node)].join(' ')"
                        >
                          <span class="traffic-shuttle-node-state-label">{{ trafficShuttleNodeDisplayStatus(row, index) }}</span>
                          <button
                            type="button"
                            class="traffic-shuttle-node-dot"
                            :aria-label="[node.nodeName || `${t('trafficMarketplace.field.serviceNode')}${index + 1}`, [node.startTime, node.endTime].filter(Boolean).join('-') || '-', trafficShuttleNodeShareStatus(row, node)].join(' ')"
                            @click.stop="openManagedShuttleNode(row, index)"
                          ></button>
                          <em>{{ [node.startTime, node.endTime].filter(Boolean).join("-") || "-" }}</em>
                        </div>
                      </div>
                      <template v-for="(node, index) in trafficShuttleNodeRows(row)" :key="`${row.shuttleId}-${index}-booking-panel`">
                        <div v-if="false && activeTrafficShuttleNodeKey === trafficShuttleNodeKey(row, index)" class="traffic-shuttle-node-popover managed-shuttle-node-drawer traffic-shuttle-node-popover--service-hall">
                          <section class="traffic-shuttle-service-summary">
                            <div>
                              <span>交通艇</span>
                              <strong>{{ row.trafficVesselName || "--" }}</strong>
                            </div>
                            <div>
                              <span>日期</span>
                              <strong>{{ trafficShuttleServiceDateLabel(row) || "--" }}</strong>
                            </div>
                            <div>
                              <span>时间节点</span>
                              <strong>{{ trafficShuttleNodeTimeLabel(node) }}</strong>
                            </div>
                            <div>
                              <span>起始到终点</span>
                              <strong>{{ trafficShuttleRouteLabel(row) }}</strong>
                            </div>
                            <div>
                              <span>单船价格</span>
                              <strong>{{ trafficShuttleBasePriceLabel(row) }}</strong>
                            </div>
                            <div>
                              <span>拼船价格</span>
                              <strong>{{ trafficShuttleSharedPriceLabel(row) }}</strong>
                            </div>
                            <div>
                              <span>报关费</span>
                              <strong>{{ trafficShuttleMoneyLabel(row.customsPrice) }}</strong>
                            </div>
                            <div>
                              <span>吊机价格（每吊）</span>
                              <strong>{{ trafficShuttleMoneyLabel(row.cranePrice) }}</strong>
                            </div>
                          </section>
                          <section class="traffic-shuttle-node-popover__info">
                            <div class="traffic-shuttle-vessel-tabs" role="tablist" aria-label="预约船舶">
                              <button
                                type="button"
                                :class="{ active: trafficShuttleNodeDraft(row, index).activeVesselSlot === 1 }"
                                @click.stop="setTrafficShuttleActiveVesselSlot(row, index, 1)"
                              >
                                预约船舶一
                              </button>
                              <button
                                type="button"
                                :class="{ active: trafficShuttleNodeDraft(row, index).activeVesselSlot === 2 }"
                                @click.stop="setTrafficShuttleActiveVesselSlot(row, index, 2)"
                              >
                                预约船舶二
                              </button>
                            </div>
                            <article class="traffic-shuttle-vessel-card traffic-shuttle-vessel-card--tabbed">
                              <dl>
                                <div>
                                  <dt>预约船舶</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot) }}</dd>
                                </div>
                                <div>
                                  <dt>货物重量（KG）</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "cargoWeight") }}</dd>
                                </div>
                                <div>
                                  <dt>货物体积（平方米）</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "cargoVolume") }}</dd>
                                </div>
                                <div>
                                  <dt>托盘数量</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "palletCount") }}</dd>
                                </div>
                                <div>
                                  <dt>抛锚时间</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "anchorageTime") }}</dd>
                                </div>
                                <div>
                                  <dt>抛锚经纬度</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "anchoragePosition") }}</dd>
                                </div>
                                <div>
                                  <dt>{{ trafficShuttleFreightFeeTitle(row, index) }}</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotFeeLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "freightFee") }}</dd>
                                </div>
                                <div>
                                  <dt>报关费</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotFeeLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "customsFee") }}</dd>
                                </div>
                                <div>
                                  <dt>吊机费</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotFeeLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "craneFee") }}</dd>
                                </div>
                                <div v-if="trafficShuttleNodeReservationTags(row, index).length">
                                  <dt>增值服务</dt>
                                  <dd class="traffic-shuttle-reservation-tags traffic-shuttle-reservation-tags--inline">
                                    <span v-for="tag in trafficShuttleNodeReservationTags(row, index)" :key="tag">{{ tag }}</span>
                                  </dd>
                                </div>
                              </dl>
                            </article>
                          </section>
                          <section class="managed-shuttle-attachment-panel">
                            <header>
                              <div>
                                <strong>服务附件</strong>
                                <span>0 个文件</span>
                              </div>
                            </header>
                            <div class="fulfillment-attachment-gallery">
                              <div class="empty-state compact">暂无服务附件</div>
                            </div>
                          </section>
                          <section class="traffic-shuttle-node-settings managed-shuttle-execution-settings">
                            <label>
                              <span>货物重量（KG）</span>
                              <input :value="trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, 'cargoWeight')" disabled placeholder="--" />
                            </label>
                            <label>
                              <span>货物体积（平方米）</span>
                              <input :value="trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, 'cargoVolume')" disabled placeholder="--" />
                            </label>
                            <label>
                              <span>托盘数量</span>
                              <input :value="trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, 'palletCount')" disabled placeholder="--" />
                            </label>
                            <label>
                              <span>抛锚经度</span>
                              <input :value="trafficShuttleNodeVesselSlotPositionPart(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, 'longitude')" disabled placeholder="--" />
                            </label>
                            <label>
                              <span>抛锚纬度</span>
                              <input :value="trafficShuttleNodeVesselSlotPositionPart(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, 'latitude')" disabled placeholder="--" />
                            </label>
                            <label>
                              <span>吊机次数</span>
                              <input :value="trafficShuttleNodeDraft(row, index).craneCount || '--'" disabled placeholder="--" />
                            </label>
                            <div class="traffic-shuttle-node-popover__footer">
                              <IconButton
                                icon="X"
                                label="关闭"
                                @click.stop="activeTrafficShuttleNodeKey = ''; activeTrafficShuttleComparePickerKey = ''"
                              />
                            </div>
                          </section>
                        </div>
                      </template>
                    </div>
                    <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--end">
                      <span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--anchor" aria-label="destination anchorage">⚓</span>
                      <div>
                        <strong>{{ trafficShuttleDestinationLabel(row) }}</strong>
                        <em>{{ trafficShuttleReturnTimeLabel(row) }}</em>
                      </div>
                    </div>
                  </section>
                  <footer class="traffic-shuttle-visual-card__foot">
                    <span class="traffic-shuttle-date-chip">{{ trafficShuttleServiceDateLabel(row) || "-" }}</span>
                  </footer>
                </article>
              </section>
            </article>
          </section>
        </template>
        <template v-else-if="false">
          <p v-if="trafficServiceNoticeKey" class="permission-static-notice">{{ t(trafficServiceNoticeKey) }}</p>
          <div v-if="trafficServiceErrorKey" class="inline-error inline-error--action">
            <span>{{ t(trafficServiceErrorKey) }}</span>
            <button type="button" @click="loadTrafficRequests">{{ t("purchaseOrder.action.retry") }}</button>
          </div>
          <section class="traffic-service-hall-stack">
            <article class="traffic-hall-section">
              <header class="traffic-hall-section__head">
                <div>
                  <strong>{{ t("trafficMarketplace.section.requestServices") }}</strong>
                  <span>{{ t("trafficMarketplace.section.requestServicesHint") }}</span>
                </div>
                <div class="toolbar-icon-actions">
                  <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficRequests" />
                  <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficRequestSearch" />
                  <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficRequestLoading" @click="loadTrafficRequests" />
                </div>
              </header>
              <section class="filter-toolbar traffic-hall-filter" aria-label="traffic service hall filters">
                <div class="filter-fields">
                  <label class="filter-field filter-field--search">
                    <span>{{ t("trafficService.filter.keyword") }}</span>
                    <input v-model="trafficRequestKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficRequests" />
                  </label>
                  <label class="filter-field">
                    <span>{{ t("trafficService.field.seaArea") }}</span>
                    <select v-model="trafficRequestSeaArea">
                      <option value="">{{ t("common.all") }}</option>
                      <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                      <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                    </select>
                  </label>
                </div>
              </section>
              <DataTable :columns="trafficRequestColumns" :rows="trafficRequestTableRows" :loading="trafficRequestLoading" row-key="requestId" row-interactive :empty-label="t('trafficMarketplace.emptyRequests')" @row-click="openTrafficRequestDetail">
                <template #cell-seaArea="{ value }">{{ seaAreaLabel(String(value || '')) }}</template>
                <template #cell-useTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
                <template #cell-serviceType="{ value }">{{ trafficTypeLabel(String(value || '')) }}</template>
                <template #cell-allowShare="{ value }">{{ value ? t("common.yes") : t("common.no") }}</template>
                <template #cell-status="{ value }"><StatusBadge :label="trafficRequestStatusLabel(String(value || ''))" variant="info" /></template>
                <template #cell-operation="{ row }">
                  <div class="table-action-buttons table-action-buttons--center">
                    <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openTrafficRequestDetail(row)" />
                    <IconButton icon="Send" :label="t('trafficMarketplace.action.quote')" variant="primary" :loading="trafficRequestSaving" @click.stop="openTrafficQuoteDialog(row)" />
                  </div>
                </template>
              </DataTable>
              <section v-if="selectedTrafficRequest" class="transport-market-detail traffic-hall-quotes">
                <header>
                  <div>
                    <span>{{ selectedTrafficRequest?.requestNo || "-" }}</span>
                    <strong>{{ selectedTrafficRequest?.anchorageName || t("trafficMarketplace.emptyRequests") }}</strong>
                  </div>
                  <StatusBadge v-if="selectedTrafficRequest" :label="trafficRequestStatusLabel(selectedTrafficRequest?.status || '')" variant="info" />
                </header>
                <DataTable :columns="trafficQuoteColumns" :rows="trafficQuoteTableRows" :loading="trafficRequestLoading" row-key="quoteId" :empty-label="t('trafficMarketplace.emptyQuotes')">
                  <template #cell-quoteAmount="{ row, value }">
                    <span :class="{ 'traffic-recommended-price': row.recommended }">{{ value == null ? "-" : formatPurchaseMoneyValue(Number(value)) }}</span>
                  </template>
                  <template #cell-availableStartTime="{ value }">{{ formatDemandDateTime(String(value || '')) || "-" }}</template>
                  <template #cell-status="{ value }"><StatusBadge :label="trafficQuoteStatusLabel(String(value || ''))" variant="info" /></template>
                  <template #cell-operation="{ row }">
                    <IconButton v-if="['SUBMITTED', 'UPDATED'].includes(String(row.status || '').toUpperCase())" icon="Ban" :label="t('trafficMarketplace.action.withdrawQuote')" variant="danger" :loading="trafficRequestSaving" @click.stop="withdrawQuote(row)" />
                  </template>
                </DataTable>
              </section>
            </article>
          </section>
        </template>
        <template v-else>
        <p v-if="trafficServiceNoticeKey" class="permission-static-notice">{{ t(trafficServiceNoticeKey) }}</p>
        <div v-if="trafficServiceErrorKey" class="inline-error inline-error--action">
          <span>{{ t(trafficServiceErrorKey) }}</span>
          <button type="button" @click="loadTrafficShuttles(true)">{{ t("purchaseOrder.action.retry") }}</button>
        </div>
        <section class="traffic-service-hall-stack">
          <template v-if="true">
          <article class="traffic-hall-section">
            <header class="traffic-hall-section__head">
              <div>
                <strong>{{ t("trafficMarketplace.section.shuttleManagement") }}</strong>
                <span>{{ t("trafficMarketplace.section.shuttleManagementHint") }}</span>
              </div>
              <div class="toolbar-icon-actions">
                <IconButton icon="Plus" :label="t('trafficMarketplace.action.createShuttle')" variant="primary" @click="openTrafficShuttleDialog" />
                <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficShuttles(true)" />
                <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficShuttleSearch(true)" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficShuttleLoading" @click="loadTrafficShuttles(true)" />
              </div>
            </header>
            <section class="filter-toolbar traffic-hall-filter" aria-label="supplier traffic shuttle filters">
              <div class="filter-fields">
                <label class="filter-field filter-field--search">
                  <span>{{ t("trafficService.filter.keyword") }}</span>
                  <input v-model="trafficShuttleKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficShuttles(true)" />
                </label>
                <label class="filter-field">
                  <span>{{ t("trafficService.field.seaArea") }}</span>
                  <select v-model="trafficShuttleSeaArea" @change="trafficShuttleAnchorageCode = ''">
                    <option value="">{{ t("common.all") }}</option>
                    <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                    <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                  </select>
                </label>
                <label class="filter-field">
                  <span>{{ t("trafficService.field.anchorage") }}</span>
                  <select v-model="trafficShuttleAnchorageCode">
                    <option value="">{{ t("common.all") }}</option>
                    <option v-for="anchorage in trafficShuttleFilterAnchorages" :key="anchorage.anchorageCode" :value="anchorage.anchorageCode">
                      {{ anchorage.anchorageName }}
                    </option>
                  </select>
                </label>
                <label class="filter-field">
                  <span>服务日期</span>
                  <StableDateTimeInput v-model="trafficShuttleServiceDate" mode="date" />
                </label>
                <label class="filter-field">
                  <span>{{ t("field.status") }}</span>
                  <select v-model="trafficShuttleStatus">
                    <option value="">{{ t("common.all") }}</option>
                    <option value="DRAFT">{{ t("trafficMarketplace.status.pendingPublish") }}</option>
                    <option value="PUBLISHED">已发布</option>
                    <option value="FULL">预约中</option>
                    <option value="IN_PROGRESS">执行中</option>
                    <option value="COMPLETED">已完成</option>
                    <option value="CLOSED">{{ t("trafficMarketplace.status.cancelled") }}</option>
                  </select>
                </label>
              </div>
            </section>
            <div v-if="trafficShuttleLoading" class="empty-state compact">{{ t("common.loading") }}</div>
            <div v-else-if="!trafficShuttleVisualRows.length" class="empty-state compact">{{ t("trafficMarketplace.emptyShuttles") }}</div>
            <section v-else class="traffic-shuttle-visual-list traffic-shuttle-management-cards">
              <article
                v-for="row in trafficShuttleVisualRows"
                :key="`managed-shuttle-${row.shuttleId}`"
                :class="['traffic-shuttle-visual-card is-management', `is-${trafficShuttleExecutionCardState(row)}`]"
              >
                <header class="traffic-shuttle-visual-card__head">
                  <div class="traffic-shuttle-visual-title">
                    <div class="traffic-shuttle-visual-name">
                      <strong>{{ row.trafficVesselName || "-" }}</strong>
                      <span>{{ row.shuttleNo || "-" }}</span>
                    </div>
                    <div class="traffic-shuttle-price-pair">
                      <span><b>单</b><em>{{ trafficShuttleBasePriceLabel(row) }}</em></span>
                      <span><b>拼</b><em>{{ trafficShuttleSharedPriceLabel(row) }}</em></span>
                      <small>增值服务：{{ trafficShuttleOtherFeeLabel(row) }}</small>
                    </div>
                  </div>
                  <div class="traffic-shuttle-card-status-corner">
                    <StatusBadge :label="trafficShuttleOperationalStatus(row).label" :variant="trafficShuttleOperationalStatus(row).variant" />
                  </div>
                </header>
                <section class="traffic-shuttle-diagram" aria-label="驳船服务路线">
                  <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--start">
                    <span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--pier">⛴</span>
                    <div><strong>{{ trafficShuttleOriginLabel(row) }}</strong><em>{{ trafficShuttleStartTimeLabel(row) }}</em></div>
                  </div>
                  <div class="traffic-shuttle-node-track">
                    <div class="traffic-shuttle-dashed-line" aria-hidden="true"></div>
                    <div class="traffic-shuttle-node-strip">
                      <div v-for="(node, index) in trafficShuttleNodeRows(row)" :key="`managed-${row.shuttleId}-${index}`" class="traffic-shuttle-node-point" :class="`is-${trafficShuttleManagedNodeVisualState(row, node, index)}`">
                        <span class="traffic-shuttle-node-state-label">{{ trafficShuttleNodeDisplayStatus(row, index) }}</span>
                        <button type="button" class="traffic-shuttle-node-dot" :aria-label="`查看${node.nodeName || `节点${index + 1}`}明细`" @click.stop="openManagedShuttleNode(row, index)"></button>
                        <em>{{ trafficShuttleNodeTimeLabel(node) }}</em>
                      </div>
                    </div>
                  </div>
                  <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--end">
                    <span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--anchor">⚓</span>
                    <div><strong>{{ trafficShuttleDestinationLabel(row) }}</strong><em>{{ trafficShuttleReturnTimeLabel(row) }}</em></div>
                  </div>
                </section>
                <footer class="traffic-shuttle-visual-card__foot">
                  <span class="traffic-shuttle-date-chip">{{ trafficShuttleServiceDateLabel(row) || "-" }}</span>
                  <div class="traffic-shuttle-management-status-actions">
                    <IconButton icon="Pencil" label="编辑驳船服务" @click.stop="openTrafficShuttleEditDialog(row)" />
                    <IconButton v-if="['PUBLISHED', 'FULL'].includes(String(row.status || '').toUpperCase())" icon="Send" label="开始执行" variant="primary" :loading="trafficShuttleSaving" @click.stop="executeTrafficShuttle(row)" />
                    <IconButton v-if="String(row.status || '').toUpperCase() === 'IN_PROGRESS'" icon="Check" label="执行完成" variant="primary" :loading="trafficShuttleSaving" @click.stop="completeTrafficShuttleAction(row)" />
                    <IconButton icon="X" :label="t('trafficMarketplace.action.closeShuttle')" variant="danger" :loading="trafficShuttleSaving" @click.stop="closeShuttle(row)" />
                  </div>
                </footer>
              </article>
            </section>
          </article>
          </template>
          <template v-else>
          <article class="traffic-hall-section">
            <header class="traffic-hall-section__head">
              <div>
                <strong>{{ t("trafficMarketplace.section.orderManagement") }}</strong>
                <span>{{ t("trafficMarketplace.section.orderManagementHint") }}</span>
              </div>
              <div class="toolbar-icon-actions">
                <IconButton icon="Search" :label="t('common.search')" @click="loadTrafficServices" />
                <IconButton icon="X" :label="t('common.reset')" @click="resetTrafficServiceSearch" />
                <IconButton icon="RefreshCw" :label="t('action.refresh')" :loading="trafficServiceLoading" @click="loadTrafficServices" />
              </div>
            </header>
            <section class="filter-toolbar traffic-hall-filter" aria-label="third party traffic service filters">
              <div class="filter-fields">
                <label class="filter-field filter-field--search">
                  <span>{{ t("trafficService.filter.keyword") }}</span>
                  <input v-model="trafficServiceKeyword" :placeholder="orderVesselSearchPlaceholder" @keydown.enter.prevent="loadTrafficServices" />
                </label>
                <label class="filter-field">
                  <span>{{ t("trafficService.field.seaArea") }}</span>
                  <select v-model="trafficServiceSeaArea">
                    <option value="">{{ t("common.all") }}</option>
                    <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                    <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                  </select>
                </label>
                <label class="filter-field">
                  <span>{{ t("field.status") }}</span>
                  <select v-model="trafficServiceStatus">
                    <option value="">{{ t("common.all") }}</option>
                    <option value="PENDING_CONFIRM">{{ t("trafficService.status.pendingConfirm") }}</option>
                    <option value="WAITING_SERVICE">{{ t("trafficService.status.waitingService") }}</option>
                    <option value="DISCARDED">{{ t("trafficService.status.discarded") }}</option>
                  </select>
                </label>
              </div>
            </section>
            <div v-if="trafficServiceLoading" class="empty-state compact">{{ t("common.loading") }}</div>
            <div v-else-if="!trafficServiceRows.length" class="empty-state compact">{{ t("trafficService.empty") }}</div>
            <section v-else class="traffic-service-order-card-list">
              <article v-for="row in trafficServiceVisualRows" :key="row.serviceOrderId" class="traffic-service-order-card" tabindex="0" @click="openTrafficServiceDetail(row as unknown as Record<string, unknown>)" @keydown.enter="openTrafficServiceDetail(row as unknown as Record<string, unknown>)">
                <div class="traffic-service-order-card__identity">
                  <span>{{ row.serviceNo || "-" }}</span>
                  <strong>{{ row.trafficVesselName || "待分配驳船" }}</strong>
                  <small>{{ seaAreaLabel(row.seaArea) }} · {{ row.anchorageName || "-" }}</small>
                </div>
                <div class="traffic-service-order-card__route">
                  <span>计划用艇</span>
                  <strong>{{ formatDemandDateTime(row.useTime) || "-" }}</strong>
                  <small>{{ trafficTypeLabel(row.serviceType) }} · {{ trafficFeeTypeLabel(row.feeType || "") }}</small>
                </div>
                <div class="traffic-service-order-card__price">
                  <span>服务金额</span>
                  <strong>{{ formatPurchaseMoneyValue(Number(row.allowShare ? row.sharedPrice : row.basePrice || 0)) }}</strong>
                </div>
                <div class="traffic-service-order-card__status">
                  <span>订单状态</span>
                  <StatusBadge :label="trafficServiceStatusLabel(row.status)" :variant="trafficServiceStatusVariant(row.status)" />
                  <div class="traffic-service-order-card__actions">
                    <IconButton icon="Eye" :label="t('action.viewDetail')" @click.stop="openTrafficServiceDetail(row as unknown as Record<string, unknown>)" />
                    <IconButton v-if="String(row.status || '').toUpperCase() === 'PENDING_CONFIRM'" icon="Check" :label="t('common.confirm')" variant="primary" @click.stop="confirmTrafficService(row as unknown as Record<string, unknown>)" />
                  </div>
                </div>
              </article>
            </section>
          </article>
          </template>
        </section>
        </template>
        </template>
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

      <ExpandablePanel v-else-if="pageKey === 'dataDictionary'" :show-header="false" :show-expand="false" class="data-dictionary-panel">
        <LoadingOverlay :active="dictionaryLoading" :label="t('common.loading')">
          <section class="dictionary-workspace">
            <p v-if="dictionaryErrorKey" class="permission-static-notice">{{ t(dictionaryErrorKey) }}</p>
            <p v-if="dictionaryNotice" class="permission-static-notice">{{ dictionaryNotice }}</p>
            <div class="dictionary-grid">
              <section class="dictionary-card">
                <div class="dictionary-card-header">
                  <div class="dictionary-card-tools">
                    <label class="list-search-field dictionary-search">
                      <span class="list-search-icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24">
                          <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                        </svg>
                      </span>
                      <input v-model="dictionaryTypeKeyword" type="search" :placeholder="t('dataDictionary.typePlaceholder')" @keyup.enter="loadDictionaryTypes" />
                    </label>
                    <IconButton icon="Search" :label="t('action.search')" @click="loadDictionaryTypes" />
                    <IconButton icon="X" :label="t('action.clear')" @click="dictionaryTypeKeyword = ''; loadDictionaryTypes()" />
                    <IconButton icon="RefreshCw" :label="t('action.refresh')" @click="loadDictionaryTypes" />
                  </div>
                </div>
                <DataTable :columns="dictionaryTypeColumns" :rows="dictionaryTypeRows" row-key="typeCode" :expanded-row-key="selectedDictionaryTypeCode" @row-click="selectDictionaryType">
                  <template #cell-enabled="{ value }">
                    <StatusBadge :label="value ? t('dataDictionary.enabled') : t('dataDictionary.disabled')" :variant="value ? 'success' : 'neutral'" />
                  </template>
                  <template #cell-description="{ value }">
                    {{ formatEmpty(String(value || "")) }}
                  </template>
                </DataTable>
              </section>

              <section class="dictionary-card dictionary-items-card">
                <div class="dictionary-card-header">
                  <div class="dictionary-card-tools">
                    <label class="list-search-field dictionary-search">
                      <span class="list-search-icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24">
                          <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                        </svg>
                      </span>
                      <input v-model="dictionaryItemKeyword" type="search" :placeholder="t('dataDictionary.itemPlaceholder')" @keyup.enter="loadDictionaryItems(selectedDictionaryTypeCode)" />
                    </label>
                    <IconButton icon="Search" :label="t('action.search')" :disabled="!selectedDictionaryTypeCode" @click="loadDictionaryItems(selectedDictionaryTypeCode)" />
                    <IconButton icon="X" :label="t('action.clear')" :disabled="!selectedDictionaryTypeCode" @click="dictionaryItemKeyword = ''; loadDictionaryItems(selectedDictionaryTypeCode)" />
                    <IconButton icon="RefreshCw" :label="t('action.refresh')" :disabled="!selectedDictionaryTypeCode" @click="loadDictionaryItems(selectedDictionaryTypeCode)" />
                  </div>
                </div>
                <p v-if="!selectedDictionaryTypeCode" class="empty-state compact">{{ t("dataDictionary.selectTypeFirst") }}</p>
                <template v-else>
                  <DataTable :columns="dictionaryItemColumns" :rows="dictionaryItemRows" row-key="id">
                    <template #cell-itemValue="{ value }">{{ formatEmpty(String(value || "")) }}</template>
                    <template #cell-itemNameEn="{ value }">{{ formatEmpty(String(value || "")) }}</template>
                    <template #cell-description="{ value }">{{ formatEmpty(String(value || "")) }}</template>
                    <template #cell-enabled="{ value }">
                      <StatusBadge :label="value ? t('dataDictionary.enabled') : t('dataDictionary.disabled')" :variant="value ? 'success' : 'neutral'" />
                    </template>
                  </DataTable>
                </template>
              </section>
            </div>
          </section>
        </LoadingOverlay>
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

      <section v-else-if="servicePendingPageKeys.has(pageKey)" class="service-pending-workspace">
        <article class="service-pending-hero">
          <div class="service-pending-orbit" aria-hidden="true">
            <span>{{ currentPendingService.icon }}</span>
          </div>
          <div class="service-pending-copy">
            <span>{{ t(currentPendingService.labelKey) }}</span>
            <h1>{{ currentPendingService.title }}</h1>
            <p>{{ currentPendingService.subtitle }}</p>
          </div>
          <div class="service-pending-status">
            <strong>正在接入中</strong>
            <small>能力编排 / 接口联调 / 数据验收</small>
          </div>
        </article>
        <section class="service-pending-grid">
          <div>
            <span>01</span>
            <strong>业务模型梳理</strong>
            <p>{{ currentPendingService.metric }}的流程、角色和状态正在收敛。</p>
          </div>
          <div>
            <span>02</span>
            <strong>数据接口联调</strong>
            <p>外部系统、单证数据和平台订单链路将统一接入。</p>
          </div>
          <div>
            <span>03</span>
            <strong>页面即将开放</strong>
            <p>完成验收后会进入当前工作台菜单，无需切换系统。</p>
          </div>
        </section>
      </section>
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
            <div v-if="purchaseOrderCreating" class="purchase-order-confirm-loading-mask" role="status" aria-live="polite">
              <span>{{ t("common.loading") }}</span>
            </div>
            <header>
              <div>
                <strong>{{ t("purchaseOrder.dialog.title") }}</strong>
                <span>{{ t("purchaseOrder.dialog.subtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="purchaseOrderDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body">
              <section class="purchase-order-checkout-hero">
                <div class="purchase-order-checkout-total">
                  <span>确认下单船舶</span>
                  <strong>{{ compareSupplyForm.vessel || "-" }}</strong>
                  <p>{{ compareSupplyForm.inquiryNo || "-" }} · {{ compareSupplyModeLabel }} · {{ compareSupplyForm.port || "-" }}</p>
                </div>
                <dl class="purchase-order-checkout-metrics">
                  <div>
                    <dt>{{ t("purchaseOrder.field.strategy") }}</dt>
                    <dd>{{ activeCompareStrategy?.label || "-" }}</dd>
                  </div>
                  <div>
                    <dt>{{ t("purchaseOrder.dialog.supplierCount") }}</dt>
                    <dd>{{ selectedCompareSupplierCount }}</dd>
                  </div>
                  <div>
                    <dt>{{ t("purchaseOrder.dialog.orderableCount") }}</dt>
                    <dd>{{ activePurchaseOrderItems.length }} / {{ selectedCompareTotalSkuCount }}</dd>
                  </div>
                </dl>
              </section>
              <section v-if="selectedCompareOrderNotices.length" class="purchase-order-warning-list">
                <span v-for="notice in selectedCompareOrderNotices" :key="notice">{{ notice }}</span>
              </section>
              <section class="purchase-order-section purchase-order-delivery-section">
                <header class="purchase-order-section-heading">
                  <div>
                    <strong>运输信息</strong>
                  </div>
                  <span>确认联系人、到港和送达安排</span>
                </header>
                <div class="purchase-order-form-grid">
                  <label>
                    <span>{{ t("purchaseOrder.field.requiredDeliveryTime") }}</span>
                    <StableDateTimeInput
                      v-model="purchaseOrderForm.requiredDeliveryTime"
                      mode="datetime"
                      step="60"
                      :placeholder="t('purchaseOrder.placeholder.requiredDeliveryTime')"
                      :class="{ 'is-invalid': purchaseOrderFormErrors.requiredDeliveryTime }"
                      :aria-invalid="Boolean(purchaseOrderFormErrors.requiredDeliveryTime)"
                      :disabled="purchaseOrderCreating"
                      @input="clearPurchaseOrderFormError('requiredDeliveryTime')"
                    />
                    <small v-if="purchaseOrderFormErrors.requiredDeliveryTime">{{ purchaseOrderFormErrors.requiredDeliveryTime }}</small>
                  </label>
                  <label>
                    <span>{{ t("purchaseOrder.field.deliveryContact") }}</span>
                    <select v-model="purchaseOrderForm.deliveryContactId" :disabled="purchaseOrderCreating" @change="handlePurchaseOrderContactChange">
                      <option :value="fallbackDeliveryContactId">{{ fallbackDeliveryContact.contactName }} / {{ fallbackDeliveryContact.contactPhone }}</option>
                      <option v-for="contact in activeCompanyContacts" :key="contact.id" :value="String(contact.contactId || contact.id)">
                        {{ contact.contactName }} / {{ contact.contactPhone }}
                      </option>
                    </select>
                  </label>
                  <label>
                    <span>{{ t("purchaseOrder.field.deliveryContactPhone") }}</span>
                    <input
                      v-model="purchaseOrderForm.deliveryContactPhone"
                      type="text"
                      :placeholder="t('purchaseOrder.placeholder.deliveryContactPhone')"
                      :class="{ 'is-invalid': purchaseOrderFormErrors.deliveryContactPhone }"
                      :aria-invalid="Boolean(purchaseOrderFormErrors.deliveryContactPhone)"
                      :disabled="purchaseOrderCreating"
                      @input="clearPurchaseOrderFormError('deliveryContactPhone')"
                    />
                    <small v-if="purchaseOrderFormErrors.deliveryContactPhone">{{ purchaseOrderFormErrors.deliveryContactPhone }}</small>
                  </label>
                  <label>
                    <span>{{ t("purchaseOrder.field.deliveryContactEmail") }}</span>
                    <input
                      v-model="purchaseOrderForm.deliveryContactEmail"
                      type="email"
                      :placeholder="t('purchaseOrder.placeholder.deliveryContactEmail')"
                      :disabled="purchaseOrderCreating"
                    />
                  </label>
                  <label class="span-2 purchase-order-remark-field">
                    <span>{{ t("purchaseOrder.field.deliveryAddress") }}</span>
                    <textarea v-model="purchaseOrderForm.buyerRemark" rows="3" :placeholder="t('purchaseOrder.placeholder.buyerRemark')" :disabled="purchaseOrderCreating"></textarea>
                  </label>
                </div>
              </section>
              <section v-if="selectedCompareSupplierSummaries.length" class="purchase-order-supplier-summary">
                <header class="purchase-order-subtle-heading">
                  <strong>{{ t("purchaseOrder.dialog.supplierSubtotal") }}</strong>
                  <span><b>{{ formatCompareMoney(selectedCompareCostAmount, selectedCompareCostAmountUsd) }}</b></span>
                </header>
                <div>
                  <article v-for="(supplier, index) in selectedCompareSupplierSummaries" :key="supplier.supplier" :class="{ 'is-winner': isPurchaseSupplierWinner(index) }">
                    <em>{{ purchaseSupplierAwardLabel(index) }}</em>
                    <strong>{{ supplier.supplier }}</strong>
                    <span>{{ t("compare.supplierAmount.skuCount", { count: supplier.count }) }}</span>
                    <b>{{ formatCompareMoney(supplier.amount, supplier.amountUsd) }}</b>
                  </article>
                </div>
              </section>
              <section class="purchase-order-section purchase-order-supply-section">
                <header class="purchase-order-section-heading">
                  <div>
                    <strong>补给费用</strong>
                  </div>
                  <em class="purchase-order-supply-mode-chip">{{ compareSupplyForm.supplyMode === "LAND" ? "🚚 陆运" : "⛴ 海运" }}</em>
                </header>
                <article v-if="compareSupplyForm.supplyMode === 'SEA'" class="purchase-order-supply-card purchase-order-supply-card--barge">
                  <div class="purchase-order-barge-card__head">
                    <div>
                      <strong>{{ [compareTrafficServiceForm.trafficVesselName || compareFixedProviderDisplayName, purchaseOrderBargeDateLabel].filter(Boolean).join(" / ") || "-" }}</strong>
                      <span>{{ compareTrafficServiceForm.shuttleNo || compareSupplyForm.inquiryNo || "-" }}</span>
                    </div>
                    <div class="purchase-order-barge-card__selected-fees">
                      <strong>{{ formatCompareMoney(compareFixedFeeTotal, compareFixedFeeTotalUsd) }}</strong>
                      <span v-for="item in purchaseOrderBargeSelectedFeeItems" :key="item.key">
                        {{ item.label }} {{ compareFixedFeeDisplayLabel(item.value) }}
                      </span>
                    </div>
                  </div>
                  <div class="purchase-order-barge-card__route">
                    <span>
                      <i>⛴</i>
                      <strong>{{ purchaseOrderBargeDeparturePointLabel }}</strong>
                      <small>{{ purchaseOrderBargeOriginTimeLabel }}</small>
                    </span>
                    <div>
                      <em v-for="(node, index) in purchaseOrderBargeNodeRows" :key="`${node.nodeName || index}-${index}`">
                        <b>{{ trafficShuttleNodeTimeLabel(node) }}</b>
                      </em>
                    </div>
                    <span>
                      <i>⚓</i>
                      <strong>{{ purchaseOrderBargeDestinationPointLabel }}</strong>
                      <small>{{ purchaseOrderBargeDestinationTimeLabel }}</small>
                    </span>
                  </div>
                </article>
                <article v-else class="purchase-order-supply-card purchase-order-supply-card--supplier">
                  <div class="purchase-order-supply-card__main">
                    <strong>{{ compareFixedProviderDisplayName }}</strong>
                  </div>
                  <div class="purchase-order-supply-card__fees">
                    <span v-for="item in compareFixedFeeDisplayItems.filter((fee) => fee.key !== 'other')" :key="item.key">
                      {{ item.label }} <strong>{{ compareFixedFeeDisplayLabel(item.value) }}</strong>
                    </span>
                  </div>
                  <div class="purchase-order-supply-card__total">
                    <span>合计</span>
                    <strong>{{ formatCompareMoney(compareFixedFeeTotal, compareFixedFeeTotalUsd) }}</strong>
                  </div>
                </article>
              </section>
              <p v-if="purchaseOrderError" class="inline-error">{{ purchaseOrderError }}</p>
            </div>
            <footer class="purchase-order-checkout-footer">
              <div class="purchase-order-checkout-bill">
                <span>报价价格 <strong>{{ formatCompareMoney(selectedCompareOrderAmount, selectedCompareOrderAmountUsd) }}</strong></span>
                <span>成本价格 <strong>{{ formatCompareMoney(selectedCompareCostAmount, selectedCompareCostAmountUsd) }}</strong></span>
                <span>补给费用 <strong>{{ formatCompareMoney(compareFixedFeeTotal, compareFixedFeeTotalUsd) }}</strong></span>
                <span class="is-profit">利润 <strong>{{ formatCompareMoney(selectedCompareProfitAmount, selectedCompareProfitAmountUsd) }}</strong></span>
              </div>
              <nav>
                <IconButton icon="X" :label="t('common.cancel')" @click="purchaseOrderDialogOpen = false" />
                <IconButton icon="Send" :label="t('purchaseOrder.action.confirmCreate')" variant="primary" :loading="purchaseOrderCreating" @click="submitPurchaseOrder" />
              </nav>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="compareSupplierDialogOpen" class="modal-backdrop" role="presentation" @click="compareSupplierDialogOpen = false">
          <section class="purchase-order-confirm-dialog compare-supplier-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>选择供货商</strong>
                <span>陆运模式下，补给费用读取供货商企业维护的增值服务费用。</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="compareSupplierDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body compare-supplier-body">
              <div v-if="compareSupplierLoading" class="empty-state compact">{{ t("common.loading") }}</div>
              <div v-else-if="!compareSupplierProviderOptions.length" class="empty-state compact">暂无可选择供货商</div>
              <section v-else class="compare-supplier-list">
                <button
                  v-for="option in compareSupplierProviderOptions"
                  :key="option.id"
                  type="button"
                  :class="{ active: compareFixedProvider.type === 'SUPPLIER' && compareFixedProvider.id === option.id }"
                  @click="selectCompareSupplierProvider(option)"
                >
                  <span class="compare-supplier-name">
                    <strong>{{ option.supplierName }}</strong>
                    <small>匹配 {{ option.skuCount || 0 }} 项</small>
                  </span>
                  <span class="compare-supplier-fees">
                    <small>运维 {{ compareSupplierFeeLabel(option.id, "freightPrice") }}</small>
                    <small>报关 {{ compareSupplierFeeLabel(option.id, "customsPrice") }}</small>
                    <small>吊机 {{ compareSupplierFeeLabel(option.id, "cranePrice") }}</small>
                  </span>
                  <em>{{ compareSupplierFeeTotalLabel(option.id) }}</em>
                </button>
              </section>
              <p v-if="compareSupplierError" class="inline-error">{{ compareSupplierError }}</p>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="compareTrafficDialogOpen" class="modal-backdrop" role="presentation" @click="compareTrafficDialogOpen = false">
          <section class="purchase-order-confirm-dialog traffic-service-dialog compare-traffic-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("trafficService.dialog.queryFreightTitle") }}</strong>
                <span>{{ t("trafficService.dialog.compareSubtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="compareTrafficDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body">
              <section class="purchase-order-form-grid traffic-service-form-grid compare-traffic-filter-grid">
                <label>
                  <span>{{ t("trafficService.field.anchorage") }}</span>
                  <select v-model="compareTrafficServiceForm.anchorageCode" @change="loadCompareTrafficShuttles">
                    <option value="">{{ t("common.all") }}</option>
                    <option v-for="anchorage in compareTrafficAnchorages" :key="anchorage.anchorageCode" :value="anchorage.anchorageCode">
                      {{ anchorage.anchorageName }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.useTime") }}</span>
                  <StableDateTimeInput v-model="compareTrafficServiceForm.useTime" mode="datetime" @update:model-value="loadCompareTrafficShuttles" />
                </label>
              </section>
              <div v-if="compareTrafficShuttleLoading" class="empty-state compact">{{ t("common.loading") }}</div>
              <div v-else-if="!compareTrafficShuttleVisualRows.length" class="empty-state compact">{{ t("trafficMarketplace.emptyShuttles") }}</div>
              <section v-else class="traffic-shuttle-visual-list compare-traffic-shuttle-list">
                <article
                  v-for="row in compareTrafficShuttleVisualRows"
                  :key="row.shuttleId"
                  :class="['traffic-shuttle-visual-card compare-traffic-shuttle-card', `is-${trafficShuttleExecutionCardState(row)}`]"
                >
                  <header class="traffic-shuttle-visual-card__head">
                    <div class="traffic-shuttle-visual-title">
                      <div class="traffic-shuttle-visual-name">
                        <strong>{{ [row.trafficVesselName, trafficShuttleServiceDateLabel(row)].filter(Boolean).join(" / ") || "-" }}</strong>
                        <span>{{ row.shuttleNo || "-" }}</span>
                      </div>
                      <div class="traffic-shuttle-price-pair">
                        <span><b>单</b><em>{{ trafficShuttleBasePriceLabel(row) }}</em></span>
                        <span><b>拼</b><em>{{ trafficShuttleSharedPriceLabel(row) }}</em></span>
                      </div>
                      <div class="traffic-shuttle-visual-summary">
                        <small>增值服务：{{ trafficShuttleOtherFeeLabel(row) }}</small>
                      </div>
                    </div>
                  </header>
                  <section class="traffic-shuttle-diagram" aria-label="traffic shuttle route diagram">
                    <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--start">
                      <span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--pier" aria-label="departure vessel">⛴</span>
                      <div>
                        <strong>{{ trafficShuttleOriginLabel(row) }}</strong>
                        <em>{{ trafficShuttleStartTimeLabel(row) }}</em>
                      </div>
                    </div>
                    <div class="traffic-shuttle-node-track">
                      <div class="traffic-shuttle-dashed-line" aria-hidden="true"></div>
                      <div class="traffic-shuttle-node-strip">
                        <div
                          v-for="(node, index) in trafficShuttleNodeRows(row)"
                          :key="`${row.shuttleId}-${index}`"
                          class="traffic-shuttle-node-point"
                          :class="[`is-${trafficShuttleNodeVisualState(row, node, index)}`, { 'is-selected': activeTrafficShuttleNodeKey === trafficShuttleNodeKey(row, index) }]"
                        >
                          <span class="traffic-shuttle-node-state-label"></span>
                          <button
                            type="button"
                            class="traffic-shuttle-node-dot"
                            :aria-label="[node.nodeName || `${t('trafficMarketplace.field.serviceNode')}${index + 1}`, [node.startTime, node.endTime].filter(Boolean).join('-') || '-', trafficShuttleNodeShareStatus(row, node)].join(' ')"
                            @click.stop="activeTrafficShuttleNodeKey = activeTrafficShuttleNodeKey === trafficShuttleNodeKey(row, index) ? '' : trafficShuttleNodeKey(row, index)"
                          ></button>
                          <em>{{ trafficShuttleNodeTimeLabel(node) }}</em>
                        </div>
                      </div>
                      <template v-for="(node, index) in trafficShuttleNodeRows(row)" :key="`${row.shuttleId}-${index}-compare-booking-panel`">
                        <Teleport to="body">
                          <div
                            v-if="activeTrafficShuttleNodeKey === trafficShuttleNodeKey(row, index)"
                            class="fulfillment-drawer-backdrop traffic-shuttle-booking-backdrop"
                            role="presentation"
                            @click="activeTrafficShuttleNodeKey = ''; activeTrafficShuttleComparePickerKey = ''"
                          >
                            <div class="traffic-shuttle-node-popover traffic-shuttle-booking-drawer" role="dialog" aria-modal="true" @click.stop>
                              <section class="traffic-shuttle-service-summary">
                            <div>
                              <span>交通艇</span>
                              <strong>{{ row.trafficVesselName || "--" }}</strong>
                            </div>
                            <div>
                              <span>日期</span>
                              <strong>{{ trafficShuttleServiceDateLabel(row) || "--" }}</strong>
                            </div>
                            <div>
                              <span>时间节点</span>
                              <strong>{{ trafficShuttleNodeTimeLabel(node) }}</strong>
                            </div>
                            <div>
                              <span>起始到终点</span>
                              <strong>{{ trafficShuttleRouteLabel(row) }}</strong>
                            </div>
                            <div>
                              <span>单船价格</span>
                              <strong>{{ trafficShuttleBasePriceLabel(row) }}</strong>
                            </div>
                            <div>
                              <span>拼船价格</span>
                              <strong>{{ trafficShuttleSharedPriceLabel(row) }}</strong>
                            </div>
                            <div>
                              <span>报关费</span>
                              <strong>{{ trafficShuttleMoneyLabel(row.customsPrice) }}</strong>
                            </div>
                            <div>
                              <span>吊机价格（每吊）</span>
                              <strong>{{ trafficShuttleMoneyLabel(row.cranePrice) }}</strong>
                            </div>
                              </section>
                              <section class="traffic-shuttle-node-popover__info">
                            <div class="traffic-shuttle-vessel-tabs" role="tablist" aria-label="预约船舶">
                              <button
                                type="button"
                                :class="{ active: trafficShuttleNodeDraft(row, index).activeVesselSlot === 1 }"
                                @click.stop="setTrafficShuttleActiveVesselSlot(row, index, 1)"
                              >
                                预约船舶一
                              </button>
                              <button
                                type="button"
                                :class="{ active: trafficShuttleNodeDraft(row, index).activeVesselSlot === 2 }"
                                @click.stop="setTrafficShuttleActiveVesselSlot(row, index, 2)"
                              >
                                预约船舶二
                              </button>
                            </div>
                            <article class="traffic-shuttle-vessel-card traffic-shuttle-vessel-card--tabbed">
                              <dl>
                                <div>
                                  <dt>预约船舶</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot) }}</dd>
                                </div>
                                <div>
                                  <dt>货物重量（KG）</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "cargoWeight") }}</dd>
                                </div>
                                <div>
                                  <dt>货物体积（平方米）</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "cargoVolume") }}</dd>
                                </div>
                                <div>
                                  <dt>托盘数量</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "palletCount") }}</dd>
                                </div>
                                <div>
                                  <dt>抛锚时间</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "anchorageTime") }}</dd>
                                </div>
                                <div>
                                  <dt>抛锚经纬度</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotValue(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "anchoragePosition") }}</dd>
                                </div>
                                <div>
                                  <dt>{{ trafficShuttleFreightFeeTitle(row, index) }}</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotFeeLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "freightFee") }}</dd>
                                </div>
                                <div>
                                  <dt>报关费</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotFeeLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "customsFee") }}</dd>
                                </div>
                                <div>
                                  <dt>吊机费</dt>
                                  <dd>{{ trafficShuttleNodeVesselSlotFeeLabel(row, index, trafficShuttleNodeDraft(row, index).activeVesselSlot, "craneFee") }}</dd>
                                </div>
                                <div v-if="trafficShuttleNodeReservationTags(row, index).length">
                                  <dt>增值服务</dt>
                                  <dd class="traffic-shuttle-reservation-tags traffic-shuttle-reservation-tags--inline">
                                    <span v-for="tag in trafficShuttleNodeReservationTags(row, index)" :key="tag">{{ tag }}</span>
                                  </dd>
                                </div>
                              </dl>
                            </article>
                              </section>
                              <section class="traffic-shuttle-node-settings">
                            <label>
                              <span>拼船</span>
                              <select v-model="trafficShuttleNodeDraft(row, index).allowShare">
                                <option :value="true">是</option>
                                <option :value="false">否</option>
                              </select>
                            </label>
                            <label>
                              <span>报关</span>
                              <select v-model="trafficShuttleNodeDraft(row, index).customsService">
                                <option :value="false">否</option>
                                <option :value="true">是</option>
                              </select>
                            </label>
                            <label>
                              <span>吊机</span>
                              <select v-model="trafficShuttleNodeDraft(row, index).craneService">
                                <option :value="false">否</option>
                                <option :value="true">是</option>
                              </select>
                            </label>
                            <label>
                              <span>吊机次数</span>
                              <input v-model.number="trafficShuttleNodeDraft(row, index).craneCount" type="number" min="1" step="1" :disabled="!trafficShuttleNodeDraft(row, index).craneService" />
                            </label>
                            <div class="traffic-shuttle-node-cargo-fields">
                              <label>
                                <span>货物重量（KG）</span>
                                <input v-model="trafficShuttleNodeDraft(row, index).cargoWeight" placeholder="--" @input="syncTrafficShuttleDraftToSlot(row, index)" />
                              </label>
                              <label>
                                <span>货物体积（平方米）</span>
                                <input v-model="trafficShuttleNodeDraft(row, index).cargoVolume" placeholder="--" @input="syncTrafficShuttleDraftToSlot(row, index)" />
                              </label>
                              <label>
                                <span>托盘数量</span>
                                <input v-model="trafficShuttleNodeDraft(row, index).palletCount" placeholder="--" @input="syncTrafficShuttleDraftToSlot(row, index)" />
                              </label>
                              <label>
                                <span>抛锚经度</span>
                                <input v-model="trafficShuttleNodeDraft(row, index).anchorageLongitude" placeholder="--" @input="updateTrafficShuttleAnchoragePosition(row, index)" />
                              </label>
                              <label>
                                <span>抛锚纬度</span>
                                <input v-model="trafficShuttleNodeDraft(row, index).anchorageLatitude" placeholder="--" @input="updateTrafficShuttleAnchoragePosition(row, index)" />
                              </label>
                            </div>
                            <div class="traffic-shuttle-node-popover__footer">
                              <IconButton
                                icon="X"
                                label="关闭预约抽屉"
                                @click.stop="activeTrafficShuttleNodeKey = ''; activeTrafficShuttleComparePickerKey = ''"
                              />
                              <IconButton
                                icon="Send"
                                label="预约"
                                variant="primary"
                                :loading="trafficShuttleBookingSavingKey === trafficShuttleNodeKey(row, index)"
                                @click.stop="confirmTrafficShuttleNodeReservation(row, index)"
                              />
                            </div>
                              </section>
                              <section class="traffic-shuttle-node-search">
                            <label class="traffic-shuttle-node-booking__compare">
                              <span>单号</span>
                              <div>
                                <input
                                  v-model="trafficShuttleNodeDraft(row, index).compareNo"
                                  placeholder="输入询价单号联想"
                                  @focus="openTrafficShuttleComparePicker(row, index)"
                                  @input="updateTrafficShuttleCompareKeyword(row, index)"
                                />
                              </div>
                              <div v-if="activeTrafficShuttleComparePickerKey === trafficShuttleNodeKey(row, index)" class="traffic-shuttle-inquiry-picker">
                                <p v-if="trafficShuttleInquiryLoading">正在加载比价中询价单</p>
                                <button
                                  v-for="option in trafficShuttleInquirySuggestions(row, index)"
                                  :key="option.inquiryNo"
                                  type="button"
                                  @click.stop="selectTrafficShuttleInquiry(row, index, option)"
                                >
                                  <strong>{{ option.inquiryNo }}</strong>
                                  <span>{{ option.vesselName }} / {{ option.status }}</span>
                                </button>
                                <p v-if="!trafficShuttleInquiryLoading && !trafficShuttleInquirySuggestions(row, index).length">暂无比价中询价单</p>
                              </div>
                            </label>
                              </section>
                            </div>
                          </div>
                        </Teleport>
                      </template>
                    </div>
                    <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--end">
                      <span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--anchor" aria-label="destination anchorage">⚓</span>
                      <div>
                        <strong>{{ trafficShuttleDestinationLabel(row) }}</strong>
                        <em>{{ trafficShuttleReturnTimeLabel(row) }}</em>
                      </div>
                    </div>
                  </section>
                </article>
              </section>
              <p v-if="compareTrafficError" class="inline-error">{{ compareTrafficError }}</p>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="trafficRouteDialogOpen || trafficServiceDialogOpen || trafficRequestDialogOpen || trafficQuoteDialogOpen || trafficShuttleDialogOpen" class="modal-backdrop" role="presentation" @click="trafficRouteDialogOpen = false; trafficServiceDialogOpen = false; trafficRequestDialogOpen = false; trafficQuoteDialogOpen = false; trafficShuttleDialogOpen = false; editingTrafficShuttleId = null">
          <section v-if="trafficRouteDialogOpen" class="purchase-order-confirm-dialog traffic-service-dialog" role="dialog" aria-modal="true" @click.stop>
            <header class="purchase-order-confirm-header">
              <div>
                <strong>{{ t("trafficRoute.action.create") }}</strong>
                <span>{{ t("trafficRoute.subtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="trafficRouteDialogOpen = false" />
            </header>
            <section class="purchase-order-form-grid traffic-service-form-grid">
              <label>
                <span>{{ t("trafficRoute.field.routeName") }}</span>
                <input v-model="trafficRouteForm.routeName" :placeholder="t('trafficRoute.field.routeName')" />
              </label>
              <label>
                <span>{{ t("trafficRoute.field.serviceDate") }}</span>
                <StableDateTimeInput v-model="trafficRouteForm.serviceDate" mode="date" />
              </label>
              <label>
                <span>{{ t("trafficService.field.seaArea") }}</span>
                <select v-model="trafficRouteForm.seaArea">
                  <option value="">{{ t("common.all") }}</option>
                  <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                  <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                </select>
              </label>
              <label>
                <span>{{ t("trafficRoute.field.supplier") }}</span>
                <input v-model="trafficRouteForm.supplierCompanyName" :placeholder="t('trafficRoute.field.supplier')" />
              </label>
              <label>
                <span>{{ t("trafficRoute.field.vessel") }}</span>
                <input v-model="trafficRouteForm.trafficVesselName" :placeholder="t('trafficRoute.field.vessel')" />
              </label>
              <label>
                <span>{{ t("trafficRoute.field.departure") }}</span>
                <StableDateTimeInput v-model="trafficRouteForm.plannedDepartureTime" mode="datetime" />
              </label>
              <label>
                <span>{{ t("trafficRoute.field.finish") }}</span>
                <StableDateTimeInput v-model="trafficRouteForm.plannedFinishTime" mode="datetime" />
              </label>
              <label>
                <span>{{ t("trafficRoute.field.estimatedCost") }}</span>
                <input v-model.number="trafficRouteForm.estimatedCost" type="number" min="0" step="0.01" />
              </label>
              <label class="traffic-service-textarea span-2">
                <span>{{ t("field.remark") }}</span>
                <textarea v-model="trafficRouteForm.remark" rows="3" :placeholder="t('field.remark')"></textarea>
              </label>
            </section>
            <p v-if="trafficRouteErrorKey" class="inline-error">{{ t(trafficRouteErrorKey) }}</p>
            <footer class="purchase-order-confirm-actions">
              <IconButton icon="X" :label="t('common.cancel')" @click="trafficRouteDialogOpen = false" />
              <IconButton icon="Save" :label="t('common.save')" variant="primary" :loading="trafficRouteSaving" @click="saveTrafficRoute" />
            </footer>
          </section>

          <section v-else-if="trafficRequestDialogOpen" class="purchase-order-confirm-dialog traffic-service-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("trafficMarketplace.dialog.requestTitle") }}</strong>
                <span>{{ t("trafficMarketplace.dialog.requestSubtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="trafficRequestDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body">
              <section class="purchase-order-form-grid traffic-service-form-grid">
                <label>
                  <span>{{ t("trafficService.field.seaArea") }}</span>
                  <select v-model="trafficRequestForm.seaArea">
                    <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                    <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.anchorage") }}</span>
                  <select v-model="trafficRequestForm.anchorageCode">
                    <option value="">{{ t("trafficService.placeholder.anchorage") }}</option>
                    <option v-for="item in trafficRequestAnchorages" :key="item.anchorageCode" :value="item.anchorageCode">
                      {{ item.anchorageName }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.useTime") }}</span>
                  <StableDateTimeInput v-model="trafficRequestForm.useTime" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.serviceType") }}</span>
                  <select v-model="trafficRequestForm.serviceType">
                    <option value="PERSONNEL">{{ t("trafficService.serviceType.personnel") }}</option>
                    <option value="GOODS">{{ t("trafficService.serviceType.goods") }}</option>
                  </select>
                </label>
                <label v-if="trafficRequestForm.serviceType === 'PERSONNEL'">
                  <span>{{ t("trafficService.field.passengerType") }}</span>
                  <select v-model="trafficRequestForm.passengerType">
                    <option value="NORMAL">{{ t("trafficService.passengerType.normal") }}</option>
                    <option value="JOINT_INSPECTION">{{ t("trafficService.passengerType.jointInspection") }}</option>
                  </select>
                </label>
                <label v-if="trafficRequestForm.serviceType === 'PERSONNEL'">
                  <span>{{ t("trafficService.field.passengerCount") }}</span>
                  <input v-model.number="trafficRequestForm.passengerCount" type="number" min="0" :placeholder="t('trafficService.placeholder.passengerCount')" />
                </label>
                <label v-if="trafficRequestForm.serviceType === 'GOODS'">
                  <span>{{ t("trafficService.field.cargoType") }}</span>
                  <select v-model="trafficRequestForm.cargoType">
                    <option value="CARGO">{{ t("trafficService.cargoType.cargo") }}</option>
                    <option value="SUPPLY">{{ t("trafficService.cargoType.supply") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.allowShare") }}</span>
                  <select v-model="trafficRequestForm.allowShare">
                    <option :value="false">{{ t("common.no") }}</option>
                    <option :value="true">{{ t("common.yes") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.returnTrip") }}</span>
                  <select v-model="trafficRequestForm.returnTrip">
                    <option :value="false">{{ t("common.no") }}</option>
                    <option :value="true">{{ t("common.yes") }}</option>
                  </select>
                </label>
                <label class="span-2 purchase-order-remark-field">
                  <span>{{ t("field.remark") }}</span>
                  <textarea v-model="trafficRequestForm.remark" rows="3" :placeholder="t('trafficService.placeholder.remark')"></textarea>
                </label>
              </section>
              <section class="traffic-cargo-editor">
                <div class="purchase-order-section-header">
                  <h3>{{ t("trafficService.section.cargo") }}</h3>
                  <IconButton icon="Plus" :label="t('trafficService.action.addCargo')" @click="trafficRequestForm.cargos = [...(trafficRequestForm.cargos || []), { cargoName: '', weightKg: undefined, volumeCbm: undefined }]" />
                </div>
                <div v-for="(cargo, index) in trafficRequestForm.cargos" :key="index" class="traffic-cargo-row">
                  <input v-model="cargo.cargoName" :placeholder="t('trafficService.field.cargoName')" />
                  <input v-model.number="cargo.weightKg" type="number" min="0" step="0.01" :placeholder="t('trafficService.field.weightKg')" />
                  <input v-model.number="cargo.volumeCbm" type="number" min="0" step="0.01" :placeholder="t('trafficService.field.volumeCbm')" />
                  <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" @click="trafficRequestForm.cargos = (trafficRequestForm.cargos || []).filter((_, itemIndex) => itemIndex !== index)" />
                </div>
              </section>
              <p v-if="trafficServiceErrorKey" class="inline-error">{{ t(trafficServiceErrorKey) }}</p>
            </div>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="trafficRequestDialogOpen = false" />
              <IconButton icon="Send" :label="t('trafficMarketplace.action.publishRequest')" variant="primary" :loading="trafficRequestSaving" @click="saveTrafficRequest" />
            </footer>
          </section>

          <section v-else-if="trafficQuoteDialogOpen" class="purchase-order-confirm-dialog traffic-service-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("trafficMarketplace.dialog.quoteTitle") }}</strong>
                <span>{{ t("trafficMarketplace.dialog.quoteSubtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="trafficQuoteDialogOpen = false" />
            </header>
            <section class="purchase-order-form-grid traffic-service-form-grid">
              <label>
                <span>{{ t("trafficMarketplace.field.quoteAmount") }}</span>
                <input v-model.number="trafficQuoteForm.quoteAmount" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.availableStartTime") }}</span>
                <StableDateTimeInput v-model="trafficQuoteForm.availableStartTime" mode="datetime" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.availableReturnTime") }}</span>
                <StableDateTimeInput v-model="trafficQuoteForm.availableReturnTime" mode="datetime" />
              </label>
              <label>
                <span>{{ t("trafficService.field.trafficVessel") }}</span>
                <input v-model="trafficQuoteForm.trafficVesselName" :placeholder="t('trafficService.field.trafficVessel')" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.contact") }}</span>
                <input v-model="trafficQuoteForm.contactName" :placeholder="t('trafficMarketplace.field.contact')" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.contactPhone") }}</span>
                <input v-model="trafficQuoteForm.contactPhone" :placeholder="t('trafficMarketplace.field.contactPhone')" />
              </label>
              <label class="span-2 purchase-order-remark-field">
                <span>{{ t("trafficMarketplace.field.message") }}</span>
                <textarea v-model="trafficQuoteForm.message" rows="3" :placeholder="t('trafficMarketplace.field.message')"></textarea>
              </label>
            </section>
            <p v-if="trafficServiceErrorKey" class="inline-error">{{ t(trafficServiceErrorKey) }}</p>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="trafficQuoteDialogOpen = false" />
              <IconButton icon="Send" :label="t('trafficMarketplace.action.submitQuote')" variant="primary" :loading="trafficRequestSaving" @click="saveTrafficQuote" />
            </footer>
          </section>

          <section v-else-if="trafficShuttleDialogOpen" class="purchase-order-confirm-dialog traffic-service-dialog traffic-shuttle-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("trafficMarketplace.dialog.shuttleTitle") }}</strong>
                <span>{{ t("trafficMarketplace.dialog.shuttleSubtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="trafficShuttleDialogOpen = false; editingTrafficShuttleId = null" />
            </header>
            <section class="purchase-order-form-grid traffic-service-form-grid traffic-shuttle-form-grid">
              <label>
                <span>{{ t("trafficService.field.trafficVessel") }}</span>
                <select :value="trafficShuttleForm.trafficVesselId || ''" @change="selectTrafficShuttleVessel(($event.target as HTMLSelectElement).value)">
                  <option value="">{{ t("common.notFilled") }}</option>
                  <option v-for="vessel in activeCompanyVessels" :key="vessel.id" :value="String(vessel.vesselId || vessel.id)">
                    {{ vessel.vesselName }}
                  </option>
                </select>
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.destinationSeaArea") }}</span>
                <select v-model="trafficShuttleForm.seaArea" @change="updateTrafficShuttleSeaArea">
                  <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                  <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                </select>
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.departurePoint") }}</span>
                <input v-model="trafficShuttleForm.departurePoint" :placeholder="t('trafficMarketplace.placeholder.departurePoint')" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.destinationAnchorage") }}</span>
                <select v-model="trafficShuttleForm.anchorageCode">
                  <option value="">{{ t("trafficService.placeholder.anchorage") }}</option>
                  <option v-for="item in trafficShuttleAnchorages" :key="item.anchorageCode" :value="item.anchorageCode">
                    {{ item.anchorageName }}
                  </option>
                </select>
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.startTime") }}</span>
                <StableDateTimeInput v-model="trafficShuttleForm.startTime" mode="datetime" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.returnTime") }}</span>
                <StableDateTimeInput v-model="trafficShuttleForm.returnTime" mode="datetime" />
              </label>
              <label>
                <span>单船价格</span>
                <input v-model.number="trafficShuttleForm.basePrice" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>拼船价格</span>
                <input v-model.number="trafficShuttleForm.sharedPrice" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>报关价格</span>
                <input v-model.number="trafficShuttleForm.customsPrice" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>吊机价格（每吊）</span>
                <input v-model.number="trafficShuttleForm.cranePrice" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.cargoCapacityKg") }}</span>
                <input v-model.number="trafficShuttleForm.cargoCapacityKg" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.cargoCapacityCbm") }}</span>
                <input v-model.number="trafficShuttleForm.cargoCapacityCbm" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>{{ t("trafficMarketplace.field.passengerCapacity") }}</span>
                <input v-model.number="trafficShuttleForm.passengerCapacity" type="number" min="0" />
              </label>
              <label class="span-3 purchase-order-remark-field">
                <span>{{ t("field.remark") }}</span>
                <textarea v-model="trafficShuttleForm.remark" rows="3" :placeholder="t('field.remark')"></textarea>
              </label>
              <section class="traffic-shuttle-node-editor">
                <div class="purchase-order-section-header">
                  <h3>{{ t("trafficMarketplace.section.shuttleNodes") }}</h3>
                  <IconButton icon="Plus" :label="t('trafficMarketplace.action.addShuttleNode')" @click="addTrafficShuttleNode" />
                </div>
                <div class="traffic-shuttle-node-list">
                  <div v-for="(node, index) in trafficShuttleForm.serviceNodes" :key="index" class="traffic-shuttle-node-row">
                    <label>
                      <span>{{ t("trafficMarketplace.field.serviceNode") }}</span>
                      <input v-model="node.nodeName" :placeholder="t('trafficMarketplace.placeholder.serviceNode')" />
                    </label>
                    <label>
                      <span>{{ t("trafficMarketplace.field.nodeStartTime") }}</span>
                      <input v-model="node.startTime" type="time" />
                    </label>
                    <label>
                      <span>{{ t("trafficMarketplace.field.nodeEndTime") }}</span>
                      <input v-model="node.endTime" type="time" />
                    </label>
                    <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" @click="removeTrafficShuttleNode(index)" />
                  </div>
                </div>
              </section>
            </section>
            <p v-if="trafficServiceErrorKey" class="inline-error">{{ t(trafficServiceErrorKey) }}</p>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="trafficShuttleDialogOpen = false; editingTrafficShuttleId = null" />
              <IconButton icon="Save" :label="t('common.save')" :loading="trafficShuttleSaving" @click="saveTrafficShuttle(editingTrafficShuttleId ? undefined : 'DRAFT')" />
              <IconButton icon="Send" :label="t('trafficMarketplace.action.publishShuttle')" variant="primary" :loading="trafficShuttleSaving" @click="saveTrafficShuttle('PUBLISHED')" />
            </footer>
          </section>

          <section v-else-if="trafficServiceDialogOpen" class="purchase-order-confirm-dialog traffic-service-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ selectedTrafficService ? t("trafficService.dialog.editTitle") : t("trafficService.dialog.createTitle") }}</strong>
                <span>{{ t("trafficService.dialog.subtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="trafficServiceDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body">
              <section class="purchase-order-form-grid traffic-service-form-grid">
                <label>
                  <span>{{ t("trafficService.field.seaArea") }}</span>
                  <select v-model="trafficServiceForm.seaArea">
                    <option value="NORTH">{{ t("trafficService.seaArea.north") }}</option>
                    <option value="SOUTH">{{ t("trafficService.seaArea.south") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.anchorage") }}</span>
                  <select v-model="trafficServiceForm.anchorageCode">
                    <option value="">{{ t("trafficService.placeholder.anchorage") }}</option>
                    <option v-for="item in filteredTrafficAnchorages" :key="item.anchorageCode" :value="item.anchorageCode">
                      {{ item.anchorageName }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.feeType") }}</span>
                  <select v-model="trafficServiceForm.feeType">
                    <option value="FREIGHT">{{ t("trafficFeeType.freight") }}</option>
                    <option value="CUSTOMS">{{ t("trafficFeeType.customs") }}</option>
                    <option value="CRANE">{{ t("trafficFeeType.crane") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.useTime") }}</span>
                  <StableDateTimeInput v-model="trafficServiceForm.useTime" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.serviceType") }}</span>
                  <select v-model="trafficServiceForm.serviceType">
                    <option value="PERSONNEL">{{ t("trafficService.serviceType.personnel") }}</option>
                    <option value="GOODS">{{ t("trafficService.serviceType.goods") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.passengerType") }}</span>
                  <select v-model="trafficServiceForm.passengerType">
                    <option value="NORMAL">{{ t("trafficService.passengerType.normal") }}</option>
                    <option value="JOINT_INSPECTION">{{ t("trafficService.passengerType.jointInspection") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.passengerCount") }}</span>
                  <input v-model.number="trafficServiceForm.passengerCount" type="number" min="0" :placeholder="t('trafficService.placeholder.passengerCount')" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.cargoType") }}</span>
                  <select v-model="trafficServiceForm.cargoType">
                    <option value="CARGO">{{ t("trafficService.cargoType.cargo") }}</option>
                    <option value="SUPPLY">{{ t("trafficService.cargoType.supply") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.basePrice") }}</span>
                  <input v-model.number="trafficServiceForm.basePrice" type="number" min="0" step="0.01" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.sharedPrice") }}</span>
                  <input v-model.number="trafficServiceForm.sharedPrice" type="number" min="0" step="0.01" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.returnTrip") }}</span>
                  <select v-model="trafficServiceForm.returnTrip">
                    <option :value="false">{{ t("common.no") }}</option>
                    <option :value="true">{{ t("common.yes") }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.allowShare") }}</span>
                  <select v-model="trafficServiceForm.allowShare">
                    <option :value="false">{{ t("common.no") }}</option>
                    <option :value="true">{{ t("common.yes") }}</option>
                  </select>
                </label>
                <label class="span-2 purchase-order-remark-field">
                  <span>{{ t("field.remark") }}</span>
                  <textarea v-model="trafficServiceForm.remark" rows="3" :placeholder="t('trafficService.placeholder.remark')"></textarea>
                </label>
                <h3 class="traffic-service-form-title span-2">{{ t("trafficService.section.supplement") }}</h3>
                <label>
                  <span>{{ t("trafficService.field.businessContact") }}</span>
                  <select :value="trafficServiceForm.businessContactId || ''" @change="selectTrafficBusinessContact(($event.target as HTMLSelectElement).value)">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option v-for="contact in activeCompanyContacts" :key="contact.id" :value="String(contact.contactId || contact.id)">
                      {{ contact.contactName }} / {{ contact.contactPhone }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.acceptedAt") }}</span>
                  <StableDateTimeInput v-model="trafficServiceForm.acceptedAt" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.trafficVessel") }}</span>
                  <select :value="trafficServiceForm.trafficVesselId || ''" @change="selectTrafficVessel(($event.target as HTMLSelectElement).value)">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option v-for="vessel in activeCompanyVessels" :key="vessel.id" :value="String(vessel.vesselId || vessel.id)">
                      {{ vessel.vesselName }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>{{ t("trafficService.field.handler") }}</span>
                  <select :value="trafficServiceForm.handlerContactId || ''" @change="selectTrafficHandlerContact(($event.target as HTMLSelectElement).value)">
                    <option value="">{{ t("common.notFilled") }}</option>
                    <option v-for="contact in activeCompanyContacts" :key="contact.id" :value="String(contact.contactId || contact.id)">
                      {{ contact.contactName }} / {{ contact.contactPhone }}
                    </option>
                  </select>
                </label>
                <label class="span-2 purchase-order-remark-field">
                  <span>{{ t("trafficService.field.supplierMessage") }}</span>
                  <textarea v-model="trafficServiceForm.supplierMessage" rows="2" :placeholder="t('trafficService.placeholder.supplierMessage')"></textarea>
                </label>
                <h3 class="traffic-service-form-title span-2">{{ t("trafficService.section.handling") }}</h3>
                <label>
                  <span>{{ t("trafficService.field.departureTime") }}</span>
                  <StableDateTimeInput v-model="trafficServiceForm.departureTime" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.arrivalTime") }}</span>
                  <StableDateTimeInput v-model="trafficServiceForm.arrivalTime" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.returnStartTime") }}</span>
                  <StableDateTimeInput v-model="trafficServiceForm.returnStartTime" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.returnEndTime") }}</span>
                  <StableDateTimeInput v-model="trafficServiceForm.returnEndTime" mode="datetime" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.signPhoto") }}</span>
                  <input v-model="trafficServiceForm.signPhotoUrl" :placeholder="t('trafficService.placeholder.photoUrl')" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.pickupPhoto") }}</span>
                  <input v-model="trafficServiceForm.pickupPhotoUrl" :placeholder="t('trafficService.placeholder.photoUrl')" />
                </label>
                <label>
                  <span>{{ t("trafficService.field.returnArrivalPhoto") }}</span>
                  <input v-model="trafficServiceForm.returnArrivalPhotoUrl" :placeholder="t('trafficService.placeholder.photoUrl')" />
                </label>
              </section>
              <section class="traffic-cargo-editor">
                <div class="purchase-order-section-header">
                  <h3>{{ t("trafficService.section.cargo") }}</h3>
                  <IconButton
                    icon="Plus"
                    :label="t('trafficService.action.addCargo')"
                    @click="trafficServiceForm.cargos = [...(trafficServiceForm.cargos || []), { cargoName: '', weightKg: undefined, volumeCbm: undefined }]"
                  />
                </div>
                <div v-for="(cargo, index) in trafficServiceForm.cargos" :key="index" class="traffic-cargo-row">
                  <input v-model="cargo.cargoName" :placeholder="t('trafficService.field.cargoName')" />
                  <input v-model.number="cargo.weightKg" type="number" min="0" step="0.01" :placeholder="t('trafficService.field.weightKg')" />
                  <input v-model.number="cargo.volumeCbm" type="number" min="0" step="0.01" :placeholder="t('trafficService.field.volumeCbm')" />
                  <IconButton icon="Trash2" :label="t('action.delete')" variant="danger" @click="trafficServiceForm.cargos = (trafficServiceForm.cargos || []).filter((_, itemIndex) => itemIndex !== index)" />
                </div>
                <p v-if="!(trafficServiceForm.cargos || []).length" class="empty-state compact">{{ t("common.empty") }}</p>
              </section>
              <p v-if="trafficServiceErrorKey" class="inline-error">{{ t(trafficServiceErrorKey) }}</p>
            </div>
            <footer>
              <IconButton icon="X" :label="t('common.cancel')" @click="trafficServiceDialogOpen = false" />
              <IconButton
                v-if="selectedTrafficService && String(selectedTrafficService.status || '').toUpperCase() === 'PENDING_CONFIRM'"
                icon="Check"
                :label="t('common.confirm')"
                variant="primary"
                :loading="trafficServiceLoading"
                @click="confirmTrafficService(selectedTrafficService)"
              />
              <IconButton icon="Save" :label="t('common.save')" variant="primary" :loading="trafficServiceSaving" @click="saveTrafficService" />
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="supplierDetailDialogRow" class="modal-backdrop" role="presentation" @click="supplierDetailDialogRow = null">
          <section class="purchase-order-confirm-dialog supplier-detail-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("purchaseOrder.section.supplierConfirm") }}</strong>
                <span>{{ displayOrderText(supplierDetailDialogRow.supplierName) }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="supplierDetailDialogRow = null" />
            </header>
            <div class="purchase-order-dialog-body">
              <article class="supplier-detail-summary">
                <div>
                  <span>{{ t("purchaseOrder.field.packagingMethod") }}</span>
                  <strong>{{ packagingMethodLabel(String(supplierDetailDialogRow.packagingMethod || "")) }}</strong>
                </div>
                <div>
                  <span>{{ t("field.status") }}</span>
                  <StatusBadge :label="purchaseStatusLabel(String(supplierDetailDialogRow.status || ''))" :variant="purchaseStatusVariant(String(supplierDetailDialogRow.status || ''))" />
                </div>
                <div>
                  <span>{{ t("purchaseOrder.field.expectedReadyAt") }}</span>
                  <strong>{{ supplierDetailDialogRow.expectedReadyAt || "-" }}</strong>
                </div>
                <div>
                  <span>{{ t("purchaseOrder.field.finalAmount") }}</span>
                  <strong>{{ formatPurchaseMoney(Number(supplierDetailDialogRow.finalAmount ?? supplierDetailDialogRow.subtotalAmount), String(supplierDetailDialogRow.currency || "CNY")) }}</strong>
                </div>
              </article>
              <DataTable :columns="purchaseItemColumns" :rows="supplierDetailDialogItems" row-key="itemId" :empty-label="t('common.empty')">
                <template #cell-productName="{ value }">
                  <span class="purchase-item-name" :title="String(value || '-')">{{ value || "-" }}</span>
                </template>
                <template #cell-unitPrice="{ row }">{{ formatPurchaseMoneyValue(Number(row.unitPrice)) }}</template>
                <template #cell-amount="{ row }">{{ formatPurchaseMoneyValue(Number(row.amount)) }}</template>
                <template #cell-flags="{ row }">
                  <span>{{ row.quantityFallbackFlag ? t("purchaseOrder.flag.quantityFallback") : "-" }}</span>
                </template>
              </DataTable>
            </div>
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
                  <StableDateTimeInput v-model="supplierConfirmForm.expectedReadyAt" mode="datetime" />
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
        <div v-if="replacementSku" class="modal-backdrop" role="presentation" @click="closeReplacementDialog">
          <section class="sku-replacement-dialog" role="dialog" aria-modal="true" @click.stop>
            <header>
              <div>
                <strong>{{ t("compare.replaceDialogTitle") }}</strong>
                <span>{{ t("compare.replaceDialogSubtitle") }}</span>
              </div>
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="closeReplacementDialog" />
            </header>
            <div class="sku-replacement-body">
              <div class="sku-replacement-impa">
                <span>{{ t("compare.sourceSkuName") }}</span>
                <strong>{{ replacementSku.sourceSkuName }}</strong>
              </div>
              <label class="list-search-field sku-replacement-search">
                <span>{{ t("compare.replacementSearch") }}</span>
                <span class="list-search-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24">
                    <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
                  </svg>
                </span>
                <input v-model="replacementSearchKeyword" type="search" :placeholder="t('compare.replacementSearchPlaceholder')" />
              </label>
              <div v-if="replacementLoading" class="sku-replacement-state">
                <strong>{{ t("common.loading") }}</strong>
              </div>
              <div v-else-if="replacementError" class="sku-replacement-state">
                <strong>{{ t("compare.skuCandidateLoadFailed") }}</strong>
                <p>{{ replacementError }}</p>
              </div>
              <div v-else-if="filteredReplacementCandidates.length" class="sku-replacement-table">
                <div class="sku-replacement-head">
                  <span>{{ t("table.index") }}</span>
                  <span>{{ t("compare.replacementCandidateName") }}</span>
                  <span>{{ t("compare.replacementSpecification") }}</span>
                  <span>{{ t("compare.replacementSupplier") }}</span>
                  <span>{{ t("compare.replacementUnitPrice") }}</span>
                </div>
                <button
                  v-for="(candidate, index) in filteredReplacementCandidates"
                  :key="`${candidate.skuId}-${candidate.supplierName}-${candidate.supplierSkuCode}`"
                  type="button"
                  class="sku-replacement-row"
                  @click="applyReplacementCandidate(candidate)"
                >
                  <strong>{{ index + 1 }}</strong>
                  <span>{{ candidate.productName || candidate.supplierSkuCode || "-" }}</span>
                  <small>{{ replacementCandidateSpecText(candidate) }}</small>
                  <small>{{ candidate.supplierName || "-" }}</small>
                  <b v-if="candidate.unitPrice != null" class="money-stack money-stack--single">
                    <span>{{ formatCompareMoney(candidate.unitPrice, candidate.unitPriceUsd) }}</span>
                  </b>
                  <b v-else>-</b>
                </button>
              </div>
              <div v-else-if="replacementCandidates.length" class="sku-replacement-state">
                <strong>{{ t("common.empty") }}</strong>
                <p>{{ t("compare.replacementNoFiltered") }}</p>
              </div>
              <div v-else class="sku-replacement-state">
                <strong>{{ t("compare.noCandidates") }}</strong>
                <p>{{ t("compare.noReplacementCandidates") }}</p>
              </div>
            </div>
            <footer>
              <button class="ghost-button" type="button" @click="closeReplacementDialog">{{ t("common.close") }}</button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <div v-if="fulfillmentDrawerOpen" class="fulfillment-drawer-backdrop" role="presentation" @click="closeManagedShuttleDrawer">
        <section
          v-if="managedShuttleNodeContext"
          :class="['traffic-shuttle-node-popover managed-shuttle-node-drawer', { 'is-readonly': managedShuttleReadonly }]"
          role="dialog"
          aria-modal="true"
          @click.stop
        >
          <section class="traffic-shuttle-service-summary">
            <div><span>交通艇</span><strong>{{ managedShuttleNodeContext.row.trafficVesselName || '-' }}</strong></div>
            <div><span>日期</span><strong>{{ trafficShuttleServiceDateLabel(managedShuttleNodeContext.row) || '-' }}</strong></div>
            <div><span>时间节点</span><strong>{{ trafficShuttleNodeTimeLabel(trafficShuttleNodeRows(managedShuttleNodeContext.row)[managedShuttleNodeContext.index]) }}</strong></div>
            <div><span>起始到终点</span><strong>{{ trafficShuttleRouteLabel(managedShuttleNodeContext.row) }}</strong></div>
            <div><span>单船价格</span><strong>{{ trafficShuttleBasePriceLabel(managedShuttleNodeContext.row) }}</strong></div>
            <div><span>拼船价格</span><strong>{{ trafficShuttleSharedPriceLabel(managedShuttleNodeContext.row) }}</strong></div>
            <div><span>报关费</span><strong>{{ trafficShuttleMoneyLabel(Number(managedShuttleNodeContext.row.customsPrice || 0)) }}</strong></div>
            <div><span>吊机价格（每吊）</span><strong>{{ trafficShuttleMoneyLabel(Number(managedShuttleNodeContext.row.cranePrice || 0)) }}</strong></div>
          </section>

          <section class="traffic-shuttle-node-popover__info">
            <div v-if="managedShuttleNodeBookings.length" class="traffic-shuttle-vessel-tabs">
              <button
                v-for="(booking, bookingIndex) in managedShuttleNodeBookings"
                :key="booking.bookingId"
                type="button"
                :class="{ active: Number(managedShuttleActiveBooking?.bookingId) === Number(booking.bookingId) }"
                @click="selectManagedShuttleBooking(booking)"
              >
                预约船舶{{ bookingIndex + 1 }}
              </button>
            </div>
            <article v-if="managedShuttleActiveBooking" class="traffic-shuttle-vessel-card traffic-shuttle-vessel-card--tabbed">
              <dl>
                <div><dt>预约船舶</dt><dd>{{ managedShuttleActiveBooking.vesselName || '-' }} / {{ managedShuttleActiveBooking.vesselImo || '-' }}</dd></div>
                <div><dt>货物重量（KG）</dt><dd>{{ managedShuttleActiveBooking.cargoWeightKg ?? '-' }}</dd></div>
                <div><dt>货物体积（平方米）</dt><dd>{{ managedShuttleActiveBooking.cargoVolumeCbm ?? '-' }}</dd></div>
                <div><dt>托盘数量</dt><dd>{{ managedShuttleActiveBooking.palletCount || '-' }}</dd></div>
                <div><dt>抛锚时间</dt><dd>{{ managedShuttleActiveBooking.anchorageTime || '-' }}</dd></div>
                <div><dt>抛锚经纬度</dt><dd>{{ managedShuttleActiveBooking.anchoragePosition || '-' }}</dd></div>
                <div><dt>运费</dt><dd>{{ trafficShuttleMoneyLabel(Number(managedShuttleActiveBooking.freightFee || managedShuttleActiveBooking.amount || 0)) }}</dd></div>
                <div><dt>报关费</dt><dd>{{ trafficShuttleMoneyLabel(Number(managedShuttleActiveBooking.customsFee || 0)) }}</dd></div>
                <div><dt>吊机费</dt><dd>{{ trafficShuttleMoneyLabel(Number(managedShuttleActiveBooking.craneFee || 0)) }}</dd></div>
              </dl>
            </article>
            <div v-else class="empty-state compact">该节点暂无预约船舶</div>
          </section>

          <section class="managed-shuttle-attachment-panel">
            <header>
              <div>
                <strong>服务附件</strong>
                <span>{{ managedShuttleActiveAttachments.length }} 个文件</span>
              </div>
              <label v-if="!managedShuttleReadonly && String(managedShuttleNodeContext.row.status).toUpperCase() === 'IN_PROGRESS'" class="fulfillment-upload-icon" title="上传服务附件" aria-label="上传服务附件">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 15V4m0 0 4 4m-4-4-4 4M4 15v4h16v-4" /></svg>
                <input type="file" multiple accept="image/*,.pdf,.doc,.docx,.xls,.xlsx" :disabled="fulfillmentUploading" @change="uploadManagedShuttleNodeAttachment" />
              </label>
            </header>
            <div class="fulfillment-attachment-gallery">
              <div v-for="item in managedShuttleActiveAttachments" :key="item.attachmentId" class="fulfillment-attachment-item">
                <button type="button" @click="previewBusinessAttachments('服务附件', [item])">
                  <span class="fulfillment-file-icon">▧</span>
                  <strong>{{ item.fileName }}</strong>
                </button>
                <IconButton v-if="!managedShuttleReadonly && String(managedShuttleNodeContext.row.status).toUpperCase() === 'IN_PROGRESS'" icon="Trash2" label="删除附件" variant="danger" @click.stop="deleteManagedShuttleNodeAttachment(item)" />
              </div>
              <div v-if="!managedShuttleActiveAttachments.length" class="empty-state compact">暂无服务附件</div>
            </div>
          </section>

          <section class="traffic-shuttle-node-settings managed-shuttle-execution-settings">
            <template v-if="managedShuttleActiveBooking">
              <label><span>货物重量（KG）</span><input v-model="managedShuttleExecutionDraft(managedShuttleActiveBooking).cargoWeight" placeholder="--" :disabled="managedShuttleReadonly || String(managedShuttleNodeContext.row.status).toUpperCase() !== 'IN_PROGRESS'" /></label>
              <label><span>货物体积（平方米）</span><input v-model="managedShuttleExecutionDraft(managedShuttleActiveBooking).cargoVolume" placeholder="--" :disabled="managedShuttleReadonly || String(managedShuttleNodeContext.row.status).toUpperCase() !== 'IN_PROGRESS'" /></label>
              <label><span>托盘数量</span><input v-model="managedShuttleExecutionDraft(managedShuttleActiveBooking).palletCount" placeholder="--" :disabled="managedShuttleReadonly || String(managedShuttleNodeContext.row.status).toUpperCase() !== 'IN_PROGRESS'" /></label>
              <label><span>抛锚经度</span><input v-model="managedShuttleExecutionDraft(managedShuttleActiveBooking).anchorageLongitude" placeholder="--" :disabled="managedShuttleReadonly || String(managedShuttleNodeContext.row.status).toUpperCase() !== 'IN_PROGRESS'" /></label>
              <label><span>抛锚纬度</span><input v-model="managedShuttleExecutionDraft(managedShuttleActiveBooking).anchorageLatitude" placeholder="--" :disabled="managedShuttleReadonly || String(managedShuttleNodeContext.row.status).toUpperCase() !== 'IN_PROGRESS'" /></label>
              <label><span>吊机次数</span><input v-model.number="managedShuttleExecutionDraft(managedShuttleActiveBooking).craneCount" type="number" min="1" step="1" :disabled="managedShuttleReadonly || String(managedShuttleNodeContext.row.status).toUpperCase() !== 'IN_PROGRESS'" /></label>
            </template>
            <div class="traffic-shuttle-node-popover__footer">
              <IconButton icon="X" label="关闭" @click="closeManagedShuttleDrawer" />
              <IconButton
                v-if="!managedShuttleReadonly && managedShuttleActiveBooking && String(managedShuttleNodeContext.row.status).toUpperCase() === 'IN_PROGRESS'"
                icon="Save"
                label="保存履约信息"
                variant="primary"
                :loading="managedShuttleExecutionSaving"
                @click="saveManagedShuttleExecution"
              />
            </div>
          </section>
        </section>

        <section v-else class="fulfillment-bottom-drawer" role="dialog" aria-modal="true" @click.stop>
          <header><div><strong>{{ fulfillmentDrawerTitle || '运输附件' }}</strong><span>履约附件证明</span></div><IconButton icon="X" label="关闭" @click="closeManagedShuttleDrawer" /></header>
          <section class="fulfillment-attachment-gallery">
            <button v-for="item in fulfillmentDrawerItems" :key="item.attachmentId" type="button" @click="previewBusinessAttachments(item.nodeName || '履约附件', [item])">
              <span class="fulfillment-file-icon">▧</span><strong>{{ item.fileName }}</strong><small>{{ item.nodeName || item.providerName || '运输证明' }}</small>
            </button>
            <div v-if="!fulfillmentDrawerItems.length" class="empty-state compact">暂无运输附件</div>
          </section>
        </section>
      </div>
    </Teleport>

    <DetailDrawer :open="Boolean(settlementEditingRow)" title="编辑结算单" width="wide" @close="settlementEditingId = 0">
      <section v-if="settlementEditingRow" class="settlement-editor">
        <header>
          <div><span>结算单号</span><strong>{{ settlementEditingRow.settlementNo }}</strong></div>
          <StatusBadge :label="settlementStatusLabel(settlementEditingRow.status)" :variant="settlementStatusVariant(settlementEditingRow.status)" />
        </header>
        <dl>
          <div><dt>采购单号</dt><dd>{{ settlementEditingRow.purchaseOrderNo || '-' }}</dd></div>
          <div><dt>船代公司</dt><dd>{{ settlementEditingRow.buyerCompanyName || '-' }}</dd></div>
          <div><dt>船舶名称</dt><dd>{{ settlementEditingRow.vesselName || '-' }}</dd></div>
          <div><dt>报价金额</dt><dd>{{ formatPurchaseMoneyValue(settlementEditingRow.quotedAmount) }}</dd></div>
        </dl>
        <label class="settlement-editor__amount">
          <span>实际金额</span>
          <input v-model="settlementInvoiceDraft(settlementEditingRow).actualAmount" type="number" min="0" step="0.01" placeholder="填写实际发生金额" />
        </label>
        <section class="settlement-editor__invoice">
          <header><strong>发票附件</strong><span>{{ settlementInvoiceDraft(settlementEditingRow).attachments.length }} 个文件</span></header>
          <div class="supplier-customs-file-list">
            <button v-for="file in settlementInvoiceDraft(settlementEditingRow).attachments" :key="file.fileId || file.fileName" type="button" @click="previewBusinessAttachments('发票附件', [file])">{{ file.fileName }}</button>
          </div>
          <label class="supplier-customs-upload">
            <span>{{ settlementInvoiceUploadingId === settlementEditingRow.settlementId ? t('common.loading') : '上传发票图片或文件' }}</span>
            <input type="file" multiple accept="image/*,.pdf,.doc,.docx,.xls,.xlsx" :disabled="settlementInvoiceUploadingId === settlementEditingRow.settlementId" @change="uploadSettlementInvoiceFile(settlementEditingRow, $event)" />
          </label>
        </section>
      </section>
      <template #footer>
        <IconButton icon="X" label="关闭" @click="settlementEditingId = 0" />
        <IconButton
          v-if="settlementEditingRow"
          icon="Send"
          label="提交发票"
          variant="primary"
          :disabled="!settlementInvoiceDraft(settlementEditingRow).actualAmount || !settlementInvoiceDraft(settlementEditingRow).attachments.length"
          :loading="settlementSaving"
          @click="submitSettlementInvoiceRow(settlementEditingRow)"
        />
      </template>
    </DetailDrawer>

    <DetailDrawer :open="Boolean(evaluationEditing)" :title="isRegulatoryEvaluationPage ? '评价审查' : '服务评价'" width="compact" @close="evaluationEditing = null">
      <section v-if="evaluationEditing" class="evaluation-editor">
        <header><strong>{{ evaluationEditing.providerName }}</strong><span>{{ evaluationEditing.purchaseOrderNo }} · {{ evaluationEditing.serviceType }}</span></header>
        <div class="evaluation-editor-ratings">
          <label>
            <span>品质</span>
            <div class="evaluation-star-picker" role="radiogroup" aria-label="品质评分">
              <button v-for="star in 5" :key="star" type="button" :class="{ active: star <= evaluationForm.rating }" :disabled="isEvaluationReadonly" :aria-label="`品质${star}分`" @click="evaluationForm.rating = star">★</button>
            </div>
          </label>
          <label>
            <span>物流</span>
            <div class="evaluation-star-picker" role="radiogroup" aria-label="物流评分">
              <button v-for="point in 5" :key="point" type="button" :class="{ active: point <= evaluationForm.logisticsRating }" :disabled="isEvaluationReadonly" :aria-label="`物流${point}分`" @click="evaluationForm.logisticsRating = point">★</button>
            </div>
          </label>
        </div>
        <label><span>评价内容</span><textarea v-model="evaluationForm.content" rows="4" :readonly="isEvaluationReadonly" placeholder="填写本次服务的真实评价"></textarea></label>
        <section class="evaluation-proof-section">
          <div class="evaluation-proof-heading">
            <span>附件上传</span>
            <label v-if="!isEvaluationReadonly" class="evaluation-proof-upload" title="上传评价证明" aria-label="上传评价证明">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 15V4m0 0 4 4m-4-4-4 4M4 15v4h16v-4" /></svg>
              <input type="file" accept="image/*,.pdf" :disabled="evaluationUploading" @change="uploadEvaluationFile" />
            </label>
          </div>
          <div class="evaluation-proof-row">
            <button v-for="file in evaluationForm.attachments" :key="file.fileId || file.fileName" type="button" class="evaluation-proof-file" @click="previewBusinessAttachments('评价证明', [file])">
              {{ file.fileName }}
            </button>
            <span v-if="isEvaluationReadonly && !evaluationForm.attachments.length" class="evaluation-proof-empty">暂无附件</span>
          </div>
        </section>
        <label v-if="isRegulatoryEvaluationPage"><span>审查意见</span><textarea v-model="evaluationForm.reviewRemark" rows="3" placeholder="填写审查意见"></textarea></label>
      </section>
      <template #footer>
        <IconButton icon="X" label="关闭" @click="evaluationEditing = null" />
        <IconButton v-if="!isEvaluationReadonly" icon="Send" label="提交评价" variant="primary" :loading="evaluationSaving" @click="submitEvaluation" />
        <IconButton v-if="isRegulatoryEvaluationPage && evaluationEditing?.status === 'PENDING_REVIEW'" icon="Ban" label="驳回" variant="danger" :loading="evaluationSaving" @click="reviewEvaluation('reject')" />
        <IconButton v-if="isRegulatoryEvaluationPage && evaluationEditing?.status === 'PENDING_REVIEW'" icon="Check" label="通过" variant="primary" :loading="evaluationSaving" @click="reviewEvaluation('approve')" />
      </template>
    </DetailDrawer>

    <ImagePreviewModal :open="previewOpen" :title="previewTitle" :images="previewImages" :attributes="previewAttributes" @close="previewOpen = false" />

    <ConfirmDialog
      :open="confirmOpen"
      :title="t('common.confirm')"
      :message="t('compare.priceFrozen')"
      @close="confirmOpen = false"
      @confirm="confirmOpen = false"
    />

    <ConfirmDialog
      :open="discardDialogOpen"
      :title="t('action.discard')"
      :message="discardError || t('common.discardCascadeConfirm')"
      :confirm-label="discardSaving ? t('common.processing') : t('action.discard')"
      danger
      @close="closeDiscardDialog"
      @confirm="confirmDiscardDocument"
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
