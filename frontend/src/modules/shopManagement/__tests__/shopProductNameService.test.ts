import { describe, expect, it } from "vitest";
import { extractShopProductName } from "../services/shopProductNameService";

describe("extractShopProductName", () => {
  it("从供应商多行描述中提取名称并保留其余内容作为规格来源", () => {
    expect(
      extractShopProductName('6"/160mm Flat Nose Plier\nmaterial: carbon steel\nBi-matel PP+TPR handle')
    ).toBe("Flat Nose Plier");
    expect(
      extractShopProductName('7"/180Mm Circlip Plier\nExternal Straight\nMaterial:Cr-V')
    ).toBe("Circlip Plier");
    expect(
      extractShopProductName('LONG REACH LONG NOSE PLIER Straight\nSIZE ：11"/275MM\nmaterial:CRV')
    ).toBe("LONG REACH LONG NOSE PLIER Straight");
  });

  it("普通单行名称保持不变，空值才返回空字符串", () => {
    expect(extractShopProductName("苹果（红富士）")).toBe("苹果（红富士）");
    expect(extractShopProductName("  ")).toBe("");
  });
});
