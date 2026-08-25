import { ApiError, getAuthSession } from "@/services/authService";

export const shopSmartImportEndpoints = {
  jobs: "/api/shop/skus/intelligent-imports",
  job: (jobId: string | number) => `/api/shop/skus/intelligent-imports/${encodeURIComponent(String(jobId))}`,
  events: (jobId: string | number) => `/api/shop/skus/intelligent-imports/${encodeURIComponent(String(jobId))}/events`,
  execute: (jobId: string | number) => `/api/shop/skus/intelligent-imports/${encodeURIComponent(String(jobId))}/execute`
} as const;

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

async function readPayload(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function readErrorMessage(payload: unknown): string {
  if (!isRecord(payload)) return "";
  const source = isRecord(payload.data) ? payload.data : payload;
  const value = source.message ?? source.error ?? source.detail ?? source.reason ?? source.code;
  return typeof value === "string" ? value.trim() : "";
}

async function requestAuthorized(endpoint: string, init: RequestInit): Promise<unknown> {
  const session = getAuthSession();
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {})
    }
  });
  const payload = await readPayload(response);
  if (!response.ok) throw new ApiError(readErrorMessage(payload) || `HTTP_${response.status}`, response.status, payload);
  return payload;
}

export function startShopSmartImport(file: File): Promise<unknown> {
  const body = new FormData();
  body.append("file", file);
  return requestAuthorized(shopSmartImportEndpoints.jobs, { method: "POST", body });
}

export function getShopSmartImportJob(jobId: string | number): Promise<unknown> {
  return requestAuthorized(shopSmartImportEndpoints.job(jobId), { method: "GET" });
}

export function executeShopSmartImport(jobId: string | number, previewRowIds: Array<string | number>): Promise<unknown> {
  return requestAuthorized(shopSmartImportEndpoints.execute(jobId), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ previewRowIds: previewRowIds.map((value) => Number(value)).filter(Number.isFinite) })
  });
}
