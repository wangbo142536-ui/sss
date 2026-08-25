// @ts-nocheck -- Vitest runs this contract in Node; the app does not ship Node typings.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import demandSource from "../pages/FoodDemandPage.vue?raw";
import importOverlaySource from "../components/FoodImportProgressOverlay.vue?raw";
import inquirySource from "../pages/FoodInquiryPage.vue?raw";
import comparisonSource from "../pages/FoodComparisonPage.vue?raw";
import shuttleSelectorSource from "../components/FoodTrafficShuttleSelector.vue?raw";
import orderSource from "../pages/FoodOrderPage.vue?raw";
import afterSalesSource from "../pages/FoodAfterSalesPage.vue?raw";
import settlementDrawerSource from "../components/FoodSettlementDrawer.vue?raw";

const materialUiCss = readFileSync(
  resolve(process.cwd(), "src/modules/procurement/food/styles/food-material-ui.css"),
  "utf8"
);
const quoteCss = readFileSync(
  resolve(process.cwd(), "src/modules/procurement/food/styles/food-quote-glass.css"),
  "utf8"
);
const materialWorkbenchSource = readFileSync(resolve(process.cwd(), "src/views/WorkbenchPage.vue"), "utf8");
const materialTypeSource = readFileSync(resolve(process.cwd(), "src/types/procurementMaterials.ts"), "utf8");
const materialServiceSource = readFileSync(resolve(process.cwd(), "src/services/procurementMaterialService.ts"), "utf8");

describe("food procurement material style contract", () => {
  it("keeps demand entry on the material upload and matching structure", () => {
    expect(demandSource).toContain("food-material-stage");
    expect(demandSource).toContain("food-supply-upload-row");
    expect(demandSource).toContain("food-supply-strip");
    expect(demandSource).toContain("food-match-panel");
  });

  it("keeps an independent food import overlay with only read, identify and import stages", () => {
    expect(demandSource).toContain("FoodImportProgressOverlay");
    expect(importOverlaySource).toContain("food-import-progress-backdrop");
    expect(importOverlaySource).toContain("width: min(680px, 100%)");
    expect(importOverlaySource).toContain('const stages = ["读取", "识别", "导入"]');
    expect(importOverlaySource).toContain("伙食清单导入");
    expect(importOverlaySource).not.toContain("IMPA");
    expect(importOverlaySource).not.toContain("物料");
  });

  it("reuses the material inquiry and comparison card contracts", () => {
    expect(inquirySource).toContain("procurement-query-card is-inquiry");
    expect(inquirySource).toContain("procurement-query-card-list");
    expect(inquirySource).toContain("inquiry-management-panel");
    expect(inquirySource).toContain(':show-expand="true"');
    expect(inquirySource).toContain("openDemand(row)");
    expect(inquirySource).toContain('label="查询"');
    expect(inquirySource).toContain("router.push(`/procurement/food/${row.demandId}`)");
    expect(inquirySource).toContain('label="一键比价"');
    expect(inquirySource).not.toContain("openBuyerQuotes");
    expect(inquirySource).not.toContain('path: "/food/quotes"');
    expect(inquirySource).toContain("quoteDeadlineAt");
    expect(inquirySource).toContain("<span>询价数</span>");
    expect(inquirySource).toContain("<span>报价数</span>");
    expect(inquirySource).not.toContain("food-inquiry-table");
    expect(comparisonSource).toContain("procurement-query-card is-comparison");
    expect(comparisonSource).toContain("procurement-query-card-list");
    expect(comparisonSource).toContain("mixedSupplierCount: 3");
    expect(comparisonSource).toContain("slice(0, targetMixedSupplierCount.value)");
  });

  it("keeps inquiry and deadline times together in one inquiry-card row", () => {
    expect(inquirySource).toContain('<div class="food-inquiry-card__timing">');
    expect(inquirySource).toContain("<span>询价 {{ formatDateTime(row.inquirySentAt) }}</span>");
    expect(inquirySource).toContain("<span>截止 {{ formatDateTime(row.quoteDeadlineAt) }}</span>");
    expect(materialUiCss).toMatch(/\.food-inquiry-card__timing\s*\{[^}]*display:\s*flex;[^}]*flex-wrap:\s*nowrap;/s);
  });

  it("shows inquiry totals above demand items and guards comparison until a quote arrives", () => {
    expect(demandSource).toContain("food-inquiry-summary-card");
    expect(demandSource).toContain("询价截止日期");
    expect(demandSource).toContain("询价供货商");
    expect(demandSource).toContain("报价供货商");
    expect(demandSource).toContain('v-model="form.quoteDeadlineDate"');
    expect(demandSource).toContain('mode="date"');
    expect(demandSource).toContain("quoteDeadlineAt: `${form.value.quoteDeadlineDate}T23:59:59`");
    expect(demandSource).toContain('glyph="比"');
    expect(demandSource).toContain('glyph="报"');
    expect(demandSource).toContain("demandSummary?.supplierCount");
    expect(demandSource).toContain("demandSummary?.submittedQuoteCount");
    expect(demandSource).toContain(':disabled="!canCompare"');
    expect(demandSource).toContain("demandSummary.value.submittedQuoteCount > 0");
    expect(demandSource).toContain("/food/comparison/${demandSummary.value.demandId}");
    expect(demandSource.indexOf("food-inquiry-summary-card")).toBeLessThan(demandSource.indexOf("food-match-panel"));
  });

  it("keeps comparison on the material detail structure with two strategies", () => {
    expect(comparisonSource).toContain("compare-workspace");
    expect(comparisonSource).toContain("compare-supply-card");
    expect(comparisonSource).toContain("compare-strategy-board");
    expect(comparisonSource).toContain("LOWEST_ITEM");
    expect(comparisonSource).toContain("SINGLE_SUPPLIER");
    expect(comparisonSource).toContain("compare-fixed-fee-bar");
    expect(comparisonSource).toContain('option value="SEA">海运');
    expect(comparisonSource).toContain('option value="LAND">陆运');
    expect(comparisonSource).toContain("利润%");
    expect(comparisonSource).toContain("FoodTrafficShuttleSelector");
    expect(shuttleSelectorSource).toContain("compare-traffic-dialog");
    expect(shuttleSelectorSource).toContain("compare-traffic-filter-grid");
    expect(shuttleSelectorSource).toContain("traffic-shuttle-diagram");
    expect(shuttleSelectorSource).toContain("traffic-shuttle-node-point");
    expect(shuttleSelectorSource).toContain("单船价格");
    expect(shuttleSelectorSource).toContain("拼船价格");
    expect(shuttleSelectorSource).toContain("customsFee");
    expect(shuttleSelectorSource).toContain("craneFee");
    expect(shuttleSelectorSource).toContain("traffic-shuttle-vessel-card");
    expect(shuttleSelectorSource).toContain("traffic-shuttle-vessel-tabs");
    expect(shuttleSelectorSource).toContain("预约船舶一");
    expect(shuttleSelectorSource).toContain("预约船舶二");
    expect(shuttleSelectorSource).toContain("traffic-shuttle-node-cargo-fields");
    expect(shuttleSelectorSource).toContain("货物重量（KG）");
    expect(shuttleSelectorSource).toContain("货物体积（平方米）");
    expect(shuttleSelectorSource).toContain("托盘数量");
    expect(shuttleSelectorSource).toContain("抛锚经度");
    expect(shuttleSelectorSource).toContain("抛锚纬度");
    expect(shuttleSelectorSource).toContain("单号检索");
    expect(shuttleSelectorSource).toContain("listFoodDemands");
    expect(comparisonSource).toContain("requestedQuantity * unitPrice");
    expect(comparisonSource).toContain("food-comparison-workspace");
    expect(comparisonSource).not.toContain("MANUAL");
    expect(comparisonSource).toContain('class="compare-quick-filters"');
    expect(comparisonSource).toContain('class="compare-core-filter"');
    expect(comparisonSource).toContain('class="compare-unmatched-filter"');
    expect(comparisonSource).toContain('type="unmatched"');
    expect(comparisonSource).toContain('v-if="isComparisonQualityProduct(row)" type="quality"');
    expect(comparisonSource.indexOf('label="保存比价设置"')).toBeLessThan(comparisonSource.indexOf('label="导出比价报价表"'));
    expect(comparisonSource.indexOf('label="比价策略引擎"')).toBeLessThan(comparisonSource.indexOf('label="导出比价报价表"'));
  });

  it("reuses the material order card and timeline contract", () => {
    expect(orderSource).toContain("purchase-order-query-card");
    expect(orderSource).toContain("purchase-order-query-timeline");
    expect(orderSource).toContain("purchase-order-detail");
    expect(orderSource).toContain("purchase-order-execution-rail");
    expect(orderSource).toContain("purchase-order-settlement-table");
    expect(orderSource).toContain("StableDateTimeInput");
    expect(orderSource).toContain("supplierOrderId: String(row.supplierOrderId)");
    expect(orderSource).toContain("detailSettlementRows");
    expect(orderSource).toContain("purchase-order-query-card__supplier");
    expect(orderSource).toContain('class="food-supplier-delivery-grid__address"');
    expect(orderSource).toContain("<dt>联系人</dt>");
    expect(orderSource).toContain("<dt>联系电话</dt>");
    expect(orderSource).not.toContain("<dt>备注</dt>");
    expect(orderSource).not.toContain('notice.value = "结算状态已更新"');
    expect(comparisonSource).toContain("purchase-order-confirm-dialog");
    expect(comparisonSource).toContain("确认下单船舶");
    expect(comparisonSource).toContain('v-model="orderForm.deliveryAddress"');
    expect(comparisonSource).toContain("具体交付地址");
    expect(comparisonSource).toContain('const comparisonLocked = computed(() => comparison.value?.demand.status === "ORDERED")');
  });

  it("restores the selected material barge node in the order dialog", () => {
    expect(materialTypeSource).toContain("selectedNodeIndex?: number");
    expect(materialServiceSource).toContain('selectedNodeIndex: readNumber(value, "selectedNodeIndex")');
    expect(materialWorkbenchSource).toContain("compareTrafficServiceForm.value.selectedNodeIndex = index");
    expect(materialWorkbenchSource).toContain("selectedNodeIndex: value?.selectedNodeIndex");
    expect(materialWorkbenchSource).toContain("selectedNodeIndex: compareTrafficServiceForm.value.selectedNodeIndex");
    expect(materialWorkbenchSource).toContain("index === purchaseOrderBargeSelectedNodeIndex");
  });

  it("keeps order contact names separate from their phone fields", () => {
    const materialContactStart = materialWorkbenchSource.indexOf('<select v-model="purchaseOrderForm.deliveryContactId"');
    const materialContactEnd = materialWorkbenchSource.indexOf("purchaseOrder.field.deliveryContactPhone", materialContactStart);
    const materialContactMarkup = materialWorkbenchSource.slice(materialContactStart, materialContactEnd);
    const foodContactStart = comparisonSource.indexOf('<select v-model="orderContactId"');
    const foodContactEnd = comparisonSource.indexOf("</select>", foodContactStart);
    const foodContactMarkup = comparisonSource.slice(foodContactStart, foodContactEnd);

    expect(materialContactMarkup).toContain("{{ fallbackDeliveryContact.contactName }}");
    expect(materialContactMarkup).toContain("{{ contact.contactName }}");
    expect(materialContactMarkup).not.toContain("contactPhone");
    expect(foodContactMarkup).toContain("{{ contact.contactName }}");
    expect(foodContactMarkup).not.toContain("contact.contactPhone");
  });

  it("reuses the material settlement and evaluation contracts", () => {
    expect(afterSalesSource).toContain("settlement-management-panel");
    expect(afterSalesSource).toContain("settlement-card__content-row");
    expect(afterSalesSource).toContain("settlement-card__inline-actions");
    expect(afterSalesSource).toContain("FoodSettlementDrawer");
    expect(afterSalesSource).toContain('icon="Eye" label="查看结算"');
    expect(settlementDrawerSource).toContain("编辑结算单");
    expect(settlementDrawerSource).toContain("实际金额");
    expect(settlementDrawerSource).toContain("发票号码");
    expect(settlementDrawerSource).toContain("上传发票图片或文件");
    expect(afterSalesSource).toContain("evaluation-management-panel");
    expect(afterSalesSource).toContain("evaluation-star-display");
  });

  it("locks the material input and panel dimensions", () => {
    expect(materialUiCss).toMatch(/\.food-supply-item input,[\s\S]*?height:\s*32px/);
    expect(materialUiCss).toMatch(/\.food-search\s*\{[\s\S]*?height:\s*40px/);
    expect(materialUiCss).toMatch(/\.food-panel\s*\{[\s\S]*?border-radius:\s*16px/);
  });

  it("keeps the eight-column quote table width without legacy horizontal overflow", () => {
    expect(quoteCss).toMatch(/\.food-quote-table\s*\{[^}]*min-width:\s*1000px;/s);
    expect(quoteCss).not.toContain("min-width: 1320px");
  });
});
