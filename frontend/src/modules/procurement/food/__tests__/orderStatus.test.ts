import { describe, expect, it } from "vitest";
import { foodOrderProgress, foodOrderStatusActions } from "../domain/orderStatus";

describe("food order button and status matrix", () => {
  it("matches the material supplier flow with one action per visible stage", () => {
    expect(foodOrderStatusActions.PENDING_CONFIRMATION.find((action) => !action.danger))
      .toEqual({ target: "PREPARING", label: "确认" });
    expect(foodOrderStatusActions.CONFIRMED)
      .toEqual([{ target: "READY_TO_SHIP", label: "备货" }]);
    expect(foodOrderStatusActions.PREPARING)
      .toEqual([{ target: "READY_TO_SHIP", label: "备货" }]);
    expect(foodOrderStatusActions.READY_TO_SHIP)
      .toEqual([{ target: "IN_TRANSIT", label: "发货" }]);
    expect(foodOrderStatusActions.IN_TRANSIT)
      .toEqual([{ target: "WAITING_SUPPLY", label: "运输完成" }]);
    expect(foodOrderStatusActions.WAITING_SUPPLY).toEqual([]);
    expect(foodOrderStatusActions.SUPPLYING).toEqual([]);
  });

  it("offers rejection only while the order is pending confirmation", () => {
    expect(foodOrderStatusActions.PENDING_CONFIRMATION).toContainEqual({ target: "REJECTED", label: "拒绝订单", danger: true });
    for (const status of foodOrderProgress.slice(1)) {
      expect(foodOrderStatusActions[status].some((action) => action.target === "REJECTED")).toBe(false);
    }
  });

  it("has no execution buttons for terminal states", () => {
    expect(foodOrderStatusActions.SUPPLIED).toEqual([]);
    expect(foodOrderStatusActions.REJECTED).toEqual([]);
    expect(foodOrderStatusActions.CANCELLED).toEqual([]);
  });
});
