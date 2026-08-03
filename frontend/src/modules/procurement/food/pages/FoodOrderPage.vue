<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import DataTable from "@/components/DataTable.vue";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import IconButton from "@/components/IconButton.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import type { TableColumn } from "@/types/workbench";
import FoodSettlementDrawer from "../components/FoodSettlementDrawer.vue";
import { buildFoodOrderExecution } from "../domain/orderExecution";
import { foodOrderStatusActions } from "../domain/orderStatus";
import { useFoodActor } from "../composables/useFoodActor";
import {
  actionFoodSettlement,
  actionSupplierFoodOrder,
  getFoodOrder,
  listBuyerFoodOrders,
  listFoodSettlements,
  listSupplierFoodOrders
} from "../services/foodProcurementApi";
import type { FoodOrderDetail, FoodOrderSummary, FoodSettlement, FoodSupplierOrderSummary } from "../types";

type FoodSettlementDisplayRow = {
  id: string;
  settlement?: FoodSettlement;
  supplierOrderId?: number;
  supplierName: string;
  type: string;
  isTransport?: boolean;
  quoteAmount: number;
  actualAmount?: number;
  status: string;
  statusLabel: string;
};

const route = useRoute();
const router = useRouter();
const { isSupplier } = useFoodActor();
const keyword = ref("");
const status = ref("");
const dateFrom = ref("");
const dateTo = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const orders = ref<Array<FoodOrderSummary | FoodSupplierOrderSummary>>([]);
const detail = ref<FoodOrderDetail>();
const settlements = ref<FoodSettlement[]>([]);
const detailTab = ref<"settlement" | "details">("settlement");
const selectedExecutionSupplierId = ref<number>();
const activeSettlement = ref<FoodSettlement>();
const settlementDrawerOpen = ref(false);
const settlementDrawerEditable = ref(false);

const statusActions = foodOrderStatusActions;
const stageLabels = ["确认中", "备货中", "运输中", "待补给", "补给中", "补给完成"];
const itemColumns: TableColumn[] = [
  { key: "supplierName", label: "供货商", width: "150px" },
  { key: "productName", label: "伙食名称", width: "240px" },
  { key: "specification", label: "规格", width: "180px" },
  { key: "quantity", label: "订单数量", width: "96px", align: "right" },
  { key: "unit", label: "单位", width: "72px" },
  { key: "unitPrice", label: "单价", width: "100px", align: "right" },
  { key: "amount", label: "金额", width: "110px", align: "right" },
  { key: "flags", label: "需求数量", width: "100px", align: "right" }
];

const visibleOrders = computed(() => orders.value.filter((row) => {
  const rowDate = String(row.updatedAt || "").slice(0, 10);
  return (!dateFrom.value || rowDate >= dateFrom.value) && (!dateTo.value || rowDate <= dateTo.value);
}));

function statusStageIndex(value?: string) {
  const normalized = String(value || "").toUpperCase();
  if (["PENDING_CONFIRMATION"].includes(normalized)) return 0;
  if (["CONFIRMED", "PREPARING"].includes(normalized)) return 1;
  if (["READY_TO_SHIP", "IN_TRANSIT"].includes(normalized)) return 2;
  if (["WAITING_SUPPLY"].includes(normalized)) return 3;
  if (["SUPPLYING"].includes(normalized)) return 4;
  if (["SUPPLIED", "COMPLETED"].includes(normalized)) return 5;
  return 0;
}

function stageState(row: FoodOrderSummary | FoodSupplierOrderSummary, index: number) {
  if (["REJECTED", "CANCELLED"].includes(row.status)) return "todo";
  const activeIndex = statusStageIndex(row.status);
  if (index < activeIndex || (activeIndex === stageLabels.length - 1 && index === activeIndex)) return "done";
  if (index === activeIndex) return "active";
  return "todo";
}

function statusLabel(value?: string) {
  const normalized = String(value || "").toUpperCase();
  return ({
    PENDING_CONFIRMATION: "确认中",
    CONFIRMED: "已确认",
    PREPARING: "备货中",
    READY_TO_SHIP: "待运输",
    IN_TRANSIT: "运输中",
    WAITING_SUPPLY: "待补给",
    SUPPLYING: "补给中",
    SUPPLIED: "补给完成",
    COMPLETED: "补给完成",
    REJECTED: "已拒绝"
  } as Record<string, string>)[normalized] || normalized || "-";
}

function statusVariant(value?: string): "info" | "success" | "warning" | "danger" {
  const normalized = String(value || "").toUpperCase();
  if (["SUPPLIED", "COMPLETED"].includes(normalized)) return "success";
  if (normalized === "REJECTED") return "danger";
  if (["WAITING_SUPPLY", "SUPPLYING"].includes(normalized)) return "warning";
  return "info";
}

const detailItems = computed(() => (detail.value?.items || [])
  .filter((item) => !selectedExecutionSupplierId.value || item.supplierOrderId === selectedExecutionSupplierId.value)
  .map((item) => ({
  itemId: item.orderItemId,
  supplierName: item.supplierName,
  productName: item.nameZh || item.nameEn || "-",
  specification: item.specification || "-",
  quantity: item.orderedQuantity,
  unit: item.unit,
  unitPrice: isSupplier.value ? item.unitPrice : (item.quotedUnitPrice ?? item.unitPrice),
  amount: isSupplier.value ? item.amount : (item.quotedAmount ?? item.amount),
  flags: item.requestedQuantity
  })) as unknown as Record<string, unknown>[]);
const detailItemsTotal = computed(() => detailItems.value.reduce((sum, row) => sum + Number(row.amount || 0), 0));

const requestedSupplierOrderId = computed(() => Number(route.query.supplierOrderId || 0) || undefined);
const visibleDetailSuppliers = computed(() => {
  const suppliers = detail.value?.suppliers || [];
  if (!isSupplier.value || !selectedExecutionSupplierId.value) return suppliers;
  return suppliers.filter((supplier) => supplier.supplierOrderId === selectedExecutionSupplierId.value);
});
const detailExecution = computed(() => buildFoodOrderExecution({
  orderStatus: (isSupplier.value ? visibleDetailSuppliers.value[0]?.status : undefined) || detail.value?.order.status,
  supplierStatuses: visibleDetailSuppliers.value.map((supplier) => supplier.status),
  supplyMode: detail.value?.order.supplyMode,
  fixedProviderType: detail.value?.order.fixedProviderType,
  fixedProviderName: detail.value?.order.fixedProviderName,
  fixedFreightFee: detail.value?.order.fixedFreightFee,
  fixedCustomsFee: detail.value?.order.fixedCustomsFee,
  fixedCraneFee: detail.value?.order.fixedCraneFee,
  fixedOtherFee: detail.value?.order.fixedOtherFee,
  trafficServiceJson: detail.value?.order.trafficServiceJson
}));
const detailTransport = computed(() => detailExecution.value.transport);
const detailStages = computed(() => detailExecution.value.stages.map((stage, index) => ({
  ...stage,
  label: stageLabels[index],
  dateText: stage.state === "done" ? detail.value?.order.updatedAt?.slice(0, 10) || "--" : "--"
})));
const detailSettlements = computed(() => settlements.value.filter((row) => row.orderId === detail.value?.order.orderId
  && (!isSupplier.value || !selectedExecutionSupplierId.value || row.supplierOrderId === selectedExecutionSupplierId.value)));
const detailSettlementRows = computed<FoodSettlementDisplayRow[]>(() => {
  const rows: FoodSettlementDisplayRow[] = visibleDetailSuppliers.value.map((supplier) => {
  const persisted = detailSettlements.value.find((row) => row.supplierOrderId === supplier.supplierOrderId);
  const settlement = persisted ? {
    ...persisted,
    vesselName: persisted.vesselName || detail.value?.order.vesselName,
    currency: persisted.currency || detail.value?.order.currency
  } : undefined;
  return {
    id: `supplier-${supplier.supplierOrderId}`,
    settlement,
    supplierOrderId: supplier.supplierOrderId,
    supplierName: supplier.supplierName,
    type: "伙食供应",
    quoteAmount: Number(supplier.subtotalAmount || 0),
    actualAmount: persisted?.actualAmount == null ? undefined : Number(persisted.actualAmount),
    status: persisted?.status || "",
    statusLabel: persisted ? settlementLabel(persisted.status) : "未生成"
  };
  });
  if (!isSupplier.value && detailTransport.value.hasSelection) {
    rows.push({
      id: "transport-service",
      supplierName: detailTransport.value.providerName,
      type: "补给费用",
      isTransport: true,
      quoteAmount: detailTransport.value.quoteAmount,
      actualAmount: undefined,
      status: "",
      statusLabel: "未生成"
    });
  }
  return rows;
});
const detailSettlementTotal = computed(() => detailSettlementRows.value.reduce((sum, row) => sum + row.quoteAmount, 0));
const activeSupplier = computed(() => {
  const suppliers = detail.value?.suppliers || [];
  return suppliers.find((supplier) => supplier.supplierOrderId === selectedExecutionSupplierId.value)
    || suppliers.find((supplier) => statusActions[supplier.status]?.length)
    || suppliers[0];
});
const activeSupplierAction = computed(() => {
  if (!activeSupplier.value) return undefined;
  return statusActions[activeSupplier.value.status]?.find((item) => !item.danger);
});
const supplierDeliveryAddress = computed(() => detail.value?.order.deliveryAddress
  || detail.value?.order.supplyPort
  || "-");
const deliveryContact = computed(() => [
  detail.value?.order.deliveryContactName,
  detail.value?.order.deliveryContactPhone
].filter(Boolean).join(" / ") || "-");

function supplierSummary(row: FoodOrderSummary | FoodSupplierOrderSummary): FoodSupplierOrderSummary | undefined {
  return "supplierOrderId" in row ? row : undefined;
}

function orderRowKey(row: FoodOrderSummary | FoodSupplierOrderSummary) {
  return supplierSummary(row) ? `${row.orderId}-${supplierSummary(row)!.supplierOrderId}` : String(row.orderId);
}

function openOrder(row: FoodOrderSummary | FoodSupplierOrderSummary) {
  if (isSupplier.value && "supplierOrderId" in row) {
    router.push({ path: `/supplier/food/orders/${row.orderId}`, query: { supplierOrderId: String(row.supplierOrderId) } });
    return;
  }
  router.push(`/food/orders/${row.orderId}`);
}

function openSupplierItems(supplierOrderId: number, supplierName: string) {
  selectedExecutionSupplierId.value = supplierOrderId;
  detailTab.value = "details";
  notice.value = `正在查看${supplierName}的订单明细`;
}

function selectDetailTab(value: "settlement" | "details") {
  detailTab.value = value;
  if (value === "settlement" && !isSupplier.value) selectedExecutionSupplierId.value = undefined;
}

function openSettlement(row: FoodSettlement, editable = false) {
  activeSettlement.value = row;
  settlementDrawerEditable.value = editable;
  settlementDrawerOpen.value = true;
}

function closeSettlement() {
  settlementDrawerOpen.value = false;
}

async function loadList() {
  loading.value = true;
  error.value = "";
  try {
    orders.value = isSupplier.value
      ? await listSupplierFoodOrders(keyword.value, status.value)
      : await listBuyerFoodOrders(keyword.value, status.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "采购订单读取失败";
  } finally {
    loading.value = false;
  }
}

async function loadDetail(id: number) {
  loading.value = true;
  error.value = "";
  try {
    const [orderResult, settlementResult] = await Promise.all([
      getFoodOrder(id, isSupplier.value),
      listFoodSettlements(isSupplier.value)
    ]);
    detail.value = orderResult;
    settlements.value = settlementResult;
    detailTab.value = "settlement";
    selectedExecutionSupplierId.value = isSupplier.value
      ? orderResult.suppliers.find((supplier) => supplier.supplierOrderId === requestedSupplierOrderId.value)?.supplierOrderId
        || orderResult.suppliers.find((supplier) => statusActions[supplier.status]?.length)?.supplierOrderId
        || orderResult.suppliers[0]?.supplierOrderId
      : undefined;
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "订单详情读取失败";
  } finally {
    loading.value = false;
  }
}

async function action(supplierOrderId: number, target: string) {
  if (!detail.value) return;
  const extra: Record<string, unknown> = {};
  if (target === "PREPARING") {
    const value = window.prompt("请输入预计备货完成时间（YYYY-MM-DDTHH:mm）", new Date(Date.now() + 86400000).toISOString().slice(0, 16));
    if (!value) return;
    extra.expectedReadyAt = value;
  }
  if (target === "REJECTED") {
    const value = window.prompt("请输入拒绝原因");
    if (!value?.trim()) return;
    extra.rejectReason = value.trim();
  }
  if (target === "IN_TRANSIT") {
    const value = window.prompt("请输入运输说明（可留空）", "伙食已装车/装船");
    if (value == null) return;
    extra.shipmentRemark = value;
  }
  if (!window.confirm(`确认执行“${statusActions[detail.value.suppliers.find((item) => item.supplierOrderId === supplierOrderId)?.status || ""]?.find((item) => item.target === target)?.label || target}”？`)) return;
  loading.value = true;
  try {
    detail.value = await actionSupplierFoodOrder(detail.value.order.orderId, supplierOrderId, target, extra);
    notice.value = `订单状态已更新为 ${statusLabel(target)}`;
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "订单状态更新失败";
  } finally {
    loading.value = false;
  }
}

async function settlementAction(row: FoodSettlement) {
  let target = "";
  if (row.status === "PENDING_INVOICE" && isSupplier.value) {
    openSettlement(row, true);
    return;
  } else if (row.status === "INVOICED" && !isSupplier.value) target = "SETTLED";
  else if (row.status === "SETTLED" && !isSupplier.value) target = "PAID";
  if (!target || !window.confirm(`确认将结算状态更新为 ${target}？`)) return;
  loading.value = true;
  try {
    await actionFoodSettlement(row.settlementId, target, isSupplier.value);
    settlements.value = await listFoodSettlements(isSupplier.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "结算状态更新失败";
  } finally {
    loading.value = false;
  }
}

async function submitSettlementInvoice(payload: { actualAmount: number; invoiceNo: string; invoiceAttachments: Array<{ fileId?: string; fileName: string; fileUrl: string }> }) {
  if (!activeSettlement.value) return;
  loading.value = true;
  error.value = "";
  try {
    await actionFoodSettlement(activeSettlement.value.settlementId, "INVOICED", true, payload);
    settlementDrawerOpen.value = false;
    notice.value = "发票和结算金额已提交";
    settlements.value = await listFoodSettlements(isSupplier.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "发票提交失败";
  } finally {
    loading.value = false;
  }
}

function settlementLabel(value: string) {
  return ({ PENDING_INVOICE: "待供发票", INVOICED: "待结算", SETTLED: "待付款", PAID: "已付款" } as Record<string, string>)[value] || value;
}

function reset() {
  keyword.value = "";
  status.value = "";
  dateFrom.value = "";
  dateTo.value = "";
  loadList();
}

watch([() => route.params.orderId, () => route.query.supplierOrderId, isSupplier], ([value]) => {
  const id = Number(value);
  detail.value = undefined;
  if (id > 0) loadDetail(id); else loadList();
}, { immediate: true });
</script>

<template>
  <section class="food-page food-order-page">
    <ExpandablePanel :show-header="false" class="inquiry-management-panel purchase-orders-panel">
      <template v-if="!detail">
        <section class="filter-toolbar" aria-label="伙食采购订单筛选">
          <div class="filter-fields">
            <label class="filter-field filter-field--search"><span>检索</span><span class="filter-search-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg></span><input v-model="keyword" placeholder="单号、船舶检索" @keydown.enter.prevent="loadList" /></label>
            <label class="filter-field"><span>执行状态</span><select v-model="status"><option value="">全部</option><option value="PENDING_CONFIRMATION">确认中</option><option value="PREPARING">备货中</option><option value="IN_TRANSIT">运输中</option><option value="WAITING_SUPPLY">待补给</option><option value="SUPPLYING">补给中</option><option value="SUPPLIED">补给完成</option><option value="REJECTED">已拒绝</option></select></label>
            <label class="filter-field"><span>下单开始</span><StableDateTimeInput v-model="dateFrom" mode="date" /></label>
            <label class="filter-field"><span>下单结束</span><StableDateTimeInput v-model="dateTo" mode="date" /></label>
          </div>
          <div class="toolbar-icon-actions"><IconButton icon="Search" label="查询" @click="loadList" /><IconButton icon="X" label="重置" @click="reset" /><IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="loadList" /></div>
        </section>
        <p v-if="error" class="inline-error">{{ error }}</p>
        <div v-if="loading" class="empty-state compact">加载中</div>
        <section v-else-if="visibleOrders.length" class="purchase-order-query-list">
          <article v-for="row in visibleOrders" :key="orderRowKey(row)" class="purchase-order-query-card" tabindex="0" @click="openOrder(row)" @keydown.enter="openOrder(row)">
            <header :class="['purchase-order-query-card__head', { 'is-supplier-view': isSupplier }]"><div class="purchase-order-query-card__vessel"><strong class="vessel-title"><span class="vessel-mini-icon" aria-hidden="true"></span>{{ row.vesselName || '-' }}</strong></div><div class="purchase-order-query-card__identity"><strong>{{ row.orderNo }}</strong></div><div v-if="supplierSummary(row)" class="purchase-order-query-card__supplier"><strong>{{ supplierSummary(row)?.supplierName }}</strong></div><div class="purchase-order-query-card__status"><StatusBadge :label="statusLabel(row.status)" :variant="statusVariant(row.status)" /></div></header>
            <section class="purchase-order-query-timeline" aria-label="订单执行进度"><div v-for="(label, index) in stageLabels" :key="`${orderRowKey(row)}-${label}`" :class="['purchase-order-query-stage', `is-${stageState(row, index)}`]"><i></i><strong>{{ label }}</strong></div></section>
            <footer class="purchase-order-query-card__footer"><div><span>靠泊时间</span><strong>{{ row.vesselEta?.replace('T', ' ') || '-' }}</strong></div><div><span>SKU</span><strong>{{ row.itemCount }}</strong></div><div><span>{{ isSupplier ? '订单金额' : '采购金额' }}</span><strong>{{ row.totalAmount.toFixed(2) }} {{ row.currency }}</strong></div><div class="icon-action-row"><IconButton icon="Eye" label="查看详情" @click.stop="openOrder(row)" /></div></footer>
          </article>
        </section>
        <div v-else class="empty-state compact">暂无伙食采购订单</div>
      </template>

      <template v-else>
        <p v-if="error" class="inline-error">{{ error }}</p><p v-if="notice" class="permission-static-notice">{{ notice }}</p>
        <div v-if="loading" class="compare-state-message">加载中</div>
        <section v-else class="purchase-order-detail">
          <article class="purchase-order-basic-card">
            <header><div class="purchase-order-basic-card__title"><strong>{{ detail.order.vesselName || '-' }}</strong><small>{{ detail.order.orderNo }}</small></div><div class="purchase-order-basic-card__right"><div v-if="isSupplier && activeSupplier && activeSupplierAction" class="purchase-order-basic-card__actions purchase-order-basic-card__actions--supplier"><IconButton :icon="activeSupplierAction.target === 'PREPARING' ? 'Check' : activeSupplierAction.target === 'READY_TO_SHIP' ? 'Save' : activeSupplierAction.target === 'IN_TRANSIT' ? 'Send' : 'Check'" :label="activeSupplierAction.label" :disabled="loading" @click="action(activeSupplier.supplierOrderId, activeSupplierAction.target)" /></div></div></header>
            <dl v-if="!isSupplier"><div><dt>补给港口</dt><dd>{{ detail.order.supplyPort || '-' }}</dd></div><div><dt>具体交付地址</dt><dd>{{ supplierDeliveryAddress }}</dd></div><div><dt>预靠港时间</dt><dd>{{ detail.order.vesselEta?.replace('T', ' ') || '-' }}</dd></div><div><dt>要求送达时间</dt><dd>{{ detail.order.requiredDeliveryTime?.replace('T', ' ') || '-' }}</dd></div><div><dt>联系人 / 联系电话</dt><dd>{{ deliveryContact }}</dd></div><div><dt>联系邮箱</dt><dd>{{ detail.order.deliveryContactEmail || '-' }}</dd></div><div><dt>包装方式</dt><dd>{{ detail.order.defaultPackagingMethod === 'SUPPLIER_PACKAGING' ? '供货商包装' : detail.order.defaultPackagingMethod === 'UNIFIED_PACKAGING' ? '统一包装' : '-' }}</dd></div><div><dt>交付备注</dt><dd>{{ detail.order.buyerRemark || '-' }}</dd></div></dl>
            <dl v-else class="food-supplier-delivery-grid"><div><dt>联系人</dt><dd>{{ detail.order.deliveryContactName || '-' }}</dd></div><div><dt>联系电话</dt><dd>{{ detail.order.deliveryContactPhone || '-' }}</dd></div><div><dt>联系邮箱</dt><dd>{{ detail.order.deliveryContactEmail || '-' }}</dd></div><div><dt>包装方式</dt><dd>{{ detail.order.defaultPackagingMethod === 'SUPPLIER_PACKAGING' ? '供货商包装' : detail.order.defaultPackagingMethod === 'UNIFIED_PACKAGING' ? '统一包装' : '-' }}</dd></div><div class="food-supplier-delivery-grid__address"><dt>交付地址</dt><dd>{{ supplierDeliveryAddress }}</dd></div></dl>
          </article>

          <article class="purchase-order-execution-card" aria-label="采购订单执行状态"><div class="purchase-order-execution-rail"><div v-for="stage in detailStages" :key="stage.key" :class="['purchase-order-execution-stage__item', `is-${stage.state}`]"><span class="purchase-order-execution-stage__count">{{ stage.progressText }}</span><i></i><strong>{{ stage.label }}</strong><small>日期：{{ stage.dateText }}</small></div></div></article>

          <section v-if="visibleDetailSuppliers.length" class="purchase-order-execution-situation" aria-label="订单执行情况">
            <div class="purchase-order-execution-situation__header"><h3>执行情况</h3></div>
            <div class="purchase-order-execution-situation__grid">
              <button v-for="supplier in visibleDetailSuppliers" :key="supplier.supplierOrderId" type="button" :class="['purchase-order-execution-situation-card', { active: selectedExecutionSupplierId === supplier.supplierOrderId }]" @click="openSupplierItems(supplier.supplierOrderId, supplier.supplierName)"><span>供货商</span><strong>{{ supplier.supplierName }}</strong><StatusBadge :label="statusLabel(supplier.status)" :variant="statusVariant(supplier.status)" /><small>时间：{{ supplier.expectedReadyAt?.replace('T', ' ') || detail.order.updatedAt?.replace('T', ' ') || '-' }}</small></button>
              <article v-if="!isSupplier && detailTransport.hasSelection" :class="['purchase-order-execution-situation-card', { 'is-barge': detailTransport.kind === 'BARGE' }]">
                <span>{{ detailTransport.roleLabel }}</span>
                <strong>{{ detailTransport.providerName }}</strong>
                <StatusBadge :label="statusLabel(detailTransport.status)" :variant="statusVariant(detailTransport.status)" />
                <small>时间：{{ detail.order.updatedAt?.replace('T', ' ') || '-' }}</small>
              </article>
            </div>
          </section>

          <nav class="purchase-order-detail-tabs purchase-order-detail-tabs--inline" aria-label="采购订单后续页签"><button type="button" :class="{ active: detailTab === 'settlement' }" @click="selectDetailTab('settlement')">订单结算</button><button type="button" :class="{ active: detailTab === 'details' }" @click="selectDetailTab('details')">订单明细</button></nav>

          <article v-if="detailTab === 'details'" class="purchase-order-section"><div class="purchase-order-section-header"><h3>订单明细</h3><strong class="purchase-order-section-total">总计 {{ detailItemsTotal.toFixed(2) }} {{ detail.order.currency }}</strong></div><DataTable :columns="itemColumns" :rows="detailItems" row-key="itemId"><template #cell-productName="{ value }"><span class="purchase-item-name" :title="String(value || '-')">{{ value || '-' }}</span></template><template #cell-unitPrice="{ row }"><span class="money-stack money-stack--single"><strong>{{ Number(row.unitPrice).toFixed(2) }}</strong></span></template><template #cell-amount="{ row }"><span class="money-stack money-stack--single"><strong>{{ Number(row.amount).toFixed(2) }}</strong></span></template></DataTable></article>

          <article v-if="isSupplier && detailTab === 'settlement'" class="purchase-order-section purchase-order-settlement-section supplier-order-settlement-section"><section class="settlement-card-list settlement-card-list--embedded"><article v-for="row in detailSettlementRows" :key="row.id" class="settlement-card is-provider"><header><div><strong class="settlement-card__no"><span class="settlement-money-icon" aria-hidden="true"></span>{{ row.settlement ? `FS-${row.settlement.settlementId}` : '结算待生成' }}</strong><small>{{ detail.order.orderNo }}</small></div><StatusBadge :label="row.statusLabel" :variant="row.status === 'PAID' ? 'success' : 'warning'" /></header><dl><div><dt>供货商</dt><dd>{{ row.supplierName }}</dd></div><div><dt>船舶名称</dt><dd>{{ detail.order.vesselName }}</dd></div><div><dt>报价金额</dt><dd>{{ row.quoteAmount.toFixed(2) }} {{ detail.order.currency }}</dd></div><div><dt>实际金额</dt><dd>{{ row.actualAmount == null ? '--' : row.actualAmount.toFixed(2) }} {{ detail.order.currency }}</dd></div></dl><footer><span>发票附件 {{ row.settlement?.invoiceAttachments?.length || 0 }} 个</span><div class="icon-action-row"><IconButton icon="Eye" label="查看结算" :disabled="!row.settlement" @click="row.settlement && openSettlement(row.settlement)" /><IconButton v-if="row.settlement?.status === 'PENDING_INVOICE'" icon="Pencil" label="编辑结算" @click="openSettlement(row.settlement, true)" /></div></footer></article></section></article>

          <article v-if="!isSupplier && detailTab === 'settlement'" class="purchase-order-section purchase-order-settlement-section"><div class="purchase-order-section-header"><h3>订单结算</h3><div class="purchase-settlement-head-actions"><strong>总计 {{ detailSettlementTotal.toFixed(2) }} {{ detail.order.currency }}</strong></div></div><div class="purchase-order-settlement-table-wrap"><table class="purchase-order-settlement-table"><thead><tr><th></th><th>序号</th><th>服务商</th><th>类型</th><th>报价金额</th><th>实际金额</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="(row, index) in detailSettlementRows" :key="row.id"><td><input type="checkbox" :disabled="!row.settlement || !['INVOICED','SETTLED'].includes(row.status)" /></td><td>{{ index + 1 }}</td><td>{{ row.supplierName }}</td><td>{{ row.type }}</td><td>{{ row.quoteAmount.toFixed(2) }}</td><td><strong>{{ row.actualAmount == null ? '--' : row.actualAmount.toFixed(2) }}</strong></td><td><span :class="['purchase-order-settlement-status', row.status === 'PAID' ? 'is-settled' : 'is-pending']">{{ row.statusLabel }}</span></td><td><div class="icon-action-row"><IconButton icon="Eye" label="查看结算" :disabled="!row.settlement" @click="row.settlement && openSettlement(row.settlement)" /><IconButton v-if="row.settlement && ['INVOICED','SETTLED'].includes(row.status)" icon="Check" :label="row.status === 'INVOICED' ? '确认结算' : '确认付款'" variant="primary" @click="settlementAction(row.settlement)" /></div></td></tr><tr v-if="!detailSettlementRows.length"><td colspan="8">暂无结算数据</td></tr></tbody></table></div></article>
        </section>
      </template>
    </ExpandablePanel>

    <FoodSettlementDrawer
      :open="settlementDrawerOpen"
      :settlement="activeSettlement"
      :editable="settlementDrawerEditable"
      :saving="loading"
      @close="closeSettlement"
      @submit="submitSettlementInvoice"
    />
  </section>
</template>
