// @ts-nocheck -- source contract for the modular provision standard library.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const pagePath = resolve(process.cwd(), "src/modules/standardLibrary/pages/ProvisionStandardLibraryPage.vue");
const servicePath = resolve(process.cwd(), "src/modules/standardLibrary/services/provisionStandardLibraryService.ts");
const router = readFileSync(resolve(process.cwd(), "src/router/index.ts"), "utf8");
const permissions = readFileSync(resolve(process.cwd(), "src/services/permissionService.ts"), "utf8");
const menus = readFileSync(resolve(process.cwd(), "src/data/mockWorkbench.ts"), "utf8");

describe("ProvisionStandardLibraryPage", () => {
  it("基础服务菜单和路由指向模块化页面", () => {
    expect(router).toContain('path: "/standard-library/provision"');
    expect(router).toContain("ProvisionStandardLibraryPage");
    expect(permissions).toContain('STANDARD_LIBRARY_PROVISION: "nav.provisionStandard"');
    expect(permissions).toContain('"/standard-library/provision": "nav.provisionStandard"');
    expect(menus).toContain('route: "/standard-library/provision"');
  });

  it("页面复用IMPA标准库结构并读取真实伙食分类接口", () => {
    const page = readFileSync(pagePath, "utf8");
    const service = readFileSync(servicePath, "utf8");
    expect(page).toContain("WorkbenchLayout");
    expect(page).toContain("impa-library-panel");
    expect(page).toContain("library-shell");
    expect(page).toContain("伙食标准库");
    expect(page).toContain("getProvisionStandardCategories");
    expect(service).toContain("/api/standard-library/provision/categories");
    expect(service).not.toContain("mock");
  });
});
