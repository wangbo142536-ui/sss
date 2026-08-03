<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import DataTable from "@/components/DataTable.vue";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import IconButton from "@/components/IconButton.vue";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { listCompanyContacts } from "@/services/companyService";
import type { TableColumn } from "@/types/workbench";
import FoodTrafficShuttleSelector, { type FoodTrafficShuttleSelection } from "../components/FoodTrafficShuttleSelector.vue";
import { roundFoodQuoteMoney } from "../domain/comparisonPricing";
import {
  createFoodOrder,
  exportFoodComparison,
  getFoodComparison,
  importFoodComparison,
  listFoodDemands,
  saveFoodComparisonItems,
  saveFoodComparisonSettings
} from "../services/foodProcurementApi";
import type { FoodComparison, FoodComparisonOption, FoodComparisonSettings, FoodDemandSummary } from "../types";

type StrategyKey = "LOWEST_ITEM" | "SINGLE_SUPPLIER";
type SupplierSummary = {
  supplierCompanyId: number;
  supplierName: string;
  coveredItemCount: number;
  totalAmount: number;
  enabled: boolean;
};
type ComparisonRow = Record<string, unknown> & {
  id: string;
  quoteItemId?: number;
  demandItemId: number;
  sequenceNo: number;
  name: string;
  nameEn: string;
  specification: string;
  unit: string;
  requestedQuantity: number;
  supplierCompanyId?: number;
  supplierName: string;
  quotedQuantity: number;
  unitPrice: number;
  subtotal: number;
  costSubtotal: number;
  quantitySatisfied: boolean;
  priceSource: string;
};

type FoodOrderTrafficService = {
  shuttleId?: number;
  shuttleNo?: string;
  trafficVesselName?: string;
  departurePoint?: string;
  destinationPoint?: string;
  anchorageName?: string;
  startTime?: string;
  returnTime?: string;
  selectedNodeIndex?: number;
  serviceNodes?: Array<{ nodeName?: string; startTime?: string; endTime?: string }>;
  selectedNode?: { nodeName?: string; startTime?: string; endTime?: string };
};

type FoodOrderContact = {
  id: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
};

const route = useRoute();
const router = useRouter();
const keyword = ref("");
const status = ref("");
const dateFrom = ref("");
const dateTo = ref("");
const skuKeyword = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const demands = ref<FoodDemandSummary[]>([]);
const comparison = ref<FoodComparison>();
const strategy = ref<StrategyKey>("LOWEST_ITEM");
const selectedSupplierCompanyId = ref<number>();
const expandedRowId = ref("");
const settings = ref<FoodComparisonSettings>({
  markupPercent: 10,
  fixedFreightFee: 0,
  fixedCustomsFee: 0,
  fixedCraneFee: 0,
  fixedOtherFee: 0,
  supplyMode: "SEA",
  fixedProviderType: "BARGE",
  fixedProviderId: "",
  fixedProviderName: "",
  trafficServiceJson: ""
});
const settingsSaving = ref(false);
const comparisonExporting = ref(false);
const comparisonImporting = ref(false);
const dirtyQuoteItemIds = ref<Set<number>>(new Set());
const comparisonImportInput = ref<HTMLInputElement>();
const comparisonWorkspaceRef = ref<HTMLElement>();
const showBackTop = ref(false);
const providerDialogOpen = ref(false);
const providerLoading = ref(false);
const selectedDemandItemIds = ref<number[]>([]);
const orderDialogOpen = ref(false);
const orderCreating = ref(false);
const orderContacts = ref<FoodOrderContact[]>([]);
const orderContactId = ref("");
const orderContactError = ref("");
const orderDraft = ref<{
  strategy: StrategyKey;
  selectedSupplierCompanyId?: number;
  selectedItems: Array<{ demandItemId: number; quoteItemId: number }>;
  allowPartial: boolean;
}>();
const orderForm = ref({
  requiredDeliveryTime: "",
  deliveryAddress: "",
  deliveryContactName: "",
  deliveryContactPhone: "",
  deliveryContactEmail: "",
  defaultPackagingMethod: "UNIFIED_PACKAGING",
  buyerRemark: ""
});
const orderFormErrors = ref({ requiredDeliveryTime: "", deliveryAddress: "", deliveryContactName: "", deliveryContactPhone: "" });

const comparisonColumns: TableColumn[] = [
  { key: "selection", label: "序号", width: "72px", align: "center" },
  { key: "name", label: "伙食名称", width: "22%" },
  { key: "specification", label: "规格", width: "22%" },
  { key: "requestedQuantity", label: "需求数量", width: "12%", align: "right" },
  { key: "quotedQuantity", label: "报价数量", width: "12%", align: "right" },
  { key: "unitPrice", label: "单价", width: "12%", align: "center" },
  { key: "subtotal", label: "小计", width: "14%", align: "center" }
];

const visibleDemands = computed(() => demands.value.filter((row) => {
  const rowDate = String(row.updatedAt || "").slice(0, 10);
  return (!dateFrom.value || rowDate >= dateFrom.value) && (!dateTo.value || rowDate <= dateTo.value);
}));
const selectedDemandItemIdSet = computed(() => new Set(selectedDemandItemIds.value));

const supplierSummaries = computed<SupplierSummary[]>(() => {
  if (!comparison.value) return [];
  const grouped = new Map<number, { name: string; options: Map<number, FoodComparisonOption> }>();
  comparison.value.items.forEach((item) => item.quotes.forEach((option) => {
    const entry = grouped.get(option.supplierCompanyId) || { name: option.supplierName, options: new Map() };
    const current = entry.options.get(item.demandItemId);
    if (!current || option.unitPrice < current.unitPrice) entry.options.set(item.demandItemId, option);
    grouped.set(option.supplierCompanyId, entry);
  }));
  return Array.from(grouped.entries()).map(([supplierCompanyId, entry]) => {
    const allValid = Array.from(entry.options.values()).filter((option) => option.quantitySatisfied);
    const selectedValid = allValid.filter((option) => selectedDemandItemIdSet.value.has(option.demandItemId));
    return {
      supplierCompanyId,
      supplierName: entry.name,
      coveredItemCount: selectedValid.length,
      totalAmount: selectedValid.reduce((sum, option) => sum + option.requestedQuantity * option.unitPrice, 0),
      enabled: allValid.length === comparison.value!.items.length
    };
  }).sort((left, right) => left.totalAmount - right.totalAmount || left.supplierName.localeCompare(right.supplierName)).slice(0, 3);
});

const singleSupplierStrategy = computed(() => comparison.value?.strategies.find((item) => item.strategyType === "SINGLE_SUPPLIER"));
const selectedSingleSupplier = computed(() => {
  const supplierCompanyId = singleSupplierStrategy.value?.supplierCompanyId;
  return supplierSummaries.value.find((item) => item.supplierCompanyId === supplierCompanyId)
    || supplierSummaries.value.find((item) => item.enabled);
});
const concentratedSupplierCards = computed(() => selectedSingleSupplier.value ? [selectedSingleSupplier.value] : []);
const concentratedStrategyEnabled = computed(() => singleSupplierStrategy.value?.enabled !== false && Boolean(selectedSingleSupplier.value?.enabled));

const markupRate = computed(() => Math.max(0, Number(settings.value.markupPercent || 0)) / 100);

function optionForItem(item: FoodComparison["items"][number]): FoodComparisonOption | undefined {
  if (strategy.value === "SINGLE_SUPPLIER") {
    return item.quotes
      .filter((option) => option.supplierCompanyId === selectedSingleSupplier.value?.supplierCompanyId)
      .sort((left, right) => left.unitPrice - right.unitPrice)[0];
  }
  const ranked = item.quotes
      .filter((option) => option.lowestPrice)
      .sort((left, right) => left.unitPrice - right.unitPrice)[0];
  return ranked || item.quotes.slice().sort((left, right) => left.unitPrice - right.unitPrice)[0];
}

const comparisonRows = computed<ComparisonRow[]>(() => (comparison.value?.items || []).map((item) => {
  const option = optionForItem(item);
  const unitPrice = Number(option?.unitPrice || 0);
  const requestedQuantity = Number(item.requestedQuantity || 0);
  const quotedQuantity = Number(option?.quotedQuantity || 0);
  return {
    id: String(item.demandItemId),
    quoteItemId: option?.quoteItemId,
    demandItemId: item.demandItemId,
    sequenceNo: item.sequenceNo,
    name: item.nameZh || item.nameEn || "-",
    nameEn: item.nameZh ? item.nameEn || "" : "",
    specification: item.specification || "-",
    unit: item.unit || "-",
    requestedQuantity,
    supplierCompanyId: option?.supplierCompanyId,
    supplierName: option?.supplierName || "-",
    quotedQuantity,
    unitPrice,
    subtotal: option ? roundFoodQuoteMoney(requestedQuantity * unitPrice) : 0,
    costSubtotal: option ? roundFoodQuoteMoney(requestedQuantity * unitPrice) : 0,
    quantitySatisfied: Boolean(option && quotedQuantity >= requestedQuantity),
    priceSource: option?.priceSource || "-"
  };
}).filter((row) => selectedSupplierCompanyId.value == null || row.supplierCompanyId === selectedSupplierCompanyId.value));

const filteredRows = computed(() => {
  const query = skuKeyword.value.trim().toLowerCase();
  return query
    ? comparisonRows.value.filter((row) => `${row.name} ${row.nameEn} ${row.specification} ${row.unit}`.toLowerCase().includes(query))
    : comparisonRows.value;
});

const lowestSupplierTotals = computed(() => {
  const totals = new Map<number, { supplierName: string; amount: number }>();
  comparison.value?.items.forEach((item) => {
    const option = item.quotes.filter((row) => row.lowestPrice && row.quantitySatisfied).sort((a, b) => a.unitPrice - b.unitPrice)[0];
    if (!option) return;
    const current = totals.get(option.supplierCompanyId) || { supplierName: option.supplierName, amount: 0 };
    if (selectedDemandItemIdSet.value.has(item.demandItemId)) {
      current.amount += option.requestedQuantity * option.unitPrice;
    }
    totals.set(option.supplierCompanyId, current);
  });
  return Array.from(totals.entries()).map(([supplierCompanyId, value]) => ({ supplierCompanyId, ...value }));
});

const lowestCoveredCount = computed(() => comparison.value?.items.filter((item) =>
  selectedDemandItemIdSet.value.has(item.demandItemId)
    && item.quotes.some((option) => option.lowestPrice && option.quantitySatisfied)
).length || 0);
const lowestTotal = computed(() => lowestSupplierTotals.value.reduce((sum, supplier) => sum + supplier.amount, 0));
const fixedFeeTotal = computed(() => Number(settings.value.fixedFreightFee || 0)
  + Number(settings.value.fixedCustomsFee || 0)
  + Number(settings.value.fixedCraneFee || 0)
  + Number(settings.value.fixedOtherFee || 0));
const quoteTotal = (cost: number) => roundFoodQuoteMoney(cost * (1 + markupRate.value));
const profitTotal = (cost: number) => quoteTotal(cost) - cost - fixedFeeTotal.value;
const selectedCost = computed(() => strategy.value === "LOWEST_ITEM" ? lowestTotal.value : Number(selectedSingleSupplier.value?.totalAmount || 0));
const providerLabel = computed(() => settings.value.supplyMode === "SEA" ? "驳船" : "供货商");
const selectedRows = computed(() => comparisonRows.value.filter(isComparisonRowSelected));
const coveredCount = computed(() => selectedRows.value.filter((row) => row.quantitySatisfied).length);
const selectedItemCount = computed(() => selectedRows.value.length);
const totalItemCount = computed(() => comparison.value?.items.length || 0);
const comparisonLocked = computed(() => comparison.value?.demand.status === "ORDERED");
const canCreateOrder = computed(() => comparison.value?.demand.status !== "ORDERED"
  && selectedItemCount.value > 0 && coveredCount.value === selectedItemCount.value
  && (strategy.value !== "SINGLE_SUPPLIER" || Boolean(selectedSingleSupplier.value?.enabled)));
const orderStrategyLabel = computed(() => orderDraft.value?.strategy === "SINGLE_SUPPLIER"
  ? `集中采购 · ${selectedSingleSupplier.value?.supplierName || "-"}`
  : "最低混供");
const orderSupplierSummaries = computed(() => {
  const grouped = new Map<number, { supplierName: string; count: number; amount: number }>();
  selectedRows.value.forEach((row) => {
    if (!row.supplierCompanyId) return;
    const current = grouped.get(row.supplierCompanyId) || { supplierName: row.supplierName, count: 0, amount: 0 };
    current.count += 1;
    current.amount += row.costSubtotal;
    grouped.set(row.supplierCompanyId, current);
  });
  return Array.from(grouped.entries()).map(([supplierCompanyId, value]) => ({ supplierCompanyId, ...value }));
});
const orderTrafficService = computed<FoodOrderTrafficService>(() => {
  if (!settings.value.trafficServiceJson) return {};
  try {
    return JSON.parse(settings.value.trafficServiceJson) as FoodOrderTrafficService;
  } catch {
    return {};
  }
});
const orderBargeDateLabel = computed(() => String(orderTrafficService.value.startTime || comparison.value?.demand.vesselEta || "").slice(0, 10) || "-");
const orderBargeDeparturePointLabel = computed(() => orderTrafficService.value.departurePoint || "-");
const orderBargeDestinationPointLabel = computed(() => orderTrafficService.value.destinationPoint || orderTrafficService.value.anchorageName || comparison.value?.demand.supplyPort || "-");
const orderBargeOriginTimeLabel = computed(() => formatTrafficTime(orderTrafficService.value.startTime));
const orderBargeDestinationTimeLabel = computed(() => formatTrafficTime(orderTrafficService.value.returnTime || orderTrafficService.value.selectedNode?.endTime));
const orderBargeNodeRows = computed(() => {
  const nodes = orderTrafficService.value.serviceNodes || [];
  return nodes.length ? nodes : orderTrafficService.value.selectedNode ? [orderTrafficService.value.selectedNode] : [];
});
const orderFixedFeeItems = computed(() => [
  { key: "freight", label: "运费", value: Number(settings.value.fixedFreightFee || 0) },
  { key: "customs", label: "报关费", value: Number(settings.value.fixedCustomsFee || 0) },
  { key: "crane", label: "吊机费", value: Number(settings.value.fixedCraneFee || 0) }
]);

function formatTrafficTime(value?: string) {
  const normalized = String(value || "").trim();
  if (!normalized) return "-";
  const match = normalized.match(/(\d{2}:\d{2})/);
  return match?.[1] || normalized;
}

function formatTrafficNodeTime(node: { startTime?: string; endTime?: string }) {
  const start = formatTrafficTime(node.startTime);
  const end = formatTrafficTime(node.endTime);
  if (start === "-" && end === "-") return "-";
  if (start === end || end === "-") return start;
  if (start === "-") return end;
  return `${start}-${end}`;
}

function isComparisonRowSelectable(row: ComparisonRow) {
  return typeof row.quoteItemId === "number" && row.quoteItemId > 0;
}

function isComparisonRowSelected(row: ComparisonRow) {
  return isComparisonRowSelectable(row) && selectedDemandItemIds.value.includes(row.demandItemId);
}

function comparisonRowClass(row: ComparisonRow) {
  return { "is-compare-deselected": isComparisonRowSelectable(row) && !isComparisonRowSelected(row) };
}

function toggleComparisonRow(row: ComparisonRow, selected: boolean) {
  if (!isComparisonRowSelectable(row)) return;
  const next = new Set(selectedDemandItemIds.value);
  if (selected) next.add(row.demandItemId); else next.delete(row.demandItemId);
  selectedDemandItemIds.value = Array.from(next);
}

function syncComparisonSelectedRows() {
  selectedDemandItemIds.value = comparisonRows.value.filter(isComparisonRowSelectable).map((row) => row.demandItemId);
}

function selectStrategy(value: StrategyKey) {
  if (value === "SINGLE_SUPPLIER" && !concentratedStrategyEnabled.value) return;
  strategy.value = value;
  selectedSupplierCompanyId.value = undefined;
  expandedRowId.value = "";
  syncComparisonSelectedRows();
}

function selectStrategySupplier(strategyKey: StrategyKey, supplierCompanyId: number) {
  if (strategyKey === "SINGLE_SUPPLIER" && !concentratedStrategyEnabled.value) return;
  const shouldClear = strategy.value === strategyKey && selectedSupplierCompanyId.value === supplierCompanyId;
  strategy.value = strategyKey;
  selectedSupplierCompanyId.value = shouldClear ? undefined : supplierCompanyId;
  expandedRowId.value = "";
  syncComparisonSelectedRows();
}

async function loadList() {
  loading.value = true;
  error.value = "";
  try {
    demands.value = await listFoodDemands(keyword.value, status.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "比价需求读取失败";
  } finally {
    loading.value = false;
  }
}

async function loadComparison(id: number) {
  loading.value = true;
  error.value = "";
  try {
    comparison.value = await getFoodComparison(id);
    dirtyQuoteItemIds.value = new Set();
    settings.value = { ...settings.value, ...comparison.value.settings };
    strategy.value = "LOWEST_ITEM";
    selectedSupplierCompanyId.value = undefined;
    const selectableDemandItemIds = comparisonRows.value.filter(isComparisonRowSelectable).map((row) => row.demandItemId);
    const savedDemandItemIds = comparison.value.settings.selectedDemandItemIds;
    selectedDemandItemIds.value = Array.isArray(savedDemandItemIds)
      ? selectableDemandItemIds.filter((demandItemId) => savedDemandItemIds.includes(demandItemId))
      : selectableDemandItemIds;
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "比价结果读取失败";
  } finally {
    loading.value = false;
  }
}

async function persistComparisonSettings() {
  if (!comparison.value || comparisonLocked.value) return;
  settingsSaving.value = true;
  error.value = "";
  try {
    await persistComparisonItems();
    settings.value = await saveFoodComparisonSettings(comparison.value.demand.demandId, {
      ...settings.value,
      selectedDemandItemIds: selectedDemandItemIds.value
    });
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "比价设置保存失败";
    throw reason;
  } finally {
    settingsSaving.value = false;
  }
}

async function persistComparisonItems() {
  if (!comparison.value || comparisonLocked.value) return;
  const currentOptionIds = new Set(comparison.value.items.flatMap((item) => {
      const option = optionForItem(item);
      return option ? [option.quoteItemId] : [];
  }));
  const quoteItemIds = new Set([...currentOptionIds, ...dirtyQuoteItemIds.value]);
  const items = comparison.value.items.flatMap((item) => item.quotes
    .filter((option) => quoteItemIds.has(option.quoteItemId))
    .map((option) => ({
      quoteItemId: option.quoteItemId,
      demandItemId: item.demandItemId,
      requestedQuantity: Number(item.requestedQuantity || 0),
      quotedQuantity: Number(option.quotedQuantity || 0),
      unitPrice: Number(option.unitPrice || 0)
    })));
  if (items.length) await saveFoodComparisonItems(comparison.value.demand.demandId, { items });
  dirtyQuoteItemIds.value = new Set();
}

function refreshComparisonRanking(item: FoodComparison["items"][number]) {
  const requestedQuantity = Number(item.requestedQuantity || 0);
  item.quotes.forEach((candidate) => {
    candidate.requestedQuantity = requestedQuantity;
    candidate.quantitySatisfied = Number(candidate.quotedQuantity || 0) >= requestedQuantity;
    candidate.amount = roundFoodQuoteMoney(requestedQuantity * Number(candidate.unitPrice || 0));
  });
  const eligiblePrices = item.quotes
    .filter((candidate) => candidate.quantitySatisfied)
    .map((candidate) => Number(candidate.unitPrice || 0));
  const lowestPrice = eligiblePrices.length ? Math.min(...eligiblePrices) : undefined;
  item.quotes.forEach((candidate) => {
    candidate.lowestPrice = lowestPrice !== undefined
      && candidate.quantitySatisfied
      && Number(candidate.unitPrice || 0) === lowestPrice;
  });
}

function updateComparisonValue(row: ComparisonRow, field: "requestedQuantity" | "quotedQuantity" | "unitPrice", value: string) {
  if (!comparison.value || comparisonLocked.value) return;
  const numericValue = Math.max(0, Number(value || 0));
  const item = comparison.value.items.find((candidate) => candidate.demandItemId === row.demandItemId);
  const option = item?.quotes.find((candidate) => candidate.quoteItemId === row.quoteItemId);
  if (!item || !option) return;
  if (field === "requestedQuantity") {
    item.requestedQuantity = numericValue;
    dirtyQuoteItemIds.value = new Set([...dirtyQuoteItemIds.value, ...item.quotes.map((candidate) => candidate.quoteItemId)]);
  } else if (field === "quotedQuantity") {
    option.quotedQuantity = numericValue;
    dirtyQuoteItemIds.value = new Set([...dirtyQuoteItemIds.value, option.quoteItemId]);
  } else {
    option.unitPrice = numericValue;
    dirtyQuoteItemIds.value = new Set([...dirtyQuoteItemIds.value, option.quoteItemId]);
  }
  refreshComparisonRanking(item);
}

const currentSelectedQuoteItems = computed(() => selectedRows.value
  .filter((row): row is ComparisonRow & { quoteItemId: number } => typeof row.quoteItemId === "number" && row.quoteItemId > 0)
  .map((row) => ({ demandItemId: row.demandItemId, quoteItemId: row.quoteItemId })));
const currentQuoteItemIds = computed(() => currentSelectedQuoteItems.value
  .map((row) => row.quoteItemId)
  .filter((value): value is number => typeof value === "number" && value > 0));

async function exportComparisonSheet() {
  if (!comparison.value || !currentQuoteItemIds.value.length) return;
  comparisonExporting.value = true;
  error.value = "";
  try {
    if (!comparisonLocked.value) await persistComparisonItems();
    await exportFoodComparison(comparison.value.demand.demandId, currentQuoteItemIds.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "比价报价表导出失败";
  } finally {
    comparisonExporting.value = false;
  }
}

function openComparisonImport() {
  if (comparisonImporting.value || loading.value || comparisonLocked.value) return;
  comparisonImportInput.value?.click();
}

async function handleComparisonImport(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file || !comparison.value) return;
  comparisonImporting.value = true;
  error.value = "";
  notice.value = "";
  try {
    const result = await importFoodComparison(comparison.value.demand.demandId, file);
    await loadComparison(comparison.value.demand.demandId);
    notice.value = `已导入并更新 ${result.updatedCount} 条报价`;
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "导入失败，请使用当前页面导出的 xlsx 文件";
  } finally {
    comparisonImporting.value = false;
  }
}

function handleComparisonScroll() {
  showBackTop.value = Number(comparisonWorkspaceRef.value?.scrollTop || 0) > 120;
}

function scrollComparisonToTop() {
  const target = comparisonWorkspaceRef.value;
  if (!target) return;
  const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
  target.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
}

function changeSupplyMode() {
  if (comparisonLocked.value) return;
  settings.value.fixedProviderType = settings.value.supplyMode === "SEA" ? "BARGE" : "SUPPLIER";
  settings.value.fixedProviderId = "";
  settings.value.fixedProviderName = "";
  settings.value.trafficServiceJson = "";
}

async function openProviderDialog() {
  if (comparisonLocked.value) return;
  providerDialogOpen.value = true;
}

function selectTrafficShuttle(selection: FoodTrafficShuttleSelection) {
  if (comparisonLocked.value) return;
  const item = selection.shuttle;
  settings.value.fixedProviderType = "BARGE";
  settings.value.fixedProviderId = String(item.shuttleId);
  settings.value.fixedProviderName = [item.shuttleNo, item.trafficVesselName].filter(Boolean).join(" / ");
  settings.value.fixedFreightFee = selection.freightFee;
  settings.value.fixedCustomsFee = selection.customsFee;
  settings.value.fixedCraneFee = selection.craneFee;
  settings.value.trafficServiceJson = JSON.stringify({
    ...item,
    selectedNodeIndex: selection.nodeIndex,
    selectedNode: selection.node,
    allowShare: selection.allowShare,
    customsService: selection.customsService,
    craneService: selection.craneService,
    craneCount: selection.craneCount,
    freightFee: selection.freightFee,
    customsFee: selection.customsFee,
    craneFee: selection.craneFee,
    demandId: selection.demandId,
    inquiryNo: selection.inquiryNo,
    vesselName: selection.vesselName,
    cargoWeight: selection.cargoWeight,
    cargoVolume: selection.cargoVolume,
    palletCount: selection.palletCount,
    anchorageTime: selection.anchorageTime,
    anchorageLongitude: selection.anchorageLongitude,
    anchorageLatitude: selection.anchorageLatitude,
    vesselSlots: selection.vesselSlots
  });
  providerDialogOpen.value = false;
}

function selectLandSupplier(item: SupplierSummary) {
  if (comparisonLocked.value) return;
  settings.value.fixedProviderType = "SUPPLIER";
  settings.value.fixedProviderId = String(item.supplierCompanyId);
  settings.value.fixedProviderName = item.supplierName;
  settings.value.trafficServiceJson = "";
  providerDialogOpen.value = false;
}

function readOrderContactItems(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== "object") return [];
  const record = payload as Record<string, unknown>;
  const unwrapped = record.data ?? record.result ?? record;
  if (Array.isArray(unwrapped)) return unwrapped;
  if (!unwrapped || typeof unwrapped !== "object") return [];
  const items = (unwrapped as Record<string, unknown>).items;
  return Array.isArray(items) ? items : [];
}

async function loadOrderContacts() {
  orderContactError.value = "";
  try {
    const payload = await listCompanyContacts({ status: "ACTIVE" });
    orderContacts.value = readOrderContactItems(payload).flatMap((value) => {
      if (!value || typeof value !== "object") return [];
      const record = value as Record<string, unknown>;
      const contactId = record.contactId ?? record.id;
      const contactName = String(record.contactName || "").trim();
      const contactPhone = String(record.contactPhone || "").trim();
      const contactEmail = String(record.contactEmail || "").trim();
      if ((typeof contactId !== "string" && typeof contactId !== "number") || !contactName || !contactPhone || record.status === "DELETED") return [];
      return [{ id: String(contactId), contactName, contactPhone, contactEmail }];
    });
  } catch {
    orderContacts.value = [];
    orderContactError.value = "企业联系人读取失败";
  }
}

function handleOrderContactChange() {
  const contact = orderContacts.value.find((item) => item.id === orderContactId.value);
  orderForm.value.deliveryContactName = contact?.contactName || "";
  orderForm.value.deliveryContactPhone = contact?.contactPhone || "";
  orderForm.value.deliveryContactEmail = contact?.contactEmail || "";
  orderFormErrors.value = { ...orderFormErrors.value, deliveryContactName: "", deliveryContactPhone: "" };
}

async function openOrderDialog() {
  if (!comparison.value || !canCreateOrder.value) return;
  await loadOrderContacts();
  const defaultContact = orderContacts.value[0];
  orderContactId.value = defaultContact?.id || "";
  orderDraft.value = {
    strategy: strategy.value,
    selectedSupplierCompanyId: strategy.value === "SINGLE_SUPPLIER" ? selectedSingleSupplier.value?.supplierCompanyId : undefined,
    selectedItems: currentSelectedQuoteItems.value.map((item) => ({ ...item })),
    allowPartial: selectedItemCount.value < totalItemCount.value
  };
  orderForm.value = {
    requiredDeliveryTime: comparison.value.demand.vesselEta?.slice(0, 16) || "",
    deliveryAddress: comparison.value.demand.supplyPort || "",
    deliveryContactName: defaultContact?.contactName || "",
    deliveryContactPhone: defaultContact?.contactPhone || "",
    deliveryContactEmail: defaultContact?.contactEmail || "",
    defaultPackagingMethod: "UNIFIED_PACKAGING",
    buyerRemark: ""
  };
  orderFormErrors.value = { requiredDeliveryTime: "", deliveryAddress: "", deliveryContactName: "", deliveryContactPhone: "" };
  error.value = "";
  orderDialogOpen.value = true;
}

function validateOrderForm() {
  orderFormErrors.value = {
    requiredDeliveryTime: orderForm.value.requiredDeliveryTime.trim() ? "" : "请选择要求送达时间",
    deliveryAddress: orderForm.value.deliveryAddress.trim() ? "" : "请输入具体交付地址",
    deliveryContactName: orderForm.value.deliveryContactName.trim() ? "" : "请输入联系人",
    deliveryContactPhone: orderForm.value.deliveryContactPhone.trim() ? "" : "请输入联系电话"
  };
  return !Object.values(orderFormErrors.value).some(Boolean);
}

async function createOrder() {
  if (!comparison.value || !orderDraft.value || !validateOrderForm()) return;
  orderCreating.value = true;
  try {
    await persistComparisonSettings();
    const result = await createFoodOrder(
      comparison.value.demand.demandId,
      orderDraft.value.strategy,
      orderDraft.value.selectedSupplierCompanyId,
      orderDraft.value.selectedItems,
      orderDraft.value.allowPartial,
      { ...orderForm.value }
    );
    orderDialogOpen.value = false;
    notice.value = `伙食采购订单 ${result.orderNo} 已生成`;
    await router.push(`/food/orders/${result.orderId}`);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "生成采购订单失败";
  } finally {
    orderCreating.value = false;
  }
}

function reset() {
  keyword.value = "";
  status.value = "";
  dateFrom.value = "";
  dateTo.value = "";
  loadList();
}

function statusLabel(value: string) {
  return ({ INQUIRY_SENT: "询价中", QUOTED: "比价中", ORDERED: "已下单" } as Record<string, string>)[value] || value || "-";
}

watch(() => route.params.demandId, (value) => {
  const id = Number(value);
  comparison.value = undefined;
  showBackTop.value = false;
  if (id > 0) loadComparison(id); else loadList();
}, { immediate: true });

onMounted(() => {
  if (!route.params.demandId) loadList();
});
</script>

<template>
  <section class="food-page food-comparison-page">
    <ExpandablePanel v-if="!comparison" :show-header="false" class="inquiry-management-panel">
      <section class="filter-toolbar" aria-label="伙食比价筛选">
        <div class="filter-fields">
          <label class="filter-field filter-field--search"><span>检索</span><span class="filter-search-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg></span><input v-model="keyword" placeholder="单号、船舶检索" @keydown.enter.prevent="loadList" /></label>
          <label class="filter-field"><span>询价开始</span><StableDateTimeInput v-model="dateFrom" mode="date" /></label>
          <label class="filter-field"><span>询价结束</span><StableDateTimeInput v-model="dateTo" mode="date" /></label>
          <label class="filter-field"><span>状态</span><select v-model="status"><option value="">全部</option><option value="INQUIRY_SENT">询价中</option><option value="QUOTED">比价中</option><option value="ORDERED">已下单</option></select></label>
        </div>
        <div class="toolbar-icon-actions"><IconButton icon="Search" label="查询" @click="loadList" /><IconButton icon="X" label="重置" @click="reset" /><IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="loadList" /></div>
      </section>
      <p v-if="error" class="inline-error">{{ error }}</p>
      <div v-if="loading" class="empty-state compact">加载中</div>
      <section v-else-if="visibleDemands.length" class="procurement-query-card-list">
        <article v-for="row in visibleDemands" :key="row.demandId" class="procurement-query-card is-comparison" tabindex="0" @click="router.push(`/food/comparison/${row.demandId}`)" @keydown.enter="router.push(`/food/comparison/${row.demandId}`)">
          <header class="procurement-query-card__head"><div><strong class="vessel-title"><span class="vessel-mini-icon" aria-hidden="true"></span>{{ row.vesselName || '-' }}</strong></div><StatusBadge :label="statusLabel(row.status)" variant="info" /></header>
          <section class="procurement-query-card__body"><div class="procurement-query-card__primary"><strong>{{ row.inquiryNo || row.demandNo }}</strong></div><div class="procurement-query-card__metrics"><div class="metric-sku"><span>SKU</span><strong>{{ row.itemCount }}</strong></div><div class="metric-exact"><span>供货商</span><strong>{{ row.supplierCount }}</strong></div><div class="metric-similar"><span>已报价</span><strong>{{ row.submittedQuoteCount }}</strong></div><div class="metric-unmatched"><span>异常</span><strong>{{ row.pendingCount }}</strong></div></div></section>
          <footer class="procurement-query-card__footer"><span>询价日期 {{ row.updatedAt?.slice(0, 10) || '-' }}</span><div class="icon-action-row"><IconButton icon="Eye" label="进入比价" :disabled="row.submittedQuoteCount === 0" @click.stop="router.push(`/food/comparison/${row.demandId}`)" /></div></footer>
        </article>
      </section>
      <div v-else class="empty-state compact">暂无可比价的伙食需求</div>
    </ExpandablePanel>

    <section v-else ref="comparisonWorkspaceRef" class="compare-workspace food-comparison-workspace" @scroll="handleComparisonScroll">
      <LoadingOverlay :active="loading" label="加载中">
        <article class="compare-supply-card">
          <div class="compare-supply-title"><strong>主要信息</strong><StatusBadge :label="statusLabel(comparison.demand.status)" variant="info" /></div>
          <dl>
            <div class="is-editable"><dt>船舶名称</dt><dd><input :value="comparison.demand.vesselName" readonly /></dd></div>
            <div class="is-editable"><dt>询价单号</dt><dd><input :value="comparison.demand.inquiryNo || comparison.demand.demandNo" readonly /></dd></div>
            <div class="is-editable"><dt>物资类别</dt><dd><input value="伙食" readonly /></dd></div>
            <div class="is-editable"><dt>补给港口</dt><dd><input :value="comparison.demand.supplyPort" readonly /></dd></div>
            <div class="is-editable"><dt>预计港时间</dt><dd><input :value="comparison.demand.vesselEta?.replace('T', ' ')" readonly /></dd></div>
            <div class="is-editable"><dt>币种</dt><dd><input :value="comparison.demand.currency" readonly /></dd></div>
            <div class="is-editable"><dt>运输方式</dt><dd><select v-model="settings.supplyMode" :disabled="comparisonLocked" @change="changeSupplyMode"><option value="SEA">海运</option><option value="LAND">陆运</option></select></dd></div>
            <div class="is-editable"><dt>报价供货商</dt><dd><input :value="comparison.submittedSupplierCount" readonly /></dd></div>
          </dl>
        </article>

        <article class="strategy-panel compare-detail-panel">
          <div class="compare-bid-area">
            <div class="strategy-choice-group compare-strategy-board" role="group" aria-label="比价策略选择">
              <article :class="['strategy-choice', 'is-green', { active: strategy === 'LOWEST_ITEM' }]">
                <button type="button" class="strategy-choice-main" @click="selectStrategy('LOWEST_ITEM')"><span class="strategy-title-stack"><strong>最低混供</strong></span><div class="strategy-count-stack"><strong>{{ lowestCoveredCount }}/{{ totalItemCount }}</strong></div><div class="strategy-total-stack"><em>成本金额 {{ lowestTotal.toFixed(2) }}</em><em>补给费用 {{ fixedFeeTotal.toFixed(2) }}</em><em>报价总额 {{ quoteTotal(lowestTotal).toFixed(2) }}</em><em>预计利润 {{ profitTotal(lowestTotal).toFixed(2) }}</em></div></button>
                <ul class="strategy-supplier-list"><li v-for="supplier in lowestSupplierTotals" :key="supplier.supplierCompanyId"><button type="button" :class="{ active: strategy === 'LOWEST_ITEM' && selectedSupplierCompanyId === supplier.supplierCompanyId }" @click="selectStrategySupplier('LOWEST_ITEM', supplier.supplierCompanyId)"><span>{{ supplier.supplierName }}</span><span class="strategy-supplier-meta"><strong>{{ supplier.amount.toFixed(2) }}</strong></span></button></li></ul>
              </article>
              <span class="strategy-vs">VS</span>
              <article :class="['strategy-choice', 'is-blue', { active: strategy === 'SINGLE_SUPPLIER', 'is-disabled': !concentratedStrategyEnabled }]">
                <button type="button" class="strategy-choice-main" :disabled="!concentratedStrategyEnabled" @click="selectStrategy('SINGLE_SUPPLIER')"><span class="strategy-title-stack"><strong>集中采购</strong></span><div class="strategy-count-stack"><strong>{{ selectedSingleSupplier?.coveredItemCount || 0 }}/{{ totalItemCount }}</strong></div><div class="strategy-total-stack"><em>成本金额 {{ (selectedSingleSupplier?.totalAmount || 0).toFixed(2) }}</em><em>补给费用 {{ fixedFeeTotal.toFixed(2) }}</em><em>报价总额 {{ quoteTotal(selectedSingleSupplier?.totalAmount || 0).toFixed(2) }}</em><em>预计利润 {{ profitTotal(selectedSingleSupplier?.totalAmount || 0).toFixed(2) }}</em></div></button>
                <ul class="strategy-supplier-list"><li v-for="supplier in concentratedSupplierCards" :key="supplier.supplierCompanyId"><button type="button" :class="{ active: strategy === 'SINGLE_SUPPLIER' && selectedSupplierCompanyId === supplier.supplierCompanyId }" :disabled="!concentratedStrategyEnabled" @click="selectStrategySupplier('SINGLE_SUPPLIER', supplier.supplierCompanyId)"><span>{{ supplier.supplierName }}</span><span class="strategy-supplier-meta"><strong>{{ supplier.totalAmount.toFixed(2) }}</strong></span></button></li></ul>
              </article>
            </div>
          </div>

          <section class="compare-fixed-fee-bar" aria-label="补给费用">
            <label><span>{{ providerLabel }}</span><span class="fixed-fee-input-with-action"><input :value="settings.fixedProviderName" :placeholder="`选择${providerLabel}`" readonly :disabled="comparisonLocked" /><IconButton icon="Search" :label="`选择${providerLabel}`" :disabled="comparisonLocked" @click="openProviderDialog" /></span></label>
            <label><span>运费</span><input v-model.number="settings.fixedFreightFee" type="number" min="0" step="0.01" :disabled="comparisonLocked" /></label>
            <label><span>报关费</span><input v-model.number="settings.fixedCustomsFee" type="number" min="0" step="0.01" :disabled="comparisonLocked" /></label>
            <label><span>吊机费</span><input v-model.number="settings.fixedCraneFee" type="number" min="0" step="0.01" :disabled="comparisonLocked" /></label>
            <label><span>其他费用</span><input v-model.number="settings.fixedOtherFee" type="number" min="0" step="0.01" :disabled="comparisonLocked" /></label>
            <div><span>补给费用</span><strong>{{ fixedFeeTotal.toFixed(2) }}</strong></div>
          </section>

          <div class="compare-list-toolbar list-search-toolbar">
            <label class="list-search-field"><span>伙食搜索</span><span class="list-search-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg></span><input v-model="skuKeyword" type="search" placeholder="搜索名称、规格、单位" /></label>
            <div class="compare-preference-filters" role="group" aria-label="比价偏好"><button type="button" class="active">价格优先</button><button type="button" disabled>质量优先</button></div>
            <label class="compare-markup-field"><span>利润%</span><input v-model.number="settings.markupPercent" type="number" min="0" max="1000" step="0.1" :disabled="comparisonLocked" /></label>
            <div class="toolbar-icon-actions">
              <IconButton icon="RefreshCw" label="刷新" :disabled="loading" @click="loadComparison(comparison.demand.demandId)" />
              <IconButton icon="Download" label="导出比价报价表" :loading="comparisonExporting" :disabled="loading || !currentQuoteItemIds.length" @click="exportComparisonSheet" />
              <IconButton icon="Upload" label="导入比价报价表" :loading="comparisonImporting" :disabled="loading || comparisonLocked" @click="openComparisonImport" />
              <IconButton icon="Save" label="保存比价设置" :loading="settingsSaving" :disabled="comparisonLocked" @click="persistComparisonSettings()" />
              <IconButton icon="Send" label="确认下单" variant="primary" :disabled="!canCreateOrder || loading" @click="openOrderDialog" />
              <input ref="comparisonImportInput" class="visually-hidden-input" type="file" accept=".xlsx" @change="handleComparisonImport" />
            </div>
          </div>
          <p v-if="notice" class="inline-success">{{ notice }}</p><p v-if="error" class="inline-error">{{ error }}</p>

          <DataTable :columns="comparisonColumns" :rows="filteredRows" :show-index="false" row-key="id" row-interactive :row-class="comparisonRowClass" :expanded-row-key="expandedRowId" empty-label="暂无有效报价" @row-click="(row) => expandedRowId = expandedRowId === String(row.id) ? '' : String(row.id)">
            <template #head-selection><span>序号</span></template>
            <template #cell-selection="{ row }"><label class="compare-row-select" @click.stop><input type="checkbox" :checked="isComparisonRowSelected(row)" :disabled="!isComparisonRowSelectable(row)" @change="toggleComparisonRow(row, ($event.target as HTMLInputElement).checked)" /><span>{{ row.sequenceNo }}</span></label></template>
            <template #cell-name="{ row }"><span class="compare-two-line-cell"><strong>{{ row.name }}</strong><small>{{ row.nameEn || '-' }}</small></span></template>
            <template #cell-specification="{ row }"><strong>{{ row.specification }}</strong></template>
            <template #cell-requestedQuantity="{ row }"><span class="compare-two-line-cell compare-two-line-cell--right"><input class="comparison-edit-input comparison-requested-quantity" type="number" min="0.01" step="0.01" :value="row.requestedQuantity" :disabled="comparisonLocked" @click.stop @input="updateComparisonValue(row, 'requestedQuantity', ($event.target as HTMLInputElement).value)" /><small>{{ row.unit }}</small></span></template>
            <template #cell-quotedQuantity="{ row }"><span :class="['compare-two-line-cell', 'compare-two-line-cell--right', { 'is-warning': !row.quantitySatisfied }]"><input class="comparison-edit-input comparison-quoted-quantity" type="number" min="0.01" step="0.01" :value="row.quotedQuantity" :disabled="comparisonLocked" @click.stop @input="updateComparisonValue(row, 'quotedQuantity', ($event.target as HTMLInputElement).value)" /><small>{{ row.unit }}</small></span></template>
            <template #cell-unitPrice="{ row }"><span class="money-stack money-stack--single"><input class="comparison-edit-input comparison-unit-price" type="number" min="0" step="0.01" :value="row.unitPrice" :disabled="comparisonLocked" @click.stop @input="updateComparisonValue(row, 'unitPrice', ($event.target as HTMLInputElement).value)" /></span></template>
            <template #cell-subtotal="{ row }"><span class="money-stack money-stack--single comparison-subtotal"><strong>{{ Number(row.subtotal).toFixed(2) }}</strong></span></template>
            <template #expanded-row="{ row, expanded }"><section v-if="expanded" class="compare-row-detail" @click.stop><section><h3>伙食需求与供应商报价</h3><div class="sku-comparison-table"><div class="sku-comparison-head"><span>字段</span><span>需求值</span><span>供应商报价</span></div><div class="sku-comparison-row"><span>名称</span><strong>{{ row.name }}</strong><em>{{ row.name }}</em></div><div class="sku-comparison-row"><span>规格</span><strong>{{ row.specification }}</strong><em>{{ row.specification }}</em></div><div class="sku-comparison-row"><span>数量 / 单位</span><strong>{{ row.requestedQuantity }} / {{ row.unit }}</strong><em>{{ row.quotedQuantity }} / {{ row.unit }}</em></div></div></section><section class="compare-row-remark-panel"><h3>报价信息</h3><textarea :value="`${row.supplierName} · ${Number(row.unitPrice).toFixed(2)} · ${row.priceSource}`" rows="5" readonly></textarea></section></section></template>
          </DataTable>
        </article>
      </LoadingOverlay>
      <Teleport to="body">
        <IconButton v-if="showBackTop" class="compare-back-top-button" icon="ArrowUp" label="回到顶部" @click="scrollComparisonToTop" />
      </Teleport>
    </section>

    <FoodTrafficShuttleSelector
      v-if="settings.supplyMode === 'SEA'"
      :open="providerDialogOpen"
      :initial-use-time="comparison?.demand.vesselEta || ''"
      :initial-demand-id="comparison?.demand.demandId"
      :initial-inquiry-no="comparison?.demand.inquiryNo || comparison?.demand.demandNo || ''"
      :initial-vessel-name="comparison?.demand.vesselName || ''"
      :initial-supply-port="comparison?.demand.supplyPort || ''"
      :initial-item-count="comparison?.demand.itemCount || 0"
      :initial-shuttle-id="orderTrafficService.shuttleId || settings.fixedProviderId"
      :initial-node-index="orderTrafficService.selectedNodeIndex"
      @close="providerDialogOpen = false"
      @select="selectTrafficShuttle"
    />

    <div v-else-if="providerDialogOpen" class="modal-backdrop" role="presentation" @click.self="providerDialogOpen = false">
      <section class="modal-card food-provider-dialog" role="dialog" aria-modal="true" :aria-label="`选择${providerLabel}`">
        <header><div><strong>选择{{ providerLabel }}</strong><p>来自本次报价供货商</p></div><IconButton icon="X" label="关闭" variant="plain" @click="providerDialogOpen = false" /></header>
        <div v-if="providerLoading" class="empty-state compact">加载中</div>
        <div v-else-if="supplierSummaries.length" class="food-provider-list"><button v-for="item in supplierSummaries" :key="item.supplierCompanyId" type="button" @click="selectLandSupplier(item)"><strong>{{ item.supplierName }}</strong><span>陆运供货商</span><em>{{ item.coveredItemCount }}/{{ totalItemCount }}</em></button></div>
        <div v-else class="empty-state compact">暂无可选{{ providerLabel }}</div>
      </section>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="orderDialogOpen" class="modal-backdrop" role="presentation" @click.self="!orderCreating && (orderDialogOpen = false)">
          <section class="purchase-order-confirm-dialog" role="dialog" aria-modal="true" aria-label="确认下单" @click.stop>
            <div v-if="orderCreating" class="purchase-order-confirm-loading-mask" role="status" aria-live="polite"><span>正在生成采购单</span></div>
            <header>
              <div><strong>确认下单</strong><span>生成采购单后将发送给供货商确认。</span></div>
              <IconButton icon="X" label="关闭" variant="plain" :disabled="orderCreating" @click="orderDialogOpen = false" />
            </header>
            <div class="purchase-order-dialog-body">
              <section class="purchase-order-checkout-hero">
                <div class="purchase-order-checkout-total">
                  <span>确认下单船舶</span>
                  <strong>{{ comparison?.demand.vesselName || '-' }}</strong>
                  <p>{{ comparison?.demand.inquiryNo || comparison?.demand.demandNo || '-' }} · {{ settings.supplyMode === 'SEA' ? '海运' : '陆运' }} · {{ comparison?.demand.supplyPort || '-' }}</p>
                </div>
                <dl class="purchase-order-checkout-metrics">
                  <div><dt>比价策略</dt><dd>{{ orderStrategyLabel }}</dd></div>
                  <div><dt>供应商数</dt><dd>{{ orderSupplierSummaries.length }}</dd></div>
                  <div><dt>下单SKU数量</dt><dd>{{ orderDraft?.selectedItems.length || 0 }} / {{ totalItemCount }}</dd></div>
                </dl>
              </section>

              <section class="purchase-order-section purchase-order-delivery-section">
                <header class="purchase-order-section-heading"><div><strong>运输信息</strong></div><span>确认联系人、到港和送达安排</span></header>
                <div class="purchase-order-form-grid">
                  <label><span>要求送达时间</span><StableDateTimeInput v-model="orderForm.requiredDeliveryTime" mode="datetime" step="60" :class="{ 'is-invalid': orderFormErrors.requiredDeliveryTime }" :disabled="orderCreating" /><small v-if="orderFormErrors.requiredDeliveryTime">{{ orderFormErrors.requiredDeliveryTime }}</small></label>
                  <label>
                    <span>选择联系人</span>
                    <select v-model="orderContactId" :class="{ 'is-invalid': orderFormErrors.deliveryContactName }" :disabled="orderCreating || !orderContacts.length" @change="handleOrderContactChange">
                      <option v-if="!orderContacts.length" value="" disabled>{{ orderContactError || '暂无企业维护联系人' }}</option>
                      <option v-for="contact in orderContacts" :key="contact.id" :value="contact.id">{{ contact.contactName }}</option>
                    </select>
                    <small v-if="orderFormErrors.deliveryContactName || orderContactError">{{ orderFormErrors.deliveryContactName || orderContactError }}</small>
                  </label>
                  <label><span>联系电话</span><input v-model="orderForm.deliveryContactPhone" type="text" placeholder="请输入联系电话" :class="{ 'is-invalid': orderFormErrors.deliveryContactPhone }" :disabled="orderCreating" /><small v-if="orderFormErrors.deliveryContactPhone">{{ orderFormErrors.deliveryContactPhone }}</small></label>
                  <label><span>联系邮箱</span><input v-model="orderForm.deliveryContactEmail" type="email" placeholder="请输入联系邮箱" :disabled="orderCreating" /></label>
                  <label class="purchase-order-remark-field"><span>送达具体地址</span><textarea v-model="orderForm.deliveryAddress" rows="2" placeholder="请输入港口、码头或船舶所在的具体交付地址" :class="{ 'is-invalid': orderFormErrors.deliveryAddress }" :disabled="orderCreating"></textarea><small v-if="orderFormErrors.deliveryAddress">{{ orderFormErrors.deliveryAddress }}</small></label>
                </div>
              </section>

              <section v-if="orderSupplierSummaries.length" class="purchase-order-supplier-summary">
                <header class="purchase-order-subtle-heading"><strong>供货商入选</strong><span><b>{{ selectedCost.toFixed(2) }}</b></span></header>
                <div><article v-for="supplier in orderSupplierSummaries" :key="supplier.supplierCompanyId" class="is-winner"><em>中标</em><strong>{{ supplier.supplierName }}</strong><span>SKU {{ supplier.count }}</span><b>{{ supplier.amount.toFixed(2) }}</b></article></div>
              </section>

              <section class="purchase-order-section purchase-order-supply-section">
                <header class="purchase-order-section-heading"><div><strong>补给费用</strong></div><em class="purchase-order-supply-mode-chip">{{ settings.supplyMode === 'SEA' ? '⛴ 海运' : '🚚 陆运' }}</em></header>
                <article v-if="settings.supplyMode === 'SEA'" class="purchase-order-supply-card purchase-order-supply-card--barge">
                  <div class="purchase-order-barge-card__head">
                    <div>
                      <strong>{{ [orderTrafficService.trafficVesselName || settings.fixedProviderName, orderBargeDateLabel].filter(Boolean).join(' / ') || '-' }}</strong>
                      <span>{{ orderTrafficService.shuttleNo || comparison?.demand.inquiryNo || comparison?.demand.demandNo || '-' }}</span>
                    </div>
                    <div class="purchase-order-barge-card__selected-fees">
                      <strong>{{ fixedFeeTotal.toFixed(2) }}</strong>
                      <span v-for="item in orderFixedFeeItems" :key="item.key">{{ item.label }} {{ item.value.toFixed(2) }}</span>
                    </div>
                  </div>
                  <div class="purchase-order-barge-card__route">
                    <span><i>⛴</i><strong>{{ orderBargeDeparturePointLabel }}</strong><small>{{ orderBargeOriginTimeLabel }}</small></span>
                    <div>
                      <em v-for="(node, index) in orderBargeNodeRows" :key="`${node.nodeName || index}-${index}`" :class="{ 'is-selected': index === Number(orderTrafficService.selectedNodeIndex) }"><b>{{ formatTrafficNodeTime(node) }}</b></em>
                    </div>
                    <span><i>⚓</i><strong>{{ orderBargeDestinationPointLabel }}</strong><small>{{ orderBargeDestinationTimeLabel }}</small></span>
                  </div>
                </article>
                <article v-else class="purchase-order-supply-card purchase-order-supply-card--supplier">
                  <div class="purchase-order-supply-card__main"><strong>{{ settings.fixedProviderName || '-' }}</strong></div>
                  <div class="purchase-order-supply-card__fees"><span v-for="item in orderFixedFeeItems" :key="item.key">{{ item.label }} <strong>{{ item.value.toFixed(2) }}</strong></span></div>
                  <div class="purchase-order-supply-card__total"><span>合计</span><strong>{{ fixedFeeTotal.toFixed(2) }}</strong></div>
                </article>
              </section>
              <p v-if="error" class="inline-error">{{ error }}</p>
            </div>
            <footer class="purchase-order-checkout-footer">
              <div class="purchase-order-checkout-bill">
                <span>报价价格 <strong>{{ quoteTotal(selectedCost).toFixed(2) }}</strong></span>
                <span>成本价格 <strong>{{ selectedCost.toFixed(2) }}</strong></span>
                <span>补给费用 <strong>{{ fixedFeeTotal.toFixed(2) }}</strong></span>
                <span class="is-profit">利润 <strong>{{ profitTotal(selectedCost).toFixed(2) }}</strong></span>
              </div>
              <nav><IconButton icon="X" label="取消" :disabled="orderCreating" @click="orderDialogOpen = false" /><IconButton icon="Send" label="确认生成采购单" variant="primary" :loading="orderCreating" @click="createOrder" /></nav>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>
