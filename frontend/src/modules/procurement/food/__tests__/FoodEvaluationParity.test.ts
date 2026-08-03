import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { saveAuthSession } from "@/services/authService";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";
import FoodAfterSalesPage from "../pages/FoodAfterSalesPage.vue";

const api = vi.hoisted(() => ({
  actionFoodSettlement: vi.fn(),
  listFoodEvaluations: vi.fn(),
  listFoodSettlements: vi.fn(),
  submitFoodEvaluation: vi.fn()
}));

vi.mock("../services/foodProcurementApi", () => api);

const pendingEvaluation = {
  evaluationId: 71,
  orderId: 17,
  orderNo: "FPO-20260721-001",
  supplierName: "供货商A",
  status: "PENDING_EVALUATION",
  updatedAt: "2026-07-21T09:00:00"
};

const pendingReview = {
  evaluationId: 72,
  orderId: 18,
  orderNo: "FPO-20260721-002",
  supplierName: "供货商B",
  status: "PENDING_REVIEW",
  qualityRating: 4,
  logisticsRating: 3,
  comment: "交付及时",
  updatedAt: "2026-07-21T10:00:00"
};

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/food/evaluations", component: FoodAfterSalesPage, props: { mode: "evaluation" } }]
  });
  await router.push("/food/evaluations");
  await router.isReady();
  const wrapper = mount(FoodAfterSalesPage, {
    props: { mode: "evaluation" },
    global: { plugins: [router], stubs: { Teleport: true } }
  });
  await flushPromises();
  return wrapper;
}

describe("FoodAfterSalesPage evaluation parity", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
    vi.clearAllMocks();
    saveAuthSession({
      token: "token",
      company: { companyType: "BUYER" },
      roles: ["PURCHASER"],
      permissions: [],
      menus: [],
      defaultRoute: "/food/evaluations",
      profileStatus: "ACTIVE"
    });
    api.listFoodEvaluations.mockResolvedValue([pendingEvaluation, pendingReview]);
    api.listFoodSettlements.mockResolvedValue([]);
    api.submitFoodEvaluation.mockResolvedValue({ ...pendingEvaluation, status: "PENDING_REVIEW" });
  });

  it("uses the material evaluation card and drawer interactions", async () => {
    const wrapper = await mountPage();

    expect(wrapper.findAll(".evaluation-card")).toHaveLength(2);
    expect(wrapper.text()).toContain("附件证明：0 个");
    expect(wrapper.findAll('input[type="range"]')).toHaveLength(0);
    expect(wrapper.findAll('button[aria-label="通过评价"]')).toHaveLength(0);

    await wrapper.get('button[aria-label="填写评价"]').trigger("click");
    expect(wrapper.find(".detail-drawer").exists()).toBe(true);
    expect(wrapper.findAll(".evaluation-star-picker button")).toHaveLength(10);
    await wrapper.get('button[aria-label="品质3分"]').trigger("click");
    await wrapper.get('button[aria-label="提交评价"]').trigger("click");
    await flushPromises();

    expect(api.submitFoodEvaluation).toHaveBeenCalledWith(71, 3, 5, "", []);
    expect(wrapper.text()).toContain("伙食供应评价已提交");
  });

  it("opens submitted evaluations read-only in the same detail drawer", async () => {
    const wrapper = await mountPage();
    await wrapper.get('button[aria-label="查看明细"]').trigger("click");

    expect(wrapper.find(".detail-drawer").exists()).toBe(true);
    expect(wrapper.get(".evaluation-editor textarea").attributes("readonly")).toBeDefined();
    expect(wrapper.findAll(".evaluation-star-picker button").every((button) => button.attributes("disabled") !== undefined)).toBe(true);
    expect(wrapper.find('button[aria-label="提交评价"]').exists()).toBe(false);
  });

  it("clears stale errors before showing the evaluation success feedback", async () => {
    api.submitFoodEvaluation
      .mockRejectedValueOnce(new Error("已添加到剪贴板"))
      .mockResolvedValueOnce({ ...pendingEvaluation, status: "PENDING_REVIEW" });
    const writeText = vi.fn();
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText } });
    const wrapper = await mountPage();

    await wrapper.get('button[aria-label="填写评价"]').trigger("click");
    await wrapper.get('button[aria-label="提交评价"]').trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("已添加到剪贴板");

    await wrapper.get('button[aria-label="提交评价"]').trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("伙食供应评价已提交");
    expect(wrapper.text()).not.toContain("已添加到剪贴板");
    expect(writeText).not.toHaveBeenCalled();
  });

  it("labels material and food sources explicitly in evaluation supervision", () => {
    expect(workbenchSource).toContain('evaluationSourceLabel(evaluationEditing.source)');
    expect(workbenchSource).toContain('source === "FOOD" ? "伙食采购" : "物料采购"');
    expect(workbenchSource).toContain("row.attachments?.length");
  });
});
