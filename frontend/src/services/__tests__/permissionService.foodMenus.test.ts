import { beforeEach, describe, expect, it, vi } from "vitest";
import type { WorkbenchMenuItem } from "@/types/workbench";
import zhCN from "@/locales/zh-CN";
import enUS from "@/locales/en-US";
import { ensureDevelopmentFoodMenus, getAuthMenus } from "../permissionService";

const visibleRoles = ["admin", "purchaser", "supplier", "operator", "finance"];

function flatten(items: WorkbenchMenuItem[]): WorkbenchMenuItem[] {
  return items.flatMap((item) => [item, ...flatten(item.children || [])]);
}

describe("ensureDevelopmentFoodMenus", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("exposes the complete buyer and supplier food menu set to every development role", () => {
    const source: WorkbenchMenuItem[] = [
      {
        key: "procurementServices",
        labelKey: "nav.procurementServices",
        icon: "PS",
        sortOrder: 10,
        roles: ["admin"],
        children: [
          {
            key: "foodProcurement",
            labelKey: "nav.foodProcurement",
            icon: "FP",
            sortOrder: 10,
            roles: ["purchaser"],
            children: [
              { key: "foodOrders", labelKey: "nav.foodOrders", route: "/food/orders", icon: "PO", sortOrder: 40, roles: ["purchaser"] }
            ]
          }
        ]
      }
    ];

    const flattened = flatten(ensureDevelopmentFoodMenus(source));
    const foodRoutes = [
      "/procurement/food",
      "/food/inquiries",
      "/food/quotes",
      "/food/comparison",
      "/food/orders",
      "/supplier/food/orders",
      "/food/settlements",
      "/supplier/food/settlements",
      "/food/evaluations"
    ];

    foodRoutes.forEach((route) => {
      const item = flattened.find((candidate) => candidate.route === route);
      expect(item, route).toBeDefined();
      expect(item?.roles).toEqual(visibleRoles);
    });
    expect(flattened.find((item) => item.route === "/food/orders")?.labelKey).toBe("nav.foodOrders");
    expect(flattened.find((item) => item.route === "/supplier/food/orders")?.labelKey).toBe("nav.foodSupplierOrders");
    expect(flattened.find((item) => item.route === "/food/settlements")?.labelKey).toBe("nav.foodSettlements");
    expect(flattened.find((item) => item.route === "/supplier/food/settlements")?.labelKey).toBe("nav.foodSupplierSettlements");
  });

  it("restores the complete food menu after a restricted backend menu response", async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify([
      { key: "DASHBOARD", route: "/dashboard", visibleRoles: "admin" }
    ]), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    }));

    const flattened = flatten(await getAuthMenus());

    expect(flattened.find((item) => item.route === "/food/orders")?.roles).toEqual(visibleRoles);
    expect(flattened.find((item) => item.route === "/supplier/food/orders")?.roles).toEqual(visibleRoles);
    expect(flattened.find((item) => item.route === "/food/settlements")?.roles).toEqual(visibleRoles);
    expect(flattened.find((item) => item.route === "/supplier/food/settlements")?.roles).toEqual(visibleRoles);
  });

  it("uses concise food submenu labels while keeping the food entry explicit", () => {
    expect(zhCN.nav.food).toBe("伙食采购入口");
    expect(zhCN.nav.foodInquiries).toBe("询价管理");
    expect(zhCN.nav.foodQuotes).toBe("报价管理");
    expect(zhCN.nav.foodCompare).toBe("比价管理");
    expect(zhCN.nav.foodSupplierOrders).toBe("供货管理 - 供货商");
    expect(zhCN.nav.foodEvaluations).toBe("评价管理");
    expect(enUS.nav.foodSupplierOrders).toBe("Supply Management - Supplier");
  });
});
