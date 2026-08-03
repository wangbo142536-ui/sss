import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it } from "vitest";
import { setLanguage } from "@/i18n";
import SupplierDataAnalysisPage from "../pages/SupplierDataAnalysisPage.vue";

describe("SupplierDataAnalysisPage dashboard layout", () => {
  beforeEach(() => {
    setLanguage("zh-CN");
  });

  const mountPage = () => mount(SupplierDataAnalysisPage, {
    global: {
      stubs: {
        WorkbenchLayout: { template: "<main><slot /></main>" }
      }
    }
  });

  it("loads the supplier analysis directly without the removed filter header", async () => {
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.find(".supplier-analytics-page-header").exists()).toBe(false);
    expect(wrapper.find(".supplier-analytics-filter").exists()).toBe(false);
    expect(wrapper.find(".supplier-analytics-demo-banner").exists()).toBe(false);
    expect(wrapper.find(".supplier-analysis-flow").exists()).toBe(false);
    expect(wrapper.find(".supplier-analytics-empty-state").exists()).toBe(false);
    expect(wrapper.find(".supplier-insight-cockpit").exists()).toBe(true);
    expect(wrapper.findAll(".supplier-insight-kpis .supplier-insight-card")).toHaveLength(3);
    expect(wrapper.findAll(".supplier-category-bar")).toHaveLength(3);
    expect(wrapper.findAll(".supplier-insight-main .supplier-insight-action")).toHaveLength(4);
    expect(wrapper.text()).toContain("质量");
    expect(wrapper.text()).not.toContain("转化");
  });

  it("opens a SKU detail view from the TOP list and returns to the cockpit", async () => {
    const wrapper = mountPage();
    await flushPromises();

    await wrapper.get(".supplier-sku-rank-item").trigger("click");

    expect(wrapper.find(".supplier-insight-cockpit").exists()).toBe(false);
    expect(wrapper.find(".supplier-sku-detail-view").exists()).toBe(true);
    expect(wrapper.find(".supplier-analytics-page-header").exists()).toBe(false);
    expect(wrapper.find(".supplier-analysis-flow").exists()).toBe(false);
    expect(wrapper.find(".supplier-sku-detail-table footer").exists()).toBe(false);
    expect(wrapper.get(".supplier-sku-detail-table .supplier-insight-panel-head button").text()).toBe("导出分析结果");

    await wrapper.get(".supplier-sku-detail-back").trigger("click");

    expect(wrapper.find(".supplier-insight-cockpit").exists()).toBe(true);
    expect(wrapper.find(".supplier-sku-detail-view").exists()).toBe(false);
  });
});
