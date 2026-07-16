import { ApiError, getAuthSession } from "@/services/authService";
import type { BusinessAttachmentPayload } from "@/services/fulfillmentService";

export type SettlementScope = "BUYER" | "SUPPLIER" | "BARGE";
export type SettlementSourceType = "SUPPLIER" | "BARGE";

export interface SettlementOrder {
  settlementId: number;
  settlementNo: string;
  purchaseOrderId: number;
  purchaseOrderNo?: string;
  supplierOrderId?: number;
  trafficServiceOrderId?: number;
  providerCompanyId?: number;
  providerName: string;
  buyerCompanyName?: string;
  vesselName?: string;
  settlementType: SettlementSourceType;
  materialType?: string;
  quotedAmount: number;
  actualAmount?: number;
  currency?: string;
  status: string;
  invoiceAttachments?: BusinessAttachmentPayload[];
  createdAt?: string;
  updatedAt?: string;
}

export interface SettlementBatchItem {
  sourceType: SettlementSourceType;
  sourceId?: number;
  quotedAmount: number;
  actualAmount?: number;
}

export interface SettlementListQuery {
  keyword?: string;
  status?: string;
  page?: number;
  size?: number;
}

async function requestSettlementJson(endpoint: string, init: RequestInit = {}) {
  const session = getAuthSession();
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers || {})
    }
  });
  const payload = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError("Settlement request failed", response.status, payload);
  return payload;
}

function buildSettlementQuery(params: Record<string, unknown> = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    search.set(key, String(value));
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

const normalizeSettlement = (value: Record<string, unknown>): SettlementOrder => ({
  settlementId: Number(value.settlementId ?? value.id ?? 0),
  settlementNo: String(value.settlementNo ?? ""),
  purchaseOrderId: Number(value.purchaseOrderId ?? 0),
  purchaseOrderNo: String(value.purchaseOrderNo ?? ""),
  supplierOrderId: value.supplierOrderId == null ? undefined : Number(value.supplierOrderId),
  trafficServiceOrderId: value.trafficServiceOrderId == null ? undefined : Number(value.trafficServiceOrderId),
  providerCompanyId: value.providerCompanyId == null ? undefined : Number(value.providerCompanyId),
  providerName: String(value.providerName ?? "-"),
  buyerCompanyName: String(value.buyerCompanyName ?? ""),
  vesselName: String(value.vesselName ?? ""),
  settlementType: String(value.settlementType ?? "SUPPLIER") as SettlementSourceType,
  materialType: String(value.materialType ?? ""),
  quotedAmount: Number(value.quotedAmount ?? 0),
  actualAmount: value.actualAmount == null ? undefined : Number(value.actualAmount),
  currency: String(value.currency ?? "CNY"),
  status: String(value.status ?? "PENDING"),
  invoiceAttachments: Array.isArray(value.invoiceAttachments) ? value.invoiceAttachments as BusinessAttachmentPayload[] : [],
  createdAt: String(value.createdAt ?? ""),
  updatedAt: String(value.updatedAt ?? "")
});

export async function listSettlements(scope: SettlementScope, query: SettlementListQuery = {}): Promise<SettlementOrder[]> {
  const payload = await requestSettlementJson(`/api/settlements${buildSettlementQuery({ scope, ...query })}`);
  const rows = Array.isArray(payload) ? payload : Array.isArray(payload?.items) ? payload.items : [];
  return rows.map((row: Record<string, unknown>) => normalizeSettlement(row));
}

export async function createSettlementBatch(purchaseOrderId: number, items: SettlementBatchItem[]): Promise<SettlementOrder[]> {
  const payload = await requestSettlementJson("/api/settlements/batch", {
    method: "POST",
    body: JSON.stringify({ purchaseOrderId, items })
  });
  const rows = Array.isArray(payload) ? payload : Array.isArray(payload?.items) ? payload.items : [];
  return rows.map((row: Record<string, unknown>) => normalizeSettlement(row));
}

export async function updateSettlement(settlementId: number, actualAmount: number): Promise<SettlementOrder> {
  return normalizeSettlement(await requestSettlementJson(`/api/settlements/${settlementId}`, {
    method: "PUT",
    body: JSON.stringify({ actualAmount })
  }));
}

export async function submitSettlementInvoice(settlementId: number, actualAmount: number, attachments: BusinessAttachmentPayload[]): Promise<SettlementOrder> {
  return normalizeSettlement(await requestSettlementJson(`/api/settlements/${settlementId}/invoice`, {
    method: "POST",
    body: JSON.stringify({ actualAmount, invoiceAttachments: attachments })
  }));
}

export async function settleSettlement(settlementId: number): Promise<SettlementOrder> {
  return normalizeSettlement(await requestSettlementJson(`/api/settlements/${settlementId}/settle`, { method: "POST" }));
}

export async function paySettlement(settlementId: number): Promise<SettlementOrder> {
  return normalizeSettlement(await requestSettlementJson(`/api/settlements/${settlementId}/pay`, { method: "POST" }));
}

export async function deleteSettlement(settlementId: number): Promise<void> {
  await requestSettlementJson(`/api/settlements/${settlementId}`, { method: "DELETE" });
}
