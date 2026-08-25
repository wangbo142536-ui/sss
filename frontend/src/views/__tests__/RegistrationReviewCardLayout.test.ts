// @ts-nocheck -- Source contract for registration review card layout and retained actions.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const page = readFileSync(resolve(process.cwd(), "src/views/WorkbenchPage.vue"), "utf8");
const styles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");
const registrationFilePreviewStart = page.indexOf("const openRegistrationFile");
const registrationFilePreviewEnd = page.indexOf("const getRegistrationFileLinks", registrationFilePreviewStart);
const registrationFilePreview = page.slice(registrationFilePreviewStart, registrationFilePreviewEnd);

describe("Registration review card layout", () => {
  const start = page.indexOf('v-else-if="pageKey === \'registrations\'"');
  const end = page.indexOf('v-else-if="pageKey === \'companyMembers\'"', start);
  const section = page.slice(start, end);

  it("服务商审核使用与服务商管理一致的玻璃卡片列表而不是表格", () => {
    expect(section).toContain('class="registration-card-list"');
    expect(section).toContain('class="registration-review-card"');
    expect(section).not.toContain("<DataTable");
    expect(styles).toMatch(/\.registration-review-card\s*\{[\s\S]*?background:\s*var\(--glass-card-bg\);[\s\S]*?backdrop-filter:\s*blur\(18px\)/);
    expect(styles).toMatch(/\.registration-card-list\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/);
  });

  it("卡片作为静态容器直接展示全部必要字段且无展开死代码", () => {
    expect(section).not.toContain('role="button"');
    expect(section).not.toContain('tabindex="0"');
    expect(section).not.toContain("aria-expanded");
    expect(section).not.toContain("toggleRegistrationDetail");
    expect(section).not.toContain("expandedRegistrationId");
    expect(page).not.toContain("const expandedRegistrationId");
    expect(page).not.toContain("const toggleRegistrationDetail");
    expect(section).toContain('t("registration.contactName")');
    expect(section).toContain('t("registration.phone")');
    expect(section).toContain('t("registration.email")');
    expect(section).toContain('t("registration.submittedAt")');
    expect(section).toContain('t("registration.account")');
    expect(section).not.toContain('t("registration.accountStatus")');
    expect(section).not.toContain('t("registration.companyStatus")');
    expect(section).toContain('v-if="row.rejectReason"');
  });

  it("两列信息区按联系人电话、邮箱、账号、提交时间排列，资质文件独占整行", () => {
    const fieldMarkers = [
      't("registration.contactName")',
      't("registration.phone")',
      't("registration.email")',
      't("registration.account")',
      't("registration.submittedAt")',
      'class="registration-review-card__files"'
    ];
    const fieldIndexes = fieldMarkers.map((marker) => section.indexOf(marker));
    expect(fieldIndexes.every((index) => index >= 0)).toBe(true);
    expect(fieldIndexes).toEqual([...fieldIndexes].sort((left, right) => left - right));
    expect(section).toContain('class="registration-review-card__contact"');
    expect(section).toContain('class="registration-review-card__contact-value"');
    expect(styles).toMatch(/\.registration-review-card__info-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/);
    expect(styles).toMatch(/\.registration-review-card__files\s*\{[\s\S]*?grid-column:\s*1\s*\/\s*-1/);
  });

  it("头部直接展示供应服务范围，且不再展示供应服务商类型", () => {
    expect(section).toContain('class="registration-review-card__service-line"');
    expect(section).toContain('t("registration.supplierServices")');
    expect(section).toContain('.join("、") || "-"');
    expect(section).not.toContain("getRegistrationCompanyTypeLabel");
    expect(section).not.toContain('class="registration-review-card__service-scope"');
    expect(section).not.toContain("member-role-chips");
  });

  it("标题可截断且状态徽标固定右对齐不被挤压", () => {
    expect(styles).toMatch(/\.registration-review-card__identity > div:first-child > strong\s*\{[\s\S]*?flex:\s*1 1 auto;[\s\S]*?min-width:\s*0;[\s\S]*?text-overflow:\s*ellipsis;/);
    expect(styles).toMatch(/\.registration-review-card__identity > div:first-child \.status-badge\s*\{[\s\S]*?flex:\s*0 0 auto;[\s\S]*?margin-left:\s*auto;/);
    expect(section).toContain('class="registration-review-card__service-line"');
  });

  it("资质文件位于字段网格且直接可打开", () => {
    expect(section).toContain('class="registration-review-card__files"');
    expect(section).not.toContain('<section class="registration-review-card__files"');
    expect(section).toContain('v-for="file in getRegistrationFileLinks(row)"');
    expect(section).toContain('@click="openRegistrationFile(file)"');
    expect(section).toContain('class="registration-review-card__files-empty"');
  });

  it("资质文件复用图片预览弹窗且不再打开新窗口或触发下载", () => {
    expect(registrationFilePreview).toContain("URL.createObjectURL(blob)");
    expect(registrationFilePreview).toContain("previewTitle.value");
    expect(registrationFilePreview).toContain("previewImages.value");
    expect(registrationFilePreview).toContain("previewAttributes.value");
    expect(registrationFilePreview).toContain('previewShopSkuRowId.value = ""');
    expect(registrationFilePreview).toContain("previewOpen.value = true");
    expect(registrationFilePreview).not.toContain("window.open");
    expect(registrationFilePreview).not.toContain('document.createElement("a")');
    expect(registrationFilePreview).not.toContain(".download");
  });

  it("资质预览 object URL 在关闭、重复预览和卸载时独立释放", () => {
    expect(page).toContain("let registrationPreviewObjectUrl");
    expect(page).toContain("const revokeRegistrationPreviewObjectUrl");
    expect(registrationFilePreview).toContain("revokeRegistrationPreviewObjectUrl()");
    expect(page).toMatch(/const closeImagePreview[\s\S]*?revokeRegistrationPreviewObjectUrl\(\)/);
    expect(page).toMatch(/onUnmounted\([\s\S]*?revokeRegistrationPreviewObjectUrl\(\)/);
    expect(page).toContain("URL.revokeObjectURL(registrationPreviewObjectUrl)");
  });

  it("历史路径型联系人不会原样显示", () => {
    expect(section).toContain("formatRegistrationContactName(row.contactName)");
    expect(page).toMatch(/const formatRegistrationContactName[\s\S]*?isSuspiciousContactPath/);
  });

  it("审批与驳回动作位于独立底部 footer 并右对齐", () => {
    const footerStart = section.indexOf('class="registration-review-card__footer"');
    const footerEnd = section.indexOf("</footer>", footerStart);
    const footer = section.slice(footerStart, footerEnd);
    expect(footerStart).toBeGreaterThan(section.indexOf('class="registration-review-card__files"'));
    expect(footer).toContain('@click="approveSelectedRegistration(row)"');
    expect(footer).toContain('@click="openRejectDialog(row)"');
    expect(styles).toMatch(/\.registration-review-card__footer\s*\{[\s\S]*?justify-content:\s*flex-end;[\s\S]*?border-top:/);
  });
});
