// @ts-nocheck -- Source contracts are verified in Vitest's Node environment.
import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

describe("Shop product row actions", () => {
  it("上下架使用图标按钮并保留明确提示", () => {
    expect(workbenchSource).not.toContain("shop-row-shelf-action");
    expect(workbenchSource).toContain("shop-row-shelf-button");
    expect(workbenchSource).toContain("row.listingStatus === 'ON_SHELF' ? 'ArrowDown' : 'ArrowUp'");
    expect(workbenchSource).toContain("row.listingStatus === 'ON_SHELF' ? t('page.supplierProducts.actionOffShelf') : t('page.supplierProducts.actionOnShelf')");
  });

  it("确认候选只处理已有候选编码，不再把无候选行静默忽略", () => {
    expect(workbenchSource).toContain("hasShopCodeCandidate(row) && !isShopMatchedCodeStatus(row.codingStatus)");
    const handler = workbenchSource.slice(
      workbenchSource.indexOf("const resolveShopException"),
      workbenchSource.indexOf("const toggleShopListFullscreen")
    );
    expect(handler).toContain('action: "SELECT_CANDIDATE"');
    expect(handler).not.toContain('"IGNORE"');
  });
});
