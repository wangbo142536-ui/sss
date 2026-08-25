// @ts-nocheck -- Vitest runs this CSS contract in Node; the app does not ship Node typings.
import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

const workbenchStyles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("Workbench product catalog integration", () => {
  it("只做模式切换和真实全量SKU接线，保留原列表", () => {
    expect(workbenchSource).toContain("ShopProductCatalog");
    expect(workbenchSource).toContain('shopProductViewMode = ref<"list" | "catalog">("catalog")');
    expect(workbenchSource).toContain("fetchAllSavedShopSkuRows()");
    expect(workbenchSource).toContain("filteredShopCatalogRows");
    expect(workbenchSource).toContain("shopProductViewMode === 'list'");
    expect(workbenchSource).toContain("shopProductViewMode === 'catalog'");
    expect(workbenchSource).toContain("icon=\"BookOpen\"");
    expect(workbenchSource).toContain("icon=\"List\"");
    expect(workbenchSource).toContain("updateShopSku");
    expect(workbenchSource).toContain("saveShopCatalogSku");
    expect(workbenchSource).toContain(':save-sku="saveShopCatalogSku"');
    expect(workbenchSource).toContain(':category-options="getShopCategoryOptions"');
    expect(workbenchSource).toContain("'is-catalog-mode': shopProductViewMode === 'catalog'");
    expect(workbenchSource).not.toContain("mockShopCatalog");
  });

  it("名册模式使用产品区域自身纵向滚动，不裁掉底部书架和SKU卡片", () => {
    expect(workbenchStyles).toMatch(
      /shop-management-pane--products\.is-catalog-mode[\s\S]*?overflow-x:\s*hidden[\s\S]*?overflow-y:\s*auto/
    );
    expect(workbenchStyles).toMatch(
      /shop-management-pane--products\.is-catalog-mode\s*>\s*\.shop-product-catalog\s*\{[\s\S]*?flex:\s*1\s+0\s+auto;[\s\S]*?min-height:\s*100%/
    );
  });

  it("企业管理全屏时改由整屏容器滚动并保持顶部工具栏可退出", () => {
    expect(workbenchSource).toContain('<Teleport to="body" :disabled="!shopListFullscreen">');
    expect(workbenchStyles).toMatch(
      /\.shop-list-card\.is-section-fullscreen\s*\{[\s\S]*?overflow-x:\s*hidden;[\s\S]*?overflow-y:\s*auto;[\s\S]*?overscroll-behavior:\s*contain/
    );
    expect(workbenchStyles).toMatch(
      /\.shop-list-card\.is-section-fullscreen \.shop-management-pane--products\.is-catalog-mode\s*\{[\s\S]*?flex:\s*0\s+0\s+auto;[\s\S]*?overflow:\s*visible/
    );
    expect(workbenchStyles).toMatch(
      /\.shop-list-card\.is-section-fullscreen \.shop-management-bar\s*\{[\s\S]*?position:\s*sticky;[\s\S]*?top:\s*0/
    );
  });

  it("商品工具栏隐藏异常导出和刷新两个入口", () => {
    expect(workbenchSource).not.toContain('label="导出导入异常"');
    expect(workbenchSource).not.toContain(':loading="shopSkuListLoading" @click="refreshShopSkuList"');
  });

  it("商品管理不提供新增联系人入口，新增联系人只保留在联系人管理", () => {
    const productToolbar = workbenchSource.slice(
      workbenchSource.indexOf("v-if=\"shopManagementTab === 'products'\""),
      workbenchSource.indexOf("v-else-if=\"shopManagementTab === 'qualifications'\"")
    );
    const contactToolbar = workbenchSource.slice(
      workbenchSource.indexOf("v-else-if=\"shopManagementTab === 'contacts'\""),
      workbenchSource.indexOf("v-else-if=\"shopManagementTab === 'vessels'\"")
    );
    expect(productToolbar).not.toContain("openCompanyContactCreateFromAnyTab");
    expect(contactToolbar).toContain('@click="addCompanyContact"');
  });

  it("名册全量分页不继承列表筛选总数，切换物料和伙食不会截断数据", () => {
    expect(workbenchSource).toContain("let total: number | null = null");
    expect(workbenchSource).not.toContain("let total = shopSkuTotal.value || 0");
    expect(workbenchSource).toMatch(
      /fetchAllSavedShopSkuRows[\s\S]*?listShopSkus\(\{\s*page,\s*size,\s*shelfStatus:\s*shopProductShelfStatusFilter\.value\s*\}\)/
    );
    expect(workbenchSource).toContain('v-model="shopProductClassificationFilter" @change="searchShopSkus"');
  });

  it("商品管理默认只读取在架商品，用户仍可显式切换查看全部或下架数据", () => {
    expect(workbenchSource).toContain('const shopProductShelfStatusFilter = ref("ON_SHELF")');
    expect(workbenchSource).toMatch(
      /fetchAllSavedShopSkuRows[\s\S]*?listShopSkus\(\{[\s\S]*?shelfStatus:\s*shopProductShelfStatusFilter\.value/
    );
    expect(workbenchSource).toMatch(
      /resetShopFilters[\s\S]*?shopProductShelfStatusFilter\.value\s*=\s*"ON_SHELF"/
    );
    expect(workbenchSource).toContain('<option value="">{{ t("common.all") }}</option>');
    expect(workbenchSource).toContain('<option value="OFF_SHELF">{{ t("page.supplierProducts.statusOffShelf") }}</option>');
  });
});
