import type { ShopCatalogCategory, ShopCatalogGroup, ShopCatalogSku } from "../types/shopProductCatalog";

const PRODUCT_TYPE_ORDER = ["MATERIAL", "FOOD"];

function clean(value: unknown): string {
  return String(value ?? "").trim();
}

function productTypeLabel(productType: string): string {
  const normalized = clean(productType).toUpperCase();
  if (normalized === "MATERIAL") return "物料";
  if (normalized === "FOOD") return "伙食";
  return clean(productType) || "其他商品";
}

function normalizeCategoryName(value: string): string {
  return clean(value).normalize("NFKC").replace(/\s+/g, "").toLocaleLowerCase("zh-CN");
}

function categoryIdentity(row: ShopCatalogSku, codeByNormalizedName: ReadonlyMap<string, string>): { key: string; code: string; label: string } {
  const rawCode = clean(row.categoryCode || row.category);
  const name = clean(row.categoryName);
  const normalizedName = normalizeCategoryName(name);
  const code = rawCode || codeByNormalizedName.get(normalizedName) || "";
  return {
    key: code ? `CODE:${code.toUpperCase()}` : normalizedName ? `NAME:${normalizedName}` : "UNCATEGORIZED",
    code,
    label: name || code || "未分类"
  };
}

function compareText(left: string, right: string): number {
  return left.localeCompare(right, "zh-CN", { numeric: true, sensitivity: "base" });
}

function compareCategory(left: ShopCatalogCategory, right: ShopCatalogCategory): number {
  if (left.key === "UNCATEGORIZED" && right.key !== "UNCATEGORIZED") return -1;
  if (right.key === "UNCATEGORIZED" && left.key !== "UNCATEGORIZED") return 1;

  const leftCode = clean(left.code);
  const rightCode = clean(right.code);
  if (leftCode && rightCode) {
    const codeOrder = compareText(leftCode, rightCode);
    if (codeOrder !== 0) return codeOrder;
  } else if (leftCode) {
    return -1;
  } else if (rightCode) {
    return 1;
  }

  return compareText(left.label, right.label);
}

export function buildShopProductCatalog(rows: readonly ShopCatalogSku[]): ShopCatalogGroup[] {
  const groups = new Map<string, Map<string, ShopCatalogCategory>>();
  const categoryCodeByTypeAndName = new Map<string, Map<string, string>>();

  rows.forEach((row) => {
    const productType = clean(row.productType).toUpperCase() || "OTHER";
    const code = clean(row.categoryCode || row.category);
    const normalizedName = normalizeCategoryName(clean(row.categoryName));
    if (!code || !normalizedName) return;
    if (!categoryCodeByTypeAndName.has(productType)) categoryCodeByTypeAndName.set(productType, new Map());
    categoryCodeByTypeAndName.get(productType)?.set(normalizedName, code);
  });

  rows.forEach((row) => {
    const productType = clean(row.productType).toUpperCase() || "OTHER";
    const category = categoryIdentity(row, categoryCodeByTypeAndName.get(productType) || new Map());
    if (!groups.has(productType)) groups.set(productType, new Map());
    const categories = groups.get(productType) as Map<string, ShopCatalogCategory>;
    if (!categories.has(category.key)) {
      categories.set(category.key, {
        key: category.key,
        code: category.code,
        label: category.label,
        skuCount: 0,
        skus: []
      });
    }
    const target = categories.get(category.key) as ShopCatalogCategory;
    if (!target.code && category.code) target.code = category.code;
    target.skus.push(row);
    target.skuCount += 1;
  });

  return [...groups.entries()]
    .map(([key, categoryMap]) => {
      const categories = [...categoryMap.values()]
        .map((category) => ({
          ...category,
          skus: [...category.skus].sort((left, right) =>
            compareText(left.productName, right.productName) || compareText(left.supplierSkuCode, right.supplierSkuCode)
          )
        }))
        .sort(compareCategory);
      return {
        key,
        label: productTypeLabel(key),
        skuCount: categories.reduce((sum, category) => sum + category.skuCount, 0),
        categories
      };
    })
    .sort((left, right) => {
      const leftIndex = PRODUCT_TYPE_ORDER.indexOf(left.key);
      const rightIndex = PRODUCT_TYPE_ORDER.indexOf(right.key);
      if (leftIndex !== rightIndex) {
        if (leftIndex < 0) return 1;
        if (rightIndex < 0) return -1;
        return leftIndex - rightIndex;
      }
      return compareText(left.label, right.label);
    });
}
