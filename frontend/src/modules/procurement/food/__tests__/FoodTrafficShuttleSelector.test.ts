import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import FoodTrafficShuttleSelector from "../components/FoodTrafficShuttleSelector.vue";

const foodApi = vi.hoisted(() => ({
  listFoodDemands: vi.fn()
}));

const trafficApi = vi.hoisted(() => ({
  listTrafficAnchorages: vi.fn(),
  listTrafficShuttles: vi.fn()
}));

vi.mock("../services/foodProcurementApi", () => foodApi);
vi.mock("@/services/trafficService", () => trafficApi);

describe("FoodTrafficShuttleSelector", () => {
  beforeEach(() => {
    trafficApi.listTrafficAnchorages.mockResolvedValue([]);
    trafficApi.listTrafficShuttles.mockResolvedValue({
      items: [{
        shuttleId: 17,
        shuttleNo: "SH-17",
        trafficVesselName: "交通艇A",
        departurePoint: "西码头",
        destinationPoint: "秀山东锚地",
        startTime: "2026-07-17T09:00:00",
        returnTime: "2026-07-17T18:00:00",
        basePrice: 3200,
        sharedPrice: 2300,
        customsPrice: 200,
        cranePrice: 300,
        status: "PUBLISHED",
        serviceNodes: [{ nodeName: "节点一", startTime: "09:00", endTime: "11:00" }]
      }],
      total: 1,
      page: 1,
      size: 50
    });
    foodApi.listFoodDemands.mockResolvedValue([]);
  });

  it("matches the material booking drawer fields and keeps vessel slot values isolated", async () => {
    const wrapper = mount(FoodTrafficShuttleSelector, {
      props: {
        open: false,
        initialDemandId: 9,
        initialInquiryNo: "FOOD20260720001",
        initialVesselName: "235456789",
        initialUseTime: "2026-07-17T09:00:00"
      },
      global: { stubs: { Teleport: true } }
    });
    await wrapper.setProps({ open: true });
    await flushPromises();
    await wrapper.get(".traffic-shuttle-node-dot").trigger("click");

    expect(wrapper.findAll(".traffic-shuttle-vessel-tabs button").map((button) => button.text()))
      .toEqual(["预约船舶一", "预约船舶二"]);
    expect(wrapper.text()).toContain("货物重量（KG）");
    expect(wrapper.text()).toContain("货物体积（平方米）");
    expect(wrapper.text()).toContain("托盘数量");
    expect(wrapper.text()).toContain("抛锚经度");
    expect(wrapper.text()).toContain("抛锚纬度");

    const field = (label: string) => {
      const target = wrapper.findAll(".traffic-shuttle-node-settings label")
        .find((item) => item.find("span").text() === label);
      if (!target) throw new Error(`Missing field: ${label}`);
      return target.get("input");
    };

    await field("货物重量（KG）").setValue("120");
    await wrapper.findAll(".traffic-shuttle-vessel-tabs button")[1].trigger("click");
    expect((field("货物重量（KG）").element as HTMLInputElement).value).toBe("");
    await field("货物重量（KG）").setValue("80");
    await wrapper.findAll(".traffic-shuttle-vessel-tabs button")[0].trigger("click");
    expect((field("货物重量（KG）").element as HTMLInputElement).value).toBe("120");

    await wrapper.get('button[aria-label="确认选择"]').trigger("click");
    const selection = wrapper.emitted("select")?.[0]?.[0] as { vesselSlots: Array<{ cargoWeight: string }> };
    expect(selection.vesselSlots.map((slot) => slot.cargoWeight)).toEqual(["120", "80"]);
  });

  it("restores booked node styling and share status from shuttle bookings", async () => {
    trafficApi.listTrafficShuttles.mockResolvedValueOnce({
      items: [{
        shuttleId: 17,
        shuttleNo: "SH-17",
        trafficVesselName: "交通艇A",
        departurePoint: "西码头",
        destinationPoint: "秀山东锚地",
        startTime: "2026-07-17T09:00:00",
        returnTime: "2026-07-17T18:00:00",
        basePrice: 3200,
        sharedPrice: 2300,
        status: "PUBLISHED",
        serviceNodes: [
          { nodeName: "节点一", startTime: "09:00", endTime: "11:00" },
          { nodeName: "节点二", startTime: "12:00", endTime: "15:00" }
        ],
        bookings: [{
          bookingId: 31,
          bookingNo: "TSB-31",
          shuttleServiceId: 17,
          nodeIndex: 1,
          nodeTime: "12:00-15:00",
          allowShare: true,
          status: "BOOKED"
        }]
      }],
      total: 1,
      page: 1,
      size: 50
    });

    const wrapper = mount(FoodTrafficShuttleSelector, {
      props: { open: false, initialUseTime: "2026-07-17T09:00:00" },
      global: { stubs: { Teleport: true } }
    });
    await wrapper.setProps({ open: true });
    await flushPromises();

    const nodes = wrapper.findAll(".traffic-shuttle-node-point");
    expect(nodes[0].classes()).not.toContain("is-joined");
    expect(nodes[1].classes()).toContain("is-joined");
    expect(nodes[1].get(".traffic-shuttle-node-state-label").text()).toBe("可拼");
  });

  it("restores the persisted shuttle and time node as selected when reopened", async () => {
    trafficApi.listTrafficShuttles.mockResolvedValue({
      items: [{
        shuttleId: 17,
        shuttleNo: "SH-17",
        trafficVesselName: "Traffic A",
        departurePoint: "Pier",
        destinationPoint: "Anchorage",
        startTime: "2026-07-17T09:00:00",
        returnTime: "2026-07-17T18:00:00",
        basePrice: 3200,
        sharedPrice: 2300,
        status: "PUBLISHED",
        serviceNodes: [
          { nodeName: "Node 1", startTime: "09:00", endTime: "11:00" },
          { nodeName: "Node 2", startTime: "12:00", endTime: "15:00" }
        ]
      }],
      total: 1,
      page: 1,
      size: 50
    });

    const wrapper = mount(FoodTrafficShuttleSelector, {
      props: {
        open: false,
        initialUseTime: "2026-07-17T09:00:00",
        initialShuttleId: 17,
        initialNodeIndex: 1
      },
      global: { stubs: { Teleport: true } }
    });

    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(wrapper.get(".compare-traffic-shuttle-card").classes()).toContain("is-selected");
    expect(wrapper.findAll(".traffic-shuttle-node-point")[1].classes()).toContain("is-selected");
    expect(wrapper.find(".traffic-shuttle-booking-drawer").exists()).toBe(false);

    await wrapper.setProps({ open: false });
    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(wrapper.findAll(".traffic-shuttle-node-point")[1].classes()).toContain("is-selected");
  });
});
