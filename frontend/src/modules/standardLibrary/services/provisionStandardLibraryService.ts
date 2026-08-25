import { ApiError, getAuthSession } from "@/services/authService";
import type { ProvisionStandardCategory } from "../types/provisionStandardLibrary";

type JsonRecord = Record<string, unknown>;

function text(value: unknown): string {
  return typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
}

function normalize(value: unknown): ProvisionStandardCategory | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  const source = value as JsonRecord;
  const categoryCode = text(source.categoryCode ?? source.code);
  const nameCn = text(source.nameCn);
  if (!categoryCode || !nameCn) return null;
  return {
    categoryCode,
    nameCn,
    nameEn: text(source.nameEn),
    parentCode: text(source.parentCode),
    level: Number(source.level) || 1,
    sortOrder: Number(source.sortOrder) || 0
  };
}

export async function getProvisionStandardCategories(keyword = ""): Promise<ProvisionStandardCategory[]> {
  const query = new URLSearchParams();
  if (keyword.trim()) query.set("keyword", keyword.trim());
  const session = getAuthSession();
  const response = await fetch(`/api/standard-library/provision/categories${query.size ? `?${query}` : ""}`, {
    headers: {
      Accept: "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {})
    }
  });
  if (!response.ok) {
    throw new ApiError("伙食标准分类加载失败", response.status, await response.text());
  }
  const payload: unknown = await response.json();
  return (Array.isArray(payload) ? payload : []).map(normalize).filter((item): item is ProvisionStandardCategory => Boolean(item));
}
