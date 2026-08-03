package com.zswy.shipsupply.procurement.food.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.AttachmentPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonSettings;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrder;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrderSummary;
import com.zswy.shipsupply.procurement.food.domain.FoodOrderStatus;

@Repository
public class FoodOrderRepository {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FoodOrderRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<OrderableQuoteItem> orderableItems(long demandId) {
        return jdbcTemplate.query(
            """
            SELECT qi.id quote_item_id, qi.demand_item_id, q.id quote_id, q.supplier_company_id,
                   supplier.company_name supplier_name, di.name_en, di.name_zh, di.specification, di.unit,
                   di.requested_quantity,
                   COALESCE(override_price.quoted_quantity, qi.quoted_quantity) quoted_quantity,
                   COALESCE(override_price.unit_price, qi.unit_price) unit_price,
                   ROUND(COALESCE(override_price.quoted_quantity, qi.quoted_quantity)
                     * COALESCE(override_price.unit_price, qi.unit_price), 4) amount
            FROM food_supplier_quote_item qi
            JOIN food_supplier_quote q ON q.id = qi.quote_id AND q.status = 'SUBMITTED'
            JOIN food_demand_item di ON di.id = qi.demand_item_id
            JOIN company supplier ON supplier.id = q.supplier_company_id
            LEFT JOIN food_comparison_quote_override override_price
              ON override_price.demand_id = q.demand_id AND override_price.quote_item_id = qi.id
            WHERE q.demand_id = ? AND qi.availability IN ('AVAILABLE', 'PARTIAL')
              AND qi.match_status = 'MATCHED'
              AND COALESCE(override_price.unit_price, qi.unit_price) IS NOT NULL
              AND COALESCE(override_price.quoted_quantity, qi.quoted_quantity) IS NOT NULL
              AND COALESCE(override_price.quoted_quantity, qi.quoted_quantity) > 0
            ORDER BY qi.demand_item_id, COALESCE(override_price.unit_price, qi.unit_price), q.supplier_company_id
            """,
            (rs, rowNum) -> new OrderableQuoteItem(
                rs.getLong("quote_item_id"),
                rs.getLong("demand_item_id"),
                rs.getLong("quote_id"),
                rs.getLong("supplier_company_id"),
                rs.getString("supplier_name"),
                rs.getString("name_en"),
                rs.getString("name_zh"),
                rs.getString("specification"),
                rs.getString("unit"),
                rs.getBigDecimal("requested_quantity"),
                rs.getBigDecimal("quoted_quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("amount"),
                null,
                null
            ),
            demandId
        );
    }

    public boolean orderExistsForDemand(long demandId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_purchase_order WHERE demand_id = ? AND status <> 'CANCELLED'",
            Integer.class,
            demandId
        );
        return count != null && count > 0;
    }

    public long insertOrder(
        long demandId, long buyerCompanyId, long userId, String strategyType, String currency,
        BigDecimal totalAmount, BigDecimal costAmount, BigDecimal quotedAmount, BigDecimal profitAmount,
        ComparisonSettings settings
    ) {
        String orderNo = nextOrderNo(buyerCompanyId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO food_purchase_order
                  (demand_id, buyer_company_id, order_no, strategy_type, currency, status, total_amount,
                   cost_amount, quoted_amount, profit_amount, quote_markup_percent, fixed_freight_fee,
                   fixed_customs_fee, fixed_crane_fee, fixed_other_fee, supply_mode, fixed_provider_type,
                   fixed_provider_id, fixed_provider_name, traffic_service_json, created_by)
                VALUES (?, ?, ?, ?, ?, 'PENDING_CONFIRMATION', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, demandId);
            statement.setLong(2, buyerCompanyId);
            statement.setString(3, orderNo);
            statement.setString(4, strategyType);
            statement.setString(5, currency);
            statement.setBigDecimal(6, totalAmount);
            statement.setBigDecimal(7, costAmount);
            statement.setBigDecimal(8, quotedAmount);
            statement.setBigDecimal(9, profitAmount);
            statement.setBigDecimal(10, settings.markupPercent());
            statement.setBigDecimal(11, settings.fixedFreightFee());
            statement.setBigDecimal(12, settings.fixedCustomsFee());
            statement.setBigDecimal(13, settings.fixedCraneFee());
            statement.setBigDecimal(14, settings.fixedOtherFee());
            statement.setString(15, settings.supplyMode());
            statement.setString(16, settings.fixedProviderType());
            statement.setString(17, settings.fixedProviderId());
            statement.setString(18, settings.fixedProviderName());
            statement.setString(19, settings.trafficServiceJson());
            statement.setLong(20, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateOrderDeliveryInfo(long orderId, OrderCreateRequest request) {
        jdbcTemplate.update(
            """
            UPDATE food_purchase_order
            SET required_delivery_time = ?, delivery_address = ?, delivery_contact_name = ?, delivery_contact_phone = ?,
                delivery_contact_email = ?, default_packaging_method = ?, buyer_remark = ?
            WHERE id = ?
            """,
            request.requiredDeliveryTime(), blankToNull(request.deliveryAddress()), blankToNull(request.deliveryContactName()),
            blankToNull(request.deliveryContactPhone()), blankToNull(request.deliveryContactEmail()),
            blankToNull(request.defaultPackagingMethod()), blankToNull(request.buyerRemark()), orderId
        );
    }

    public Map<Long, Long> insertSupplierOrders(long orderId, List<OrderableQuoteItem> selected) {
        Map<Long, BigDecimal> subtotals = new LinkedHashMap<>();
        for (OrderableQuoteItem item : selected) {
            subtotals.merge(item.supplierCompanyId(), item.amount(), BigDecimal::add);
        }
        Map<Long, Long> supplierOrderIds = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : subtotals.entrySet()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO food_purchase_order_supplier (order_id, supplier_company_id, status, subtotal_amount) VALUES (?, ?, 'PENDING_CONFIRMATION', ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, orderId);
                statement.setLong(2, entry.getKey());
                statement.setBigDecimal(3, entry.getValue());
                return statement;
            }, keyHolder);
            supplierOrderIds.put(entry.getKey(), keyHolder.getKey().longValue());
        }
        return supplierOrderIds;
    }

    public void insertOrderItems(long orderId, Map<Long, Long> supplierOrderIds, List<OrderableQuoteItem> selected) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO food_purchase_order_item
              (order_id, supplier_order_id, demand_item_id, quote_item_id, name_en, name_zh,
               specification, unit, requested_quantity, ordered_quantity, unit_price, quoted_unit_price,
               amount, quoted_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            selected,
            Math.min(selected.size(), 500),
            (statement, item) -> {
                statement.setLong(1, orderId);
                statement.setLong(2, supplierOrderIds.get(item.supplierCompanyId()));
                statement.setLong(3, item.demandItemId());
                statement.setLong(4, item.quoteItemId());
                statement.setString(5, item.nameEn());
                statement.setString(6, item.nameZh());
                statement.setString(7, item.specification());
                statement.setString(8, item.unit());
                statement.setBigDecimal(9, item.requestedQuantity());
                statement.setBigDecimal(10, item.quotedQuantity());
                statement.setBigDecimal(11, item.unitPrice());
                statement.setBigDecimal(12, item.quotedUnitPrice());
                statement.setBigDecimal(13, item.amount());
                statement.setBigDecimal(14, item.quotedAmount());
            }
        );
    }

    public void finalizeDemandAfterOrder(long demandId, long userId) {
        int updated = jdbcTemplate.update(
            "UPDATE food_demand SET status = 'ORDERED', updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            userId,
            demandId
        );
    }

    public List<OrderSummary> listBuyerOrders(long buyerCompanyId, String keyword, String status) {
        return listOrders("o.buyer_company_id", buyerCompanyId, keyword, status);
    }

    public List<SupplierOrderSummary> listSupplierOrders(long supplierCompanyId, String keyword, String status) {
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String like = "%" + safe(keyword).toLowerCase(Locale.ROOT) + "%";
        return jdbcTemplate.query(
            """
            SELECT o.id order_id, o.order_no, os.id supplier_order_id, os.supplier_company_id,
                   supplier.company_name supplier_name, d.demand_no, d.vessel_name, d.supply_port,
                   d.vessel_eta, o.currency, os.status, os.subtotal_amount total_amount,
                   1 supplier_count, os.expected_ready_at, os.updated_at,
                   (SELECT COUNT(*) FROM food_purchase_order_item oi WHERE oi.supplier_order_id = os.id) item_count
            FROM food_purchase_order_supplier os
            JOIN food_purchase_order o ON o.id = os.order_id
            JOIN food_demand d ON d.id = o.demand_id
            JOIN company supplier ON supplier.id = os.supplier_company_id
            WHERE os.supplier_company_id = ?
              AND (? = '' OR LOWER(CONCAT_WS(' ', o.order_no, d.demand_no, d.vessel_name, d.supply_port, supplier.company_name)) LIKE ?)
              AND (? = '' OR os.status = ?)
            ORDER BY os.updated_at DESC, os.id DESC
            """,
            (rs, rowNum) -> supplierOrderSummary(rs),
            supplierCompanyId,
            safe(keyword),
            like,
            normalizedStatus,
            normalizedStatus
        );
    }

    public List<SupplierOrderSummary> listAllSupplierOrders(String keyword, String status) {
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String like = "%" + safe(keyword).toLowerCase(Locale.ROOT) + "%";
        return jdbcTemplate.query(
            """
            SELECT o.id order_id, o.order_no, os.id supplier_order_id, os.supplier_company_id,
                   supplier.company_name supplier_name, d.demand_no, d.vessel_name, d.supply_port,
                   d.vessel_eta, o.currency, os.status, os.subtotal_amount total_amount,
                   1 supplier_count, os.expected_ready_at, os.updated_at,
                   (SELECT COUNT(*) FROM food_purchase_order_item oi WHERE oi.supplier_order_id = os.id) item_count
            FROM food_purchase_order_supplier os
            JOIN food_purchase_order o ON o.id = os.order_id
            JOIN food_demand d ON d.id = o.demand_id
            JOIN company supplier ON supplier.id = os.supplier_company_id
            WHERE (? = '' OR LOWER(CONCAT_WS(' ', o.order_no, d.demand_no, d.vessel_name, d.supply_port, supplier.company_name)) LIKE ?)
              AND (? = '' OR os.status = ?)
            ORDER BY os.updated_at DESC, os.id DESC
            """,
            (rs, rowNum) -> supplierOrderSummary(rs),
            safe(keyword),
            like,
            normalizedStatus,
            normalizedStatus
        );
    }

    public OrderDetail getOrder(long orderId, long companyId) {
        OrderSummary summary = jdbcTemplate.query(
            """
            SELECT o.*, d.demand_no, d.vessel_name, d.supply_port, d.vessel_eta,
                   (SELECT COUNT(*) FROM food_purchase_order_supplier os WHERE os.order_id = o.id) supplier_count,
                   (SELECT COUNT(*) FROM food_purchase_order_item oi WHERE oi.order_id = o.id) item_count
            FROM food_purchase_order o
            JOIN food_demand d ON d.id = o.demand_id
            WHERE o.id = ? AND (o.buyer_company_id = ? OR EXISTS (
              SELECT 1 FROM food_purchase_order_supplier os WHERE os.order_id = o.id AND os.supplier_company_id = ?
            ))
            """,
            (rs, rowNum) -> orderSummary(rs),
            orderId,
            companyId,
            companyId
        ).stream().findFirst().orElse(null);
        if (summary == null) {
            return null;
        }
        return new OrderDetail(summary, supplierOrders(orderId, companyId), orderItems(orderId, companyId));
    }

    public OrderDetail getOrderForSupplierManagement(long orderId) {
        OrderSummary summary = jdbcTemplate.query(
            """
            SELECT o.*, d.demand_no, d.vessel_name, d.supply_port, d.vessel_eta,
                   (SELECT COUNT(*) FROM food_purchase_order_supplier os WHERE os.order_id = o.id) supplier_count,
                   (SELECT COUNT(*) FROM food_purchase_order_item oi WHERE oi.order_id = o.id) item_count
            FROM food_purchase_order o
            JOIN food_demand d ON d.id = o.demand_id
            WHERE o.id = ?
            """,
            (rs, rowNum) -> orderSummary(rs),
            orderId
        ).stream().findFirst().orElse(null);
        if (summary == null) return null;
        return new OrderDetail(summary, supplierOrdersForManagement(orderId), orderItemsForManagement(orderId));
    }

    public SupplierOrder findSupplierOrder(long orderId, long supplierOrderId, long supplierCompanyId) {
        return jdbcTemplate.query(
            """
            SELECT os.*, c.company_name supplier_name
            FROM food_purchase_order_supplier os
            JOIN company c ON c.id = os.supplier_company_id
            WHERE os.id = ? AND os.order_id = ? AND os.supplier_company_id = ?
            """,
            (rs, rowNum) -> supplierOrder(rs),
            supplierOrderId,
            orderId,
            supplierCompanyId
        ).stream().findFirst().orElse(null);
    }

    public SupplierOrder findSupplierOrderForManagement(long orderId, long supplierOrderId) {
        return jdbcTemplate.query(
            """
            SELECT os.*, c.company_name supplier_name
            FROM food_purchase_order_supplier os
            JOIN company c ON c.id = os.supplier_company_id
            WHERE os.id = ? AND os.order_id = ?
            """,
            (rs, rowNum) -> supplierOrder(rs),
            supplierOrderId,
            orderId
        ).stream().findFirst().orElse(null);
    }

    public void updateSupplierOrderStatus(
        long orderId,
        long supplierOrderId,
        long supplierCompanyId,
        long userId,
        String currentStatus,
        String targetStatus,
        LocalDateTime expectedReadyAt,
        String rejectReason,
        String shipmentRemark
    ) {
        String timestampColumn = switch (targetStatus) {
            case FoodOrderStatus.CONFIRMED, FoodOrderStatus.PREPARING -> "confirmed_at";
            case FoodOrderStatus.READY_TO_SHIP -> "ready_at";
            case FoodOrderStatus.IN_TRANSIT -> "shipped_at";
            case FoodOrderStatus.SUPPLIED -> "completed_at";
            default -> null;
        };
        String timestampSql = timestampColumn == null ? "" : ", " + timestampColumn + " = CURRENT_TIMESTAMP";
        jdbcTemplate.update(
            """
            UPDATE food_purchase_order_supplier
            SET status = ?, expected_ready_at = COALESCE(?, expected_ready_at),
                reject_reason = COALESCE(?, reject_reason), shipment_remark = COALESCE(?, shipment_remark),
                updated_at = CURRENT_TIMESTAMP
            """ + timestampSql + " WHERE id = ? AND order_id = ? AND supplier_company_id = ?",
            targetStatus,
            expectedReadyAt == null ? null : Timestamp.valueOf(expectedReadyAt),
            blankToNull(rejectReason),
            blankToNull(shipmentRemark),
            supplierOrderId,
            orderId,
            supplierCompanyId
        );
        jdbcTemplate.update(
            """
            INSERT INTO food_purchase_order_event
              (order_id, supplier_order_id, event_type, from_status, to_status, remark, operator_user_id)
            VALUES (?, ?, 'STATUS_CHANGE', ?, ?, ?, ?)
            """,
            orderId,
            supplierOrderId,
            currentStatus,
            targetStatus,
            firstText(rejectReason, shipmentRemark),
            userId
        );
        aggregateOrder(orderId);
        if (FoodOrderStatus.SUPPLIED.equals(targetStatus)) {
            ensureSettlementAndEvaluation(orderId, supplierOrderId);
        }
    }

    public void updateSupplierOrderStatusForManagement(
        long orderId,
        long supplierOrderId,
        long userId,
        String currentStatus,
        String targetStatus,
        LocalDateTime expectedReadyAt,
        String rejectReason,
        String shipmentRemark
    ) {
        updateSupplierOrderStatusWithoutCompanyScope(
            orderId, supplierOrderId, userId, currentStatus, targetStatus,
            expectedReadyAt, rejectReason, shipmentRemark
        );
    }

    private void updateSupplierOrderStatusWithoutCompanyScope(
        long orderId,
        long supplierOrderId,
        long userId,
        String currentStatus,
        String targetStatus,
        LocalDateTime expectedReadyAt,
        String rejectReason,
        String shipmentRemark
    ) {
        String timestampColumn = switch (targetStatus) {
            case FoodOrderStatus.CONFIRMED, FoodOrderStatus.PREPARING -> "confirmed_at";
            case FoodOrderStatus.READY_TO_SHIP -> "ready_at";
            case FoodOrderStatus.IN_TRANSIT -> "shipped_at";
            case FoodOrderStatus.SUPPLIED -> "completed_at";
            default -> null;
        };
        String timestampSql = timestampColumn == null ? "" : ", " + timestampColumn + " = CURRENT_TIMESTAMP";
        int updated = jdbcTemplate.update(
            """
            UPDATE food_purchase_order_supplier
            SET status = ?, expected_ready_at = COALESCE(?, expected_ready_at),
                reject_reason = COALESCE(?, reject_reason), shipment_remark = COALESCE(?, shipment_remark),
                updated_at = CURRENT_TIMESTAMP
            """ + timestampSql + " WHERE id = ? AND order_id = ? AND status = ?",
            targetStatus,
            expectedReadyAt == null ? null : Timestamp.valueOf(expectedReadyAt),
            blankToNull(rejectReason),
            blankToNull(shipmentRemark),
            supplierOrderId,
            orderId,
            currentStatus
        );
        if (updated != 1) throw new IllegalStateException("FOOD_SUPPLIER_ORDER_CONCURRENT_UPDATE");
        jdbcTemplate.update(
            """
            INSERT INTO food_purchase_order_event
              (order_id, supplier_order_id, event_type, from_status, to_status, remark, operator_user_id)
            VALUES (?, ?, 'STATUS_CHANGE', ?, ?, ?, ?)
            """,
            orderId,
            supplierOrderId,
            currentStatus,
            targetStatus,
            firstText(rejectReason, shipmentRemark),
            userId
        );
        aggregateOrder(orderId);
        if (FoodOrderStatus.SUPPLIED.equals(targetStatus)) {
            ensureSettlementAndEvaluation(orderId, supplierOrderId);
        }
    }

    public List<SettlementSummary> listBuyerSettlements(long companyId, String status) {
        return listSettlements("s.buyer_company_id", companyId, status);
    }

    public List<SettlementSummary> listSupplierSettlements(long companyId, String status) {
        return listSettlements("s.supplier_company_id", companyId, status);
    }

    public List<SettlementSummary> listAllSupplierSettlements(String status) {
        return listSettlements(null, null, status);
    }

    public SettlementSummary updateSettlement(
        long settlementId, long companyId, String targetStatus, String invoiceNo, String invoiceFileId,
        BigDecimal actualAmount, List<AttachmentPayload> invoiceAttachments
    ) {
        return updateSettlement(
            settlementId, "buyer_company_id", companyId, false, targetStatus, invoiceNo, invoiceFileId,
            actualAmount, invoiceAttachments
        );
    }

    public SettlementSummary updateSupplierSettlement(
        long settlementId, long companyId, String targetStatus, String invoiceNo, String invoiceFileId,
        BigDecimal actualAmount, List<AttachmentPayload> invoiceAttachments
    ) {
        return updateSettlement(
            settlementId, "supplier_company_id", companyId, false, targetStatus, invoiceNo, invoiceFileId,
            actualAmount, invoiceAttachments
        );
    }

    public SettlementSummary updateSettlementForManagement(
        long settlementId, String targetStatus, String invoiceNo, String invoiceFileId,
        BigDecimal actualAmount, List<AttachmentPayload> invoiceAttachments
    ) {
        return updateSettlement(
            settlementId, null, null, true, targetStatus, invoiceNo, invoiceFileId, actualAmount, invoiceAttachments
        );
    }

    private SettlementSummary updateSettlement(
        long settlementId, String companyColumn, Long companyId, boolean management, String targetStatus, String invoiceNo,
        String invoiceFileId, BigDecimal actualAmount, List<AttachmentPayload> invoiceAttachments
    ) {
        SettlementSummary current = management
            ? findSettlementForManagement(settlementId)
            : findSettlement(settlementId, companyColumn, companyId);
        if (current == null) {
            return null;
        }
        String allowed = switch (targetStatus) {
            case "INVOICED" -> "PENDING_INVOICE";
            case "SETTLED" -> "INVOICED";
            case "PAID" -> "SETTLED";
            default -> "";
        };
        if (!allowed.equals(current.status())) {
            throw new IllegalStateException("FOOD_SETTLEMENT_INVALID_STATUS_TRANSITION");
        }
        String scope = management ? "" : " AND " + companyColumn + " = ?";
        List<Object> args = new ArrayList<>();
        args.add(targetStatus);
        args.add(blankToNull(invoiceNo));
        args.add(blankToNull(invoiceFileId));
        args.add(actualAmount);
        args.add(invoiceAttachments == null ? null : attachmentJson(invoiceAttachments));
        args.add(targetStatus);
        args.add(targetStatus);
        args.add(settlementId);
        if (!management) {
            args.add(companyId);
        }
        jdbcTemplate.update(
            """
            UPDATE food_settlement
            SET status = ?, invoice_no = COALESCE(?, invoice_no), invoice_file_id = COALESCE(?, invoice_file_id),
                actual_amount = COALESCE(?, actual_amount),
                invoice_attachments_json = COALESCE(?, invoice_attachments_json),
                settled_at = CASE WHEN ? = 'SETTLED' THEN CURRENT_TIMESTAMP ELSE settled_at END,
                paid_at = CASE WHEN ? = 'PAID' THEN CURRENT_TIMESTAMP ELSE paid_at END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """ + scope,
            args.toArray()
        );
        return management ? findSettlementForManagement(settlementId) : findSettlement(settlementId, companyColumn, companyId);
    }

    public List<EvaluationSummary> listEvaluations(long companyId, String status) {
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        return jdbcTemplate.query(
            """
            SELECT e.*, o.order_no, supplier.company_name supplier_name
            FROM food_evaluation e
            JOIN food_purchase_order o ON o.id = e.order_id
            JOIN company supplier ON supplier.id = e.supplier_company_id
            WHERE (e.buyer_company_id = ? OR e.supplier_company_id = ?)
              AND (? = '' OR e.status = ?)
            ORDER BY e.updated_at DESC, e.id DESC
            """,
            (rs, rowNum) -> evaluationSummary(rs),
            companyId,
            companyId,
            normalizedStatus,
            normalizedStatus
        );
    }

    public List<EvaluationSummary> listRegulatoryEvaluations(String keyword, String status) {
        String normalizedKeyword = safe(keyword);
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String like = "%" + normalizedKeyword + "%";
        return jdbcTemplate.query(
            """
            SELECT e.*, o.order_no, supplier.company_name supplier_name
            FROM food_evaluation e
            JOIN food_purchase_order o ON o.id = e.order_id
            JOIN company supplier ON supplier.id = e.supplier_company_id
            WHERE e.status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED')
              AND (? = '' OR e.status = ?)
              AND (? = '' OR o.order_no LIKE ? OR supplier.company_name LIKE ? OR e.comment LIKE ?)
            ORDER BY e.updated_at DESC, e.id DESC
            """,
            (rs, rowNum) -> evaluationSummary(rs),
            normalizedStatus,
            normalizedStatus,
            normalizedKeyword,
            like,
            like,
            like
        );
    }

    public EvaluationSummary submitEvaluation(
        long evaluationId, long buyerCompanyId, int quality, int logistics, String comment,
        List<AttachmentPayload> attachments
    ) {
        jdbcTemplate.update(
            """
            UPDATE food_evaluation
            SET status = 'PENDING_REVIEW', quality_rating = ?, logistics_rating = ?, comment = ?, attachments_json = ?,
                submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ? AND status IN ('PENDING_EVALUATION', 'REJECTED')
            """,
            quality,
            logistics,
            blankToNull(comment),
            evaluationAttachmentJson(attachments),
            evaluationId,
            buyerCompanyId
        );
        return findEvaluation(evaluationId, buyerCompanyId);
    }

    public EvaluationSummary reviewEvaluation(long evaluationId, long companyId, String targetStatus, String remark) {
        if (!List.of("APPROVED", "REJECTED").contains(targetStatus)) {
            throw new IllegalStateException("FOOD_EVALUATION_INVALID_REVIEW_STATUS");
        }
        jdbcTemplate.update(
            """
            UPDATE food_evaluation
            SET status = ?, review_remark = ?, reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'PENDING_REVIEW' AND (buyer_company_id = ? OR supplier_company_id = ?)
            """,
            targetStatus,
            blankToNull(remark),
            evaluationId,
            companyId,
            companyId
        );
        return findEvaluation(evaluationId, companyId);
    }

    public EvaluationSummary reviewRegulatoryEvaluation(
        long evaluationId, long reviewerId, String targetStatus, String remark
    ) {
        int updated = jdbcTemplate.update(
            """
            UPDATE food_evaluation
            SET status = ?, review_remark = ?, reviewer_id = ?, reviewed_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'PENDING_REVIEW'
            """,
            targetStatus,
            blankToNull(remark),
            reviewerId,
            evaluationId
        );
        return updated == 0 ? null : findRegulatoryEvaluation(evaluationId);
    }

    public String orderNo(long orderId) {
        return jdbcTemplate.queryForObject("SELECT order_no FROM food_purchase_order WHERE id = ?", String.class, orderId);
    }

    private List<OrderSummary> listOrders(String companyColumn, long companyId, String keyword, String status) {
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String sql = """
            SELECT o.*, d.demand_no, d.vessel_name, d.supply_port, d.vessel_eta,
                   (SELECT COUNT(*) FROM food_purchase_order_supplier os WHERE os.order_id = o.id) supplier_count,
                   (SELECT COUNT(*) FROM food_purchase_order_item oi WHERE oi.order_id = o.id) item_count
            FROM food_purchase_order o
            JOIN food_demand d ON d.id = o.demand_id
            WHERE %s = ?
              AND (? = '' OR LOWER(CONCAT_WS(' ', o.order_no, d.demand_no, d.vessel_name, d.supply_port)) LIKE ?)
              AND (? = '' OR o.status = ?)
            ORDER BY o.updated_at DESC, o.id DESC
            """.formatted(companyColumn);
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> orderSummary(rs),
            companyId,
            safe(keyword),
            "%" + safe(keyword).toLowerCase(Locale.ROOT) + "%",
            normalizedStatus,
            normalizedStatus
        );
    }

    private List<SupplierOrder> supplierOrders(long orderId, long companyId) {
        return jdbcTemplate.query(
            """
            SELECT os.*, c.company_name supplier_name
            FROM food_purchase_order_supplier os
            JOIN food_purchase_order o ON o.id = os.order_id
            JOIN company c ON c.id = os.supplier_company_id
            WHERE os.order_id = ? AND (o.buyer_company_id = ? OR os.supplier_company_id = ?)
            ORDER BY os.id
            """,
            (rs, rowNum) -> supplierOrder(rs),
            orderId,
            companyId,
            companyId
        );
    }

    private List<SupplierOrder> supplierOrdersForManagement(long orderId) {
        return jdbcTemplate.query(
            """
            SELECT os.*, c.company_name supplier_name
            FROM food_purchase_order_supplier os
            JOIN company c ON c.id = os.supplier_company_id
            WHERE os.order_id = ?
            ORDER BY os.id
            """,
            (rs, rowNum) -> supplierOrder(rs),
            orderId
        );
    }

    private List<OrderItem> orderItems(long orderId, long companyId) {
        return jdbcTemplate.query(
            """
            SELECT oi.*, c.company_name supplier_name
            FROM food_purchase_order_item oi
            JOIN food_purchase_order o ON o.id = oi.order_id
            JOIN food_purchase_order_supplier os ON os.id = oi.supplier_order_id
            JOIN company c ON c.id = os.supplier_company_id
            WHERE oi.order_id = ? AND (o.buyer_company_id = ? OR os.supplier_company_id = ?)
            ORDER BY oi.id
            """,
            (rs, rowNum) -> new OrderItem(
                rs.getLong("id"), rs.getLong("supplier_order_id"), rs.getLong("demand_item_id"), rs.getLong("quote_item_id"),
                rs.getString("supplier_name"), rs.getString("name_en"), rs.getString("name_zh"), rs.getString("specification"),
                rs.getString("unit"), rs.getBigDecimal("requested_quantity"), rs.getBigDecimal("ordered_quantity"),
                rs.getBigDecimal("unit_price"), rs.getBigDecimal("amount"),
                rs.getBigDecimal("quoted_unit_price"), rs.getBigDecimal("quoted_amount")
            ),
            orderId,
            companyId,
            companyId
        );
    }

    private List<OrderItem> orderItemsForManagement(long orderId) {
        return jdbcTemplate.query(
            """
            SELECT oi.*, c.company_name supplier_name
            FROM food_purchase_order_item oi
            JOIN food_purchase_order_supplier os ON os.id = oi.supplier_order_id
            JOIN company c ON c.id = os.supplier_company_id
            WHERE oi.order_id = ?
            ORDER BY oi.id
            """,
            (rs, rowNum) -> new OrderItem(
                rs.getLong("id"), rs.getLong("supplier_order_id"), rs.getLong("demand_item_id"), rs.getLong("quote_item_id"),
                rs.getString("supplier_name"), rs.getString("name_en"), rs.getString("name_zh"), rs.getString("specification"),
                rs.getString("unit"), rs.getBigDecimal("requested_quantity"), rs.getBigDecimal("ordered_quantity"),
                rs.getBigDecimal("unit_price"), rs.getBigDecimal("amount"),
                rs.getBigDecimal("quoted_unit_price"), rs.getBigDecimal("quoted_amount")
            ),
            orderId
        );
    }

    private List<SettlementSummary> listSettlements(String companyColumn, Long companyId, String status) {
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String companyScope = companyColumn == null ? "" : " AND " + companyColumn + " = ?";
        String sql = """
            SELECT s.*, o.order_no, o.currency, d.vessel_name,
                   buyer.company_name buyer_company_name, supplier.company_name supplier_name
            FROM food_settlement s
            JOIN food_purchase_order o ON o.id = s.order_id
            JOIN food_demand d ON d.id = o.demand_id
            JOIN company buyer ON buyer.id = s.buyer_company_id
            JOIN company supplier ON supplier.id = s.supplier_company_id
            WHERE (? = '' OR s.status = ?)
            %s
            ORDER BY s.updated_at DESC, s.id DESC
            """.formatted(companyScope);
        List<Object> args = new ArrayList<>();
        args.add(normalizedStatus);
        args.add(normalizedStatus);
        if (companyColumn != null) args.add(companyId);
        return jdbcTemplate.query(sql, (rs, rowNum) -> settlementSummary(rs), args.toArray());
    }

    private SettlementSummary findSettlement(long settlementId, String companyColumn, long companyId) {
        return jdbcTemplate.query(
            """
            SELECT s.*, o.order_no, o.currency, d.vessel_name,
                   buyer.company_name buyer_company_name, supplier.company_name supplier_name
            FROM food_settlement s
            JOIN food_purchase_order o ON o.id = s.order_id
            JOIN food_demand d ON d.id = o.demand_id
            JOIN company buyer ON buyer.id = s.buyer_company_id
            JOIN company supplier ON supplier.id = s.supplier_company_id
            WHERE s.id = ? AND s.%s = ?
            """.formatted(companyColumn),
            (rs, rowNum) -> settlementSummary(rs),
            settlementId,
            companyId
        ).stream().findFirst().orElse(null);
    }

    private SettlementSummary findSettlementForManagement(long settlementId) {
        return jdbcTemplate.query(
            """
            SELECT s.*, o.order_no, o.currency, d.vessel_name,
                   buyer.company_name buyer_company_name, supplier.company_name supplier_name
            FROM food_settlement s
            JOIN food_purchase_order o ON o.id = s.order_id
            JOIN food_demand d ON d.id = o.demand_id
            JOIN company buyer ON buyer.id = s.buyer_company_id
            JOIN company supplier ON supplier.id = s.supplier_company_id
            WHERE s.id = ?
            """,
            (rs, rowNum) -> settlementSummary(rs),
            settlementId
        ).stream().findFirst().orElse(null);
    }

    private EvaluationSummary findEvaluation(long evaluationId, long companyId) {
        return jdbcTemplate.query(
            """
            SELECT e.*, o.order_no, supplier.company_name supplier_name
            FROM food_evaluation e
            JOIN food_purchase_order o ON o.id = e.order_id
            JOIN company supplier ON supplier.id = e.supplier_company_id
            WHERE e.id = ? AND (e.buyer_company_id = ? OR e.supplier_company_id = ?)
            """,
            (rs, rowNum) -> evaluationSummary(rs),
            evaluationId,
            companyId,
            companyId
        ).stream().findFirst().orElse(null);
    }

    private EvaluationSummary findRegulatoryEvaluation(long evaluationId) {
        return jdbcTemplate.query(
            """
            SELECT e.*, o.order_no, supplier.company_name supplier_name
            FROM food_evaluation e
            JOIN food_purchase_order o ON o.id = e.order_id
            JOIN company supplier ON supplier.id = e.supplier_company_id
            WHERE e.id = ?
            """,
            (rs, rowNum) -> evaluationSummary(rs),
            evaluationId
        ).stream().findFirst().orElse(null);
    }

    private void aggregateOrder(long orderId) {
        List<String> statuses = jdbcTemplate.query(
            "SELECT status FROM food_purchase_order_supplier WHERE order_id = ?",
            (rs, rowNum) -> rs.getString("status"),
            orderId
        );
        jdbcTemplate.update(
            "UPDATE food_purchase_order SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            FoodOrderStatus.aggregate(statuses),
            orderId
        );
    }

    private void ensureSettlementAndEvaluation(long orderId, long supplierOrderId) {
        jdbcTemplate.update(
            """
            INSERT IGNORE INTO food_settlement
              (order_id, supplier_order_id, buyer_company_id, supplier_company_id, status, amount)
            SELECT o.id, os.id, o.buyer_company_id, os.supplier_company_id, 'PENDING_INVOICE', os.subtotal_amount
            FROM food_purchase_order o
            JOIN food_purchase_order_supplier os ON os.order_id = o.id
            WHERE o.id = ? AND os.id = ?
            """,
            orderId,
            supplierOrderId
        );
        jdbcTemplate.update(
            """
            INSERT IGNORE INTO food_evaluation
              (order_id, buyer_company_id, supplier_company_id, status)
            SELECT o.id, o.buyer_company_id, os.supplier_company_id, 'PENDING_EVALUATION'
            FROM food_purchase_order o
            JOIN food_purchase_order_supplier os ON os.order_id = o.id
            WHERE o.id = ? AND os.id = ?
            """,
            orderId,
            supplierOrderId
        );
    }

    private String nextOrderNo(long buyerCompanyId) {
        String prefix = "FPO-" + DAY.format(LocalDate.now()) + "-";
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_purchase_order WHERE buyer_company_id = ? AND order_no LIKE ?",
            Integer.class,
            buyerCompanyId,
            prefix + "%"
        );
        return prefix + String.format(Locale.ROOT, "%03d", (count == null ? 0 : count) + 1);
    }

    private OrderSummary orderSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OrderSummary(
            rs.getLong("id"), rs.getString("order_no"), rs.getLong("demand_id"), rs.getString("demand_no"),
            rs.getString("vessel_name"), rs.getString("supply_port"), localDateTime(rs.getTimestamp("vessel_eta")),
            rs.getString("currency"), rs.getString("status"), rs.getBigDecimal("total_amount"),
            rs.getInt("supplier_count"), rs.getInt("item_count"), localDateTime(rs.getTimestamp("updated_at")),
            localDateTime(rs.getTimestamp("required_delivery_time")), rs.getString("delivery_address"),
            rs.getString("delivery_contact_name"),
            rs.getString("delivery_contact_phone"), rs.getString("delivery_contact_email"),
            rs.getString("default_packaging_method"), rs.getString("buyer_remark"),
            rs.getBigDecimal("fixed_freight_fee"), rs.getBigDecimal("fixed_customs_fee"),
            rs.getBigDecimal("fixed_crane_fee"), rs.getBigDecimal("fixed_other_fee"),
            rs.getString("supply_mode"), rs.getString("fixed_provider_type"),
            rs.getString("fixed_provider_id"), rs.getString("fixed_provider_name"),
            rs.getString("traffic_service_json")
        );
    }

    private SupplierOrderSummary supplierOrderSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupplierOrderSummary(
            rs.getLong("order_id"), rs.getString("order_no"), rs.getLong("supplier_order_id"),
            rs.getLong("supplier_company_id"), rs.getString("supplier_name"), rs.getString("demand_no"),
            rs.getString("vessel_name"), rs.getString("supply_port"), localDateTime(rs.getTimestamp("vessel_eta")),
            rs.getString("currency"), rs.getString("status"), rs.getBigDecimal("total_amount"),
            rs.getInt("supplier_count"), rs.getInt("item_count"), localDateTime(rs.getTimestamp("expected_ready_at")),
            localDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private SupplierOrder supplierOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupplierOrder(
            rs.getLong("id"), rs.getLong("supplier_company_id"), rs.getString("supplier_name"), rs.getString("status"),
            rs.getBigDecimal("subtotal_amount"), localDateTime(rs.getTimestamp("expected_ready_at")),
            rs.getString("reject_reason"), rs.getString("shipment_remark")
        );
    }

    private SettlementSummary settlementSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SettlementSummary(
            rs.getLong("id"), rs.getLong("order_id"), rs.getString("order_no"), rs.getLong("supplier_order_id"),
            rs.getString("supplier_name"), rs.getString("buyer_company_name"), rs.getString("vessel_name"),
            rs.getString("currency"), rs.getString("status"), rs.getBigDecimal("amount"), rs.getBigDecimal("actual_amount"),
            rs.getString("invoice_no"), rs.getString("invoice_file_id"),
            settlementAttachments(
                rs.getString("invoice_attachments_json"), rs.getString("invoice_file_id"), rs.getString("invoice_no")
            ),
            localDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String attachmentJson(List<AttachmentPayload> attachments) {
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (Exception exception) {
            throw new IllegalStateException("FOOD_SETTLEMENT_ATTACHMENT_DATA_INVALID", exception);
        }
    }

    private List<AttachmentPayload> settlementAttachments(String json, String legacyFileId, String invoiceNo) {
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<AttachmentPayload>>() { });
            } catch (Exception exception) {
                throw new IllegalStateException("FOOD_SETTLEMENT_ATTACHMENT_DATA_INVALID", exception);
            }
        }
        String fileId = blankToNull(legacyFileId);
        if (fileId == null) return List.of();
        String fileName = blankToNull(invoiceNo);
        return List.of(new AttachmentPayload(fileId, fileName == null ? fileId : fileName, "/api/files/" + fileId));
    }

    private EvaluationSummary evaluationSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new EvaluationSummary(
            rs.getLong("id"), rs.getLong("order_id"), rs.getString("order_no"), rs.getString("supplier_name"),
            rs.getString("status"), nullableInteger(rs.getObject("quality_rating")), nullableInteger(rs.getObject("logistics_rating")),
            rs.getString("comment"), evaluationAttachments(rs.getString("attachments_json")), rs.getString("review_remark"),
            localDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String evaluationAttachmentJson(List<AttachmentPayload> attachments) {
        try {
            return objectMapper.writeValueAsString(attachments == null ? List.of() : attachments);
        } catch (Exception exception) {
            throw new IllegalStateException("FOOD_EVALUATION_ATTACHMENT_DATA_INVALID", exception);
        }
    }

    private List<AttachmentPayload> evaluationAttachments(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AttachmentPayload>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return safe(value).isBlank() ? null : value.trim();
    }

    private String firstText(String first, String second) {
        return !safe(first).isBlank() ? first.trim() : blankToNull(second);
    }

    private Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record OrderableQuoteItem(
        long quoteItemId,
        long demandItemId,
        long quoteId,
        long supplierCompanyId,
        String supplierName,
        String nameEn,
        String nameZh,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        BigDecimal quotedUnitPrice,
        BigDecimal quotedAmount
    ) {
    }
}
