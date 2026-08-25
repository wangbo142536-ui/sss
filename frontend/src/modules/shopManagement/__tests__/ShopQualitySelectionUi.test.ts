// @ts-nocheck -- Source/CSS contract test.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import editorSource from "@/modules/shopManagement/components/ShopProductCatalogEditor.vue?raw";
import catalogSource from "@/modules/shopManagement/components/ShopProductCatalog.vue?raw";

const supplierPage = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/pages/SupplierEnterpriseViewPage.vue"), "utf8");
const serviceSource = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/services/supplierDirectoryService.ts"), "utf8");

describe("enterprise introductions and platform quality selection", () => {
  it("shows product introduction and a separate platform QC section", () => {
    expect(editorSource).toContain("产品介绍");
    expect(editorSource).toContain("平台严选");
    expect(editorSource).toContain("检查过程留痕");
    expect(editorSource).toContain("检测报告");
    expect(editorSource).toContain("检测结论");
  });

  it("renders the Maritime Selected badge independently from merchant tags", () => {
    expect(catalogSource).toContain("shop-product-catalog__quality-badge");
    expect(catalogSource).toContain("海事严选");
    expect(catalogSource).toContain("row.productTags");
  });

  it("keeps supplier data read-only while platform administrators can maintain QC", () => {
    expect(supplierPage).toContain("readonly");
    expect(supplierPage).toContain(':quality-editable="canManageStatus"');
    expect(supplierPage).toContain("企业介绍");
    expect(serviceSource).toContain("/quality-selections");
    expect(serviceSource).toContain("/quality-selection");
  });
});
