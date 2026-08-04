import { ApiError, getAuthSession } from "@/services/authService";
import type {
  CustomsBusinessType,
  CustomsDeclarationContext,
  CustomsDeclarationPage,
  CustomsDeclarationRecord
} from "../types";

const endpoint = "/api/customs/declarations";

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const session = getAuthSession();
  const response = await fetch(url, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers || {})
    }
  });
  const text = await response.text();
  const payload = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!response.ok) {
    const message = payload && typeof payload === "object" && "message" in payload
      ? String((payload as { message?: unknown }).message || "")
      : `HTTP_${response.status}`;
    throw new ApiError(message, response.status, payload);
  }
  return payload as T;
}

function params(values: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  return query.toString();
}

export function getCustomsContext(businessType: CustomsBusinessType, purchaseOrderId: number) {
  return requestJson<CustomsDeclarationContext>(`${endpoint}/context?${params({ businessType, purchaseOrderId })}`);
}

export function declareCustoms(businessType: CustomsBusinessType, purchaseOrderId: number) {
  return requestJson<CustomsDeclarationRecord>(endpoint, {
    method: "POST",
    body: JSON.stringify({ businessType, purchaseOrderId })
  });
}

export function listCustomsDeclarations(query: {
  keyword?: string;
  status?: string;
  deliveryDate?: string;
  page?: number;
  size?: number;
} = {}) {
  return requestJson<CustomsDeclarationPage>(`${endpoint}?${params(query)}`);
}

export async function downloadCustomsAttachment(id: number, fileName = "附件一-凯珀物资上船通知单.pdf") {
  const session = getAuthSession();
  const response = await fetch(`${endpoint}/${encodeURIComponent(String(id))}/attachment`, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  });
  if (!response.ok) throw new ApiError(`HTTP_${response.status}`, response.status, await response.text());
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
