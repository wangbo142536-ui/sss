// @ts-nocheck -- Source contract assertion reads the router file.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { menuItems } from "@/data/mockWorkbench";
import enUS from "@/locales/en-US";
import zhCN from "@/locales/zh-CN";
import { getAdminMenus, getAuthMenus } from "@/services/permissionService";

const platformRoutes = [
  "/shop/products",
  "/platform-operations/data-analysis",
  "/admin/registrations",
  "/suppliers"
];

describe("platform operations menu", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("places the four platform operations entries in strict order and removes their basic-service duplicates", () => {
    const platformIndex = menuItems.findIndex((item) => item.key === "platformOperations");
    const basicIndex = menuItems.findIndex((item) => item.key === "basicManagement");
    const platform = menuItems[platformIndex];
    const basic = menuItems[basicIndex];

    expect(platformIndex).toBeGreaterThan(-1);
    expect(basicIndex).toBe(platformIndex + 1);
    expect(platform.sortOrder).toBe(95);
    expect(platform.roles).toEqual(["admin", "purchaser", "supplier"]);
    expect(platform.children?.map((item) => item.route)).toEqual(platformRoutes);
    expect(basic.children?.map((item) => item.route)).not.toEqual(expect.arrayContaining(platformRoutes));
    expect(basic.roles).toEqual(["admin", "purchaser", "supplier", "operator"]);

    const visibleRoutes = (role: "admin" | "purchaser" | "supplier" | "operator") =>
      platform.children?.filter((item) => item.roles.includes(role)).map((item) => item.route);
    expect(visibleRoutes("admin")).toEqual(platformRoutes);
    expect(visibleRoutes("purchaser")).toEqual(["/shop/products", "/suppliers"]);
    expect(visibleRoutes("supplier")).toEqual(["/shop/products", "/platform-operations/data-analysis"]);
    expect(visibleRoutes("operator")).toEqual([]);
  });

  it("moves legacy backend basic-service children into platform operations for workbench and permission trees", async () => {
    const responseBody = [
      {
        key: "PLATFORM_OPERATIONS",
        visibleRoles: "PLATFORM_ADMIN,SUPPLIER",
        children: [{ key: "DATA_ANALYSIS", route: "/platform-operations/data-analysis", visibleRoles: "PLATFORM_ADMIN,SUPPLIER" }]
      },
      {
        key: "BASIC_MANAGEMENT",
        children: [
          { key: "SHOP_PRODUCTS", route: "/shop/products", visibleRoles: "PLATFORM_ADMIN,SHIP_AGENT,SUPPLIER" },
          { key: "ADMIN_REGISTRATIONS", route: "/admin/registrations", visibleRoles: "PLATFORM_ADMIN" },
          { key: "SUPPLIERS", route: "/suppliers", visibleRoles: "PLATFORM_ADMIN,SHIP_AGENT" },
          { key: "STANDARD_LIBRARY_IMPA", route: "/standard-library/impa", visibleRoles: "PLATFORM_ADMIN,SHIP_AGENT" }
        ]
      }
    ];
    const fetchMock = vi.mocked(fetch);
    fetchMock.mockImplementation(async () => new Response(JSON.stringify(responseBody), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    }));

    const [workbenchMenus, permissionMenus] = await Promise.all([getAuthMenus(), getAdminMenus()]);
    const workbenchPlatform = workbenchMenus.find((item) => item.key === "platformOperations");
    const permissionPlatform = permissionMenus.find((item) => item.key === "platformOperations");
    const workbenchBasic = workbenchMenus.find((item) => item.key === "basicManagement");
    const permissionBasic = permissionMenus.find((item) => item.key === "basicManagement");

    expect(workbenchPlatform).toEqual(expect.objectContaining({ labelKey: "nav.platformOperations", sortOrder: 95 }));
    expect(workbenchPlatform?.children?.map((item) => item.route)).toEqual(platformRoutes);
    expect(permissionPlatform?.children?.map((item) => item.route)).toEqual(platformRoutes);
    expect(workbenchBasic?.children?.map((item) => item.route)).toEqual(["/standard-library/impa"]);
    expect(permissionBasic?.children?.map((item) => item.route)).toEqual(["/standard-library/impa"]);
    expect(workbenchPlatform?.children?.find((item) => item.route === "/shop/products")?.roles).toEqual(["admin", "purchaser", "supplier"]);
    expect(workbenchPlatform?.children?.find((item) => item.route === "/platform-operations/data-analysis")?.roles).toEqual(["admin", "supplier"]);
    expect(workbenchPlatform?.children?.find((item) => item.route === "/admin/registrations")?.roles).toEqual(["admin"]);
    expect(workbenchPlatform?.children?.find((item) => item.route === "/suppliers")?.roles).toEqual(["admin", "purchaser"]);
  });

  it("does not invent platform operations when the authenticated menu response grants none", async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify([
      { key: "DASHBOARD", route: "/dashboard", visibleRoles: "SHIP_AGENT" }
    ]), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    }));

    const menus = await getAuthMenus();

    expect(menus.some((item) => item.key === "platformOperations")).toBe(false);
  });

  it("uses the service-provider review name in navigation and the admin route title", () => {
    const routerSource = readFileSync(resolve(process.cwd(), "src/router/index.ts"), "utf8");

    expect(zhCN.nav.registrations).toBe("服务商审核");
    expect(enUS.nav.registrations).toBe("Service Provider Review");
    expect(routerSource).toMatch(/path:\s*"\/admin\/registrations"[\s\S]*?title:\s*"服务商审核"/);
  });
});
