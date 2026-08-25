// @ts-nocheck -- Vitest runs this source contract in Node; the app does not ship Node typings.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const materialPage = readFileSync(resolve(process.cwd(), "src/views/WorkbenchPage.vue"), "utf8");
const foodPage = readFileSync(resolve(process.cwd(), "src/modules/procurement/food/pages/FoodOrderPage.vue"), "utf8");
const styles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("material and food order detail glass parity", () => {
  it("uses the same glass detail root for buyer and supplier material/food orders", () => {
    expect(materialPage).toContain('class="purchase-order-detail order-detail-glass"');
    expect(foodPage).toContain('class="purchase-order-detail order-detail-glass"');
    expect(materialPage).not.toContain("is-supplier-glass");
    expect(foodPage).not.toContain("is-supplier-glass");
  });

  it("gives the detail shell and each suitable business region one restrained glass layer", () => {
    expect(styles).toMatch(/\.purchase-order-detail\.order-detail-glass\s*\{[^}]*border-color:\s*var\(--glass-card-border\);[^}]*background:\s*var\(--glass-card-bg\);[^}]*backdrop-filter:\s*blur\(16px\) saturate\(1\.16\);/s);
    expect(styles).toContain(".order-detail-glass > .purchase-order-execution-card,");
    expect(styles).toContain(".order-detail-glass > .purchase-order-execution-situation,");
    expect(styles).toContain(".order-detail-glass > .purchase-order-section");
    expect(styles).toMatch(/\.order-detail-glass > \.purchase-order-execution-card[\s\S]*?box-shadow:[\s\S]*?0 4px 8px rgba\(38, 112, 176, 0\.08\);/);
  });

  it("keeps inner content readable and includes a no-backdrop fallback", () => {
    expect(styles).toMatch(/\.order-detail-glass > \.purchase-order-execution-card \.purchase-order-execution-rail\s*\{[^}]*background:\s*var\(--glass-card-bg-soft\);[^}]*box-shadow:/s);
    expect(styles).toContain("@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px)))");
    expect(styles).toContain(".purchase-order-detail.order-detail-glass");
  });
});
