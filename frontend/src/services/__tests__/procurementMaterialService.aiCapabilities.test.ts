import { afterEach, describe, expect, it, vi } from "vitest";
import { getMaterialAiCapabilities, getMaterialDemandComparison } from "../procurementMaterialService";

describe("material AI capability contract", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("按后端契约读取物料大类分析与比价重排能力", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      categoryAnalysisConfigured: true,
      comparisonRerankConfigured: false,
      status: "CONFIGURED"
    }), { status: 200, headers: { "Content-Type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getMaterialAiCapabilities()).resolves.toEqual({
      categoryAnalysisConfigured: true,
      comparisonRerankConfigured: false,
      status: "CONFIGURED"
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/procurement/materials/ai-capabilities",
      expect.objectContaining({ method: "GET" })
    );
  });

  it("保留comparison响应中的真实AI处理结果", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({
      demand: { demandId: 9, status: "COMPARING" },
      strategies: [],
      items: [],
      aiProcessing: {
        status: "MODEL_FALLBACK",
        appliedItemCount: 0,
        fallbackItemCount: 3
      }
    }), { status: 200, headers: { "Content-Type": "application/json" } })));

    await expect(getMaterialDemandComparison(9)).resolves.toMatchObject({
      aiProcessing: {
        status: "MODEL_FALLBACK",
        appliedItemCount: 0,
        fallbackItemCount: 3
      }
    });
  });
});
