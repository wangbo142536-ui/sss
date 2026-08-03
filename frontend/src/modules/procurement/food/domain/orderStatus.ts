export type FoodOrderAction = { target: string; label: string; danger?: boolean };

export const foodOrderProgress = [
  "PENDING_CONFIRMATION",
  "CONFIRMED",
  "PREPARING",
  "READY_TO_SHIP",
  "IN_TRANSIT",
  "WAITING_SUPPLY",
  "SUPPLYING",
  "SUPPLIED"
] as const;

export const foodOrderStatusActions: Record<string, FoodOrderAction[]> = {
  PENDING_CONFIRMATION: [
    { target: "PREPARING", label: "确认" },
    { target: "REJECTED", label: "拒绝订单", danger: true }
  ],
  CONFIRMED: [{ target: "READY_TO_SHIP", label: "备货" }],
  PREPARING: [{ target: "READY_TO_SHIP", label: "备货" }],
  READY_TO_SHIP: [{ target: "IN_TRANSIT", label: "发货" }],
  IN_TRANSIT: [{ target: "WAITING_SUPPLY", label: "运输完成" }],
  WAITING_SUPPLY: [],
  SUPPLYING: [],
  SUPPLIED: [],
  REJECTED: [],
  CANCELLED: []
};
