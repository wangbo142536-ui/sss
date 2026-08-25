import type { MaterialComparisonCandidate, MaterialComparisonItem } from "@/types/procurementMaterials";

export interface MaterialSavedQuoteRestoreState {
  actualPriceInputs: Record<string, string>;
  unitSelections: Record<string, string>;
  matchedItemIds: string[];
  useDefaultSelection: boolean;
}

interface MaterialQuoteStrategyOption {
  strategyType: string;
  enabled?: boolean;
}

const normalizeQuoteContextText = (value?: string | null) => String(value ?? "").trim().toUpperCase();

const quoteCandidateForStrategy = (item: MaterialComparisonItem, strategyType: string) =>
  normalizeQuoteContextText(strategyType) === "SINGLE_SUPPLIER"
    ? item.singleSupplierCandidate
    : item.lowestCandidate;

const resolveMatchingSelectedUnit = (candidate: MaterialComparisonCandidate, savedUnit: string) => {
  const normalizedSavedUnit = normalizeQuoteContextText(savedUnit);
  if (!normalizedSavedUnit) return null;
  const unitOptions = candidate.unitPriceOptions?.filter((option) => normalizeQuoteContextText(option.unit)) ?? [];
  const matchingOption = unitOptions.find((option) => normalizeQuoteContextText(option.unit) === normalizedSavedUnit);
  if (matchingOption) {
    return {
      selectedUnit: String(matchingOption.unit).trim(),
      unitPrice: matchingOption.unitPrice ?? candidate.unitPrice
    };
  }
  if (unitOptions.length) return null;
  const currentUnit = candidate.selectedUnit || candidate.stockUnit || candidate.unit;
  if (normalizeQuoteContextText(currentUnit) !== normalizedSavedUnit) return null;
  return {
    selectedUnit: String(currentUnit).trim(),
    unitPrice: candidate.unitPrice
  };
};

const ceilQuoteMoneyToCents = (value: number) => Math.ceil((value - Number.EPSILON) * 100) / 100;

export const resolveMaterialInitialQuoteStrategy = (
  items: MaterialComparisonItem[],
  strategies: MaterialQuoteStrategyOption[]
) => {
  const enabledStrategies = strategies.filter((strategy) => strategy.enabled !== false);
  const savedStrategyType = items.find((item) =>
    item.actualQuotePrice != null
    && Number.isFinite(item.actualQuotePrice)
    && enabledStrategies.some((strategy) =>
      normalizeQuoteContextText(strategy.strategyType) === normalizeQuoteContextText(item.quoteStrategyType)
    )
  )?.quoteStrategyType;
  if (savedStrategyType) {
    return enabledStrategies.find((strategy) =>
      normalizeQuoteContextText(strategy.strategyType) === normalizeQuoteContextText(savedStrategyType)
    )?.strategyType ?? savedStrategyType;
  }
  return enabledStrategies[0]?.strategyType || strategies[0]?.strategyType || "LOWEST_MIXED";
};

export const buildMaterialSavedQuoteRestoreState = (
  items: MaterialComparisonItem[],
  strategyType: string,
  defaultMarkupPercent: number
): MaterialSavedQuoteRestoreState => {
  const actualPriceInputs: Record<string, string> = {};
  const unitSelections: Record<string, string> = {};
  const matchedItemIds: string[] = [];
  let hasSavedQuote = false;
  let hasContextMismatch = false;

  items.forEach((item, index) => {
    if (item.actualQuotePrice == null || !Number.isFinite(item.actualQuotePrice)) return;
    hasSavedQuote = true;
    const itemKey = String(item.demandItemId ?? item.rowNo ?? index);
    const candidate = quoteCandidateForStrategy(item, strategyType);
    const strategyMatches = normalizeQuoteContextText(item.quoteStrategyType) === normalizeQuoteContextText(strategyType);
    const skuMatches = candidate?.skuId != null
      && item.quoteSupplierSkuId != null
      && Number(candidate.skuId) === Number(item.quoteSupplierSkuId);
    const matchingUnit = candidate ? resolveMatchingSelectedUnit(candidate, item.quoteSelectedUnit || "") : null;
    if (!strategyMatches || !skuMatches || !matchingUnit) {
      hasContextMismatch = true;
      return;
    }

    matchedItemIds.push(itemKey);
    unitSelections[itemKey] = matchingUnit.selectedUnit;
    const markupPercent = item.quoteMarkupPercent ?? defaultMarkupPercent;
    const expectedQuotePrice = matchingUnit.unitPrice == null
      ? undefined
      : ceilQuoteMoneyToCents(matchingUnit.unitPrice * (1 + markupPercent / 100));
    const savedQuotePrice = ceilQuoteMoneyToCents(item.actualQuotePrice);
    if (expectedQuotePrice == null || Math.abs(savedQuotePrice - expectedQuotePrice) > 0.01) {
      actualPriceInputs[itemKey] = String(savedQuotePrice);
    }
  });

  return {
    actualPriceInputs,
    unitSelections,
    matchedItemIds,
    useDefaultSelection: !hasSavedQuote || hasContextMismatch
  };
};
