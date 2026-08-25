import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import BackgroundTaskOverlay from "../components/BackgroundTaskOverlay.vue";

describe("BackgroundTaskOverlay", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("只展示调用方提供的真实阶段、任务上下文和计数", () => {
    const wrapper = mount(BackgroundTaskOverlay, {
      props: {
        visible: true,
        kicker: "后台任务",
        title: "正在分析商品文件",
        fileName: "products.xlsx",
        status: "RUNNING",
        stages: [
          { code: "PARSE", label: "解析工作簿" },
          { code: "MATCH", label: "标准库匹配" }
        ],
        stageIndex: 1,
        progressPercent: 64,
        elapsedSeconds: 12,
        contextItems: ["当前 Sheet：物料", "分片 2 / 4"],
        counters: [
          { label: "已处理", value: "64 / 100" },
          { label: "匹配 / 待确认 / 失败", value: "52 / 10 / 2" }
        ],
        allowBackground: true
      }
    });

    const overlay = document.body.querySelector(".background-task-overlay") as HTMLElement;
    expect(overlay.querySelectorAll(".background-task-overlay__stages li")).toHaveLength(2);
    expect(overlay.textContent).toContain("当前 Sheet：物料");
    expect(overlay.textContent).toContain("分片 2 / 4");
    expect(overlay.textContent).toContain("64 / 100");
    expect(overlay.textContent).toContain("52 / 10 / 2");
    expect(overlay.querySelector('[role="progressbar"]')?.getAttribute("aria-valuenow")).toBe("64");
    expect(overlay.querySelector('[aria-label="转入后台运行"]')).not.toBeNull();
    wrapper.unmount();
  });

  it("后端没有进度协议时使用诚实的不确定态，不生成百分比和伪阶段", () => {
    const wrapper = mount(BackgroundTaskOverlay, {
      props: {
        visible: true,
        kicker: "物料清单导入",
        title: "正在等待服务端分析结果",
        fileName: "materials.xls",
        status: "RUNNING",
        stages: [{ code: "REQUEST", label: "分析文件并生成匹配预览" }],
        stageIndex: 0,
        progressPercent: null,
        elapsedSeconds: 3,
        message: "服务端暂未提供阶段进度，完成后将直接展示预览。"
      }
    });

    const overlay = document.body.querySelector(".background-task-overlay") as HTMLElement;
    expect(overlay.querySelectorAll(".background-task-overlay__stages li")).toHaveLength(1);
    expect(overlay.querySelector('[role="progressbar"]')?.hasAttribute("aria-valuenow")).toBe(false);
    expect(overlay.textContent).toContain("等待服务端返回真实进度");
    expect(overlay.textContent).not.toMatch(/24%|48%|72%|88%/);
    wrapper.unmount();
  });
});
