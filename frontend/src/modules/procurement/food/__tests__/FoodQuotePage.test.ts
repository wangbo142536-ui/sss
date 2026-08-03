import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { saveAuthSession } from "@/services/authService";
import FoodQuotePage from "../pages/FoodQuotePage.vue";

const api = vi.hoisted(() => ({
  getSupplierFoodQuote: vi.fn(),
  getBuyerFoodQuote: vi.fn(),
  listSupplierFoodInquiries: vi.fn(),
  listBuyerFoodInquiries: vi.fn(),
  saveSupplierFoodQuote: vi.fn(),
  virtualFillFoodQuote: vi.fn(),
  submitFoodQuote: vi.fn(),
  exportFoodQuote: vi.fn(),
  previewFoodQuoteImport: vi.fn(),
  commitFoodQuoteImport: vi.fn()
}));

vi.mock("../services/foodProcurementApi", () => api);

const quote = {
  quoteId: 30,
  demandId: 10,
  quoteNo: "FQ-20260716-001",
  demandNo: "FREQ-20260716-001",
  vesselName: "MV AMBER",
  supplyPort: "ZHOUSHAN",
  vesselEta: "2026-07-20T09:00:00",
  buyerName: "Buyer",
  supplierName: "Food Supplier",
  currency: "USD",
  status: "DRAFT",
  versionNo: 1,
  totalAmount: 30,
  quotedItemCount: 1,
  missingItemCount: 0,
  quantityDifferenceCount: 0,
  updatedAt: "2026-07-16T09:00:00",
  items: [{
    quoteItemId: 31,
    demandItemId: 11,
    sequenceNo: 1,
    nameEn: "Fresh Apple",
    nameZh: "\u82f9\u679c",
    specification: "10*500G",
    unit: "KG",
    requestedQuantity: 10,
    quotedQuantity: 10,
    unitPrice: 3,
    amount: 30,
    availability: "AVAILABLE",
    priceSource: "MANUAL",
    matchStatus: "MATCHED"
  }]
};

describe("FoodQuotePage supplier buttons", () => {
  beforeEach(() => {
    saveAuthSession({
      token: "token",
      company: { companyType: "SUPPLIER" },
      roles: ["SUPPLIER"],
      permissions: [],
      menus: [],
      defaultRoute: "/food/quotes",
      profileStatus: "ACTIVE"
    });
    api.getSupplierFoodQuote.mockResolvedValue(structuredClone(quote));
    api.getBuyerFoodQuote.mockResolvedValue(structuredClone(quote));
    api.saveSupplierFoodQuote.mockResolvedValue(structuredClone(quote));
    api.virtualFillFoodQuote.mockResolvedValue({ filledCount: 1, preservedCount: 0 });
    api.submitFoodQuote.mockResolvedValue({ ...structuredClone(quote), status: "SUBMITTED" });
    api.previewFoodQuoteImport.mockResolvedValue({ batchId: 40, fileName: "quote.xlsx", selectedSheet: "Quotation", matchedCount: 1, issueCount: 0, updates: [{}], issues: [] });
    api.commitFoodQuoteImport.mockResolvedValue({ updatedCount: 1, versionNo: 2 });
    api.listSupplierFoodInquiries.mockResolvedValue([]);
    api.listBuyerFoodInquiries.mockResolvedValue([]);
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("shows draft and submitted quotes when the status filter is all", async () => {
    api.listSupplierFoodInquiries.mockResolvedValue([
      {
        inquirySupplierId: 2, demandId: 11, quoteId: 31, demandNo: "FOOD-DRAFT", vesselName: "MV B", supplyPort: "PORT",
        vesselEta: "2026-07-20T09:00:00", buyerName: "Buyer", supplierName: "Supplier", status: "SENT",
        quoteStatus: "DRAFT", itemCount: 3, quotedItemCount: 1, missingItemCount: 2, totalAmount: 10,
        updatedAt: "2026-07-18T10:00:00"
      },
      {
        inquirySupplierId: 3, demandId: 12, quoteId: 32, demandNo: "FOOD-SUBMITTED", vesselName: "MV C", supplyPort: "PORT",
        vesselEta: "2026-07-20T09:00:00", buyerName: "Buyer", supplierName: "Supplier", status: "SUBMITTED",
        quoteStatus: "SUBMITTED", itemCount: 3, quotedItemCount: 3, missingItemCount: 0, totalAmount: 30,
        updatedAt: "2026-07-18T11:00:00"
      }
    ]);
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: "/food/quotes", component: FoodQuotePage },
        { path: "/food/quotes/:quoteId", component: FoodQuotePage }
      ]
    });
    await router.push("/food/quotes");
    await router.isReady();

    const wrapper = mount(FoodQuotePage, { global: { plugins: [router] } });
    await flushPromises();

    expect(api.listSupplierFoodInquiries).toHaveBeenCalledWith("", "");
    expect(api.listBuyerFoodInquiries).toHaveBeenCalledWith("", "");
    expect(wrapper.findAll("tbody tr")).toHaveLength(2);
    expect(wrapper.text()).not.toContain("伙食报价管理");
    expect(wrapper.text()).not.toContain("统一查看本公司发出的供货商报价和收到的待报价任务。");
    expect(wrapper.find<HTMLSelectElement>('select').findAll("option").map((option) => option.attributes("value")))
      .toEqual(["", "DRAFT", "SUBMITTED"]);
  });

  it("shows a back-to-top button after the quote table is scrolled", async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: "/food/quotes/:quoteId", component: FoodQuotePage }]
    });
    await router.push("/food/quotes/30");
    await router.isReady();
    const wrapper = mount(FoodQuotePage, {
      global: {
        plugins: [router],
        stubs: { Teleport: true }
      }
    });
    await flushPromises();

    const scrollArea = wrapper.get(".food-quote-table-wrap");
    const scrollTo = vi.fn();
    Object.defineProperty(scrollArea.element, "scrollTop", { value: 180, configurable: true });
    Object.defineProperty(scrollArea.element, "scrollTo", { value: scrollTo });
    await scrollArea.trigger("scroll");

    const backTop = wrapper.get('button[aria-label="回到顶部"]');
    await backTop.trigger("click");

    expect(backTop.classes()).toContain("compare-back-top-button");
    expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: "smooth" });
  });

  it("opens buyer draft quotes with the same editable actions as supplier quotes", async () => {
    api.listSupplierFoodInquiries.mockResolvedValue([{
      inquirySupplierId: 51, demandId: 15, quoteId: 61, demandNo: "RECEIVED-DRAFT", vesselName: "MV RECEIVED", supplyPort: "PORT",
      vesselEta: "2026-07-20T09:00:00", buyerName: "Other Buyer", supplierName: "Current Company", status: "SENT",
      quoteStatus: "DRAFT", itemCount: 3, quotedItemCount: 0, missingItemCount: 3, totalAmount: 0,
      updatedAt: "2026-07-18T10:00:00"
    }]);
    api.listBuyerFoodInquiries.mockResolvedValue([{
      inquirySupplierId: 52, demandId: 16, quoteId: 62, demandNo: "SENT-DRAFT", vesselName: "MV SENT", supplyPort: "PORT",
      vesselEta: "2026-07-20T09:00:00", buyerName: "Current Company", supplierName: "Other Supplier", status: "SENT",
      quoteStatus: "DRAFT", itemCount: 4, quotedItemCount: 0, missingItemCount: 4, totalAmount: 0,
      updatedAt: "2026-07-18T11:00:00"
    }]);
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: "/food/quotes", component: FoodQuotePage },
        { path: "/food/quotes/:quoteId", component: FoodQuotePage }
      ]
    });
    await router.push("/food/quotes");
    await router.isReady();
    const wrapper = mount(FoodQuotePage, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.text()).toContain("RECEIVED-DRAFT");
    expect(wrapper.text()).toContain("SENT-DRAFT");
    expect(wrapper.text()).toContain("收到");
    expect(wrapper.text()).toContain("发出");

    const sentRow = wrapper.findAll("tbody tr").find((row) => row.text().includes("SENT-DRAFT"));
    await sentRow!.get('button[aria-label="查看报价"]').trigger("click");
    await flushPromises();

    expect(router.currentRoute.value.fullPath).toBe("/food/quotes/62?actor=buyer");
    expect(api.getBuyerFoodQuote).toHaveBeenCalledWith(62);
    expect(api.getSupplierFoodQuote).not.toHaveBeenCalledWith(62);
    expect(wrapper.findAll(".food-quote-toolbar button")).toHaveLength(5);
    expect(wrapper.find('button[aria-label="导出"]').exists()).toBe(true);
    expect(wrapper.find('button[aria-label="导入"]').attributes("disabled")).toBeUndefined();
    expect(wrapper.find('button[aria-label="保存"]').attributes("disabled")).toBeUndefined();
    expect(wrapper.find('button[aria-label="提交"]').attributes("disabled")).toBeUndefined();
    expect(wrapper.text()).toContain("一键报价");
    expect(wrapper.findAll(".food-quote-table input").every((input) => input.attributes("disabled") === undefined)).toBe(true);
  });

  it("keeps submitted buyer quotes locked while leaving all actions visible", async () => {
    api.getBuyerFoodQuote.mockResolvedValue({ ...structuredClone(quote), status: "SUBMITTED" });
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: "/food/quotes/:quoteId", component: FoodQuotePage }]
    });
    await router.push("/food/quotes/30?actor=buyer");
    await router.isReady();

    const wrapper = mount(FoodQuotePage, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.find('button[aria-label="导出"]').attributes("disabled")).toBeUndefined();
    expect(wrapper.find('button[aria-label="导入"]').attributes("disabled")).toBeDefined();
    expect(wrapper.find('button[aria-label="保存"]').attributes("disabled")).toBeDefined();
    expect(wrapper.find('button[aria-label="提交"]').attributes("disabled")).toBeDefined();
    expect(wrapper.find(".food-virtual-button").attributes("disabled")).toBeDefined();
    expect(wrapper.findAll(".food-quote-table input").every((input) => input.attributes("disabled") !== undefined)).toBe(true);
  });

  it("keeps status-filtered quote results ordered by updated time and inquiry supplier id descending", async () => {
    api.listSupplierFoodInquiries.mockImplementation(async (_keyword: string, selectedStatus: string) => selectedStatus === "SUBMITTED" ? [
      {
        inquirySupplierId: 21, demandId: 11, quoteId: 41, demandNo: "SORT-OLDER", vesselName: "MV B", supplyPort: "PORT",
        vesselEta: "2026-07-20T09:00:00", buyerName: "Buyer", supplierName: "Supplier", status: "SUBMITTED",
        quoteStatus: "SUBMITTED", itemCount: 3, quotedItemCount: 3, missingItemCount: 0, totalAmount: 10,
        updatedAt: "2026-07-17T10:00:00"
      },
      {
        inquirySupplierId: 22, demandId: 12, quoteId: 42, demandNo: "SORT-SAME-LOW", vesselName: "MV C", supplyPort: "PORT",
        vesselEta: "2026-07-20T09:00:00", buyerName: "Buyer", supplierName: "Supplier", status: "SUBMITTED",
        quoteStatus: "SUBMITTED", itemCount: 3, quotedItemCount: 3, missingItemCount: 0, totalAmount: 20,
        updatedAt: "2026-07-18T11:00:00"
      },
      {
        inquirySupplierId: 24, demandId: 14, quoteId: 44, demandNo: "SORT-SAME-HIGH", vesselName: "MV D", supplyPort: "PORT",
        vesselEta: "2026-07-20T09:00:00", buyerName: "Buyer", supplierName: "Supplier", status: "SUBMITTED",
        quoteStatus: "SUBMITTED", itemCount: 3, quotedItemCount: 3, missingItemCount: 0, totalAmount: 30,
        updatedAt: "2026-07-18T11:00:00"
      }
    ] : []);
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: "/food/quotes", component: FoodQuotePage },
        { path: "/food/quotes/:quoteId", component: FoodQuotePage }
      ]
    });
    await router.push("/food/quotes");
    await router.isReady();
    const wrapper = mount(FoodQuotePage, { global: { plugins: [router] } });
    await flushPromises();

    await wrapper.get('input[placeholder="单号、船舶、供货商检索"]').setValue("SORT");
    await wrapper.get("select").setValue("SUBMITTED");
    await wrapper.get('button[aria-label="查询"]').trigger("click");
    await flushPromises();

    expect(api.listSupplierFoodInquiries).toHaveBeenLastCalledWith("SORT", "SUBMITTED");
    expect(wrapper.findAll("tbody tr td:first-child strong").map((node) => node.text()))
      .toEqual(["SORT-SAME-HIGH", "SORT-SAME-LOW", "SORT-OLDER"]);
  });

  it("renders and executes download, import, virtual fill, save and submit", async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: "/food/quotes", component: FoodQuotePage },
        { path: "/food/quotes/:quoteId", component: FoodQuotePage }
      ]
    });
    await router.push("/food/quotes/30");
    await router.isReady();
    const wrapper = mount(FoodQuotePage, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.find('button[aria-label="导出"]').exists()).toBe(true);
    expect(wrapper.find('button[aria-label="导入"]').exists()).toBe(true);
    expect(wrapper.find('button[aria-label="保存"]').exists()).toBe(true);
    expect(wrapper.find('button[aria-label="提交"]').exists()).toBe(true);
    expect(wrapper.text()).toContain("一键报价");
    expect(wrapper.find(".food-quote-facts > .food-quote-toolbar").exists()).toBe(true);
    expect(wrapper.findAll(".food-quote-table th").map((cell) => cell.text())).toEqual([
      "序号", "名称", "规格", "单位", "需求数量", "单价", "金额", "备注"
    ]);

    await wrapper.find('button[aria-label="导出"]').trigger("click");
    await wrapper.find(".food-virtual-button").trigger("click");
    await flushPromises();
    await wrapper.find('button[aria-label="保存"]').trigger("click");
    await flushPromises();

    const input = wrapper.find<HTMLInputElement>('input[type="file"]');
    const file = new File(["sheet"], "quote.xlsx");
    Object.defineProperty(input.element, "files", { value: [file] });
    await input.trigger("change");
    await flushPromises();

    await wrapper.find('button[aria-label="提交"]').trigger("click");
    await flushPromises();

    expect(api.exportFoodQuote).toHaveBeenCalledWith(30);
    expect(api.virtualFillFoodQuote).toHaveBeenCalledWith(30, true);
    expect(api.saveSupplierFoodQuote).toHaveBeenCalledTimes(2);
    expect(api.previewFoodQuoteImport).toHaveBeenCalledWith(30, file);
    expect(api.submitFoodQuote).toHaveBeenCalledWith(30);
  });

  it("renders an editable unit price when the internal price source is not assigned yet", async () => {
    api.getSupplierFoodQuote.mockResolvedValue({
      ...structuredClone(quote),
      items: [{ ...structuredClone(quote.items[0]), priceSource: null }]
    });
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: "/food/quotes/:quoteId", component: FoodQuotePage }]
    });
    await router.push("/food/quotes/30");
    await router.isReady();
    const wrapper = mount(FoodQuotePage, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.find('.food-quote-table input[type="number"]').exists()).toBe(true);
    expect(wrapper.find(".food-source-tag").exists()).toBe(false);
  });
});
