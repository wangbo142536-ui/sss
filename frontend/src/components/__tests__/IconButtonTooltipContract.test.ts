// @ts-nocheck -- Vitest reads the global CSS contract in Node; the app does not ship Node typings.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import iconButtonSource from "../IconButton.vue?raw";

const workbenchStyles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");
const tooltipStyles = readFileSync(resolve(process.cwd(), "src/styles/icon-button-tooltip.css"), "utf8");

describe("IconButton tooltip source contract", () => {
  it("移除会被祖先裁剪的按钮伪元素和原生title", () => {
    expect(workbenchStyles).not.toContain(".ui-icon-button::after");
    expect(iconButtonSource).not.toContain(':title="label"');
    expect(iconButtonSource).not.toContain(':data-tooltip="label"');
  });

  it("使用body Teleport、语义层级、无障碍关系和reduced-motion", () => {
    expect(iconButtonSource).toContain('<Teleport to="body">');
    expect(iconButtonSource).toContain('role="tooltip"');
    expect(iconButtonSource).toContain(":aria-describedby=");
    expect(iconButtonSource).toContain('matches(":focus-visible")');
    expect(tooltipStyles).toContain("position: fixed");
    expect(tooltipStyles).toContain("z-index: var(--z-icon-button-tooltip, 2147483000)");
    expect(tooltipStyles).toContain("pointer-events: none");
    expect(tooltipStyles).not.toContain("overflow: hidden");
    expect(tooltipStyles).toContain("@media (prefers-reduced-motion: reduce)");
  });
});
