// @ts-nocheck -- Source and CSS layout contracts are verified in Vitest's Node environment.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const workbenchSource = readFileSync(resolve(process.cwd(), "src/views/WorkbenchPage.vue"), "utf8");
const workbenchCss = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("platform permissions shared layout", () => {
  it("keeps all three tabs inside the same vertically scrollable panel chain", () => {
    expect(workbenchSource).toContain("{ key: 'users', label: t('permission.platformAccounts') }");
    expect(workbenchSource).toContain("{ key: 'menuPermissions', label: t('permission.roleMenuPermissions') }");
    expect(workbenchSource).toContain("{ key: 'menuOrder', label: t('permission.menuOrder') }");

    expect(workbenchCss).toMatch(
      /\.permission-admin-panel\s*\{[^}]*flex:\s*1 1 auto;[^}]*width:\s*100%;[^}]*min-width:\s*0;[^}]*min-height:\s*0;/s
    );
    expect(workbenchCss).toMatch(
      /\.permission-admin-panel\s*>\s*\.expandable-panel__body\s*\{[^}]*overflow-x:\s*auto;[^}]*overflow-y:\s*auto;/s
    );
    expect(workbenchCss).toMatch(
      /\.permission-admin-panel\s+\.loading-host\s*\{[^}]*width:\s*100%;[^}]*min-width:\s*0;[^}]*min-height:\s*100%;/s
    );
    expect(workbenchCss).toMatch(
      /\.permission-main\s*\{[^}]*min-width:\s*0;[^}]*overflow:\s*visible;/s
    );
  });

  it("reserves one row for the keyword, three filters and actions without wrapping labels or buttons", () => {
    expect(workbenchCss).toMatch(
      /\.permission-user-toolbar\s*\{[^}]*grid-template-columns:\s*minmax\(240px,\s*1\.4fr\)\s+repeat\(3,\s*minmax\(128px,\s*0\.65fr\)\)\s+auto;/s
    );
    expect(workbenchCss).toMatch(
      /\.permission-main-toolbar\s+\.animated-tabs\s+button,\s*\.permission-user-toolbar\s+\.permission-filter-field\s*>\s*span,\s*\.permission-user-toolbar\s+\.toolbar-icon-actions\s*\{[^}]*white-space:\s*nowrap;/s
    );
    expect(workbenchCss).toMatch(
      /\.permission-user-toolbar\s+\.toolbar-icon-actions\s*\{[^}]*flex-wrap:\s*nowrap;/s
    );
  });

  it("does not restore the obsolete three-column permissions workspace on narrow screens", () => {
    expect(workbenchCss).not.toMatch(
      /\.permission-admin-workspace\s*\{[^}]*grid-template-columns:\s*minmax\(220px,\s*240px\)\s+minmax\(560px,\s*1fr\)\s+minmax\(250px,\s*280px\)/s
    );
    expect(workbenchCss).toMatch(
      /@media\s*\(max-width:\s*980px\)[\s\S]*?\.permission-admin-workspace\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\);[^}]*min-width:\s*860px;/s
    );
  });
});
