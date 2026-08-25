import { computed, getCurrentScope, onScopeDispose, ref } from "vue";
import {
  clearBackgroundTaskRecovery,
  readBackgroundTaskRecovery,
  writeBackgroundTaskRecovery
} from "@/modules/backgroundProcessing/services/backgroundTaskRecovery";
import { executeShopSmartImport, getShopSmartImportJob, startShopSmartImport } from "../services/shopSmartImportService";
import {
  EMPTY_SHOP_SMART_IMPORT_COUNTS,
  EMPTY_SHOP_SMART_IMPORT_JOB,
  SHOP_SMART_IMPORT_ANALYSIS_STAGES,
  SHOP_SMART_IMPORT_EXECUTION_STAGES,
  isShopSmartImportTerminal,
  type ShopSmartImportCounts,
  type ShopSmartImportJob,
  type ShopSmartImportPhase,
  type ShopSmartImportStage,
  type ShopSmartImportStatus
} from "../types/shopSmartImport";

type SmartImportControllerOptions = {
  pollIntervalMs?: number;
};

const SHOP_SMART_IMPORT_RECOVERY_KEY = "ship-supply:shop-smart-import-task";

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function unwrap(value: unknown): Record<string, unknown> {
  if (!isRecord(value)) return {};
  if (isRecord(value.data)) return value.data;
  return value;
}

function readString(source: Record<string, unknown>, ...keys: string[]): string {
  for (const key of keys) {
    const value = source[key];
    if (value !== undefined && value !== null && String(value).trim()) return String(value).trim();
  }
  return "";
}

function readNumber(source: Record<string, unknown>, ...keys: string[]): number {
  for (const key of keys) {
    const value = Number(source[key]);
    if (Number.isFinite(value)) return value;
  }
  return 0;
}

function normalizeStatus(value: string, fallback: ShopSmartImportStatus): ShopSmartImportStatus {
  const status = value.toUpperCase();
  if (["IDLE", "UPLOADING", "QUEUED", "RUNNING", "PARTIAL", "COMPLETED", "FAILED"].includes(status)) {
    return status as ShopSmartImportStatus;
  }
  if (status === "PREVIEW_READY") return "COMPLETED";
  if (["SUCCESS", "SUCCEEDED", "FINISHED", "DONE"].includes(status)) return "COMPLETED";
  if (["ERROR", "FAILURE"].includes(status)) return "FAILED";
  return fallback;
}

function normalizePhase(value: string, fallback: ShopSmartImportPhase): ShopSmartImportPhase {
  return value.toUpperCase() === "EXECUTION" ? "EXECUTION" : value ? "ANALYSIS" : fallback;
}

function normalizeStage(value: string, phase: ShopSmartImportPhase, fallback: ShopSmartImportStage): ShopSmartImportStage {
  const aliases: Record<string, ShopSmartImportStage> = {
    VALIDATE_SELECTION: "SKU_CREATE",
    PERSIST_PRODUCTS: "SKU_CREATE",
    LINK_IMAGES: "DETAIL_IMAGE_SAVE",
    IMPORT_COMPLETE: "COUNT_RECONCILIATION"
  };
  const stage = aliases[value.toUpperCase()] || value.toUpperCase();
  const stages = phase === "EXECUTION" ? SHOP_SMART_IMPORT_EXECUTION_STAGES : SHOP_SMART_IMPORT_ANALYSIS_STAGES;
  return stages.some((item) => item.code === stage) ? (stage as ShopSmartImportStage) : fallback;
}

function normalizeCounts(value: unknown, fallback: ShopSmartImportCounts): ShopSmartImportCounts {
  const source = isRecord(value) ? value : {};
  return {
    sheetTotal: readNumber(source, "sheetTotal", "totalSheets") || fallback.sheetTotal,
    sheetProcessed: readNumber(source, "sheetProcessed", "processedSheets") || fallback.sheetProcessed,
    itemTotal: readNumber(source, "itemTotal", "totalItems", "totalCount") || fallback.itemTotal,
    itemProcessed: readNumber(source, "itemProcessed", "processedItems", "processedCount") || fallback.itemProcessed,
    materialCount: readNumber(source, "materialCount") || fallback.materialCount,
    foodCount: readNumber(source, "foodCount") || fallback.foodCount,
    impaMatchedCount: readNumber(source, "impaMatchedCount") || fallback.impaMatchedCount,
    categoryMatchedCount: readNumber(source, "categoryMatchedCount") || fallback.categoryMatchedCount,
    pendingReviewCount: readNumber(source, "pendingReviewCount", "exceptionCount") || fallback.pendingReviewCount,
    imageTotal: readNumber(source, "imageTotal", "totalImages") || fallback.imageTotal,
    imageProcessed: readNumber(source, "imageProcessed", "processedImages") || fallback.imageProcessed,
    failedCount: readNumber(source, "failedCount") || fallback.failedCount
  };
}

export function normalizeShopSmartImportJob(value: unknown, fallback: ShopSmartImportJob = EMPTY_SHOP_SMART_IMPORT_JOB): ShopSmartImportJob {
  const source = unwrap(value);
  const phase = normalizePhase(readString(source, "phase"), fallback.phase);
  const stage = normalizeStage(readString(source, "stage", "currentStage"), phase, fallback.stage);
  const stages = phase === "EXECUTION" ? SHOP_SMART_IMPORT_EXECUTION_STAGES : SHOP_SMART_IMPORT_ANALYSIS_STAGES;
  const stageByCode = stages.findIndex((item) => item.code === stage);
  const rawPercent = readNumber(source, "overallPercent", "progressPercent", "progress");
  return {
    ...fallback,
    jobId: readString(source, "jobId", "id") || fallback.jobId,
    batchId: readString(source, "batchId", "importBatchId") || fallback.batchId,
    fileName: readString(source, "fileName", "sourceFileName") || fallback.fileName,
    phase,
    status: normalizeStatus(readString(source, "status", "jobStatus"), fallback.status),
    stage,
    stageIndex: stageByCode >= 0 ? stageByCode : Math.max(0, readNumber(source, "stageIndex") - 1),
    stageCount: readNumber(source, "stageCount") || stages.length,
    overallPercent: Math.min(100, Math.max(0, rawPercent || fallback.overallPercent)),
    stageProcessed: readNumber(source, "stageProcessed", "processed") || fallback.stageProcessed,
    stageTotal: readNumber(source, "stageTotal", "total") || fallback.stageTotal,
    currentSheet: readString(source, "currentSheet", "sheetName") || fallback.currentSheet,
    currentChunk: readNumber(source, "currentChunk", "chunkIndex") || fallback.currentChunk,
    totalChunks: readNumber(source, "totalChunks", "chunkTotal") || fallback.totalChunks,
    message: readString(source, "message", "progressMessage") || fallback.message,
    errorCode: readString(source, "errorCode", "code") || fallback.errorCode,
    retryable: typeof source.retryable === "boolean" ? source.retryable : fallback.retryable,
    counts: normalizeCounts(source.counts ?? source.statistics ?? source, fallback.counts),
    result: source.result ?? source.preview ?? source.payload ?? (Array.isArray(source.items) ? source : fallback.result)
  };
}

export function useShopSmartImportJob(options: SmartImportControllerOptions = {}) {
  const pollIntervalMs = options.pollIntervalMs ?? 1200;
  const visible = ref(false);
  const job = ref<ShopSmartImportJob>({ ...EMPTY_SHOP_SMART_IMPORT_JOB, counts: { ...EMPTY_SHOP_SMART_IMPORT_COUNTS } });
  const elapsedSeconds = ref(0);
  const lastFile = ref<File | null>(null);
  let elapsedTimer: number | undefined;
  let pollTimer: number | undefined;
  let activeRun = 0;
  let startedAt = 0;

  const isTerminal = computed(() => isShopSmartImportTerminal(job.value.status));
  const isRunning = computed(() => ["UPLOADING", "QUEUED", "RUNNING"].includes(job.value.status));

  function stopElapsedTimer(): void {
    if (elapsedTimer !== undefined) window.clearInterval(elapsedTimer);
    elapsedTimer = undefined;
  }

  function startElapsedTimer(initialStartedAt = Date.now()): void {
    stopElapsedTimer();
    startedAt = initialStartedAt;
    elapsedSeconds.value = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
    elapsedTimer = window.setInterval(() => {
      elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000);
    }, 1000);
  }

  function stopPolling(): void {
    activeRun += 1;
    if (pollTimer !== undefined) window.clearTimeout(pollTimer);
    pollTimer = undefined;
  }

  function waitForPoll(run: number): Promise<boolean> {
    return new Promise((resolve) => {
      pollTimer = window.setTimeout(() => {
        pollTimer = undefined;
        resolve(run === activeRun);
      }, pollIntervalMs);
    });
  }

  function applyServerJob(payload: unknown): ShopSmartImportJob {
    job.value = normalizeShopSmartImportJob(payload, job.value);
    if (job.value.jobId) {
      writeBackgroundTaskRecovery(SHOP_SMART_IMPORT_RECOVERY_KEY, {
        jobId: job.value.jobId,
        fileName: job.value.fileName,
        startedAt: startedAt || Date.now()
      });
    }
    if (isShopSmartImportTerminal(job.value.status)) stopElapsedTimer();
    return job.value;
  }

  async function pollUntilTerminal(run: number): Promise<ShopSmartImportJob> {
    while (run === activeRun && !isShopSmartImportTerminal(job.value.status)) {
      if (!(await waitForPoll(run))) break;
      const payload = await getShopSmartImportJob(job.value.jobId);
      if (run !== activeRun) break;
      applyServerJob(payload);
    }
    return job.value;
  }

  async function restorePersistedTask(): Promise<ShopSmartImportJob | null> {
    const snapshot = readBackgroundTaskRecovery(SHOP_SMART_IMPORT_RECOVERY_KEY);
    if (!snapshot) return null;
    const run = activeRun;
    visible.value = true;
    job.value = {
      ...EMPTY_SHOP_SMART_IMPORT_JOB,
      jobId: snapshot.jobId,
      fileName: snapshot.fileName,
      status: "QUEUED",
      message: "正在恢复后台任务进度",
      counts: { ...EMPTY_SHOP_SMART_IMPORT_COUNTS }
    };
    startElapsedTimer(snapshot.startedAt);
    try {
      const payload = await getShopSmartImportJob(snapshot.jobId);
      if (run !== activeRun) return job.value;
      applyServerJob(payload);
      if (!isShopSmartImportTerminal(job.value.status)) await pollUntilTerminal(run);
      return job.value;
    } catch (error) {
      if (run !== activeRun) return job.value;
      stopElapsedTimer();
      job.value = {
        ...job.value,
        status: "FAILED",
        message: error instanceof Error && error.message ? error.message : "后台任务恢复失败",
        retryable: false
      };
      return job.value;
    }
  }

  async function startAnalysis(file: File): Promise<ShopSmartImportJob> {
    stopPolling();
    const run = activeRun;
    lastFile.value = file;
    visible.value = true;
    job.value = {
      ...EMPTY_SHOP_SMART_IMPORT_JOB,
      fileName: file.name,
      status: "UPLOADING",
      message: "正在上传文件",
      counts: { ...EMPTY_SHOP_SMART_IMPORT_COUNTS }
    };
    startElapsedTimer();

    try {
      applyServerJob(await startShopSmartImport(file));
      if (!job.value.jobId || isShopSmartImportTerminal(job.value.status)) return job.value;
      return pollUntilTerminal(run);
    } catch (error) {
      stopElapsedTimer();
      job.value = {
        ...job.value,
        status: "FAILED",
        message: error instanceof Error && error.message ? error.message : "商品文件分析失败",
        retryable: true
      };
      throw error;
    }
  }

  async function execute(previewRowIds: Array<string | number>): Promise<ShopSmartImportJob> {
    if (!job.value.jobId) throw new Error("INTELLIGENT_IMPORT_JOB_ID_MISSING");
    stopPolling();
    const run = activeRun;
    job.value = {
      ...job.value,
      phase: "EXECUTION",
      status: "QUEUED",
      stage: "SKU_CREATE",
      stageIndex: 0,
      stageCount: SHOP_SMART_IMPORT_EXECUTION_STAGES.length,
      overallPercent: 0,
      message: "正在准备创建商品记录",
      errorCode: "",
      retryable: false
    };
    visible.value = true;
    startElapsedTimer();
    try {
      applyServerJob(await executeShopSmartImport(job.value.jobId, previewRowIds));
      return pollUntilTerminal(run);
    } catch (error) {
      stopElapsedTimer();
      job.value = {
        ...job.value,
        status: "FAILED",
        message: error instanceof Error && error.message ? error.message : "商品入库任务失败",
        retryable: true
      };
      throw error;
    }
  }

  function beginExecution(fileName: string, counts: Partial<ShopSmartImportCounts> = {}): void {
    stopPolling();
    job.value = {
      ...job.value,
      fileName: fileName || job.value.fileName,
      phase: "EXECUTION",
      status: "RUNNING",
      stage: "SKU_CREATE",
      stageIndex: 0,
      stageCount: SHOP_SMART_IMPORT_EXECUTION_STAGES.length,
      overallPercent: 0,
      message: "正在校验已确认的商品数据",
      errorCode: "",
      retryable: false,
      counts: { ...job.value.counts, ...counts }
    };
    visible.value = true;
    startElapsedTimer();
  }

  function finishExecution(status: "COMPLETED" | "PARTIAL" | "FAILED", options: { message?: string; counts?: Partial<ShopSmartImportCounts>; errorCode?: string } = {}): void {
    job.value = {
      ...job.value,
      status,
      stage: status === "FAILED" ? job.value.stage : "COUNT_RECONCILIATION",
      stageIndex: status === "FAILED" ? job.value.stageIndex : SHOP_SMART_IMPORT_EXECUTION_STAGES.length - 1,
      overallPercent: status === "FAILED" ? job.value.overallPercent : 100,
      message: options.message || (status === "COMPLETED" ? "商品已完成入库" : status === "PARTIAL" ? "部分商品待确认" : "商品入库未完成"),
      errorCode: options.errorCode || "",
      retryable: status === "FAILED",
      counts: { ...job.value.counts, ...options.counts }
    };
    if (job.value.jobId) {
      writeBackgroundTaskRecovery(SHOP_SMART_IMPORT_RECOVERY_KEY, {
        jobId: job.value.jobId,
        fileName: job.value.fileName,
        startedAt: startedAt || Date.now()
      });
    }
    stopElapsedTimer();
  }

  function close(): void {
    visible.value = false;
    if (isShopSmartImportTerminal(job.value.status)) clearBackgroundTaskRecovery(SHOP_SMART_IMPORT_RECOVERY_KEY);
  }

  function show(): void {
    visible.value = true;
  }

  async function retry(): Promise<ShopSmartImportJob | null> {
    return lastFile.value ? startAnalysis(lastFile.value) : null;
  }

  function dispose(): void {
    stopPolling();
    stopElapsedTimer();
  }

  const restoration = restorePersistedTask();

  if (getCurrentScope()) onScopeDispose(dispose);

  return {
    visible,
    job,
    elapsedSeconds,
    isTerminal,
    isRunning,
    startAnalysis,
    execute,
    beginExecution,
    finishExecution,
    close,
    show,
    retry,
    restoration,
    dispose
  };
}
