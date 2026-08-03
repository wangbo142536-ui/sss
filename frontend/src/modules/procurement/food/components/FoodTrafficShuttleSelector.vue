<script setup lang="ts">
import { computed, ref, watch } from "vue";
import IconButton from "@/components/IconButton.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import { listFoodDemands } from "../services/foodProcurementApi";
import type { FoodDemandSummary } from "../types";
import {
  listTrafficAnchorages,
  listTrafficShuttles,
  type TrafficAnchorage,
  type TrafficShuttleNode,
  type TrafficShuttleService
} from "@/services/trafficService";

export interface FoodTrafficShuttleSelection {
  shuttle: TrafficShuttleService;
  nodeIndex: number;
  node: TrafficShuttleNode;
  allowShare: boolean;
  customsService: boolean;
  craneService: boolean;
  craneCount: number;
  freightFee: number;
  customsFee: number;
  craneFee: number;
  demandId?: number;
  inquiryNo?: string;
  vesselName?: string;
  cargoWeight?: string;
  cargoVolume?: string;
  palletCount?: string;
  anchorageTime?: string;
  anchorageLongitude?: string;
  anchorageLatitude?: string;
  vesselSlots: FoodTrafficVesselSlot[];
}

type FoodTrafficVesselSlot = {
  slot: 1 | 2;
  selectedDemand?: FoodDemandSummary;
  inquiryKeyword: string;
  cargoWeight: string;
  cargoVolume: string;
  palletCount: string;
  anchorageTime: string;
  anchorageLongitude: string;
  anchorageLatitude: string;
};

const props = defineProps<{
  open: boolean;
  initialAnchorageCode?: string;
  initialUseTime?: string;
  initialDemandId?: number;
  initialInquiryNo?: string;
  initialVesselName?: string;
  initialSupplyPort?: string;
  initialItemCount?: number;
  initialShuttleId?: number | string;
  initialNodeIndex?: number;
}>();

const emit = defineEmits<{
  close: [];
  select: [selection: FoodTrafficShuttleSelection];
}>();

const anchorages = ref<TrafficAnchorage[]>([]);
const rows = ref<TrafficShuttleService[]>([]);
const anchorageCode = ref("");
const useTime = ref("");
const loading = ref(false);
const error = ref("");
const activeNodeKey = ref("");
const selectedNodeKey = ref("");
const allowShare = ref(false);
const customsService = ref(false);
const craneService = ref(false);
const craneCount = ref(1);
const inquiryKeyword = ref("");
const inquiryOptions = ref<FoodDemandSummary[]>([]);
const inquiryLoading = ref(false);
const inquiryPickerOpen = ref(false);
const selectedDemand = ref<FoodDemandSummary>();
const activeVesselSlot = ref<1 | 2>(1);
const cargoWeight = ref("");
const cargoVolume = ref("");
const palletCount = ref("");
const anchorageTime = ref("");
const anchorageLongitude = ref("");
const anchorageLatitude = ref("");
const vesselSlots = ref<[FoodTrafficVesselSlot, FoodTrafficVesselSlot]>([emptyVesselSlot(1), emptyVesselSlot(2)]);

const visibleAnchorages = computed(() => anchorages.value.filter((item) => item.enabled !== false));
const visibleInquiryOptions = computed(() => {
  const keyword = inquiryKeyword.value.trim().toLowerCase();
  return inquiryOptions.value.filter((item) => !keyword || `${item.inquiryNo || item.demandNo} ${item.vesselName}`.toLowerCase().includes(keyword));
});

function initialDemand(): FoodDemandSummary | undefined {
  if (!props.initialDemandId) return undefined;
  return {
    demandId: props.initialDemandId,
    demandNo: props.initialInquiryNo || "",
    inquiryNo: props.initialInquiryNo,
    vesselName: props.initialVesselName || "",
    supplyPort: props.initialSupplyPort || "",
    vesselEta: props.initialUseTime || "",
    currency: "",
    status: "QUOTED",
    itemCount: Number(props.initialItemCount || 0),
    matchedCount: 0,
    pendingCount: 0,
    supplierCount: 0,
    submittedQuoteCount: 0,
    updatedAt: ""
  };
}

function emptyVesselSlot(slot: 1 | 2): FoodTrafficVesselSlot {
  return {
    slot,
    inquiryKeyword: "",
    cargoWeight: "",
    cargoVolume: "",
    palletCount: "",
    anchorageTime: "",
    anchorageLongitude: "",
    anchorageLatitude: ""
  };
}

function currentSlotSnapshot(): FoodTrafficVesselSlot {
  return {
    slot: activeVesselSlot.value,
    selectedDemand: selectedDemand.value,
    inquiryKeyword: inquiryKeyword.value,
    cargoWeight: cargoWeight.value,
    cargoVolume: cargoVolume.value,
    palletCount: palletCount.value,
    anchorageTime: anchorageTime.value,
    anchorageLongitude: anchorageLongitude.value,
    anchorageLatitude: anchorageLatitude.value
  };
}

function saveActiveVesselSlot() {
  vesselSlots.value[activeVesselSlot.value - 1] = currentSlotSnapshot();
}

function loadVesselSlot(slot: 1 | 2) {
  activeVesselSlot.value = slot;
  const target = vesselSlots.value[slot - 1];
  selectedDemand.value = target.selectedDemand;
  inquiryKeyword.value = target.inquiryKeyword;
  cargoWeight.value = target.cargoWeight;
  cargoVolume.value = target.cargoVolume;
  palletCount.value = target.palletCount;
  anchorageTime.value = target.anchorageTime;
  anchorageLongitude.value = target.anchorageLongitude;
  anchorageLatitude.value = target.anchorageLatitude;
  inquiryPickerOpen.value = false;
}

function setActiveVesselSlot(slot: 1 | 2) {
  saveActiveVesselSlot();
  loadVesselSlot(slot);
}

function vesselSlotLabel(slot: 1 | 2) {
  const demand = slot === activeVesselSlot.value ? selectedDemand.value : vesselSlots.value[slot - 1].selectedDemand;
  return demand?.vesselName || "--";
}

function anchoragePositionLabel() {
  return [anchorageLongitude.value, anchorageLatitude.value].filter(Boolean).join(" / ") || "--";
}

function selectedFreightFee(row: TrafficShuttleService) {
  return Number(allowShare.value ? (row.sharedPrice ?? row.basePrice ?? 0) : (row.basePrice ?? row.sharedPrice ?? 0));
}

function selectedCustomsFee(row: TrafficShuttleService) {
  return customsService.value ? Number(row.customsPrice || 0) : 0;
}

function selectedCraneFee(row: TrafficShuttleService) {
  return craneService.value ? Number(row.cranePrice || 0) * Math.max(1, Math.floor(Number(craneCount.value || 1))) : 0;
}

function comparisonStatusLabel(value?: string) {
  return value === "QUOTED" ? "比价中" : value === "ORDERED" ? "已下单" : "询价中";
}

function defaultNodes(): TrafficShuttleNode[] {
  return [
    { nodeName: "节点一", startTime: "09:00", endTime: "11:00" },
    { nodeName: "节点二", startTime: "12:00", endTime: "15:00" },
    { nodeName: "节点三", startTime: "16:00", endTime: "18:00" }
  ];
}

function nodeRows(row: TrafficShuttleService) {
  return row.serviceNodes?.length ? row.serviceNodes : defaultNodes();
}

function nodeKey(row: TrafficShuttleService, index: number) {
  return `${row.shuttleId}-${index}`;
}

function isSelectedShuttle(row: TrafficShuttleService) {
  const prefix = `${row.shuttleId}-`;
  return selectedNodeKey.value.startsWith(prefix) || activeNodeKey.value.startsWith(prefix);
}

function restoreInitialSelection() {
  selectedNodeKey.value = "";
  const shuttleId = String(props.initialShuttleId ?? "").trim();
  const nodeIndex = Number(props.initialNodeIndex);
  if (!shuttleId || !Number.isInteger(nodeIndex) || nodeIndex < 0) return;
  const row = rows.value.find((item) => String(item.shuttleId) === shuttleId);
  if (!row || nodeIndex >= nodeRows(row).length) return;
  selectedNodeKey.value = nodeKey(row, nodeIndex);
}

function nodeTimeLabel(node: TrafficShuttleNode) {
  return [node.startTime, node.endTime].filter(Boolean).join("-") || "--";
}

function bookingNodeIndex(row: TrafficShuttleService, booking: NonNullable<TrafficShuttleService["bookings"]>[number]) {
  const direct = Number(booking.nodeIndex);
  const nodes = nodeRows(row);
  if (Number.isInteger(direct) && direct >= 0 && direct < nodes.length) return direct;
  const bookingTime = String(booking.nodeTime || booking.remark || "").trim();
  if (!bookingTime) return 0;
  const matched = nodes.findIndex((node) => nodeTimeLabel(node) === bookingTime);
  return matched >= 0 ? matched : 0;
}

function nodeBookings(row: TrafficShuttleService, index: number) {
  return (row.bookings || []).filter((booking) => bookingNodeIndex(row, booking) === index);
}

function nodeVisualState(row: TrafficShuttleService, index: number) {
  return nodeBookings(row, index).length ? "joined" : "available";
}

function nodeDisplayStatus(row: TrafficShuttleService, index: number) {
  const bookings = nodeBookings(row, index);
  if (!bookings.length) return "";
  return bookings.some((booking) => booking.allowShare) ? "可拼" : "已预约";
}

function serviceDateLabel(row: TrafficShuttleService) {
  return String(row.startTime || row.createdAt || "").slice(0, 10);
}

function timeLabel(value?: string) {
  const text = String(value || "");
  return text.includes("T") ? text.slice(11, 16) : text.slice(-5);
}

function moneyLabel(value?: number) {
  return `¥ ${Number(value || 0).toFixed(2)}`;
}

function executionState(row: TrafficShuttleService) {
  const status = String(row.status || "").toUpperCase();
  if (["COMPLETED", "FINISHED"].includes(status)) return "completed";
  if (["IN_PROGRESS", "IN_TRANSIT", "EXECUTING"].includes(status)) return "executing";
  return "idle";
}

function openNode(row: TrafficShuttleService, index: number) {
  activeNodeKey.value = nodeKey(row, index);
  allowShare.value = false;
  customsService.value = false;
  craneService.value = false;
  craneCount.value = 1;
  const demand = initialDemand();
  vesselSlots.value = [
    { ...emptyVesselSlot(1), selectedDemand: demand, inquiryKeyword: props.initialInquiryNo || "", anchorageTime: props.initialUseTime || "" },
    emptyVesselSlot(2)
  ];
  loadVesselSlot(1);
  inquiryPickerOpen.value = false;
}

function closeNode() {
  activeNodeKey.value = "";
  inquiryPickerOpen.value = false;
}

function confirmNode(row: TrafficShuttleService, index: number) {
  const node = nodeRows(row)[index] || defaultNodes()[index] || defaultNodes()[0];
  const count = craneService.value ? Math.max(1, Math.floor(Number(craneCount.value || 1))) : 0;
  saveActiveVesselSlot();
  selectedNodeKey.value = nodeKey(row, index);
  const primarySlot = vesselSlots.value[0];
  emit("select", {
    shuttle: row,
    nodeIndex: index,
    node,
    allowShare: allowShare.value,
    customsService: customsService.value,
    craneService: craneService.value,
    craneCount: count,
    freightFee: selectedFreightFee(row),
    customsFee: selectedCustomsFee(row),
    craneFee: selectedCraneFee(row),
    demandId: primarySlot.selectedDemand?.demandId,
    inquiryNo: primarySlot.selectedDemand?.inquiryNo || primarySlot.selectedDemand?.demandNo,
    vesselName: primarySlot.selectedDemand?.vesselName,
    cargoWeight: primarySlot.cargoWeight,
    cargoVolume: primarySlot.cargoVolume,
    palletCount: primarySlot.palletCount,
    anchorageTime: primarySlot.anchorageTime,
    anchorageLongitude: primarySlot.anchorageLongitude,
    anchorageLatitude: primarySlot.anchorageLatitude,
    vesselSlots: vesselSlots.value.map((slot) => ({ ...slot }))
  });
  closeNode();
}

async function openInquiryPicker() {
  inquiryPickerOpen.value = true;
  if (inquiryOptions.value.length || inquiryLoading.value) return;
  inquiryLoading.value = true;
  try {
    inquiryOptions.value = await listFoodDemands("", "QUOTED");
  } catch {
    inquiryOptions.value = [];
  } finally {
    inquiryLoading.value = false;
  }
}

function updateInquiryKeyword() {
  selectedDemand.value = undefined;
  void openInquiryPicker();
}

function selectInquiry(item: FoodDemandSummary) {
  selectedDemand.value = item;
  inquiryKeyword.value = item.inquiryNo || item.demandNo;
  anchorageTime.value = item.vesselEta || anchorageTime.value;
  inquiryPickerOpen.value = false;
}

function useTimeRange() {
  const date = String(useTime.value || "").slice(0, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(date)
    ? { startTimeFrom: `${date} 00:00`, startTimeTo: `${date} 23:59` }
    : {};
}

async function loadRows() {
  loading.value = true;
  error.value = "";
  try {
    const response = await listTrafficShuttles({
      status: "PUBLISHED",
      anchorageCode: anchorageCode.value,
      ...useTimeRange(),
      page: 1,
      size: 50
    });
    rows.value = [...response.items].sort((left, right) =>
      String(right.startTime || right.createdAt || "").localeCompare(String(left.startTime || left.createdAt || ""))
    );
    restoreInitialSelection();
  } catch (reason) {
    rows.value = [];
    error.value = reason instanceof Error ? reason.message : "驳船服务读取失败";
  } finally {
    loading.value = false;
  }
}

async function initialize() {
  anchorageCode.value = props.initialAnchorageCode || "";
  useTime.value = props.initialUseTime || "";
  activeNodeKey.value = "";
  selectedDemand.value = initialDemand();
  inquiryKeyword.value = props.initialInquiryNo || "";
  inquiryPickerOpen.value = false;
  error.value = "";
  try {
    if (!anchorages.value.length) anchorages.value = await listTrafficAnchorages();
    await loadRows();
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "驳船服务读取失败";
  }
}

watch(() => props.open, (open) => {
  if (open) void initialize();
});
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-backdrop" role="presentation" @click="emit('close')">
        <section class="purchase-order-confirm-dialog traffic-service-dialog compare-traffic-dialog" role="dialog" aria-modal="true" aria-label="选择驳船服务" @click.stop>
          <header>
            <div><strong>查询驳船费用</strong><span>选择服务节点及单船、拼船和附加服务。</span></div>
            <IconButton icon="X" label="关闭" variant="plain" @click="emit('close')" />
          </header>
          <div class="purchase-order-dialog-body">
            <section class="purchase-order-form-grid traffic-service-form-grid compare-traffic-filter-grid">
              <label><span>锚地</span><select v-model="anchorageCode" @change="loadRows"><option value="">全部</option><option v-for="item in visibleAnchorages" :key="item.anchorageCode" :value="item.anchorageCode">{{ item.anchorageName }}</option></select></label>
              <label><span>使用时间</span><StableDateTimeInput v-model="useTime" mode="datetime" @update:model-value="loadRows" /></label>
            </section>
            <div v-if="loading" class="empty-state compact">加载中</div>
            <div v-else-if="!rows.length" class="empty-state compact">暂无可选择驳船</div>
            <section v-else class="traffic-shuttle-visual-list compare-traffic-shuttle-list">
              <article v-for="row in rows" :key="row.shuttleId" :class="['traffic-shuttle-visual-card compare-traffic-shuttle-card', `is-${executionState(row)}`, { 'is-selected': isSelectedShuttle(row) }]">
                <header class="traffic-shuttle-visual-card__head">
                  <div class="traffic-shuttle-visual-title">
                    <div class="traffic-shuttle-visual-name"><strong>{{ [row.trafficVesselName, serviceDateLabel(row)].filter(Boolean).join(' / ') || '-' }}</strong><span>{{ row.shuttleNo || '-' }}</span></div>
                    <div class="traffic-shuttle-price-pair"><span><b>单</b><em>{{ moneyLabel(row.basePrice) }}</em></span><span><b>拼</b><em>{{ moneyLabel(row.sharedPrice) }}</em></span></div>
                    <div class="traffic-shuttle-visual-summary"><small>增值服务：报关 {{ moneyLabel(row.customsPrice) }} / 吊机 {{ moneyLabel(row.cranePrice) }}</small></div>
                  </div>
                </header>
                <section class="traffic-shuttle-diagram" aria-label="驳船服务路线">
                  <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--start"><span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--pier" aria-hidden="true">⛴</span><div><strong>{{ row.departurePoint || '-' }}</strong><em>{{ timeLabel(row.startTime) || '-' }}</em></div></div>
                  <div class="traffic-shuttle-node-track">
                    <div class="traffic-shuttle-dashed-line" aria-hidden="true"></div>
                    <div class="traffic-shuttle-node-strip">
                      <div v-for="(node, index) in nodeRows(row)" :key="nodeKey(row, index)" class="traffic-shuttle-node-point" :class="[`is-${nodeVisualState(row, index)}`, { 'is-selected': selectedNodeKey === nodeKey(row, index) || activeNodeKey === nodeKey(row, index) }]">
                        <span class="traffic-shuttle-node-state-label">{{ nodeDisplayStatus(row, index) }}</span>
                        <button type="button" class="traffic-shuttle-node-dot" :aria-label="`选择${node.nodeName || `节点${index + 1}`} ${nodeTimeLabel(node)}`" @click.stop="openNode(row, index)"></button>
                        <em>{{ nodeTimeLabel(node) }}</em>
                      </div>
                    </div>
                  </div>
                  <div class="traffic-shuttle-endpoint traffic-shuttle-endpoint--end"><span class="traffic-shuttle-endpoint-icon traffic-shuttle-endpoint-icon--anchor" aria-hidden="true">⚓</span><div><strong>{{ row.destinationPoint || row.anchorageName || '-' }}</strong><em>{{ timeLabel(row.returnTime) || '-' }}</em></div></div>
                </section>
                <template v-for="(node, index) in nodeRows(row)" :key="`${nodeKey(row, index)}-drawer`">
                  <Teleport to="body">
                    <div v-if="activeNodeKey === nodeKey(row, index)" class="fulfillment-drawer-backdrop traffic-shuttle-booking-backdrop" role="presentation" @click="closeNode">
                      <div class="traffic-shuttle-node-popover traffic-shuttle-booking-drawer" role="dialog" aria-modal="true" @click.stop>
                        <section class="traffic-shuttle-service-summary">
                          <div><span>交通艇</span><strong>{{ row.trafficVesselName || '--' }}</strong></div><div><span>日期</span><strong>{{ serviceDateLabel(row) || '--' }}</strong></div><div><span>时间节点</span><strong>{{ nodeTimeLabel(node) }}</strong></div><div><span>起始到终点</span><strong>{{ row.departurePoint || '-' }} → {{ row.destinationPoint || row.anchorageName || '-' }}</strong></div><div><span>单船价格</span><strong>{{ moneyLabel(row.basePrice) }}</strong></div><div><span>拼船价格</span><strong>{{ moneyLabel(row.sharedPrice) }}</strong></div><div><span>报关费</span><strong>{{ moneyLabel(row.customsPrice) }}</strong></div><div><span>吊机价格（每吊）</span><strong>{{ moneyLabel(row.cranePrice) }}</strong></div>
                        </section>
                        <section class="traffic-shuttle-node-popover__info">
                          <div class="traffic-shuttle-vessel-tabs" role="tablist" aria-label="预约船舶">
                            <button type="button" :class="{ active: activeVesselSlot === 1 }" @click.stop="setActiveVesselSlot(1)">预约船舶一</button>
                            <button type="button" :class="{ active: activeVesselSlot === 2 }" @click.stop="setActiveVesselSlot(2)">预约船舶二</button>
                          </div>
                          <article :class="['traffic-shuttle-vessel-card', 'traffic-shuttle-vessel-card--tabbed', { 'is-empty': !selectedDemand }]">
                            <dl>
                              <div><dt>预约船舶</dt><dd>{{ vesselSlotLabel(activeVesselSlot) }}</dd></div>
                              <div><dt>货物重量（KG）</dt><dd>{{ cargoWeight || '--' }}</dd></div>
                              <div><dt>货物体积（平方米）</dt><dd>{{ cargoVolume || '--' }}</dd></div>
                              <div><dt>托盘数量</dt><dd>{{ palletCount || '--' }}</dd></div>
                              <div><dt>抛锚时间</dt><dd>{{ anchorageTime?.replace('T', ' ') || '--' }}</dd></div>
                              <div><dt>抛锚经纬度</dt><dd>{{ anchoragePositionLabel() }}</dd></div>
                              <div><dt>运费</dt><dd>{{ moneyLabel(selectedFreightFee(row)) }}</dd></div>
                              <div><dt>报关费</dt><dd>{{ moneyLabel(selectedCustomsFee(row)) }}</dd></div>
                              <div><dt>吊机费</dt><dd>{{ moneyLabel(selectedCraneFee(row)) }}</dd></div>
                            </dl>
                          </article>
                        </section>
                        <section class="traffic-shuttle-node-search">
                          <label class="traffic-shuttle-node-booking__compare">
                            <span>单号检索</span>
                            <div><input v-model="inquiryKeyword" placeholder="输入比价单号或船舶名称" @focus="openInquiryPicker" @input="updateInquiryKeyword" /></div>
                            <div v-if="inquiryPickerOpen" class="traffic-shuttle-inquiry-picker">
                              <p v-if="inquiryLoading">正在加载比价中单据</p>
                              <button v-for="item in visibleInquiryOptions" :key="item.demandId" type="button" @click.stop="selectInquiry(item)"><strong>{{ item.inquiryNo || item.demandNo }}</strong><span>{{ item.vesselName }} / {{ comparisonStatusLabel(item.status) }}</span></button>
                              <p v-if="!inquiryLoading && !visibleInquiryOptions.length">暂无匹配的比价中单据</p>
                            </div>
                          </label>
                        </section>
                        <section class="traffic-shuttle-node-settings">
                          <label><span>拼船</span><select v-model="allowShare"><option :value="true">是</option><option :value="false">否</option></select></label>
                          <label><span>报关</span><select v-model="customsService"><option :value="false">否</option><option :value="true">是</option></select></label>
                          <label><span>吊机</span><select v-model="craneService"><option :value="false">否</option><option :value="true">是</option></select></label>
                          <label><span>吊机次数</span><input v-model.number="craneCount" type="number" min="1" step="1" :disabled="!craneService" /></label>
                          <div class="traffic-shuttle-node-cargo-fields">
                            <label><span>货物重量（KG）</span><input v-model="cargoWeight" placeholder="--" /></label>
                            <label><span>货物体积（平方米）</span><input v-model="cargoVolume" placeholder="--" /></label>
                            <label><span>托盘数量</span><input v-model="palletCount" placeholder="--" /></label>
                            <label><span>抛锚经度</span><input v-model="anchorageLongitude" placeholder="--" /></label>
                            <label><span>抛锚纬度</span><input v-model="anchorageLatitude" placeholder="--" /></label>
                          </div>
                          <div class="traffic-shuttle-node-popover__footer"><IconButton icon="X" label="取消" @click="closeNode" /><IconButton icon="Check" label="确认选择" variant="primary" @click="confirmNode(row, index)" /></div>
                        </section>
                      </div>
                    </div>
                  </Teleport>
                </template>
              </article>
            </section>
            <p v-if="error" class="inline-error">{{ error }}</p>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
