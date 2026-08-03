import { beforeEach, describe, expect, it, vi } from "vitest";
import { menuItems } from "@/data/mockWorkbench";
import { getAdminMenus, getAuthMenus } from "@/services/permissionService";

describe("platform operations menu", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("places platform operations immediately before basic services for admin and supplier roles", () => {
    const platformIndex = menuItems.findIndex((item) => item.key === "platformOperations");
    const basicIndex = menuItems.findIndex((item) => item.key === "basicManagement");
    const platform = menuItems[platformIndex];

    expect(platformIndex).toBeGreaterThan(-1);
    expect(basicIndex).toBe(platformIndex + 1);
    expect(platform.sortOrder).toBe(95);
    expect(platform.roles).toEqual(["admin", "supplier"]);
    expect(platform.children).toEqual(expect.arrayContaining([
      expect.objectContaining({ key: "supplierDataAnalysis", route: "/platform-operations/data-analysis" })
    ]));
  });

  it("canonicalizes backend menu codes for workbench and permission trees", async () => {
    const responseBody = [{
      key: "PLATFORM_OPERATIONS",
      children: [{ key: "DATA_ANALYSIS", route: "/platform-operations/data-analysis" }]
    }];
    const fetchMock = vi.mocked(fetch);
    fetchMock.mockImplementation(async () => new Response(JSON.stringify(responseBody), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    }));

    const [workbenchMenus, permissionMenus] = await Promise.all([getAuthMenus(), getAdminMenus()]);
    const workbenchPlatform = workbenchMenus.find((item) => item.key === "platformOperations");
    const permissionPlatform = permissionMenus.find((item) => item.key === "platformOperations");

    expect(workbenchPlatform).toEqual(expect.objectContaining({ labelKey: "nav.platformOperations", sortOrder: 95 }));
    expect(workbenchPlatform?.children?.[0]).toEqual(expect.objectContaining({
      key: "supplierDataAnalysis",
      labelKey: "nav.dataAnalysis",
      route: "/platform-operations/data-analysis"
    }));
    expect(permissionPlatform?.children?.[0]).toEqual(expect.objectContaining({
      key: "supplierDataAnalysis",
      labelKey: "nav.dataAnalysis"
    }));
  });
});

