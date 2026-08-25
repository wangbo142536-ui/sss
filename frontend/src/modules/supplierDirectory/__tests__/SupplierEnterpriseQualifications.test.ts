// @ts-nocheck -- source and rendering contract for public supplier qualifications.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const page = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/pages/SupplierEnterpriseViewPage.vue"), "utf8");
const service = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/services/supplierDirectoryService.ts"), "utf8");
const styles = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/styles/supplier-directory.css"), "utf8");

describe("Supplier enterprise public qualifications", () => {
  it("商品板块下方以只读卡片展示企业证书和空态", () => {
    expect(page.indexOf('class="supplier-enterprise-qualifications"')).toBeGreaterThan(page.indexOf('class="supplier-enterprise-products"'));
    expect(page).toContain("企业证书");
    expect(page).toContain("暂无企业证书");
    expect(page).toContain("listSupplierDirectoryQualifications");
    expect(page).not.toContain("saveSupplierQualification");
    expect(styles).toContain(".supplier-enterprise-qualifications");
  });

  it("证书图片使用现有预览弹窗并且接口按目标企业隔离", () => {
    expect(page).toContain("ImagePreviewModal");
    expect(page).toContain("openQualificationPreview");
    expect(service).toContain("/qualifications");
    expect(service).toContain("companyId");
    expect(service).toContain("loadSupplierQualificationImage");
  });
});
