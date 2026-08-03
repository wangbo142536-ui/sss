import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  actionFoodSettlement,
  actionSupplierFoodOrder,
  commitFoodQuoteImport,
  createFoodOrder,
  exportFoodQuote,
  getBuyerFoodQuote,
  getFoodComparison,
  getFoodDemand,
  getFoodOrder,
  getSupplierFoodQuote,
  listBuyerFoodInquiries,
  listBuyerFoodOrders,
  listFoodDemands,
  listFoodEvaluations,
  listFoodSettlements,
  listFoodSuppliers,
  listSupplierFoodInquiries,
  listSupplierFoodOrders,
  previewFoodDemand,
  previewFoodQuoteImport,
  reviewFoodEvaluation,
  saveFoodDemand,
  saveFoodComparisonSettings,
  saveSupplierFoodQuote,
  sendFoodInquiry,
  submitFoodEvaluation,
  submitFoodQuote,
  virtualFillFoodQuote
} from "../services/foodProcurementApi";
import type { FoodQuote } from "../types";

const fetchMock = vi.fn<typeof fetch>();

describe("food procurement API button contract", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
    fetchMock.mockImplementation(async (input) => {
      const url = String(input);
      if (url.endsWith("/export")) {
        return new Response(new Blob(["xlsx"]), {
          status: 200,
          headers: { "Content-Disposition": "attachment; filename=food-quote.xlsx" }
        });
      }
      return new Response(JSON.stringify({}), { status: 200, headers: { "Content-Type": "application/json" } });
    });
    vi.stubGlobal("fetch", fetchMock);
  });

  it("connects every demand, inquiry, quote, comparison, order, settlement and evaluation action", async () => {
    const file = new File(["sheet"], "food.xlsx", { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    const quote = {
      quoteId: 30,
      items: [{ quoteItemId: 31, requestedQuantity: 10, quotedQuantity: 2, unitPrice: 3, availability: "UNAVAILABLE", priceSource: "MANUAL" }]
    } as FoodQuote;

    await previewFoodDemand(file);
    await saveFoodDemand({ vesselName: "MV", supplyPort: "PORT", vesselEta: "2026-07-17T09:00:00", quoteDeadlineAt: "2026-07-20T23:59:59", currency: "USD", items: [] });
    await saveFoodDemand({ demandId: 10, vesselName: "MV", supplyPort: "PORT", vesselEta: "2026-07-17T09:00:00", quoteDeadlineAt: "2026-07-20T23:59:59", currency: "USD", items: [] });
    await listFoodDemands("MV", "DRAFT");
    await getFoodDemand(10);
    await listFoodSuppliers();
    await sendFoodInquiry(10, [20], 5);
    await listBuyerFoodInquiries();
    await listSupplierFoodInquiries();
    await getBuyerFoodQuote(30);
    await getSupplierFoodQuote(30);
    await saveSupplierFoodQuote(quote);
    await virtualFillFoodQuote(30);
    await submitFoodQuote(30);
    await exportFoodQuote(30);
    await previewFoodQuoteImport(30, file);
    await commitFoodQuoteImport(30, 40);
    await getFoodComparison(10);
    await saveFoodComparisonSettings(10, {
      markupPercent: 10,
      fixedFreightFee: 100,
      fixedCustomsFee: 20,
      fixedCraneFee: 30,
      fixedOtherFee: 5,
      supplyMode: "SEA",
      fixedProviderType: "BARGE",
      fixedProviderId: "9",
      fixedProviderName: "交通艇A",
      trafficServiceJson: "{}",
      selectedDemandItemIds: [11]
    });
    await createFoodOrder(10, "LOWEST_ITEM", undefined, [
      { demandItemId: 11, quoteItemId: 31 }
    ], true);
    await listBuyerFoodOrders();
    await listSupplierFoodOrders();
    await getFoodOrder(50, false);
    await getFoodOrder(50, true);
    await actionSupplierFoodOrder(50, 51, "CONFIRMED", { expectedReadyAt: "2026-07-17T09:00:00" });
    await listFoodSettlements(false);
    await listFoodSettlements(true);
    await actionFoodSettlement(60, "INVOICED", true, {
      invoiceNo: "INV-1",
      actualAmount: 88.5,
      invoiceAttachments: [{ fileId: "9", fileName: "invoice.pdf", fileUrl: "/api/files/9" }]
    });
    await listFoodEvaluations();
    await submitFoodEvaluation(70, 5, 4, "good", [{ fileId: "10", fileName: "proof.pdf", fileUrl: "/api/files/10" }]);
    await reviewFoodEvaluation(70, "APPROVED");

    const calls = fetchMock.mock.calls.map(([input, init]) => `${String(init?.method || "GET")} ${String(input)}`);
    expect(calls).toEqual(expect.arrayContaining([
      "POST /api/procurement/food/demands/match-preview",
      "POST /api/procurement/food/demands",
      "PUT /api/procurement/food/demands/10",
      "GET /api/procurement/food/demands?keyword=MV&status=DRAFT",
      "GET /api/procurement/food/demands/10",
      "GET /api/procurement/food/suppliers",
      "POST /api/procurement/food/demands/10/send-inquiry",
      "GET /api/procurement/food/inquiries",
      "GET /api/supplier/food/inquiries",
      "GET /api/procurement/food/quotes/30",
      "GET /api/supplier/food/quotes/30",
      "PUT /api/supplier/food/quotes/30/draft",
      "POST /api/supplier/food/quotes/30/virtual-fill",
      "POST /api/supplier/food/quotes/30/submit",
      "GET /api/supplier/food/quotes/30/export",
      "POST /api/supplier/food/quotes/30/import-preview",
      "POST /api/supplier/food/quotes/30/imports/40/commit",
      "GET /api/procurement/food/demands/10/comparison",
      "PUT /api/procurement/food/demands/10/comparison-settings",
      "POST /api/procurement/food/demands/10/purchase-orders",
      "GET /api/procurement/food/purchase-orders",
      "GET /api/supplier/food/purchase-orders",
      "GET /api/procurement/food/purchase-orders/50",
      "GET /api/supplier/food/purchase-orders/50",
      "POST /api/supplier/food/purchase-orders/50/supplier-orders/51/action",
      "GET /api/procurement/food/settlements",
      "GET /api/supplier/food/settlements",
      "POST /api/supplier/food/settlements/60/action",
      "GET /api/procurement/food/evaluations",
      "POST /api/procurement/food/evaluations/70/submit",
      "POST /api/procurement/food/evaluations/70/review"
    ]));
    expect(calls).toHaveLength(31);
    const inquiryCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/demands/10/send-inquiry"));
    expect(JSON.parse(String(inquiryCall?.[1]?.body))).toEqual({ supplierCompanyIds: [20], quoteValidityDays: 5 });
    const saveQuoteCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/quotes/30/draft"));
    expect(JSON.parse(String(saveQuoteCall?.[1]?.body))).toEqual({
      items: [{
        quoteItemId: 31,
        quotedQuantity: 10,
        unitPrice: 3,
        availability: "AVAILABLE",
        priceSource: "MANUAL"
      }]
    });
    const saveComparisonCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/demands/10/comparison-settings"));
    expect(JSON.parse(String(saveComparisonCall?.[1]?.body))).toEqual(expect.objectContaining({
      selectedDemandItemIds: [11]
    }));
    const createOrderCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/demands/10/purchase-orders"));
    expect(JSON.parse(String(createOrderCall?.[1]?.body))).toEqual({
      strategyType: "LOWEST_ITEM",
      allowPartial: true,
      selectedItems: [{ demandItemId: 11, quoteItemId: 31 }]
    });
    const settlementCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/settlements/60/action"));
    expect(JSON.parse(String(settlementCall?.[1]?.body))).toEqual({
      targetStatus: "INVOICED",
      invoiceNo: "INV-1",
      actualAmount: 88.5,
      invoiceAttachments: [{ fileId: "9", fileName: "invoice.pdf", fileUrl: "/api/files/9" }]
    });
    const evaluationCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/evaluations/70/submit"));
    expect(JSON.parse(String(evaluationCall?.[1]?.body))).toEqual({
      qualityRating: 5,
      logisticsRating: 4,
      comment: "good",
      attachments: [{ fileId: "10", fileName: "proof.pdf", fileUrl: "/api/files/10" }]
    });
  });
});
