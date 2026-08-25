import { afterEach, describe, expect, it, vi } from "vitest";
import { getImpaStandardItemPage } from "../standardLibraryService";

describe("IMPA standard library pagination", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("requests a page and preserves backend pagination metadata", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      items: [{ impaCode: "615001", categoryCode: "61", nameCn: "第二页物料" }],
      total: 125,
      page: 2,
      pageSize: 50,
      totalPages: 3
    }), { status: 200, headers: { "Content-Type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getImpaStandardItemPage({ categoryCode: "61", page: 2, pageSize: 50 })).resolves.toMatchObject({
      total: 125,
      page: 2,
      pageSize: 50,
      totalPages: 3,
      items: [{ impaCode: "615001" }]
    });
    expect(String(fetchMock.mock.calls[0][0])).toContain("page=2");
    expect(String(fetchMock.mock.calls[0][0])).toContain("pageSize=50");
  });
});
