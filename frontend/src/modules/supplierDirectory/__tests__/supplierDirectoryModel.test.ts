import { describe, expect, it } from "vitest";
import {
  filterSupplierDirectory,
  filterSupplierProducts,
  paginateSupplierDirectory,
  supplierProductClassificationGroups,
  supplierDirectorySummary
} from "../services/supplierDirectoryModel";
import type { ShopCatalogSku } from "@/modules/shopManagement/types/shopProductCatalog";
import type { SupplierDirectoryItem } from "../types/supplierDirectory";

const rows: SupplierDirectoryItem[] = [
  {
    companyId: 11,
    id: "SUP-11",
    name: "舟山海供物资有限公司",
    creditCode: "91330900ZS001",
    logoFileId: "",
    logoUrl: "",
    introduction: "Marine supplier introduction",
    servicePorts: ["舟山港", "宁波港"],
    categories: ["甲板物料", "厨房用品"],
    status: "ACTIVE",
    skuCount: 120,
    categoryCount: 2,
    contactName: "李海燕",
    contactPhone: "13900001111",
    contactEmail: "li@example.com",
    averageRating: 4.8,
    evaluationCount: 32,
    positiveRate: 96,
    reputationLevel: "优秀"
  },
  {
    companyId: 12,
    id: "SUP-12",
    name: "宁波港兴船舶供应",
    creditCode: "91330200NB002",
    logoFileId: "",
    logoUrl: "",
    introduction: "Marine supplier introduction",
    servicePorts: ["宁波港"],
    categories: ["伙食"],
    status: "DISABLED",
    skuCount: 20,
    categoryCount: 1,
    contactName: "王磊",
    contactPhone: "13700002222",
    contactEmail: "wang@example.com",
    averageRating: null,
    evaluationCount: 0,
    positiveRate: null,
    reputationLevel: "暂无评价"
  }
];

describe("supplier directory model", () => {
  it("searches company identity and contact fields together", () => {
    expect(filterSupplierDirectory(rows, { keyword: "91330900", port: "", category: "", status: "" })).toHaveLength(1);
    expect(filterSupplierDirectory(rows, { keyword: "1370000", port: "", category: "", status: "" })[0]?.companyId).toBe(12);
  });

  it("filters by service port, category and status", () => {
    expect(filterSupplierDirectory(rows, { keyword: "", port: "宁波港", category: "厨房用品", status: "ACTIVE" })[0]?.companyId).toBe(11);
    expect(filterSupplierDirectory(rows, { keyword: "", port: "", category: "", status: "DISABLED" })[0]?.companyId).toBe(12);
  });

  it("summarizes the filtered result without inventing ratings", () => {
    expect(supplierDirectorySummary(rows)).toEqual({ supplierCount: 2, activeCount: 1, skuCount: 140 });
  });

  it("keeps postcard pages stable", () => {
    expect(paginateSupplierDirectory(rows, 1, 1).map((row) => row.companyId)).toEqual([11]);
    expect(paginateSupplierDirectory(rows, 2, 1).map((row) => row.companyId)).toEqual([12]);
  });

  it("按商品名称、分级分类、编码状态和上下架状态筛选供货商商品", () => {
    const products = [
      { id: "1", productType: "MATERIAL", categoryName: "文具类", categoryCode: "47", productName: "荧光笔", supplierSkuCode: "PEN-01", platformCode: "470673", impaCode: "470673", thumbnail: "", imageUrl: "", packing: "盒", stock: 3, unit: "支", price: 1.5, currency: "CNY", codingStatus: "CODE_MATCHED", listingStatus: "ON_SHELF" },
      { id: "2", productType: "FOOD", categoryName: "调味品", categoryCode: "FOOD-01", productName: "生抽", supplierSkuCode: "SOY-01", platformCode: "", thumbnail: "", imageUrl: "", packing: "瓶", stock: 2, unit: "瓶", price: 12, currency: "CNY", codingStatus: "PENDING_EXCEPTION", listingStatus: "OFF_SHELF" }
    ] as ShopCatalogSku[];
    expect(filterSupplierProducts(products, { keyword: "470673", classification: "TYPE::MATERIAL", codeStatus: "MATCHED_SUCCESS", shelfStatus: "ON_SHELF" })).toHaveLength(1);
    expect(filterSupplierProducts(products, { keyword: "生抽", classification: "CATEGORY::FOOD::%E8%B0%83%E5%91%B3%E5%93%81", codeStatus: "PENDING_EXCEPTION", shelfStatus: "OFF_SHELF" })[0]?.id).toBe("2");
  });

  it("供货商商品分类按物料和伙食分组并展示数量", () => {
    const products = [
      { productType: "MATERIAL", categoryName: "文具类" },
      { productType: "MATERIAL", categoryName: "文具类" },
      { productType: "FOOD", categoryName: "调味品" }
    ] as ShopCatalogSku[];
    expect(supplierProductClassificationGroups(products)).toEqual([
      { productType: "MATERIAL", label: "物料", categories: [{ name: "文具类", label: "文具类", count: 2 }] },
      { productType: "FOOD", label: "伙食", categories: [{ name: "调味品", label: "调味品", count: 1 }] }
    ]);
  });
});
