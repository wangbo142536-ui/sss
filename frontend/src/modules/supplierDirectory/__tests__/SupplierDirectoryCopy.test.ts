// @ts-nocheck -- Source contract for visible service-provider terminology.
import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { supplierDirectoryMessages } from "../locales";
import pageSource from "../pages/SupplierDirectoryPage.vue?raw";

const styles = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/styles/supplier-directory.css"), "utf8");

describe("SupplierDirectoryPage visible terminology", () => {
  it("汇总、筛选和卡片目录统一使用服务商文案", () => {
    expect(pageSource).toContain('aria-label="服务商筛选"');
    expect(pageSource).toContain("服务商总数");
    expect(pageSource).toContain('aria-label="服务商明信片目录"');
    expect(pageSource).not.toContain('aria-label="供货商筛选"');
    expect(pageSource).not.toContain("家供货商");
    expect(pageSource).not.toContain('aria-label="供货商明信片目录"');
    expect(pageSource).toContain('"服务商管理查询失败"');
  });

  it("检索标题保留在输入框上方且输入框只提示企业名称和信用代码", () => {
    expect(pageSource).toContain("<span>{{ labels.search }}</span>");
    expect(pageSource).toContain(':placeholder="labels.keywordPlaceholder"');
    expect(pageSource).toContain(':aria-label="labels.search"');
    expect(pageSource).not.toContain('<span>{{ labels.keyword }} <small>（{{ labels.keywordHint }}）</small></span>');
    expect(supplierDirectoryMessages["zh-CN"].search).toBe("检索");
    expect(supplierDirectoryMessages["zh-CN"].keywordPlaceholder).toBe("企业名称，信用代码");
  });

  it("移除概览标题后由三张玻璃统计卡直接充满整行", () => {
    expect(pageSource).toContain('class="supplier-directory-overview"');
    expect(pageSource).not.toContain("服务商概览");
    expect(pageSource).not.toContain("当前检索范围");
    expect(pageSource).not.toContain('class="supplier-directory-overview__stats"');
    expect(pageSource.match(/class="supplier-directory-overview__stat(?:\s|")/g)).toHaveLength(3);
    expect(pageSource).toContain('class="supplier-directory-overview__stat is-total"');
    expect(pageSource).toContain('class="supplier-directory-overview__stat is-active"');
    expect(pageSource).toContain('class="supplier-directory-overview__stat is-products"');
    expect(pageSource.indexOf('class="supplier-directory-overview"')).toBeLessThan(pageSource.indexOf('class="supplier-directory-filter"'));
    expect(pageSource).not.toContain('class="supplier-directory-summary"');
    expect(styles).toMatch(/\.supplier-directory-overview\s*\{[^}]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/);
    expect(styles).toMatch(/\.supplier-directory-overview__stat\s*\{[^}]*box-sizing:\s*border-box;[^}]*min-width:\s*0;[^}]*background:\s*var\(--glass-card-bg\);[^}]*backdrop-filter:/);
    expect(styles).toMatch(/\.supplier-directory-overview__stat\.is-active\s*\{[^}]*border-color:[^}]*background:/);
    expect(styles).toMatch(/\.supplier-directory-overview__stat\.is-active strong\s*\{[^}]*color:\s*#13845d/);
    expect(styles).toMatch(/\.supplier-directory-overview__stat\s*\{[^}]*box-shadow:\s*var\(--glass-card-shadow\)/);
    expect(styles).not.toContain(".supplier-directory-overview__stat + .supplier-directory-overview__stat");
    expect(styles).toMatch(/@media \(max-width:\s*720px\)[\s\S]*?\.supplier-directory-overview\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/);
  });

  it("筛选区与服务商卡片目录之间保留清晰的分组间距", () => {
    expect(styles).toMatch(/\.supplier-directory-grid\s*\{[^}]*margin-top:\s*12px;/);
  });

  it("窄屏下统计与筛选切换为单列且操作区不造成横向溢出", () => {
    expect(styles).toMatch(/@media \(max-width:\s*720px\)[\s\S]*?\.supplier-directory-filter\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/);
    expect(styles).toMatch(/@media \(max-width:\s*720px\)[\s\S]*?\.supplier-directory-filter__actions\s*\{[^}]*flex-wrap:\s*wrap/);
  });
});
