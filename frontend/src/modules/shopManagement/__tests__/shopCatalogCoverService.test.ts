import { describe, expect, it } from "vitest";
import { resolveShopCatalogCover } from "../services/shopCatalogCoverService";

describe("shopCatalogCoverService", () => {
  it("优先按规范化分类名称匹配内容封面，并兼容标准大类编码", () => {
    expect(resolveShopCatalogCover({ code: "47", label: " 文具类 " }, "MATERIAL")).toContain("stationery");
    expect(resolveShopCatalogCover({ code: "17", label: "尚未同步名称" }, "MATERIAL")).toContain("kitchen-supplies");
    expect(resolveShopCatalogCover({ code: "F01", label: "水果" }, "FOOD")).toContain("fruit");
  });

  it("新出现且尚未配置的分类按商品大类使用默认封面", () => {
    expect(resolveShopCatalogCover({ code: "NEW-M", label: "新物料分类" }, "MATERIAL")).toContain("material-general");
    expect(resolveShopCatalogCover({ code: "NEW-F", label: "新伙食分类" }, "FOOD")).toContain("food-general");
  });
});
