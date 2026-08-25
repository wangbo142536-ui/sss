import { beforeEach, describe, expect, it, vi } from "vitest";
import { executeShopSmartImport, getShopSmartImportJob, startShopSmartImport } from "../services/shopSmartImportService";

vi.mock("@/services/authService", () => ({
  ApiError: class ApiError extends Error {
    constructor(message: string, public status: number, public payload?: unknown) {
      super(message);
    }
  },
  getAuthSession: vi.fn(() => ({ token: "token-0818" }))
}));

describe("shopSmartImportService", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("使用新建任务接口上传原始文件", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify({ jobId: "job-1", status: "QUEUED" }), { status: 202 }));
    const file = new File(["xlsx"], "random.xlsx");

    await startShopSmartImport(file);

    expect(fetch).toHaveBeenCalledWith(
      "/api/shop/skus/intelligent-imports",
      expect.objectContaining({
        method: "POST",
        body: expect.any(FormData),
        headers: expect.objectContaining({ Authorization: "Bearer token-0818" })
      })
    );
  });

  it("按jobId轮询服务端真实进度", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify({ jobId: "job/2", status: "RUNNING" }), { status: 200 }));

    await getShopSmartImportJob("job/2");

    expect(fetch).toHaveBeenCalledWith(
      "/api/shop/skus/intelligent-imports/job%2F2",
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: "Bearer token-0818" }) })
    );
  });

  it("通过任务执行接口提交已确认的预览行", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify({ jobId: "job-3", status: "QUEUED" }), { status: 202 }));

    await executeShopSmartImport("job-3", [101, "102"]);

    expect(fetch).toHaveBeenCalledWith(
      "/api/shop/skus/intelligent-imports/job-3/execute",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ previewRowIds: [101, 102] })
      })
    );
  });
});
