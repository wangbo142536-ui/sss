import type { SupplierDirectoryFilters, SupplierDirectoryItem } from "../types/supplierDirectory";
import type { ShopCatalogSku } from "@/modules/shopManagement/types/shopProductCatalog";

export type SupplierProductFilters = {
  keyword: string;
  classification: string;
  codeStatus: string;
  shelfStatus: string;
};

const MATCHED_CODE_STATUSES = new Set([
  "CODE_MATCHED", "SPEC_MATCHED", "CONFIRMED", "IGNORED", "MATCHED", "CODE_MATCH", "SPEC_MATCH", "SPECIFICATION_MATCH"
]);

function normalized(value: unknown): string {
  return String(value ?? "").trim().toLocaleLowerCase();
}

export function filterSupplierDirectory(rows: SupplierDirectoryItem[], filters: SupplierDirectoryFilters): SupplierDirectoryItem[] {
  const keyword = normalized(filters.keyword);
  const port = normalized(filters.port);
  const category = normalized(filters.category);
  const status = normalized(filters.status);

  return rows.filter((row) => {
    const identity = [row.name, row.creditCode, row.contactName, row.contactPhone, row.contactEmail]
      .map(normalized)
      .join(" ");
    if (keyword && !identity.includes(keyword)) return false;
    if (port && !row.servicePorts.some((item) => normalized(item) === port)) return false;
    if (category && !row.categories.some((item) => normalized(item) === category)) return false;
    if (status && normalized(row.status) !== status) return false;
    return true;
  });
}

export function supplierDirectorySummary(rows: SupplierDirectoryItem[]) {
  return {
    supplierCount: rows.length,
    activeCount: rows.filter((row) => normalized(row.status) === "active").length,
    skuCount: rows.reduce((total, row) => total + Math.max(0, Number(row.skuCount) || 0), 0)
  };
}

export function paginateSupplierDirectory(rows: SupplierDirectoryItem[], page: number, pageSize: number): SupplierDirectoryItem[] {
  const safeSize = Math.max(1, pageSize);
  const safePage = Math.max(1, page);
  return rows.slice((safePage - 1) * safeSize, safePage * safeSize);
}

function parseClassification(value: string): { productType: string; categoryName: string } {
  const [mode, productType = "", encodedCategory = ""] = String(value || "").split("::");
  if (mode === "TYPE") return { productType, categoryName: "" };
  if (mode !== "CATEGORY") return { productType: "", categoryName: "" };
  try {
    return { productType, categoryName: decodeURIComponent(encodedCategory) };
  } catch {
    return { productType, categoryName: encodedCategory };
  }
}

export function filterSupplierProducts(rows: ShopCatalogSku[], filters: SupplierProductFilters): ShopCatalogSku[] {
  const keyword = normalized(filters.keyword);
  const { productType, categoryName } = parseClassification(filters.classification);
  const codeStatus = String(filters.codeStatus || "").trim().toUpperCase();
  const shelfStatus = String(filters.shelfStatus || "").trim().toUpperCase();
  return rows.filter((row) => {
    const searchable = [row.productName, row.supplierSkuCode, row.platformCode, row.impaCode, row.barcode, row.brand, ...(row.specs || [])]
      .map(normalized)
      .join(" ");
    if (keyword && !searchable.includes(keyword)) return false;
    if (productType && normalized(row.productType) !== normalized(productType)) return false;
    const rowCategory = String(row.categoryName || row.category || "").trim();
    if (categoryName === "__UNCATEGORIZED__" && rowCategory) return false;
    if (categoryName && categoryName !== "__UNCATEGORIZED__" && normalized(rowCategory) !== normalized(categoryName)) return false;
    const rowCodeStatus = String(row.codingStatus || "").trim().toUpperCase();
    if (codeStatus === "MATCHED_SUCCESS" && !MATCHED_CODE_STATUSES.has(rowCodeStatus)) return false;
    if (codeStatus && codeStatus !== "MATCHED_SUCCESS" && rowCodeStatus !== codeStatus) return false;
    if (shelfStatus && String(row.listingStatus || "").trim().toUpperCase() !== shelfStatus) return false;
    return true;
  });
}

export function supplierProductClassificationGroups(rows: ShopCatalogSku[]) {
  return ["MATERIAL", "FOOD"].map((productType) => {
    const counts = new Map<string, number>();
    rows.filter((row) => String(row.productType).toUpperCase() === productType).forEach((row) => {
      const name = String(row.categoryName || row.category || "").trim() || "__UNCATEGORIZED__";
      counts.set(name, (counts.get(name) || 0) + 1);
    });
    return {
      productType,
      label: productType === "FOOD" ? "伙食" : "物料",
      categories: [...counts.entries()]
        .map(([name, count]) => ({ name, label: name === "__UNCATEGORIZED__" ? "未分类" : name, count }))
        .sort((left, right) => left.label.localeCompare(right.label, "zh-CN", { numeric: true, sensitivity: "base" }))
    };
  });
}
