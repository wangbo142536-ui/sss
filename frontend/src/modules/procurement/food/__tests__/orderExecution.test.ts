import { describe, expect, it } from "vitest";
import { buildFoodOrderExecution } from "../domain/orderExecution";

describe("food order transport execution", () => {
  it("uses suppliers for confirmation and stocking, then one barge for transport and supply", () => {
    const execution = buildFoodOrderExecution({
      orderStatus: "PENDING_CONFIRMATION",
      supplierStatuses: ["PENDING_CONFIRMATION", "PENDING_CONFIRMATION", "PENDING_CONFIRMATION"],
      supplyMode: "SEA",
      fixedProviderType: "BARGE",
      fixedProviderName: "交通艇A",
      fixedFreightFee: 2800,
      fixedCustomsFee: 200,
      fixedCraneFee: 300,
      fixedOtherFee: 0
    });

    expect(execution.transport.kind).toBe("BARGE");
    expect(execution.transport.hasSelection).toBe(true);
    expect(execution.transport.providerName).toBe("交通艇A");
    expect(execution.transport.quoteAmount).toBe(3300);
    expect(execution.stages.map((stage) => stage.progressText)).toEqual([
      "0/3", "0/3", "0/1", "0/1", "0/1", "0/1"
    ]);
  });

  it("also keeps every transport and supply stage at one executor for land delivery", () => {
    const execution = buildFoodOrderExecution({
      orderStatus: "WAITING_SUPPLY",
      supplierStatuses: ["WAITING_SUPPLY", "WAITING_SUPPLY"],
      supplyMode: "LAND",
      fixedProviderType: "SUPPLIER",
      fixedProviderName: "供货商配送",
      fixedFreightFee: 120
    });

    expect(execution.transport.kind).toBe("LAND");
    expect(execution.stages.map((stage) => stage.progressText)).toEqual([
      "2/2", "2/2", "1/1", "0/1", "0/1", "0/1"
    ]);
  });

  it("restores the barge provider from the saved traffic snapshot", () => {
    const execution = buildFoodOrderExecution({
      orderStatus: "IN_TRANSIT",
      supplierStatuses: ["IN_TRANSIT"],
      supplyMode: "SEA",
      fixedProviderType: "BARGE",
      trafficServiceJson: JSON.stringify({ shuttleNo: "SH-20260723-021", trafficVesselName: "交通艇B" })
    });

    expect(execution.transport.providerName).toBe("交通艇B");
    expect(execution.stages[2].progressText).toBe("0/1");
    expect(execution.stages[2].state).toBe("active");
  });

  it("does not invent a barge card or settlement for historical orders with only the old SEA default", () => {
    const execution = buildFoodOrderExecution({
      orderStatus: "SUPPLIED",
      supplierStatuses: ["SUPPLIED"],
      supplyMode: "SEA"
    });

    expect(execution.transport.hasSelection).toBe(false);
  });
});
