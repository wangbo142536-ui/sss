import { beforeEach, describe, expect, it, vi } from "vitest";
import { useShopSmartImportJob } from "../composables/useShopSmartImportJob";
import { executeShopSmartImport, getShopSmartImportJob, startShopSmartImport } from "../services/shopSmartImportService";

vi.mock("../services/shopSmartImportService", () => ({
  startShopSmartImport: vi.fn(),
  getShopSmartImportJob: vi.fn(),
  executeShopSmartImport: vi.fn()
}));

describe("useShopSmartImportJob", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  it("刷新页面后按持久化任务ID恢复真实服务端进度", async () => {
    window.localStorage.setItem(
      "ship-supply:shop-smart-import-task",
      JSON.stringify({ jobId: "job-restored", fileName: "restored.xlsx", startedAt: Date.now() - 3000 })
    );
    vi.mocked(getShopSmartImportJob).mockResolvedValueOnce({
      jobId: "job-restored",
      fileName: "restored.xlsx",
      status: "COMPLETED",
      stage: "PREVIEW_BUILD",
      overallPercent: 100,
      counts: { itemTotal: 18, itemProcessed: 18 }
    });

    const controller = useShopSmartImportJob({ pollIntervalMs: 1000 });
    await controller.restoration;

    expect(getShopSmartImportJob).toHaveBeenCalledWith("job-restored");
    expect(controller.visible.value).toBe(true);
    expect(controller.job.value).toMatchObject({ jobId: "job-restored", status: "COMPLETED", overallPercent: 100 });
    expect(controller.elapsedSeconds.value).toBeGreaterThanOrEqual(3);
  });

  it("只使用服务端轮询数据推进进度，终态保持展示", async () => {
    vi.mocked(startShopSmartImport).mockResolvedValueOnce({
      jobId: "job-1",
      status: "QUEUED",
      stage: "UPLOAD",
      overallPercent: 7
    });
    vi.mocked(getShopSmartImportJob)
      .mockResolvedValueOnce({ jobId: "job-1", status: "RUNNING", stage: "WORKBOOK_PARSE", overallPercent: 27 })
      .mockResolvedValueOnce({
        jobId: "job-1",
        status: "PREVIEW_READY",
        stage: "PREVIEW_BUILD",
        overallPercent: 100,
        counts: { itemTotal: 131, itemProcessed: 131 }
      });
    const controller = useShopSmartImportJob({ pollIntervalMs: 1000 });

    const terminalPromise = controller.startAnalysis(new File(["xlsx"], "random.xlsx"));
    await vi.advanceTimersByTimeAsync(1000);
    expect(controller.job.value.overallPercent).toBe(27);
    await vi.advanceTimersByTimeAsync(1000);
    await expect(terminalPromise).resolves.toMatchObject({ status: "COMPLETED", overallPercent: 100 });
    expect(controller.visible.value).toBe(true);
    expect(controller.isTerminal.value).toBe(true);
    expect(controller.job.value.counts.itemProcessed).toBe(131);
  });

  it("将后端 PREVIEW_READY 识别为分析完成并停止轮询", async () => {
    vi.mocked(startShopSmartImport).mockResolvedValueOnce({
      jobId: "job-preview-ready",
      status: "QUEUED",
      stage: "UPLOAD",
      overallPercent: 8
    });
    vi.mocked(getShopSmartImportJob).mockResolvedValueOnce({
      jobId: "job-preview-ready",
      status: "PREVIEW_READY",
      stage: "PREVIEW_BUILD",
      overallPercent: 100,
      preview: { batchId: 67, items: [] }
    });
    const controller = useShopSmartImportJob({ pollIntervalMs: 1000 });

    const terminalPromise = controller.startAnalysis(new File(["xlsx"], "empty-template.xlsx"));
    await vi.advanceTimersByTimeAsync(1000);

    await expect(terminalPromise).resolves.toMatchObject({ status: "COMPLETED", overallPercent: 100 });
    expect(controller.isTerminal.value).toBe(true);
    expect(controller.visible.value).toBe(true);
    expect(getShopSmartImportJob).toHaveBeenCalledTimes(1);
  });

  it("确认预览后调用执行接口并按服务端4阶段更新", async () => {
    vi.mocked(startShopSmartImport).mockResolvedValueOnce({ jobId: "job-2", status: "COMPLETED", stage: "PREVIEW_BUILD", overallPercent: 100 });
    vi.mocked(executeShopSmartImport).mockResolvedValueOnce({ jobId: "job-2", status: "QUEUED" });
    vi.mocked(getShopSmartImportJob).mockResolvedValueOnce({
      jobId: "job-2",
      status: "COMPLETED",
      stage: "COUNT_RECONCILIATION",
      stageIndex: 4,
      stageCount: 4,
      overallPercent: 100
    });
    const controller = useShopSmartImportJob({ pollIntervalMs: 1000 });
    await controller.startAnalysis(new File(["xlsx"], "random.xlsx"));

    const terminalPromise = controller.execute([101, 102]);
    await vi.advanceTimersByTimeAsync(1000);
    await expect(terminalPromise).resolves.toMatchObject({ phase: "EXECUTION", stage: "COUNT_RECONCILIATION", stageIndex: 3 });
    expect(executeShopSmartImport).toHaveBeenCalledWith("job-2", [101, 102]);
    expect(controller.visible.value).toBe(true);
  });
});
