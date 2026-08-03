import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import FoodDemandPage from "../pages/FoodDemandPage.vue";
import { previewFoodDemand } from "../services/foodProcurementApi";

vi.mock("@/services/dataDictionaryService", () => ({
  listPublicDictionaryItems: vi.fn().mockResolvedValue([])
}));

vi.mock("../services/foodProcurementApi", () => ({
  getFoodDemand: vi.fn(),
  previewFoodDemand: vi.fn(),
  saveFoodDemand: vi.fn()
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/procurement/food", component: { template: "<div />" } }]
  });
  await router.push("/procurement/food");
  await router.isReady();
  return mount(FoodDemandPage, { attachTo: document.body, global: { plugins: [router] } });
}

function chooseFile(wrapper: ReturnType<typeof mount>, name = "food.xlsx") {
  const input = wrapper.get('input[type="file"]');
  Object.defineProperty(input.element, "files", { configurable: true, value: [new File(["food"], name)] });
  return input.trigger("change");
}

describe("food demand import overlay", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    document.body.innerHTML = "";
  });

  it("stays visible for the full preview request and closes after successful import", async () => {
    const request = deferred<Awaited<ReturnType<typeof previewFoodDemand>>>();
    vi.mocked(previewFoodDemand).mockReturnValueOnce(request.promise);
    const wrapper = await mountPage();

    await chooseFile(wrapper);
    await flushPromises();
    expect(document.body.querySelector(".food-import-progress-backdrop")).not.toBeNull();
    expect(document.body.textContent).toContain("读取");
    expect(document.body.textContent).toContain("识别");
    expect(document.body.textContent).toContain("导入");
    expect(document.body.textContent).not.toContain("IMPA");

    request.resolve({
      fileName: "food.xlsx",
      selectedSheet: "Food",
      headerRow: 1,
      totalRows: 1,
      matchedCount: 1,
      pendingCount: 0,
      sheets: [],
      items: [{ sequenceNo: 1, nameZh: "大米", unit: "KG", requestedQuantity: 10, matchStatus: "MATCHED" }]
    });
    await flushPromises();
    expect(document.body.textContent).toContain("伙食清单导入完成");

    await vi.advanceTimersByTimeAsync(340);
    await flushPromises();
    expect(document.body.querySelector(".food-import-progress-backdrop")).toBeNull();
    expect(wrapper.text()).not.toContain("已识别 1 项");
    wrapper.unmount();
  });

  it("closes after failure and preserves the existing error message", async () => {
    vi.mocked(previewFoodDemand).mockRejectedValueOnce(new Error("文件格式不正确"));
    const wrapper = await mountPage();

    await chooseFile(wrapper, "invalid.xlsx");
    await flushPromises();
    expect(document.body.textContent).toContain("伙食清单导入失败");

    await vi.advanceTimersByTimeAsync(600);
    await flushPromises();
    expect(document.body.querySelector(".food-import-progress-backdrop")).toBeNull();
    expect(wrapper.get(".food-alert.is-error").text()).toBe("文件格式不正确");
    wrapper.unmount();
  });
});
