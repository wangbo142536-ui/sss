import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import ImagePreviewModal from "@/components/ImagePreviewModal.vue";

afterEach(() => {
  document.body.innerHTML = "";
});

describe("Shop product image preview", () => {
  it("大图弹窗把替换图片图标放在图片展示区域内并向外触发替换操作", async () => {
    const wrapper = mount(ImagePreviewModal, {
      props: {
        open: true,
        title: "商品图片",
        images: [{ src: "/images/product.png", alt: "商品图片" }],
        actionLabel: "替换商品图片",
        actionLoading: false
      },
      attachTo: document.body
    });

    const dialog = document.body.querySelector('[role="dialog"]') as HTMLElement;
    expect(dialog).toBeTruthy();
    expect(dialog.querySelector('img[alt="商品图片"]')).toBeTruthy();
    const replaceButton = dialog.querySelector('button[aria-label="替换商品图片"]') as HTMLButtonElement;
    expect(replaceButton).toBeTruthy();
    expect(dialog.querySelector(".image-preview-stage-actions")?.contains(replaceButton)).toBe(true);
    expect(dialog.querySelector("header")?.contains(replaceButton)).toBe(false);
    replaceButton.click();
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("action")).toHaveLength(1);
    wrapper.unmount();
  });

  it("未提供替换操作时保持通用图片预览弹窗原样", () => {
    const wrapper = mount(ImagePreviewModal, {
      props: {
        open: true,
        title: "附件预览",
        images: [{ src: "/images/file.png", alt: "附件" }]
      },
      attachTo: document.body
    });

    expect(document.body.querySelector('button[aria-label="替换商品图片"]')).toBeNull();
    wrapper.unmount();
  });
});
