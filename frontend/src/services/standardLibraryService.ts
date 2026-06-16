import type { ImpaStandardItem, StandardCategoryNode } from "@/types/standardLibrary";

const MATERIAL_CATEGORY_ENDPOINT = "/api/standard-library/impa/categories";
const IMPA_ITEMS_ENDPOINT = "/api/standard-library/impa/items";

export interface StandardCategoryResult {
  categories: StandardCategoryNode[];
  error?: string;
}

function normalizeCategories(payload: unknown): StandardCategoryNode[] {
  if (Array.isArray(payload)) return payload as StandardCategoryNode[];
  if (payload && typeof payload === "object" && "data" in payload) {
    const data = (payload as { data?: unknown }).data;
    if (Array.isArray(data)) return data as StandardCategoryNode[];
  }
  return [];
}

function normalizeItems(payload: unknown): ImpaStandardItem[] {
  if (Array.isArray(payload)) return payload as ImpaStandardItem[];
  if (payload && typeof payload === "object" && "data" in payload) {
    const data = (payload as { data?: unknown }).data;
    if (Array.isArray(data)) return data as ImpaStandardItem[];
  }
  return [];
}

export async function getImpaStandardCategories(): Promise<StandardCategoryNode[]> {
  if (typeof window === "undefined" || !window.fetch) {
    return [];
  }

  const response = await fetch(MATERIAL_CATEGORY_ENDPOINT);
  if (!response.ok) {
    return [];
  }

  return normalizeCategories(await response.json());
}

export async function getImpaStandardItems(params: {
  categoryCode?: string;
  segmentCode?: string;
  keyword?: string;
  limit?: number;
}): Promise<ImpaStandardItem[]> {
  if (typeof window === "undefined" || !window.fetch) {
    return [];
  }

  const query = new URLSearchParams();
  if (params.categoryCode) query.set("categoryCode", params.categoryCode);
  if (params.segmentCode) query.set("segmentCode", params.segmentCode);
  if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
  query.set("limit", String(params.limit ?? 80));

  const response = await fetch(`${IMPA_ITEMS_ENDPOINT}?${query.toString()}`);
  if (!response.ok) {
    return [];
  }

  return normalizeItems(await response.json());
}

export async function getMaterialStandardCategories(): Promise<StandardCategoryResult> {
  if (typeof window === "undefined" || !window.fetch) {
    return { categories: [] };
  }

  try {
    const response = await fetch(MATERIAL_CATEGORY_ENDPOINT);
    if (!response.ok) {
      return { categories: [], error: `Category API returned ${response.status}.` };
    }

    const categories = normalizeCategories(await response.json());
    if (!categories.length) {
      return { categories: [] };
    }

    return { categories };
  } catch (error) {
    return {
      categories: [],
      error: error instanceof Error ? error.message : "Category API request failed."
    };
  }
}
