import { describe, expect, it } from "vitest";
import { buildMaterialSavedQuoteRestoreState, resolveMaterialInitialQuoteStrategy } from "@/modules/procurement/materials/services/materialComparisonQuoteRestore";
import type { MaterialComparisonItem } from "@/types/procurementMaterials";

const savedItem = (overrides: Partial<MaterialComparisonItem> = {}): MaterialComparisonItem => ({
  demandItemId: 11,
  quantity: "2",
  actualQuotePrice: 150.25,
  quoteMarkupPercent: 14,
  quoteSupplierSkuId: 101,
  quoteSelectedUnit: "BOX",
  quoteStrategyType: "LOWEST_MIXED",
  lowestCandidate: {
    skuId: 101,
    selectedUnit: "PCS",
    unitPrice: 10,
    unitPriceOptions: [
      { unit: "PCS", unitPrice: 10, defaultSelected: true },
      { unit: "BOX", unitPrice: 100 }
    ]
  },
  singleSupplierCandidate: {
    skuId: 202,
    selectedUnit: "PCS",
    unitPrice: 12
  },
  candidates: [],
  ...overrides
});

describe("material comparison saved quote restoration", () => {
  it("opens the enabled strategy that owns the saved quote before restoring its context", () => {
    expect(resolveMaterialInitialQuoteStrategy(
      [savedItem({ quoteStrategyType: "SINGLE_SUPPLIER" })],
      [
        { strategyType: "LOWEST_MIXED", enabled: true },
        { strategyType: "SINGLE_SUPPLIER", enabled: true }
      ]
    )).toBe("SINGLE_SUPPLIER");

    expect(resolveMaterialInitialQuoteStrategy(
      [savedItem({ quoteStrategyType: "SINGLE_SUPPLIER" })],
      [
        { strategyType: "LOWEST_MIXED", enabled: true },
        { strategyType: "SINGLE_SUPPLIER", enabled: false }
      ]
    )).toBe("LOWEST_MIXED");
  });

  it("restores a manual quote and its selected unit only when strategy, SKU and unit all match", () => {
    const state = buildMaterialSavedQuoteRestoreState([savedItem()], "LOWEST_MIXED", 14);

    expect(state.actualPriceInputs).toEqual({ "11": "150.25" });
    expect(state.unitSelections).toEqual({ "11": "BOX" });
    expect(state.matchedItemIds).toEqual(["11"]);
    expect(state.useDefaultSelection).toBe(false);
  });

  it.each([
    ["strategy", savedItem(), "SINGLE_SUPPLIER"],
    ["supplier SKU", savedItem({ quoteSupplierSkuId: 999 }), "LOWEST_MIXED"],
    ["pricing unit", savedItem({ quoteSelectedUnit: "CRATE" }), "LOWEST_MIXED"]
  ])("does not restore the saved quote when %s differs", (_reason, item, strategyType) => {
    const state = buildMaterialSavedQuoteRestoreState([item], strategyType, 14);

    expect(state.actualPriceInputs).toEqual({});
    expect(state.unitSelections).toEqual({});
    expect(state.matchedItemIds).toEqual([]);
    expect(state.useDefaultSelection).toBe(true);
  });

  it("treats a matching formula quote as selected without pinning it as a manual price", () => {
    const state = buildMaterialSavedQuoteRestoreState([
      savedItem({ actualQuotePrice: 100, quoteSelectedUnit: "BOX", quoteMarkupPercent: 0 })
    ], "LOWEST_MIXED", 14);

    expect(state.actualPriceInputs).toEqual({});
    expect(state.unitSelections).toEqual({ "11": "BOX" });
    expect(state.matchedItemIds).toEqual(["11"]);
    expect(state.useDefaultSelection).toBe(false);
  });
});
