import { describe, expect, it } from "vitest";
import { foodQuotedSubtotal, foodQuotedUnitPrice, roundFoodQuoteMoney } from "../domain/comparisonPricing";

describe("food comparison pricing", () => {
  it("updates line unit price and subtotal from markup using the order rounding rule", () => {
    expect(foodQuotedUnitPrice(3.33, 15)).toBe(3.83);
    expect(foodQuotedSubtotal(10, 3.33, 15)).toBe(38.3);
    expect(foodQuotedUnitPrice(3.33, 20)).toBe(4);
    expect(foodQuotedSubtotal(10, 3.33, 20)).toBe(40);
  });

  it("does not round an exact cent up because of floating point noise", () => {
    expect(roundFoodQuoteMoney(45 * 1.1)).toBe(49.5);
  });
});
