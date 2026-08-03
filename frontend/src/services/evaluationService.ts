import { ApiError, getAuthSession } from "@/services/authService";
import type { BusinessAttachmentPayload } from "@/services/fulfillmentService";

export interface ServiceEvaluation {
  source: "MATERIAL" | "FOOD";
  sourceKey: string;
  evaluationId: number;
  settlementId: number;
  settlementNo?: string;
  purchaseOrderId: number;
  purchaseOrderNo?: string;
  providerName: string;
  serviceType: string;
  rating?: number;
  logisticsRating?: number;
  content?: string;
  status: string;
  attachments: BusinessAttachmentPayload[];
  reviewRemark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ServiceEvaluationListQuery {
  keyword?: string;
  status?: string;
  page?: number;
  size?: number;
}

async function requestJson(endpoint: string, init: RequestInit = {}) {
  const session = getAuthSession();
  const response = await fetch(endpoint, {
    ...init,
    headers: { "Content-Type": "application/json", ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}), ...(init.headers || {}) }
  });
  const payload = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(String(payload?.message || "评价操作失败"), response.status, payload);
  return payload;
}

function buildQuery(params: Record<string, unknown> = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    search.set(key, String(value));
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

const normalize = (row: Record<string, unknown>): ServiceEvaluation => ({
  source: "MATERIAL",
  sourceKey: `MATERIAL:${Number(row.evaluationId ?? row.id ?? 0)}`,
  evaluationId: Number(row.evaluationId ?? row.id ?? 0),
  settlementId: Number(row.settlementId ?? 0),
  settlementNo: String(row.settlementNo || ""),
  purchaseOrderId: Number(row.purchaseOrderId ?? 0),
  purchaseOrderNo: String(row.purchaseOrderNo || ""),
  providerName: String(row.providerName || "-"),
  serviceType: String(row.serviceType || "-"),
  rating: row.rating == null ? undefined : Number(row.rating),
  logisticsRating: row.logisticsRating == null ? undefined : Number(row.logisticsRating),
  content: String(row.content || ""),
  status: String(row.status || "PENDING_EVALUATION"),
  attachments: Array.isArray(row.attachments) ? row.attachments as BusinessAttachmentPayload[] : [],
  reviewRemark: String(row.reviewRemark || ""),
  createdAt: String(row.createdAt || ""),
  updatedAt: String(row.updatedAt || "")
});

const normalizeFood = (row: Record<string, unknown>): ServiceEvaluation => ({
  source: "FOOD",
  sourceKey: `FOOD:${Number(row.evaluationId ?? row.id ?? 0)}`,
  evaluationId: Number(row.evaluationId ?? row.id ?? 0),
  settlementId: 0,
  purchaseOrderId: Number(row.orderId ?? 0),
  purchaseOrderNo: String(row.orderNo || ""),
  providerName: String(row.supplierName || "-"),
  serviceType: "伙食采购",
  rating: row.qualityRating == null ? undefined : Number(row.qualityRating),
  logisticsRating: row.logisticsRating == null ? undefined : Number(row.logisticsRating),
  content: String(row.comment || ""),
  status: String(row.status || "PENDING_REVIEW"),
  attachments: Array.isArray(row.attachments) ? row.attachments as BusinessAttachmentPayload[] : [],
  reviewRemark: String(row.reviewRemark || ""),
  updatedAt: String(row.updatedAt || "")
});

export async function listServiceEvaluations(scope: "BUYER" | "REGULATORY", query: ServiceEvaluationListQuery = {}) {
  const payload = await requestJson(`/api/evaluations${buildQuery({ scope, ...query })}`);
  const values = Array.isArray(payload) ? payload : Array.isArray(payload?.items) ? payload.items : [];
  return values.map((row: Record<string, unknown>) => normalize(row));
}

export async function listRegulatoryFoodEvaluations(query: ServiceEvaluationListQuery = {}) {
  const payload = await requestJson(`/api/regulatory/food/evaluations${buildQuery({ ...query })}`);
  const values = Array.isArray(payload) ? payload : Array.isArray(payload?.items) ? payload.items : [];
  return values.map((row: Record<string, unknown>) => normalizeFood(row));
}

export async function submitServiceEvaluation(evaluationId: number, rating: number, content: string, attachments: BusinessAttachmentPayload[], logisticsRating?: number) {
  return normalize(await requestJson(`/api/evaluations/${evaluationId}/submit`, { method: "POST", body: JSON.stringify({ rating, logisticsRating, content, attachments }) }));
}

export async function reviewServiceEvaluation(evaluationId: number, action: "approve" | "reject", remark: string) {
  return normalize(await requestJson(`/api/regulatory/evaluations/${evaluationId}/${action}`, { method: "POST", body: JSON.stringify({ remark }) }));
}

export async function reviewRegulatoryFoodEvaluation(evaluationId: number, action: "approve" | "reject", reviewRemark: string) {
  return normalizeFood(await requestJson(`/api/regulatory/food/evaluations/${evaluationId}/${action}`, {
    method: "POST",
    body: JSON.stringify({ reviewRemark })
  }));
}
