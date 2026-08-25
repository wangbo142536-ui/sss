// @ts-nocheck -- Source and CSS layout contract test.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const page = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/pages/SupplierEnterpriseViewPage.vue"), "utf8");
const styles = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/styles/supplier-directory.css"), "utf8");

describe("SupplierEnterpriseViewPage profile layout", () => {
  it("企业名与联系资料复用目录卡片脱敏逻辑且无障碍文本不泄露原值", () => {
    expect(page).toContain("maskSupplierName");
    expect(page).toContain("maskSupplierContactName");
    expect(page).toContain("maskSupplierPhone");
    expect(page).toContain("maskSupplierEmail");
    expect(page).toContain("<h1>{{ maskedSupplierName }}</h1>");
    expect(page).toContain(':alt="`${maskedSupplierName}标志`"');
    expect(page).toContain("<strong>{{ maskedContactName }}</strong>");
    expect(page).toContain("<strong>{{ maskedContactPhone }}</strong>");
    expect(page).toContain("<strong>{{ maskedContactEmail }}</strong>");
    expect(page).not.toContain("<h1>{{ supplier.name }}</h1>");
    expect(page).not.toContain(':alt="`${supplier.name}标志`"');
  });

  it("信用代码紧跟脱敏企业名且空值固定展示十位星号", () => {
    expect(page).toMatch(/<h1>\{\{ maskedSupplierName \}\}<\/h1>[\s\S]*?<small aria-label="社会信用代码已隐藏">\*{10}<\/small>/);
    expect(page).not.toContain("统一社会信用代码：");
    expect(page).not.toContain("supplier.creditCode");
  });

  it("状态右对齐，六项企业信息保持同一行", () => {
    expect(styles).toMatch(/\.supplier-enterprise-profile__title\s*\{[\s\S]*?justify-content:\s*space-between/);
    expect(styles).toMatch(/\.supplier-enterprise-profile__facts\s*\{[\s\S]*?grid-template-columns:\s*repeat\(6,\s*minmax\(0,\s*1fr\)\)/);
    expect(page.match(/supplier-enterprise-profile__facts/g)).toHaveLength(1);
  });

  it("详情评价与目录卡片一致使用五颗星，暂无评价时保留五颗灰星", () => {
    expect(page).toContain('class="supplier-postcard__metric-stars supplier-enterprise-profile__rating-stars"');
    expect(page).toContain('v-for="point in 5"');
    expect(page).toContain("supplier.evaluationCount > 0 && point <= Math.round(Number(supplier.averageRating || 0))");
    expect(page).not.toContain('supplier.evaluationCount ? `${supplier.averageRating?.toFixed(1)}分` : "暂无评价"');
    expect(styles).toMatch(/\.supplier-enterprise-profile__facts\s+\.supplier-enterprise-profile__rating-stars\s*\{[\s\S]*?display:\s*inline-flex;[\s\S]*?flex-wrap:\s*nowrap;[\s\S]*?width:\s*max-content;/);
    expect(styles).toMatch(/\.supplier-enterprise-profile__facts\s+\.supplier-enterprise-profile__rating-stars\s+i\s*\{[\s\S]*?flex:\s*0\s+0\s+auto;/);
  });

  it("状态右侧始终展示开关，但仅平台管理员可操作真实启停", () => {
    expect(page).not.toMatch(/supplier-enterprise-profile__status[\s\S]*?<em/);
    expect(page).toContain('role="switch"');
    expect(page).toContain(':aria-checked="supplier.status === \'ACTIVE\'"');
    expect(page).toContain(':aria-busy="statusSaving"');
    expect(page).not.toContain('v-if="canManageStatus"');
    expect(page).toContain(':disabled="statusSaving || !canManageStatus"');
    expect(page).toContain("仅平台管理员可修改");
    expect(page).toContain("session?.user?.userType");
    expect(page).toContain("!canManageStatus.value");
    expect(page).toContain("updateSupplierDirectoryStatus");
    expect(page).toContain("supplier.value.status = previousStatus");
    expect(styles).toMatch(/\.supplier-enterprise-status-switch\s*\{[\s\S]*?border-radius:\s*999px/);
    expect(styles).toMatch(/\.supplier-enterprise-status-switch\.is-disabled\s*\{[\s\S]*?background:\s*#(?:[0-9a-f]{6})/i);
  });

  it("用户可见导航文案统一为服务商管理和企业信息", () => {
    expect(page).toContain("<span>服务商管理</span>");
    expect(page).toContain("<span>企业信息</span>");
    expect(page).not.toContain("<span>供货商信息</span>");
    expect(page).not.toContain("<span>企业管理</span>");
  });

  it("企业资料与完整商品名册统一由整页纵向滚动承载", () => {
    expect(page).toMatch(/class="supplier-enterprise-page"[\s\S]*?tabindex="0"[\s\S]*?aria-label="供货商企业与商品名册"/);
    expect(page).not.toContain("supplier-enterprise-products__catalog-scroll");
    expect(styles).toMatch(/\.supplier-enterprise-page\s*\{[\s\S]*?height:\s*100%;[\s\S]*?overflow-x:\s*hidden;[\s\S]*?overflow-y:\s*auto;[\s\S]*?overscroll-behavior:\s*contain/);
    expect(styles).toMatch(/\.supplier-enterprise-products\s*\{[\s\S]*?flex:\s*0\s+0\s+auto;[\s\S]*?overflow:\s*visible/);
  });

  it("商品少或为空时取消双层固定高度，商品多时由真实内容自然撑开", () => {
    expect(styles).toMatch(/\.supplier-enterprise-products\s*\{[\s\S]*?min-height:\s*0;/);
    expect(styles).toMatch(/\.supplier-enterprise-products\s*>\s*\.shop-product-catalog\s*\{[\s\S]*?min-height:\s*0;[\s\S]*?height:\s*auto;/);
    expect(styles).not.toMatch(/\.supplier-enterprise-products\s*\{[\s\S]*?min-height:\s*520px;/);
  });

  it("商品说明区替换为与企业管理一致的四项检索区域", () => {
    expect(page).not.toContain("当前为供货商企业的只读商品名册");
    expect(page).not.toContain("supplier-enterprise-products__heading");
    expect(page).toContain("supplier-enterprise-products__filters");
    expect(page).toContain('v-model="filterDraft.keyword"');
    expect(page).toContain('v-model="filterDraft.classification"');
    expect(page).toContain('v-model="filterDraft.codeStatus"');
    expect(page).toContain('v-model="filterDraft.shelfStatus"');
    expect(page).toContain(':rows="filteredProducts"');
    expect(page).not.toContain('filteredProducts.length.toLocaleString() }} / {{ products.length.toLocaleString()');
  });

  it("服务商商品名册以只读详情模式复用商品弹窗", () => {
    expect(page).toContain('<ShopProductCatalog');
    expect(page).toContain(':rows="filteredProducts"');
    expect(page).toContain('readonly');
    expect(page).toContain(':quality-editable="canManageStatus"');
  });

  it("企业证书区不展示内部资质类型代码和冗余说明", () => {
    expect(page).not.toContain("展示该企业注册及企业资质文件，仅供查阅。");
    expect(page).not.toContain("item.qualificationType");
    expect(page).not.toContain("ENTERPRISE_REGISTRATION_QUALIFICA");
    expect(page).toContain("item.title || item.fileName");
  });
});
