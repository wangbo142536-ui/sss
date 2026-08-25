// @ts-nocheck -- Source contracts are verified in Vitest's Node environment.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";
import catalogSource from "@/modules/shopManagement/components/ShopProductCatalog.vue?raw";
import editorSource from "@/modules/shopManagement/components/ShopProductCatalogEditor.vue?raw";

const catalogCss = readFileSync(resolve(process.cwd(), "src/modules/shopManagement/styles/shop-product-catalog.css"), "utf8");

describe("shop classification filter and product tags", () => {
  it("opens in catalog mode and exposes a material/food category hierarchy", () => {
    expect(workbenchSource).toContain('ref<"list" | "catalog">("catalog")');
    expect(workbenchSource).toContain("shopProductClassificationFilter");
    expect(workbenchSource).toContain("<optgroup");
    expect(workbenchSource).toContain("shopProductCategoryNameFilter");
    expect(workbenchSource).toContain("categoryName: shopProductCategoryNameFilter.value");
  });

  it("persists editable tags from list and catalog editors", () => {
    expect(workbenchSource).toContain("productTags: string[]");
    expect(workbenchSource).toContain("productTags: row.productTags");
    expect(workbenchSource).toContain("shop-sku-expanded__tags");
    expect(editorSource).toContain("draft.productTags");
    expect(editorSource).toContain("添加商品标签");
  });

  it("renders compact tags at the top-right of catalog SKU cards", () => {
    expect(catalogSource).toContain("visibleProductTags(row)");
    expect(catalogSource).toContain("hiddenProductTagCount(row)");
    expect(catalogSource).toContain("shop-product-catalog__sku-tags");
    expect(catalogCss).toContain(".shop-product-catalog__sku-tags");
    expect(catalogCss).toMatch(/\.shop-product-catalog__sku-tags\s*\{[\s\S]*?position:\s*absolute;[\s\S]*?top:/);
  });
});
