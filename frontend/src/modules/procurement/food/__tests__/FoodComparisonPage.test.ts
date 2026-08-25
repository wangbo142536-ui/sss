import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import FoodComparisonPage from "../pages/FoodComparisonPage.vue";
import { listCompanyContacts } from "@/services/companyService";
import {
  createFoodOrder,
  exportFoodComparison,
  getFoodComparison,
  saveFoodComparisonItems,
  saveFoodComparisonSettings
} from "../services/foodProcurementApi";
import type { FoodComparison, FoodComparisonSettings } from "../types";

vi.mock("../services/foodProcurementApi", () => ({
  createFoodOrder: vi.fn().mockResolvedValue({ orderId: 77, orderNo: "FPO-77", status: "PENDING_CONFIRMATION", totalAmount: 49.5 }),
  exportFoodComparison: vi.fn().mockResolvedValue(undefined),
  getFoodComparison: vi.fn(),
  importFoodComparison: vi.fn(),
  listFoodDemands: vi.fn().mockResolvedValue([]),
  saveFoodComparisonItems: vi.fn(),
  saveFoodComparisonSettings: vi.fn()
}));

vi.mock("@/services/companyService", () => ({
  listCompanyContacts: vi.fn()
}));

const settings = {
  markupPercent: 10,
  fixedFreightFee: 0,
  fixedCustomsFee: 0,
  fixedCraneFee: 0,
  fixedOtherFee: 0,
  supplyMode: "SEA" as const,
  fixedProviderType: "BARGE" as const,
  fixedProviderId: "",
  fixedProviderName: "",
  trafficServiceJson: "",
  mixedSupplierCount: 3,
  priceEnabled: true,
  priceLevel: 5,
  qualityEnabled: true,
  qualityLevel: 3,
  coreDemandItemIds: []
};

function comparison(comparisonSettings: FoodComparisonSettings = settings): FoodComparison {
  return {
    demand: {
      demandId: 55,
      demandNo: "FD-55",
      inquiryNo: "FOOD20260718001",
      vesselName: "NEW AMBER",
      supplyPort: "舟山",
      vesselEta: "2026-07-20T10:00:00",
      currency: "USD",
      status: "QUOTED",
      itemCount: 2,
      matchedCount: 2,
      pendingCount: 0,
      supplierCount: 1,
      submittedQuoteCount: 1,
      updatedAt: "2026-07-20T09:00:00"
    },
    submittedSupplierCount: 1,
    strategies: [
      { strategyType: "LOWEST_ITEM", label: "最低混供", enabled: true, coveredItemCount: 2, totalItemCount: 2, totalAmount: 370 },
      { strategyType: "SINGLE_SUPPLIER", label: "集中采购", enabled: true, coveredItemCount: 2, totalItemCount: 2, totalAmount: 370, supplierCompanyId: 7, supplierName: "供货商A" }
    ],
    items: [
      { demandItemId: 1, sequenceNo: 1, nameZh: "土豆", nameEn: "POTATO", specification: "L", unit: "KG", requestedQuantity: 100, quotes: [{ demandItemId: 1, quoteItemId: 91, quoteId: 31, supplierCompanyId: 7, supplierName: "供货商A", requestedQuantity: 100, quotedQuantity: 100, unitPrice: 3.25, amount: 325, availability: "AVAILABLE", priceSource: "MANUAL", quantitySatisfied: true, lowestPrice: true, productTags: ["质量高"] }] },
      { demandItemId: 2, sequenceNo: 2, nameZh: "生姜", nameEn: "GINGER", specification: "-", unit: "KG", requestedQuantity: 10, quotes: [{ demandItemId: 2, quoteItemId: 92, quoteId: 31, supplierCompanyId: 7, supplierName: "供货商A", requestedQuantity: 10, quotedQuantity: 10, unitPrice: 4.5, amount: 45, availability: "AVAILABLE", priceSource: "MANUAL", quantitySatisfied: true, lowestPrice: true }] }
    ],
    settings: comparisonSettings
  };
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/food/comparison/:demandId", component: FoodComparisonPage },
      { path: "/food/orders/:orderId", component: { template: "<div />" } }
    ]
  });
  await router.push("/food/comparison/55");
  await router.isReady();
  const wrapper = mount(FoodComparisonPage, { global: { plugins: [router], stubs: { FoodTrafficShuttleSelector: true } } });
  await flushPromises();
  return wrapper;
}

async function confirmOrder(wrapper: Awaited<ReturnType<typeof mountPage>>) {
  await wrapper.get('button[aria-label="确认下单"]').trigger("click");
  await flushPromises();
  const dialog = document.querySelector<HTMLElement>('.purchase-order-confirm-dialog');
  expect(dialog).not.toBeNull();
  const setValue = (selector: string, value: string) => {
    const input = dialog!.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event("input", { bubbles: true }));
  };
  setValue('textarea[placeholder="请输入港口、码头或船舶所在的具体交付地址"]', "码头仓库交付");
  dialog!.querySelector<HTMLButtonElement>('button[aria-label="确认生成采购单"]')!.click();
  await flushPromises();
}

describe("food comparison selection", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
    vi.clearAllMocks();
    vi.mocked(getFoodComparison).mockResolvedValue(comparison());
    vi.mocked(saveFoodComparisonItems).mockResolvedValue({ demandId: 55, updatedCount: 2 });
    vi.mocked(saveFoodComparisonSettings).mockResolvedValue(settings);
    vi.mocked(listCompanyContacts).mockResolvedValue({
      items: [
        { contactId: 11, contactName: "王经理", contactPhone: "15300800761", contactEmail: "wang@example.com", status: "ACTIVE" },
        { contactId: 12, contactName: "张经理", contactPhone: "15241256006", contactEmail: "zhang@example.com", status: "ACTIVE" }
      ]
    });
  });

  it("shows comparing status and KG below quoted quantity", async () => {
    const wrapper = await mountPage();
    expect(wrapper.text()).toContain("比价中");
    expect(wrapper.text()).not.toContain("数量满足");
    expect(wrapper.findAll(".compare-two-line-cell--right small").map((item) => item.text())).toEqual(["KG", "KG", "KG", "KG"]);
    expect(wrapper.findAll('.data-table-row [aria-label="质量高"]')).toHaveLength(1);
  });

  it("keeps the material comparison action order", async () => {
    const wrapper = await mountPage();
    expect(wrapper.findAll(".toolbar-icon-actions button").map((button) => button.attributes("aria-label"))).toEqual([
      "刷新",
      "保存比价设置",
      "比价策略引擎",
      "导出比价报价表",
      "导入比价报价表",
      "确认下单"
    ]);
  });

  it("syncs the material product strategy engine without IMPA-only controls", async () => {
    const wrapper = await mountPage();

    expect(wrapper.get('button[aria-label="比价策略引擎"]')).toBeTruthy();
    expect(wrapper.findAll(".compare-preference-filters")).toHaveLength(0);
    await wrapper.get('button[aria-label="比价策略引擎"]').trigger("click");
    await flushPromises();

    const dialog = document.querySelector<HTMLElement>('[role="dialog"][aria-label="比价策略引擎"]');
    expect(dialog).not.toBeNull();
    expect(dialog!.textContent).toContain("最低混供供货商数量");
    expect(dialog!.textContent).toContain("3 家");
    expect(dialog!.textContent).toContain("价格低");
    expect(dialog!.textContent).toContain("质量高");
    expect(dialog!.textContent).toContain("核心商品");
    expect(dialog!.textContent).not.toContain("IMPA");
  });

  it("keeps unmatched rows visible and provides core and unmatched quick filters", async () => {
    const payload = comparison({ ...settings, coreDemandItemIds: [1] });
    payload.items[1].quotes = [];
    vi.mocked(getFoodComparison).mockResolvedValue(payload);

    const wrapper = await mountPage();
    expect(wrapper.findAll(".data-table-row")).toHaveLength(2);
    expect(wrapper.findAll(".data-table-row")[0].classes()).toContain("is-compare-core-product");
    expect(wrapper.findAll(".data-table-row")[1].classes()).toContain("is-compare-unmatched");
    expect(wrapper.get('button[aria-label="筛选核心商品"]')).toBeTruthy();
    expect(wrapper.get('button[aria-label="筛选未匹配商品"]')).toBeTruthy();
    expect(wrapper.get('button[aria-label="筛选未匹配商品"] .material-strategy-icon__unmatched-face')).toBeTruthy();

    await wrapper.get('button[aria-label="筛选未匹配商品"]').trigger("click");
    expect(wrapper.findAll(".data-table-row")).toHaveLength(1);
    expect(wrapper.find(".data-table-row").text()).toContain("生姜");
  });

  it("keeps fixed supply fees separate from profit", async () => {
    vi.mocked(getFoodComparison).mockResolvedValue(comparison({
      ...settings,
      fixedFreightFee: 50
    }));

    const wrapper = await mountPage();
    const mixedStrategy = wrapper.findAll(".strategy-choice")[0];
    expect(mixedStrategy.text()).toContain("补给费用 50.00");
    expect(mixedStrategy.text()).toContain("预计利润 37.00");
  });

  it("allows row selection, marks unchecked rows red and exports only selected rows", async () => {
    const wrapper = await mountPage();
    const checkboxes = wrapper.findAll(".compare-row-select input");
    expect(checkboxes).toHaveLength(2);
    expect(checkboxes.every((item) => !item.attributes("disabled"))).toBe(true);

    await checkboxes[0].setValue(false);
    expect(wrapper.findAll(".data-table-row")[0].classes()).toContain("is-compare-deselected");
    const strategyCards = wrapper.findAll(".strategy-choice");
    expect(strategyCards[0].text()).toContain("1/2");
    expect(strategyCards[0].text()).toContain("成本金额 45.00");
    expect(strategyCards[0].text()).toContain("报价总额 49.50");
    expect(strategyCards[0].text()).not.toContain("USD");
    expect(strategyCards[0].text()).toContain("预计利润 4.50");
    expect(strategyCards[1].text()).toContain("1/2");
    expect(strategyCards[1].text()).toContain("成本金额 45.00");
    expect(strategyCards[1].text()).not.toContain("USD");
    expect(strategyCards[1].findAll(".strategy-supplier-meta em")).toHaveLength(0);
    expect(wrapper.find(".compare-fixed-fee-bar").text()).not.toContain("USD");

    await wrapper.get('button[aria-label="导出比价报价表"]').trigger("click");
    await flushPromises();
    expect(saveFoodComparisonItems).toHaveBeenCalledWith(55, expect.objectContaining({
      items: expect.arrayContaining([
        expect.objectContaining({ quoteItemId: 91 }),
        expect.objectContaining({ quoteItemId: 92 })
      ])
    }));
    expect(exportFoodComparison).toHaveBeenCalledWith(55, [92]);
  });

  it("creates a partial order from the selected comparison rows", async () => {
    const wrapper = await mountPage();
    await wrapper.findAll(".compare-row-select input")[0].setValue(false);
    await confirmOrder(wrapper);
    expect(createFoodOrder).toHaveBeenCalledWith(55, "LOWEST_ITEM", undefined, [{ demandItemId: 2, quoteItemId: 92 }], true, {
      requiredDeliveryTime: "2026-07-20T10:00",
      deliveryAddress: "码头仓库交付",
      deliveryContactName: "王经理",
      deliveryContactPhone: "15300800761",
      deliveryContactEmail: "wang@example.com",
      defaultPackagingMethod: "UNIFIED_PACKAGING",
      buyerRemark: ""
    });
  });

  it("matches the material purchase order dialog structure and amount summary", async () => {
    const wrapper = await mountPage();
    await wrapper.get('button[aria-label="确认下单"]').trigger("click");
    await flushPromises();

    const dialog = document.querySelector<HTMLElement>(".purchase-order-confirm-dialog");
    expect(dialog).not.toBeNull();
    expect(dialog!.querySelector(":scope > header")?.textContent).toContain("确认下单");
    expect(dialog!.querySelector(":scope > header")?.textContent).toContain("生成采购单后将发送给供货商确认");
    expect(Array.from(dialog!.querySelectorAll(".purchase-order-checkout-metrics dt")).map((node) => node.textContent)).toEqual([
      "比价策略",
      "供应商数",
      "下单SKU数量"
    ]);
    expect(dialog!.querySelector(".purchase-order-delivery-section")?.textContent).toContain("运输信息");
    const contactSelect = dialog!.querySelector<HTMLSelectElement>(".purchase-order-delivery-section select");
    expect(contactSelect?.options.length).toBe(2);
    expect(contactSelect?.options[0].textContent).toBe("王经理");
    contactSelect!.value = "12";
    contactSelect!.dispatchEvent(new Event("change", { bubbles: true }));
    await flushPromises();
    expect(dialog!.querySelector<HTMLInputElement>('input[placeholder="请输入联系电话"]')?.value).toBe("15241256006");
    expect(dialog!.querySelector<HTMLInputElement>('input[placeholder="请输入联系邮箱"]')?.value).toBe("zhang@example.com");
    expect(dialog!.textContent).not.toContain("包装方式");
    expect(dialog!.textContent).not.toContain("交付备注");
    expect(dialog!.querySelector(".purchase-order-supplier-summary")?.textContent).toContain("供货商入选");
    expect(dialog!.querySelector(".purchase-order-supply-card")).not.toBeNull();
    expect(Array.from(dialog!.querySelectorAll(".purchase-order-checkout-bill > span")).map((node) => node.textContent?.replace(/\s+/g, " ").trim())).toEqual([
      expect.stringContaining("报价价格"),
      expect.stringContaining("成本价格"),
      expect.stringContaining("补给费用"),
      expect.stringContaining("利润")
    ]);
  });

  it("saves comparison settings without showing a success banner", async () => {
    const wrapper = await mountPage();
    await wrapper.findAll(".compare-row-select input")[0].setValue(false);
    await wrapper.get('button[aria-label="保存比价设置"]').trigger("click");
    await flushPromises();
    expect(saveFoodComparisonItems).toHaveBeenCalledWith(55, expect.objectContaining({
      items: expect.arrayContaining([
        expect.objectContaining({ demandItemId: 1, quoteItemId: 91, requestedQuantity: 100, quotedQuantity: 100, unitPrice: 3.25 }),
        expect.objectContaining({ demandItemId: 2, quoteItemId: 92, requestedQuantity: 10, quotedQuantity: 10, unitPrice: 4.5 })
      ])
    }));
    expect(saveFoodComparisonSettings).toHaveBeenCalledWith(55, expect.objectContaining({
      selectedDemandItemIds: [2]
    }));
    expect(wrapper.text()).not.toContain("比价设置已保存");
  });

  it("edits requested quantity, quoted quantity and unit price with a live requested-times-price subtotal", async () => {
    const wrapper = await mountPage();
    const firstRow = wrapper.findAll(".data-table-row")[0];

    await firstRow.get(".comparison-requested-quantity").setValue("12");
    await firstRow.get(".comparison-quoted-quantity").setValue("11");
    await firstRow.get(".comparison-unit-price").setValue("4.5");

    expect(firstRow.get(".comparison-subtotal").text()).toBe("54.00");
    await wrapper.get('button[aria-label="保存比价设置"]').trigger("click");
    await flushPromises();
    expect(saveFoodComparisonItems).toHaveBeenCalledWith(55, expect.objectContaining({
      items: expect.arrayContaining([
        expect.objectContaining({ demandItemId: 1, quoteItemId: 91, requestedQuantity: 12, quotedQuantity: 11, unitPrice: 4.5 })
      ])
    }));
  });

  it("marks the selected traffic time node green by its persisted index", async () => {
    vi.mocked(getFoodComparison).mockResolvedValue(comparison({
      ...settings,
      trafficServiceJson: JSON.stringify({
        selectedNodeIndex: 1,
        serviceNodes: [
          { startTime: "2026-07-20T09:00:00", endTime: "2026-07-20T11:00:00" },
          { startTime: "2026-07-20T12:00:00", endTime: "2026-07-20T15:00:00" },
          { startTime: "2026-07-20T16:00:00", endTime: "2026-07-20T18:00:00" }
        ]
      })
    }));
    const wrapper = await mountPage();
    await wrapper.get('button[aria-label="确认下单"]').trigger("click");
    await flushPromises();

    const nodes = Array.from(document.querySelectorAll(".purchase-order-barge-card__route em"));
    expect(nodes).toHaveLength(3);
    expect(nodes[0].classList.contains("is-selected")).toBe(false);
    expect(nodes[1].classList.contains("is-selected")).toBe(true);
  });

  it("locks transport settings and saving after the comparison is ordered", async () => {
    const source = comparison();
    source.demand.status = "ORDERED";
    vi.mocked(getFoodComparison).mockResolvedValue(source);
    const wrapper = await mountPage();

    expect(wrapper.get('.compare-supply-card select').attributes("disabled")).toBeDefined();
    expect(wrapper.get('button[aria-label="选择驳船"]').attributes("disabled")).toBeDefined();
    expect(wrapper.findAll('.compare-fixed-fee-bar input').every((input) => input.attributes("disabled") !== undefined)).toBe(true);
    expect(wrapper.get('.compare-markup-field input').attributes("disabled")).toBeDefined();
    expect(wrapper.get('button[aria-label="保存比价设置"]').attributes("disabled")).toBeDefined();
    expect(wrapper.get('button[aria-label="导入比价报价表"]').attributes("disabled")).toBeDefined();

    await wrapper.get('button[aria-label="保存比价设置"]').trigger("click");
    expect(saveFoodComparisonSettings).not.toHaveBeenCalled();
  });

  it("restores the saved row selection after reloading the comparison", async () => {
    vi.mocked(getFoodComparison).mockResolvedValue(comparison({
      ...settings,
      selectedDemandItemIds: [2]
    }));
    const wrapper = await mountPage();
    const checkboxes = wrapper.findAll(".compare-row-select input");
    expect((checkboxes[0].element as HTMLInputElement).checked).toBe(false);
    expect((checkboxes[1].element as HTMLInputElement).checked).toBe(true);
    expect(wrapper.findAll(".data-table-row")[0].classes()).toContain("is-compare-deselected");
    expect(wrapper.findAll(".strategy-choice")[0].text()).toContain("1/2");
  });

  it("matches material strategy labels, supplier linkage and single-supplier concentration", async () => {
    const source = comparison();
    source.submittedSupplierCount = 3;
    source.items[0].quotes.push(
      { ...source.items[0].quotes[0], quoteItemId: 101, quoteId: 41, supplierCompanyId: 8, supplierName: "供货商B", unitPrice: 3.5, amount: 350, lowestPrice: false },
      { ...source.items[0].quotes[0], quoteItemId: 111, quoteId: 51, supplierCompanyId: 9, supplierName: "供货商C", unitPrice: 3.6, amount: 360, lowestPrice: false }
    );
    source.items[1].quotes[0].lowestPrice = false;
    source.items[1].quotes.push(
      { ...source.items[1].quotes[0], quoteItemId: 102, quoteId: 41, supplierCompanyId: 8, supplierName: "供货商B", unitPrice: 4, amount: 40, lowestPrice: true },
      { ...source.items[1].quotes[0], quoteItemId: 112, quoteId: 51, supplierCompanyId: 9, supplierName: "供货商C", unitPrice: 5, amount: 50, lowestPrice: false }
    );
    vi.mocked(getFoodComparison).mockResolvedValue(source);
    const wrapper = await mountPage();
    const cards = wrapper.findAll(".strategy-choice");
    expect(cards[0].text()).toContain("最低混供");
    expect(cards[1].text()).toContain("集中采购");

    const mixedSupplierButtons = cards[0].findAll(".strategy-supplier-list button");
    expect(mixedSupplierButtons.map((item) => item.text())).toEqual(expect.arrayContaining(["供货商A325.00", "供货商B40.00"]));
    expect(mixedSupplierButtons.every((item) => !item.attributes("disabled"))).toBe(true);
    await mixedSupplierButtons.find((item) => item.text().includes("供货商B"))!.trigger("click");
    await flushPromises();
    expect(wrapper.findAll(".data-table-row")).toHaveLength(1);
    expect(wrapper.find(".data-table-row").text()).toContain("生姜");

    await cards[0].get(".strategy-choice-main").trigger("click");
    await flushPromises();
    expect(wrapper.findAll(".data-table-row")).toHaveLength(2);

    const concentratedSupplierButtons = cards[1].findAll(".strategy-supplier-list button");
    expect(concentratedSupplierButtons).toHaveLength(1);
    expect(concentratedSupplierButtons[0].text()).toContain("供货商A");
    await cards[1].get(".strategy-choice-main").trigger("click");
    await confirmOrder(wrapper);
    expect(createFoodOrder).toHaveBeenCalledWith(55, "SINGLE_SUPPLIER", 7, [
      { demandItemId: 1, quoteItemId: 91 },
      { demandItemId: 2, quoteItemId: 92 }
    ], false, expect.objectContaining({
      requiredDeliveryTime: "2026-07-20T10:00",
      deliveryContactName: "王经理",
      deliveryContactPhone: "15300800761"
    }));
  });

  it("reselects the live lowest supplier after a price edit and exports the new selection", async () => {
    const source = comparison();
    source.items[0].quotes.push({
      ...source.items[0].quotes[0],
      quoteItemId: 101,
      quoteId: 41,
      supplierCompanyId: 8,
      supplierName: "供货商B",
      unitPrice: 3.5,
      amount: 350,
      lowestPrice: false
    });
    vi.mocked(getFoodComparison).mockResolvedValue(source);
    const wrapper = await mountPage();

    await wrapper.findAll(".comparison-unit-price")[0].setValue("4");
    await flushPromises();

    expect((wrapper.findAll(".comparison-unit-price")[0].element as HTMLInputElement).value).toBe("3.5");
    await wrapper.get('button[aria-label="导出比价报价表"]').trigger("click");
    await flushPromises();
    expect(saveFoodComparisonItems).toHaveBeenCalledWith(55, expect.objectContaining({
      items: expect.arrayContaining([
        expect.objectContaining({ quoteItemId: 91, unitPrice: 4 }),
        expect.objectContaining({ quoteItemId: 101, unitPrice: 3.5 })
      ])
    }));
    expect(exportFoodComparison).toHaveBeenCalledWith(55, [101, 92]);
  });
});
