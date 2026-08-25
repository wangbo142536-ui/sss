import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import MaterialStrategyIcon from "../components/MaterialStrategyIcon.vue";

describe("MaterialStrategyIcon", () => {
  it.each([
    ["core", "核心商品", "material-strategy-icon__star"],
    ["price", "价格低", "material-strategy-icon__price"],
    ["quality", "质量好", "material-strategy-icon__thumb"],
    ["unmatched", "未匹配商品", "material-strategy-icon__unmatched-face"],
  ] as const)("renders a clear %s semantic glyph with accessible label", (type, label, markerClass) => {
    const wrapper = mount(MaterialStrategyIcon, { props: { type, label } });
    expect(wrapper.attributes("aria-label")).toBe(label);
    expect(wrapper.classes()).toContain(`is-${type}`);
    expect(wrapper.find("svg").exists()).toBe(true);
    expect(wrapper.find(`.${markerClass}`).exists()).toBe(true);
    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("uses a direct yen and down-arrow symbol for the low-price meaning", () => {
    const wrapper = mount(MaterialStrategyIcon, { props: { type: "price", label: "价格低" } });
    expect(wrapper.text()).toContain("¥");
    expect(wrapper.find(".material-strategy-icon__down").exists()).toBe(true);
  });

  it("uses an unhappy face and tear for the unmatched meaning", () => {
    const wrapper = mount(MaterialStrategyIcon, { props: { type: "unmatched", label: "未匹配商品" } });
    expect(wrapper.find(".material-strategy-icon__unmatched-face").exists()).toBe(true);
    expect(wrapper.find(".material-strategy-icon__tear").exists()).toBe(true);
  });
});
