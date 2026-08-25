import { beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";
import { getMaterialAiCapabilities } from "@/services/procurementMaterialService";
import { useMaterialComparisonAiOverlay } from "../composables/useMaterialComparisonAiOverlay";

vi.mock("@/services/procurementMaterialService", () => ({
  getMaterialAiCapabilities: vi.fn()
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise;
  });
  return { promise, resolve };
}

describe("useMaterialComparisonAiOverlay", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
  });

  it("仅在模型重排已配置且真实比价请求进行时展示诚实不确定进度", async () => {
    vi.mocked(getMaterialAiCapabilities).mockResolvedValueOnce({
      categoryAnalysisConfigured: true,
      comparisonRerankConfigured: true,
      status: "CONFIGURED"
    });
    const request = deferred<{
      items: Array<{ candidates: unknown[] }>;
      demand: { status: string };
      aiProcessing: { status: string; appliedItemCount: number; fallbackItemCount: number };
    }>();
    const controller = useMaterialComparisonAiOverlay();

    const resultPromise = controller.runLoad(() => request.promise);
    await flushPromises();
    expect(controller.visible.value).toBe(true);
    expect(controller.status.value).toBe("RUNNING");
    expect(controller.progressPercent.value).toBeNull();
    expect(controller.stages[0].label).toBe("候选筛选与必要时模型重排");

    request.resolve({
      items: [{ candidates: [{}, {}] }, { candidates: [{}] }],
      demand: { status: "COMPARING" },
      aiProcessing: { status: "MODEL_APPLIED", appliedItemCount: 2, fallbackItemCount: 0 }
    });
    await expect(resultPromise).resolves.toMatchObject({ items: expect.any(Array) });
    expect(controller.status.value).toBe("COMPLETED");
    expect(controller.counters.value.map((item) => item.value).join(" ")).toContain("2");
    expect(controller.counters.value.map((item) => item.value).join(" ")).toContain("3");
    expect(controller.message.value).toContain("模型已重排");
    expect(controller.message.value).toContain("2");
    expect(controller.counters.value.map((item) => item.value).join(" ")).toContain("MODEL_APPLIED");

    await vi.advanceTimersByTimeAsync(500);
    expect(controller.visible.value).toBe(false);
  });

  it("模型重排未配置时照常加载比价，但完全不显示AI遮罩", async () => {
    vi.mocked(getMaterialAiCapabilities).mockResolvedValueOnce({
      categoryAnalysisConfigured: false,
      comparisonRerankConfigured: false,
      status: "MODEL_CONFIGURATION_REQUIRED"
    });
    const controller = useMaterialComparisonAiOverlay();

    await expect(controller.runLoad(async () => ({ items: [], demand: { status: "SAVED" } }))).resolves.toBeTruthy();
    expect(controller.visible.value).toBe(false);
    expect(controller.status.value).toBe("IDLE");
  });

  it.each([
    ["MODEL_FALLBACK", 0, 4, "模型失败已回退确定性", "4"],
    ["MODEL_CONFIGURATION_REQUIRED", 0, 0, "模型待配置已使用确定性", "MODEL_CONFIGURATION_REQUIRED"],
    ["DETERMINISTIC", 0, 0, "确定性匹配完成", "DETERMINISTIC"]
  ])("按真实aiProcessing状态展示终态：%s", async (aiStatus, appliedItemCount, fallbackItemCount, expectedMessage, expectedCounter) => {
    vi.mocked(getMaterialAiCapabilities).mockResolvedValueOnce({
      categoryAnalysisConfigured: true,
      comparisonRerankConfigured: true,
      status: "CONFIGURED"
    });
    const controller = useMaterialComparisonAiOverlay();

    await controller.runLoad(async () => ({
      items: [],
      demand: { status: "COMPARING" },
      aiProcessing: { status: aiStatus, appliedItemCount, fallbackItemCount }
    }));

    expect(controller.message.value).toContain(expectedMessage);
    expect(controller.counters.value.map((item) => item.value).join(" ")).toContain(expectedCounter);
  });
});
