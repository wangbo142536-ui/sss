import { ApiError, getAuthSession, isAuthExpiredError } from "@/services/authService";

export interface TrafficAnchorage {
  anchorageId?: number;
  anchorageCode: string;
  anchorageName: string;
  seaArea: string;
  enabled?: boolean;
  sortOrder?: number;
}

export interface TrafficServiceCargo {
  cargoId?: number;
  cargoName: string;
  weightKg?: number;
  volumeCbm?: number;
}

export interface TrafficServiceOrder {
  serviceOrderId: number;
  serviceNo: string;
  purchaseOrderId?: number;
  supplierCompanyId?: number;
  supplierCompanyName?: string;
  feeType?: string;
  seaArea: string;
  anchorageCode: string;
  anchorageName: string;
  useTime: string;
  serviceType: string;
  passengerType: string;
  passengerCount?: number;
  cargoType: string;
  returnTrip: boolean;
  allowShare: boolean;
  basePrice?: number;
  sharedPrice?: number;
  status: string;
  remark: string;
  businessContactId?: number;
  businessContactName?: string;
  businessContactPhone?: string;
  acceptedAt?: string;
  trafficVesselId?: number;
  trafficVesselName?: string;
  handlerContactId?: number;
  handlerName?: string;
  handlerPhone?: string;
  supplierMessage?: string;
  departureTime?: string;
  arrivalTime?: string;
  returnStartTime?: string;
  returnEndTime?: string;
  signPhotoUrl?: string;
  pickupPhotoUrl?: string;
  returnArrivalPhotoUrl?: string;
  bookingId?: number;
  shuttleServiceId?: number;
  shuttleNo?: string;
  shuttleDeparturePoint?: string;
  shuttleDestinationPoint?: string;
  shuttleStartTime?: string;
  shuttleReturnTime?: string;
  shuttleServiceNodes?: TrafficShuttleNode[];
  bookingNodeIndex?: number;
  bookingNodeName?: string;
  bookingNodeTime?: string;
  bookingAmount?: number;
  bookingFreightFee?: number;
  bookingCustomsFee?: number;
  bookingCraneFee?: number;
  createdAt: string;
  updatedAt?: string;
  cargos: TrafficServiceCargo[];
}

export interface TrafficServiceOrderPayload {
  seaArea: string;
  anchorageCode: string;
  feeType?: string;
  useTime?: string;
  serviceType: string;
  passengerType: string;
  passengerCount?: number;
  cargoType: string;
  returnTrip: boolean;
  allowShare: boolean;
  basePrice?: number;
  sharedPrice?: number;
  remark?: string;
  businessContactId?: number;
  businessContactName?: string;
  businessContactPhone?: string;
  acceptedAt?: string;
  trafficVesselId?: number;
  trafficVesselName?: string;
  handlerContactId?: number;
  handlerName?: string;
  handlerPhone?: string;
  supplierMessage?: string;
  departureTime?: string;
  arrivalTime?: string;
  returnStartTime?: string;
  returnEndTime?: string;
  signPhotoUrl?: string;
  pickupPhotoUrl?: string;
  returnArrivalPhotoUrl?: string;
  cargos?: TrafficServiceCargo[];
}

export interface TrafficServiceOrderListResponse {
  items: TrafficServiceOrder[];
  total: number;
  page: number;
  size: number;
}

export interface TrafficServiceRequest {
  requestId: number;
  requestNo: string;
  requesterCompanyId?: number;
  demandId?: number;
  purchaseOrderId?: number;
  feeType?: string;
  seaArea: string;
  anchorageCode: string;
  anchorageName: string;
  useTime?: string;
  serviceType: string;
  passengerType: string;
  passengerCount?: number;
  cargoType: string;
  returnTrip: boolean;
  allowShare: boolean;
  remark?: string;
  status: string;
  recommendedQuoteId?: number;
  selectedQuoteId?: number;
  selectedSupplierCompanyId?: number;
  trafficServiceOrderId?: number;
  createdAt?: string;
  cargos: TrafficServiceCargo[];
}

export interface TrafficServiceQuote {
  quoteId: number;
  requestId: number;
  supplierCompanyId?: number;
  supplierCompanyName?: string;
  quoteAmount?: number;
  basePrice?: number;
  sharedPrice?: number;
  currency: string;
  availableStartTime?: string;
  availableReturnTime?: string;
  trafficVesselId?: number;
  trafficVesselName?: string;
  contactName?: string;
  contactPhone?: string;
  message?: string;
  status: string;
  recommended: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface TrafficServiceRequestDetail {
  request: TrafficServiceRequest | null;
  quotes: TrafficServiceQuote[];
  events: Array<{ eventId: number; requestId: number; quoteId?: number; eventType: string; eventMessage?: string; createdAt?: string }>;
}

export interface TrafficServiceRequestListResponse {
  items: TrafficServiceRequest[];
  total: number;
  page: number;
  size: number;
}

export interface TrafficServiceRequestPayload {
  demandId?: number;
  purchaseOrderId?: number;
  feeType?: string;
  seaArea: string;
  anchorageCode: string;
  useTime?: string;
  serviceType: string;
  passengerType: string;
  passengerCount?: number;
  cargoType: string;
  returnTrip: boolean;
  allowShare: boolean;
  remark?: string;
  publish?: boolean;
  cargos?: TrafficServiceCargo[];
}

export interface TrafficServiceQuotePayload {
  quoteAmount?: number;
  basePrice?: number;
  sharedPrice?: number;
  currency?: string;
  availableStartTime?: string;
  availableReturnTime?: string;
  trafficVesselId?: number;
  trafficVesselName?: string;
  contactName?: string;
  contactPhone?: string;
  message?: string;
}

export interface TrafficShuttleService {
  shuttleId: number;
  shuttleServiceId?: number;
  shuttleNo: string;
  supplierCompanyId?: number;
  supplierCompanyName?: string;
  seaArea: string;
  anchorageCode: string;
  anchorageName: string;
  departurePoint?: string;
  destinationPoint?: string;
  startTime: string;
  returnTime?: string;
  basePrice?: number;
  sharedPrice?: number;
  customsPrice?: number;
  cranePrice?: number;
  passengerCapacity?: number;
  cargoCapacityKg?: number;
  cargoCapacityCbm?: number;
  bookedPassengerCount?: number;
  bookedCargoWeightKg?: number;
  bookedCargoVolumeCbm?: number;
  trafficVesselId?: number;
  trafficVesselName?: string;
  status: string;
  remark?: string;
  serviceNodes?: TrafficShuttleNode[];
  bookings?: TrafficShuttleBooking[];
  createdAt?: string;
}

export interface TrafficShuttleNode {
  nodeName?: string;
  startTime?: string;
  endTime?: string;
}

export interface TrafficShuttleListResponse {
  items: TrafficShuttleService[];
  total: number;
  page: number;
  size: number;
}

export interface TrafficShuttlePayload {
  seaArea: string;
  anchorageCode: string;
  departurePoint?: string;
  destinationPoint?: string;
  startTime: string;
  returnTime?: string;
  basePrice?: number;
  sharedPrice?: number;
  customsPrice?: number;
  cranePrice?: number;
  passengerCapacity?: number;
  cargoCapacityKg?: number;
  cargoCapacityCbm?: number;
  trafficVesselId?: number;
  trafficVesselName?: string;
  status?: string;
  remark?: string;
  serviceNodes?: TrafficShuttleNode[];
}

export interface TrafficShuttleBookingPayload {
  bookingId?: number;
  demandId?: number;
  purchaseOrderId?: number;
  requestNo?: string;
  nodeIndex?: number;
  nodeName?: string;
  nodeTime?: string;
  vesselName?: string;
  vesselImo?: string;
  anchorageTime?: string;
  anchoragePosition?: string;
  palletCount?: string;
  allowShare?: boolean;
  customsService?: boolean;
  craneService?: boolean;
  craneCount?: number;
  passengerCount?: number;
  cargoSummary?: string;
  cargoWeightKg?: number;
  cargoVolumeCbm?: number;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
}

export interface TrafficShuttleExecutionPayload {
  cargoWeightKg?: number;
  cargoVolumeCbm?: number;
  palletCount?: string;
  anchoragePosition?: string;
  longitude?: number;
  latitude?: number;
  craneCount?: number;
}

export interface TrafficShuttleBooking {
  bookingId: number;
  bookingNo: string;
  shuttleServiceId: number;
  requesterCompanyId?: number;
  requestId?: number;
  purchaseOrderId?: number;
  requestNo?: string;
  nodeIndex?: number;
  nodeName?: string;
  nodeTime?: string;
  vesselName?: string;
  vesselImo?: string;
  anchorageTime?: string;
  anchoragePosition?: string;
  palletCount?: string;
  trafficServiceOrderId?: number;
  passengerCount?: number;
  cargoSummary?: string;
  cargoWeightKg?: number;
  cargoVolumeCbm?: number;
  amount?: number;
  allowShare?: boolean;
  customsService?: boolean;
  craneService?: boolean;
  craneCount?: number;
  freightFee?: number;
  customsFee?: number;
  craneFee?: number;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
  status: string;
  createdAt?: string;
}

export interface TrafficBoatPrice {
  priceId?: number;
  supplierCompanyId?: number;
  anchorageCode: string;
  anchorageName: string;
  seaArea: string;
  basePrice?: number;
  sharedPrice?: number;
  enabled: boolean;
  remark?: string;
  updatedAt?: string;
}

export interface TrafficLowestPrice {
  supplierCompanyId?: number;
  supplierCompanyName?: string;
  anchorageCode: string;
  anchorageName: string;
  allowShare: boolean;
  basePrice?: number;
  sharedPrice?: number;
  amount?: number;
}

export interface TrafficRoutePlan {
  routePlanId: number;
  routeNo: string;
  routeName: string;
  serviceDate: string;
  seaArea?: string;
  supplierCompanyId?: number;
  supplierCompanyName?: string;
  trafficVesselId?: number;
  trafficVesselName?: string;
  plannedDepartureTime?: string;
  plannedFinishTime?: string;
  allowShare: boolean;
  orderCount: number;
  totalIncome?: number;
  estimatedCost?: number;
  estimatedProfit?: number;
  status: string;
  remark?: string;
  createdAt?: string;
}

export interface TrafficRouteStop {
  routeStopId: number;
  routePlanId: number;
  trafficServiceOrderId: number;
  stopSequence: number;
  serviceNo: string;
  anchorageCode: string;
  anchorageName: string;
  plannedServiceTime?: string;
  serviceType?: string;
  contactName?: string;
  contactPhone?: string;
  amount?: number;
  allowShare: boolean;
  status: string;
  remark?: string;
}

export interface TrafficRouteEvent {
  eventId: number;
  routePlanId: number;
  trafficServiceOrderId?: number;
  eventType: string;
  eventMessage?: string;
  createdAt?: string;
}

export interface TrafficRouteDetail {
  route: TrafficRoutePlan | null;
  stops: TrafficRouteStop[];
  events: TrafficRouteEvent[];
}

export interface TrafficRoutePlanPayload {
  routeName: string;
  serviceDate: string;
  seaArea?: string;
  supplierCompanyId?: number;
  supplierCompanyName?: string;
  trafficVesselId?: number;
  trafficVesselName?: string;
  plannedDepartureTime?: string;
  plannedFinishTime?: string;
  allowShare?: boolean;
  estimatedCost?: number;
  remark?: string;
}

export interface TrafficRouteStopPayload {
  trafficServiceOrderId: number;
  plannedServiceTime?: string;
  remark?: string;
}

export interface TrafficRoutePlanListResponse {
  items: TrafficRoutePlan[];
  total: number;
  page: number;
  size: number;
}

export interface TrafficBoatPricePayload {
  anchorageCode: string;
  basePrice?: number;
  sharedPrice?: number;
  enabled?: boolean;
  remark?: string;
}

const ANCHORAGE_ENDPOINT = "/api/traffic/anchorages";
const TRAFFIC_SERVICE_ENDPOINT = "/api/traffic/services";
const TRAFFIC_SERVICE_REQUEST_ENDPOINT = "/api/traffic/service-requests";
const SUPPLIER_TRAFFIC_SERVICE_REQUEST_ENDPOINT = "/api/supplier/traffic-service-requests";
const SUPPLIER_TRAFFIC_SERVICE_QUOTE_ENDPOINT = "/api/supplier/traffic-service-quotes";
const TRAFFIC_ROUTE_ENDPOINT = "/api/traffic/routes";
const TRAFFIC_BOAT_PRICE_ENDPOINT = "/api/supplier/traffic-boat/prices";
const TRAFFIC_SHUTTLE_ENDPOINT = "/api/traffic/shuttles";
const SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT = "/api/supplier/traffic-shuttles";

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function unwrapPayload(payload: unknown): unknown {
  if (!isRecord(payload)) return payload;
  if ("data" in payload) return payload.data;
  if ("result" in payload) return payload.result;
  return payload;
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function readString(source: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "string") return value.trim();
  }
  return "";
}

function readNumber(source: Record<string, unknown>, keys: string[]): number | undefined {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "number" && Number.isFinite(value)) return value;
    if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value);
  }
  return undefined;
}

function readBoolean(source: Record<string, unknown>, keys: string[], fallback = false): boolean {
  for (const key of keys) {
    const value = source[key];
    if (typeof value === "boolean") return value;
    if (typeof value === "number") return value !== 0;
  }
  return fallback;
}

function createApiError(response: Response, payload: unknown): ApiError {
  const message = isRecord(payload) ? readString(payload, ["message", "error", "detail", "reason"]) : "";
  return new ApiError(message || `HTTP_${response.status}`, response.status, payload);
}

async function requestTrafficJson(endpoint: string, init: RequestInit = {}): Promise<unknown> {
  const session = getAuthSession();
  const response = await fetch(endpoint, {
    ...init,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {})
    }
  });
  const payload = await readJson(response);
  if (!response.ok) {
    if (isAuthExpiredError(createApiError(response, payload))) {
      throw createApiError(response, payload);
    }
    throw createApiError(response, payload);
  }
  return unwrapPayload(payload);
}

function normalizeAnchorage(value: unknown): TrafficAnchorage {
  const source = isRecord(value) ? value : {};
  return {
    anchorageId: readNumber(source, ["anchorageId", "id"]),
    anchorageCode: readString(source, ["anchorageCode", "code"]),
    anchorageName: readString(source, ["anchorageName", "name"]),
    seaArea: readString(source, ["seaArea", "area"]),
    enabled: readBoolean(source, ["enabled"], true),
    sortOrder: readNumber(source, ["sortOrder"])
  };
}

function normalizeCargo(value: unknown): TrafficServiceCargo {
  const source = isRecord(value) ? value : {};
  return {
    cargoId: readNumber(source, ["cargoId", "id"]),
    cargoName: readString(source, ["cargoName", "name"]),
    weightKg: readNumber(source, ["weightKg"]),
    volumeCbm: readNumber(source, ["volumeCbm"])
  };
}

function normalizeOrder(value: unknown): TrafficServiceOrder {
  const source = isRecord(value) ? value : {};
  const cargos = Array.isArray(source.cargos) ? source.cargos.map(normalizeCargo) : [];
  return {
    serviceOrderId: readNumber(source, ["serviceOrderId", "id"]) ?? 0,
    serviceNo: readString(source, ["serviceNo"]),
    purchaseOrderId: readNumber(source, ["purchaseOrderId"]),
    supplierCompanyId: readNumber(source, ["supplierCompanyId"]),
    supplierCompanyName: readString(source, ["supplierCompanyName"]),
    feeType: readString(source, ["feeType"]) || "FREIGHT",
    seaArea: readString(source, ["seaArea"]),
    anchorageCode: readString(source, ["anchorageCode"]),
    anchorageName: readString(source, ["anchorageName"]),
    useTime: readString(source, ["useTime"]),
    serviceType: readString(source, ["serviceType"]),
    passengerType: readString(source, ["passengerType"]),
    passengerCount: readNumber(source, ["passengerCount"]),
    cargoType: readString(source, ["cargoType"]),
    returnTrip: readBoolean(source, ["returnTrip"]),
    allowShare: readBoolean(source, ["allowShare"]),
    basePrice: readNumber(source, ["basePrice"]),
    sharedPrice: readNumber(source, ["sharedPrice"]),
    status: readString(source, ["status"]) || "ACTIVE",
    remark: readString(source, ["remark"]),
    businessContactId: readNumber(source, ["businessContactId"]),
    businessContactName: readString(source, ["businessContactName"]),
    businessContactPhone: readString(source, ["businessContactPhone"]),
    acceptedAt: readString(source, ["acceptedAt"]),
    trafficVesselId: readNumber(source, ["trafficVesselId"]),
    trafficVesselName: readString(source, ["trafficVesselName"]),
    handlerContactId: readNumber(source, ["handlerContactId"]),
    handlerName: readString(source, ["handlerName"]),
    handlerPhone: readString(source, ["handlerPhone"]),
    supplierMessage: readString(source, ["supplierMessage"]),
    departureTime: readString(source, ["departureTime"]),
    arrivalTime: readString(source, ["arrivalTime"]),
    returnStartTime: readString(source, ["returnStartTime"]),
    returnEndTime: readString(source, ["returnEndTime"]),
    signPhotoUrl: readString(source, ["signPhotoUrl"]),
    pickupPhotoUrl: readString(source, ["pickupPhotoUrl"]),
    returnArrivalPhotoUrl: readString(source, ["returnArrivalPhotoUrl"]),
    bookingId: readNumber(source, ["bookingId"]),
    shuttleServiceId: readNumber(source, ["shuttleServiceId"]),
    shuttleNo: readString(source, ["shuttleNo"]),
    shuttleDeparturePoint: readString(source, ["shuttleDeparturePoint"]),
    shuttleDestinationPoint: readString(source, ["shuttleDestinationPoint"]),
    shuttleStartTime: readString(source, ["shuttleStartTime"]),
    shuttleReturnTime: readString(source, ["shuttleReturnTime"]),
    shuttleServiceNodes: normalizeShuttleNodes(source.shuttleServiceNodes),
    bookingNodeIndex: readNumber(source, ["bookingNodeIndex"]),
    bookingNodeName: readString(source, ["bookingNodeName"]),
    bookingNodeTime: readString(source, ["bookingNodeTime"]),
    bookingAmount: readNumber(source, ["bookingAmount"]),
    bookingFreightFee: readNumber(source, ["bookingFreightFee"]),
    bookingCustomsFee: readNumber(source, ["bookingCustomsFee"]),
    bookingCraneFee: readNumber(source, ["bookingCraneFee"]),
    createdAt: readString(source, ["createdAt"]),
    updatedAt: readString(source, ["updatedAt"]),
    cargos
  };
}

function normalizeRequest(value: unknown): TrafficServiceRequest {
  const source = isRecord(value) ? value : {};
  return {
    requestId: readNumber(source, ["requestId", "id"]) ?? 0,
    requestNo: readString(source, ["requestNo"]),
    requesterCompanyId: readNumber(source, ["requesterCompanyId"]),
    demandId: readNumber(source, ["demandId"]),
    purchaseOrderId: readNumber(source, ["purchaseOrderId"]),
    feeType: readString(source, ["feeType"]) || "FREIGHT",
    seaArea: readString(source, ["seaArea"]),
    anchorageCode: readString(source, ["anchorageCode"]),
    anchorageName: readString(source, ["anchorageName"]),
    useTime: readString(source, ["useTime"]),
    serviceType: readString(source, ["serviceType"]) || "GOODS",
    passengerType: readString(source, ["passengerType"]) || "NORMAL",
    passengerCount: readNumber(source, ["passengerCount"]),
    cargoType: readString(source, ["cargoType"]) || "CARGO",
    returnTrip: readBoolean(source, ["returnTrip"]),
    allowShare: readBoolean(source, ["allowShare"]),
    remark: readString(source, ["remark"]),
    status: readString(source, ["status"]) || "DRAFT",
    recommendedQuoteId: readNumber(source, ["recommendedQuoteId"]),
    selectedQuoteId: readNumber(source, ["selectedQuoteId"]),
    selectedSupplierCompanyId: readNumber(source, ["selectedSupplierCompanyId"]),
    trafficServiceOrderId: readNumber(source, ["trafficServiceOrderId"]),
    createdAt: readString(source, ["createdAt"]),
    cargos: Array.isArray(source.cargos) ? source.cargos.map(normalizeCargo) : []
  };
}

function normalizeQuote(value: unknown): TrafficServiceQuote {
  const source = isRecord(value) ? value : {};
  return {
    quoteId: readNumber(source, ["quoteId", "id"]) ?? 0,
    requestId: readNumber(source, ["requestId"]) ?? 0,
    supplierCompanyId: readNumber(source, ["supplierCompanyId"]),
    supplierCompanyName: readString(source, ["supplierCompanyName"]),
    quoteAmount: readNumber(source, ["quoteAmount"]),
    basePrice: readNumber(source, ["basePrice"]),
    sharedPrice: readNumber(source, ["sharedPrice"]),
    currency: readString(source, ["currency"]) || "CNY",
    availableStartTime: readString(source, ["availableStartTime"]),
    availableReturnTime: readString(source, ["availableReturnTime"]),
    trafficVesselId: readNumber(source, ["trafficVesselId"]),
    trafficVesselName: readString(source, ["trafficVesselName"]),
    contactName: readString(source, ["contactName"]),
    contactPhone: readString(source, ["contactPhone"]),
    message: readString(source, ["message"]),
    status: readString(source, ["status"]) || "SUBMITTED",
    recommended: readBoolean(source, ["recommended"]),
    createdAt: readString(source, ["createdAt"]),
    updatedAt: readString(source, ["updatedAt"])
  };
}

function normalizeRequestDetail(value: unknown): TrafficServiceRequestDetail {
  const source = isRecord(value) ? value : {};
  return {
    request: source.request ? normalizeRequest(source.request) : null,
    quotes: Array.isArray(source.quotes) ? source.quotes.map(normalizeQuote) : [],
    events: Array.isArray(source.events)
      ? source.events.map((item) => {
          const event = isRecord(item) ? item : {};
          return {
            eventId: readNumber(event, ["eventId", "id"]) ?? 0,
            requestId: readNumber(event, ["requestId"]) ?? 0,
            quoteId: readNumber(event, ["quoteId"]),
            eventType: readString(event, ["eventType"]),
            eventMessage: readString(event, ["eventMessage"]),
            createdAt: readString(event, ["createdAt"])
          };
        })
      : []
  };
}

function normalizeShuttle(value: unknown): TrafficShuttleService {
  const source = isRecord(value) ? value : {};
  const shuttleServiceId = readNumber(source, ["shuttleServiceId"]);
  return {
    shuttleId: readNumber(source, ["shuttleId", "id"]) ?? shuttleServiceId ?? 0,
    shuttleServiceId,
    shuttleNo: readString(source, ["shuttleNo"]),
    supplierCompanyId: readNumber(source, ["supplierCompanyId"]),
    supplierCompanyName: readString(source, ["supplierCompanyName"]),
    seaArea: readString(source, ["seaArea"]),
    anchorageCode: readString(source, ["anchorageCode"]),
    anchorageName: readString(source, ["anchorageName"]),
    departurePoint: readString(source, ["departurePoint"]),
    destinationPoint: readString(source, ["destinationPoint"]),
    startTime: readString(source, ["startTime"]),
    returnTime: readString(source, ["returnTime"]),
    basePrice: readNumber(source, ["basePrice"]),
    sharedPrice: readNumber(source, ["sharedPrice"]),
    customsPrice: readNumber(source, ["customsPrice", "customsClearancePrice"]),
    cranePrice: readNumber(source, ["cranePrice", "liftingPrice"]),
    passengerCapacity: readNumber(source, ["passengerCapacity"]),
    cargoCapacityKg: readNumber(source, ["cargoCapacityKg"]),
    cargoCapacityCbm: readNumber(source, ["cargoCapacityCbm"]),
    bookedPassengerCount: readNumber(source, ["bookedPassengerCount"]),
    bookedCargoWeightKg: readNumber(source, ["bookedCargoWeightKg"]),
    bookedCargoVolumeCbm: readNumber(source, ["bookedCargoVolumeCbm"]),
    trafficVesselId: readNumber(source, ["trafficVesselId"]),
    trafficVesselName: readString(source, ["trafficVesselName"]),
    status: readString(source, ["status"]) || "PUBLISHED",
    remark: readString(source, ["remark"]),
    serviceNodes: normalizeShuttleNodes(source.serviceNodes),
    bookings: Array.isArray(source.bookings) ? source.bookings.map(normalizeBooking) : [],
    createdAt: readString(source, ["createdAt"])
  };
}

function normalizeShuttleNodes(value: unknown): TrafficShuttleNode[] {
  if (!Array.isArray(value)) return [];
  return value
    .filter(isRecord)
    .map((node) => ({
      nodeName: readString(node, ["nodeName", "name"]),
      startTime: readString(node, ["startTime", "start"]),
      endTime: readString(node, ["endTime", "end"])
    }))
    .filter((node) => node.nodeName || node.startTime || node.endTime);
}

function normalizeBooking(value: unknown): TrafficShuttleBooking {
  const source = isRecord(value) ? value : {};
  return {
    bookingId: readNumber(source, ["bookingId", "id"]) ?? 0,
    bookingNo: readString(source, ["bookingNo"]),
    shuttleServiceId: readNumber(source, ["shuttleServiceId"]) ?? 0,
    requesterCompanyId: readNumber(source, ["requesterCompanyId"]),
    requestId: readNumber(source, ["requestId"]),
    purchaseOrderId: readNumber(source, ["purchaseOrderId"]),
    requestNo: readString(source, ["requestNo"]),
    nodeIndex: readNumber(source, ["nodeIndex"]),
    nodeName: readString(source, ["nodeName"]),
    nodeTime: readString(source, ["nodeTime"]),
    vesselName: readString(source, ["vesselName"]),
    vesselImo: readString(source, ["vesselImo"]),
    anchorageTime: readString(source, ["anchorageTime"]),
    anchoragePosition: readString(source, ["anchoragePosition"]),
    palletCount: readString(source, ["palletCount"]),
    trafficServiceOrderId: readNumber(source, ["trafficServiceOrderId"]),
    passengerCount: readNumber(source, ["passengerCount"]),
    cargoSummary: readString(source, ["cargoSummary"]),
    cargoWeightKg: readNumber(source, ["cargoWeightKg"]),
    cargoVolumeCbm: readNumber(source, ["cargoVolumeCbm"]),
    amount: readNumber(source, ["amount"]),
    allowShare: readBoolean(source, ["allowShare"]),
    customsService: readBoolean(source, ["customsService"]),
    craneService: readBoolean(source, ["craneService"]),
    craneCount: readNumber(source, ["craneCount"]),
    freightFee: readNumber(source, ["freightFee"]),
    customsFee: readNumber(source, ["customsFee"]),
    craneFee: readNumber(source, ["craneFee"]),
    contactName: readString(source, ["contactName"]),
    contactPhone: readString(source, ["contactPhone"]),
    remark: readString(source, ["remark"]),
    status: readString(source, ["status"]) || "BOOKED",
    createdAt: readString(source, ["createdAt"])
  };
}

function normalizePrice(value: unknown): TrafficBoatPrice {
  const source = isRecord(value) ? value : {};
  return {
    priceId: readNumber(source, ["priceId", "id"]),
    supplierCompanyId: readNumber(source, ["supplierCompanyId"]),
    anchorageCode: readString(source, ["anchorageCode"]),
    anchorageName: readString(source, ["anchorageName"]),
    seaArea: readString(source, ["seaArea"]),
    basePrice: readNumber(source, ["basePrice"]),
    sharedPrice: readNumber(source, ["sharedPrice"]),
    enabled: readBoolean(source, ["enabled"]),
    remark: readString(source, ["remark"]),
    updatedAt: readString(source, ["updatedAt"])
  };
}

function normalizeLowestPrice(value: unknown): TrafficLowestPrice {
  const source = isRecord(value) ? value : {};
  return {
    supplierCompanyId: readNumber(source, ["supplierCompanyId"]),
    supplierCompanyName: readString(source, ["supplierCompanyName"]),
    anchorageCode: readString(source, ["anchorageCode"]),
    anchorageName: readString(source, ["anchorageName"]),
    allowShare: readBoolean(source, ["allowShare"]),
    basePrice: readNumber(source, ["basePrice"]),
    sharedPrice: readNumber(source, ["sharedPrice"]),
    amount: readNumber(source, ["amount"])
  };
}

function normalizeRoute(value: unknown): TrafficRoutePlan {
  const source = isRecord(value) ? value : {};
  return {
    routePlanId: readNumber(source, ["routePlanId", "id"]) ?? 0,
    routeNo: readString(source, ["routeNo"]),
    routeName: readString(source, ["routeName"]),
    serviceDate: readString(source, ["serviceDate"]),
    seaArea: readString(source, ["seaArea"]),
    supplierCompanyId: readNumber(source, ["supplierCompanyId"]),
    supplierCompanyName: readString(source, ["supplierCompanyName"]),
    trafficVesselId: readNumber(source, ["trafficVesselId"]),
    trafficVesselName: readString(source, ["trafficVesselName"]),
    plannedDepartureTime: readString(source, ["plannedDepartureTime"]),
    plannedFinishTime: readString(source, ["plannedFinishTime"]),
    allowShare: readBoolean(source, ["allowShare"], true),
    orderCount: readNumber(source, ["orderCount"]) ?? 0,
    totalIncome: readNumber(source, ["totalIncome"]),
    estimatedCost: readNumber(source, ["estimatedCost"]),
    estimatedProfit: readNumber(source, ["estimatedProfit"]),
    status: readString(source, ["status"]) || "DRAFT",
    remark: readString(source, ["remark"]),
    createdAt: readString(source, ["createdAt"])
  };
}

function normalizeRouteStop(value: unknown): TrafficRouteStop {
  const source = isRecord(value) ? value : {};
  return {
    routeStopId: readNumber(source, ["routeStopId", "id"]) ?? 0,
    routePlanId: readNumber(source, ["routePlanId"]) ?? 0,
    trafficServiceOrderId: readNumber(source, ["trafficServiceOrderId"]) ?? 0,
    stopSequence: readNumber(source, ["stopSequence"]) ?? 0,
    serviceNo: readString(source, ["serviceNo"]),
    anchorageCode: readString(source, ["anchorageCode"]),
    anchorageName: readString(source, ["anchorageName"]),
    plannedServiceTime: readString(source, ["plannedServiceTime"]),
    serviceType: readString(source, ["serviceType"]),
    contactName: readString(source, ["contactName"]),
    contactPhone: readString(source, ["contactPhone"]),
    amount: readNumber(source, ["amount"]),
    allowShare: readBoolean(source, ["allowShare"]),
    status: readString(source, ["status"]) || "PLANNED",
    remark: readString(source, ["remark"])
  };
}

function normalizeRouteEvent(value: unknown): TrafficRouteEvent {
  const source = isRecord(value) ? value : {};
  return {
    eventId: readNumber(source, ["eventId", "id"]) ?? 0,
    routePlanId: readNumber(source, ["routePlanId"]) ?? 0,
    trafficServiceOrderId: readNumber(source, ["trafficServiceOrderId"]),
    eventType: readString(source, ["eventType"]),
    eventMessage: readString(source, ["eventMessage"]),
    createdAt: readString(source, ["createdAt"])
  };
}

function normalizeRouteDetail(value: unknown): TrafficRouteDetail {
  const source = isRecord(value) ? value : {};
  return {
    route: source.route ? normalizeRoute(source.route) : null,
    stops: Array.isArray(source.stops) ? source.stops.map(normalizeRouteStop) : [],
    events: Array.isArray(source.events) ? source.events.map(normalizeRouteEvent) : []
  };
}

export async function listTrafficAnchorages(seaArea = ""): Promise<TrafficAnchorage[]> {
  const query = seaArea ? `?seaArea=${encodeURIComponent(seaArea)}` : "";
  const payload = await requestTrafficJson(`${ANCHORAGE_ENDPOINT}${query}`);
  return Array.isArray(payload) ? payload.map(normalizeAnchorage) : [];
}

export async function listTrafficServiceOrders(params: {
  keyword?: string;
  status?: string;
  seaArea?: string;
  purchaseOrderId?: number | string;
  page?: number;
  size?: number;
} = {}): Promise<TrafficServiceOrderListResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${TRAFFIC_SERVICE_ENDPOINT}?${query.toString()}`);
  const source = isRecord(payload) ? payload : {};
  return {
    items: Array.isArray(source.items) ? source.items.map(normalizeOrder) : [],
    total: readNumber(source, ["total"]) ?? 0,
    page: readNumber(source, ["page"]) ?? params.page ?? 1,
    size: readNumber(source, ["size"]) ?? params.size ?? 20
  };
}

export async function listTrafficServiceRequests(params: {
  keyword?: string;
  status?: string;
  seaArea?: string;
  anchorageCode?: string;
  page?: number;
  size?: number;
} = {}): Promise<TrafficServiceRequestListResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${TRAFFIC_SERVICE_REQUEST_ENDPOINT}?${query.toString()}`);
  const source = isRecord(payload) ? payload : {};
  return {
    items: Array.isArray(source.items) ? source.items.map(normalizeRequest) : [],
    total: readNumber(source, ["total"]) ?? 0,
    page: readNumber(source, ["page"]) ?? params.page ?? 1,
    size: readNumber(source, ["size"]) ?? params.size ?? 20
  };
}

export async function listSupplierTrafficServiceRequests(params: {
  keyword?: string;
  status?: string;
  seaArea?: string;
  anchorageCode?: string;
  page?: number;
  size?: number;
} = {}): Promise<TrafficServiceRequestListResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${SUPPLIER_TRAFFIC_SERVICE_REQUEST_ENDPOINT}?${query.toString()}`);
  const source = isRecord(payload) ? payload : {};
  return {
    items: Array.isArray(source.items) ? source.items.map(normalizeRequest) : [],
    total: readNumber(source, ["total"]) ?? 0,
    page: readNumber(source, ["page"]) ?? params.page ?? 1,
    size: readNumber(source, ["size"]) ?? params.size ?? 20
  };
}

export async function getTrafficServiceRequest(requestId: number | string): Promise<TrafficServiceRequestDetail> {
  return normalizeRequestDetail(await requestTrafficJson(`${TRAFFIC_SERVICE_REQUEST_ENDPOINT}/${encodeURIComponent(String(requestId))}`));
}

export async function createTrafficServiceRequest(payload: TrafficServiceRequestPayload): Promise<TrafficServiceRequest> {
  return normalizeRequest(await requestTrafficJson(TRAFFIC_SERVICE_REQUEST_ENDPOINT, { method: "POST", body: JSON.stringify(payload) }));
}

export async function publishTrafficServiceRequest(requestId: number | string): Promise<TrafficServiceRequest> {
  return normalizeRequest(await requestTrafficJson(`${TRAFFIC_SERVICE_REQUEST_ENDPOINT}/${encodeURIComponent(String(requestId))}/publish`, { method: "POST" }));
}

export async function cancelTrafficServiceRequest(requestId: number | string): Promise<TrafficServiceRequest> {
  return normalizeRequest(await requestTrafficJson(`${TRAFFIC_SERVICE_REQUEST_ENDPOINT}/${encodeURIComponent(String(requestId))}/cancel`, { method: "POST" }));
}

export async function selectTrafficServiceQuote(requestId: number | string, quoteId: number | string): Promise<TrafficServiceRequestDetail> {
  return normalizeRequestDetail(await requestTrafficJson(`${TRAFFIC_SERVICE_REQUEST_ENDPOINT}/${encodeURIComponent(String(requestId))}/select-quote`, { method: "POST", body: JSON.stringify({ quoteId }) }));
}

export async function submitTrafficServiceQuote(requestId: number | string, payload: TrafficServiceQuotePayload): Promise<TrafficServiceQuote> {
  return normalizeQuote(await requestTrafficJson(`${SUPPLIER_TRAFFIC_SERVICE_REQUEST_ENDPOINT}/${encodeURIComponent(String(requestId))}/quotes`, { method: "POST", body: JSON.stringify(payload) }));
}

export async function withdrawTrafficServiceQuote(quoteId: number | string): Promise<TrafficServiceQuote> {
  return normalizeQuote(await requestTrafficJson(`${SUPPLIER_TRAFFIC_SERVICE_QUOTE_ENDPOINT}/${encodeURIComponent(String(quoteId))}/withdraw`, { method: "POST" }));
}

export async function createTrafficServiceOrder(payload: TrafficServiceOrderPayload): Promise<TrafficServiceOrder> {
  return normalizeOrder(await requestTrafficJson(TRAFFIC_SERVICE_ENDPOINT, { method: "POST", body: JSON.stringify(payload) }));
}

export async function updateTrafficServiceOrder(orderId: number | string, payload: TrafficServiceOrderPayload): Promise<TrafficServiceOrder> {
  return normalizeOrder(await requestTrafficJson(`${TRAFFIC_SERVICE_ENDPOINT}/${encodeURIComponent(String(orderId))}`, { method: "PUT", body: JSON.stringify(payload) }));
}

export async function discardTrafficServiceOrder(orderId: number | string): Promise<TrafficServiceOrder> {
  return normalizeOrder(await requestTrafficJson(`${TRAFFIC_SERVICE_ENDPOINT}/${encodeURIComponent(String(orderId))}/discard`, { method: "POST" }));
}

export async function confirmTrafficServiceOrder(orderId: number | string): Promise<TrafficServiceOrder> {
  return normalizeOrder(await requestTrafficJson(`${TRAFFIC_SERVICE_ENDPOINT}/${encodeURIComponent(String(orderId))}/confirm`, { method: "POST" }));
}

export async function getTrafficLowestPrice(anchorageCode: string, allowShare = false): Promise<TrafficLowestPrice> {
  const query = new URLSearchParams({ allowShare: String(allowShare) });
  return normalizeLowestPrice(await requestTrafficJson(`${ANCHORAGE_ENDPOINT}/${encodeURIComponent(anchorageCode)}/lowest-price?${query.toString()}`));
}

export async function listTrafficBoatPrices(params: { keyword?: string; seaArea?: string } = {}): Promise<TrafficBoatPrice[]> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${TRAFFIC_BOAT_PRICE_ENDPOINT}?${query.toString()}`);
  return Array.isArray(payload) ? payload.map(normalizePrice) : [];
}

export async function createTrafficBoatPrice(payload: TrafficBoatPricePayload): Promise<TrafficBoatPrice> {
  return normalizePrice(await requestTrafficJson(TRAFFIC_BOAT_PRICE_ENDPOINT, { method: "POST", body: JSON.stringify(payload) }));
}

export async function updateTrafficBoatPrice(priceId: number | string, payload: TrafficBoatPricePayload): Promise<TrafficBoatPrice> {
  return normalizePrice(await requestTrafficJson(`${TRAFFIC_BOAT_PRICE_ENDPOINT}/${encodeURIComponent(String(priceId))}`, { method: "PUT", body: JSON.stringify(payload) }));
}

export async function updateTrafficBoatPrices(items: TrafficBoatPricePayload[]): Promise<TrafficBoatPrice[]> {
  const payload = await requestTrafficJson(TRAFFIC_BOAT_PRICE_ENDPOINT, { method: "PUT", body: JSON.stringify({ items }) });
  return Array.isArray(payload) ? payload.map(normalizePrice) : [];
}

export async function listTrafficShuttles(params: {
  keyword?: string;
  status?: string;
  seaArea?: string;
  anchorageCode?: string;
  startTimeFrom?: string;
  startTimeTo?: string;
  page?: number;
  size?: number;
} = {}): Promise<TrafficShuttleListResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${TRAFFIC_SHUTTLE_ENDPOINT}?${query.toString()}`);
  const source = isRecord(payload) ? payload : {};
  return {
    items: Array.isArray(source.items) ? source.items.map(normalizeShuttle) : [],
    total: readNumber(source, ["total"]) ?? 0,
    page: readNumber(source, ["page"]) ?? params.page ?? 1,
    size: readNumber(source, ["size"]) ?? params.size ?? 20
  };
}

export async function listSupplierTrafficShuttles(params: {
  keyword?: string;
  status?: string;
  seaArea?: string;
  anchorageCode?: string;
  startTimeFrom?: string;
  startTimeTo?: string;
  page?: number;
  size?: number;
} = {}): Promise<TrafficShuttleListResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT}?${query.toString()}`);
  const source = isRecord(payload) ? payload : {};
  return {
    items: Array.isArray(source.items) ? source.items.map(normalizeShuttle) : [],
    total: readNumber(source, ["total"]) ?? 0,
    page: readNumber(source, ["page"]) ?? params.page ?? 1,
    size: readNumber(source, ["size"]) ?? params.size ?? 20
  };
}

export async function createTrafficShuttle(payload: TrafficShuttlePayload): Promise<TrafficShuttleService> {
  return normalizeShuttle(await requestTrafficJson(SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT, { method: "POST", body: JSON.stringify(payload) }));
}

export async function updateTrafficShuttle(shuttleId: number | string, payload: TrafficShuttlePayload): Promise<TrafficShuttleService> {
  return normalizeShuttle(await requestTrafficJson(`${SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT}/${encodeURIComponent(String(shuttleId))}`, { method: "PUT", body: JSON.stringify(payload) }));
}

export async function closeTrafficShuttle(shuttleId: number | string): Promise<TrafficShuttleService> {
  return normalizeShuttle(await requestTrafficJson(`${SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT}/${encodeURIComponent(String(shuttleId))}/close`, { method: "POST" }));
}

export async function startTrafficShuttle(shuttleId: number | string): Promise<TrafficShuttleService> {
  return normalizeShuttle(await requestTrafficJson(`${SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT}/${encodeURIComponent(String(shuttleId))}/start`, { method: "POST" }));
}

export async function completeTrafficShuttle(shuttleId: number | string): Promise<TrafficShuttleService> {
  return normalizeShuttle(await requestTrafficJson(`${SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT}/${encodeURIComponent(String(shuttleId))}/complete`, { method: "POST" }));
}

export async function updateTrafficShuttleBookingExecution(
  shuttleId: number | string,
  bookingId: number | string,
  payload: TrafficShuttleExecutionPayload
): Promise<TrafficShuttleBooking> {
  return normalizeBooking(await requestTrafficJson(
    `${SUPPLIER_TRAFFIC_SHUTTLE_ENDPOINT}/${encodeURIComponent(String(shuttleId))}/bookings/${encodeURIComponent(String(bookingId))}/execution`,
    { method: "PUT", body: JSON.stringify(payload) }
  ));
}

export async function bookTrafficShuttle(shuttleId: number | string, payload: TrafficShuttleBookingPayload): Promise<TrafficShuttleBooking> {
  return normalizeBooking(await requestTrafficJson(`${TRAFFIC_SHUTTLE_ENDPOINT}/${encodeURIComponent(String(shuttleId))}/book`, { method: "POST", body: JSON.stringify(payload) }));
}

export async function listTrafficRoutes(params: {
  keyword?: string;
  status?: string;
  seaArea?: string;
  serviceDate?: string;
  page?: number;
  size?: number;
} = {}): Promise<TrafficRoutePlanListResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) query.set(key, String(value));
  });
  const payload = await requestTrafficJson(`${TRAFFIC_ROUTE_ENDPOINT}?${query.toString()}`);
  const source = isRecord(payload) ? payload : {};
  return {
    items: Array.isArray(source.items) ? source.items.map(normalizeRoute) : [],
    total: readNumber(source, ["total"]) ?? 0,
    page: readNumber(source, ["page"]) ?? params.page ?? 1,
    size: readNumber(source, ["size"]) ?? params.size ?? 20
  };
}

export async function getTrafficRoute(routeId: number | string): Promise<TrafficRouteDetail> {
  return normalizeRouteDetail(await requestTrafficJson(`${TRAFFIC_ROUTE_ENDPOINT}/${encodeURIComponent(String(routeId))}`));
}

export async function createTrafficRoute(payload: TrafficRoutePlanPayload): Promise<TrafficRouteDetail> {
  return normalizeRouteDetail(await requestTrafficJson(TRAFFIC_ROUTE_ENDPOINT, { method: "POST", body: JSON.stringify(payload) }));
}

export async function addTrafficRouteStop(routeId: number | string, payload: TrafficRouteStopPayload): Promise<TrafficRouteDetail> {
  return normalizeRouteDetail(await requestTrafficJson(`${TRAFFIC_ROUTE_ENDPOINT}/${encodeURIComponent(String(routeId))}/stops`, { method: "POST", body: JSON.stringify(payload) }));
}

export async function reorderTrafficRouteStops(routeId: number | string, stops: Array<{ routeStopId: number; stopSequence: number }>): Promise<TrafficRouteDetail> {
  return normalizeRouteDetail(await requestTrafficJson(`${TRAFFIC_ROUTE_ENDPOINT}/${encodeURIComponent(String(routeId))}/stops/reorder`, { method: "PUT", body: JSON.stringify({ stops }) }));
}

export async function removeTrafficRouteStop(routeId: number | string, stopId: number | string): Promise<TrafficRouteDetail> {
  return normalizeRouteDetail(await requestTrafficJson(`${TRAFFIC_ROUTE_ENDPOINT}/${encodeURIComponent(String(routeId))}/stops/${encodeURIComponent(String(stopId))}`, { method: "DELETE" }));
}

export async function updateTrafficRouteStatus(routeId: number | string, action: "confirm" | "start" | "complete" | "discard"): Promise<TrafficRouteDetail> {
  return normalizeRouteDetail(await requestTrafficJson(`${TRAFFIC_ROUTE_ENDPOINT}/${encodeURIComponent(String(routeId))}/${action}`, { method: "POST" }));
}
