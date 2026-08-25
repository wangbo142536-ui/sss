// @ts-nocheck -- CSS source contract test.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const styles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("global editable control typography", () => {
  it("输入、选择、文本域和可编辑控件统一缩小2px并保留12px可读下限", () => {
    expect(styles).toMatch(/input,\s*select,\s*textarea,\s*\[contenteditable="true"\]\s*\{[\s\S]*?font-size:\s*max\(12px,\s*calc\(1em - 2px\)\)/);
    expect(styles).toMatch(/input::placeholder,\s*textarea::placeholder\s*\{[\s\S]*?font-size:\s*inherit/);
  });
});
