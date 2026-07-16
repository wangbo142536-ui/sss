package com.zswy.shipsupply.transport;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrafficServiceController {

    private final TrafficServiceService trafficServiceService;

    public TrafficServiceController(TrafficServiceService trafficServiceService) {
        this.trafficServiceService = trafficServiceService;
    }

    @GetMapping("/api/traffic/anchorages")
    public List<TrafficAnchorageResponse> anchorages(@RequestParam(value = "seaArea", required = false) String seaArea) {
        return trafficServiceService.listAnchorages(seaArea);
    }

    @GetMapping("/api/traffic/service-requests")
    public TrafficServiceRequestListResponse listServiceRequests(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "seaArea", required = false) String seaArea,
        @RequestParam(value = "anchorageCode", required = false) String anchorageCode,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return trafficServiceService.listRequests(authorizationHeader, keyword, status, seaArea, anchorageCode, page, size);
    }

    @GetMapping("/api/traffic/service-requests/{requestId}")
    public TrafficServiceRequestDetailResponse getServiceRequest(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long requestId
    ) {
        return trafficServiceService.getRequestDetail(authorizationHeader, requestId);
    }

    @PostMapping("/api/traffic/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public TrafficServiceRequestResponse createServiceRequest(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TrafficServiceRequestPayload payload
    ) {
        return trafficServiceService.createRequest(authorizationHeader, payload);
    }

    @PostMapping("/api/traffic/service-requests/{requestId}/publish")
    public TrafficServiceRequestResponse publishServiceRequest(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long requestId
    ) {
        return trafficServiceService.publishRequest(authorizationHeader, requestId);
    }

    @PostMapping("/api/traffic/service-requests/{requestId}/cancel")
    public TrafficServiceRequestResponse cancelServiceRequest(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long requestId
    ) {
        return trafficServiceService.cancelRequest(authorizationHeader, requestId);
    }

    @PostMapping("/api/traffic/service-requests/{requestId}/select-quote")
    public TrafficServiceRequestDetailResponse selectServiceRequestQuote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long requestId,
        @RequestBody TrafficServiceQuoteSelectPayload payload
    ) {
        return trafficServiceService.selectQuote(authorizationHeader, requestId, payload);
    }

    @GetMapping("/api/supplier/traffic-service-requests")
    public TrafficServiceRequestListResponse listSupplierServiceRequests(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "seaArea", required = false) String seaArea,
        @RequestParam(value = "anchorageCode", required = false) String anchorageCode,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return trafficServiceService.listSupplierRequests(authorizationHeader, keyword, status, seaArea, anchorageCode, page, size);
    }

    @PostMapping("/api/supplier/traffic-service-requests/{requestId}/quotes")
    public TrafficServiceQuoteResponse submitSupplierServiceQuote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long requestId,
        @RequestBody TrafficServiceQuotePayload payload
    ) {
        return trafficServiceService.submitQuote(authorizationHeader, requestId, payload);
    }

    @PostMapping("/api/supplier/traffic-service-quotes/{quoteId}/withdraw")
    public TrafficServiceQuoteResponse withdrawSupplierServiceQuote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long quoteId
    ) {
        return trafficServiceService.withdrawQuote(authorizationHeader, quoteId);
    }

    @GetMapping("/api/traffic/shuttles")
    public TrafficShuttleListResponse listShuttles(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "seaArea", required = false) String seaArea,
        @RequestParam(value = "anchorageCode", required = false) String anchorageCode,
        @RequestParam(value = "startTimeFrom", required = false) String startTimeFrom,
        @RequestParam(value = "startTimeTo", required = false) String startTimeTo,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return trafficServiceService.listShuttles(authorizationHeader, false, keyword, status, seaArea, anchorageCode, startTimeFrom, startTimeTo, page, size);
    }

    @GetMapping("/api/traffic/shuttles/{shuttleId}")
    public TrafficShuttleServiceResponse getShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId
    ) {
        return trafficServiceService.getShuttle(authorizationHeader, shuttleId);
    }

    @PostMapping("/api/traffic/shuttles/{shuttleId}/book")
    public TrafficShuttleBookingResponse bookShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId,
        @RequestBody TrafficShuttleBookingPayload payload
    ) {
        return trafficServiceService.bookShuttle(authorizationHeader, shuttleId, payload);
    }

    @GetMapping("/api/supplier/traffic-shuttles")
    public TrafficShuttleListResponse listSupplierShuttles(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "seaArea", required = false) String seaArea,
        @RequestParam(value = "anchorageCode", required = false) String anchorageCode,
        @RequestParam(value = "startTimeFrom", required = false) String startTimeFrom,
        @RequestParam(value = "startTimeTo", required = false) String startTimeTo,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return trafficServiceService.listShuttles(authorizationHeader, true, keyword, status, seaArea, anchorageCode, startTimeFrom, startTimeTo, page, size);
    }

    @PostMapping("/api/supplier/traffic-shuttles")
    @ResponseStatus(HttpStatus.CREATED)
    public TrafficShuttleServiceResponse createSupplierShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TrafficShuttlePayload payload
    ) {
        return trafficServiceService.createShuttle(authorizationHeader, payload);
    }

    @PutMapping("/api/supplier/traffic-shuttles/{shuttleId}")
    public TrafficShuttleServiceResponse updateSupplierShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId,
        @RequestBody TrafficShuttlePayload payload
    ) {
        return trafficServiceService.updateShuttle(authorizationHeader, shuttleId, payload);
    }

    @PostMapping("/api/supplier/traffic-shuttles/{shuttleId}/close")
    public TrafficShuttleServiceResponse closeSupplierShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId
    ) {
        return trafficServiceService.changeShuttleStatus(authorizationHeader, shuttleId, "CLOSED");
    }

    @PutMapping("/api/supplier/traffic-shuttles/{shuttleId}/bookings/{bookingId}/execution")
    public TrafficShuttleBookingResponse updateSupplierBookingExecution(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId,
        @PathVariable Long bookingId,
        @RequestBody TrafficShuttleBookingExecutionPayload payload
    ) {
        return trafficServiceService.updateBookingExecution(authorizationHeader, shuttleId, bookingId, payload);
    }

    @GetMapping("/api/traffic/services")
    public TrafficServiceOrderListResponse listOrders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "seaArea", required = false) String seaArea,
        @RequestParam(value = "purchaseOrderId", required = false) Long purchaseOrderId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return trafficServiceService.listOrders(authorizationHeader, keyword, status, seaArea, purchaseOrderId, page, size);
    }

    @GetMapping("/api/traffic/anchorages/{anchorageCode}/lowest-price")
    public TrafficLowestPriceResponse lowestPrice(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String anchorageCode,
        @RequestParam(value = "allowShare", defaultValue = "false") boolean allowShare
    ) {
        return trafficServiceService.lowestPrice(authorizationHeader, anchorageCode, allowShare);
    }

    @GetMapping("/api/traffic/services/{orderId}")
    public TrafficServiceOrderResponse getOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId
    ) {
        return trafficServiceService.getOrder(authorizationHeader, orderId);
    }

    @PostMapping("/api/traffic/services")
    @ResponseStatus(HttpStatus.CREATED)
    public TrafficServiceOrderResponse createOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TrafficServiceOrderPayload payload
    ) {
        return trafficServiceService.createOrder(authorizationHeader, payload);
    }

    @PutMapping("/api/traffic/services/{orderId}")
    public TrafficServiceOrderResponse updateOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId,
        @RequestBody TrafficServiceOrderPayload payload
    ) {
        return trafficServiceService.updateOrder(authorizationHeader, orderId, payload);
    }

    @PostMapping("/api/traffic/services/{orderId}/discard")
    public TrafficServiceOrderResponse discardOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId
    ) {
        return trafficServiceService.discardOrder(authorizationHeader, orderId);
    }

    @PostMapping("/api/traffic/services/{orderId}/confirm")
    public TrafficServiceOrderResponse confirmOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId
    ) {
        return trafficServiceService.confirmOrder(authorizationHeader, orderId);
    }

    @GetMapping("/api/traffic/routes")
    public TrafficRoutePlanListResponse listRoutes(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "seaArea", required = false) String seaArea,
        @RequestParam(value = "serviceDate", required = false) String serviceDate,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return trafficServiceService.listRoutes(authorizationHeader, keyword, status, seaArea, serviceDate, page, size);
    }

    @GetMapping("/api/traffic/routes/{routeId}")
    public TrafficRouteDetailResponse getRoute(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId
    ) {
        return trafficServiceService.getRoute(authorizationHeader, routeId);
    }

    @PostMapping("/api/traffic/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public TrafficRouteDetailResponse createRoute(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TrafficRoutePlanPayload payload
    ) {
        return trafficServiceService.createRoute(authorizationHeader, payload);
    }

    @PostMapping("/api/traffic/routes/{routeId}/stops")
    public TrafficRouteDetailResponse addRouteStop(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId,
        @RequestBody TrafficRouteStopPayload payload
    ) {
        return trafficServiceService.addRouteStop(authorizationHeader, routeId, payload);
    }

    @PutMapping("/api/traffic/routes/{routeId}/stops/reorder")
    public TrafficRouteDetailResponse reorderRouteStops(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId,
        @RequestBody TrafficRouteStopReorderPayload payload
    ) {
        return trafficServiceService.reorderRouteStops(authorizationHeader, routeId, payload);
    }

    @DeleteMapping("/api/traffic/routes/{routeId}/stops/{stopId}")
    public TrafficRouteDetailResponse removeRouteStop(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId,
        @PathVariable Long stopId
    ) {
        return trafficServiceService.removeRouteStop(authorizationHeader, routeId, stopId);
    }

    @PostMapping("/api/traffic/routes/{routeId}/confirm")
    public TrafficRouteDetailResponse confirmRoute(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId
    ) {
        return trafficServiceService.changeRouteStatus(authorizationHeader, routeId, "READY");
    }

    @PostMapping("/api/traffic/routes/{routeId}/start")
    public TrafficRouteDetailResponse startRoute(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId
    ) {
        return trafficServiceService.changeRouteStatus(authorizationHeader, routeId, "IN_PROGRESS");
    }

    @PostMapping("/api/traffic/routes/{routeId}/complete")
    public TrafficRouteDetailResponse completeRoute(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId
    ) {
        return trafficServiceService.changeRouteStatus(authorizationHeader, routeId, "COMPLETED");
    }

    @PostMapping("/api/traffic/routes/{routeId}/discard")
    public TrafficRouteDetailResponse discardRoute(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long routeId
    ) {
        return trafficServiceService.changeRouteStatus(authorizationHeader, routeId, "DISCARDED");
    }

    @GetMapping("/api/supplier/traffic-boat/prices")
    public List<TrafficBoatPriceResponse> listPrices(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "seaArea", required = false) String seaArea
    ) {
        return trafficServiceService.listPrices(authorizationHeader, keyword, seaArea);
    }

    @PostMapping("/api/supplier/traffic-boat/prices")
    public TrafficBoatPriceResponse createPrice(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TrafficBoatPricePayload payload
    ) {
        return trafficServiceService.savePrice(authorizationHeader, payload);
    }

    @PutMapping("/api/supplier/traffic-boat/prices/{priceId}")
    public TrafficBoatPriceResponse updatePrice(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long priceId,
        @RequestBody TrafficBoatPricePayload payload
    ) {
        return trafficServiceService.savePrice(authorizationHeader, payload);
    }

    @PutMapping("/api/supplier/traffic-boat/prices")
    public List<TrafficBoatPriceResponse> updatePrices(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TrafficBoatPriceBatchPayload payload
    ) {
        return trafficServiceService.savePrices(authorizationHeader, payload);
    }
}
