import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services/authService", () => ({
  ApiError: class ApiError extends Error {},
  getAuthSession: () => ({ token: "test-token" })
}));

import { listSupplierDirectoryProducts } from "../services/supplierDirectoryService";

function sku(index: number) {
  return {
    id: index,
    productName: `商品${index}`,
    productType: "MATERIAL",
    stockQty: 1,
    unitPrice: 1
  };
}

describe("supplier directory product pagination", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("按服务端实际100条分页继续加载，详情数量与数据库汇总155条一致", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      const page = Number(new URL(url, "http://localhost").searchParams.get("page"));
      const items = page === 1
        ? Array.from({ length: 100 }, (_, index) => sku(index + 1))
        : Array.from({ length: 55 }, (_, index) => sku(index + 101));
      return new Response(JSON.stringify({ items, total: 155, page, size: 100 }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    const rows = await listSupplierDirectoryProducts(259);

    expect(rows).toHaveLength(155);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(String(fetchMock.mock.calls[1]?.[0])).toContain("page=2");
  });

  it("保留服务商商品标签和规格明细供只读详情展示", async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify({
      items: [{
        ...sku(1),
        productTags: ["热卖", "质量好"],
        specifications: [{ attributeId: 9, key: "size", name: "规格", value: "20L", unit: "桶" }]
      }],
      total: 1
    }), { status: 200, headers: { "Content-Type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    const [row] = await listSupplierDirectoryProducts(259);

    expect(row.productTags).toEqual(["热卖", "质量好"]);
    expect(row.specItems).toEqual([{ id: "9", key: "size", name: "规格", value: "20L", unit: "桶" }]);
  });
});
