import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

describe("Workbench product smart import integration", () => {
  it("使用独立智能导入模块且不再使用前端定时假进度", () => {
    expect(workbenchSource).toContain("ShopSmartImportOverlay");
    expect(workbenchSource).toContain("useShopSmartImportJob");
    expect(workbenchSource).toContain("startShopSmartImportAnalysis(file)");
    expect(workbenchSource).toContain("beginShopSmartImportExecution");
    expect(workbenchSource).not.toContain("const shopImportStages");
    expect(workbenchSource).not.toContain("shopImportTimer");
    expect(workbenchSource).not.toContain("previewShopSkuImport(formData)");
  });
});
