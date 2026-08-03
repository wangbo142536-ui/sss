<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import IconButton from "@/components/IconButton.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { useFoodActor } from "../composables/useFoodActor";
import {
  commitFoodQuoteImport,
  exportFoodQuote,
  getBuyerFoodQuote,
  getSupplierFoodQuote,
  listBuyerFoodInquiries,
  listSupplierFoodInquiries,
  previewFoodQuoteImport,
  saveSupplierFoodQuote,
  submitFoodQuote,
  virtualFillFoodQuote
} from "../services/foodProcurementApi";
import type { FoodImportPreview, FoodInquiry, FoodQuote, FoodQuoteItem } from "../types";

const route = useRoute();
const router = useRouter();
const { isSupplier } = useFoodActor();
type QuoteActor = "BUYER" | "SUPPLIER";
type QuoteInquiry = FoodInquiry & { actor: QuoteActor };

const importInput = ref<HTMLInputElement>();
const quoteTableScrollRef = ref<HTMLElement>();
const showBackTop = ref(false);
const keyword = ref("");
const status = ref("");
const dateFrom = ref("");
const dateTo = ref("");
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const notice = ref("");
const inquiries = ref<QuoteInquiry[]>([]);
const quote = ref<FoodQuote>();
const detailActor = ref<QuoteActor>("SUPPLIER");
const importPreview = ref<FoodImportPreview>();

const isEditable = computed(() => quote.value?.status === "DRAFT");
const localTotal = computed(() => (quote.value?.items || []).reduce((sum, item) => sum + Number(item.requestedQuantity || 0) * Number(item.unitPrice || 0), 0));
const visibleInquiries = computed(() => inquiries.value.filter((row) => {
  const rowDate = String(row.updatedAt || "").slice(0, 10);
  const demandId = Number(route.query.demandId);
  const text = `${row.demandNo} ${row.inquiryNo || ""} ${row.vesselName} ${row.supplierName} ${row.buyerName}`.toLowerCase();
  return (!Number.isFinite(demandId) || demandId <= 0 || row.demandId === demandId)
    && (!keyword.value.trim() || text.includes(keyword.value.trim().toLowerCase()))
    && (!dateFrom.value || rowDate >= dateFrom.value)
    && (!dateTo.value || rowDate <= dateTo.value);
}).sort(compareInquiryRecency));

function compareInquiryRecency(left: FoodInquiry, right: FoodInquiry) {
  return parseUpdatedAt(right.updatedAt) - parseUpdatedAt(left.updatedAt)
    || right.inquirySupplierId - left.inquirySupplierId;
}

function parseUpdatedAt(value: string) {
  const timestamp = Date.parse(value || "");
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function quoteStatusLabel(value: string) {
  return ({ DRAFT: "待报价", SENT: "待报价", NOT_STARTED: "待报价", SUBMITTED: "已报价" } as Record<string, string>)[value] || value;
}

function quoteStatusVariant(value: string): "neutral" | "info" | "warning" | "success" {
  return value === "SUBMITTED" ? "success" : "warning";
}

async function loadList() {
  loading.value = true;
  error.value = "";
  try {
    const [received, sent] = await Promise.all([
      listSupplierFoodInquiries(keyword.value, status.value),
      listBuyerFoodInquiries(keyword.value, status.value)
    ]);
    inquiries.value = [
      ...received.map((row) => ({ ...row, actor: "SUPPLIER" as const })),
      ...sent.map((row) => ({ ...row, actor: "BUYER" as const }))
    ];
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "报价列表读取失败";
  } finally {
    loading.value = false;
  }
}

function routeActor(value: unknown): QuoteActor {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "BUYER" || normalized === "SUPPLIER") return normalized;
  return isSupplier.value ? "SUPPLIER" : "BUYER";
}

function openQuote(row: QuoteInquiry) {
  void router.push({ path: `/food/quotes/${row.quoteId}`, query: { actor: row.actor.toLowerCase() } });
}

async function loadQuote(id: number, actor = routeActor(route.query.actor)) {
  loading.value = true;
  error.value = "";
  detailActor.value = actor;
  try {
    quote.value = actor === "SUPPLIER" ? await getSupplierFoodQuote(id) : await getBuyerFoodQuote(id);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "报价单读取失败";
  } finally {
    loading.value = false;
  }
}

function markManual(item: FoodQuoteItem) {
  item.quotedQuantity = item.requestedQuantity;
  item.availability = "AVAILABLE";
  item.priceSource = "MANUAL";
}

async function save() {
  if (!quote.value) return;
  saving.value = true;
  error.value = "";
  try {
    quote.value = await saveSupplierFoodQuote(quote.value);
    notice.value = "报价草稿已保存";
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "报价保存失败";
  } finally {
    saving.value = false;
  }
}

async function virtualFill() {
  if (!quote.value || !window.confirm("一键报价将覆盖所有 SKU 当前单价，确认继续？")) return;
  saving.value = true;
  error.value = "";
  try {
    const result = await virtualFillFoodQuote(quote.value.quoteId, true);
    notice.value = `已为 ${result.filledCount} 项 SKU 生成虚拟报价`;
    await loadQuote(quote.value.quoteId, detailActor.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "虚拟报价补全失败";
  } finally {
    saving.value = false;
  }
}

async function submit() {
  if (!quote.value || !window.confirm("确认提交报价？提交后状态将变为已报价。")) return;
  saving.value = true;
  error.value = "";
  try {
    const saved = await saveSupplierFoodQuote(quote.value);
    quote.value = await submitFoodQuote(saved.quoteId);
    notice.value = "报价已提交";
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "报价提交失败";
  } finally {
    saving.value = false;
  }
}

async function importFile(file?: File) {
  if (!quote.value || !file) return;
  saving.value = true;
  try {
    importPreview.value = await previewFoodQuoteImport(quote.value.quoteId, file);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "报价导入预览失败";
  } finally {
    saving.value = false;
    if (importInput.value) importInput.value.value = "";
  }
}

async function commitImport() {
  if (!quote.value || !importPreview.value) return;
  saving.value = true;
  try {
    const result = await commitFoodQuoteImport(quote.value.quoteId, importPreview.value.batchId);
    notice.value = `已导入更新 ${result.updatedCount} 项报价`;
    importPreview.value = undefined;
    await loadQuote(quote.value.quoteId);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "导入更新失败";
  } finally {
    saving.value = false;
  }
}

function reset() {
  keyword.value = "";
  status.value = "";
  dateFrom.value = "";
  dateTo.value = "";
  void loadList();
}

function handleQuoteTableScroll() {
  showBackTop.value = Number(quoteTableScrollRef.value?.scrollTop || 0) > 120;
}

function scrollQuoteTableToTop() {
  const target = quoteTableScrollRef.value;
  if (!target) return;
  const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
  target.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
}

watch(() => [route.params.quoteId, route.query.actor] as const, ([value, actor]) => {
  const id = Number(value);
  quote.value = undefined;
  showBackTop.value = false;
  if (id > 0) void loadQuote(id, routeActor(actor));
  else void loadList();
}, { immediate: true });
</script>

<template>
  <section class="food-page">
    <template v-if="!quote">
      <section class="filter-toolbar" aria-label="伙食报价筛选">
        <div class="filter-fields">
          <label class="filter-field filter-field--search"><span>检索</span><span class="filter-search-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg></span><input v-model="keyword" placeholder="单号、船舶、供货商检索" @keydown.enter.prevent="loadList" /></label>
          <label class="filter-field"><span>报价开始</span><StableDateTimeInput v-model="dateFrom" mode="date" /></label>
          <label class="filter-field"><span>报价结束</span><StableDateTimeInput v-model="dateTo" mode="date" /></label>
          <label class="filter-field"><span>报价状态</span><select v-model="status"><option value="">全部</option><option value="DRAFT">待报价</option><option value="SUBMITTED">已报价</option></select></label>
        </div>
        <div class="toolbar-icon-actions"><IconButton icon="Search" label="查询" @click="loadList" /><IconButton icon="X" label="重置" @click="reset" /><IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="loadList" /></div>
      </section>
      <p v-if="error" class="food-alert is-error">{{ error }}</p>
      <section class="food-panel">
        <div class="food-table-scroll"><table class="food-table"><thead><tr><th>报价编号</th><th>船舶</th><th>业务视角</th><th>往来公司</th><th>SKU</th><th>已报价</th><th>待报价</th><th>总额</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-for="row in visibleInquiries" :key="`${row.actor}-${row.inquirySupplierId}`"><td><strong>{{ row.inquiryNo || row.demandNo }}</strong></td><td>{{ row.vesselName }}</td><td><StatusBadge :label="row.actor === 'SUPPLIER' ? '收到' : '发出'" :variant="row.actor === 'SUPPLIER' ? 'info' : 'neutral'" /></td><td>{{ row.actor === "SUPPLIER" ? row.buyerName : row.supplierName }}</td><td>{{ row.itemCount }}</td><td>{{ row.quotedItemCount }}</td><td>{{ row.missingItemCount }}</td><td>{{ row.totalAmount?.toFixed(2) }}</td><td><StatusBadge :label="quoteStatusLabel(row.quoteStatus)" :variant="quoteStatusVariant(row.quoteStatus)" /></td><td><div class="icon-action-row"><IconButton :icon="row.actor === 'SUPPLIER' && row.quoteStatus !== 'SUBMITTED' ? 'Pencil' : 'Eye'" :label="row.actor === 'SUPPLIER' && row.quoteStatus !== 'SUBMITTED' ? '填写报价' : '查看报价'" :disabled="!row.quoteId" @click="openQuote(row)" /></div></td></tr>
          <tr v-if="!visibleInquiries.length"><td colspan="10" class="food-empty">暂无伙食报价单</td></tr>
        </tbody></table></div>
      </section>
    </template>

    <template v-else>
      <section class="food-quote-glass">
        <header class="food-quote-hero">
          <div><button type="button" class="food-back-link" @click="router.push('/food/quotes')">返回报价列表</button><h1>{{ quote.quoteNo }}</h1><p>{{ quote.vesselName }} · {{ quote.supplyPort }} · {{ quote.currency }}</p></div>
          <div class="food-quote-total"><span>报价总额</span><strong>{{ localTotal.toFixed(2) }}</strong><small>{{ quote.currency }}</small></div>
        </header>
        <div class="food-quote-facts">
          <span>采购方 <b>{{ quote.buyerName }}</b></span><span>供货商 <b>{{ quote.supplierName }}</b></span><span>SKU <b>{{ quote.items.length }}</b></span><StatusBadge :label="quoteStatusLabel(quote.status)" :variant="quoteStatusVariant(quote.status)" />
          <div class="food-quote-toolbar">
            <IconButton icon="Download" label="导出" @click="exportFoodQuote(quote.quoteId)" />
            <input ref="importInput" type="file" accept=".xlsx,.xls" hidden @change="importFile(($event.target as HTMLInputElement).files?.[0])" />
            <IconButton icon="Upload" label="导入" :disabled="!isEditable" @click="importInput?.click()" />
            <button type="button" class="food-virtual-button" :disabled="!isEditable || saving" @click="virtualFill">一键报价</button>
            <IconButton icon="Save" label="保存" :disabled="!isEditable" :loading="saving" @click="save" />
            <IconButton icon="Send" label="提交" variant="primary" :disabled="!isEditable" :loading="saving" @click="submit" />
          </div>
        </div>
        <p v-if="error" class="food-alert is-error">{{ error }}</p><p v-if="notice" class="food-alert is-success">{{ notice }}</p>
        <div ref="quoteTableScrollRef" class="food-table-scroll food-quote-table-wrap" @scroll="handleQuoteTableScroll"><table class="food-table food-quote-table"><thead><tr><th>序号</th><th>名称</th><th>规格</th><th>单位</th><th>需求数量</th><th>单价</th><th>金额</th><th>备注</th></tr></thead><tbody>
          <tr v-for="item in quote.items" :key="item.quoteItemId">
            <td>{{ item.sequenceNo }}</td><td><strong>{{ item.nameZh || item.nameEn }}</strong><small>{{ item.nameZh ? item.nameEn : "" }}</small></td><td>{{ item.specification || "-" }}</td><td>{{ item.unit }}</td><td>{{ item.requestedQuantity }}</td>
            <td><input v-model.number="item.unitPrice" type="number" min="0" step="0.0001" :disabled="!isEditable" @input="markManual(item)" /></td><td>{{ (Number(item.requestedQuantity || 0) * Number(item.unitPrice || 0)).toFixed(2) }}</td><td><input v-model="item.supplierRemark" :disabled="!isEditable" placeholder="备注" @input="markManual(item)" /></td>
          </tr>
        </tbody></table></div>
      </section>
      <Teleport to="body">
        <IconButton v-if="showBackTop" class="compare-back-top-button" icon="ArrowUp" label="回到顶部" @click="scrollQuoteTableToTop" />
      </Teleport>
    </template>

    <div v-if="importPreview" class="food-modal-backdrop" @click.self="importPreview = undefined"><section class="food-modal food-import-modal" role="dialog" aria-modal="true"><header><div><h2>导入报价预览</h2><p>{{ importPreview.fileName }} · {{ importPreview.selectedSheet }}</p></div><IconButton icon="X" label="关闭" @click="importPreview = undefined" /></header><div class="food-import-summary"><span>匹配 <b>{{ importPreview.matchedCount }}</b></span><span>问题 <b>{{ importPreview.issueCount }}</b></span></div><div class="food-table-scroll"><table class="food-table"><thead><tr><th>源行</th><th>名称</th><th>规格</th><th>单位</th><th>问题</th></tr></thead><tbody><tr v-for="issue in importPreview.issues" :key="`${issue.sourceRow}-${issue.issueType}`"><td>{{ issue.sourceRow }}</td><td>{{ issue.nameZh || issue.nameEn }}</td><td>{{ issue.specification }}</td><td>{{ issue.unit }}</td><td>{{ issue.message }}</td></tr><tr v-if="!importPreview.issues.length"><td colspan="5" class="food-empty">全部精确匹配，可以更新</td></tr></tbody></table></div><footer><button type="button" class="food-secondary-button" @click="importPreview = undefined">取消</button><button type="button" class="food-primary-button" :disabled="!importPreview.matchedCount" @click="commitImport">确认更新 {{ importPreview.matchedCount }} 项</button></footer></section></div>
  </section>
</template>
