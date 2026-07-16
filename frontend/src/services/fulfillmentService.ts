import { ApiError, getAuthSession } from "@/services/authService";

export interface BusinessAttachmentPayload {
  fileId?: string;
  fileName: string;
  fileUrl: string;
}

export interface FulfillmentAttachment extends BusinessAttachmentPayload {
  attachmentId: number;
  purchaseOrderId?: number;
  supplierOrderId?: number;
  trafficShuttleId?: number;
  bookingId?: number;
  trafficServiceOrderId?: number;
  providerType: "SUPPLIER" | "BARGE";
  providerName?: string;
  nodeIndex?: number;
  nodeName?: string;
  createdAt?: string;
}

async function requestJson(endpoint: string, init: RequestInit = {}) {
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
  if (!response.ok) throw new ApiError(String(payload?.message || "履约附件操作失败"), response.status, payload);
  return payload;
}

const normalizeAttachment = (row: Record<string, unknown>): FulfillmentAttachment => ({
  attachmentId: Number(row.attachmentId ?? row.id ?? 0),
  purchaseOrderId: row.purchaseOrderId == null ? undefined : Number(row.purchaseOrderId),
  supplierOrderId: row.supplierOrderId == null ? undefined : Number(row.supplierOrderId),
  trafficShuttleId: row.trafficShuttleId == null ? undefined : Number(row.trafficShuttleId),
  bookingId: row.bookingId == null ? undefined : Number(row.bookingId),
  trafficServiceOrderId: row.trafficServiceOrderId == null ? undefined : Number(row.trafficServiceOrderId),
  providerType: String(row.providerType || "SUPPLIER") as FulfillmentAttachment["providerType"],
  providerName: String(row.providerName || ""),
  nodeIndex: row.nodeIndex == null ? undefined : Number(row.nodeIndex),
  nodeName: String(row.nodeName || ""),
  fileId: String(row.fileId || ""),
  fileName: String(row.fileName || "附件"),
  fileUrl: String(row.fileUrl || ""),
  createdAt: String(row.createdAt || "")
});

const rows = (payload: unknown) => {
  const source = payload as { items?: Record<string, unknown>[] };
  const values = Array.isArray(payload) ? payload : Array.isArray(source?.items) ? source.items : [];
  return values.map((row) => normalizeAttachment(row as Record<string, unknown>));
};

export async function listPurchaseFulfillmentAttachments(orderId: number | string) {
  return rows(await requestJson(`/api/procurement/purchase-orders/${orderId}/fulfillment-attachments`));
}

export async function saveSupplierFulfillmentAttachments(orderId: number | string, supplierOrderId: number | string, attachments: BusinessAttachmentPayload[]) {
  return rows(await requestJson(`/api/supplier/purchase-orders/${orderId}/supplier-orders/${supplierOrderId}/fulfillment-attachments`, {
    method: "POST",
    body: JSON.stringify({ attachments })
  }));
}

export async function saveSupplierFulfillmentAttachment(orderId: number | string, supplierOrderId: number | string, attachment: BusinessAttachmentPayload) {
  return saveSupplierFulfillmentAttachments(orderId, supplierOrderId, [attachment]);
}

export async function saveBargeNodeAttachments(
  shuttleId: number | string,
  nodeIndex: number,
  attachments: BusinessAttachmentPayload[],
  options: { nodeName?: string; bookingId?: number; trafficServiceOrderId?: number } = {}
) {
  return rows(await requestJson(`/api/supplier/traffic-shuttles/${shuttleId}/nodes/${nodeIndex}/attachments`, {
    method: "POST",
    body: JSON.stringify({ attachments, ...options })
  }));
}

export async function saveBargeNodeAttachment(shuttleId: number | string, nodeIndex: number, attachment: BusinessAttachmentPayload) {
  return saveBargeNodeAttachments(shuttleId, nodeIndex, [attachment]);
}

export async function listBargeShuttleAttachments(shuttleId: number | string) {
  return rows(await requestJson(`/api/supplier/traffic-shuttles/${shuttleId}/attachments`));
}

export async function deleteFulfillmentAttachment(attachmentId: number | string) {
  const session = getAuthSession();
  const response = await fetch(`/api/supplier/fulfillment-attachments/${encodeURIComponent(String(attachmentId))}`, {
    method: "DELETE",
    headers: {
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {})
    }
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new ApiError(String(payload?.message || "附件删除失败"), response.status, payload);
  }
}
