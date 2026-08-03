import { describe, expect, it } from "vitest";
import { resolveFoodActor } from "../composables/useFoodActor";

describe("resolveFoodActor", () => {
  it("uses the explicit order route before the company type fallback", () => {
    expect(resolveFoodActor("/food/orders", true)).toBe("BUYER");
    expect(resolveFoodActor("/food/orders/7", true)).toBe("BUYER");
    expect(resolveFoodActor("/supplier/food/orders", false)).toBe("SUPPLIER");
    expect(resolveFoodActor("/supplier/food/orders/7", false)).toBe("SUPPLIER");
  });

  it("keeps account-based behavior for shared food routes", () => {
    expect(resolveFoodActor("/food/quotes", true)).toBe("SUPPLIER");
    expect(resolveFoodActor("/food/quotes", false)).toBe("BUYER");
  });
});
