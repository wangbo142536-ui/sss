import { getCurrentScope, onScopeDispose, ref } from "vue";
import type { BackgroundTaskCounter, BackgroundTaskStage, BackgroundTaskStatus } from "@/modules/backgroundProcessing/types/backgroundTask";
import { getMaterialAiCapabilities, type MaterialAiCapabilities } from "@/services/procurementMaterialService";
import type { MaterialComparisonAiProcessing } from "@/types/procurementMaterials";

type ComparisonResponseShape = {
  items?: Array<{ candidates?: unknown[] }>;
  demand?: { status?: unknown };
  aiProcessing?: MaterialComparisonAiProcessing | {
    status: string;
    appliedItemCount: number;
    fallbackItemCount: number;
  };
};

export function useMaterialComparisonAiOverlay() {
  const visible = ref(false);
  const status = ref<BackgroundTaskStatus>("IDLE");
  const progressPercent = ref<number | null>(null);
  const elapsedSeconds = ref(0);
  const message = ref("");
  const counters = ref<BackgroundTaskCounter[]>([]);
  const capabilities = ref<MaterialAiCapabilities | null>(null);
  const stages: readonly BackgroundTaskStage[] = [
    { code: "CANDIDATE_RERANK", label: "候选筛选与必要时模型重排" }
  ];
  let capabilityRequest: Promise<MaterialAiCapabilities | null> | null = null;
  let elapsedTimer: number | undefined;
  let closeTimer: number | undefined;
  let startedAt = 0;

  function stopTimers(): void {
    if (elapsedTimer !== undefined) window.clearInterval(elapsedTimer);
    if (closeTimer !== undefined) window.clearTimeout(closeTimer);
    elapsedTimer = undefined;
    closeTimer = undefined;
  }

  function start(): void {
    stopTimers();
    visible.value = true;
    status.value = "RUNNING";
    progressPercent.value = null;
    elapsedSeconds.value = 0;
    message.value = "正在加载候选商品；仅在候选结果需要时调用模型重排";
    counters.value = [
      { label: "比价商品", value: "--" },
      { label: "候选商品", value: "--" },
      { label: "返回状态", value: "处理中" }
    ];
    startedAt = Date.now();
    elapsedTimer = window.setInterval(() => {
      elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000);
    }, 1000);
  }

  function scheduleClose(): void {
    closeTimer = window.setTimeout(() => {
      visible.value = false;
      closeTimer = undefined;
    }, 420);
  }

  function complete(result: ComparisonResponseShape): void {
    if (elapsedTimer !== undefined) window.clearInterval(elapsedTimer);
    elapsedTimer = undefined;
    const items = Array.isArray(result.items) ? result.items : [];
    const candidateCount = items.reduce((total, item) => total + (Array.isArray(item.candidates) ? item.candidates.length : 0), 0);
    const responseStatus = String(result.demand?.status || "已返回");
    const aiProcessing = result.aiProcessing;
    const aiStatus = String(aiProcessing?.status || "");
    const appliedItemCount = Number(aiProcessing?.appliedItemCount || 0);
    const fallbackItemCount = Number(aiProcessing?.fallbackItemCount || 0);
    const aiMessage = aiStatus === "MODEL_APPLIED"
      ? `模型已重排：实际应用 ${appliedItemCount} 个商品`
      : aiStatus === "MODEL_FALLBACK"
        ? `模型失败已回退确定性：回退 ${fallbackItemCount} 个商品`
        : aiStatus === "MODEL_CONFIGURATION_REQUIRED"
          ? "模型待配置已使用确定性"
          : aiStatus === "DETERMINISTIC"
            ? "确定性匹配完成"
            : "候选筛选已返回，未收到AI处理状态";
    status.value = "COMPLETED";
    message.value = aiMessage;
    counters.value = [
      { label: "比价商品", value: String(items.length) },
      { label: "候选商品", value: String(candidateCount) },
      { label: "返回状态", value: responseStatus },
      { label: "AI处理状态", value: aiStatus || "未返回" },
      { label: "模型应用 / 回退", value: `${appliedItemCount} / ${fallbackItemCount}` }
    ];
    scheduleClose();
  }

  function fail(error: unknown): void {
    if (elapsedTimer !== undefined) window.clearInterval(elapsedTimer);
    elapsedTimer = undefined;
    status.value = "FAILED";
    message.value = error instanceof Error && error.message ? error.message : "比价候选加载失败";
    counters.value = [{ label: "返回状态", value: "FAILED" }];
    scheduleClose();
  }

  async function ensureCapabilities(): Promise<MaterialAiCapabilities | null> {
    if (capabilities.value) return capabilities.value;
    if (!capabilityRequest) {
      capabilityRequest = getMaterialAiCapabilities()
        .then((value) => {
          capabilities.value = value;
          return value;
        })
        .catch(() => null)
        .finally(() => {
          capabilityRequest = null;
        });
    }
    return capabilityRequest;
  }

  async function runLoad<T extends ComparisonResponseShape>(loader: () => Promise<T>): Promise<T> {
    const capability = await ensureCapabilities();
    const showAiOverlay = capability?.comparisonRerankConfigured === true;
    if (showAiOverlay) start();
    try {
      const result = await loader();
      if (showAiOverlay) complete(result);
      return result;
    } catch (error) {
      if (showAiOverlay) fail(error);
      throw error;
    }
  }

  function close(): void {
    visible.value = false;
    stopTimers();
  }

  if (getCurrentScope()) onScopeDispose(stopTimers);

  return {
    visible,
    status,
    progressPercent,
    elapsedSeconds,
    message,
    counters,
    capabilities,
    stages,
    runLoad,
    close
  };
}
