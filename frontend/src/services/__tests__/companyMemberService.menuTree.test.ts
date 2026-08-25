import { beforeEach, describe, expect, it, vi } from "vitest";
import { getCompanyMenuOptions } from "../companyMemberService";

describe("company member menu option tree", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("preserves every menu level returned by the company permission endpoint", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([
      {
        menuCode: "CS_ROOT",
        menuName: "一级菜单",
        children: [
          {
            menuCode: "CS_CHILD",
            menuName: "二级菜单",
            children: [
              { menuCode: "CS_LEAF", menuName: "三级菜单", children: [] }
            ]
          }
        ]
      }
    ]), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    })));

    await expect(getCompanyMenuOptions()).resolves.toEqual([
      {
        code: "CS_ROOT",
        name: "一级菜单",
        children: [
          {
            code: "CS_CHILD",
            name: "二级菜单",
            children: [
              { code: "CS_LEAF", name: "三级菜单", children: [] }
            ]
          }
        ]
      }
    ]);
  });
});
