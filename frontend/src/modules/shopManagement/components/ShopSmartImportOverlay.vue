<script setup lang="ts">
import { computed } from "vue";
import BackgroundTaskOverlay from "@/modules/backgroundProcessing/components/BackgroundTaskOverlay.vue";
import type { BackgroundTaskCounter, BackgroundTaskStatus } from "@/modules/backgroundProcessing/types/backgroundTask";
import {
  SHOP_SMART_IMPORT_ANALYSIS_STAGES,
  SHOP_SMART_IMPORT_EXECUTION_STAGES,
  isShopSmartImportTerminal,
  type ShopSmartImportJob
} from "../types/shopSmartImport";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    job: ShopSmartImportJob;
    elapsedSeconds: number;
    allowBackground?: boolean;
  }>(),
  { allowBackground: true }
);

defineEmits<{
  background: [];
  retry: [];
  close: [];
  "view-preview": [];
  "download-errors": [];
}>();

const stages = computed(() => (props.job.phase === "EXECUTION" ? SHOP_SMART_IMPORT_EXECUTION_STAGES : SHOP_SMART_IMPORT_ANALYSIS_STAGES));
const terminal = computed(() => isShopSmartImportTerminal(props.job.status));
const failed = computed(() => props.job.status === "FAILED");
const partial = computed(() => props.job.status === "PARTIAL");
const overlayStatus = computed<BackgroundTaskStatus>(() => {
  if (failed.value) return "FAILED";
  if (partial.value) return "PARTIAL";
  if (props.job.status === "COMPLETED") return "COMPLETED";
  return "RUNNING";
});
const title = computed(() => {
  if (failed.value) return props.job.phase === "EXECUTION" ? "商品入库未完成" : "商品文件分析未完成";
  if (partial.value) return props.job.phase === "EXECUTION" ? "部分商品已入库" : "商品分析已完成，部分待确认";
  if (props.job.status === "COMPLETED") return props.job.phase === "EXECUTION" ? "商品入库完成" : "商品智能分析完成";
  return props.job.phase === "EXECUTION" ? "正在写入商品" : "正在分析商品文件";
});
const contextItems = computed(() => {
  const items: string[] = [];
  if (props.job.currentSheet) items.push(`当前 Sheet：${props.job.currentSheet}`);
  if (props.job.totalChunks) items.push(`分片 ${props.job.currentChunk || 0} / ${props.job.totalChunks}`);
  if (props.job.counts.sheetTotal) items.push(`工作表 ${props.job.counts.sheetProcessed} / ${props.job.counts.sheetTotal}`);
  if (props.job.counts.imageTotal) items.push(`图片 ${props.job.counts.imageProcessed} / ${props.job.counts.imageTotal}`);
  return items;
});
const matchedCount = computed(() => props.job.counts.impaMatchedCount + props.job.counts.categoryMatchedCount);
const liveMessage = computed(() => {
  if (!terminal.value || failed.value) return props.job.message || "正在等待服务端进度";
  if (partial.value) {
    return props.job.phase === "EXECUTION"
      ? "已入库可确认商品，待确认商品保留在导入结果中"
      : "导入预览已生成，部分商品待人工确认";
  }
  return props.job.phase === "EXECUTION" ? "商品入库与数量核对完成" : "已生成可追溯导入预览";
});
const counters = computed<BackgroundTaskCounter[]>(() => [
  { label: "已处理商品", value: `${props.job.counts.itemProcessed} / ${props.job.counts.itemTotal}` },
  { label: "类型识别", value: `物料 ${props.job.counts.materialCount} / 伙食 ${props.job.counts.foodCount}` },
  { label: "匹配 / 待确认 / 失败", value: `${matchedCount.value} / ${props.job.counts.pendingReviewCount} / ${props.job.counts.failedCount}` }
]);
const viewActionLabel = computed(() => {
  if (props.job.phase !== "EXECUTION") return "查看导入预览";
  return partial.value ? "查看导入结果" : "查看商品列表";
});
</script>

<template>
  <BackgroundTaskOverlay
    :visible="visible"
    kicker="商品智能导入"
    :title="title"
    :file-name="job.fileName"
    :status="overlayStatus"
    :stages="stages"
    :stage-index="job.stageIndex"
    :progress-percent="job.overallPercent"
    :elapsed-seconds="elapsedSeconds"
    :message="liveMessage"
    :context-items="contextItems"
    :counters="counters"
    :error-code="job.errorCode"
    :allow-background="!terminal && allowBackground"
    :retryable="failed && job.retryable"
    :download-errors="(failed || partial) && job.counts.failedCount > 0"
    :primary-action-label="terminal && !failed ? viewActionLabel : ''"
    close-label="关闭导入进度"
    legacy-prefix="shop-smart-import"
    @background="$emit('background')"
    @retry="$emit('retry')"
    @close="$emit('close')"
    @primary="$emit('view-preview')"
    @download-errors="$emit('download-errors')"
  />
</template>
