package com.zswy.shipsupply.transport;

import java.util.List;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrafficServiceService {

    private final CurrentUserService currentUserService;
    private final TrafficServiceRepository trafficServiceRepository;

    public TrafficServiceService(CurrentUserService currentUserService, TrafficServiceRepository trafficServiceRepository) {
        this.currentUserService = currentUserService;
        this.trafficServiceRepository = trafficServiceRepository;
    }

    public List<TrafficAnchorageResponse> listAnchorages(String seaArea) {
        return trafficServiceRepository.listAnchorages(seaArea);
    }

    public TrafficServiceRequestListResponse listRequests(
        String authorizationHeader,
        String keyword,
        String status,
        String seaArea,
        String anchorageCode,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.listRequests(currentUser.companyId(), keyword, status, seaArea, anchorageCode, page, size);
    }

    public TrafficServiceRequestListResponse listSupplierRequests(
        String authorizationHeader,
        String keyword,
        String status,
        String seaArea,
        String anchorageCode,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.listSupplierRequests(currentUser.companyId(), keyword, status, seaArea, anchorageCode, page, size);
    }

    public TrafficServiceRequestDetailResponse getRequestDetail(String authorizationHeader, Long requestId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.getRequestDetail(currentUser.companyId(), requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_REQUEST_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceRequestResponse createRequest(String authorizationHeader, TrafficServiceRequestPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateRequest(payload);
        return trafficServiceRepository.createRequest(currentUser.companyId(), currentUser.userId(), payload);
    }

    @Transactional
    public TrafficServiceRequestResponse publishRequest(String authorizationHeader, Long requestId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.publishRequest(currentUser.companyId(), currentUser.userId(), requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_REQUEST_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceRequestResponse cancelRequest(String authorizationHeader, Long requestId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.cancelRequest(currentUser.companyId(), currentUser.userId(), requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_REQUEST_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceQuoteResponse submitQuote(String authorizationHeader, Long requestId, TrafficServiceQuotePayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateQuote(payload);
        return trafficServiceRepository.submitQuote(currentUser.companyId(), currentUser.userId(), requestId, payload)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_REQUEST_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceQuoteResponse withdrawQuote(String authorizationHeader, Long quoteId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.withdrawQuote(currentUser.companyId(), currentUser.userId(), quoteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_QUOTE_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceRequestDetailResponse selectQuote(String authorizationHeader, Long requestId, TrafficServiceQuoteSelectPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (payload == null || payload.quoteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SERVICE_QUOTE_REQUIRED");
        }
        return trafficServiceRepository.selectQuote(currentUser.companyId(), currentUser.userId(), requestId, payload.quoteId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_QUOTE_NOT_FOUND"));
    }

    public TrafficShuttleListResponse listShuttles(
        String authorizationHeader,
        boolean supplierOnly,
        String keyword,
        String status,
        String seaArea,
        String anchorageCode,
        String startTimeFrom,
        String startTimeTo,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.listShuttles(currentUser.companyId(), supplierOnly, keyword, status, seaArea, anchorageCode, startTimeFrom, startTimeTo, page, size);
    }

    public TrafficShuttleServiceResponse getShuttle(String authorizationHeader, Long shuttleId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.getShuttle(currentUser.companyId(), shuttleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND"));
    }

    @Transactional
    public TrafficShuttleServiceResponse createShuttle(String authorizationHeader, TrafficShuttlePayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateShuttle(payload);
        return trafficServiceRepository.createShuttle(currentUser.companyId(), currentUser.userId(), payload);
    }

    @Transactional
    public TrafficShuttleServiceResponse updateShuttle(String authorizationHeader, Long shuttleId, TrafficShuttlePayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateShuttle(payload);
        return trafficServiceRepository.updateShuttle(currentUser.companyId(), shuttleId, payload)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND"));
    }

    @Transactional
    public TrafficShuttleServiceResponse changeShuttleStatus(String authorizationHeader, Long shuttleId, String status) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.changeShuttleStatus(currentUser.companyId(), shuttleId, status)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND"));
    }

    @Transactional
    public TrafficShuttleBookingResponse bookShuttle(String authorizationHeader, Long shuttleId, TrafficShuttleBookingPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SHUTTLE_BOOKING_REQUIRED");
        }
        return trafficServiceRepository.bookShuttle(currentUser.companyId(), currentUser.userId(), shuttleId, payload)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND"));
    }

    @Transactional
    public TrafficShuttleBookingResponse updateBookingExecution(
        String authorizationHeader,
        Long shuttleId,
        Long bookingId,
        TrafficShuttleBookingExecutionPayload payload
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SHUTTLE_BOOKING_EXECUTION_REQUIRED");
        }
        if (payload.cargoWeightKg() != null && payload.cargoWeightKg().signum() < 0
            || payload.cargoVolumeCbm() != null && payload.cargoVolumeCbm().signum() < 0
            || payload.craneCount() != null && payload.craneCount() < 0
            || payload.longitude() != null && payload.longitude().abs().compareTo(java.math.BigDecimal.valueOf(180)) > 0
            || payload.latitude() != null && payload.latitude().abs().compareTo(java.math.BigDecimal.valueOf(90)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TRAFFIC_SHUTTLE_EXECUTION_VALUE");
        }
        return trafficServiceRepository.updateBookingExecution(
            currentUser.companyId(), shuttleId, bookingId, payload
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_BOOKING_NOT_FOUND"));
    }

    public TrafficServiceOrderListResponse listOrders(
        String authorizationHeader,
        String keyword,
        String status,
        String seaArea,
        Long purchaseOrderId,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.listOrders(currentUser.companyId(), keyword, status, seaArea, purchaseOrderId, page, size);
    }

    public TrafficLowestPriceResponse lowestPrice(String authorizationHeader, String anchorageCode, boolean allowShare) {
        currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (isBlank(anchorageCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANCHORAGE_REQUIRED");
        }
        return trafficServiceRepository.lowestPrice(anchorageCode, allowShare)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_PRICE_NOT_FOUND"));
    }

    public TrafficServiceOrderResponse getOrder(String authorizationHeader, Long orderId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.getOrder(currentUser.companyId(), orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_ORDER_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceOrderResponse createOrder(String authorizationHeader, TrafficServiceOrderPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateOrder(payload);
        return trafficServiceRepository.createOrder(currentUser.companyId(), currentUser.userId(), payload);
    }

    @Transactional
    public TrafficServiceOrderResponse updateOrder(String authorizationHeader, Long orderId, TrafficServiceOrderPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateOrder(payload);
        return trafficServiceRepository.updateOrder(currentUser.companyId(), orderId, payload)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_ORDER_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceOrderResponse discardOrder(String authorizationHeader, Long orderId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.discardOrder(currentUser.companyId(), orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_ORDER_NOT_FOUND"));
    }

    @Transactional
    public TrafficServiceOrderResponse confirmOrder(String authorizationHeader, Long orderId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.confirmOrder(currentUser.companyId(), orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_ORDER_NOT_FOUND"));
    }

    public TrafficRoutePlanListResponse listRoutes(
        String authorizationHeader,
        String keyword,
        String status,
        String seaArea,
        String serviceDate,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.listRoutes(currentUser.companyId(), keyword, status, seaArea, serviceDate, page, size);
    }

    public TrafficRouteDetailResponse getRoute(String authorizationHeader, Long routeId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.getRoute(currentUser.companyId(), routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_ROUTE_NOT_FOUND"));
    }

    @Transactional
    public TrafficRouteDetailResponse createRoute(String authorizationHeader, TrafficRoutePlanPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateRoute(payload);
        return trafficServiceRepository.createRoute(currentUser.companyId(), currentUser.userId(), payload);
    }

    @Transactional
    public TrafficRouteDetailResponse addRouteStop(String authorizationHeader, Long routeId, TrafficRouteStopPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (payload == null || payload.trafficServiceOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SERVICE_ORDER_REQUIRED");
        }
        return trafficServiceRepository.addRouteStop(currentUser.companyId(), currentUser.userId(), routeId, payload)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_ROUTE_OR_ORDER_NOT_FOUND"));
    }

    @Transactional
    public TrafficRouteDetailResponse reorderRouteStops(String authorizationHeader, Long routeId, TrafficRouteStopReorderPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.reorderRouteStops(currentUser.companyId(), routeId, payload)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_ROUTE_NOT_FOUND"));
    }

    @Transactional
    public TrafficRouteDetailResponse removeRouteStop(String authorizationHeader, Long routeId, Long stopId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.removeRouteStop(currentUser.companyId(), routeId, stopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_ROUTE_STOP_NOT_FOUND"));
    }

    @Transactional
    public TrafficRouteDetailResponse changeRouteStatus(String authorizationHeader, Long routeId, String status) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.changeRouteStatus(currentUser.companyId(), currentUser.userId(), routeId, status)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_ROUTE_NOT_FOUND"));
    }

    public List<TrafficBoatPriceResponse> listPrices(String authorizationHeader, String keyword, String seaArea) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return trafficServiceRepository.listPrices(currentUser.companyId(), keyword, seaArea);
    }

    @Transactional
    public TrafficBoatPriceResponse savePrice(String authorizationHeader, TrafficBoatPricePayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validatePrice(payload);
        return trafficServiceRepository.savePrice(currentUser.companyId(), payload);
    }

    @Transactional
    public List<TrafficBoatPriceResponse> savePrices(String authorizationHeader, TrafficBoatPriceBatchPayload payload) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (payload == null || payload.items() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_BOAT_PRICES_REQUIRED");
        }
        for (TrafficBoatPricePayload item : payload.items()) {
            validatePrice(item);
            trafficServiceRepository.savePrice(currentUser.companyId(), item);
        }
        return trafficServiceRepository.listPrices(currentUser.companyId(), null, null);
    }

    private void validateOrder(TrafficServiceOrderPayload payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SERVICE_ORDER_REQUIRED");
        }
        if (isBlank(payload.seaArea())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SEA_AREA_REQUIRED");
        }
        if (isBlank(payload.anchorageCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANCHORAGE_REQUIRED");
        }
    }

    private void validateRequest(TrafficServiceRequestPayload payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SERVICE_REQUEST_REQUIRED");
        }
        if (isBlank(payload.seaArea())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SEA_AREA_REQUIRED");
        }
        if (isBlank(payload.anchorageCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANCHORAGE_REQUIRED");
        }
    }

    private void validateQuote(TrafficServiceQuotePayload payload) {
        if (payload == null || payload.quoteAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SERVICE_QUOTE_AMOUNT_REQUIRED");
        }
        if (payload.quoteAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SERVICE_QUOTE_AMOUNT_REQUIRED");
        }
    }

    private void validateShuttle(TrafficShuttlePayload payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SHUTTLE_REQUIRED");
        }
        if (isBlank(payload.seaArea())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SEA_AREA_REQUIRED");
        }
        if (isBlank(payload.anchorageCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANCHORAGE_REQUIRED");
        }
        if (isBlank(payload.departurePoint())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SHUTTLE_DEPARTURE_REQUIRED");
        }
        if (isBlank(payload.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_SHUTTLE_START_TIME_REQUIRED");
        }
    }

    private void validatePrice(TrafficBoatPricePayload payload) {
        if (payload == null || isBlank(payload.anchorageCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANCHORAGE_REQUIRED");
        }
    }

    private void validateRoute(TrafficRoutePlanPayload payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRAFFIC_ROUTE_REQUIRED");
        }
        if (isBlank(payload.routeName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ROUTE_NAME_REQUIRED");
        }
        if (isBlank(payload.serviceDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SERVICE_DATE_REQUIRED");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
