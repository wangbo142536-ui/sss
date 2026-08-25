import { describe, expect, it } from "vitest";
import { buildShopProductCatalog } from "../composables/useShopProductCatalog";
import type { ShopCatalogSku } from "../types/shopProductCatalog";

function sku(overrides: Partial<ShopCatalogSku>): ShopCatalogSku {
  return {
    id: "sku-default",
    productType: "MATERIAL",
    categoryCode: "1501",
    categoryName: "甲板用品",
    productName: "测试商品",
    supplierSkuCode: "SUP-001",
    platformCode: "150101",
    thumbnail: "",
    imageUrl: "",
    packing: "箱",
    stock: 1,
    unit: "件",
    price: 10,
    currency: "CNY",
    ...overrides
  };
}

describe("buildShopProductCatalog", () => {
  it("按真实商品类型和分类字段生成三级名册并统计全部SKU", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "m-1", productType: "MATERIAL", categoryCode: "1501", categoryName: "甲板用品" }),
      sku({ id: "m-2", productType: "MATERIAL", categoryCode: "1502", categoryName: "文具用品" }),
      sku({ id: "f-1", productType: "FOOD", categoryCode: "F01", categoryName: "新鲜蔬菜" })
    ]);

    expect(catalog).toHaveLength(2);
    expect(catalog[0]).toMatchObject({ key: "MATERIAL", label: "物料", skuCount: 2 });
    expect(catalog[0].categories).toHaveLength(2);
    expect(catalog[1]).toMatchObject({ key: "FOOD", label: "伙食", skuCount: 1 });
  });

  it("同名商品不去重，分类数量按真实SKU行统计", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "marker-1", productName: "荧光笔", supplierSkuCode: "MARKER-A", packing: "10支/盒" }),
      sku({ id: "marker-2", productName: "荧光笔", supplierSkuCode: "MARKER-B", packing: "12支/盒" })
    ]);

    expect(catalog[0].skuCount).toBe(2);
    expect(catalog[0].categories[0].skus.map((item) => item.id)).toEqual(["marker-1", "marker-2"]);
  });

  it("分类缺失时进入未分类书册，不伪造标准库名称", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "unknown", categoryCode: "", categoryName: "" })
    ]);

    expect(catalog[0].categories[0]).toMatchObject({ key: "UNCATEGORIZED", label: "未分类", skuCount: 1 });
  });

  it("未分类书册始终排在已分类书册之前，优先暴露待处理异常", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "classified-b", categoryCode: "63", categoryName: "切削工具" }),
      sku({ id: "uncategorized", categoryCode: "", categoryName: "" }),
      sku({ id: "classified-a", categoryCode: "61", categoryName: "一般作业工具类" })
    ]);

    expect(catalog[0].categories.map((category) => category.key)).toEqual([
      "UNCATEGORIZED",
      "CODE:61",
      "CODE:63"
    ]);
  });

  it("未分类优先，其余书册按大类编码自然升序排列", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "c-25", categoryCode: "25", categoryName: "船舶油漆" }),
      sku({ id: "c-empty", categoryCode: "", categoryName: "" }),
      sku({ id: "c-11", categoryCode: "11", categoryName: "船员后勤" }),
      sku({ id: "c-17", categoryCode: "17", categoryName: "厨房用品" })
    ]);

    expect(catalog[0].categories.map((category) => category.code)).toEqual(["", "11", "17", "25"]);
  });

  it("同名分类有编码和无编码时合并，并优先保留真实分类编码", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "linen-with-code", categoryCode: "15", categoryName: "亚麻布类" }),
      sku({ id: "linen-without-code", categoryCode: "", categoryName: "亚麻布类" })
    ]);

    expect(catalog[0].categories).toHaveLength(1);
    expect(catalog[0].categories[0]).toMatchObject({ code: "15", label: "亚麻布类", skuCount: 2 });
  });

  it("同一标准大类编码存在不同脏名称时仍合并为一本书册", () => {
    const catalog = buildShopProductCatalog([
      sku({ id: "paint-1", categoryCode: "25", categoryName: "船舶油漆" }),
      sku({ id: "paint-2", categoryCode: "25", categoryName: "非保税船用一般物料（料件、垫片）" })
    ]);

    expect(catalog[0].categories).toHaveLength(1);
    expect(catalog[0].categories[0]).toMatchObject({ key: "CODE:25", code: "25", skuCount: 2 });
  });
});
