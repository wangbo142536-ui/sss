package com.zswy.shipsupply.transport;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TrafficServiceRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TrafficShuttleNodePayload>> SHUTTLE_NODE_LIST_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;

    public TrafficServiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureTrafficShuttlePriceColumns() {
        if (!tableExists("traffic_shuttle_service")) {
            return;
        }
        if (!columnExists("traffic_shuttle_service", "base_price")) {
            jdbcTemplate.execute("ALTER TABLE traffic_shuttle_service ADD COLUMN base_price DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER return_time");
            jdbcTemplate.execute("UPDATE traffic_shuttle_service SET base_price = COALESCE(shared_price, 0) WHERE base_price = 0");
        }
        if (!columnExists("traffic_shuttle_service", "customs_price")) {
            jdbcTemplate.execute("ALTER TABLE traffic_shuttle_service ADD COLUMN customs_price DECIMAL(12,2) NULL AFTER shared_price");
        }
        if (!columnExists("traffic_shuttle_service", "crane_price")) {
            jdbcTemplate.execute("ALTER TABLE traffic_shuttle_service ADD COLUMN crane_price DECIMAL(12,2) NULL AFTER customs_price");
        }
        if (tableExists("traffic_shuttle_booking")) {
            addColumnIfMissing("traffic_shuttle_booking", "request_no", "VARCHAR(80) NULL AFTER request_id");
            addColumnIfMissing("traffic_shuttle_booking", "purchase_order_id", "BIGINT NULL AFTER request_id");
            addColumnIfMissing("traffic_shuttle_booking", "node_index", "INT NULL AFTER request_no");
            addColumnIfMissing("traffic_shuttle_booking", "node_name", "VARCHAR(80) NULL AFTER node_index");
            addColumnIfMissing("traffic_shuttle_booking", "node_time", "VARCHAR(80) NULL AFTER node_name");
            addColumnIfMissing("traffic_shuttle_booking", "vessel_name", "VARCHAR(160) NULL AFTER node_time");
            addColumnIfMissing("traffic_shuttle_booking", "vessel_imo", "VARCHAR(80) NULL AFTER vessel_name");
            addColumnIfMissing("traffic_shuttle_booking", "anchorage_time", "VARCHAR(80) NULL AFTER vessel_imo");
            addColumnIfMissing("traffic_shuttle_booking", "anchorage_position", "VARCHAR(120) NULL AFTER anchorage_time");
            addColumnIfMissing("traffic_shuttle_booking", "longitude", "DECIMAL(10,7) NULL AFTER anchorage_position");
            addColumnIfMissing("traffic_shuttle_booking", "latitude", "DECIMAL(10,7) NULL AFTER longitude");
            addColumnIfMissing("traffic_shuttle_booking", "pallet_count", "VARCHAR(40) NULL AFTER anchorage_position");
            addColumnIfMissing("traffic_shuttle_booking", "allow_share", "TINYINT(1) NOT NULL DEFAULT 1 AFTER amount");
            addColumnIfMissing("traffic_shuttle_booking", "customs_service", "TINYINT(1) NOT NULL DEFAULT 0 AFTER allow_share");
            addColumnIfMissing("traffic_shuttle_booking", "crane_service", "TINYINT(1) NOT NULL DEFAULT 0 AFTER customs_service");
            addColumnIfMissing("traffic_shuttle_booking", "crane_count", "INT NOT NULL DEFAULT 0 AFTER crane_service");
            addColumnIfMissing("traffic_shuttle_booking", "freight_fee", "DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER crane_count");
            addColumnIfMissing("traffic_shuttle_booking", "customs_fee", "DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER freight_fee");
            addColumnIfMissing("traffic_shuttle_booking", "crane_fee", "DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER customs_fee");
        }
    }

    public List<TrafficAnchorageResponse> listAnchorages(String seaArea) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT id, item_code AS anchorage_code, item_name AS anchorage_name, item_value AS sea_area, enabled, sort_order
            FROM sys_dictionary_item
            WHERE type_code = 'ANCHORAGE' AND enabled = 1
            """
        );
        if (!isBlank(seaArea)) {
            sql.append(" AND item_value = ?");
            args.add(seaArea.trim().toUpperCase());
        }
        sql.append(" ORDER BY sort_order ASC, id ASC");
        return jdbcTemplate.query(sql.toString(), this::anchorage, args.toArray());
    }

    public TrafficServiceOrderListResponse listOrders(Long companyId, String keyword, String status, String seaArea, Long purchaseOrderId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (purchaseOrderId != null) {
            Optional<Long> bookingId = findPurchaseOrderBookingId(companyId, purchaseOrderId);
            if (bookingId.isPresent()) {
                List<TrafficServiceOrderResponse> booked = listPurchaseOrderBooking(
                    companyId, purchaseOrderId, bookingId.get(), keyword, status, seaArea
                );
                return new TrafficServiceOrderListResponse(booked, booked.size(), safePage, safeSize);
            }
        }
        List<Object> whereArgs = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE (requester_company_id = ? OR supplier_company_id = ?)");
        whereArgs.add(companyId);
        whereArgs.add(companyId);
        if (!isBlank(keyword)) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            where.append(" AND (service_no LIKE ? ESCAPE '\\\\' OR anchorage_name LIKE ? ESCAPE '\\\\' OR remark LIKE ? ESCAPE '\\\\')");
            whereArgs.add(pattern);
            whereArgs.add(pattern);
            whereArgs.add(pattern);
        }
        if (!isBlank(status)) {
            where.append(" AND status = ?");
            whereArgs.add(status.trim().toUpperCase());
        }
        if (!isBlank(seaArea)) {
            where.append(" AND sea_area = ?");
            whereArgs.add(seaArea.trim().toUpperCase());
        }
        if (purchaseOrderId != null) {
            where.append(" AND purchase_order_id = ?");
            whereArgs.add(purchaseOrderId);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM traffic_service_order" + where, Long.class, whereArgs.toArray());
        List<Object> args = new ArrayList<>(whereArgs);
        args.add((safePage - 1) * safeSize);
        args.add(safeSize);
        List<TrafficServiceOrderResponse> items = jdbcTemplate.query(
            """
            SELECT *
            FROM traffic_service_order
            """ + where + " ORDER BY created_at DESC, id DESC LIMIT ?, ?",
            (rs, rowNum) -> order(rs, true),
            args.toArray()
        );
        return new TrafficServiceOrderListResponse(items, total == null ? 0 : total, safePage, safeSize);
    }

    private Optional<Long> findPurchaseOrderBookingId(Long companyId, Long purchaseOrderId) {
        return jdbcTemplate.query(
            """
            SELECT booking.id
            FROM purchase_order purchase
            JOIN material_demand demand ON demand.id = purchase.demand_id
            JOIN traffic_shuttle_booking booking ON booking.status <> 'CANCELLED'
            JOIN traffic_service_order linked_order ON linked_order.id = booking.traffic_service_order_id
            WHERE purchase.id = ?
              AND (linked_order.requester_company_id = ? OR linked_order.supplier_company_id = ?)
              AND (
                booking.purchase_order_id = purchase.id
                OR JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.bookingId')) = CAST(booking.id AS CHAR)
                OR JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.trafficServiceOrderId')) = CAST(booking.traffic_service_order_id AS CHAR)
                OR (
                  booking.request_id = purchase.demand_id
                  AND booking.purchase_order_id IS NULL
                  AND (linked_order.purchase_order_id IS NULL OR linked_order.purchase_order_id = purchase.id)
                  AND NOT EXISTS (
                    SELECT 1 FROM purchase_order other_purchase
                    WHERE other_purchase.demand_id = purchase.demand_id
                      AND other_purchase.id <> purchase.id
                  )
                )
              )
            ORDER BY CASE
              WHEN JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.bookingId')) = CAST(booking.id AS CHAR) THEN 0
              WHEN JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.trafficServiceOrderId')) = CAST(booking.traffic_service_order_id AS CHAR) THEN 1
              WHEN booking.purchase_order_id = purchase.id THEN 2
              ELSE 3
            END, booking.id DESC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("id"),
            purchaseOrderId,
            companyId,
            companyId
        ).stream().findFirst();
    }

    private List<TrafficServiceOrderResponse> listPurchaseOrderBooking(
        Long companyId,
        Long purchaseOrderId,
        Long bookingId,
        String keyword,
        String status,
        String seaArea
    ) {
        List<Object> args = new ArrayList<>();
        args.add(bookingId);
        args.add(companyId);
        args.add(companyId);
        StringBuilder filters = new StringBuilder();
        if (!isBlank(keyword)) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            filters.append(" AND (service_order.service_no LIKE ? ESCAPE '\\\\' OR service_order.anchorage_name LIKE ? ESCAPE '\\\\' OR service_order.remark LIKE ? ESCAPE '\\\\')");
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (!isBlank(status)) {
            filters.append(" AND service_order.status = ?");
            args.add(status.trim().toUpperCase());
        }
        if (!isBlank(seaArea)) {
            filters.append(" AND service_order.sea_area = ?");
            args.add(seaArea.trim().toUpperCase());
        }
        return jdbcTemplate.query(
            """
            SELECT service_order.*,
                   booking.id AS resolved_booking_id,
                   booking.shuttle_service_id AS resolved_shuttle_service_id,
                   booking.node_index AS resolved_booking_node_index,
                   booking.node_name AS resolved_booking_node_name,
                   booking.node_time AS resolved_booking_node_time,
                   booking.amount AS resolved_booking_amount,
                   booking.freight_fee AS resolved_booking_freight_fee,
                   booking.customs_fee AS resolved_booking_customs_fee,
                   booking.crane_fee AS resolved_booking_crane_fee,
                   shuttle.shuttle_no AS resolved_shuttle_no,
                   shuttle.departure_point AS resolved_shuttle_departure_point,
                   shuttle.destination_point AS resolved_shuttle_destination_point,
                   shuttle.start_time AS resolved_shuttle_start_time,
                   shuttle.return_time AS resolved_shuttle_return_time,
                   shuttle.service_nodes_json AS resolved_shuttle_service_nodes_json
            FROM traffic_shuttle_booking booking
            JOIN traffic_shuttle_service shuttle ON shuttle.id = booking.shuttle_service_id
            JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
            WHERE booking.id = ?
              AND (service_order.requester_company_id = ? OR service_order.supplier_company_id = ?)
            """ + filters,
            (rs, rowNum) -> order(rs, true, purchaseOrderId, true),
            args.toArray()
        );
    }

    public Optional<TrafficLowestPriceResponse> lowestPrice(String anchorageCode, boolean allowShare) {
        TrafficPricePick price = lowestEnabledPrice(normalize(anchorageCode), allowShare);
        if (price == null) return Optional.empty();
        BigDecimal amount = allowShare ? price.sharedPrice() : price.basePrice();
        return Optional.of(new TrafficLowestPriceResponse(
            price.supplierCompanyId(),
            price.supplierCompanyName(),
            normalize(anchorageCode),
            anchorageName(anchorageCode),
            allowShare,
            price.basePrice(),
            price.sharedPrice(),
            amount
        ));
    }

    public Optional<TrafficServiceOrderResponse> getOrder(Long companyId, Long orderId) {
        List<TrafficServiceOrderResponse> rows = jdbcTemplate.query(
            "SELECT * FROM traffic_service_order WHERE id = ? AND (requester_company_id = ? OR supplier_company_id = ?)",
            (rs, rowNum) -> order(rs, true),
            orderId,
            companyId,
            companyId
        );
        return rows.stream().findFirst();
    }

    public TrafficServiceOrderResponse createOrder(Long companyId, Long userId, TrafficServiceOrderPayload payload) {
        String anchorageName = anchorageName(payload.anchorageCode());
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_order (
              service_no, requester_company_id, fee_type, sea_area, anchorage_code, anchorage_name, use_time,
              service_type, passenger_type, passenger_count, cargo_type, return_trip, allow_share,
              base_price, shared_price, status, remark,
              business_contact_id, business_contact_name, business_contact_phone, accepted_at,
              traffic_vessel_id, traffic_vessel_name, handler_contact_id, handler_name, handler_phone,
              supplier_message, departure_time, arrival_time, return_start_time, return_end_time,
              sign_photo_url, pickup_photo_url, return_arrival_photo_url, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            nextNo(),
            companyId,
            defaultFeeType(payload.feeType()),
            normalize(payload.seaArea()),
            normalize(payload.anchorageCode()),
            anchorageName,
            trimToNull(payload.useTime()),
            defaultText(payload.serviceType(), "PERSONNEL"),
            defaultText(payload.passengerType(), "NORMAL"),
            payload.passengerCount(),
            defaultText(payload.cargoType(), "CARGO"),
            Boolean.TRUE.equals(payload.returnTrip()) ? 1 : 0,
            Boolean.TRUE.equals(payload.allowShare()) ? 1 : 0,
            payload.basePrice(),
            payload.sharedPrice(),
            trimToNull(payload.remark()),
            payload.businessContactId(),
            trimToNull(payload.businessContactName()),
            trimToNull(payload.businessContactPhone()),
            trimToNull(payload.acceptedAt()),
            payload.trafficVesselId(),
            trimToNull(payload.trafficVesselName()),
            payload.handlerContactId(),
            trimToNull(payload.handlerName()),
            trimToNull(payload.handlerPhone()),
            trimToNull(payload.supplierMessage()),
            trimToNull(payload.departureTime()),
            trimToNull(payload.arrivalTime()),
            trimToNull(payload.returnStartTime()),
            trimToNull(payload.returnEndTime()),
            trimToNull(payload.signPhotoUrl()),
            trimToNull(payload.pickupPhotoUrl()),
            trimToNull(payload.returnArrivalPhotoUrl()),
            userId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        replaceCargos(id, payload.cargos());
        return getOrder(companyId, id).orElseThrow();
    }

    public void createLinkedOrders(
        Long requesterCompanyId,
        Long userId,
        Long demandId,
        Long purchaseOrderId,
        Map<String, Object> trafficService,
        BigDecimal freightFee,
        BigDecimal customsFee,
        BigDecimal craneFee
    ) {
        if (trafficService == null || trafficService.isEmpty()) {
            throw new IllegalArgumentException("TRAFFIC_SERVICE_REQUIRED");
        }
        String anchorageCode = normalize(mapText(trafficService, "anchorageCode"));
        if (isBlank(anchorageCode)) {
            throw new IllegalArgumentException("TRAFFIC_ANCHORAGE_REQUIRED");
        }
        boolean allowShare = mapBoolean(trafficService, "allowShare");
        String remark = trimToNull(mapText(trafficService, "remark"));

        TrafficPricePick price = lowestEnabledPrice(anchorageCode, allowShare);
        if (price == null) {
            throw new IllegalArgumentException("TRAFFIC_PRICE_NOT_FOUND");
        }
        insertLinkedOrder(requesterCompanyId, userId, demandId, purchaseOrderId, trafficService, price, "FREIGHT", amountOrFallback(allowShare ? price.sharedPrice() : price.basePrice(), freightFee), remark);
        insertLinkedOrder(requesterCompanyId, userId, demandId, purchaseOrderId, trafficService, price, "CUSTOMS", amountOrFallback(customsFee, BigDecimal.ZERO), remark);
        insertLinkedOrder(requesterCompanyId, userId, demandId, purchaseOrderId, trafficService, price, "CRANE", amountOrFallback(craneFee, BigDecimal.ZERO), remark);
    }

    private void insertLinkedOrder(
        Long requesterCompanyId,
        Long userId,
        Long demandId,
        Long purchaseOrderId,
        Map<String, Object> trafficService,
        TrafficPricePick price,
        String feeType,
        BigDecimal amount,
        String remark
    ) {
        String anchorageCode = normalize(mapText(trafficService, "anchorageCode"));
        String seaArea = normalize(mapText(trafficService, "seaArea"));
        String useTime = trimToNull(mapText(trafficService, "useTime"));
        String serviceType = defaultText(mapText(trafficService, "serviceType"), "PERSONNEL");
        String passengerType = defaultText(mapText(trafficService, "passengerType"), "NORMAL");
        Integer passengerCount = mapInt(trafficService, "passengerCount");
        String cargoType = defaultText(mapText(trafficService, "cargoType"), "CARGO");
        boolean returnTrip = mapBoolean(trafficService, "returnTrip");
        boolean allowShare = mapBoolean(trafficService, "allowShare");
        String anchorageName = anchorageName(anchorageCode);
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_order (
              service_no, requester_company_id, demand_id, purchase_order_id, supplier_company_id, supplier_company_name, fee_type,
              sea_area, anchorage_code, anchorage_name, use_time,
              service_type, passenger_type, passenger_count, cargo_type, return_trip, allow_share,
              base_price, shared_price, status, remark, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_CONFIRM', ?, ?)
            """,
            nextNo(),
            requesterCompanyId,
            demandId,
            purchaseOrderId,
            price.supplierCompanyId(),
            price.supplierCompanyName(),
            defaultFeeType(feeType),
            seaArea,
            anchorageCode,
            anchorageName,
            useTime,
            serviceType,
            passengerType,
            passengerCount,
            cargoType,
            returnTrip ? 1 : 0,
            allowShare ? 1 : 0,
            amount,
            amount,
            remark,
            userId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        replaceCargos(id, mapCargos(trafficService));
    }

    public Optional<TrafficServiceOrderResponse> updateOrder(Long companyId, Long orderId, TrafficServiceOrderPayload payload) {
        String anchorageName = anchorageName(payload.anchorageCode());
        int updated = jdbcTemplate.update(
            """
            UPDATE traffic_service_order
            SET sea_area = ?, anchorage_code = ?, anchorage_name = ?, use_time = ?,
                fee_type = ?, service_type = ?, passenger_type = ?, passenger_count = ?, cargo_type = ?,
                return_trip = ?, allow_share = ?, base_price = ?, shared_price = ?, remark = ?,
                business_contact_id = ?, business_contact_name = ?, business_contact_phone = ?, accepted_at = ?,
                traffic_vessel_id = ?, traffic_vessel_name = ?, handler_contact_id = ?, handler_name = ?, handler_phone = ?,
                supplier_message = ?, departure_time = ?, arrival_time = ?, return_start_time = ?, return_end_time = ?,
                sign_photo_url = ?, pickup_photo_url = ?, return_arrival_photo_url = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND requester_company_id = ? AND status <> 'DISCARDED'
            """,
            normalize(payload.seaArea()),
            normalize(payload.anchorageCode()),
            anchorageName,
            trimToNull(payload.useTime()),
            defaultFeeType(payload.feeType()),
            defaultText(payload.serviceType(), "PERSONNEL"),
            defaultText(payload.passengerType(), "NORMAL"),
            payload.passengerCount(),
            defaultText(payload.cargoType(), "CARGO"),
            Boolean.TRUE.equals(payload.returnTrip()) ? 1 : 0,
            Boolean.TRUE.equals(payload.allowShare()) ? 1 : 0,
            payload.basePrice(),
            payload.sharedPrice(),
            trimToNull(payload.remark()),
            payload.businessContactId(),
            trimToNull(payload.businessContactName()),
            trimToNull(payload.businessContactPhone()),
            trimToNull(payload.acceptedAt()),
            payload.trafficVesselId(),
            trimToNull(payload.trafficVesselName()),
            payload.handlerContactId(),
            trimToNull(payload.handlerName()),
            trimToNull(payload.handlerPhone()),
            trimToNull(payload.supplierMessage()),
            trimToNull(payload.departureTime()),
            trimToNull(payload.arrivalTime()),
            trimToNull(payload.returnStartTime()),
            trimToNull(payload.returnEndTime()),
            trimToNull(payload.signPhotoUrl()),
            trimToNull(payload.pickupPhotoUrl()),
            trimToNull(payload.returnArrivalPhotoUrl()),
            orderId,
            companyId
        );
        if (updated == 0) return Optional.empty();
        replaceCargos(orderId, payload.cargos());
        return getOrder(companyId, orderId);
    }

    public Optional<TrafficServiceOrderResponse> discardOrder(Long companyId, Long orderId) {
        int updated = jdbcTemplate.update(
            "UPDATE traffic_service_order SET status = 'DISCARDED', updated_at = CURRENT_TIMESTAMP WHERE id = ? AND requester_company_id = ?",
            orderId,
            companyId
        );
        if (updated == 0) return Optional.empty();
        return getOrder(companyId, orderId);
    }

    public Optional<TrafficServiceOrderResponse> confirmOrder(Long supplierCompanyId, Long orderId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE traffic_service_order
            SET status = 'WAITING_SERVICE', updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND supplier_company_id = ? AND status = 'PENDING_CONFIRM'
            """,
            orderId,
            supplierCompanyId
        );
        if (updated == 0) return getOrder(supplierCompanyId, orderId)
            .filter(order -> "WAITING_SERVICE".equalsIgnoreCase(order.status()) || "CONFIRMED".equalsIgnoreCase(order.status()));
        return getOrder(supplierCompanyId, orderId);
    }

    public TrafficRoutePlanListResponse listRoutes(Long companyId, String keyword, String status, String seaArea, String serviceDate, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> whereArgs = new ArrayList<>();
        StringBuilder where = new StringBuilder(
            """
            WHERE (
              route.supplier_company_id IS NULL
              OR route.supplier_company_id = ?
              OR EXISTS (
                SELECT 1
                FROM traffic_route_stop stop
                JOIN traffic_service_order service_order ON service_order.id = stop.traffic_service_order_id
                WHERE stop.route_plan_id = route.id
                  AND (service_order.requester_company_id = ? OR service_order.supplier_company_id = ?)
              )
            )
            """
        );
        whereArgs.add(companyId);
        whereArgs.add(companyId);
        whereArgs.add(companyId);
        if (!isBlank(keyword)) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            where.append(" AND (route.route_no LIKE ? ESCAPE '\\\\' OR route.route_name LIKE ? ESCAPE '\\\\' OR route.supplier_company_name LIKE ? ESCAPE '\\\\')");
            whereArgs.add(pattern);
            whereArgs.add(pattern);
            whereArgs.add(pattern);
        }
        if (!isBlank(status)) {
            where.append(" AND route.status = ?");
            whereArgs.add(status.trim().toUpperCase());
        }
        if (!isBlank(seaArea)) {
            where.append(" AND route.sea_area = ?");
            whereArgs.add(seaArea.trim().toUpperCase());
        }
        if (!isBlank(serviceDate)) {
            where.append(" AND route.service_date = ?");
            whereArgs.add(serviceDate.trim());
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM traffic_route_plan route " + where, Long.class, whereArgs.toArray());
        List<Object> args = new ArrayList<>(whereArgs);
        args.add((safePage - 1) * safeSize);
        args.add(safeSize);
        List<TrafficRoutePlanResponse> items = jdbcTemplate.query(
            "SELECT route.* FROM traffic_route_plan route " + where + " ORDER BY route.service_date DESC, route.id DESC LIMIT ?, ?",
            this::route,
            args.toArray()
        );
        return new TrafficRoutePlanListResponse(items, total == null ? 0 : total, safePage, safeSize);
    }

    public Optional<TrafficRouteDetailResponse> getRoute(Long companyId, Long routeId) {
        List<TrafficRoutePlanResponse> routes = jdbcTemplate.query(
            """
            SELECT route.*
            FROM traffic_route_plan route
            WHERE route.id = ?
              AND (
                route.supplier_company_id IS NULL
                OR route.supplier_company_id = ?
                OR EXISTS (
                  SELECT 1
                  FROM traffic_route_stop stop
                  JOIN traffic_service_order service_order ON service_order.id = stop.traffic_service_order_id
                  WHERE stop.route_plan_id = route.id
                    AND (service_order.requester_company_id = ? OR service_order.supplier_company_id = ?)
                )
              )
            """,
            this::route,
            routeId,
            companyId,
            companyId,
            companyId
        );
        if (routes.isEmpty()) return Optional.empty();
        return Optional.of(new TrafficRouteDetailResponse(routes.get(0), routeStops(routeId), routeEvents(routeId)));
    }

    public TrafficRouteDetailResponse createRoute(Long companyId, Long userId, TrafficRoutePlanPayload payload) {
        jdbcTemplate.update(
            """
            INSERT INTO traffic_route_plan (
              route_no, route_name, service_date, sea_area, supplier_company_id, supplier_company_name,
              traffic_vessel_id, traffic_vessel_name, planned_departure_time, planned_finish_time,
              allow_share, estimated_cost, remark, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            nextRouteNo(),
            payload.routeName().trim(),
            payload.serviceDate(),
            normalize(payload.seaArea()),
            payload.supplierCompanyId(),
            trimToNull(payload.supplierCompanyName()),
            payload.trafficVesselId(),
            trimToNull(payload.trafficVesselName()),
            trimToNull(payload.plannedDepartureTime()),
            trimToNull(payload.plannedFinishTime()),
            payload.allowShare() == null || payload.allowShare() ? 1 : 0,
            payload.estimatedCost(),
            trimToNull(payload.remark()),
            userId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        addRouteEvent(id, null, "ROUTE_CREATED", "路线已创建", userId);
        recalculateRoute(id);
        return getRoute(companyId, id).orElseThrow();
    }

    public Optional<TrafficRouteDetailResponse> addRouteStop(Long companyId, Long userId, Long routeId, TrafficRouteStopPayload payload) {
        Optional<TrafficRouteDetailResponse> route = getRoute(companyId, routeId);
        if (route.isEmpty()) return Optional.empty();
        List<TrafficServiceOrderResponse> orders = jdbcTemplate.query(
            """
            SELECT *
            FROM traffic_service_order
            WHERE id = ?
              AND (requester_company_id = ? OR supplier_company_id = ?)
              AND status <> 'DISCARDED'
            """,
            (rs, rowNum) -> order(rs, false),
            payload.trafficServiceOrderId(),
            companyId,
            companyId
        );
        if (orders.isEmpty()) return Optional.empty();
        TrafficServiceOrderResponse serviceOrder = orders.get(0);
        Integer nextSequence = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(stop_sequence), 0) + 1 FROM traffic_route_stop WHERE route_plan_id = ?",
            Integer.class,
            routeId
        );
        jdbcTemplate.update(
            """
            INSERT INTO traffic_route_stop (
              route_plan_id, traffic_service_order_id, stop_sequence, anchorage_code, anchorage_name,
              planned_service_time, service_type, contact_name, contact_phone, status, remark
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PLANNED', ?)
            ON DUPLICATE KEY UPDATE
              route_plan_id = VALUES(route_plan_id),
              stop_sequence = VALUES(stop_sequence),
              anchorage_code = VALUES(anchorage_code),
              anchorage_name = VALUES(anchorage_name),
              planned_service_time = VALUES(planned_service_time),
              service_type = VALUES(service_type),
              contact_name = VALUES(contact_name),
              contact_phone = VALUES(contact_phone),
              status = 'PLANNED',
              remark = VALUES(remark),
              updated_at = CURRENT_TIMESTAMP
            """,
            routeId,
            payload.trafficServiceOrderId(),
            nextSequence == null ? 1 : nextSequence,
            serviceOrder.anchorageCode(),
            serviceOrder.anchorageName(),
            trimToNull(payload.plannedServiceTime()),
            serviceOrder.serviceType(),
            serviceOrder.businessContactName(),
            serviceOrder.businessContactPhone(),
            trimToNull(payload.remark())
        );
        Long stopId = jdbcTemplate.queryForObject(
            "SELECT id FROM traffic_route_stop WHERE traffic_service_order_id = ?",
            Long.class,
            payload.trafficServiceOrderId()
        );
        jdbcTemplate.update(
            """
            UPDATE traffic_service_order
            SET route_plan_id = ?, route_stop_id = ?, planned_sequence = ?, planned_service_time = ?, status = CASE WHEN status = 'PENDING_CONFIRM' THEN 'PENDING_CONFIRM' ELSE status END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            routeId,
            stopId,
            nextSequence == null ? 1 : nextSequence,
            trimToNull(payload.plannedServiceTime()),
            payload.trafficServiceOrderId()
        );
        if (route.get().route().supplierCompanyId() == null && serviceOrder.supplierCompanyId() != null) {
            jdbcTemplate.update(
                "UPDATE traffic_route_plan SET supplier_company_id = ?, supplier_company_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                serviceOrder.supplierCompanyId(),
                serviceOrder.supplierCompanyName(),
                routeId
            );
        }
        addRouteEvent(routeId, payload.trafficServiceOrderId(), "STOP_ADDED", "订单加入路线", userId);
        recalculateRoute(routeId);
        return getRoute(companyId, routeId);
    }

    public Optional<TrafficRouteDetailResponse> reorderRouteStops(Long companyId, Long routeId, TrafficRouteStopReorderPayload payload) {
        if (getRoute(companyId, routeId).isEmpty()) return Optional.empty();
        if (payload != null && payload.stops() != null) {
            for (TrafficRouteStopOrderPayload stop : payload.stops()) {
                if (stop == null || stop.routeStopId() == null || stop.stopSequence() == null) continue;
                jdbcTemplate.update(
                    "UPDATE traffic_route_stop SET stop_sequence = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND route_plan_id = ?",
                    stop.stopSequence(),
                    stop.routeStopId(),
                    routeId
                );
            }
            recalculateRoute(routeId);
        }
        return getRoute(companyId, routeId);
    }

    public Optional<TrafficRouteDetailResponse> removeRouteStop(Long companyId, Long routeId, Long stopId) {
        if (getRoute(companyId, routeId).isEmpty()) return Optional.empty();
        List<Long> orderIds = jdbcTemplate.query(
            "SELECT traffic_service_order_id FROM traffic_route_stop WHERE id = ? AND route_plan_id = ?",
            (rs, rowNum) -> rs.getLong("traffic_service_order_id"),
            stopId,
            routeId
        );
        int deleted = jdbcTemplate.update("DELETE FROM traffic_route_stop WHERE id = ? AND route_plan_id = ?", stopId, routeId);
        if (deleted == 0) return Optional.empty();
        if (!orderIds.isEmpty()) {
            jdbcTemplate.update(
                "UPDATE traffic_service_order SET route_plan_id = NULL, route_stop_id = NULL, planned_sequence = NULL, planned_service_time = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                orderIds.get(0)
            );
        }
        recalculateRoute(routeId);
        return getRoute(companyId, routeId);
    }

    public Optional<TrafficRouteDetailResponse> changeRouteStatus(Long companyId, Long userId, Long routeId, String status) {
        Optional<TrafficRouteDetailResponse> route = getRoute(companyId, routeId);
        if (route.isEmpty()) return Optional.empty();
        String normalized = defaultText(status, "DRAFT");
        int updated = jdbcTemplate.update(
            "UPDATE traffic_route_plan SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            normalized,
            routeId
        );
        if (updated == 0) return Optional.empty();
        if ("READY".equals(normalized)) {
            jdbcTemplate.update(
                "UPDATE traffic_service_order SET status = 'PLANNED', updated_at = CURRENT_TIMESTAMP WHERE route_plan_id = ? AND status <> 'DISCARDED'",
                routeId
            );
        } else if ("IN_PROGRESS".equals(normalized)) {
            jdbcTemplate.update(
                "UPDATE traffic_service_order SET status = 'IN_TRANSIT', updated_at = CURRENT_TIMESTAMP WHERE route_plan_id = ? AND status <> 'DISCARDED'",
                routeId
            );
        } else if ("COMPLETED".equals(normalized)) {
            jdbcTemplate.update(
                "UPDATE traffic_service_order SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE route_plan_id = ? AND status <> 'DISCARDED'",
                routeId
            );
        } else if ("DISCARDED".equals(normalized)) {
            jdbcTemplate.update(
                "UPDATE traffic_service_order SET route_plan_id = NULL, route_stop_id = NULL, planned_sequence = NULL, planned_service_time = NULL, updated_at = CURRENT_TIMESTAMP WHERE route_plan_id = ? AND status NOT IN ('COMPLETED', 'DISCARDED')",
                routeId
            );
        }
        addRouteEvent(routeId, null, "ROUTE_" + normalized, "路线状态更新为 " + normalized, userId);
        recalculateRoute(routeId);
        return getRoute(companyId, routeId);
    }

    public TrafficServiceRequestResponse createRequest(Long companyId, Long userId, TrafficServiceRequestPayload payload) {
        String status = Boolean.TRUE.equals(payload.publish()) ? "PUBLISHED" : "DRAFT";
        String anchorageCode = normalize(payload.anchorageCode());
        String anchorageName = anchorageName(anchorageCode);
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_request (
              request_no, requester_company_id, demand_id, purchase_order_id, fee_type, sea_area,
              anchorage_code, anchorage_name, use_time, service_type, passenger_type, passenger_count,
              cargo_type, return_trip, allow_share, remark, status, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            nextRequestNo(),
            companyId,
            payload.demandId(),
            payload.purchaseOrderId(),
            defaultFeeType(payload.feeType()),
            normalize(payload.seaArea()),
            anchorageCode,
            anchorageName,
            trimToNull(payload.useTime()),
            defaultText(payload.serviceType(), "GOODS"),
            defaultText(payload.passengerType(), "NORMAL"),
            payload.passengerCount(),
            defaultText(payload.cargoType(), "CARGO"),
            Boolean.TRUE.equals(payload.returnTrip()) ? 1 : 0,
            Boolean.TRUE.equals(payload.allowShare()) ? 1 : 0,
            trimToNull(payload.remark()),
            status,
            userId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        replaceRequestCargos(id, payload.cargos());
        addRequestEvent(id, null, "REQUEST_CREATED", "Traffic service request created", userId);
        if ("PUBLISHED".equals(status)) {
            addRequestEvent(id, null, "REQUEST_PUBLISHED", "Traffic service request published", userId);
        }
        return getRequest(companyId, id).orElseThrow();
    }

    public TrafficServiceRequestListResponse listRequests(Long companyId, String keyword, String status, String seaArea, String anchorageCode, int page, int size) {
        return listRequestsInternal(companyId, false, keyword, status, seaArea, anchorageCode, page, size);
    }

    public TrafficServiceRequestListResponse listSupplierRequests(Long supplierCompanyId, String keyword, String status, String seaArea, String anchorageCode, int page, int size) {
        return listRequestsInternal(supplierCompanyId, true, keyword, status, seaArea, anchorageCode, page, size);
    }

    private TrafficServiceRequestListResponse listRequestsInternal(Long companyId, boolean supplierHall, String keyword, String status, String seaArea, String anchorageCode, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> whereArgs = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (supplierHall) {
            where.append(" WHERE request.status IN ('PUBLISHED', 'QUOTING', 'AWARDED', 'ORDER_CREATED')");
        } else {
            where.append(" WHERE request.requester_company_id = ?");
            whereArgs.add(companyId);
        }
        if (!isBlank(keyword)) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            where.append(" AND (request.request_no LIKE ? ESCAPE '\\\\' OR request.anchorage_name LIKE ? ESCAPE '\\\\' OR request.remark LIKE ? ESCAPE '\\\\')");
            whereArgs.add(pattern);
            whereArgs.add(pattern);
            whereArgs.add(pattern);
        }
        if (!isBlank(status)) {
            where.append(" AND request.status = ?");
            whereArgs.add(status.trim().toUpperCase());
        }
        if (!isBlank(seaArea)) {
            where.append(" AND request.sea_area = ?");
            whereArgs.add(seaArea.trim().toUpperCase());
        }
        if (!isBlank(anchorageCode)) {
            where.append(" AND request.anchorage_code = ?");
            whereArgs.add(normalize(anchorageCode));
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM traffic_service_request request" + where, Long.class, whereArgs.toArray());
        List<Object> args = new ArrayList<>(whereArgs);
        args.add((safePage - 1) * safeSize);
        args.add(safeSize);
        List<TrafficServiceRequestResponse> items = jdbcTemplate.query(
            "SELECT request.* FROM traffic_service_request request" + where + " ORDER BY request.created_at DESC, request.id DESC LIMIT ?, ?",
            (rs, rowNum) -> request(rs, false),
            args.toArray()
        );
        return new TrafficServiceRequestListResponse(items, total == null ? 0 : total, safePage, safeSize);
    }

    public Optional<TrafficServiceRequestDetailResponse> getRequestDetail(Long companyId, Long requestId) {
        return getRequest(companyId, requestId)
            .map(request -> new TrafficServiceRequestDetailResponse(request, quotes(requestId), requestEvents(requestId)));
    }

    private Optional<TrafficServiceRequestResponse> getRequest(Long companyId, Long requestId) {
        List<TrafficServiceRequestResponse> rows = jdbcTemplate.query(
            """
            SELECT *
            FROM traffic_service_request
            WHERE id = ?
              AND (
                requester_company_id = ?
                OR status IN ('PUBLISHED', 'QUOTING', 'AWARDED', 'ORDER_CREATED')
                OR selected_supplier_company_id = ?
                OR EXISTS (
                  SELECT 1 FROM traffic_service_quote quote
                  WHERE quote.request_id = traffic_service_request.id AND quote.supplier_company_id = ?
                )
              )
            """,
            (rs, rowNum) -> request(rs, true),
            requestId,
            companyId,
            companyId,
            companyId
        );
        return rows.stream().findFirst();
    }

    public Optional<TrafficServiceRequestResponse> publishRequest(Long companyId, Long userId, Long requestId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE traffic_service_request
            SET status = 'PUBLISHED', updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND requester_company_id = ? AND status = 'DRAFT'
            """,
            requestId,
            companyId
        );
        if (updated == 0) return Optional.empty();
        addRequestEvent(requestId, null, "REQUEST_PUBLISHED", "Traffic service request published", userId);
        return getRequest(companyId, requestId);
    }

    public Optional<TrafficServiceRequestResponse> cancelRequest(Long companyId, Long userId, Long requestId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE traffic_service_request
            SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND requester_company_id = ? AND status NOT IN ('AWARDED', 'ORDER_CREATED', 'CANCELLED')
            """,
            requestId,
            companyId
        );
        if (updated == 0) return Optional.empty();
        addRequestEvent(requestId, null, "REQUEST_CANCELLED", "Traffic service request cancelled", userId);
        return getRequest(companyId, requestId);
    }

    public Optional<TrafficServiceQuoteResponse> submitQuote(Long supplierCompanyId, Long userId, Long requestId, TrafficServiceQuotePayload payload) {
        Optional<TrafficServiceRequestResponse> request = getRequest(supplierCompanyId, requestId);
        if (request.isEmpty() || !List.of("PUBLISHED", "QUOTING").contains(request.get().status())) return Optional.empty();
        String companyName = companyName(supplierCompanyId);
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_quote (
              request_id, supplier_company_id, supplier_company_name, quote_amount, base_price, shared_price,
              currency, available_start_time, available_return_time, traffic_vessel_id, traffic_vessel_name,
              contact_name, contact_phone, message, status, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', ?)
            ON DUPLICATE KEY UPDATE
              supplier_company_name = VALUES(supplier_company_name),
              quote_amount = VALUES(quote_amount),
              base_price = VALUES(base_price),
              shared_price = VALUES(shared_price),
              currency = VALUES(currency),
              available_start_time = VALUES(available_start_time),
              available_return_time = VALUES(available_return_time),
              traffic_vessel_id = VALUES(traffic_vessel_id),
              traffic_vessel_name = VALUES(traffic_vessel_name),
              contact_name = VALUES(contact_name),
              contact_phone = VALUES(contact_phone),
              message = VALUES(message),
              status = 'UPDATED',
              updated_at = CURRENT_TIMESTAMP
            """,
            requestId,
            supplierCompanyId,
            companyName,
            payload.quoteAmount() == null ? BigDecimal.ZERO : payload.quoteAmount(),
            payload.basePrice(),
            payload.sharedPrice(),
            defaultText(payload.currency(), "CNY"),
            trimToNull(payload.availableStartTime()),
            trimToNull(payload.availableReturnTime()),
            payload.trafficVesselId(),
            trimToNull(payload.trafficVesselName()),
            trimToNull(payload.contactName()),
            trimToNull(payload.contactPhone()),
            trimToNull(payload.message()),
            userId
        );
        Long quoteId = jdbcTemplate.queryForObject(
            "SELECT id FROM traffic_service_quote WHERE request_id = ? AND supplier_company_id = ?",
            Long.class,
            requestId,
            supplierCompanyId
        );
        updateRequestRecommendedQuote(requestId);
        jdbcTemplate.update(
            "UPDATE traffic_service_request SET status = 'QUOTING', updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PUBLISHED'",
            requestId
        );
        addRequestEvent(requestId, quoteId, "QUOTE_SUBMITTED", "Traffic service quote submitted", userId);
        return quotes(requestId).stream().filter(quote -> quote.quoteId().equals(quoteId)).findFirst();
    }

    public Optional<TrafficServiceQuoteResponse> withdrawQuote(Long supplierCompanyId, Long userId, Long quoteId) {
        List<Long> requestIds = jdbcTemplate.query(
            "SELECT request_id FROM traffic_service_quote WHERE id = ? AND supplier_company_id = ?",
            (rs, rowNum) -> rs.getLong("request_id"),
            quoteId,
            supplierCompanyId
        );
        if (requestIds.isEmpty()) return Optional.empty();
        Long requestId = requestIds.get(0);
        int updated = jdbcTemplate.update(
            "UPDATE traffic_service_quote SET status = 'WITHDRAWN', updated_at = CURRENT_TIMESTAMP WHERE id = ? AND supplier_company_id = ? AND status NOT IN ('SELECTED', 'REJECTED')",
            quoteId,
            supplierCompanyId
        );
        if (updated == 0) return Optional.empty();
        updateRequestRecommendedQuote(requestId);
        addRequestEvent(requestId, quoteId, "QUOTE_WITHDRAWN", "Traffic service quote withdrawn", userId);
        return quotes(requestId).stream().filter(quote -> quote.quoteId().equals(quoteId)).findFirst();
    }

    public Optional<TrafficServiceRequestDetailResponse> selectQuote(Long requesterCompanyId, Long userId, Long requestId, Long quoteId) {
        Optional<TrafficServiceRequestResponse> request = getRequest(requesterCompanyId, requestId);
        if (request.isEmpty() || !requesterCompanyId.equals(request.get().requesterCompanyId())) return Optional.empty();
        List<TrafficServiceQuoteResponse> quoteRows = quotes(requestId).stream()
            .filter(quote -> quote.quoteId().equals(quoteId) && List.of("SUBMITTED", "UPDATED").contains(quote.status()))
            .toList();
        if (quoteRows.isEmpty()) return Optional.empty();
        TrafficServiceQuoteResponse quote = quoteRows.get(0);
        Long orderId = createOrderFromQuote(request.get(), quote, userId);
        jdbcTemplate.update("UPDATE traffic_service_quote SET status = 'REJECTED', updated_at = CURRENT_TIMESTAMP WHERE request_id = ? AND id <> ?", requestId, quoteId);
        jdbcTemplate.update("UPDATE traffic_service_quote SET status = 'SELECTED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", quoteId);
        jdbcTemplate.update(
            """
            UPDATE traffic_service_request
            SET status = 'ORDER_CREATED', selected_quote_id = ?, selected_supplier_company_id = ?,
                traffic_service_order_id = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            quoteId,
            quote.supplierCompanyId(),
            orderId,
            requestId
        );
        addRequestEvent(requestId, quoteId, "QUOTE_SELECTED", "Traffic service quote selected", userId);
        addRequestEvent(requestId, quoteId, "ORDER_CREATED", "Traffic service order created", userId);
        return getRequestDetail(requesterCompanyId, requestId);
    }

    private Long createOrderFromQuote(TrafficServiceRequestResponse request, TrafficServiceQuoteResponse quote, Long userId) {
        BigDecimal amount = amountOrFallback(quote.quoteAmount(), Boolean.TRUE.equals(request.allowShare()) ? quote.sharedPrice() : quote.basePrice());
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_order (
              service_no, requester_company_id, demand_id, purchase_order_id, supplier_company_id, supplier_company_name,
              fee_type, sea_area, anchorage_code, anchorage_name, use_time, service_type, passenger_type, passenger_count,
              cargo_type, return_trip, allow_share, base_price, shared_price, status, remark,
              traffic_vessel_id, traffic_vessel_name, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_CONFIRM', ?, ?, ?, ?)
            """,
            nextNo(),
            request.requesterCompanyId(),
            request.demandId(),
            request.purchaseOrderId(),
            quote.supplierCompanyId(),
            quote.supplierCompanyName(),
            defaultFeeType(request.feeType()),
            normalize(request.seaArea()),
            normalize(request.anchorageCode()),
            request.anchorageName(),
            trimToNull(request.useTime()),
            defaultText(request.serviceType(), "GOODS"),
            defaultText(request.passengerType(), "NORMAL"),
            request.passengerCount(),
            defaultText(request.cargoType(), "CARGO"),
            Boolean.TRUE.equals(request.returnTrip()) ? 1 : 0,
            Boolean.TRUE.equals(request.allowShare()) ? 1 : 0,
            amount,
            amount,
            trimToNull(request.remark()),
            quote.trafficVesselId(),
            trimToNull(quote.trafficVesselName()),
            userId
        );
        Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        List<TrafficServiceCargoPayload> cargos = request.cargos() == null ? List.of() : request.cargos().stream()
            .map(cargo -> new TrafficServiceCargoPayload(cargo.cargoName(), cargo.weightKg(), cargo.volumeCbm()))
            .toList();
        replaceCargos(orderId, cargos);
        return orderId;
    }

    public TrafficShuttleServiceResponse createShuttle(Long supplierCompanyId, Long userId, TrafficShuttlePayload payload) {
        String anchorageCode = normalize(payload.anchorageCode());
        jdbcTemplate.update(
            """
            INSERT INTO traffic_shuttle_service (
              shuttle_no, supplier_company_id, supplier_company_name, sea_area, anchorage_code, anchorage_name,
              departure_point, destination_point, start_time, return_time, base_price, shared_price, customs_price, crane_price, passenger_capacity, cargo_capacity_kg, cargo_capacity_cbm,
              traffic_vessel_id, traffic_vessel_name, status, remark, service_nodes_json, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            nextShuttleNo(),
            supplierCompanyId,
            companyName(supplierCompanyId),
            normalize(payload.seaArea()),
            anchorageCode,
            anchorageName(anchorageCode),
            trimToNull(payload.departurePoint()),
            trimToNull(payload.destinationPoint()),
            trimToNull(payload.startTime()),
            trimToNull(payload.returnTime()),
            payload.basePrice() == null ? BigDecimal.ZERO : payload.basePrice(),
            payload.sharedPrice() == null ? BigDecimal.ZERO : payload.sharedPrice(),
            payload.customsPrice(),
            payload.cranePrice(),
            payload.passengerCapacity(),
            payload.cargoCapacityKg(),
            payload.cargoCapacityCbm(),
            payload.trafficVesselId(),
            trimToNull(payload.trafficVesselName()),
            normalizeShuttleStatus(payload.status()),
            trimToNull(payload.remark()),
            serializeShuttleNodes(payload.serviceNodes()),
            userId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return getShuttle(supplierCompanyId, id).orElseThrow();
    }

    public TrafficShuttleListResponse listShuttles(Long companyId, boolean supplierOnly, String keyword, String status, String seaArea, String anchorageCode, String startTimeFrom, String startTimeTo, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> whereArgs = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (supplierOnly) {
            where.append(" AND supplier_company_id = ?");
            whereArgs.add(companyId);
        } else {
            where.append(" AND status IN ('PUBLISHED', 'FULL', 'IN_PROGRESS', 'COMPLETED', 'CLOSED', 'CANCELLED')");
        }
        if (!isBlank(keyword)) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            where.append(" AND (shuttle_no LIKE ? ESCAPE '\\\\' OR traffic_vessel_name LIKE ? ESCAPE '\\\\')");
            whereArgs.add(pattern);
            whereArgs.add(pattern);
        }
        if (!isBlank(status)) {
            where.append(" AND status = ?");
            whereArgs.add(status.trim().toUpperCase());
        }
        if (!isBlank(seaArea)) {
            where.append(" AND sea_area = ?");
            whereArgs.add(seaArea.trim().toUpperCase());
        }
        if (!isBlank(anchorageCode)) {
            where.append(" AND anchorage_code = ?");
            whereArgs.add(normalize(anchorageCode));
        }
        if (!isBlank(startTimeFrom)) {
            where.append(" AND start_time >= ?");
            whereArgs.add(trimToNull(startTimeFrom));
        }
        if (!isBlank(startTimeTo)) {
            where.append(" AND return_time <= ?");
            whereArgs.add(trimToNull(startTimeTo));
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM traffic_shuttle_service" + where, Long.class, whereArgs.toArray());
        List<Object> args = new ArrayList<>(whereArgs);
        args.add((safePage - 1) * safeSize);
        args.add(safeSize);
        List<TrafficShuttleServiceResponse> items = jdbcTemplate.query(
            "SELECT * FROM traffic_shuttle_service" + where + " ORDER BY start_time ASC, id DESC LIMIT ?, ?",
            this::shuttle,
            args.toArray()
        );
        items = attachShuttleBookings(companyId, items);
        return new TrafficShuttleListResponse(items, total == null ? 0 : total, safePage, safeSize);
    }

    public Optional<TrafficShuttleServiceResponse> getShuttle(Long companyId, Long shuttleId) {
        List<TrafficShuttleServiceResponse> rows = jdbcTemplate.query(
            "SELECT * FROM traffic_shuttle_service WHERE id = ? AND (supplier_company_id = ? OR status IN ('PUBLISHED', 'FULL', 'IN_PROGRESS', 'COMPLETED', 'CLOSED', 'CANCELLED'))",
            this::shuttle,
            shuttleId,
            companyId
        );
        return rows.stream().findFirst()
            .map(row -> attachShuttleBookings(companyId, List.of(row)).stream().findFirst().orElse(row));
    }

    public Optional<TrafficShuttleServiceResponse> updateShuttle(Long supplierCompanyId, Long shuttleId, TrafficShuttlePayload payload) {
        String anchorageCode = normalize(payload.anchorageCode());
        int updated = jdbcTemplate.update(
            """
            UPDATE traffic_shuttle_service
            SET sea_area = ?,
                anchorage_code = ?,
                anchorage_name = ?,
                departure_point = ?,
                destination_point = ?,
                start_time = ?,
                return_time = ?,
                base_price = ?,
                shared_price = ?,
                customs_price = ?,
                crane_price = ?,
                passenger_capacity = ?,
                cargo_capacity_kg = ?,
                cargo_capacity_cbm = ?,
                traffic_vessel_id = ?,
                traffic_vessel_name = ?,
                status = ?,
                remark = ?,
                service_nodes_json = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND supplier_company_id = ?
            """,
            normalize(payload.seaArea()),
            anchorageCode,
            anchorageName(anchorageCode),
            trimToNull(payload.departurePoint()),
            trimToNull(payload.destinationPoint()),
            trimToNull(payload.startTime()),
            trimToNull(payload.returnTime()),
            payload.basePrice() == null ? BigDecimal.ZERO : payload.basePrice(),
            payload.sharedPrice() == null ? BigDecimal.ZERO : payload.sharedPrice(),
            payload.customsPrice(),
            payload.cranePrice(),
            payload.passengerCapacity(),
            payload.cargoCapacityKg(),
            payload.cargoCapacityCbm(),
            payload.trafficVesselId(),
            trimToNull(payload.trafficVesselName()),
            normalizeShuttleStatus(payload.status()),
            trimToNull(payload.remark()),
            serializeShuttleNodes(payload.serviceNodes()),
            shuttleId,
            supplierCompanyId
        );
        if (updated == 0) return Optional.empty();
        return getShuttle(supplierCompanyId, shuttleId);
    }

    public Optional<TrafficShuttleServiceResponse> changeShuttleStatus(Long supplierCompanyId, Long shuttleId, String status) {
        String normalizedStatus = defaultText(status, "CLOSED");
        int updated = jdbcTemplate.update(
            "UPDATE traffic_shuttle_service SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND supplier_company_id = ?",
            normalizedStatus,
            shuttleId,
            supplierCompanyId
        );
        if (updated == 0) return Optional.empty();
        syncLinkedShuttleOrderStatus(shuttleId, normalizedStatus);
        return getShuttle(supplierCompanyId, shuttleId);
    }

    private void syncLinkedShuttleOrderStatus(Long shuttleId, String shuttleStatus) {
        String normalizedStatus = defaultText(shuttleStatus, "").toUpperCase();
        String purchaseOrderStatus = switch (normalizedStatus) {
            case "IN_PROGRESS" -> "SUPPLYING";
            case "COMPLETED" -> "COMPLETED";
            default -> null;
        };
        String trafficOrderStatus = switch (normalizedStatus) {
            case "IN_PROGRESS" -> "IN_PROGRESS";
            case "COMPLETED" -> "COMPLETED";
            default -> null;
        };
        if (trafficOrderStatus != null) {
            jdbcTemplate.update(
                """
                UPDATE traffic_service_order service_order
                JOIN traffic_shuttle_booking booking ON booking.traffic_service_order_id = service_order.id
                SET service_order.status = ?, service_order.updated_at = CURRENT_TIMESTAMP
                WHERE booking.shuttle_service_id = ?
                  AND booking.status <> 'CANCELLED'
                  AND service_order.status <> 'DISCARDED'
                """,
                trafficOrderStatus,
                shuttleId
            );
        }
        if (purchaseOrderStatus != null) {
            jdbcTemplate.update(
                """
                UPDATE purchase_order purchase
                JOIN (
                  SELECT DISTINCT COALESCE(booking.purchase_order_id, service_order.purchase_order_id, purchase_by_demand.id) AS purchase_order_id
                  FROM traffic_shuttle_booking booking
                  LEFT JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
                  LEFT JOIN purchase_order purchase_by_demand
                    ON purchase_by_demand.demand_id = booking.request_id
                   AND purchase_by_demand.buyer_company_id = booking.requester_company_id
                   AND purchase_by_demand.status <> 'DISCARDED'
                  WHERE booking.shuttle_service_id = ?
                    AND booking.status <> 'CANCELLED'
                ) linked ON linked.purchase_order_id = purchase.id
                SET purchase.status = ?, purchase.updated_at = CURRENT_TIMESTAMP
                """,
                shuttleId,
                purchaseOrderStatus
            );
        }
        syncLinkedPurchaseSupplierStatus(shuttleId, normalizedStatus);
    }

    private void syncLinkedPurchaseSupplierStatus(Long shuttleId, String shuttleStatus) {
        String supplierStatus = switch (shuttleStatus) {
            case "IN_PROGRESS" -> "WAITING_SUPPLY";
            case "COMPLETED" -> "SUPPLIED";
            default -> null;
        };
        if (supplierStatus == null) {
            return;
        }
        String allowedStatuses = switch (supplierStatus) {
            case "WAITING_SUPPLY" -> "'READY_TO_DELIVER', 'PARTIALLY_READY', 'IN_TRANSIT', 'WAITING_SUPPLY'";
            case "SUPPLIED" -> "'READY_TO_DELIVER', 'PARTIALLY_READY', 'IN_TRANSIT', 'WAITING_SUPPLY', 'SUPPLIED', 'PARTIALLY_SUPPLIED'";
            default -> "''";
        };
        jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier supplier
            JOIN (
              SELECT DISTINCT COALESCE(booking.purchase_order_id, service_order.purchase_order_id, purchase_by_demand.id) AS purchase_order_id
              FROM traffic_shuttle_booking booking
              LEFT JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
              LEFT JOIN purchase_order purchase_by_demand
                ON purchase_by_demand.demand_id = booking.request_id
               AND purchase_by_demand.buyer_company_id = booking.requester_company_id
               AND purchase_by_demand.status <> 'DISCARDED'
              WHERE booking.shuttle_service_id = ?
                AND booking.status <> 'CANCELLED'
            ) linked ON linked.purchase_order_id = supplier.order_id
            SET supplier.status = ?,
                supplier.updated_at = CURRENT_TIMESTAMP,
                supplier.supplied_at = CASE
                  WHEN ? = 'SUPPLIED' THEN COALESCE(supplier.supplied_at, CURRENT_TIMESTAMP)
                  ELSE supplier.supplied_at
                END
            WHERE supplier.status IN (
            """ + allowedStatuses + """
            )
            """,
            shuttleId,
            supplierStatus,
            supplierStatus
        );
    }

    public Optional<TrafficShuttleBookingResponse> bookShuttle(Long requesterCompanyId, Long userId, Long shuttleId, TrafficShuttleBookingPayload payload) {
        Optional<TrafficShuttleServiceResponse> shuttle = getShuttle(requesterCompanyId, shuttleId)
            .filter(row -> List.of("PUBLISHED", "FULL").contains(row.status()));
        if (shuttle.isEmpty()) return Optional.empty();
        TrafficShuttleServiceResponse service = shuttle.get();
        BigDecimal freightFee = Boolean.FALSE.equals(payload.allowShare())
            ? amountOrFallback(service.basePrice(), service.sharedPrice())
            : amountOrFallback(service.sharedPrice(), service.basePrice());
        BigDecimal customsFee = Boolean.TRUE.equals(payload.customsService()) ? amountOrFallback(service.customsPrice(), BigDecimal.ZERO) : BigDecimal.ZERO;
        int craneCount = Boolean.TRUE.equals(payload.craneService()) ? Math.max(payload.craneCount() == null ? 1 : payload.craneCount(), 1) : 0;
        BigDecimal craneFee = Boolean.TRUE.equals(payload.craneService())
            ? amountOrFallback(service.cranePrice(), BigDecimal.ZERO).multiply(BigDecimal.valueOf(craneCount))
            : BigDecimal.ZERO;
        BigDecimal amount = freightFee.add(customsFee).add(craneFee);
        BookingSource source = resolveBookingSource(requesterCompanyId, payload);
        Long orderId = payload.bookingId() == null ? createOrderFromShuttle(requesterCompanyId, userId, service, payload, source) : null;
        if (payload.bookingId() != null) {
            Long existingOrderId = jdbcTemplate.queryForObject(
                "SELECT traffic_service_order_id FROM traffic_shuttle_booking WHERE id = ? AND shuttle_service_id = ? AND requester_company_id = ?",
                Long.class, payload.bookingId(), shuttleId, requesterCompanyId
            );
            if (existingOrderId != null && trafficOrderMatchesSource(requesterCompanyId, existingOrderId, source)) {
                orderId = existingOrderId;
                syncTrafficOrderSource(requesterCompanyId, orderId, source);
            } else {
                orderId = createOrderFromShuttle(requesterCompanyId, userId, service, payload, source);
            }
            if (orderId == null) return Optional.empty();
            updateBookingReservation(requesterCompanyId, payload, source, orderId, shuttleId, freightFee, customsFee, craneFee);
            updateMaterialDemandTrafficBooking(requesterCompanyId, source.demandId(), payload.requestNo(), payload.bookingId(), orderId, freightFee, customsFee, craneFee);
            return booking(payload.bookingId());
        }
        jdbcTemplate.update(
            """
            INSERT INTO traffic_shuttle_booking (
              booking_no, shuttle_service_id, requester_company_id, request_id, purchase_order_id, request_no, node_index, node_name, node_time,
              vessel_name, vessel_imo, anchorage_time, anchorage_position, pallet_count, traffic_service_order_id, passenger_count,
              cargo_summary, cargo_weight_kg, cargo_volume_cbm, amount, allow_share, customs_service, crane_service, crane_count,
              freight_fee, customs_fee, crane_fee, contact_name, contact_phone, remark, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            nextBookingNo(),
            shuttleId,
            requesterCompanyId,
            source.demandId(),
            source.purchaseOrderId(),
            trimToNull(payload.requestNo()),
            payload.nodeIndex(),
            trimToNull(payload.nodeName()),
            trimToNull(payload.nodeTime()),
            trimToNull(payload.vesselName()),
            trimToNull(payload.vesselImo()),
            trimToNull(payload.anchorageTime()),
            trimToNull(payload.anchoragePosition()),
            trimToNull(payload.palletCount()),
            orderId,
            payload.passengerCount(),
            trimToNull(payload.cargoSummary()),
            payload.cargoWeightKg(),
            payload.cargoVolumeCbm(),
            amount,
            Boolean.FALSE.equals(payload.allowShare()) ? 0 : 1,
            Boolean.TRUE.equals(payload.customsService()) ? 1 : 0,
            Boolean.TRUE.equals(payload.craneService()) ? 1 : 0,
            craneCount,
            freightFee,
            customsFee,
            craneFee,
            trimToNull(payload.contactName()),
            trimToNull(payload.contactPhone()),
            trimToNull(payload.remark()),
            userId
        );
        Long bookingId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        updateMaterialDemandTrafficBooking(
            requesterCompanyId, source.demandId(), payload.requestNo(), bookingId, orderId,
            freightFee, customsFee, craneFee
        );
        jdbcTemplate.update(
            """
            UPDATE traffic_shuttle_service
            SET booked_passenger_count = booked_passenger_count + ?,
                booked_cargo_weight_kg = booked_cargo_weight_kg + ?,
                booked_cargo_volume_cbm = booked_cargo_volume_cbm + ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            payload.passengerCount() == null ? 0 : payload.passengerCount(),
            payload.cargoWeightKg() == null ? BigDecimal.ZERO : payload.cargoWeightKg(),
            payload.cargoVolumeCbm() == null ? BigDecimal.ZERO : payload.cargoVolumeCbm(),
            shuttleId
        );
        return booking(bookingId);
    }

    private void updateBookingReservation(Long requesterCompanyId, TrafficShuttleBookingPayload payload, BookingSource source, Long orderId, Long shuttleId, BigDecimal freightFee, BigDecimal customsFee, BigDecimal craneFee) {
        jdbcTemplate.update(
            """
            UPDATE traffic_shuttle_booking
            SET request_id = ?, purchase_order_id = ?, request_no = ?, node_index = ?, node_name = ?, node_time = ?,
                vessel_name = ?, vessel_imo = ?, anchorage_time = ?, anchorage_position = ?, pallet_count = ?,
                traffic_service_order_id = ?, passenger_count = ?, cargo_summary = ?, cargo_weight_kg = ?, cargo_volume_cbm = ?,
                amount = ?, allow_share = ?, customs_service = ?, crane_service = ?, crane_count = ?,
                freight_fee = ?, customs_fee = ?, crane_fee = ?, remark = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND shuttle_service_id = ? AND requester_company_id = ?
            """,
            source.demandId(), source.purchaseOrderId(), trimToNull(payload.requestNo()), payload.nodeIndex(), trimToNull(payload.nodeName()), trimToNull(payload.nodeTime()),
            trimToNull(payload.vesselName()), trimToNull(payload.vesselImo()), trimToNull(payload.anchorageTime()), trimToNull(payload.anchoragePosition()), trimToNull(payload.palletCount()),
            orderId, payload.passengerCount(), trimToNull(payload.cargoSummary()), payload.cargoWeightKg(), payload.cargoVolumeCbm(),
            freightFee.add(customsFee).add(craneFee), Boolean.FALSE.equals(payload.allowShare()) ? 0 : 1,
            Boolean.TRUE.equals(payload.customsService()) ? 1 : 0, Boolean.TRUE.equals(payload.craneService()) ? 1 : 0,
            Boolean.TRUE.equals(payload.craneService()) ? Math.max(payload.craneCount() == null ? 1 : payload.craneCount(), 1) : 0,
            freightFee, customsFee, craneFee, trimToNull(payload.remark()), payload.bookingId(), shuttleId, requesterCompanyId
        );
    }

    public Optional<TrafficShuttleBookingResponse> updateBookingExecution(
        Long supplierCompanyId,
        Long shuttleId,
        Long bookingId,
        TrafficShuttleBookingExecutionPayload payload
    ) {
        int updated = jdbcTemplate.update(
            """
            UPDATE traffic_shuttle_booking booking
            JOIN traffic_shuttle_service shuttle ON shuttle.id = booking.shuttle_service_id
            SET booking.node_index = COALESCE(?, booking.node_index),
                booking.node_name = COALESCE(?, booking.node_name),
                booking.node_time = COALESCE(?, booking.node_time),
                booking.vessel_name = COALESCE(?, booking.vessel_name),
                booking.vessel_imo = COALESCE(?, booking.vessel_imo),
                booking.anchorage_time = COALESCE(?, booking.anchorage_time),
                booking.anchorage_position = COALESCE(?, booking.anchorage_position),
                booking.longitude = COALESCE(?, booking.longitude),
                booking.latitude = COALESCE(?, booking.latitude),
                booking.pallet_count = COALESCE(?, booking.pallet_count),
                booking.passenger_count = COALESCE(?, booking.passenger_count),
                booking.cargo_summary = COALESCE(?, booking.cargo_summary),
                booking.cargo_weight_kg = COALESCE(?, booking.cargo_weight_kg),
                booking.cargo_volume_cbm = COALESCE(?, booking.cargo_volume_cbm),
                booking.allow_share = COALESCE(?, booking.allow_share),
                booking.customs_service = COALESCE(?, booking.customs_service),
                booking.crane_service = COALESCE(?, booking.crane_service),
                booking.crane_count = COALESCE(?, booking.crane_count),
                booking.contact_name = COALESCE(?, booking.contact_name),
                booking.contact_phone = COALESCE(?, booking.contact_phone),
                booking.remark = COALESCE(?, booking.remark),
                booking.updated_at = CURRENT_TIMESTAMP
            WHERE booking.id = ? AND booking.shuttle_service_id = ?
              AND shuttle.supplier_company_id = ?
            """,
            payload.nodeIndex(),
            trimToNull(payload.nodeName()),
            trimToNull(payload.nodeTime()),
            trimToNull(payload.vesselName()),
            trimToNull(payload.vesselImo()),
            trimToNull(payload.anchorageTime()),
            trimToNull(payload.anchoragePosition()),
            payload.longitude(),
            payload.latitude(),
            trimToNull(payload.palletCount()),
            payload.passengerCount(),
            trimToNull(payload.cargoSummary()),
            payload.cargoWeightKg(),
            payload.cargoVolumeCbm(),
            payload.allowShare() == null ? null : Boolean.TRUE.equals(payload.allowShare()) ? 1 : 0,
            payload.customsService() == null ? null : Boolean.TRUE.equals(payload.customsService()) ? 1 : 0,
            payload.craneService() == null ? null : Boolean.TRUE.equals(payload.craneService()) ? 1 : 0,
            payload.craneCount(),
            trimToNull(payload.contactName()),
            trimToNull(payload.contactPhone()),
            trimToNull(payload.remark()),
            bookingId,
            shuttleId,
            supplierCompanyId
        );
        return updated == 0 ? Optional.empty() : booking(bookingId);
    }

    private Long createOrderFromShuttle(
        Long requesterCompanyId,
        Long userId,
        TrafficShuttleServiceResponse shuttle,
        TrafficShuttleBookingPayload payload,
        BookingSource source
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_order (
              service_no, requester_company_id, demand_id, purchase_order_id, supplier_company_id, supplier_company_name, fee_type,
              sea_area, anchorage_code, anchorage_name, use_time, service_type, passenger_type, passenger_count,
              cargo_type, return_trip, allow_share, base_price, shared_price, status, remark,
              traffic_vessel_id, traffic_vessel_name, created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, 'FREIGHT', ?, ?, ?, ?, 'GOODS', 'NORMAL', ?, 'CARGO', 1, ?, ?, ?, 'PENDING_CONFIRM', ?, ?, ?, ?)
            """,
            nextNo(),
            requesterCompanyId,
            source.demandId(),
            source.purchaseOrderId(),
            shuttle.supplierCompanyId(),
            shuttle.supplierCompanyName(),
            shuttle.seaArea(),
            shuttle.anchorageCode(),
            shuttle.anchorageName(),
            trimToNull(shuttle.startTime()),
            payload.passengerCount(),
            Boolean.FALSE.equals(payload.allowShare()) ? 0 : 1,
            shuttle.basePrice(),
            shuttle.sharedPrice(),
            trimToNull(payload.remark()),
            shuttle.trafficVesselId(),
            trimToNull(shuttle.trafficVesselName()),
            userId
        );
        Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (!isBlank(payload.cargoSummary())) {
            replaceCargos(orderId, List.of(new TrafficServiceCargoPayload(payload.cargoSummary(), payload.cargoWeightKg(), payload.cargoVolumeCbm())));
        }
        return orderId;
    }

    private boolean trafficOrderMatchesSource(Long requesterCompanyId, Long orderId, BookingSource source) {
        if (source == null || source.demandId() == null && source.purchaseOrderId() == null) {
            return true;
        }
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM traffic_service_order
            WHERE id = ? AND requester_company_id = ? AND status <> 'DISCARDED'
              AND (
                (? IS NOT NULL AND (purchase_order_id = ? OR purchase_order_id IS NULL))
                OR (? IS NOT NULL AND (demand_id = ? OR demand_id IS NULL))
              )
            """,
            Integer.class,
            orderId,
            requesterCompanyId,
            source.purchaseOrderId(),
            source.purchaseOrderId(),
            source.demandId(),
            source.demandId()
        );
        return count != null && count > 0;
    }

    private void syncTrafficOrderSource(Long requesterCompanyId, Long orderId, BookingSource source) {
        if (source == null || source.demandId() == null && source.purchaseOrderId() == null) {
            return;
        }
        jdbcTemplate.update(
            """
            UPDATE traffic_service_order
            SET demand_id = COALESCE(?, demand_id),
                purchase_order_id = COALESCE(?, purchase_order_id),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND requester_company_id = ?
            """,
            source.demandId(),
            source.purchaseOrderId(),
            orderId,
            requesterCompanyId
        );
    }

    private BookingSource resolveBookingSource(Long companyId, TrafficShuttleBookingPayload payload) {
        if (payload.purchaseOrderId() != null) {
            List<BookingSource> rows = jdbcTemplate.query(
                "SELECT demand_id, id AS purchase_order_id FROM purchase_order WHERE id = ? AND buyer_company_id = ?",
                (rs, rowNum) -> new BookingSource(rs.getLong("demand_id"), rs.getLong("purchase_order_id")),
                payload.purchaseOrderId(),
                companyId
            );
            if (!rows.isEmpty()) return rows.get(0);
        }
        if (payload.demandId() != null) {
            List<BookingSource> rows = jdbcTemplate.query(
                """
                SELECT demand.id AS demand_id,
                       (
                         SELECT purchase.id
                         FROM purchase_order purchase
                         WHERE purchase.demand_id = demand.id
                           AND purchase.buyer_company_id = demand.company_id
                           AND purchase.status <> 'DISCARDED'
                         ORDER BY purchase.id DESC
                         LIMIT 1
                       ) AS purchase_order_id
                FROM material_demand demand
                WHERE demand.id = ? AND demand.company_id = ?
                """,
                (rs, rowNum) -> new BookingSource(rs.getLong("demand_id"), nullableLong(rs, "purchase_order_id")),
                payload.demandId(),
                companyId
            );
            if (!rows.isEmpty()) return rows.get(0);
        }
        return new BookingSource(findMaterialDemandId(companyId, payload.requestNo()).orElse(null), null);
    }

    private Optional<Long> findMaterialDemandId(Long companyId, String requestNo) {
        if (isBlank(requestNo)) {
            return Optional.empty();
        }
        List<Long> rows = jdbcTemplate.query(
            """
            SELECT id
            FROM material_demand
            WHERE company_id = ? AND (inquiry_no = ? OR demand_no = ?)
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("id"),
            companyId,
            trimToNull(requestNo),
            trimToNull(requestNo)
        );
        return rows.stream().findFirst();
    }

    private void updateMaterialDemandTrafficBooking(
        Long companyId,
        Long demandId,
        String requestNo,
        Long bookingId,
        Long trafficServiceOrderId,
        BigDecimal freightFee,
        BigDecimal customsFee,
        BigDecimal craneFee
    ) {
        if (demandId == null && isBlank(requestNo)) {
            return;
        }
        String normalizedRequestNo = trimToNull(requestNo);
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET fixed_freight_fee = ?,
                fixed_customs_fee = ?,
                fixed_crane_fee = ?,
                traffic_service_json = JSON_SET(
                  COALESCE(traffic_service_json, JSON_OBJECT()),
                  '$.supplyMode', 'SEA',
                  '$.fixedProviderType', 'BARGE',
                  '$.bookingId', ?,
                  '$.trafficServiceOrderId', ?
                ),
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ?
              AND (id = ? OR (? IS NULL AND (inquiry_no = ? OR demand_no = ?)))
            """,
            freightFee,
            customsFee,
            craneFee,
            bookingId,
            trafficServiceOrderId,
            companyId,
            demandId,
            demandId,
            normalizedRequestNo,
            normalizedRequestNo
        );
    }

    public List<TrafficBoatPriceResponse> listPrices(Long supplierCompanyId, String keyword, String seaArea) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT price.id, price.supplier_company_id,
                   anchorage.item_code AS anchorage_code,
                   anchorage.item_name AS anchorage_name,
                   anchorage.item_value AS sea_area,
                   price.base_price, price.shared_price, price.enabled, price.remark, price.updated_at
            FROM sys_dictionary_item anchorage
            LEFT JOIN traffic_boat_price price
              ON price.anchorage_code COLLATE utf8mb4_unicode_ci = anchorage.item_code AND price.supplier_company_id = ?
            WHERE anchorage.type_code = 'ANCHORAGE' AND anchorage.enabled = 1
            """
        );
        args.add(supplierCompanyId);
        if (!isBlank(keyword)) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            sql.append(" AND (anchorage.item_name LIKE ? ESCAPE '\\\\' OR anchorage.item_code LIKE ? ESCAPE '\\\\')");
            args.add(pattern);
            args.add(pattern);
        }
        if (!isBlank(seaArea)) {
            sql.append(" AND anchorage.item_value = ?");
            args.add(seaArea.trim().toUpperCase());
        }
        sql.append(" ORDER BY anchorage.sort_order ASC, anchorage.id ASC");
        return jdbcTemplate.query(sql.toString(), this::price, args.toArray());
    }

    public TrafficBoatPriceResponse savePrice(Long supplierCompanyId, TrafficBoatPricePayload payload) {
        String code = normalize(payload.anchorageCode());
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM traffic_boat_price WHERE supplier_company_id = ? AND anchorage_code = ?",
            Integer.class,
            supplierCompanyId,
            code
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(
                """
                UPDATE traffic_boat_price
                SET base_price = ?, shared_price = ?, enabled = ?, remark = ?, updated_at = CURRENT_TIMESTAMP
                WHERE supplier_company_id = ? AND anchorage_code = ?
                """,
                payload.basePrice(),
                payload.sharedPrice(),
                payload.enabled() == null || payload.enabled() ? 1 : 0,
                trimToNull(payload.remark()),
                supplierCompanyId,
                code
            );
        } else {
            jdbcTemplate.update(
                """
                INSERT INTO traffic_boat_price (supplier_company_id, anchorage_code, base_price, shared_price, enabled, remark)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                supplierCompanyId,
                code,
                payload.basePrice(),
                payload.sharedPrice(),
                payload.enabled() == null || payload.enabled() ? 1 : 0,
                trimToNull(payload.remark())
            );
        }
        return listPrices(supplierCompanyId, code, null).stream()
            .filter(row -> code.equals(row.anchorageCode()))
            .findFirst()
            .orElseThrow();
    }

    private void replaceCargos(Long orderId, List<TrafficServiceCargoPayload> cargos) {
        jdbcTemplate.update("DELETE FROM traffic_service_cargo WHERE service_order_id = ?", orderId);
        if (cargos == null) return;
        for (TrafficServiceCargoPayload cargo : cargos) {
            if (cargo == null || isBlank(cargo.cargoName())) continue;
            jdbcTemplate.update(
                "INSERT INTO traffic_service_cargo (service_order_id, cargo_name, weight_kg, volume_cbm) VALUES (?, ?, ?, ?)",
                orderId,
                cargo.cargoName().trim(),
                cargo.weightKg(),
                cargo.volumeCbm()
            );
        }
    }

    private void replaceRequestCargos(Long requestId, List<TrafficServiceCargoPayload> cargos) {
        jdbcTemplate.update("DELETE FROM traffic_service_request_cargo WHERE request_id = ?", requestId);
        if (cargos == null) return;
        for (TrafficServiceCargoPayload cargo : cargos) {
            if (cargo == null || isBlank(cargo.cargoName())) continue;
            jdbcTemplate.update(
                "INSERT INTO traffic_service_request_cargo (request_id, cargo_name, weight_kg, volume_cbm) VALUES (?, ?, ?, ?)",
                requestId,
                cargo.cargoName().trim(),
                cargo.weightKg(),
                cargo.volumeCbm()
            );
        }
    }

    private TrafficAnchorageResponse anchorage(ResultSet rs, int rowNum) throws SQLException {
        return new TrafficAnchorageResponse(
            rs.getLong("id"),
            rs.getString("anchorage_code"),
            rs.getString("anchorage_name"),
            rs.getString("sea_area"),
            rs.getBoolean("enabled"),
            rs.getInt("sort_order")
        );
    }

    private TrafficRoutePlanResponse route(ResultSet rs, int rowNum) throws SQLException {
        return new TrafficRoutePlanResponse(
            rs.getLong("id"),
            rs.getString("route_no"),
            rs.getString("route_name"),
            rs.getDate("service_date") == null ? null : rs.getDate("service_date").toLocalDate().toString(),
            rs.getString("sea_area"),
            nullableLong(rs, "supplier_company_id"),
            rs.getString("supplier_company_name"),
            nullableLong(rs, "traffic_vessel_id"),
            rs.getString("traffic_vessel_name"),
            timestampToString(rs.getTimestamp("planned_departure_time")),
            timestampToString(rs.getTimestamp("planned_finish_time")),
            rs.getBoolean("allow_share"),
            (Integer) rs.getObject("order_count"),
            rs.getBigDecimal("total_income"),
            rs.getBigDecimal("estimated_cost"),
            rs.getBigDecimal("estimated_profit"),
            rs.getString("status"),
            rs.getString("remark"),
            timestampToString(rs.getTimestamp("created_at"))
        );
    }

    private TrafficServiceRequestResponse request(ResultSet rs, boolean includeCargos) throws SQLException {
        Long id = rs.getLong("id");
        return new TrafficServiceRequestResponse(
            id,
            rs.getString("request_no"),
            rs.getLong("requester_company_id"),
            nullableLong(rs, "demand_id"),
            nullableLong(rs, "purchase_order_id"),
            rs.getString("fee_type"),
            rs.getString("sea_area"),
            rs.getString("anchorage_code"),
            rs.getString("anchorage_name"),
            timestampToString(rs.getTimestamp("use_time")),
            rs.getString("service_type"),
            rs.getString("passenger_type"),
            (Integer) rs.getObject("passenger_count"),
            rs.getString("cargo_type"),
            rs.getBoolean("return_trip"),
            rs.getBoolean("allow_share"),
            rs.getString("remark"),
            rs.getString("status"),
            nullableLong(rs, "recommended_quote_id"),
            nullableLong(rs, "selected_quote_id"),
            nullableLong(rs, "selected_supplier_company_id"),
            nullableLong(rs, "traffic_service_order_id"),
            timestampToString(rs.getTimestamp("created_at")),
            includeCargos ? requestCargos(id) : List.of()
        );
    }

    private List<TrafficServiceCargoResponse> requestCargos(Long requestId) {
        return jdbcTemplate.query(
            "SELECT id, cargo_name, weight_kg, volume_cbm FROM traffic_service_request_cargo WHERE request_id = ? ORDER BY id ASC",
            (rs, rowNum) -> new TrafficServiceCargoResponse(
                rs.getLong("id"),
                rs.getString("cargo_name"),
                rs.getBigDecimal("weight_kg"),
                rs.getBigDecimal("volume_cbm")
            ),
            requestId
        );
    }

    private List<TrafficServiceQuoteResponse> quotes(Long requestId) {
        return jdbcTemplate.query(
            """
            SELECT quote.*, request.recommended_quote_id
            FROM traffic_service_quote quote
            JOIN traffic_service_request request ON request.id = quote.request_id
            WHERE quote.request_id = ?
            ORDER BY CASE WHEN quote.status IN ('SUBMITTED', 'UPDATED', 'SELECTED') THEN 0 ELSE 1 END,
                     quote.quote_amount ASC, quote.updated_at DESC, quote.id ASC
            """,
            (rs, rowNum) -> new TrafficServiceQuoteResponse(
                rs.getLong("id"),
                rs.getLong("request_id"),
                rs.getLong("supplier_company_id"),
                rs.getString("supplier_company_name"),
                rs.getBigDecimal("quote_amount"),
                rs.getBigDecimal("base_price"),
                rs.getBigDecimal("shared_price"),
                rs.getString("currency"),
                timestampToString(rs.getTimestamp("available_start_time")),
                timestampToString(rs.getTimestamp("available_return_time")),
                nullableLong(rs, "traffic_vessel_id"),
                rs.getString("traffic_vessel_name"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("message"),
                rs.getString("status"),
                rs.getObject("recommended_quote_id") != null && rs.getLong("recommended_quote_id") == rs.getLong("id"),
                timestampToString(rs.getTimestamp("created_at")),
                timestampToString(rs.getTimestamp("updated_at"))
            ),
            requestId
        );
    }

    private List<TrafficServiceRequestEventResponse> requestEvents(Long requestId) {
        return jdbcTemplate.query(
            """
            SELECT id, request_id, quote_id, event_type, event_message, created_at
            FROM traffic_service_request_event
            WHERE request_id = ?
            ORDER BY created_at DESC, id DESC
            """,
            (rs, rowNum) -> new TrafficServiceRequestEventResponse(
                rs.getLong("id"),
                rs.getLong("request_id"),
                nullableLong(rs, "quote_id"),
                rs.getString("event_type"),
                rs.getString("event_message"),
                timestampToString(rs.getTimestamp("created_at"))
            ),
            requestId
        );
    }

    private TrafficShuttleServiceResponse shuttle(ResultSet rs, int rowNum) throws SQLException {
        return new TrafficShuttleServiceResponse(
            rs.getLong("id"),
            rs.getString("shuttle_no"),
            rs.getLong("supplier_company_id"),
            rs.getString("supplier_company_name"),
            rs.getString("sea_area"),
            rs.getString("anchorage_code"),
            rs.getString("anchorage_name"),
            rs.getString("departure_point"),
            rs.getString("destination_point"),
            timestampToString(rs.getTimestamp("start_time")),
            timestampToString(rs.getTimestamp("return_time")),
            rs.getBigDecimal("base_price"),
            rs.getBigDecimal("shared_price"),
            rs.getBigDecimal("customs_price"),
            rs.getBigDecimal("crane_price"),
            (Integer) rs.getObject("passenger_capacity"),
            rs.getBigDecimal("cargo_capacity_kg"),
            rs.getBigDecimal("cargo_capacity_cbm"),
            (Integer) rs.getObject("booked_passenger_count"),
            rs.getBigDecimal("booked_cargo_weight_kg"),
            rs.getBigDecimal("booked_cargo_volume_cbm"),
            nullableLong(rs, "traffic_vessel_id"),
            rs.getString("traffic_vessel_name"),
            rs.getString("status"),
            rs.getString("remark"),
            deserializeShuttleNodes(rs.getString("service_nodes_json")),
            List.of(),
            timestampToString(rs.getTimestamp("created_at"))
        );
    }

    private List<TrafficShuttleServiceResponse> attachShuttleBookings(Long companyId, List<TrafficShuttleServiceResponse> shuttles) {
        if (shuttles == null || shuttles.isEmpty()) {
            return shuttles == null ? List.of() : shuttles;
        }
        List<Long> ids = shuttles.stream().map(TrafficShuttleServiceResponse::shuttleId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return shuttles;
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>(ids);
        args.add(companyId);
        args.add(companyId);
        Map<Long, List<TrafficShuttleBookingResponse>> bookingsByShuttle = jdbcTemplate.query(
            """
            SELECT *
            FROM traffic_shuttle_booking
            WHERE shuttle_service_id IN (%s)
              AND status <> 'CANCELLED'
              AND (
                requester_company_id = ?
                OR shuttle_service_id IN (
                  SELECT id FROM traffic_shuttle_service WHERE supplier_company_id = ?
                )
              )
            ORDER BY created_at ASC, id ASC
            """.formatted(placeholders),
            this::mapBooking,
            args.toArray()
        ).stream().collect(Collectors.groupingBy(TrafficShuttleBookingResponse::shuttleServiceId));
        return shuttles.stream()
            .map(row -> new TrafficShuttleServiceResponse(
                row.shuttleId(),
                row.shuttleNo(),
                row.supplierCompanyId(),
                row.supplierCompanyName(),
                row.seaArea(),
                row.anchorageCode(),
                row.anchorageName(),
                row.departurePoint(),
                row.destinationPoint(),
                row.startTime(),
                row.returnTime(),
                row.basePrice(),
                row.sharedPrice(),
                row.customsPrice(),
                row.cranePrice(),
                row.passengerCapacity(),
                row.cargoCapacityKg(),
                row.cargoCapacityCbm(),
                row.bookedPassengerCount(),
                row.bookedCargoWeightKg(),
                row.bookedCargoVolumeCbm(),
                row.trafficVesselId(),
                row.trafficVesselName(),
                row.status(),
                row.remark(),
                row.serviceNodes(),
                bookingsByShuttle.getOrDefault(row.shuttleId(), List.of()),
                row.createdAt()
            ))
            .toList();
    }

    private Optional<TrafficShuttleBookingResponse> booking(Long bookingId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM traffic_shuttle_booking
            WHERE id = ?
            """,
            this::mapBooking,
            bookingId
        ).stream().findFirst();
    }

    private TrafficShuttleBookingResponse mapBooking(ResultSet rs, int rowNum) throws SQLException {
        return new TrafficShuttleBookingResponse(
            rs.getLong("id"),
            rs.getString("booking_no"),
            rs.getLong("shuttle_service_id"),
            rs.getLong("requester_company_id"),
            nullableLong(rs, "request_id"),
            rs.getString("request_no"),
            (Integer) rs.getObject("node_index"),
            rs.getString("node_name"),
            rs.getString("node_time"),
            rs.getString("vessel_name"),
            rs.getString("vessel_imo"),
            rs.getString("anchorage_time"),
            rs.getString("anchorage_position"),
            rs.getBigDecimal("longitude"),
            rs.getBigDecimal("latitude"),
            rs.getString("pallet_count"),
            nullableLong(rs, "traffic_service_order_id"),
            (Integer) rs.getObject("passenger_count"),
            rs.getString("cargo_summary"),
            rs.getBigDecimal("cargo_weight_kg"),
            rs.getBigDecimal("cargo_volume_cbm"),
            rs.getBigDecimal("amount"),
            rs.getBoolean("allow_share"),
            rs.getBoolean("customs_service"),
            rs.getBoolean("crane_service"),
            (Integer) rs.getObject("crane_count"),
            rs.getBigDecimal("freight_fee"),
            rs.getBigDecimal("customs_fee"),
            rs.getBigDecimal("crane_fee"),
            rs.getString("contact_name"),
            rs.getString("contact_phone"),
            rs.getString("remark"),
            rs.getString("status"),
            timestampToString(rs.getTimestamp("created_at"))
        );
    }

    private List<TrafficRouteStopResponse> routeStops(Long routeId) {
        return jdbcTemplate.query(
            """
            SELECT stop.*, service_order.service_no, service_order.base_price, service_order.shared_price, service_order.allow_share
            FROM traffic_route_stop stop
            JOIN traffic_service_order service_order ON service_order.id = stop.traffic_service_order_id
            WHERE stop.route_plan_id = ?
            ORDER BY stop.stop_sequence ASC, stop.id ASC
            """,
            (rs, rowNum) -> new TrafficRouteStopResponse(
                rs.getLong("id"),
                rs.getLong("route_plan_id"),
                rs.getLong("traffic_service_order_id"),
                rs.getInt("stop_sequence"),
                rs.getString("service_no"),
                rs.getString("anchorage_code"),
                rs.getString("anchorage_name"),
                timestampToString(rs.getTimestamp("planned_service_time")),
                rs.getString("service_type"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getBoolean("allow_share") ? rs.getBigDecimal("shared_price") : rs.getBigDecimal("base_price"),
                rs.getBoolean("allow_share"),
                rs.getString("status"),
                rs.getString("remark")
            ),
            routeId
        );
    }

    private List<TrafficRouteEventResponse> routeEvents(Long routeId) {
        return jdbcTemplate.query(
            """
            SELECT id, route_plan_id, traffic_service_order_id, event_type, event_message, created_at
            FROM traffic_route_event
            WHERE route_plan_id = ?
            ORDER BY created_at DESC, id DESC
            """,
            (rs, rowNum) -> new TrafficRouteEventResponse(
                rs.getLong("id"),
                rs.getLong("route_plan_id"),
                nullableLong(rs, "traffic_service_order_id"),
                rs.getString("event_type"),
                rs.getString("event_message"),
                timestampToString(rs.getTimestamp("created_at"))
            ),
            routeId
        );
    }

    private TrafficServiceOrderResponse order(ResultSet rs, boolean includeCargos) throws SQLException {
        return order(rs, includeCargos, null, false);
    }

    private TrafficServiceOrderResponse order(
        ResultSet rs,
        boolean includeCargos,
        Long purchaseOrderOverride,
        boolean includeBooking
    ) throws SQLException {
        Long id = rs.getLong("id");
        return new TrafficServiceOrderResponse(
            id,
            rs.getString("service_no"),
            purchaseOrderOverride != null ? purchaseOrderOverride : nullableLong(rs, "purchase_order_id"),
            rs.getObject("supplier_company_id") == null ? null : rs.getLong("supplier_company_id"),
            rs.getString("supplier_company_name"),
            safeString(rs, "fee_type"),
            rs.getString("sea_area"),
            rs.getString("anchorage_code"),
            rs.getString("anchorage_name"),
            timestampToString(rs.getTimestamp("use_time")),
            rs.getString("service_type"),
            rs.getString("passenger_type"),
            (Integer) rs.getObject("passenger_count"),
            rs.getString("cargo_type"),
            rs.getBoolean("return_trip"),
            rs.getBoolean("allow_share"),
            rs.getBigDecimal("base_price"),
            rs.getBigDecimal("shared_price"),
            rs.getString("status"),
            rs.getString("remark"),
            nullableLong(rs, "business_contact_id"),
            rs.getString("business_contact_name"),
            rs.getString("business_contact_phone"),
            timestampToString(rs.getTimestamp("accepted_at")),
            nullableLong(rs, "traffic_vessel_id"),
            rs.getString("traffic_vessel_name"),
            nullableLong(rs, "handler_contact_id"),
            rs.getString("handler_name"),
            rs.getString("handler_phone"),
            rs.getString("supplier_message"),
            timestampToString(rs.getTimestamp("departure_time")),
            timestampToString(rs.getTimestamp("arrival_time")),
            timestampToString(rs.getTimestamp("return_start_time")),
            timestampToString(rs.getTimestamp("return_end_time")),
            rs.getString("sign_photo_url"),
            rs.getString("pickup_photo_url"),
            rs.getString("return_arrival_photo_url"),
            timestampToString(rs.getTimestamp("created_at")),
            timestampToString(rs.getTimestamp("updated_at")),
            includeCargos ? cargos(id) : List.of(),
            includeBooking ? nullableLong(rs, "resolved_booking_id") : null,
            includeBooking ? nullableLong(rs, "resolved_shuttle_service_id") : null,
            includeBooking ? rs.getString("resolved_shuttle_no") : null,
            includeBooking ? rs.getString("resolved_shuttle_departure_point") : null,
            includeBooking ? rs.getString("resolved_shuttle_destination_point") : null,
            includeBooking ? timestampToString(rs.getTimestamp("resolved_shuttle_start_time")) : null,
            includeBooking ? timestampToString(rs.getTimestamp("resolved_shuttle_return_time")) : null,
            includeBooking ? deserializeShuttleNodes(rs.getString("resolved_shuttle_service_nodes_json")) : null,
            includeBooking ? (Integer) rs.getObject("resolved_booking_node_index") : null,
            includeBooking ? rs.getString("resolved_booking_node_name") : null,
            includeBooking ? rs.getString("resolved_booking_node_time") : null,
            includeBooking ? rs.getBigDecimal("resolved_booking_amount") : null,
            includeBooking ? rs.getBigDecimal("resolved_booking_freight_fee") : null,
            includeBooking ? rs.getBigDecimal("resolved_booking_customs_fee") : null,
            includeBooking ? rs.getBigDecimal("resolved_booking_crane_fee") : null
        );
    }

    private record BookingSource(Long demandId, Long purchaseOrderId) { }

    private List<TrafficServiceCargoResponse> cargos(Long orderId) {
        return jdbcTemplate.query(
            "SELECT id, cargo_name, weight_kg, volume_cbm FROM traffic_service_cargo WHERE service_order_id = ? ORDER BY id ASC",
            (rs, rowNum) -> new TrafficServiceCargoResponse(
                rs.getLong("id"),
                rs.getString("cargo_name"),
                rs.getBigDecimal("weight_kg"),
                rs.getBigDecimal("volume_cbm")
            ),
            orderId
        );
    }

    private TrafficBoatPriceResponse price(ResultSet rs, int rowNum) throws SQLException {
        Long priceId = rs.getObject("id") == null ? null : rs.getLong("id");
        Long supplierCompanyId = rs.getObject("supplier_company_id") == null ? null : rs.getLong("supplier_company_id");
        return new TrafficBoatPriceResponse(
            priceId,
            supplierCompanyId,
            rs.getString("anchorage_code"),
            rs.getString("anchorage_name"),
            rs.getString("sea_area"),
            rs.getBigDecimal("base_price"),
            rs.getBigDecimal("shared_price"),
            rs.getObject("enabled") == null ? Boolean.FALSE : rs.getBoolean("enabled"),
            rs.getString("remark"),
            timestampToString(rs.getTimestamp("updated_at"))
        );
    }

    private void addRouteEvent(Long routeId, Long serviceOrderId, String eventType, String message, Long userId) {
        jdbcTemplate.update(
            """
            INSERT INTO traffic_route_event (route_plan_id, traffic_service_order_id, event_type, event_message, created_by)
            VALUES (?, ?, ?, ?, ?)
            """,
            routeId,
            serviceOrderId,
            eventType,
            message,
            userId
        );
    }

    private void addRequestEvent(Long requestId, Long quoteId, String eventType, String message, Long userId) {
        jdbcTemplate.update(
            """
            INSERT INTO traffic_service_request_event (request_id, quote_id, event_type, event_message, created_by)
            VALUES (?, ?, ?, ?, ?)
            """,
            requestId,
            quoteId,
            eventType,
            message,
            userId
        );
    }

    private void updateRequestRecommendedQuote(Long requestId) {
        List<Long> quoteIds = jdbcTemplate.query(
            """
            SELECT id
            FROM traffic_service_quote
            WHERE request_id = ? AND status IN ('SUBMITTED', 'UPDATED')
            ORDER BY quote_amount ASC, updated_at DESC, id ASC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("id"),
            requestId
        );
        jdbcTemplate.update(
            "UPDATE traffic_service_request SET recommended_quote_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            quoteIds.isEmpty() ? null : quoteIds.get(0),
            requestId
        );
    }

    private void recalculateRoute(Long routeId) {
        jdbcTemplate.update(
            """
            UPDATE traffic_route_plan route
            SET order_count = (
                  SELECT COUNT(*) FROM traffic_route_stop stop WHERE stop.route_plan_id = route.id
                ),
                total_income = (
                  SELECT COALESCE(SUM(CASE WHEN service_order.allow_share = 1 THEN service_order.shared_price ELSE service_order.base_price END), 0)
                  FROM traffic_route_stop stop
                  JOIN traffic_service_order service_order ON service_order.id = stop.traffic_service_order_id
                  WHERE stop.route_plan_id = route.id
                ),
                estimated_profit = (
                  SELECT COALESCE(SUM(CASE WHEN service_order.allow_share = 1 THEN service_order.shared_price ELSE service_order.base_price END), 0) - COALESCE(route.estimated_cost, 0)
                  FROM traffic_route_stop stop
                  JOIN traffic_service_order service_order ON service_order.id = stop.traffic_service_order_id
                  WHERE stop.route_plan_id = route.id
                ),
                updated_at = CURRENT_TIMESTAMP
            WHERE route.id = ?
            """,
            routeId
        );
    }

    private TrafficPricePick lowestEnabledPrice(String anchorageCode, boolean allowShare) {
        String amountColumn = allowShare ? "shared_price" : "base_price";
        List<TrafficPricePick> rows = jdbcTemplate.query(
            """
            SELECT price.supplier_company_id, company.company_name AS supplier_company_name,
                   price.base_price, price.shared_price
            FROM traffic_boat_price price
            JOIN company ON company.id = price.supplier_company_id
            WHERE price.anchorage_code = ? AND price.enabled = 1
              AND price.%s IS NOT NULL AND price.%s > 0
            ORDER BY price.%s ASC, company.company_name ASC, price.supplier_company_id ASC
            LIMIT 1
            """.formatted(amountColumn, amountColumn, amountColumn),
            (rs, rowNum) -> new TrafficPricePick(
                rs.getLong("supplier_company_id"),
                rs.getString("supplier_company_name"),
                rs.getBigDecimal("base_price"),
                rs.getBigDecimal("shared_price")
            ),
            anchorageCode
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String anchorageName(String code) {
        List<String> names = jdbcTemplate.query(
            "SELECT item_name FROM sys_dictionary_item WHERE type_code = 'ANCHORAGE' AND item_code = ?",
            (rs, rowNum) -> rs.getString("item_name"),
            normalize(code)
        );
        return names.isEmpty() ? normalize(code) : names.get(0);
    }

    private String nextNo() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM traffic_service_order", Long.class);
        return "TS-" + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", next == null ? 1 : next);
    }

    private String nextRouteNo() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM traffic_route_plan", Long.class);
        return "TR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", next == null ? 1 : next);
    }

    private String nextRequestNo() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM traffic_service_request", Long.class);
        return "TSR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", next == null ? 1 : next);
    }

    private String nextShuttleNo() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM traffic_shuttle_service", Long.class);
        return "SH-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", next == null ? 1 : next);
    }

    private String nextBookingNo() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM traffic_shuttle_booking", Long.class);
        return "SB-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", next == null ? 1 : next);
    }

    private String normalizeShuttleStatus(String status) {
        return "PUBLISHED".equals(normalize(status)) ? "PUBLISHED" : "DRAFT";
    }

    private String serializeShuttleNodes(List<TrafficShuttleNodePayload> nodes) {
        if (nodes == null || nodes.isEmpty()) return null;
        List<TrafficShuttleNodePayload> sanitized = nodes.stream()
            .filter(node -> node != null)
            .map(node -> new TrafficShuttleNodePayload(
                trimToNull(node.nodeName()),
                trimToNull(node.startTime()),
                trimToNull(node.endTime())
            ))
            .filter(node -> node.nodeName() != null || node.startTime() != null || node.endTime() != null)
            .toList();
        if (sanitized.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<TrafficShuttleNodePayload> deserializeShuttleNodes(String value) {
        if (isBlank(value)) return List.of();
        try {
            List<TrafficShuttleNodePayload> nodes = OBJECT_MAPPER.readValue(value, SHUTTLE_NODE_LIST_TYPE);
            if (nodes == null) return List.of();
            return nodes.stream()
                .filter(node -> node != null)
                .map(node -> new TrafficShuttleNodePayload(
                    trimToNull(node.nodeName()),
                    trimToNull(node.startTime()),
                    trimToNull(node.endTime())
                ))
                .filter(node -> node.nodeName() != null || node.startTime() != null || node.endTime() != null)
                .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String companyName(Long companyId) {
        List<String> names = jdbcTemplate.query(
            "SELECT company_name FROM company WHERE id = ?",
            (rs, rowNum) -> rs.getString("company_name"),
            companyId
        );
        return names.isEmpty() ? null : names.get(0);
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : rs.getLong(column);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim().toUpperCase();
    }

    private String defaultFeeType(String value) {
        return isBlank(value) ? "FREIGHT" : value.trim().toUpperCase();
    }

    private BigDecimal amountOrFallback(BigDecimal amount, BigDecimal fallback) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) return amount;
        return fallback == null ? BigDecimal.ZERO : fallback;
    }

    private String safeString(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException ex) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @SuppressWarnings("unchecked")
    private List<TrafficServiceCargoPayload> mapCargos(Map<String, Object> value) {
        Object raw = value.get("cargos");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<TrafficServiceCargoPayload> cargos = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            cargos.add(new TrafficServiceCargoPayload(
                String.valueOf(map.get("cargoName") == null ? "" : map.get("cargoName")),
                mapDecimal(map.get("weightKg")),
                mapDecimal(map.get("volumeCbm"))
            ));
        }
        return cargos;
    }

    private String mapText(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        return raw == null ? null : String.valueOf(raw);
    }

    private Integer mapInt(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw instanceof Number number) return number.intValue();
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean mapBoolean(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw instanceof Boolean bool) return bool;
        return raw != null && "true".equalsIgnoreCase(String.valueOf(raw));
    }

    private java.math.BigDecimal mapDecimal(Object raw) {
        if (raw instanceof java.math.BigDecimal decimal) return decimal;
        if (raw instanceof Number number) return java.math.BigDecimal.valueOf(number.doubleValue());
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        try {
            return new java.math.BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
            Integer.class,
            tableName,
            columnName
        );
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private record TrafficPricePick(
        Long supplierCompanyId,
        String supplierCompanyName,
        java.math.BigDecimal basePrice,
        java.math.BigDecimal sharedPrice
    ) {
    }
}
