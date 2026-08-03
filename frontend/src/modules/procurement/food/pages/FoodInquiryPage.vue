<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import IconButton from "@/components/IconButton.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { listFoodDemands } from "../services/foodProcurementApi";
import type { FoodDemandSummary } from "../types";

const router = useRouter();
const keyword = ref("");
const status = ref("");
const dateFrom = ref("");
const dateTo = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const demands = ref<FoodDemandSummary[]>([]);

const statusOptions = [
  { value: "DRAFT", label: "草稿" },
  { value: "INQUIRY_SENT", label: "待报价" },
  { value: "QUOTED", label: "已报价" },
  { value: "ORDERED", label: "已下单" }
];

const visibleDemands = computed(() => demands.value.filter((row) => {
  const text = `${row.demandNo} ${row.inquiryNo || ""} ${row.vesselName} ${row.supplyPort}`.toLowerCase();
  return matchesFilters(text, buyerDisplayStatus(row), row.updatedAt);
}).sort(compareDemandRecency));

function compareDemandRecency(left: FoodDemandSummary, right: FoodDemandSummary) {
  return parseUpdatedAt(right.updatedAt) - parseUpdatedAt(left.updatedAt)
    || right.demandId - left.demandId;
}

function parseUpdatedAt(value: string) {
  const timestamp = Date.parse(value || "");
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function matchesFilters(text: string, rowStatus: string, updatedAt: string) {
  const rowDate = String(updatedAt || "").slice(0, 10);
  return (!keyword.value.trim() || text.includes(keyword.value.trim().toLowerCase()))
    && (!status.value || rowStatus === status.value)
    && (!dateFrom.value || rowDate >= dateFrom.value)
    && (!dateTo.value || rowDate <= dateTo.value);
}

function buyerDisplayStatus(row: FoodDemandSummary) {
  if (row.status === "DRAFT") return "DRAFT";
  if (row.status === "ORDERED") return "ORDERED";
  return row.submittedQuoteCount > 0 || row.status === "QUOTED" ? "QUOTED" : "INQUIRY_SENT";
}

function statusLabel(value: string) {
  return ({
    DRAFT: "草稿",
    SENT: "待报价",
    INQUIRY_SENT: "待报价",
    QUOTED: "已报价",
    SUBMITTED: "已报价",
    ORDERED: "已下单"
  } as Record<string, string>)[value] || value;
}

function statusVariant(value: string): "neutral" | "info" | "warning" | "success" {
  if (["QUOTED", "SUBMITTED", "ORDERED"].includes(value)) return "success";
  if (["SENT", "INQUIRY_SENT"].includes(value)) return "warning";
  if (value === "DRAFT") return "neutral";
  return "info";
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    demands.value = await listFoodDemands();
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "询价列表读取失败";
  } finally {
    loading.value = false;
  }
}

function reset() {
  keyword.value = "";
  status.value = "";
  dateFrom.value = "";
  dateTo.value = "";
  void load();
}

function openDemand(row: FoodDemandSummary) {
  router.push(`/procurement/food/${row.demandId}`);
}

function formatDateTime(value?: string) {
  return value ? value.replace("T", " ").slice(0, 16) : "-";
}

function quoteProgress(row: FoodDemandSummary) {
  return row.supplierCount ? Math.min(100, Math.round(row.submittedQuoteCount / row.supplierCount * 100)) : 0;
}

onMounted(load);
</script>

<template>
  <section class="food-page">
    <ExpandablePanel :show-header="false" :show-expand="true" class="inquiry-management-panel">
      <section class="filter-toolbar" aria-label="伙食询价筛选">
        <div class="filter-fields">
          <label class="filter-field filter-field--search">
            <span>检索</span>
            <span class="filter-search-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg></span>
            <input v-model="keyword" placeholder="单号、船舶检索" @keydown.enter.prevent="load" />
          </label>
          <label class="filter-field"><span>询价开始</span><StableDateTimeInput v-model="dateFrom" mode="date" /></label>
          <label class="filter-field"><span>询价结束</span><StableDateTimeInput v-model="dateTo" mode="date" /></label>
          <label class="filter-field">
            <span>状态</span>
            <select v-model="status"><option value="">全部</option><option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select>
          </label>
        </div>
        <div class="toolbar-icon-actions">
          <IconButton icon="Search" label="查询" @click="load" />
          <IconButton icon="X" label="重置" @click="reset" />
          <IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="load" />
          <IconButton icon="Plus" label="新增伙食需求" variant="primary" @click="router.push('/procurement/food')" />
        </div>
      </section>

      <div v-if="error" class="inline-error">{{ error }}</div>
      <div v-if="notice" class="permission-static-notice">{{ notice }}</div>
      <div v-if="loading" class="empty-state compact">加载中</div>
      <div v-else-if="!visibleDemands.length" class="empty-state compact">暂无伙食询价单</div>

      <section v-else class="procurement-query-card-list">
        <article v-for="row in visibleDemands" :key="row.demandId" class="procurement-query-card is-inquiry food-inquiry-card" tabindex="0" @click="openDemand(row)" @keydown.enter="openDemand(row)">
            <header class="procurement-query-card__head">
              <div><strong class="vessel-title"><span class="vessel-mini-icon" aria-hidden="true"></span>{{ row.vesselName || "-" }}</strong></div>
              <StatusBadge :label="statusLabel(buyerDisplayStatus(row))" :variant="statusVariant(buyerDisplayStatus(row))" />
            </header>
            <section class="procurement-query-card__body">
              <div class="procurement-query-card__primary"><strong>{{ row.inquiryNo || row.demandNo }}</strong><small>预计到港 {{ formatDateTime(row.vesselEta) }}</small></div>
              <div class="procurement-query-card__metrics">
                <div class="metric-sku"><span>SKU</span><strong>{{ row.itemCount }}</strong></div>
                <div class="metric-exact"><span>询价数</span><strong>{{ row.supplierCount }}</strong></div>
                <div class="metric-similar"><span>报价数</span><strong>{{ row.submittedQuoteCount }}</strong></div>
                <div class="metric-unmatched"><span>报价进度</span><strong>{{ quoteProgress(row) }}%</strong></div>
              </div>
              <div v-if="row.supplierCount" class="procurement-query-card__progress"><span><b>报价进度</b><em>{{ row.submittedQuoteCount }}/{{ row.supplierCount }}</em></span><i><b :style="{ width: `${quoteProgress(row)}%` }"></b></i></div>
            </section>
            <footer class="procurement-query-card__footer">
              <div class="food-inquiry-card__timing"><span>询价 {{ formatDateTime(row.inquirySentAt) }}</span><span>截止 {{ formatDateTime(row.quoteDeadlineAt) }}</span></div>
              <div class="icon-action-row">
                <IconButton icon="Eye" label="查询" @click.stop="openDemand(row)" />
                <IconButton icon="Search" label="一键比价" variant="primary" :disabled="row.submittedQuoteCount === 0" @click.stop="router.push(`/food/comparison/${row.demandId}`)" />
              </div>
            </footer>
        </article>
      </section>
    </ExpandablePanel>
  </section>
</template>
