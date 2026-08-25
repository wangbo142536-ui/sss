import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import ShopSmartImportOverlay from "../components/ShopSmartImportOverlay.vue";
import type { ShopSmartImportJob } from "../types/shopSmartImport";

function buildJob(overrides: Partial<ShopSmartImportJob> = {}): ShopSmartImportJob {
  return {
    jobId: "job-0818",
    batchId: "batch-0818",
    fileName: "中外运推荐价格表.xlsx",
    phase: "ANALYSIS",
    status: "RUNNING",
    stage: "MODEL_RECOGNITION",
    stageIndex: 2,
    stageCount: 5,
    overallPercent: 56,
    stageProcessed: 620,
    stageTotal: 1314,
    currentSheet: "文具",
    currentChunk: 3,
    totalChunks: 12,
    message: "正在识别商品字段与类型",
    errorCode: "",
    retryable: false,
    counts: {
      sheetTotal: 7,
      sheetProcessed: 3,
      itemTotal: 1314,
      itemProcessed: 620,
      materialCount: 552,
      foodCount: 68,
      impaMatchedCount: 500,
      categoryMatchedCount: 80,
      pendingReviewCount: 40,
      imageTotal: 131,
      imageProcessed: 68,
      failedCount: 0
    },
    ...overrides
  };
}

describe("ShopSmartImportOverlay", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("在分析阶段展示5个真实任务节点与实时计数", () => {
    const wrapper = mount(ShopSmartImportOverlay, {
      props: { visible: true, job: buildJob(), elapsedSeconds: 18 }
    });

    const overlay = document.body.querySelector(".shop-smart-import") as HTMLElement;
    expect(overlay.querySelectorAll(".shop-smart-import__stages li")).toHaveLength(5);
    expect(overlay.textContent).toContain("上传文件");
    expect(overlay.textContent).toContain("解析工作簿");
    expect(overlay.textContent).toContain("模型识别");
    expect(overlay.textContent).toContain("标准库匹配");
    expect(overlay.textContent).toContain("生成预览");
    expect(overlay.textContent).toContain("文具");
    expect(overlay.textContent).toContain("620 / 1314");
    expect(overlay.textContent).toContain("物料 552 / 伙食 68");
    expect(overlay.textContent).toContain("图片 68 / 131");
    expect(overlay.querySelector('[aria-label="转入后台运行"]')).not.toBeNull();
    wrapper.unmount();
  });

  it("执行入库时展示4个节点，终态不自动关闭并提供图标操作", async () => {
    const wrapper = mount(ShopSmartImportOverlay, {
      props: {
        visible: true,
        elapsedSeconds: 31,
        job: buildJob({
          phase: "EXECUTION",
          status: "COMPLETED",
          stage: "COUNT_RECONCILIATION",
          stageIndex: 3,
          stageCount: 4,
          overallPercent: 100,
          message: "商品已入库"
        })
      }
    });

    const overlay = document.body.querySelector(".shop-smart-import") as HTMLElement;
    expect(overlay.querySelectorAll(".shop-smart-import__stages li")).toHaveLength(4);
    expect(overlay.textContent).toContain("创建商品记录");
    expect(overlay.textContent).toContain("写入标准库关联");
    expect(overlay.textContent).toContain("保存规格价格图片");
    expect(overlay.textContent).toContain("数量核对完成");
    expect(overlay.querySelector('[aria-label="查看商品列表"]')).not.toBeNull();
    expect(overlay.querySelector('[aria-label="关闭导入进度"]')).not.toBeNull();
    (overlay.querySelector('[aria-label="查看商品列表"]') as HTMLButtonElement).click();
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("view-preview")).toHaveLength(1);
    wrapper.unmount();
  });

  it("部分入库终态使用完成态文案并进入导入结果", () => {
    const wrapper = mount(ShopSmartImportOverlay, {
      props: {
        visible: true,
        elapsedSeconds: 9,
        job: buildJob({
          phase: "EXECUTION",
          status: "PARTIAL",
          stage: "COUNT_RECONCILIATION",
          stageIndex: 3,
          stageCount: 4,
          overallPercent: 100,
          message: "正在核对数量并完成导入"
        })
      }
    });

    const overlay = document.body.querySelector(".shop-smart-import") as HTMLElement;
    expect(overlay.textContent).toContain("已入库可确认商品，待确认商品保留在导入结果中");
    expect(overlay.querySelector('[aria-label="查看导入结果"]')).not.toBeNull();
    expect(overlay.textContent).not.toContain("正在核对数量并完成导入");
    wrapper.unmount();
  });
});
