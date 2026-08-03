<script setup lang="ts">
import { computed, ref, watch } from "vue";
import DetailDrawer from "@/components/DetailDrawer.vue";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import IconButton from "@/components/IconButton.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { getAuthSession, uploadQualificationFile } from "@/services/authService";
import type { BusinessAttachmentPayload } from "@/services/fulfillmentService";
import FoodSettlementDrawer from "../components/FoodSettlementDrawer.vue";
import { useFoodActor } from "../composables/useFoodActor";
import {
  actionFoodSettlement,
  listFoodEvaluations,
  listFoodSettlements,
  submitFoodEvaluation
} from "../services/foodProcurementApi";
import type { FoodEvaluation, FoodSettlement } from "../types";

const props = defineProps<{ mode: "settlement" | "evaluation" }>();
const { isSupplier } = useFoodActor();
const status = ref("");
const keyword = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const settlements = ref<FoodSettlement[]>([]);
const evaluations = ref<FoodEvaluation[]>([]);
const editingEvaluation = ref<FoodEvaluation>();
const activeSettlement = ref<FoodSettlement>();
const settlementEditorOpen = ref(false);
const settlementEditorEditable = ref(false);
const evaluationForm = ref({ qualityRating: 5, logisticsRating: 5, comment: "", attachments: [] as BusinessAttachmentPayload[] });
const evaluationUploading = ref(false);
const evaluationPreviewing = ref("");
const evaluationEditorReadonly = computed(() => !editingEvaluation.value
  || !["PENDING_EVALUATION", "REJECTED"].includes(String(editingEvaluation.value.status || "").toUpperCase()));

const statusOptions = computed(() => props.mode === "settlement"
  ? [
      { value: "PENDING_INVOICE", label: "待供发票" },
      { value: "INVOICED", label: "待结算" },
      { value: "SETTLED", label: "待付款" },
      { value: "PAID", label: "已付款" }
    ]
  : [
      { value: "PENDING_EVALUATION", label: "待评价" },
      { value: "PENDING_REVIEW", label: "待审核" },
      { value: "APPROVED", label: "已通过" },
      { value: "REJECTED", label: "已退回" }
    ]);

const visibleSettlements = computed(() => settlements.value.filter((row) => {
  const text = `${row.orderNo} ${row.supplierName} ${row.invoiceNo || ""}`.toLowerCase();
  return !keyword.value.trim() || text.includes(keyword.value.trim().toLowerCase());
}));

const visibleEvaluations = computed(() => evaluations.value.filter((row) => {
  const text = `${row.orderNo} ${row.supplierName} ${row.comment || ""}`.toLowerCase();
  return !keyword.value.trim() || text.includes(keyword.value.trim().toLowerCase());
}));

function settlementLabel(value: string) {
  return ({ PENDING_INVOICE: "待供发票", INVOICED: "待结算", SETTLED: "待付款", PAID: "已付款" } as Record<string, string>)[value] || value;
}

function evaluationLabel(value: string) {
  return ({ PENDING_EVALUATION: "待评价", PENDING_REVIEW: "待审查", APPROVED: "已完成", REJECTED: "已驳回" } as Record<string, string>)[value] || value;
}

function badgeVariant(value: string): "neutral" | "info" | "warning" | "success" | "danger" {
  if (["PAID", "APPROVED"].includes(value)) return "success";
  if (value === "REJECTED") return "danger";
  if (["PENDING_INVOICE", "PENDING_EVALUATION"].includes(value)) return "warning";
  return "info";
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    if (props.mode === "settlement") settlements.value = await listFoodSettlements(isSupplier.value, status.value);
    else evaluations.value = await listFoodEvaluations(status.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "数据读取失败";
  } finally {
    loading.value = false;
  }
}

function reset() {
  status.value = "";
  keyword.value = "";
  load();
}

async function settlementAction(row: FoodSettlement) {
  let target = "";
  if (row.status === "PENDING_INVOICE" && isSupplier.value) {
    openSettlement(row, true);
    return;
  } else if (row.status === "INVOICED" && !isSupplier.value) target = "SETTLED";
  else if (row.status === "SETTLED" && !isSupplier.value) target = "PAID";
  if (!target || !window.confirm(`确认将结算状态更新为 ${settlementLabel(target)}？`)) return;
  loading.value = true;
  try {
    await actionFoodSettlement(row.settlementId, target, isSupplier.value);
    await load();
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "结算状态更新失败";
  } finally {
    loading.value = false;
  }
}

function openSettlement(row: FoodSettlement, editable = false) {
  activeSettlement.value = row;
  settlementEditorEditable.value = editable;
  settlementEditorOpen.value = true;
}

async function submitSettlementInvoice(payload: { actualAmount: number; invoiceNo: string; invoiceAttachments: Array<{ fileId?: string; fileName: string; fileUrl: string }> }) {
  if (!activeSettlement.value) return;
  loading.value = true;
  error.value = "";
  try {
    await actionFoodSettlement(activeSettlement.value.settlementId, "INVOICED", true, payload);
    settlementEditorOpen.value = false;
    notice.value = "发票和结算金额已提交";
    await load();
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "发票提交失败";
  } finally {
    loading.value = false;
  }
}

function openEvaluation(row: FoodEvaluation) {
  editingEvaluation.value = row;
  evaluationForm.value = {
    qualityRating: row.qualityRating || 5,
    logisticsRating: row.logisticsRating || 5,
    comment: row.comment || "",
    attachments: [...(row.attachments || [])]
  };
}

async function uploadEvaluationFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  evaluationUploading.value = true;
  error.value = "";
  try {
    const uploaded = await uploadQualificationFile(file);
    const fileId = String(uploaded.fileId || uploaded.id || "");
    evaluationForm.value.attachments.push({
      fileId,
      fileName: uploaded.name || file.name,
      fileUrl: uploaded.url || (fileId ? `/api/files/${encodeURIComponent(fileId)}` : "")
    });
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "评价附件上传失败";
  } finally {
    evaluationUploading.value = false;
    input.value = "";
  }
}

async function previewEvaluationAttachment(file: BusinessAttachmentPayload) {
  const source = file.fileUrl || (file.fileId ? `/api/files/${encodeURIComponent(file.fileId)}` : "");
  if (!source) return;
  evaluationPreviewing.value = file.fileId || file.fileName;
  error.value = "";
  try {
    const session = getAuthSession();
    const response = await fetch(source, { headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {} });
    if (!response.ok) throw new Error("评价附件读取失败");
    const objectUrl = URL.createObjectURL(await response.blob());
    window.open(objectUrl, "_blank", "noopener,noreferrer");
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "评价附件读取失败";
  } finally {
    evaluationPreviewing.value = "";
  }
}

async function submitEvaluation() {
  if (!editingEvaluation.value) return;
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await submitFoodEvaluation(
      editingEvaluation.value.evaluationId,
      evaluationForm.value.qualityRating,
      evaluationForm.value.logisticsRating,
      evaluationForm.value.comment,
      evaluationForm.value.attachments
    );
    editingEvaluation.value = undefined;
    await load();
    notice.value = "伙食供应评价已提交";
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "评价提交失败";
  } finally {
    loading.value = false;
  }
}

watch([() => props.mode, isSupplier], load, { immediate: true });
</script>

<template>
  <section class="food-page">
    <ExpandablePanel :show-header="false" :class="['inquiry-management-panel', mode === 'settlement' ? 'settlement-management-panel' : 'evaluation-management-panel']">
      <section :class="['filter-toolbar', { 'evaluation-filter-toolbar': mode === 'evaluation' }]" :aria-label="mode === 'settlement' ? '伙食结算筛选' : '伙食评价筛选'">
        <div class="filter-fields">
          <label class="filter-field filter-field--search"><span>检索</span><input v-model="keyword" placeholder="订单号、供应商检索" @keydown.enter.prevent="load" /></label>
          <label class="filter-field"><span>状态</span><select v-model="status"><option value="">全部</option><option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
        </div>
        <div class="toolbar-icon-actions"><IconButton icon="Search" label="查询" @click="load" /><IconButton icon="X" label="重置" @click="reset" /><IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="load" /></div>
      </section>

      <div v-if="error" class="inline-error">{{ error }}</div>
      <div v-if="notice" class="permission-static-notice">{{ notice }}</div>
      <div v-if="loading" class="empty-state compact">加载中</div>
      <div v-else-if="mode === 'settlement' && !visibleSettlements.length" class="empty-state compact">暂无伙食结算单</div>
      <div v-else-if="mode === 'evaluation' && !visibleEvaluations.length" class="empty-state compact">暂无伙食评价任务</div>

      <section v-else-if="mode === 'settlement'" :class="['settlement-card-list', isSupplier ? 'is-provider' : 'is-buyer']">
        <article v-for="row in visibleSettlements" :key="row.settlementId" :class="['settlement-card', isSupplier ? 'is-provider' : 'is-buyer']">
          <header>
            <div><strong class="settlement-card__no"><span class="settlement-money-icon" aria-hidden="true"></span>FS-{{ row.settlementId }}</strong></div>
            <small class="settlement-card__purchase-no">{{ row.orderNo || '-' }}</small>
            <div class="settlement-card__status-type"><StatusBadge :label="settlementLabel(row.status)" :variant="badgeVariant(row.status)" /></div>
          </header>
          <div class="settlement-card__content-row">
            <dl>
              <div><dt>{{ isSupplier ? '船代公司' : '服务商' }}</dt><dd>{{ isSupplier ? (row.buyerCompanyName || '-') : row.supplierName }}</dd></div>
              <div><dt>船舶名称</dt><dd>{{ row.vesselName || '-' }}</dd></div>
              <div><dt>报价金额</dt><dd>{{ row.amount.toFixed(2) }} {{ row.currency || '' }}</dd></div>
              <div><dt>实际金额</dt><dd>{{ row.actualAmount == null ? '--' : `${row.actualAmount.toFixed(2)} ${row.currency || ''}` }}</dd></div>
            </dl>
            <div class="icon-action-row settlement-card__inline-actions">
              <IconButton v-if="isSupplier && row.status === 'PENDING_INVOICE'" icon="Pencil" label="编辑结算" @click.stop="openSettlement(row, true)" />
              <IconButton v-if="!isSupplier && row.status === 'INVOICED'" icon="Check" label="确认已结算" variant="primary" @click.stop="settlementAction(row)" />
              <IconButton icon="Eye" label="查看结算" @click.stop="openSettlement(row)" />
              <IconButton v-if="!isSupplier && row.status === 'SETTLED'" icon="Check" label="付款" variant="primary" @click.stop="settlementAction(row)" />
            </div>
          </div>
        </article>
      </section>

      <section v-else class="evaluation-card-list">
        <article v-for="row in visibleEvaluations" :key="row.evaluationId" class="evaluation-card">
          <header><div><strong>{{ row.supplierName }}</strong></div><StatusBadge :label="evaluationLabel(row.status)" :variant="badgeVariant(row.status)" /></header>
          <div class="evaluation-card__order"><span>采购单</span><strong>{{ row.orderNo }}</strong></div>
          <div class="evaluation-score-rows">
            <div class="evaluation-score-line"><span>品质</span><div class="evaluation-star-display" :aria-label="`品质${row.qualityRating || 0}分`"><span v-for="point in 5" :key="point" :class="{ active: point <= Number(row.qualityRating || 0) }">★</span></div></div>
            <div class="evaluation-score-line"><span>物流</span><div class="evaluation-star-display" :aria-label="`物流${row.logisticsRating || 0}分`"><span v-for="point in 5" :key="point" :class="{ active: point <= Number(row.logisticsRating || 0) }">★</span></div></div>
          </div>
          <dl class="evaluation-card__content"><div><dd>{{ row.comment || '尚未填写评价内容' }}</dd></div></dl>
          <footer>
            <span>附件证明：{{ row.attachments?.length || 0 }} 个</span>
            <div class="evaluation-card__actions">
              <IconButton v-if="['PENDING_EVALUATION', 'REJECTED'].includes(row.status)" icon="Pencil" label="填写评价" @click="openEvaluation(row)" />
              <IconButton v-if="row.status === 'PENDING_REVIEW'" icon="Eye" label="查看明细" @click="openEvaluation(row)" />
              <IconButton v-if="row.attachments?.length" icon="Image" label="查看证明" @click="previewEvaluationAttachment(row.attachments[0])" />
            </div>
          </footer>
        </article>
      </section>
    </ExpandablePanel>

    <FoodSettlementDrawer
      :open="settlementEditorOpen"
      :settlement="activeSettlement"
      :editable="settlementEditorEditable"
      :saving="loading"
      @close="settlementEditorOpen = false"
      @submit="submitSettlementInvoice"
    />

    <DetailDrawer :open="Boolean(editingEvaluation)" title="服务评价" width="compact" @close="editingEvaluation = undefined">
      <section v-if="editingEvaluation" class="evaluation-editor">
        <header><strong>{{ editingEvaluation.supplierName }}</strong><span>{{ editingEvaluation.orderNo }} · 伙食采购</span></header>
        <div class="evaluation-editor-ratings">
          <label>
            <span>品质</span>
            <div class="evaluation-star-picker" role="radiogroup" aria-label="品质评分">
              <button v-for="point in 5" :key="point" type="button" :class="{ active: point <= evaluationForm.qualityRating }" :disabled="evaluationEditorReadonly" :aria-label="`品质${point}分`" @click="evaluationForm.qualityRating = point">★</button>
            </div>
          </label>
          <label>
            <span>物流</span>
            <div class="evaluation-star-picker" role="radiogroup" aria-label="物流评分">
              <button v-for="point in 5" :key="point" type="button" :class="{ active: point <= evaluationForm.logisticsRating }" :disabled="evaluationEditorReadonly" :aria-label="`物流${point}分`" @click="evaluationForm.logisticsRating = point">★</button>
            </div>
          </label>
        </div>
        <label><span>评价内容</span><textarea v-model="evaluationForm.comment" rows="4" :readonly="evaluationEditorReadonly" placeholder="填写本次服务的真实评价"></textarea></label>
        <section class="evaluation-proof-section">
          <div class="evaluation-proof-heading">
            <span>附件上传</span>
            <label v-if="!evaluationEditorReadonly" class="evaluation-proof-upload" title="上传评价证明" aria-label="上传评价证明">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 15V4m0 0 4 4m-4-4-4 4M4 15v4h16v-4" /></svg>
              <input type="file" accept="image/*,.pdf" :disabled="evaluationUploading" @change="uploadEvaluationFile" />
            </label>
          </div>
          <div class="evaluation-proof-row">
            <button v-for="file in evaluationForm.attachments" :key="file.fileId || file.fileName" type="button" class="evaluation-proof-file" :disabled="evaluationPreviewing === (file.fileId || file.fileName)" @click="previewEvaluationAttachment(file)">{{ file.fileName }}</button>
            <span v-if="!evaluationForm.attachments.length" class="evaluation-proof-empty">暂无附件</span>
          </div>
        </section>
      </section>
      <template #footer>
        <IconButton icon="X" label="关闭" @click="editingEvaluation = undefined" />
        <IconButton v-if="!evaluationEditorReadonly" icon="Send" label="提交评价" variant="primary" :loading="loading" @click="submitEvaluation" />
      </template>
    </DetailDrawer>
  </section>
</template>
