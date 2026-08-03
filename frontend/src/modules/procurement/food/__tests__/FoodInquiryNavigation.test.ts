import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import FoodDemandPage from "../pages/FoodDemandPage.vue";
import FoodInquiryPage from "../pages/FoodInquiryPage.vue";
import { getFoodDemand, listFoodDemands, listSupplierFoodInquiries } from "../services/foodProcurementApi";
import type { FoodDemandSummary } from "../types";

const authActor = vi.hoisted(() => ({ companyType: "BUYER", roles: ["PURCHASER"] }));

vi.mock("@/services/authService", () => ({
  ApiError: class ApiError extends Error {},
  getAuthSession: () => ({ company: { companyType: authActor.companyType }, roles: authActor.roles })
}));

vi.mock("@/services/dataDictionaryService", () => ({
  listPublicDictionaryItems: vi.fn().mockResolvedValue([])
}));

vi.mock("../services/foodProcurementApi", () => ({
  getFoodDemand: vi.fn(),
  listFoodDemands: vi.fn(),
  listSupplierFoodInquiries: vi.fn().mockResolvedValue([]),
  previewFoodDemand: vi.fn(),
  saveFoodDemand: vi.fn()
}));

const demand: FoodDemandSummary = {
  demandId: 42,
  demandNo: "FREQ-20260718-001",
  inquiryNo: "FOOD20260718001",
  vesselName: "NEW AMBER",
  supplyPort: "秀山东锚地",
  vesselEta: "2026-07-20T09:00",
  currency: "USD",
  status: "QUOTED",
  itemCount: 1,
  matchedCount: 1,
  pendingCount: 0,
  supplierCount: 7,
  submittedQuoteCount: 2,
  inquirySentAt: "2026-07-18T10:00",
  quoteDeadlineAt: "2026-07-21T10:00",
  updatedAt: "2026-07-18T10:00"
};

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/procurement/food/:demandId", component: { template: "<div />" } },
      { path: "/food/comparison/:demandId", component: { template: "<div />" } }
    ]
  });
}

describe("food inquiry detail navigation", () => {
  beforeEach(() => {
    authActor.companyType = "BUYER";
    authActor.roles = ["PURCHASER"];
    vi.clearAllMocks();
    vi.mocked(getFoodDemand).mockResolvedValue({
      demand,
      sourceFileName: "food.xlsx",
      sourceSheetName: "Food",
      items: [{ sequenceNo: 1, nameZh: "大米", unit: "KG", requestedQuantity: 10, matchStatus: "MATCHED" }]
    });
    vi.mocked(listFoodDemands).mockResolvedValue([demand]);
  });

  it("shows supplier and received-quote totals and opens comparison", async () => {
    const router = createTestRouter();
    await router.push("/procurement/food/42");
    await router.isReady();
    const wrapper = mount(FoodDemandPage, { global: { plugins: [router] } });
    await flushPromises();

    const summary = wrapper.get(".food-inquiry-summary-card");
    expect(summary.text()).toContain("询价供货商7 家");
    expect(summary.text()).toContain("报价供货商2 家");
    expect(summary.get(".stable-datetime-trigger").text()).toContain("2026-07-21");
    const compareButton = summary.get('button[aria-label="比价"]');
    expect(compareButton.attributes("disabled")).toBeUndefined();
    expect(compareButton.text()).toBe("比");
    expect(wrapper.get('button[aria-label="一键发起报价单"]').text()).toBe("报");

    await compareButton.trigger("click");
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe("/food/comparison/42");
  });

  it("disables comparison when no supplier quote has arrived", async () => {
    vi.mocked(getFoodDemand).mockResolvedValueOnce({
      demand: { ...demand, submittedQuoteCount: 0 },
      items: [{ sequenceNo: 1, nameZh: "大米", unit: "KG", requestedQuantity: 10, matchStatus: "MATCHED" }]
    });
    const router = createTestRouter();
    await router.push("/procurement/food/42");
    await router.isReady();
    const wrapper = mount(FoodDemandPage, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.get('button[aria-label="比价"]').attributes("disabled")).toBeDefined();
  });

  it("opens the food demand entry detail from buyer inquiry management", async () => {
    const router = createTestRouter();
    await router.push("/food/comparison/0");
    await router.isReady();
    const wrapper = mount(FoodInquiryPage, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.find('button[aria-label="查看报价"]').exists()).toBe(false);
    await wrapper.get('.procurement-query-card__footer button[aria-label="查询"]').trigger("click");
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe("/procurement/food/42");
  });

  it("shows inquiries sent by a supplier company instead of received supplier inquiries", async () => {
    authActor.companyType = "SUPPLIER";
    authActor.roles = ["SUPPLIER"];
    const router = createTestRouter();
    await router.push("/food/comparison/0");
    await router.isReady();

    const wrapper = mount(FoodInquiryPage, { global: { plugins: [router] } });
    await flushPromises();

    expect(listFoodDemands).toHaveBeenCalledOnce();
    expect(listSupplierFoodInquiries).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("FOOD20260718001");
  });

  it("shows every demand status when the status filter is all", async () => {
    vi.mocked(listFoodDemands).mockResolvedValueOnce([
      { ...demand, demandId: 40, inquiryNo: "FOOD-DRAFT", status: "DRAFT", supplierCount: 0, submittedQuoteCount: 0 },
      { ...demand, demandId: 41, inquiryNo: "FOOD-SENT", status: "INQUIRY_SENT", supplierCount: 0, submittedQuoteCount: 0 },
      { ...demand, demandId: 42, inquiryNo: "FOOD-QUOTED", status: "QUOTED", supplierCount: 2, submittedQuoteCount: 1 },
      { ...demand, demandId: 43, inquiryNo: "FOOD-ORDERED", status: "ORDERED", supplierCount: 2, submittedQuoteCount: 2 }
    ]);
    const router = createTestRouter();
    await router.push("/food/comparison/0");
    await router.isReady();

    const wrapper = mount(FoodInquiryPage, { global: { plugins: [router] } });
    await flushPromises();

    expect(listFoodDemands).toHaveBeenCalledOnce();
    expect(wrapper.findAll(".procurement-query-card")).toHaveLength(4);
    expect(wrapper.findAll<HTMLSelectElement>("select")[0].findAll("option").map((option) => option.attributes("value")))
      .toEqual(["", "DRAFT", "INQUIRY_SENT", "QUOTED", "ORDERED"]);
  });

  it("keeps filtered inquiry results ordered by updated time and demand id descending", async () => {
    vi.mocked(listFoodDemands).mockResolvedValueOnce([
      { ...demand, demandId: 41, inquiryNo: "SORT-OLDER", status: "QUOTED", updatedAt: "2026-07-17T09:00:00" },
      { ...demand, demandId: 42, inquiryNo: "SORT-SAME-LOW", status: "QUOTED", updatedAt: "2026-07-18T10:00:00" },
      { ...demand, demandId: 44, inquiryNo: "SORT-SAME-HIGH", status: "QUOTED", updatedAt: "2026-07-18T10:00:00" },
      { ...demand, demandId: 45, inquiryNo: "OTHER-DRAFT", status: "DRAFT", updatedAt: "2026-07-19T10:00:00" }
    ]);
    const router = createTestRouter();
    await router.push("/food/comparison/0");
    await router.isReady();
    const wrapper = mount(FoodInquiryPage, { global: { plugins: [router] } });
    await flushPromises();

    await wrapper.get('input[placeholder="单号、船舶检索"]').setValue("SORT");
    await wrapper.get("select").setValue("QUOTED");

    expect(wrapper.findAll(".procurement-query-card__primary strong").map((node) => node.text()))
      .toEqual(["SORT-SAME-HIGH", "SORT-SAME-LOW", "SORT-OLDER"]);
  });

  it("reloads the real demand detail when the route demandId changes", async () => {
    const router = createTestRouter();
    await router.push("/procurement/food/42");
    await router.isReady();
    const wrapper = mount(FoodDemandPage, { global: { plugins: [router] } });
    await flushPromises();

    vi.mocked(getFoodDemand).mockResolvedValueOnce({
      demand: {
        ...demand,
        demandId: 43,
        inquiryNo: "FOOD20260718002",
        vesselName: "SECOND VESSEL",
        supplierCount: 5,
        submittedQuoteCount: 3,
        quoteDeadlineAt: "2026-07-22T18:00"
      },
      sourceFileName: "second-food.xlsx",
      sourceSheetName: "Food",
      items: [{ sequenceNo: 1, nameZh: "面粉", unit: "KG", requestedQuantity: 20, matchStatus: "MATCHED" }]
    });

    await router.push("/procurement/food/43");
    await flushPromises();

    expect(getFoodDemand).toHaveBeenLastCalledWith(43);
    expect(wrapper.get('input[placeholder="请输入船舶名称"]').element).toHaveProperty("value", "SECOND VESSEL");
    expect(wrapper.get(".food-inquiry-summary-card").text()).toContain("询价供货商5 家");
    expect(wrapper.get(".food-inquiry-summary-card").text()).toContain("报价供货商3 家");
    expect(wrapper.get('input[placeholder="中文名称"]').element).toHaveProperty("value", "面粉");
  });
});
