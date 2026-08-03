<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import IconButton from "@/components/IconButton.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import { listPublicDictionaryItems } from "@/services/dataDictionaryService";
import FoodImportProgressOverlay from "../components/FoodImportProgressOverlay.vue";
import { getFoodDemand, previewFoodDemand, saveFoodDemand } from "../services/foodProcurementApi";
import type { FoodDemandItem, FoodDemandSummary, FoodMatchPreview } from "../types";

const route = useRoute();
const router = useRouter();
const fileInput = ref<HTMLInputElement>();
const loading = ref(false);
const importing = ref(false);
const saving = ref(false);
const sending = ref(false);
const submitAttempted = ref(false);
const error = ref("");
const notice = ref("");
const preview = ref<FoodMatchPreview>();
const query = ref("");
const selectedFile = ref("");
const importFileName = ref("");
const importOverlayVisible = ref(false);
const importProgress = ref(0);
const importStageIndex = ref(0);
const importState = ref<"running" | "success" | "failed">("running");
const importResult = ref<FoodMatchPreview>();
let importProgressTimer: number | undefined;
const demandId = ref<number>();
const demandSummary = ref<FoodDemandSummary>();
const form = ref({ inquiryNo: "", vesselName: "", supplyPort: "", vesselEta: "", quoteDeadlineDate: "", currency: "USD" });

const fallbackUnitOptions = ["KG", "PKT", "DRUM", "BTL", "BOX", "CASE", "PC", "PCS", "TIN", "BAG", "CTN"];
const unitOptions = ref<string[]>(fallbackUnitOptions);

const rows = computed(() => (preview.value?.items || []).filter((item) => {
  const text = `${item.nameEn || ""} ${item.nameZh || ""} ${item.specification || ""} ${item.unit || ""}`.toLowerCase();
  return !query.value.trim() || text.includes(query.value.trim().toLowerCase());
}));
const canCompare = computed(() => Boolean(demandSummary.value?.demandId && demandSummary.value.submittedQuoteCount > 0));
const deadlineInvalid = computed(() => submitAttempted.value && !form.value.quoteDeadlineDate);

function openComparison() {
  if (!canCompare.value || !demandSummary.value) return;
  void router.push(`/food/comparison/${demandSummary.value.demandId}`);
}

function openFile() {
  if (loading.value || importing.value) return;
  fileInput.value?.click();
}

function clearImportProgressTimer() {
  if (importProgressTimer !== undefined) {
    window.clearTimeout(importProgressTimer);
    importProgressTimer = undefined;
  }
}

function startImportProgress() {
  clearImportProgressTimer();
  importOverlayVisible.value = true;
  importProgress.value = 8;
  importStageIndex.value = 0;
  importState.value = "running";
  importResult.value = undefined;

  const steps = [
    { percent: 30, stage: 0 },
    { percent: 62, stage: 1 },
    { percent: 88, stage: 2 }
  ];
  let index = 0;
  const runStep = () => {
    const step = steps[index];
    if (!step || importState.value !== "running") return;
    importProgress.value = step.percent;
    importStageIndex.value = step.stage;
    index += 1;
    importProgressTimer = window.setTimeout(runStep, 520);
  };
  importProgressTimer = window.setTimeout(runStep, 450);
}

function delay(ms: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, ms));
}

async function upload(file?: File) {
  if (!file || loading.value || importing.value) return;
  importing.value = true;
  importFileName.value = file.name;
  error.value = "";
  notice.value = "";
  startImportProgress();
  try {
    preview.value = await previewFoodDemand(file);
    selectedFile.value = file.name;
    importResult.value = preview.value;
    clearImportProgressTimer();
    importStageIndex.value = 2;
    importProgress.value = 100;
    importState.value = "success";
    await delay(340);
  } catch (reason) {
    clearImportProgressTimer();
    importState.value = "failed";
    error.value = reason instanceof Error ? reason.message : "询价单识别失败";
    await delay(600);
  } finally {
    importOverlayVisible.value = false;
    importing.value = false;
    if (fileInput.value) fileInput.value.value = "";
  }
}

function removeRow(index: number) {
  if (!preview.value) return;
  const target = rows.value[index];
  preview.value.items = preview.value.items.filter((item) => item !== target);
  preview.value.totalRows = preview.value.items.length;
}

function addRow() {
  if (!preview.value) {
    preview.value = { fileName: "manual", selectedSheet: "manual", headerRow: 0, totalRows: 0, matchedCount: 0, pendingCount: 0, sheets: [], items: [] };
  }
  const sequenceNo = preview.value.items.length + 1;
  preview.value.items.push({ sequenceNo, nameEn: "", nameZh: "", specification: "", unit: unitOptions.value[0] || "", requestedQuantity: 1, matchStatus: "MATCHED", rawColumns: {} });
  preview.value.totalRows = preview.value.items.length;
}

function payloadItems(): FoodDemandItem[] {
  return (preview.value?.items || []).map((item, index) => ({
    ...item,
    sourceRow: item.sourceRow || index + 1,
    sequenceNo: item.sequenceNo || index + 1,
    requestedQuantity: Number(item.requestedQuantity),
    matchStatus: item.matchStatus || "MATCHED",
    rawColumns: item.rawColumns || {}
  }));
}

async function saveDemandDraft() {
  submitAttempted.value = true;
  error.value = "";
  if (!form.value.vesselName.trim() || !form.value.supplyPort.trim() || !form.value.vesselEta || !form.value.quoteDeadlineDate || !preview.value?.items.length) {
    error.value = "请完整填写船舶、补给港、预计到港时间、询价截止日期并导入伙食清单";
    return null;
  }
  const invalid = preview.value.items.some((item) =>
    (!item.nameEn?.trim() && !item.nameZh?.trim()) || !item.unit?.trim() || Number(item.requestedQuantity) <= 0
  );
  if (invalid) {
    error.value = "清单中存在名称、单位或数量不完整的行";
    return null;
  }
  saving.value = true;
  try {
    const result = await saveFoodDemand({
      demandId: demandId.value,
      vesselName: form.value.vesselName,
      supplyPort: form.value.supplyPort,
      vesselEta: form.value.vesselEta,
      quoteDeadlineAt: `${form.value.quoteDeadlineDate}T23:59:59`,
      currency: form.value.currency,
      inquiryNo: form.value.inquiryNo || undefined,
      sourceFileName: selectedFile.value || preview.value.fileName,
      sourceSheetName: preview.value.selectedSheet,
      items: payloadItems()
    });
    demandId.value = result.demandId;
    if (result.inquiryNo) form.value.inquiryNo = result.inquiryNo;
    demandSummary.value = undefined;
    notice.value = `询价单 ${result.inquiryNo || result.demandNo} 已生成，已发送给 ${result.supplierCount} 家供货商`;
    return result;
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "保存失败";
    return null;
  } finally {
    saving.value = false;
  }
}

async function save() {
  await sendQuoteToAllSuppliers();
}

async function sendQuoteToAllSuppliers() {
  sending.value = true;
  const result = await saveDemandDraft();
  if (result) await router.push("/food/inquiries");
  sending.value = false;
}

async function loadUnitOptions() {
  try {
    const items = await listPublicDictionaryItems("UNIT");
    const values = items
      .map((item) => item.itemValue || item.itemName || item.itemCode)
      .map((value) => value.trim())
      .filter(Boolean);
    unitOptions.value = values.length ? Array.from(new Set(values)) : fallbackUnitOptions;
  } catch {
    unitOptions.value = fallbackUnitOptions;
  }
}

async function loadDetail(id: number) {
  loading.value = true;
  try {
    const detail = await getFoodDemand(id);
    demandId.value = detail.demand.demandId;
    demandSummary.value = detail.demand;
    form.value = {
      inquiryNo: detail.demand.inquiryNo || "",
      vesselName: detail.demand.vesselName,
      supplyPort: detail.demand.supplyPort,
      vesselEta: detail.demand.vesselEta,
      quoteDeadlineDate: detail.demand.quoteDeadlineAt?.slice(0, 10) || "",
      currency: detail.demand.currency
    };
    selectedFile.value = detail.sourceFileName || "";
    preview.value = {
      fileName: detail.sourceFileName || "",
      selectedSheet: detail.sourceSheetName || "",
      headerRow: 0,
      totalRows: detail.items.length,
      matchedCount: detail.demand.matchedCount,
      pendingCount: detail.demand.pendingCount,
      sheets: [],
      items: detail.items
    };
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "读取需求失败";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadUnitOptions();
});

watch(() => route.params.demandId, (value) => {
  const id = Number(value);
  if (Number.isFinite(id) && id > 0) void loadDetail(id);
}, { immediate: true });

onBeforeUnmount(clearImportProgressTimer);
</script>

<template>
  <section class="food-page food-demand-page food-material-stage">
    <header class="food-command-head">
      <div class="food-supply-upload-row">
        <button class="food-drop-zone" type="button" :disabled="loading || importing" @click="openFile" @dragover.prevent @drop.prevent="upload($event.dataTransfer?.files[0])">
          <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="upload(($event.target as HTMLInputElement).files?.[0])" />
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 15V4m0 0 4 4m-4-4-4 4M4 15v4h16v-4" /></svg>
          <span><strong>{{ importing ? "正在导入" : "点击上传或拖拽文件到此处" }}</strong><small>{{ selectedFile || "支持 xlsx/xls 文件，上传后即时生成预览。" }}</small></span>
        </button>
      </div>
      <div class="food-supply-strip">
        <div class="food-supply-item"><label><span><i>*</i>船舶名称</span><input v-model="form.vesselName" placeholder="请输入船舶名称" /></label></div>
        <div class="food-supply-item"><label><span>询价单号</span><input :value="form.inquiryNo" readonly placeholder="保存后自动生成" /></label></div>
        <div class="food-supply-item"><label><span><i>*</i>补给港口</span><input v-model="form.supplyPort" placeholder="请输入补给港口" /></label></div>
        <div class="food-supply-item"><label><span>币种</span><select v-model="form.currency"><option>USD</option><option>CNY</option></select></label></div>
        <div class="food-supply-item"><label><span><i>*</i>预计到港时间</span><StableDateTimeInput v-model="form.vesselEta" mode="datetime" /></label></div>
      </div>
    </header>

    <p v-if="error" class="food-alert is-error">{{ error }}</p>
    <p v-if="notice" class="food-alert is-success">{{ notice }}</p>

    <section class="food-panel food-inquiry-summary-card" aria-label="询价概览">
      <div class="food-inquiry-summary-stats">
        <label class="food-inquiry-summary-item food-inquiry-deadline">
          <span><i>*</i>询价截止日期</span>
          <StableDateTimeInput v-model="form.quoteDeadlineDate" mode="date" placeholder="请选择询价截止日期" :invalid="deadlineInvalid" />
        </label>
        <div class="food-inquiry-summary-item">
          <span>询价供货商</span>
          <strong>{{ demandSummary?.supplierCount || 0 }} 家</strong>
        </div>
        <div class="food-inquiry-summary-item">
          <span>报价供货商</span>
          <strong>{{ demandSummary?.submittedQuoteCount || 0 }} 家</strong>
        </div>
      </div>
      <IconButton icon="Search" glyph="比" label="比价" variant="primary" :disabled="!canCompare" @click="openComparison" />
    </section>

    <section class="food-panel food-match-panel">
      <header class="food-panel-head">
        <div><h2>伙食明细</h2><p>按名称、规格、单位生成询价明细。</p></div>
        <div class="toolbar-icon-actions">
          <IconButton icon="Plus" label="新增伙食明细" @click="addRow" />
          <IconButton icon="Send" glyph="报" label="一键发起报价单" variant="primary" :loading="sending" @click="sendQuoteToAllSuppliers" />
          <IconButton icon="Save" label="保存并发起询价" variant="primary" :loading="saving || sending" @click="save" />
        </div>
      </header>
      <div class="food-match-tools">
        <label class="food-search"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg><input v-model="query" placeholder="搜索名称、规格、单位" /></label>
      </div>
      <div class="food-grid-table food-demand-grid">
        <div class="food-grid-head"><span>序号</span><span>英文名称</span><span>中文名称</span><span>规格</span><span>单位</span><span>数量</span><span>操作</span></div>
        <div v-for="(row, index) in rows" :key="`${row.sourceRow}-${index}`" class="food-grid-row">
          <span>{{ row.sequenceNo }}</span>
          <input v-model="row.nameEn" placeholder="English name" />
          <input v-model="row.nameZh" placeholder="中文名称" />
          <input v-model="row.specification" placeholder="规格" />
          <select v-model="row.unit">
            <option value="">请选择</option>
            <option v-for="unit in unitOptions" :key="unit" :value="unit">{{ unit }}</option>
          </select>
          <input v-model.number="row.requestedQuantity" type="number" min="0.0001" step="0.0001" />
          <IconButton icon="Trash2" label="删除明细" variant="danger" @click="removeRow(index)" />
        </div>
        <div v-if="!rows.length" class="food-empty">导入询价清单或手动新增伙食明细</div>
      </div>
    </section>

    <FoodImportProgressOverlay
      :visible="importOverlayVisible"
      :file-name="importFileName"
      :percent="importProgress"
      :stage-index="importStageIndex"
      :state="importState"
      :total-rows="importResult?.totalRows"
    />
  </section>
</template>
