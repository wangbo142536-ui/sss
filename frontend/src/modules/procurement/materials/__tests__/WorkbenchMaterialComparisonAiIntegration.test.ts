// @ts-nocheck -- Vitest runs this source contract in Node; the app does not ship Node typings.
import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import iconButtonSource from "@/components/IconButton.vue?raw";
import strategyDialogSource from "@/modules/procurement/materials/components/MaterialComparisonStrategyDialog.vue?raw";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

const workbenchCss = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");
const strategyCss = readFileSync(resolve(process.cwd(), "src/modules/procurement/materials/styles/material-comparison-strategy.css"), "utf8");

describe("Workbench material comparison AI capability integration", () => {
  it("仅通过能力门控包装真实comparison请求，不创建模拟百分比", () => {
    expect(workbenchSource).toContain("useMaterialComparisonAiOverlay");
    expect(workbenchSource).toContain("runMaterialComparisonAiLoad(() => getMaterialDemandComparison(currentDemandId))");
    expect(workbenchSource).toContain("候选筛选与必要时模型重排");
    expect(workbenchSource).toContain("BackgroundTaskOverlay");
    expect(workbenchSource).not.toContain("compareAiProgressTimer");
  });

  it("补给费非零时利润仍严格等于报价减成本且允许负利润", () => {
    const cnyFormula = workbenchSource.match(/const profitAmount = ([^;]+);/)?.[1];
    const usdFormula = workbenchSource.match(/const profitAmountUsd = ([^;]+);/)?.[1];
    expect(cnyFormula).toBeTruthy();
    expect(usdFormula).toBeTruthy();

    const evaluateCny = new Function("quoteAmount", "costAmount", "fixedFeeAmount", `return ${cnyFormula}`);
    const evaluateUsd = new Function("quoteAmountUsd", "costAmountUsd", "fixedFeeAmountUsd", `return ${usdFormula}`);
    expect(evaluateCny(120, 100, 50)).toBe(20);
    expect(evaluateCny(90, 100, 50)).toBe(-10);
    expect(evaluateUsd(12, 10, 5)).toBe(2);
    expect(evaluateUsd(9, 10, 5)).toBe(-1);

    const checkoutFormula = workbenchSource.match(/const selectedCompareProfitAmount = computed\(\(\) => ([^;]+)\);/)?.[1];
    const detailFormula = workbenchSource.match(/const purchaseDetailProfitAmount = computed\(\(\) => ([^;]+)\);/)?.[1];
    expect(checkoutFormula).toBeTruthy();
    expect(detailFormula).toBeTruthy();
    const evaluateCheckout = new Function(
      "selectedCompareOrderAmount", "selectedCompareCostAmount", "compareFixedFeeTotal",
      `return ${checkoutFormula}`
    );
    const evaluateDetail = new Function(
      "purchaseDetailQuoteAmount", "purchaseDetailCostAmount", "purchaseDetailFixedFeeAmount",
      `return ${detailFormula}`
    );
    expect(evaluateCheckout({ value: 120 }, { value: 100 }, { value: 50 })).toBe(20);
    expect(evaluateDetail({ value: 120 }, { value: 100 }, { value: 50 })).toBe(20);
  });

  it("历史报价恢复和行选择统一经过策略、SKU、单位严格匹配", () => {
    expect(workbenchSource).toContain("buildMaterialSavedQuoteRestoreState");
    expect(workbenchSource).toContain("resolveMaterialInitialQuoteStrategy(response.items, response.strategies)");
    expect(workbenchSource).toContain("restoreCompareSavedQuoteState(selectedStrategy.value)");
    expect(workbenchSource).toContain("compareUnitSelections.value = restored.unitSelections");
    expect(workbenchSource).toContain("compareQuoteActualPrices.value = restored.actualPriceInputs");
    expect(workbenchSource).toContain("restored.useDefaultSelection");
    expect(workbenchSource).toContain("const usesActiveQuoteContext = strategyKey === selectedStrategy.value");
    expect(workbenchSource).toContain("usesActiveQuoteContext ? compareUnitSelections.value[demandLineKey] : \"\"");
    expect(workbenchSource).toContain("usesActiveQuoteContext ? (compareQuoteActualPrices.value[demandLineKey] ?? \"\") : \"\"");

    const strategyRestoreIndex = workbenchSource.indexOf("selectedStrategy.value = resolveMaterialInitialQuoteStrategy(response.items, response.strategies)");
    const stateRestoreIndex = workbenchSource.indexOf("restoreCompareSavedQuoteState(selectedStrategy.value);", strategyRestoreIndex);
    const unitRestoreIndex = workbenchSource.indexOf("compareUnitSelections.value = restored.unitSelections");
    const rowGenerationIndex = workbenchSource.indexOf("const rowsAfterRestore = compareSkuRows.value");
    expect(strategyRestoreIndex).toBeGreaterThan(-1);
    expect(stateRestoreIndex).toBeGreaterThan(strategyRestoreIndex);
    expect(unitRestoreIndex).toBeGreaterThan(-1);
    expect(rowGenerationIndex).toBeGreaterThan(unitRestoreIndex);
  });

  it("保存右侧提供产品维度策略入口并用紧凑图标展示供应商属性", () => {
    const saveIndex = workbenchSource.indexOf('icon="Save" :label="t(\'compare.saveQuote\')"');
    const strategyIndex = workbenchSource.indexOf('icon="Settings" label="比价策略引擎" variant="strategy"');
    expect(saveIndex).toBeGreaterThan(-1);
    expect(strategyIndex).toBeGreaterThan(saveIndex);
    expect(strategyDialogSource).toContain('id="material-strategy-title">比价策略引擎</h2>');
    expect(workbenchSource).not.toContain("优化比价策略");
    expect(strategyDialogSource).not.toContain("优化比价策略");
    expect(workbenchSource).toContain("saveMaterialComparisonStrategy");
    expect(workbenchSource).toContain("supplier.attributeTags");
    expect(workbenchSource).toContain("<MaterialStrategyIcon");
    expect(workbenchSource).toContain("strategyIconType(tag)");
    expect(workbenchSource).not.toContain("运输最快");
    expect(iconButtonSource).toContain('| "strategy"');
  });

  it("保持供货商卡片等高并精简比价工具栏反馈", () => {
    expect(strategyCss).toMatch(/\.strategy-supplier-identity\s*\{[^}]*min-height:\s*24px/s);
    expect(workbenchSource).not.toContain("compare-preference-filters");
    expect(workbenchSource).not.toContain("策略已保存，比价结果已重新生成。");
    expect(workbenchCss).not.toContain("#d76408");
    expect(workbenchCss).not.toContain("rgba(176, 75, 0, 0.18)");
  });

  it("优化策略按钮复用核心商品图标的浅橙色语义色", () => {
    expect(workbenchCss).toContain("--color-strategy-core-surface: #fff1d7;");
    expect(workbenchCss).toContain("--color-strategy-core-ink: #c96b05;");
    expect(workbenchCss).toMatch(/\.ui-icon-button--strategy\s*\{[^}]*background:\s*var\(--color-strategy-core-surface\);[^}]*color:\s*var\(--color-strategy-core-ink\);/s);
    expect(strategyCss).toMatch(/\.material-strategy-icon\.is-core\s*\{[^}]*color:\s*var\(--color-strategy-core-ink\);[^}]*background:\s*var\(--color-strategy-core-surface\);/s);
  });

  it("核心商品在比价明细中使用橙色整行强调", () => {
    expect(workbenchSource).toContain("compareCoreDemandItemIds");
    expect(workbenchSource).toContain('"is-compare-core-product"');
    expect(workbenchSource).toContain("compareCoreDemandItemIds.value.has(Number(compareRow.demandItemId))");
  });

  it("未匹配筛选保留问题明细并且不阻断已选商品下单", () => {
    expect(workbenchSource).toContain('const compareUnmatchedOnly = ref(false)');
    expect(workbenchSource).toContain('class="compare-unmatched-filter"');
    expect(workbenchSource).toContain(':aria-pressed="compareUnmatchedOnly"');
    expect(workbenchSource).toContain('<MaterialStrategyIcon type="unmatched"');
    expect(workbenchSource).not.toContain('{{ t("compare.unmatchedOnly") }}');
    expect(workbenchSource).toContain("const isCompareIssueRow = (row: CompareSkuRow)");
    expect(workbenchSource).toContain("Boolean(row.unmatched) || !selectedCompareRowIds.value.includes(row.id)");
    expect(workbenchSource).toContain("if (compareUnmatchedOnly.value && !isCompareIssueRow(row)) return false;");
    expect(workbenchSource).toContain("compareSkuRows.value.map((row) => buildSelectedPurchaseItem(row))");
    expect(workbenchSource).not.toContain("compareFilteredSkus.value.map((row) => buildSelectedPurchaseItem(row))");
    expect(workbenchCss).toMatch(/\.compare-unmatched-filter\s*\{[^}]*background:\s*#fff1f2;[^}]*color:\s*#b42318;/s);
    expect(strategyCss).toContain(".data-table-row.is-compare-core-product.is-compare-unmatched");
  });

  it("核心商品与未匹配使用相邻紧凑筛选且只改变列表展示", () => {
    expect(workbenchSource).toContain('const compareCoreOnly = ref(false)');
    expect(workbenchSource).toContain('class="compare-quick-filters"');
    expect(workbenchSource).toContain('class="compare-core-filter"');
    expect(workbenchSource).toContain(':aria-pressed="compareCoreOnly"');
    expect(workbenchSource).toContain('type="core"');
    expect(workbenchSource).toContain("if (compareCoreOnly.value && !isCompareCoreProduct(row)) return false;");
    expect(workbenchSource.indexOf('class="compare-core-filter"')).toBeLessThan(workbenchSource.indexOf('class="compare-unmatched-filter"'));
    expect(workbenchSource).toContain('type="unmatched"');
    expect(workbenchCss).toMatch(/\.compare-quick-filters\s*\{[^}]*display:\s*flex;[^}]*gap:\s*6px;/s);
    expect(workbenchCss).toMatch(/\.compare-core-filter\s*\{[^}]*background:\s*var\(--color-strategy-core-surface\);[^}]*color:\s*var\(--color-strategy-core-ink\);/s);
    expect(workbenchCss).toMatch(/\.compare-unmatched-filter\s*\{[^}]*min-width:\s*34px;[^}]*width:\s*34px;/s);
    expect(workbenchSource).toContain("compareSkuRows.value.map((row) => buildSelectedPurchaseItem(row))");
    expect(workbenchSource).not.toContain("compareFilteredSkus.value.map((row) => buildSelectedPurchaseItem(row))");
  });

  it("按当前候选 SKU 的真实质量标签在明细行显示质量图标", () => {
    expect(workbenchSource).toContain("const isCompareQualityProduct = (row: CompareSkuRow)");
    expect(workbenchSource).toContain('tag === "质量高" || tag === "质量好"');
    expect(workbenchSource).toContain('<MaterialStrategyIcon v-if="isCompareQualityProduct(row)" type="quality" label="质量高" compact />');
  });
});
