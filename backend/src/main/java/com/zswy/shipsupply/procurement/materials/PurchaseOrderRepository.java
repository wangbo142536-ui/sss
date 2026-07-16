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
import java.util.Collections;
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
            SELECT po.*, supplier_counts.quoted_supplier_count, supplier_counts.total_supplier_count,
                   md.inquiry_no AS source_inquiry_no,
                   md.material_type AS source_material_type,
                   md.currency AS source_currency,
                   md.recipient_company AS source_recipient_company,
                   md.handler_name AS source_handler_name,
                   md.handler_email AS source_handler_email,
                   md.fixed_freight_fee AS source_fixed_freight_fee,
                   md.fixed_customs_fee AS source_fixed_customs_fee,
                   md.fixed_crane_fee AS source_fixed_crane_fee,
                   md.fixed_other_fee AS source_fixed_other_fee,
                   JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.supplyMode')) AS source_supply_mode,
                   JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType')) AS source_fixed_provider_type
            FROM purchase_order po
            LEFT JOIN material_demand md ON md.id = po.demand_id
            LEFT JOIN (
              SELECT order_id,
                     SUM(CASE WHEN COALESCE(subtotal_amount, 0) > 0 THEN 1 ELSE 0 END) AS quoted_supplier_count,
                     COUNT(*) AS total_supplier_count
              FROM purchase_order_supplier
              GROUP BY order_id
            ) supplier_counts ON supplier_counts.order_id = po.id
            WHERE buyer_company_id = ?
              AND demand_id = ?
              AND strategy_type = ?
              AND po.status <> 'DISCARDED'
            ORDER BY id DESC
            LIMIT 1
            """,
            (rs, rowNum) -> summary(rs),
            buyerCompanyId,
            demandId,
            strategyType
        ).stream().findFirst();
    }

    public Long findFirstActiveOrderIdByDemand(Long buyerCompanyId, Long demandId) {
        return jdbcTemplate.query(
            """
            SELECT id
            FROM purchase_order
            WHERE buyer_company_id = ?
              AND demand_id = ?
              AND status <> 'DISCARDED'
            ORDER BY id ASC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("id"),
            buyerCompanyId,
            demandId
        ).stream().findFirst().orElse(null);
    }

    public boolean isOrderDiscarded(Long orderId) {
        return jdbcTemplate.query(
            """
            SELECT status = 'DISCARDED'
            FROM purchase_order
            WHERE id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getBoolean(1),
            orderId
        ).stream().findFirst().orElse(false);
    }

    public int discardByDemand(Long buyerCompanyId, Long demandId, Long userId) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET status = 'DISCARDED',
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ?
              AND id = ?
            """,
            buyerCompanyId,
            demandId
        );
        List<Long> orderIds = jdbcTemplate.query(
            """
            SELECT id
            FROM purchase_order
            WHERE buyer_company_id = ?
              AND demand_id = ?
            """,
            (rs, rowNum) -> rs.getLong("id"),
            buyerCompanyId,
            demandId
        );
        int discardedOrders = 0;
        for (Long orderId : orderIds) {
            if (discardOrderRows(orderId, userId, buyerCompanyId)) {
                discardedOrders++;
            }
        }
        return discardedOrders;
    }

    public PurchaseOrderDiscardResult discardByOrder(Long buyerCompanyId, Long orderId, Long userId) {
        List<Long> demandIds = jdbcTemplate.query(
            """
            SELECT demand_id
            FROM purchase_order
            WHERE buyer_company_id = ?
              AND id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("demand_id"),
            buyerCompanyId,
            orderId
        );
        if (demandIds.isEmpty()) {
            return null;
        }
        Long demandId = demandIds.get(0);
        int discardedOrders = discardByDemand(buyerCompanyId, demandId, userId);
        return new PurchaseOrderDiscardResult(demandId, discardedOrders);
    }

    public void markDemandOrdered(Long buyerCompanyId, Long demandId) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET status = 'ORDERED',
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ?
              AND id = ?
              AND status <> 'DISCARDED'
            """,
            buyerCompanyId,
            demandId
        );
    }

    public String nextOrderNo(Long buyerCompanyId, LocalDate date) {
        String prefix = "PO-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        List<String> existing = jdbcTemplate.query(
            """
            SELECT order_no
            FROM purchase_order
            WHERE order_no LIKE CONCAT(?, '%')
            ORDER BY order_no DESC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("order_no"),
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

    public void linkBargeBookingToPurchaseOrder(Long buyerCompanyId, Long purchaseOrderId) {
        jdbcTemplate.update(
            """
            UPDATE traffic_shuttle_booking booking
            JOIN purchase_order purchase
              ON purchase.id = ? AND purchase.buyer_company_id = ?
            LEFT JOIN material_demand demand ON demand.id = purchase.demand_id
            SET booking.purchase_order_id = purchase.id,
                booking.updated_at = CURRENT_TIMESTAMP
            WHERE booking.requester_company_id = purchase.buyer_company_id
              AND booking.status <> 'CANCELLED'
              AND (
                booking.purchase_order_id = purchase.id
                OR booking.request_id = purchase.demand_id
                OR JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.bookingId')) = CAST(booking.id AS CHAR)
                OR JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.trafficServiceOrderId')) = CAST(booking.traffic_service_order_id AS CHAR)
              )
            """,
            purchaseOrderId,
            buyerCompanyId
        );
        jdbcTemplate.update(
            """
            UPDATE traffic_service_order traffic
            JOIN traffic_shuttle_booking booking
              ON booking.traffic_service_order_id = traffic.id AND booking.status <> 'CANCELLED'
            JOIN purchase_order purchase
              ON purchase.id = ? AND purchase.buyer_company_id = ?
            LEFT JOIN material_demand demand ON demand.id = purchase.demand_id
            SET traffic.purchase_order_id = purchase.id,
                traffic.updated_at = CURRENT_TIMESTAMP
            WHERE traffic.requester_company_id = purchase.buyer_company_id
              AND traffic.status <> 'DISCARDED'
              AND (traffic.purchase_order_id IS NULL OR traffic.purchase_order_id = purchase.id)
              AND (
                booking.purchase_order_id = purchase.id
                OR booking.request_id = purchase.demand_id
                OR traffic.demand_id = purchase.demand_id
                OR JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.bookingId')) = CAST(booking.id AS CHAR)
                OR JSON_UNQUOTE(JSON_EXTRACT(demand.traffic_service_json, '$.trafficServiceOrderId')) = CAST(traffic.id AS CHAR)
              )
            """,
            purchaseOrderId,
            buyerCompanyId
        );
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
        String sql = "SELECT po.*, supplier_counts.quoted_supplier_count, supplier_counts.total_supplier_count, packaging_methods.packaging_method, "
            + "supplier_contacts.supplier_contact_name, supplier_contacts.supplier_contact_phone, "
            + "purchase_progress.purchased_sku_count, purchase_progress.total_sku_count, supplier_readiness.ready_supplier_count, supplier_readiness.supplier_stage_index, "
            + "md.inquiry_no AS source_inquiry_no, md.material_type AS source_material_type, md.currency AS source_currency, "
            + "md.recipient_company AS source_recipient_company, md.handler_name AS source_handler_name, md.handler_email AS source_handler_email, "
            + "md.fixed_freight_fee AS source_fixed_freight_fee, md.fixed_customs_fee AS source_fixed_customs_fee, "
            + "md.fixed_crane_fee AS source_fixed_crane_fee, md.fixed_other_fee AS source_fixed_other_fee, "
            + "JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.supplyMode')) AS source_supply_mode, "
            + "JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType')) AS source_fixed_provider_type "
            + "FROM purchase_order po LEFT JOIN material_demand md ON md.id = po.demand_id "
            + supplierCountJoin()
            + packagingMethodJoin()
            + supplierContactJoin()
            + purchaseProgressJoin()
            + supplierReadinessJoin()
            + query.where()
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
            SELECT po.*, supplier_counts.quoted_supplier_count, supplier_counts.total_supplier_count, packaging_methods.packaging_method,
                   supplier_contacts.supplier_contact_name,
                   supplier_contacts.supplier_contact_phone,
                   purchase_progress.purchased_sku_count,
                   purchase_progress.total_sku_count,
                   supplier_readiness.ready_supplier_count,
                   supplier_readiness.supplier_stage_index,
                   md.inquiry_no AS source_inquiry_no,
                   md.material_type AS source_material_type,
                   md.currency AS source_currency,
                   md.recipient_company AS source_recipient_company,
                   md.handler_name AS source_handler_name,
                   md.handler_email AS source_handler_email,
                   md.fixed_freight_fee AS source_fixed_freight_fee,
                   md.fixed_customs_fee AS source_fixed_customs_fee,
                   md.fixed_crane_fee AS source_fixed_crane_fee,
                   md.fixed_other_fee AS source_fixed_other_fee,
                   JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.supplyMode')) AS source_supply_mode,
                   JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType')) AS source_fixed_provider_type
            FROM purchase_order po
            LEFT JOIN material_demand md ON md.id = po.demand_id
            """ + supplierCountJoin() + packagingMethodJoin() + """
            """ + supplierContactJoin() + """
            """ + purchaseProgressJoin() + """
            """ + supplierReadinessJoin() + """
            WHERE po.buyer_company_id = ? AND po.id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> summary(rs),
            buyerCompanyId,
            orderId
        ).stream().findFirst();
        return order.map(summary -> new PurchaseOrderDetailResponse(
            summary,
            supplierOrders(orderId, null),
            events(orderId),
            attachments(orderId, null)
        ));
    }

    public boolean updateDeliveryInfo(
        Long buyerCompanyId,
        Long orderId,
        PurchaseOrderDeliveryInfoUpdateRequest request
    ) {
        if (request == null) {
            return false;
        }
        int updated = jdbcTemplate.update(
            """
            UPDATE purchase_order
            SET supply_port = ?,
                vessel_eta = ?,
                required_delivery_time = ?,
                delivery_contact_name = ?,
                delivery_contact_phone = ?,
                delivery_contact_email = ?,
                buyer_remark = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ?
            """,
            blankToNull(request.supplyPort()),
            blankToNull(request.vesselEta()),
            blankToNull(request.requiredDeliveryTime()),
            blankToNull(request.deliveryContactName()),
            blankToNull(request.deliveryContactPhone()),
            blankToNull(request.deliveryContactEmail()),
            blankToNull(request.buyerRemark()),
            orderId,
            buyerCompanyId
        );
        return updated > 0;
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
        String supplierStageCase = "CASE "
            + "WHEN pos.status IN ('SUPPLIED', 'COMPLETED') THEN 5 "
            + "WHEN pos.status IN ('PARTIALLY_SUPPLIED', 'SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS') THEN 4 "
            + "WHEN pos.status IN ('WAITING_SUPPLY', 'WAITING_SERVICE') THEN 3 "
            + "WHEN pos.status IN ('PARTIALLY_READY', 'READY_TO_DELIVER', 'IN_TRANSIT') THEN 2 "
            + "WHEN pos.status IN ('PARTIALLY_CONFIRMED', 'PREPARING') THEN 1 "
            + "ELSE 0 END";
        String sql = "SELECT DISTINCT po.*, pos.id AS supplier_order_id, "
            + "CASE WHEN COALESCE(barge_progress.barge_stage_index, 0) > " + supplierStageCase + " THEN barge_progress.barge_status ELSE pos.status END AS supplier_status, "
            + "pos.expected_ready_at AS supplier_expected_ready_at, pos.packaging_method AS packaging_method, "
            + "pos.subtotal_amount AS supplier_subtotal_amount, pos.final_amount AS supplier_final_amount, "
            + "GREATEST(" + supplierStageCase + ", COALESCE(barge_progress.barge_stage_index, 0)) AS supplier_stage_index, "
            + "supplier_counts.quoted_supplier_count, supplier_counts.total_supplier_count, "
            + "md.inquiry_no AS source_inquiry_no, md.material_type AS source_material_type, md.currency AS source_currency, "
            + "md.recipient_company AS source_recipient_company, md.handler_name AS source_handler_name, md.handler_email AS source_handler_email, "
            + "md.fixed_freight_fee AS source_fixed_freight_fee, md.fixed_customs_fee AS source_fixed_customs_fee, "
            + "md.fixed_crane_fee AS source_fixed_crane_fee, md.fixed_other_fee AS source_fixed_other_fee, "
            + "JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.supplyMode')) AS source_supply_mode, "
            + "JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType')) AS source_fixed_provider_type "
            + "FROM purchase_order po JOIN purchase_order_supplier pos ON pos.order_id = po.id "
            + "LEFT JOIN material_demand md ON md.id = po.demand_id "
            + supplierBargeProgressJoin()
            + supplierCountJoin()
            + query.where()
            + " ORDER BY po.created_at DESC, po.id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>();
        args.add(supplierCompanyId);
        args.addAll(query.args());
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
        insertQcRequiredEvent(orderId, supplierOrderId, userId, supplierCompanyId);
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

    public Optional<PurchaseOrderDetailResponse> markSupplierReady(
        Long supplierCompanyId,
        Long userId,
        Long orderId,
        Long supplierOrderId
    ) {
        Optional<PurchaseSupplierOrderResponse> supplierOrder = supplierOrderForSupplier(orderId, supplierOrderId, supplierCompanyId);
        if (supplierOrder.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier
            SET status = 'READY_TO_DELIVER',
                ready_at = CURRENT_TIMESTAMP
            WHERE id = ? AND order_id = ? AND supplier_company_id = ? AND status = 'PREPARING'
            """,
            supplierOrderId,
            orderId,
            supplierCompanyId
        );
        insertEvent(orderId, supplierOrderId, "SUPPLIER_READY", "Supplier marked ready to deliver", userId, supplierCompanyId);
        aggregateOrderStatus(orderId);
        return findSupplierDetail(supplierCompanyId, orderId);
    }

    public Optional<PurchaseOrderDetailResponse> saveSupplierCustomsDocuments(
        Long supplierCompanyId,
        Long userId,
        Long orderId,
        Long supplierOrderId,
        PurchaseOrderAttachmentSaveRequest request
    ) {
        Optional<PurchaseSupplierOrderResponse> supplierOrder = supplierOrderForSupplier(orderId, supplierOrderId, supplierCompanyId);
        if (supplierOrder.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update(
            """
            DELETE FROM purchase_order_attachment
            WHERE order_id = ? AND supplier_order_id = ? AND attachment_type = 'CUSTOMS_DOCUMENT'
            """,
            orderId,
            supplierOrderId
        );
        List<PurchaseOrderAttachmentPayload> files = request == null || request.files() == null ? List.of() : request.files();
        for (PurchaseOrderAttachmentPayload file : files) {
            String fileName = blankToNull(file == null ? null : file.fileName());
            String fileId = blankToNull(file == null ? null : file.fileId());
            String fileUrl = blankToNull(file == null ? null : file.fileUrl());
            if (fileName == null && fileId == null && fileUrl == null) {
                continue;
            }
            jdbcTemplate.update(
                """
                INSERT INTO purchase_order_attachment
                  (order_id, supplier_order_id, attachment_type, file_id, file_name, file_url, created_by)
                VALUES (?, ?, 'CUSTOMS_DOCUMENT', ?, ?, ?, ?)
                """,
                orderId,
                supplierOrderId,
                fileId,
                fileName,
                fileUrl,
                userId
            );
        }
        insertEvent(orderId, supplierOrderId, "CUSTOMS_DOCUMENTS_SAVED", "Customs documents saved", userId, supplierCompanyId);
        return findSupplierDetail(supplierCompanyId, orderId);
    }

    public Optional<PurchaseOrderDetailResponse> markSupplierSupplied(
        Long supplierCompanyId,
        Long userId,
        Long orderId,
        Long supplierOrderId,
        PurchaseSupplierSupplyCompleteRequest request
    ) {
        Optional<PurchaseSupplierOrderResponse> supplierOrder = supplierOrderForSupplier(orderId, supplierOrderId, supplierCompanyId);
        if (supplierOrder.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier
            SET status = 'IN_TRANSIT',
                supplied_at = CURRENT_TIMESTAMP,
                delivery_image_file_id = ?,
                delivery_image_url = ?,
                delivery_remark = ?
            WHERE id = ? AND order_id = ? AND supplier_company_id = ? AND status = 'READY_TO_DELIVER'
            """,
            blankToNull(request.deliveryImageFileId()),
            blankToNull(request.deliveryImageUrl()),
            blankToNull(request.deliveryRemark()),
            supplierOrderId,
            orderId,
            supplierCompanyId
        );
        insertEvent(orderId, supplierOrderId, "SUPPLIER_IN_TRANSIT", "Supplier started delivery", userId, supplierCompanyId);
        aggregateOrderStatus(orderId);
        return findSupplierDetail(supplierCompanyId, orderId);
    }

    public Optional<PurchaseOrderDetailResponse> markSupplierWaitingSupply(
        Long supplierCompanyId,
        Long userId,
        Long orderId,
        Long supplierOrderId
    ) {
        Optional<PurchaseSupplierOrderResponse> supplierOrder = supplierOrderForSupplier(orderId, supplierOrderId, supplierCompanyId);
        if (supplierOrder.isEmpty()) {
            return Optional.empty();
        }
        int updated = jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier
            SET status = 'WAITING_SUPPLY',
                supplied_at = COALESCE(supplied_at, CURRENT_TIMESTAMP)
            WHERE id = ? AND order_id = ? AND supplier_company_id = ? AND status = 'IN_TRANSIT'
            """,
            supplierOrderId,
            orderId,
            supplierCompanyId
        );
        if (updated > 0) {
            insertEvent(orderId, supplierOrderId, "SUPPLIER_WAITING_SUPPLY", "Supplier completed transport", userId, supplierCompanyId);
            aggregateOrderStatus(orderId);
        }
        return findSupplierDetail(supplierCompanyId, orderId);
    }

    public Optional<PurchaseOrderDetailResponse> findSupplierDetail(Long supplierCompanyId, Long orderId) {
        Optional<PurchaseOrderSummaryResponse> order = jdbcTemplate.query(
            """
            SELECT po.*, pos.id AS supplier_order_id, pos.expected_ready_at AS supplier_expected_ready_at, pos.packaging_method AS packaging_method,
                   supplier_counts.quoted_supplier_count,
                   supplier_counts.total_supplier_count,
                   md.inquiry_no AS source_inquiry_no,
                   md.material_type AS source_material_type,
                   md.currency AS source_currency,
                   md.recipient_company AS source_recipient_company,
                   md.handler_name AS source_handler_name,
                   md.handler_email AS source_handler_email,
                   md.fixed_freight_fee AS source_fixed_freight_fee,
                   md.fixed_customs_fee AS source_fixed_customs_fee,
                   md.fixed_crane_fee AS source_fixed_crane_fee,
                   md.fixed_other_fee AS source_fixed_other_fee,
                   JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.supplyMode')) AS source_supply_mode,
                   JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType')) AS source_fixed_provider_type
            FROM purchase_order po
            JOIN purchase_order_supplier pos ON pos.order_id = po.id AND pos.supplier_company_id = ?
            LEFT JOIN material_demand md ON md.id = po.demand_id
            """ + supplierCountJoin() + """
            WHERE po.id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> summary(rs),
            supplierCompanyId,
            orderId
        ).stream().findFirst();
        return order.map(summary -> new PurchaseOrderDetailResponse(
            summary,
            supplierOrders(orderId, supplierCompanyId),
            events(orderId),
            attachments(orderId, supplierCompanyId)
        ));
    }

    private boolean discardOrderRows(Long orderId, Long userId, Long operatorCompanyId) {
        int updatedOrders = jdbcTemplate.update(
            """
            UPDATE purchase_order
            SET status = 'DISCARDED',
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND status <> 'DISCARDED'
            """,
            orderId
        );
        if (updatedOrders == 0) {
            return false;
        }
        jdbcTemplate.update(
            """
            UPDATE purchase_order_supplier
            SET status = 'DISCARDED'
            WHERE order_id = ?
              AND status <> 'DISCARDED'
            """,
            orderId
        );
        insertEvent(orderId, null, "DOCUMENT_DISCARDED", "Document discarded", userId, operatorCompanyId);
        return true;
    }

    private long insertMaster(PurchaseOrderDraft draft) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO purchase_order
                  (order_no, demand_id, demand_no, application_no, buyer_company_id, buyer_company_name,
                   vessel_name, supply_port, vessel_eta, required_delivery_time,
                   delivery_contact_name, delivery_contact_phone, delivery_contact_email, strategy_type,
                   strategy_name, supplier_count, item_count, total_amount, total_amount_usd, currency, status, buyer_remark, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            statement.setString(11, draft.deliveryContactName());
            statement.setString(12, draft.deliveryContactPhone());
            statement.setString(13, draft.deliveryContactEmail());
            statement.setString(14, draft.strategyType());
            statement.setString(15, draft.strategyName());
            statement.setInt(16, draft.supplierCount());
            statement.setInt(17, draft.itemCount());
            statement.setBigDecimal(18, draft.totalAmount());
            statement.setBigDecimal(19, draft.totalAmountUsd());
            statement.setString(20, draft.currency());
            statement.setString(21, draft.status());
            statement.setString(22, draft.buyerRemark());
            statement.setLong(23, draft.createdBy());
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
               unit_price_usd, amount, amount_usd, actual_quote_price, actual_quote_currency, quote_markup_percent,
               quote_profit_amount, currency, unit_mismatch_flag, quantity_fallback_flag, source_match_type, source_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            item.unitPriceUsd(),
            item.amount(),
            item.amountUsd(),
            item.actualQuotePrice(),
            item.actualQuoteCurrency(),
            item.quoteMarkupPercent(),
            item.quoteProfitAmount(),
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

    private void insertQcRequiredEvent(Long orderId, Long supplierOrderId, Long userId, Long supplierCompanyId) {
        List<String> labels = jdbcTemplate.query(
            """
            SELECT COALESCE(NULLIF(product_name, ''), NULLIF(supplier_sku_code, ''), CONCAT('SKU ', id)) AS label
            FROM purchase_order_item
            WHERE order_id = ? AND supplier_order_id = ?
            ORDER BY id ASC
            """,
            (rs, rowNum) -> rs.getString("label"),
            orderId,
            supplierOrderId
        );
        if (labels.isEmpty()) {
            return;
        }
        Collections.shuffle(labels);
        List<String> selected = labels.stream().limit(5).toList();
        insertEvent(orderId, supplierOrderId, "QC_REQUIRED", "QC random check: " + String.join(", ", selected), userId, supplierCompanyId);
    }

    private void aggregateOrderStatus(Long orderId) {
        List<String> statuses = jdbcTemplate.query(
            "SELECT status FROM purchase_order_supplier WHERE order_id = ?",
            (rs, rowNum) -> rs.getString("status"),
            orderId
        );
        String computedStatus = aggregateStatus(statuses);
        String currentStatus = jdbcTemplate.query(
            "SELECT status FROM purchase_order WHERE id = ?",
            rs -> rs.next() ? rs.getString("status") : null,
            orderId
        );
        String status = forwardStatus(currentStatus, computedStatus);
        BigDecimal total = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(final_amount), 0) FROM purchase_order_supplier WHERE order_id = ?",
            BigDecimal.class,
            orderId
        );
        jdbcTemplate.update(
            "UPDATE purchase_order SET status = ?, total_amount = ?, total_amount_usd = COALESCE(total_amount_usd, 0) WHERE id = ?",
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
        boolean allReady = statuses.stream().allMatch(PurchaseOrderService.READY_TO_DELIVER::equals);
        boolean allInTransit = statuses.stream().allMatch(PurchaseOrderService.IN_TRANSIT::equals);
        boolean allWaitingSupply = statuses.stream().allMatch(PurchaseOrderService.WAITING_SUPPLY::equals);
        boolean allSupplied = statuses.stream().allMatch(PurchaseOrderService.SUPPLIED::equals);
        boolean allRejected = statuses.stream().allMatch(PurchaseOrderService.REJECTED::equals);
        boolean anyPreparing = statuses.stream().anyMatch(PurchaseOrderService.PREPARING::equals);
        boolean anyReady = statuses.stream().anyMatch(PurchaseOrderService.READY_TO_DELIVER::equals);
        boolean anyInTransit = statuses.stream().anyMatch(PurchaseOrderService.IN_TRANSIT::equals);
        boolean anyWaitingSupply = statuses.stream().anyMatch(PurchaseOrderService.WAITING_SUPPLY::equals);
        boolean anySupplied = statuses.stream().anyMatch(PurchaseOrderService.SUPPLIED::equals);
        boolean anyRejected = statuses.stream().anyMatch(PurchaseOrderService.REJECTED::equals);
        if (allSupplied) {
            return PurchaseOrderService.SUPPLIED;
        }
        if (anySupplied) {
            return "PARTIALLY_SUPPLIED";
        }
        if (allWaitingSupply || anyWaitingSupply) {
            return PurchaseOrderService.WAITING_SUPPLY;
        }
        if (allInTransit || anyInTransit) {
            return PurchaseOrderService.IN_TRANSIT;
        }
        if (allReady) {
            return PurchaseOrderService.READY_TO_DELIVER;
        }
        if (anyReady) {
            return "PARTIALLY_READY";
        }
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

    private String forwardStatus(String currentStatus, String computedStatus) {
        String current = normalizeStatus(currentStatus);
        String computed = normalizeStatus(computedStatus);
        if (current == null) {
            return computed;
        }
        if (computed == null) {
            return current;
        }
        return statusStage(computed) >= statusStage(current) ? computed : current;
    }

    private int statusStage(String status) {
        if (status == null) {
            return -1;
        }
        return switch (status) {
            case "COMPLETED", "SUPPLIED" -> 6;
            case "PARTIALLY_SUPPLIED", "SUPPLYING", "IN_SERVICE", "IN_PROGRESS" -> 5;
            case "WAITING_SUPPLY", "WAITING_SERVICE" -> 4;
            case "IN_TRANSIT" -> 3;
            case "READY_TO_DELIVER", "PARTIALLY_READY" -> 2;
            case "PREPARING", "PARTIALLY_CONFIRMED" -> 1;
            case "PENDING_SUPPLIER_CONFIRM" -> 0;
            case "DISCARDED", "CANCELLED", "CANCELED", "REJECTED", "PARTIALLY_REJECTED" -> 0;
            default -> 0;
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
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
            Optional.ofNullable(safeString(rs, "supplier_status")).orElse(rs.getString("status")),
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
            optionalTimestamp(rs, "ready_at"),
            optionalTimestamp(rs, "supplied_at"),
            optionalString(rs, "delivery_image_file_id"),
            optionalString(rs, "delivery_image_url"),
            optionalString(rs, "delivery_remark"),
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

    private List<PurchaseOrderAttachmentResponse> attachments(Long orderId, Long supplierCompanyId) {
        String sql = """
            SELECT attachment.*
            FROM purchase_order_attachment attachment
            JOIN purchase_order_supplier supplier_order ON supplier_order.id = attachment.supplier_order_id
            WHERE attachment.order_id = ?
            """ + (supplierCompanyId == null ? "" : " AND supplier_order.supplier_company_id = ?")
            + " ORDER BY attachment.created_at ASC, attachment.id ASC";
        Object[] args = supplierCompanyId == null ? new Object[] {orderId} : new Object[] {orderId, supplierCompanyId};
        return jdbcTemplate.query(sql, (rs, rowNum) -> attachment(rs), args);
    }

    private PurchaseOrderAttachmentResponse attachment(ResultSet rs) throws SQLException {
        return new PurchaseOrderAttachmentResponse(
            rs.getLong("id"),
            rs.getLong("order_id"),
            rs.getLong("supplier_order_id"),
            rs.getString("attachment_type"),
            optionalString(rs, "file_id"),
            optionalString(rs, "file_name"),
            optionalString(rs, "file_url"),
            timestampToString(rs.getTimestamp("created_at"))
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
                   OR EXISTS (
                     SELECT 1 FROM purchase_order_supplier pos_kw
                     WHERE pos_kw.order_id = po.id AND pos_kw.supplier_name LIKE CONCAT('%', ?, '%')
                   )
                 )
                """);
            args.add(keyword);
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
            optionalString(rs, "source_inquiry_no"),
            optionalString(rs, "source_material_type"),
            optionalString(rs, "source_currency"),
            optionalString(rs, "source_recipient_company"),
            optionalString(rs, "source_handler_name"),
            optionalString(rs, "source_handler_email"),
            rs.getLong("buyer_company_id"),
            rs.getString("buyer_company_name"),
            rs.getString("vessel_name"),
            rs.getString("supply_port"),
            rs.getString("vessel_eta"),
            rs.getString("required_delivery_time"),
            optionalString(rs, "delivery_contact_name"),
            optionalString(rs, "delivery_contact_phone"),
            optionalString(rs, "delivery_contact_email"),
            optionalString(rs, "supplier_contact_name"),
            optionalString(rs, "supplier_contact_phone"),
            rs.getString("strategy_type"),
            rs.getString("strategy_name"),
            rs.getInt("supplier_count"),
            optionalInt(rs, "quoted_supplier_count", rs.getInt("supplier_count")),
            optionalInt(rs, "total_supplier_count", rs.getInt("supplier_count")),
            rs.getInt("item_count"),
            optionalInt(rs, "purchased_sku_count", rs.getInt("item_count")),
            optionalInt(rs, "total_sku_count", rs.getInt("item_count")),
            optionalInt(rs, "ready_supplier_count", 0),
            optionalInt(rs, "supplier_stage_index", 0),
            rs.getBigDecimal("total_amount"),
            optionalBigDecimal(rs, "total_amount_usd", BigDecimal.ZERO),
            optionalBigDecimal(rs, "supplier_subtotal_amount", null),
            optionalBigDecimal(rs, "supplier_final_amount", null),
            rs.getString("currency"),
            Optional.ofNullable(safeString(rs, "supplier_status")).orElse(rs.getString("status")),
            optionalString(rs, "packaging_method"),
            optionalLong(rs, "supplier_order_id"),
            optionalTimestamp(rs, "supplier_expected_ready_at"),
            optionalBigDecimal(rs, "source_fixed_freight_fee", BigDecimal.ZERO),
            optionalBigDecimal(rs, "source_fixed_customs_fee", BigDecimal.ZERO),
            optionalBigDecimal(rs, "source_fixed_crane_fee", BigDecimal.ZERO),
            optionalBigDecimal(rs, "source_fixed_other_fee", BigDecimal.ZERO),
            optionalString(rs, "source_supply_mode"),
            optionalString(rs, "source_fixed_provider_type"),
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
            optionalBigDecimal(rs, "unit_price_usd", null),
            rs.getBigDecimal("amount"),
            optionalBigDecimal(rs, "amount_usd", null),
            optionalBigDecimal(rs, "actual_quote_price", null),
            safeString(rs, "actual_quote_currency"),
            optionalBigDecimal(rs, "quote_markup_percent", null),
            optionalBigDecimal(rs, "quote_profit_amount", null),
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

    private int optionalInt(ResultSet rs, String column, int fallback) throws SQLException {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? fallback : value;
        } catch (SQLException ex) {
            return fallback;
        }
    }

    private Long optionalLong(ResultSet rs, String column) throws SQLException {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException ex) {
            return null;
        }
    }

    private BigDecimal optionalBigDecimal(ResultSet rs, String column, BigDecimal fallback) throws SQLException {
        try {
            BigDecimal value = rs.getBigDecimal(column);
            return value == null ? fallback : value;
        } catch (SQLException ex) {
            return fallback;
        }
    }

    private String safeString(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException ex) {
            return null;
        }
    }

    private String supplierCountJoin() {
        return """
             LEFT JOIN (
               SELECT order_id,
                      SUM(CASE WHEN COALESCE(subtotal_amount, 0) > 0 THEN 1 ELSE 0 END) AS quoted_supplier_count,
                      COUNT(*) AS total_supplier_count
               FROM purchase_order_supplier
               GROUP BY order_id
             ) supplier_counts ON supplier_counts.order_id = po.id
            """;
    }

    private String packagingMethodJoin() {
        return """
             LEFT JOIN (
               SELECT order_id,
                      CASE
                        WHEN COUNT(DISTINCT COALESCE(NULLIF(packaging_method, ''), 'SUPPLIER_PACKAGING')) > 1 THEN 'MULTIPLE'
                        ELSE MIN(COALESCE(NULLIF(packaging_method, ''), 'SUPPLIER_PACKAGING'))
                      END AS packaging_method
               FROM purchase_order_supplier
               GROUP BY order_id
             ) packaging_methods ON packaging_methods.order_id = po.id
            """;
    }

    private String supplierContactJoin() {
        return """
             LEFT JOIN (
               SELECT pos.order_id,
                      SUBSTRING_INDEX(GROUP_CONCAT(cc.contact_name ORDER BY pos.id ASC, cc.id ASC SEPARATOR '||'), '||', 1) AS supplier_contact_name,
                      SUBSTRING_INDEX(GROUP_CONCAT(cc.contact_phone ORDER BY pos.id ASC, cc.id ASC SEPARATOR '||'), '||', 1) AS supplier_contact_phone
               FROM purchase_order_supplier pos
               JOIN company_contact cc ON cc.company_id = pos.supplier_company_id AND cc.status = 'ACTIVE'
               GROUP BY pos.order_id
             ) supplier_contacts ON supplier_contacts.order_id = po.id
            """;
    }

    private String purchaseProgressJoin() {
        return """
             LEFT JOIN (
               SELECT purchase.id AS order_id,
                      COALESCE(item_counts.purchased_sku_count, 0) AS purchased_sku_count,
                      COALESCE(demand_item_counts.total_sku_count, 0) AS total_sku_count
               FROM purchase_order purchase
               LEFT JOIN (
                 SELECT order_id, COUNT(*) AS purchased_sku_count
                 FROM purchase_order_item
                 GROUP BY order_id
               ) item_counts ON item_counts.order_id = purchase.id
               LEFT JOIN (
                 SELECT demand_id, COUNT(*) AS total_sku_count
                 FROM material_demand_item
                 GROUP BY demand_id
               ) demand_item_counts ON demand_item_counts.demand_id = purchase.demand_id
               GROUP BY purchase.id
             ) purchase_progress ON purchase_progress.order_id = po.id
            """;
    }

    private String supplierReadinessJoin() {
        return """
             LEFT JOIN (
               SELECT order_id,
                      SUM(
                        CASE
                          WHEN status IN ('READY_TO_DELIVER', 'IN_TRANSIT', 'WAITING_SUPPLY', 'WAITING_SERVICE', 'SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS', 'SUPPLIED', 'PARTIALLY_SUPPLIED', 'COMPLETED')
                          THEN 1 ELSE 0
                        END
                      ) AS ready_supplier_count,
                      MAX(
                        CASE
                          WHEN status IN ('SUPPLIED', 'COMPLETED') THEN 5
                          WHEN status IN ('PARTIALLY_SUPPLIED', 'SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS') THEN 4
                          WHEN status IN ('WAITING_SUPPLY', 'WAITING_SERVICE') THEN 3
                          WHEN status IN ('PARTIALLY_READY', 'READY_TO_DELIVER', 'IN_TRANSIT') THEN 2
                          WHEN status IN ('PARTIALLY_CONFIRMED', 'PREPARING') THEN 1
                          WHEN status IS NOT NULL AND status <> 'PENDING_SUPPLIER_CONFIRM' THEN 1
                          ELSE 0
                        END
                      ) AS supplier_stage_index
               FROM purchase_order_supplier
               GROUP BY order_id
             ) supplier_readiness ON supplier_readiness.order_id = po.id
            """;
    }

    private String supplierBargeProgressJoin() {
        return """
             LEFT JOIN (
               SELECT linked.order_id,
                      MAX(
                        CASE
                          WHEN linked.status IN ('COMPLETED') THEN 5
                          WHEN linked.status IN ('SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS', 'IN_TRANSIT') THEN 4
                          WHEN linked.status IN ('WAITING_SUPPLY', 'WAITING_SERVICE') THEN 3
                          ELSE 0
                        END
                      ) AS barge_stage_index,
                      CASE
                        WHEN MAX(
                          CASE
                            WHEN linked.status IN ('COMPLETED') THEN 5
                            WHEN linked.status IN ('SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS', 'IN_TRANSIT') THEN 4
                            WHEN linked.status IN ('WAITING_SUPPLY', 'WAITING_SERVICE') THEN 3
                            ELSE 0
                          END
                        ) >= 5 THEN 'COMPLETED'
                        WHEN MAX(
                          CASE
                            WHEN linked.status IN ('COMPLETED') THEN 5
                            WHEN linked.status IN ('SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS', 'IN_TRANSIT') THEN 4
                            WHEN linked.status IN ('WAITING_SUPPLY', 'WAITING_SERVICE') THEN 3
                            ELSE 0
                          END
                        ) >= 4 THEN 'SUPPLYING'
                        WHEN MAX(
                          CASE
                            WHEN linked.status IN ('COMPLETED') THEN 5
                            WHEN linked.status IN ('SUPPLYING', 'IN_SERVICE', 'IN_PROGRESS', 'IN_TRANSIT') THEN 4
                            WHEN linked.status IN ('WAITING_SUPPLY', 'WAITING_SERVICE') THEN 3
                            ELSE 0
                          END
                        ) >= 3 THEN 'WAITING_SUPPLY'
                        ELSE NULL
                      END AS barge_status
               FROM (
                 SELECT DISTINCT COALESCE(booking.purchase_order_id, service_order.purchase_order_id, purchase_by_demand.id) AS order_id,
                        service_order.status
                 FROM traffic_shuttle_booking booking
                 LEFT JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
                 LEFT JOIN purchase_order purchase_by_demand
                   ON purchase_by_demand.demand_id = booking.request_id
                  AND purchase_by_demand.buyer_company_id = booking.requester_company_id
                  AND purchase_by_demand.status <> 'DISCARDED'
                 WHERE booking.status <> 'CANCELLED'
                   AND (service_order.id IS NULL OR service_order.status <> 'DISCARDED')
               ) linked
               JOIN purchase_order_supplier pos_scope
                 ON pos_scope.order_id = linked.order_id
                AND pos_scope.supplier_company_id = ?
               WHERE linked.order_id IS NOT NULL
               GROUP BY linked.order_id
             ) barge_progress ON barge_progress.order_id = po.id
            """;
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private String optionalTimestamp(ResultSet rs, String column) throws SQLException {
        try {
            return timestampToString(rs.getTimestamp(column));
        } catch (SQLException ex) {
            return null;
        }
    }

    private String optionalString(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record QueryParts(String where, List<Object> args) {
    }
}
