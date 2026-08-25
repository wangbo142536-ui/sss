import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import IconButton from "../IconButton.vue";
import { resetIconButtonTooltipRegistry } from "../iconButtonTooltipRegistry";

const rect = (top: number, left: number, width: number, height: number) => ({
  top,
  left,
  width,
  height,
  right: left + width,
  bottom: top + height,
  x: left,
  y: top,
  toJSON: () => ({})
});

describe("IconButton global tooltip", () => {
  beforeEach(() => {
    resetIconButtonTooltipRegistry();
    Object.defineProperty(window, "innerWidth", { configurable: true, value: 320 });
    Object.defineProperty(window, "innerHeight", { configurable: true, value: 240 });
    vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function (this: HTMLElement) {
      if (this.classList.contains("ui-icon-button-tooltip")) return rect(0, 0, 80, 24) as DOMRect;
      if (this.getAttribute("aria-label") === "顶部按钮") return rect(4, 2, 36, 36) as DOMRect;
      if (this.getAttribute("aria-label") === "第二按钮") return rect(100, 220, 36, 36) as DOMRect;
      return rect(100, 100, 36, 36) as DOMRect;
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    resetIconButtonTooltipRegistry();
    document.body.innerHTML = "";
  });

  it("Teleport到body，默认显示在按钮上方且只保留一个自定义提示", async () => {
    const Host = defineComponent({
      components: { IconButton },
      template: `<div><IconButton icon="Check" label="第一按钮" /><IconButton icon="X" label="第二按钮" /></div>`
    });
    const wrapper = mount(Host, { attachTo: document.body });
    const buttons = wrapper.findAll("button");

    await buttons[0].trigger("mouseenter");
    await wrapper.vm.$nextTick();
    const firstTooltip = document.body.querySelector('[role="tooltip"]') as HTMLElement;
    expect(firstTooltip.textContent).toBe("第一按钮");
    expect(firstTooltip.style.position).toBe("fixed");
    expect(firstTooltip.style.top).toBe("68px");
    expect(firstTooltip.style.left).toBe("78px");
    expect(buttons[0].attributes("title")).toBeUndefined();
    expect(buttons[0].attributes("data-tooltip")).toBeUndefined();
    expect(buttons[0].attributes("aria-describedby")).toBe(firstTooltip.id);

    await buttons[1].trigger("mouseenter");
    await wrapper.vm.$nextTick();
    expect(document.body.querySelectorAll('[role="tooltip"]')).toHaveLength(1);
    expect(document.body.querySelector('[role="tooltip"]')?.textContent).toBe("第二按钮");
    wrapper.unmount();
  });

  it("顶部空间不足时翻转到下方，并在左右保留8px安全距离", async () => {
    const wrapper = mount(IconButton, { attachTo: document.body, props: { icon: "Check", label: "顶部按钮" } });
    const button = wrapper.get("button");
    vi.spyOn(button.element, "matches").mockImplementation((selector) => selector === ":focus-visible");
    await button.trigger("focus");
    await wrapper.vm.$nextTick();

    const tooltip = document.body.querySelector('[role="tooltip"]') as HTMLElement;
    expect(tooltip.dataset.placement).toBe("bottom");
    expect(tooltip.style.top).toBe("48px");
    expect(tooltip.style.left).toBe("8px");

    window.dispatchEvent(new Event("scroll"));
    await wrapper.vm.$nextTick();
    expect(document.body.querySelector('[role="tooltip"]')).toBeNull();
    expect(button.attributes("aria-describedby")).toBeUndefined();
    wrapper.unmount();
  });

  it("不影响click、disabled和loading状态", async () => {
    const wrapper = mount(IconButton, { attachTo: document.body, props: { icon: "Check", label: "保存" } });
    await wrapper.get("button").trigger("click");
    expect(wrapper.emitted("click")).toHaveLength(1);

    await wrapper.setProps({ disabled: true });
    expect(wrapper.get("button").attributes("disabled")).toBeDefined();
    await wrapper.get("button").trigger("click");
    expect(wrapper.emitted("click")).toHaveLength(1);
    await wrapper.setProps({ disabled: false, loading: true });
    expect(wrapper.get("button").attributes("disabled")).toBeDefined();
    await wrapper.get("button").trigger("click");
    expect(wrapper.emitted("click")).toHaveLength(1);
    expect(wrapper.find(".button-spinner").exists()).toBe(true);
    wrapper.unmount();
  });
});
