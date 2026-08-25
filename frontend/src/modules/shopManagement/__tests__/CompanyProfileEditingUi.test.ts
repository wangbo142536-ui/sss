// @ts-nocheck -- Source and CSS contracts are verified in Vitest's Node environment.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

const workbenchStyles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("Company profile editing UI", () => {
  it("企业资料采用单行信息卡，状态右侧依次放编辑、保存和编辑态取消", () => {
    expect(workbenchSource).toContain("const shopProfileEditing = ref(false)");
    expect(workbenchSource).toContain("startShopProfileEdit");
    expect(workbenchSource).toContain("cancelShopProfileEdit");
    expect(workbenchSource).toContain('icon="Pencil"');
    expect(workbenchSource).toContain(':disabled="shopProfileEditing || shopProfileLoading || shopSaving || shopLogoUploading"');
    expect(workbenchSource).toContain('v-if="shopProfileEditing"');
    expect(workbenchSource).toContain('icon="Save"');
    expect(workbenchSource).toContain(':disabled="!shopProfileEditing || shopSaving || shopLogoUploading"');
    expect(workbenchSource).toContain('icon="X"');
    expect(workbenchSource).toContain(':readonly="!shopProfileEditing"');

    expect(workbenchSource).toContain('class="shop-store-identity-row"');
    expect(workbenchSource).toContain('class="shop-store-profile-actions"');
    expect(workbenchSource).toMatch(/shop-store-profile-actions[\s\S]*?<StatusBadge[\s\S]*?icon="Pencil"[\s\S]*?icon="Save"/);

    const actions = workbenchSource.slice(
      workbenchSource.indexOf('<div class="shop-store-profile-actions">'),
      workbenchSource.indexOf('</div>', workbenchSource.indexOf('<div class="shop-store-profile-actions">'))
    );
    expect(actions.indexOf('icon="Pencil"')).toBeLessThan(actions.indexOf('icon="Save"'));
    expect(actions).not.toContain('icon="RefreshCw"');
  });

  it("编辑态覆盖注册资料字段并提供逐项错误反馈", () => {
    expect(workbenchSource).toContain('v-model="shopProfileForm.shopName"');
    expect(workbenchSource).toContain('v-model="shopProfileForm.creditCode"');
    expect(workbenchSource).toContain('v-model="shopProfileForm.contactName"');
    expect(workbenchSource).toContain('v-model="shopProfileForm.contactPhone"');
    expect(workbenchSource).toContain('v-model="shopProfileForm.contactEmail"');
    expect(workbenchSource).toContain("shopProfileFieldErrors");
    expect(workbenchSource).toContain("validateShopProfileForm");
    expect(workbenchStyles).toContain(".shop-store-identity-fields");
    expect(workbenchStyles).toContain(".shop-profile-fields.is-readonly");
  });

  it("企业名右侧展示社会信用代码，联系人与三项指标合计六块同排", () => {
    expect(workbenchSource).toMatch(/shop-store-identity-row[\s\S]*?shopStoreName[\s\S]*?shopCreditCode/);
    expect(workbenchStyles).toMatch(/\.shop-store-facts\s*\{[\s\S]*?grid-template-columns:\s*repeat\(6,\s*minmax\(0,\s*1fr\)\)/);
    expect(workbenchSource).toContain("'shop-store-facts', 'shop-profile-fields'");
    expect(workbenchSource).toContain('class="shop-store-metric-field"');
    expect(workbenchSource).toMatch(/shop-store-metric-field[\s\S]*?<span>\{\{ metric\.label \}\}<\/span>[\s\S]*?<output>\{\{ metric\.value \}\}<\/output>/);
    expect(workbenchStyles).toMatch(/\.shop-store-metric-field\s*\{[\s\S]*?display:\s*grid;[\s\S]*?gap:\s*5px/);
    expect(workbenchStyles).toMatch(/\.shop-store-metric-field output\s*\{[\s\S]*?height:\s*40px;[\s\S]*?border-radius:\s*11px/);
  });

  it("LOGO只在编辑态可选，上传后等待统一保存而不是立即更新企业资料", () => {
    expect(workbenchSource).toContain(':disabled="!shopProfileEditing || shopLogoUploading || shopSaving"');
    const handler = workbenchSource.slice(
      workbenchSource.indexOf("const handleShopLogoChange"),
      workbenchSource.indexOf("const openCompanyQualificationFilePicker")
    );
    expect(handler).not.toContain("updateCompanyProfile(");
    expect(handler).not.toContain("loadShopProfile()");
    expect(handler).toContain('shopNoticeKey.value = "page.supplierProducts.logoUploadPendingSave"');
  });
});
