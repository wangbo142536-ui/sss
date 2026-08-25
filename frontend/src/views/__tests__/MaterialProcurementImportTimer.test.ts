import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import MaterialProcurementEntry from "../MaterialProcurementEntry.vue";
import { uploadMaterialMatchPreview } from "@/services/procurementMaterialService";

vi.mock("@/services/dataDictionaryService", () => ({
  listPublicDictionaryItems: vi.fn().mockResolvedValue([])
}));

vi.mock("@/services/procurementMaterialService", () => ({
  getMaterialDemandDetail: vi.fn(),
  getMaterialAiCapabilities: vi.fn().mockResolvedValue({
    categoryAnalysisConfigured: false,
    comparisonRerankConfigured: false,
    status: "MODEL_CONFIGURATION_REQUIRED"
  }),
  saveMaterialDemand: vi.fn(),
  uploadMaterialMatchPreview: vi.fn()
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise;
  });
  return { promise, resolve };
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/procurement/materials", component: { template: "<div />" } }]
  });
  await router.push("/procurement/materials");
  await router.isReady();
  return mount(MaterialProcurementEntry, {
    attachTo: document.body,
    global: {
      plugins: [router],
      stubs: {
        WorkbenchLayout: { template: "<main><slot /></main>" },
        StableDateTimeInput: { template: "<input />" }
      }
    }
  });
}

async function chooseFile(wrapper: ReturnType<typeof mount>, name = "materials.xlsx") {
  const input = wrapper.get('input[type="file"]');
  Object.defineProperty(input.element, "files", { configurable: true, value: [new File(["materials"], name)] });
  await input.trigger("change");
}

describe("material procurement import timer", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    document.body.innerHTML = "";
  });

  it("使用统一遮罩展示真实耗时，并在无任务协议时保持诚实不确定进度", async () => {
    const request = deferred<Awaited<ReturnType<typeof uploadMaterialMatchPreview>>>();
    vi.mocked(uploadMaterialMatchPreview).mockReturnValueOnce(request.promise);
    const wrapper = await mountPage();

    await chooseFile(wrapper);
    await flushPromises();
    const overlay = document.body.querySelector(".background-task-overlay") as HTMLElement;
    expect(overlay).not.toBeNull();
    expect(overlay.querySelectorAll(".background-task-overlay__stages li")).toHaveLength(1);
    expect(overlay.textContent).toContain("标准库匹配与AI大类分析");
    expect(overlay.textContent).toContain("MODEL_CONFIGURATION_REQUIRED");
    expect(overlay.textContent).toContain("待配置");
    expect(overlay.querySelector('[role="progressbar"]')?.hasAttribute("aria-valuenow")).toBe(false);
    expect(overlay.textContent).toContain("等待服务端返回真实进度");
    expect(overlay.textContent).not.toMatch(/24%|48%|72%|88%/);
    expect(overlay.querySelector(".background-task-overlay__timer")?.textContent).toContain("0");

    await vi.advanceTimersByTimeAsync(2100);
    expect(document.body.querySelector(".background-task-overlay__timer")?.textContent).toContain("2");

    request.resolve({
      sourceFileName: "materials.xlsx",
      sourceFileId: "source-1",
      documentType: "MATERIAL",
      headerRowIndex: 1,
      totalRows: 1,
      exactCount: 1,
      similarCount: 0,
      unmatchedCount: 0,
      items: []
    });
    await flushPromises();
    expect(document.body.querySelector(".background-task-overlay__timer")?.textContent).toContain("2");
    expect(document.body.textContent).toContain("已匹配 1 / 待确认 0 / 失败 0");

    await vi.advanceTimersByTimeAsync(400);
    expect(document.body.querySelector(".background-task-overlay__timer")).toBeNull();
    expect(vi.getTimerCount()).toBe(0);
    wrapper.unmount();
  });
});
