package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public PurchaseOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PurchaseOrderSummaryResponse> findActiveByDemandAndStrategy(
        Long buyerCompanyId,
        Long demandId,
        String strategyType
    ) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM purchase_order
            WHERE buyer_company_id = ?
              AND demand_id = ?
              AND strategy_type = ?
              AND status <> 'CANCELLED'
            ORDER BY id DESC
            LIMIT 1
            """,
            (rs, rowNum) -> summary(rs),
            buyerCompanyId,
            demandId,
            strategyType
        ).stream().findFirst();
    }

    public String nextOrderNo(Long buyerCompanyId, LocalDate date) {
        String prefix = "PO-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        List<String> existing = jdbcTemplate.query(
            """
            SELECT order_no
            FROM purchase_order
            WHERE buyer_company_id = ? AND order_no LIKE CONCAT(?, '%')
            ORDER BY order_no DESC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("order_no"),
            buyerCompanyId,
            prefix
        );
        if (existing.isEmpty()) {
            return prefix + "001";
        }
        String last = existing.get(0);
        int next = 1;
        int index = last.lastIndexOf('-');
        if (index >= 0 && index < last.length() - 1) {
            try {
                next = Integer.parseInt(last.substring(index + 1)) + 1;
            } catch (NumberFormatException ignored) {
                next = 1;
            }
        }
        return prefix + String.format("%03d", next);
    }

    public String companyName(Long companyId) {
        List<String> names = jdbcTemplate.query(
            "SELECT company_name FROM company WHERE id = ? LIMIT 1",
            (rs, rowNum) -> rs.getString("company_name"),
            companyId
        );
        return names.isEmpty() ? null : names.get(0);
    }

    public PurchaseOrderDetailResponse insertOrder(PurchaseOrderDraft draft) {
        long orderId = insertMaster(draft);
        Map<String, Long> supplierOrderIds = new LinkedHashMap<>();
        for (PurchaseSupplierOrderDraft supplier : draft.supplierOrders()) {
            long supplierOrderId = insertSupplierOrder(orderId, supplier);
            supplierOrderIds.put(supplier.supplierOrderNo(), supplierOrderId);
            insertEvent(orderId, supplierOrderId, "SUPPLIER_ORDER_CREATED", "Supplier order created", draft.createdBy(), supplier.supplierCompanyId());
        }
        for (PurchaseOrderItemDraft item : draft.items()) {
            Long supplierOrderId = supplierOrderIds.get(item.supplierOrderNo());
            insertItem(orderId, supplierOrderId, item);
        }
        insertEvent(orderId, null, "ORDER_CREATED", "Purchase order created", draft.createdBy(), draft.buyerCompanyId());
        return findBuyerDetail(draft.buyerCompanyId(), orderId).orElseThrow();
    }

    public PurchaseOrderListResponse listBuyer(
        Long buyerCompanyId,
        String keyword,
        String status,
        String supplier,
        String createdFrom,
        String createdTo,
        String deliveryFrom,
        String deliveryTo,
        int page,
        int size
    ) {
        QueryParts query = buyerQuery(buyerCompanyId, keyword, status, supplier, createdFrom, createdTo, deliveryFrom, deliveryTo);
        long total = count("purchase_order po", query);
        String sql = "SELECT po.* FROM purchase_order po " + query.where()
            + " ORDER BY po.created_at DESC, po.id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>(query.args());
        args.add(size);
        args.add((page - 1) * size);
        List<PurchaseOrderSummaryResponse> items = jdbcTemplate.query(sql, (rs, rowNum) -> summary(rs), args.toArray());
        return new PurchaseOrderListResponse(items, page, size, total);
    }

    public Optional<PurchaseOrderDetailResponse> findBuyerDetail(Long buyerCompanyId, Long orderId) {
        Optional<PurchaseOrderSummaryResponse> order = jdbcTemplate.query(
            """
            SELECT *
            FROM purchase_order
            WHERE buyer_company_id = ? AND id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> summary(rs),
            buyerCompanyId,
            orderId
        ).stream().findFirst();
        return order.map(summary -> new PurchaseOrderDetailResponse(
            summary,
            supplierOrders(orderId, null),
            events(orderId)
        ));
    }

    public PurchaseOrderListResponse listSupplier(
        Long supplierCompanyId,
        String keyword,
        String status,
        String createdFrom,
        String createdTo,
        int page,
        int size
    ) {
        QueryParts query = supplierQuery(supplierCompanyId, keyword, status, createdFrom, createdTo);
        long total = count("purchase_order po JOIN purchase_order_supplier pos ON pos.order_id = po.id", query);
        String sql = "SELECT DISTINCT po.* FROM purchase_order po JOIN purchase_order_supplier pos ON pos.order_id = po.id "
            + query.where()
            + " ORDER BY po.created_at DESC, po.id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>(query.args());
        args.add(size);
        args.add((page - 1) * size);
        List<PurchaseOrderSummaryResponse> items = jdbcTemplate.query(sql, (rs, rowNum) -> summary(rs), args.toArray());
        return new PurchaseOrderListResponse(items, page, size, total);
    }

    public Optional<PurchaseOrderDetailResponse> confirmSupplierOrder(
        Long supplierCompanyId,
        Long userId,
        Long orderId,
        Long supplierOrderId,
        PurchaseSupplierConfirmRequest request
    ) {
        Optional<PurchaseSupplierOrderResponse> supplierOrder = supplierOrderForSupplier(orderId, supplierOrderId, supplierCompanyId);
        if (supplierOrder.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal discountAmount = discountAmount(supplierOrder.get().subtotalAmount(), request.discountType(), request.discountValue());
        BigDecimal finalAmount = supplierOrder.get().subtotalAmount().subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier
            SET status = 'PREPARING',
                discount_type = ?,
                discount_value = ?,
                discount_amount = ?,
                final_amount = ?,
                packaging_method = COALESCE(?, packaging_method),
                expected_ready_at = ?,
                supplier_remark = ?,
                reject_reason = NULL,
                confirmed_at = CURRENT_TIMESTAMP,
                rejected_at = NULL
            WHERE id = ? AND order_id = ? AND supplier_company_id = ?
            """,
            request.discountType(),
            request.discountValue(),
            discountAmount,
            finalAmount,
            blankToNull(request.packagingMethod()),
            request.expectedReadyAt(),
            blankToNull(request.supplierRemark()),
            supplierOrderId,
            orderId,
            supplierCompanyId
        );
        insertEvent(orderId, supplierOrderId, "SUPPLIER_CONFIRMED", "Supplier confirmed", userId, supplierCompanyId);
        aggregateOrderStatus(orderId);
        return findSupplierDetail(supplierCompanyId, orderId);
    }

    public Optional<PurchaseOrderDetailResponse> rejectSupplierOrder(
        Long supplierCompanyId,
        Long userId,
        Long orderId,
        Long supplierOrderId,
        PurchaseSupplierRejectRequest request
    ) {
        Optional<PurchaseSupplierOrderResponse> supplierOrder = supplierOrderForSupplier(orderId, supplierOrderId, supplierCompanyId);
        if (supplierOrder.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier
            SET status = 'REJECTED',
                reject_reason = ?,
                rejected_at = CURRENT_TIMESTAMP
            WHERE id = ? AND order_id = ? AND supplier_company_id = ?
            """,
            request.rejectReason().trim(),
            supplierOrderId,
            orderId,
            supplierCompanyId
        );
        insertEvent(orderId, supplierOrderId, "SUPPLIER_REJECTED", "Supplier rejected", userId, supplierCompanyId);
        aggregateOrderStatus(orderId);
        return findSupplierDetail(supplierCompanyId, orderId);
    }

    private Optional<PurchaseOrderDetailResponse> findSupplierDetail(Long supplierCompanyId, Long orderId) {
        Optional<PurchaseOrderSummaryResponse> order = jdbcTemplate.query(
            """
            SELECT po.*
            FROM purchase_order po
            WHERE po.id = ?
              AND EXISTS (
                SELECT 1
                FROM purchase_order_supplier pos
                WHERE pos.order_id = po.id AND pos.supplier_company_id = ?
              )
            LIMIT 1
            """,
            (rs, rowNum) -> summary(rs),
            orderId,
            supplierCompanyId
        ).stream().findFirst();
        return order.map(summary -> new PurchaseOrderDetailResponse(
            summary,
            supplierOrders(orderId, supplierCompanyId),
            events(orderId)
        ));
    }

    private long insertMaster(PurchaseOrderDraft draft) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO purchase_order
                  (order_no, demand_id, demand_no, application_no, buyer_company_id, buyer_company_name,
                   vessel_name, supply_port, vessel_eta, required_delivery_time, strategy_type,
                   strategy_name, supplier_count, item_count, total_amount, currency, status, buyer_remark, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, draft.orderNo());
            statement.setLong(2, draft.demandId());
            statement.setString(3, draft.demandNo());
            statement.setString(4, draft.applicationNo());
            statement.setLong(5, draft.buyerCompanyId());
            statement.setString(6, draft.buyerCompanyName());
            statement.setString(7, draft.vesselName());
            statement.setString(8, draft.supplyPort());
            statement.setString(9, draft.vesselEta());
            statement.setString(10, draft.requiredDeliveryTime());
            statement.setString(11, draft.strategyType());
            statement.setString(12, draft.strategyName());
            statement.setInt(13, draft.supplierCount());
            statement.setInt(14, draft.itemCount());
            statement.setBigDecimal(15, draft.totalAmount());
            statement.setString(16, draft.currency());
            statement.setString(17, draft.status());
            statement.setString(18, draft.buyerRemark());
            statement.setLong(19, draft.createdBy());
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private long insertSupplierOrder(long orderId, PurchaseSupplierOrderDraft supplier) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO purchase_order_supplier
                  (supplier_order_no, order_id, supplier_company_id, supplier_name, status, item_count,
                   subtotal_amount, final_amount, currency, packaging_method)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, supplier.supplierOrderNo());
            statement.setLong(2, orderId);
            statement.setLong(3, supplier.supplierCompanyId());
            statement.setString(4, supplier.supplierName());
            statement.setString(5, supplier.status());
            statement.setInt(6, supplier.itemCount());
            statement.setBigDecimal(7, supplier.subtotalAmount());
            statement.setBigDecimal(8, supplier.finalAmount());
            statement.setString(9, supplier.currency());
            statement.setString(10, supplier.packagingMethod());
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertItem(long orderId, Long supplierOrderId, PurchaseOrderItemDraft item) {
        jdbcTemplate.update(
            """
            INSERT INTO purchase_order_item
              (supplier_order_id, order_id, demand_item_id, sku_id, supplier_sku_code, platform_code,
               impa_code, product_name, specification, quantity, unit, pricing_quantity, unit_price,
               amount, currency, unit_mismatch_flag, quantity_fallback_flag, source_match_type, source_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            supplierOrderId,
            orderId,
            item.demandItemId(),
            item.skuId(),
            item.supplierSkuCode(),
            item.platformCode(),
            item.impaCode(),
            item.productName(),
            item.specification(),
            item.quantity(),
            item.unit(),
            item.pricingQuantity(),
            item.unitPrice(),
            item.amount(),
            item.currency(),
            item.unitMismatchFlag() ? 1 : 0,
            item.quantityFallbackFlag() ? 1 : 0,
            item.sourceMatchType(),
            item.sourceReason()
        );
    }

    private void insertEvent(
        Long orderId,
        Long supplierOrderId,
        String eventType,
        String eventMessage,
        Long operatorUserId,
        Long operatorCompanyId
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO purchase_order_event
              (order_id, supplier_order_id, event_type, event_message, operator_user_id, operator_company_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            orderId,
            supplierOrderId,
            eventType,
            eventMessage,
            operatorUserId,
            operatorCompanyId
        );
    }

    private void aggregateOrderStatus(Long orderId) {
        List<String> statuses = jdbcTemplate.query(
            "SELECT status FROM purchase_order_supplier WHERE order_id = ?",
            (rs, rowNum) -> rs.getString("status"),
            orderId
        );
        String status = aggregateStatus(statuses);
        BigDecimal total = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(final_amount), 0) FROM purchase_order_supplier WHERE order_id = ?",
            BigDecimal.class,
            orderId
        );
        jdbcTemplate.update(
            "UPDATE purchase_order SET status = ?, total_amount = ? WHERE id = ?",
            status,
            total == null ? BigDecimal.ZERO : total,
            orderId
        );
    }

    private String aggregateStatus(List<String> statuses) {
        if (statuses.isEmpty()) {
            return PurchaseOrderService.PENDING_SUPPLIER_CONFIRM;
        }
        boolean allPreparing = statuses.stream().allMatch(PurchaseOrderService.PREPARING::equals);
        boolean allRejected = statuses.stream().allMatch(PurchaseOrderService.REJECTED::equals);
        boolean anyPreparing = statuses.stream().anyMatch(PurchaseOrderService.PREPARING::equals);
        boolean anyRejected = statuses.stream().anyMatch(PurchaseOrderService.REJECTED::equals);
        if (allPreparing) {
            return PurchaseOrderService.PREPARING;
        }
        if (allRejected) {
            return PurchaseOrderService.REJECTED;
        }
        if (anyRejected) {
            return "PARTIALLY_REJECTED";
        }
        if (anyPreparing) {
            return "PARTIALLY_CONFIRMED";
        }
        return PurchaseOrderService.PENDING_SUPPLIER_CONFIRM;
    }

    private List<PurchaseSupplierOrderResponse> supplierOrders(Long orderId, Long supplierCompanyId) {
        String sql = """
            SELECT *
            FROM purchase_order_supplier
            WHERE order_id = ?
            """ + (supplierCompanyId == null ? "" : " AND supplier_company_id = ?")
            + " ORDER BY id ASC";
        Object[] args = supplierCompanyId == null ? new Object[] {orderId} : new Object[] {orderId, supplierCompanyId};
        return jdbcTemplate.query(sql, (rs, rowNum) -> supplierOrder(rs, supplierCompanyId), args);
    }

    private PurchaseSupplierOrderResponse supplierOrder(ResultSet rs, Long supplierCompanyId) throws SQLException {
        Long supplierOrderId = rs.getLong("id");
        Long orderId = rs.getLong("order_id");
        return new PurchaseSupplierOrderResponse(
            supplierOrderId,
            rs.getString("supplier_order_no"),
            orderId,
            rs.getLong("supplier_company_id"),
            rs.getString("supplier_name"),
            rs.getString("status"),
            rs.getInt("item_count"),
            rs.getBigDecimal("subtotal_amount"),
            rs.getString("discount_type"),
            rs.getBigDecimal("discount_value"),
            rs.getBigDecimal("discount_amount"),
            rs.getBigDecimal("final_amount"),
            rs.getString("currency"),
            rs.getString("packaging_method"),
            rs.getString("expected_ready_at"),
            rs.getString("supplier_remark"),
            rs.getString("reject_reason"),
            timestampToString(rs.getTimestamp("confirmed_at")),
            timestampToString(rs.getTimestamp("rejected_at")),
            items(orderId, supplierOrderId)
        );
    }

    private Optional<PurchaseSupplierOrderResponse> supplierOrderForSupplier(Long orderId, Long supplierOrderId, Long supplierCompanyId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM purchase_order_supplier
            WHERE id = ? AND order_id = ? AND supplier_company_id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> supplierOrder(rs, supplierCompanyId),
            supplierOrderId,
            orderId,
            supplierCompanyId
        ).stream().findFirst();
    }

    private List<PurchaseOrderItemResponse> items(Long orderId, Long supplierOrderId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM purchase_order_item
            WHERE order_id = ? AND supplier_order_id = ?
            ORDER BY id ASC
            """,
            (rs, rowNum) -> item(rs),
            orderId,
            supplierOrderId
        );
    }

    private List<PurchaseOrderEventResponse> events(Long orderId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM purchase_order_event
            WHERE order_id = ?
            ORDER BY created_at ASC, id ASC
            """,
            (rs, rowNum) -> event(rs),
            orderId
        );
    }

    private QueryParts buyerQuery(
        Long buyerCompanyId,
        String keyword,
        String status,
        String supplier,
        String createdFrom,
        String createdTo,
        String deliveryFrom,
        String deliveryTo
    ) {
        StringBuilder where = new StringBuilder(" WHERE po.buyer_company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(buyerCompanyId);
        if (status != null) {
            where.append(" AND po.status = ?");
            args.add(status);
        }
        if (keyword != null) {
            where.append("""
                 AND (
                   po.order_no LIKE CONCAT('%', ?, '%')
                   OR po.demand_no LIKE CONCAT('%', ?, '%')
                   OR po.application_no LIKE CONCAT('%', ?, '%')
                   OR po.vessel_name LIKE CONCAT('%', ?, '%')
                 )
                """);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        if (supplier != null) {
            where.append("""
                 AND EXISTS (
                   SELECT 1 FROM purchase_order_supplier pos2
                   WHERE pos2.order_id = po.id AND pos2.supplier_name LIKE CONCAT('%', ?, '%')
                 )
                """);
            args.add(supplier);
        }
        addRange(where, args, "DATE(po.created_at)", createdFrom, createdTo);
        addRange(where, args, "po.required_delivery_time", deliveryFrom, deliveryTo);
        return new QueryParts(where.toString(), args);
    }

    private QueryParts supplierQuery(
        Long supplierCompanyId,
        String keyword,
        String status,
        String createdFrom,
        String createdTo
    ) {
        StringBuilder where = new StringBuilder(" WHERE pos.supplier_company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(supplierCompanyId);
        if (status != null) {
            where.append(" AND pos.status = ?");
            args.add(status);
        }
        if (keyword != null) {
            where.append("""
                 AND (
                   po.order_no LIKE CONCAT('%', ?, '%')
                   OR po.demand_no LIKE CONCAT('%', ?, '%')
                   OR po.application_no LIKE CONCAT('%', ?, '%')
                   OR po.vessel_name LIKE CONCAT('%', ?, '%')
                 )
                """);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        addRange(where, args, "DATE(po.created_at)", createdFrom, createdTo);
        return new QueryParts(where.toString(), args);
    }

    private void addRange(StringBuilder where, List<Object> args, String column, String from, String to) {
        if (from != null) {
            where.append(" AND ").append(column).append(" >= ?");
            args.add(from);
        }
        if (to != null) {
            where.append(" AND ").append(column).append(" <= ?");
            args.add(to);
        }
    }

    private long count(String from, QueryParts query) {
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT po.id) FROM " + from + " " + query.where(),
            Long.class,
            query.args().toArray()
        );
        return total == null ? 0 : total;
    }

    private PurchaseOrderSummaryResponse summary(ResultSet rs) throws SQLException {
        return new PurchaseOrderSummaryResponse(
            rs.getLong("id"),
            rs.getString("order_no"),
            rs.getLong("demand_id"),
            rs.getString("demand_no"),
            rs.getString("application_no"),
            rs.getLong("buyer_company_id"),
            rs.getString("buyer_company_name"),
            rs.getString("vessel_name"),
            rs.getString("supply_port"),
            rs.getString("vessel_eta"),
            rs.getString("required_delivery_time"),
            rs.getString("strategy_type"),
            rs.getString("strategy_name"),
            rs.getInt("supplier_count"),
            rs.getInt("item_count"),
            rs.getBigDecimal("total_amount"),
            rs.getString("currency"),
            rs.getString("status"),
            rs.getString("buyer_remark"),
            timestampToString(rs.getTimestamp("created_at")),
            timestampToString(rs.getTimestamp("updated_at"))
        );
    }

    private PurchaseOrderItemResponse item(ResultSet rs) throws SQLException {
        return new PurchaseOrderItemResponse(
            rs.getLong("id"),
            rs.getLong("supplier_order_id"),
            rs.getLong("order_id"),
            rs.getLong("demand_item_id"),
            rs.getLong("sku_id"),
            rs.getString("supplier_sku_code"),
            rs.getString("platform_code"),
            rs.getString("impa_code"),
            rs.getString("product_name"),
            rs.getString("specification"),
            rs.getString("quantity"),
            rs.getString("unit"),
            rs.getBigDecimal("pricing_quantity"),
            rs.getBigDecimal("unit_price"),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getBoolean("unit_mismatch_flag"),
            rs.getBoolean("quantity_fallback_flag"),
            rs.getString("source_match_type"),
            rs.getString("source_reason")
        );
    }

    private PurchaseOrderEventResponse event(ResultSet rs) throws SQLException {
        return new PurchaseOrderEventResponse(
            rs.getLong("id"),
            rs.getLong("order_id"),
            nullableLong(rs, "supplier_order_id"),
            rs.getString("event_type"),
            rs.getString("event_message"),
            nullableLong(rs, "operator_user_id"),
            nullableLong(rs, "operator_company_id"),
            timestampToString(rs.getTimestamp("created_at"))
        );
    }

    private BigDecimal discountAmount(BigDecimal subtotal, String discountType, BigDecimal discountValue) {
        if (subtotal == null || discountType == null || discountValue == null) {
            return BigDecimal.ZERO;
        }
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return subtotal.multiply(discountValue).divide(new BigDecimal("100"));
        }
        if ("AMOUNT".equalsIgnoreCase(discountType)) {
            return discountValue;
        }
        return BigDecimal.ZERO;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record QueryParts(String where, List<Object> args) {
    }
}
