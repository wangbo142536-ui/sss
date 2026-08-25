// @ts-nocheck -- Vitest reads component source as a structural contract.
import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

describe("IMPA library pagination UI contract", () => {
  it("renders one compact row with previous, page indicator, and next controls", () => {
    const start = workbenchSource.indexOf('<ExpandablePanel v-else-if="pageKey === \'impa\'"');
    const end = workbenchSource.indexOf("</ExpandablePanel>", start);
    const impaPanel = workbenchSource.slice(start, end);

    expect(impaPanel).toContain('class="shop-pagination impa-pagination"');
    expect(impaPanel).toContain('icon="ChevronLeft"');
    expect(impaPanel).toContain('icon="ChevronRight"');
    expect(impaPanel).toContain("impaPageTotalPages");
    expect(impaPanel).toContain("goImpaPage(impaPage - 1)");
    expect(impaPanel).toContain("goImpaPage(impaPage + 1)");
  });
});
