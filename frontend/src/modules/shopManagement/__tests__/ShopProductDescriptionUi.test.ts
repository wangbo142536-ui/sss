// @ts-nocheck -- Source contracts are verified in Vitest's Node environment.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

const workbenchCss = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("shop product introduction editor", () => {
  it("keeps a persisted product description in the expanded product row", () => {
    expect(workbenchSource).toContain("productDescription: string");
    expect(workbenchSource).toContain('v-model="row.productDescription"');
    expect(workbenchSource).toContain("productDescription: row.productDescription");
    expect(workbenchSource).toContain('t("page.supplierProducts.field.productDescription")');
    expect(workbenchSource).toContain("page.supplierProducts.productDescriptionPlaceholder");
    expect(workbenchCss).toContain(".shop-sku-expanded__description");
  });

  it("uses the shared icon button at the right edge of the specification title", () => {
    expect(workbenchSource).toMatch(/shop-spec-title[\s\S]*?<IconButton[\s\S]*?icon="Plus"/);
    expect(workbenchSource).not.toContain('class="shop-spec-add"');
    expect(workbenchCss).toMatch(/\.shop-sku-expanded__specs \.shop-spec-title\s*\{[\s\S]*?justify-content:\s*space-between/);
  });
});
