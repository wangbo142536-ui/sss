export type FoodOrderExecutionInput = {
  orderStatus?: string;
  supplierStatuses?: string[];
  supplyMode?: string;
  fixedProviderType?: string;
  fixedProviderName?: string;
  fixedFreightFee?: number;
  fixedCustomsFee?: number;
  fixedCraneFee?: number;
  fixedOtherFee?: number;
  trafficServiceJson?: string;
};

export type FoodOrderExecutionStage = {
  key: string;
  state: "done" | "active" | "todo";
  progressText: string;
};

export type FoodOrderTransportExecution = {
  kind: "BARGE" | "LAND";
  roleLabel: string;
  providerName: string;
  quoteAmount: number;
  status: string;
  hasSelection: boolean;
};

const supplierPendingStatuses = new Set(["", "PENDING_CONFIRMATION"]);
const supplierStockingCompletedStatuses = new Set([
  "READY_TO_SHIP", "IN_TRANSIT", "WAITING_SUPPLY", "SUPPLYING", "SUPPLIED", "COMPLETED"
]);

function normalized(value?: string) {
  return String(value || "").trim().toUpperCase();
}

function orderStageIndex(value?: string) {
  const status = normalized(value);
  if (status === "PENDING_CONFIRMATION") return 0;
  if (["CONFIRMED", "PREPARING"].includes(status)) return 1;
  if (["READY_TO_SHIP", "IN_TRANSIT"].includes(status)) return 2;
  if (status === "WAITING_SUPPLY") return 3;
  if (status === "SUPPLYING") return 4;
  if (["SUPPLIED", "COMPLETED"].includes(status)) return 5;
  return 0;
}

function trafficSnapshot(json?: string) {
  if (!json?.trim()) return {} as Record<string, unknown>;
  try {
    const value = JSON.parse(json);
    return value && typeof value === "object" ? value as Record<string, unknown> : {};
  } catch {
    return {} as Record<string, unknown>;
  }
}

function snapshotText(snapshot: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = String(snapshot[key] || "").trim();
    if (value) return value;
  }
  return "";
}

export function buildFoodOrderExecution(input: FoodOrderExecutionInput) {
  const supplierStatuses = (input.supplierStatuses || []).map(normalized)
    .filter((status) => !["REJECTED", "CANCELLED"].includes(status));
  const supplierTotal = supplierStatuses.length;
  const supplierConfirmed = supplierStatuses.filter((status) => !supplierPendingStatuses.has(status)).length;
  const supplierStocked = supplierStatuses.filter((status) => supplierStockingCompletedStatuses.has(status)).length;
  const activeIndex = orderStageIndex(input.orderStatus);
  const terminal = ["SUPPLIED", "COMPLETED"].includes(normalized(input.orderStatus));
  const stageProgress = [
    { completed: supplierConfirmed, total: supplierTotal },
    { completed: supplierStocked, total: supplierTotal },
    ...[2, 3, 4, 5].map((index) => ({
      completed: activeIndex > index || (terminal && index === 5) ? 1 : 0,
      total: 1
    }))
  ];
  const stages: FoodOrderExecutionStage[] = stageProgress.map((progress, index) => ({
    key: String(index),
    state: progress.total > 0 && progress.completed >= progress.total
      ? "done"
      : index === activeIndex ? "active" : "todo",
    progressText: `${progress.completed}/${progress.total}`
  }));

  const snapshot = trafficSnapshot(input.trafficServiceJson);
  const kind = normalized(input.supplyMode) === "SEA" || normalized(input.fixedProviderType) === "BARGE"
    ? "BARGE" as const
    : "LAND" as const;
  const providerName = snapshotText(snapshot, [
    "trafficVesselName", "shuttleNo", "supplierCompanyName", "providerName", "serviceNo"
  ]) || String(input.fixedProviderName || "").trim() || (kind === "BARGE" ? "驳船服务" : "陆运服务");
  const quoteAmount = [
    input.fixedFreightFee, input.fixedCustomsFee, input.fixedCraneFee, input.fixedOtherFee
  ].reduce<number>((sum, value) => sum + (Number.isFinite(Number(value)) ? Number(value) : 0), 0);
  const hasSelection = Boolean(
    String(input.fixedProviderName || "").trim()
    || String(input.fixedProviderType || "").trim()
    || String(input.trafficServiceJson || "").trim()
    || quoteAmount > 0
  );

  const transport: FoodOrderTransportExecution = {
    kind,
    roleLabel: kind === "BARGE" ? "驳船" : "陆运",
    providerName,
    quoteAmount,
    status: normalized(input.orderStatus),
    hasSelection
  };
  return { stages, transport };
}
