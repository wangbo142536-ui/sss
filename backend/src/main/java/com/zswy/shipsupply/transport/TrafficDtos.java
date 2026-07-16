package com.zswy.shipsupply.transport;

import java.math.BigDecimal;
import java.util.List;

record TrafficAnchorageResponse(
    Long anchorageId,
    String anchorageCode,
    String anchorageName,
    String seaArea,
    Boolean enabled,
    Integer sortOrder
) {
}

record TrafficServiceOrderResponse(
    Long serviceOrderId,
    String serviceNo,
    Long purchaseOrderId,
    Long supplierCompanyId,
    String supplierCompanyName,
    String feeType,
    String seaArea,
    String anchorageCode,
    String anchorageName,
    String useTime,
    String serviceType,
    String passengerType,
    Integer passengerCount,
    String cargoType,
    Boolean returnTrip,
    Boolean allowShare,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    String status,
    String remark,
    Long businessContactId,
    String businessContactName,
    String businessContactPhone,
    String acceptedAt,
    Long trafficVesselId,
    String trafficVesselName,
    Long handlerContactId,
    String handlerName,
    String handlerPhone,
    String supplierMessage,
    String departureTime,
    String arrivalTime,
    String returnStartTime,
    String returnEndTime,
    String signPhotoUrl,
    String pickupPhotoUrl,
    String returnArrivalPhotoUrl,
    String createdAt,
    String updatedAt,
    List<TrafficServiceCargoResponse> cargos,
    Long bookingId,
    Long shuttleServiceId,
    String shuttleNo,
    String shuttleDeparturePoint,
    String shuttleDestinationPoint,
    String shuttleStartTime,
    String shuttleReturnTime,
    List<TrafficShuttleNodePayload> shuttleServiceNodes,
    Integer bookingNodeIndex,
    String bookingNodeName,
    String bookingNodeTime,
    BigDecimal bookingAmount,
    BigDecimal bookingFreightFee,
    BigDecimal bookingCustomsFee,
    BigDecimal bookingCraneFee
) {
}

record TrafficLowestPriceResponse(
    Long supplierCompanyId,
    String supplierCompanyName,
    String anchorageCode,
    String anchorageName,
    Boolean allowShare,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    BigDecimal amount
) {
}

record TrafficServiceCargoResponse(
    Long cargoId,
    String cargoName,
    BigDecimal weightKg,
    BigDecimal volumeCbm
) {
}

record TrafficServiceOrderListResponse(
    List<TrafficServiceOrderResponse> items,
    long total,
    int page,
    int size
) {
}

record TrafficServiceRequestResponse(
    Long requestId,
    String requestNo,
    Long requesterCompanyId,
    Long demandId,
    Long purchaseOrderId,
    String feeType,
    String seaArea,
    String anchorageCode,
    String anchorageName,
    String useTime,
    String serviceType,
    String passengerType,
    Integer passengerCount,
    String cargoType,
    Boolean returnTrip,
    Boolean allowShare,
    String remark,
    String status,
    Long recommendedQuoteId,
    Long selectedQuoteId,
    Long selectedSupplierCompanyId,
    Long trafficServiceOrderId,
    String createdAt,
    List<TrafficServiceCargoResponse> cargos
) {
}

record TrafficServiceQuoteResponse(
    Long quoteId,
    Long requestId,
    Long supplierCompanyId,
    String supplierCompanyName,
    BigDecimal quoteAmount,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    String currency,
    String availableStartTime,
    String availableReturnTime,
    Long trafficVesselId,
    String trafficVesselName,
    String contactName,
    String contactPhone,
    String message,
    String status,
    Boolean recommended,
    String createdAt,
    String updatedAt
) {
}

record TrafficServiceRequestEventResponse(
    Long eventId,
    Long requestId,
    Long quoteId,
    String eventType,
    String eventMessage,
    String createdAt
) {
}

record TrafficServiceRequestDetailResponse(
    TrafficServiceRequestResponse request,
    List<TrafficServiceQuoteResponse> quotes,
    List<TrafficServiceRequestEventResponse> events
) {
}

record TrafficServiceRequestListResponse(
    List<TrafficServiceRequestResponse> items,
    long total,
    int page,
    int size
) {
}

record TrafficServiceRequestPayload(
    Long demandId,
    Long purchaseOrderId,
    String feeType,
    String seaArea,
    String anchorageCode,
    String useTime,
    String serviceType,
    String passengerType,
    Integer passengerCount,
    String cargoType,
    Boolean returnTrip,
    Boolean allowShare,
    String remark,
    Boolean publish,
    List<TrafficServiceCargoPayload> cargos
) {
}

record TrafficServiceQuotePayload(
    BigDecimal quoteAmount,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    String currency,
    String availableStartTime,
    String availableReturnTime,
    Long trafficVesselId,
    String trafficVesselName,
    String contactName,
    String contactPhone,
    String message
) {
}

record TrafficServiceQuoteSelectPayload(
    Long quoteId
) {
}

record TrafficShuttleServiceResponse(
    Long shuttleId,
    String shuttleNo,
    Long supplierCompanyId,
    String supplierCompanyName,
    String seaArea,
    String anchorageCode,
    String anchorageName,
    String departurePoint,
    String destinationPoint,
    String startTime,
    String returnTime,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    BigDecimal customsPrice,
    BigDecimal cranePrice,
    Integer passengerCapacity,
    BigDecimal cargoCapacityKg,
    BigDecimal cargoCapacityCbm,
    Integer bookedPassengerCount,
    BigDecimal bookedCargoWeightKg,
    BigDecimal bookedCargoVolumeCbm,
    Long trafficVesselId,
    String trafficVesselName,
    String status,
    String remark,
    List<TrafficShuttleNodePayload> serviceNodes,
    List<TrafficShuttleBookingResponse> bookings,
    String createdAt
) {
}

record TrafficShuttleListResponse(
    List<TrafficShuttleServiceResponse> items,
    long total,
    int page,
    int size
) {
}

record TrafficShuttlePayload(
    String seaArea,
    String anchorageCode,
    String departurePoint,
    String destinationPoint,
    String startTime,
    String returnTime,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    BigDecimal customsPrice,
    BigDecimal cranePrice,
    Integer passengerCapacity,
    BigDecimal cargoCapacityKg,
    BigDecimal cargoCapacityCbm,
    Long trafficVesselId,
    String trafficVesselName,
    String status,
    String remark,
    List<TrafficShuttleNodePayload> serviceNodes
) {
}

record TrafficShuttleNodePayload(
    String nodeName,
    String startTime,
    String endTime
) {
}

record TrafficShuttleBookingResponse(
    Long bookingId,
    String bookingNo,
    Long shuttleServiceId,
    Long requesterCompanyId,
    Long requestId,
    String requestNo,
    Integer nodeIndex,
    String nodeName,
    String nodeTime,
    String vesselName,
    String vesselImo,
    String anchorageTime,
    String anchoragePosition,
    BigDecimal longitude,
    BigDecimal latitude,
    String palletCount,
    Long trafficServiceOrderId,
    Integer passengerCount,
    String cargoSummary,
    BigDecimal cargoWeightKg,
    BigDecimal cargoVolumeCbm,
    BigDecimal amount,
    Boolean allowShare,
    Boolean customsService,
    Boolean craneService,
    Integer craneCount,
    BigDecimal freightFee,
    BigDecimal customsFee,
    BigDecimal craneFee,
    String contactName,
    String contactPhone,
    String remark,
    String status,
    String createdAt
) {
}

record TrafficShuttleBookingExecutionPayload(
    Integer nodeIndex,
    String nodeName,
    String nodeTime,
    String vesselName,
    String vesselImo,
    String anchorageTime,
    String anchoragePosition,
    BigDecimal longitude,
    BigDecimal latitude,
    String palletCount,
    Integer passengerCount,
    String cargoSummary,
    BigDecimal cargoWeightKg,
    BigDecimal cargoVolumeCbm,
    Boolean allowShare,
    Boolean customsService,
    Boolean craneService,
    Integer craneCount,
    String contactName,
    String contactPhone,
    String remark
) {
}

record TrafficShuttleBookingPayload(
    Long bookingId,
    String requestNo,
    Long demandId,
    Long purchaseOrderId,
    Integer nodeIndex,
    String nodeName,
    String nodeTime,
    String vesselName,
    String vesselImo,
    String anchorageTime,
    String anchoragePosition,
    String palletCount,
    Boolean allowShare,
    Boolean customsService,
    Boolean craneService,
    Integer craneCount,
    Integer passengerCount,
    String cargoSummary,
    BigDecimal cargoWeightKg,
    BigDecimal cargoVolumeCbm,
    String contactName,
    String contactPhone,
    String remark
) {
}

record TrafficServiceCargoPayload(
    String cargoName,
    BigDecimal weightKg,
    BigDecimal volumeCbm
) {
}

record TrafficServiceOrderPayload(
    String seaArea,
    String anchorageCode,
    String feeType,
    String useTime,
    String serviceType,
    String passengerType,
    Integer passengerCount,
    String cargoType,
    Boolean returnTrip,
    Boolean allowShare,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    String remark,
    Long businessContactId,
    String businessContactName,
    String businessContactPhone,
    String acceptedAt,
    Long trafficVesselId,
    String trafficVesselName,
    Long handlerContactId,
    String handlerName,
    String handlerPhone,
    String supplierMessage,
    String departureTime,
    String arrivalTime,
    String returnStartTime,
    String returnEndTime,
    String signPhotoUrl,
    String pickupPhotoUrl,
    String returnArrivalPhotoUrl,
    List<TrafficServiceCargoPayload> cargos
) {
}

record TrafficBoatPriceResponse(
    Long priceId,
    Long supplierCompanyId,
    String anchorageCode,
    String anchorageName,
    String seaArea,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    Boolean enabled,
    String remark,
    String updatedAt
) {
}

record TrafficBoatPricePayload(
    String anchorageCode,
    BigDecimal basePrice,
    BigDecimal sharedPrice,
    Boolean enabled,
    String remark
) {
}

record TrafficBoatPriceBatchPayload(
    List<TrafficBoatPricePayload> items
) {
}

record TrafficRoutePlanResponse(
    Long routePlanId,
    String routeNo,
    String routeName,
    String serviceDate,
    String seaArea,
    Long supplierCompanyId,
    String supplierCompanyName,
    Long trafficVesselId,
    String trafficVesselName,
    String plannedDepartureTime,
    String plannedFinishTime,
    Boolean allowShare,
    Integer orderCount,
    BigDecimal totalIncome,
    BigDecimal estimatedCost,
    BigDecimal estimatedProfit,
    String status,
    String remark,
    String createdAt
) {
}

record TrafficRoutePlanListResponse(
    List<TrafficRoutePlanResponse> items,
    long total,
    int page,
    int size
) {
}

record TrafficRouteStopResponse(
    Long routeStopId,
    Long routePlanId,
    Long trafficServiceOrderId,
    Integer stopSequence,
    String serviceNo,
    String anchorageCode,
    String anchorageName,
    String plannedServiceTime,
    String serviceType,
    String contactName,
    String contactPhone,
    BigDecimal amount,
    Boolean allowShare,
    String status,
    String remark
) {
}

record TrafficRouteEventResponse(
    Long eventId,
    Long routePlanId,
    Long trafficServiceOrderId,
    String eventType,
    String eventMessage,
    String createdAt
) {
}

record TrafficRouteDetailResponse(
    TrafficRoutePlanResponse route,
    List<TrafficRouteStopResponse> stops,
    List<TrafficRouteEventResponse> events
) {
}

record TrafficRoutePlanPayload(
    String routeName,
    String serviceDate,
    String seaArea,
    Long supplierCompanyId,
    String supplierCompanyName,
    Long trafficVesselId,
    String trafficVesselName,
    String plannedDepartureTime,
    String plannedFinishTime,
    Boolean allowShare,
    BigDecimal estimatedCost,
    String remark
) {
}

record TrafficRouteStopPayload(
    Long trafficServiceOrderId,
    String plannedServiceTime,
    String remark
) {
}

record TrafficRouteStopOrderPayload(
    Long routeStopId,
    Integer stopSequence
) {
}

record TrafficRouteStopReorderPayload(
    List<TrafficRouteStopOrderPayload> stops
) {
}
