import { beforeEach, describe, expect, it, vi } from "vitest";
import { getAuthMenus } from "../permissionService";

describe("company member menu grouping", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("moves a legacy root company member menu into basic services", async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify([
      {
        menuCode: "BASIC_MANAGEMENT",
        menuName: "基础服务",
        icon: "Settings",
        sortOrder: 100,
        visibleRoles: "SUPPLIER"
      },
      {
        menuCode: "COMPANY_MEMBERS",
        menuName: "企业成员",
        routePath: "/company/members",
        icon: "UsersRound",
        sortOrder: 110,
        visibleRoles: "SUPPLIER"
      }
    ]), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    }));

    const menus = await getAuthMenus();
    const basic = menus.find((item) => item.key === "basicManagement");

    expect(menus.some((item) => item.route === "/company/members" && !item.children?.length)).toBe(false);
    expect(basic?.children?.some((item) => item.route === "/company/members")).toBe(true);
  });
});
