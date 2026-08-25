import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import MaterialComparisonStrategyDialog from "../components/MaterialComparisonStrategyDialog.vue";

const items = [
  { demandItemId: 201, productName: "船用荧光笔", impaCode: "470672", specification: "橙色", candidates: [] },
  { demandItemId: 202, productName: "斜口钳", impaCode: "611706", specification: "180MM", candidates: [] }
];

afterEach(() => {
  document.body.innerHTML = "";
});

describe("MaterialComparisonStrategyDialog", () => {
  it("defaults the lowest mixed plan to three suppliers", () => {
    const wrapper = mount(MaterialComparisonStrategyDialog, {
      attachTo: document.body,
      props: { open: true, items },
      global: { stubs: { Teleport: true } }
    });

    const presetButtons = wrapper.findAll(".material-strategy-supplier-count > button");
    expect(presetButtons[1].classes()).toContain("active");
    const customCount = wrapper.find('.material-strategy-supplier-count input[type="number"]').element as HTMLInputElement;
    expect(customCount.value).toBe("3");
    wrapper.unmount();
  });

  it("only exposes product price and merchant quality strategies", () => {
    const wrapper = mount(MaterialComparisonStrategyDialog, {
      attachTo: document.body,
      props: { open: true, items },
      global: { stubs: { Teleport: true } }
    });

    expect(wrapper.text()).toContain("价格低");
    expect(wrapper.text()).toContain("质量高");
    expect(wrapper.text()).toContain("质量依据商家商品标签");
    expect(wrapper.text()).not.toContain("运输快");
    wrapper.unmount();
  });

  it("requires one objective and submits supplier count plus core products", async () => {
    const wrapper = mount(MaterialComparisonStrategyDialog, {
      attachTo: document.body,
      props: { open: true, items },
      global: { stubs: { Teleport: true } }
    });
    const objectiveChecks = wrapper.findAll('.material-strategy-objective input[type="checkbox"]');
    await objectiveChecks[0].setValue(false);
    await objectiveChecks[1].setValue(false);
    expect(wrapper.text()).toContain("至少保留一项");

    await objectiveChecks[0].setValue(true);
    const customCount = wrapper.find('.material-strategy-supplier-count input[type="number"]');
    await customCount.setValue("4");
    const coreChecks = wrapper.findAll('.material-strategy-core-list input[type="checkbox"]');
    await coreChecks[1].setValue(true);
    await wrapper.find(".material-strategy-primary").trigger("click");

    expect(wrapper.emitted("apply")?.[0]?.[0]).toMatchObject({
      mixedSupplierCount: 4,
      priceEnabled: true,
      qualityEnabled: false,
      coreDemandItemIds: [202]
    });
    wrapper.unmount();
  });
});
