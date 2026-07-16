package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementOrderRepository {

    private static final String SELECT_COLUMNS = """
        SELECT settlement.id, settlement.settlement_no, settlement.purchase_order_id,
               purchase.order_no AS purchase_order_no,
               purchase.vessel_name,
               settlement.supplier_order_id, settlement.traffic_service_order_id,
               settlement.buyer_company_id, buyer.company_name AS buyer_company_name,
               settlement.provider_company_id, settlement.provider_name,
               settlement.settlement_type, settlement.material_type,
               settlement.quoted_amount, settlement.actual_amount, settlement.currency,
               settlement.status, settlement.invoice_attachments_json,
               DATE_FORMAT(settlement.invoice_submitted_at, '%Y-%m-%d %H:%i:%s') AS invoice_submitted_at_text,
               DATE_FORMAT(settlement.settled_at, '%Y-%m-%d %H:%i:%s') AS settled_at_text,
               settlement.created_by,
               DATE_FORMAT(settlement.created_at, '%Y-%m-%d %H:%i:%s') AS created_at_text,
               DATE_FORMAT(settlement.updated_at, '%Y-%m-%d %H:%i:%s') AS updated_at_text
        FROM settlement_order settlement
        JOIN purchase_order purchase ON purchase.id = settlement.purchase_order_id
        LEFT JOIN company buyer ON buyer.id = settlement.buyer_company_id
        """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SettlementOrderRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void ensureSettlementTable() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS settlement_order (
              id BIGINT NOT NULL AUTO_INCREMENT,
              settlement_no VARCHAR(50) NOT NULL,
              source_key VARCHAR(80) NOT NULL,
              purchase_order_id BIGINT NOT NULL,
              supplier_order_id BIGINT NULL,
              traffic_service_order_id BIGINT NULL,
              buyer_company_id BIGINT NOT NULL,
              provider_company_id BIGINT NOT NULL,
              provider_name VARCHAR(200) NULL,
              settlement_type VARCHAR(20) NOT NULL,
              material_type VARCHAR(60) NULL,
              quoted_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
              actual_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
              currency VARCHAR(20) NOT NULL DEFAULT 'CNY',
              status VARCHAR(30) NOT NULL DEFAULT 'PENDING_INVOICE',
              created_by BIGINT NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_settlement_no (settlement_no),
              UNIQUE KEY uk_settlement_source (purchase_order_id, settlement_type, source_key),
              KEY idx_settlement_buyer_status (buyer_company_id, status, created_at),
              KEY idx_settlement_provider_type (provider_company_id, settlement_type, status, created_at),
              KEY idx_settlement_purchase_order (purchase_order_id),
              CONSTRAINT fk_settlement_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id) ON DELETE CASCADE,
              CONSTRAINT fk_settlement_supplier_order FOREIGN KEY (supplier_order_id) REFERENCES purchase_order_supplier (id) ON DELETE SET NULL,
              CONSTRAINT fk_settlement_traffic_order FOREIGN KEY (traffic_service_order_id) REFERENCES traffic_service_order (id) ON DELETE SET NULL,
              CONSTRAINT fk_settlement_buyer FOREIGN KEY (buyer_company_id) REFERENCES company (id),
              CONSTRAINT fk_settlement_provider FOREIGN KEY (provider_company_id) REFERENCES company (id),
              CONSTRAINT fk_settlement_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """
        );
        addColumnIfMissing("invoice_attachments_json", "TEXT NULL AFTER status");
        addColumnIfMissing("invoice_submitted_at", "DATETIME NULL AFTER invoice_attachments_json");
        addColumnIfMissing("settled_at", "DATETIME NULL AFTER invoice_submitted_at");
        jdbcTemplate.update("UPDATE settlement_order SET status = 'PENDING_INVOICE' WHERE status = 'PENDING'");
        jdbcTemplate.execute("ALTER TABLE settlement_order ALTER COLUMN status SET DEFAULT 'PENDING_INVOICE'");
    }

    public Optional<SettlementPurchaseContext> purchaseContext(Long buyerCompanyId, Long purchaseOrderId) {
        return jdbcTemplate.query(
            """
            SELECT purchase.id, purchase.order_no, purchase.demand_id, purchase.buyer_company_id,
                   purchase.buyer_company_name, purchase.vessel_name,
                   COALESCE(NULLIF(demand.material_type, ''), 'MATERIAL') AS material_type,
                   COALESCE(NULLIF(demand.currency, ''), purchase.currency, 'CNY') AS currency,
                   demand.traffic_service_json, demand.fixed_freight_fee,
                   demand.fixed_customs_fee, demand.fixed_crane_fee
            FROM purchase_order purchase
            LEFT JOIN material_demand demand ON demand.id = purchase.demand_id
            WHERE purchase.id = ? AND purchase.buyer_company_id = ?
            """,
            (rs, rowNum) -> new SettlementPurchaseContext(
                rs.getLong("id"),
                rs.getString("order_no"),
                rs.getLong("demand_id"),
                rs.getLong("buyer_company_id"),
                rs.getString("buyer_company_name"),
                rs.getString("vessel_name"),
                rs.getString("material_type"),
                rs.getString("currency"),
                rs.getString("traffic_service_json"),
                amount(rs, "fixed_freight_fee"),
                amount(rs, "fixed_customs_fee"),
                amount(rs, "fixed_crane_fee")
            ),
            purchaseOrderId,
            buyerCompanyId
        ).stream().findFirst();
    }

    public Optional<SupplierSettlementSource> supplierSource(Long buyerCompanyId, Long purchaseOrderId, Long supplierOrderId) {
        return jdbcTemplate.query(
            """
            SELECT supplier.id, supplier.supplier_company_id, supplier.supplier_name,
                   supplier.final_amount, supplier.currency
            FROM purchase_order_supplier supplier
            JOIN purchase_order purchase ON purchase.id = supplier.order_id
            WHERE supplier.id = ? AND supplier.order_id = ? AND purchase.buyer_company_id = ?
            """,
            (rs, rowNum) -> new SupplierSettlementSource(
                rs.getLong("id"),
                rs.getLong("supplier_company_id"),
                rs.getString("supplier_name"),
                amount(rs, "final_amount"),
                rs.getString("currency")
            ),
            supplierOrderId,
            purchaseOrderId,
            buyerCompanyId
        ).stream().findFirst();
    }

    public List<SupplierSettlementSource> supplierSources(Long buyerCompanyId, Long purchaseOrderId) {
        return jdbcTemplate.query(
            """
            SELECT supplier.id, supplier.supplier_company_id, supplier.supplier_name,
                   supplier.final_amount, supplier.currency
            FROM purchase_order_supplier supplier
            JOIN purchase_order purchase ON purchase.id = supplier.order_id
            WHERE supplier.order_id = ? AND purchase.buyer_company_id = ?
            ORDER BY supplier.id
            """,
            (rs, rowNum) -> new SupplierSettlementSource(
                rs.getLong("id"), rs.getLong("supplier_company_id"), rs.getString("supplier_name"),
                amount(rs, "final_amount"), rs.getString("currency")
            ),
            purchaseOrderId,
            buyerCompanyId
        );
    }

    public Optional<TrafficSettlementSource> linkExistingTrafficSource(
        Long buyerCompanyId,
        Long purchaseOrderId,
        Long demandId,
        Long snapshotTrafficServiceOrderId
    ) {
        List<Long> orderIds = jdbcTemplate.query(
            """
            SELECT DISTINCT traffic.id
            FROM traffic_service_order traffic
            LEFT JOIN traffic_shuttle_booking booking
              ON booking.traffic_service_order_id = traffic.id AND booking.status <> 'CANCELLED'
            WHERE traffic.requester_company_id = ?
              AND traffic.status <> 'DISCARDED'
              AND (
                traffic.purchase_order_id = ?
                OR booking.purchase_order_id = ?
                OR (
                  traffic.purchase_order_id IS NULL
                  AND (traffic.demand_id = ? OR booking.request_id = ? OR traffic.id = ?)
                )
              )
            ORDER BY traffic.id
            """,
            (rs, rowNum) -> rs.getLong("id"),
            buyerCompanyId,
            purchaseOrderId,
            purchaseOrderId,
            demandId,
            demandId,
            snapshotTrafficServiceOrderId
        );
        for (Long orderId : orderIds) {
            jdbcTemplate.update(
                """
                UPDATE traffic_service_order traffic
                LEFT JOIN traffic_shuttle_booking booking
                  ON booking.traffic_service_order_id = traffic.id
                 AND booking.status <> 'CANCELLED'
                 AND booking.purchase_order_id = ?
                SET traffic.demand_id = COALESCE(?, traffic.demand_id),
                    traffic.purchase_order_id = ?,
                    traffic.updated_at = CURRENT_TIMESTAMP
                WHERE traffic.id = ?
                  AND traffic.requester_company_id = ?
                  AND traffic.status <> 'DISCARDED'
                  AND (traffic.purchase_order_id IS NULL OR booking.purchase_order_id = ?)
                """,
                purchaseOrderId,
                demandId,
                purchaseOrderId,
                orderId,
                buyerCompanyId,
                purchaseOrderId
            );
        }
        return trafficSource(buyerCompanyId, purchaseOrderId, null);
    }

    public Optional<TrafficSettlementSource> trafficSource(Long buyerCompanyId, Long purchaseOrderId, Long expectedOrderId) {
        List<TrafficSettlementSource> sources = jdbcTemplate.query(
            """
            SELECT traffic.id, traffic.supplier_company_id, traffic.supplier_company_name,
                   COALESCE(
                     NULLIF(booking.amount, 0),
                     NULLIF(COALESCE(booking.freight_fee, 0) + COALESCE(booking.customs_fee, 0) + COALESCE(booking.crane_fee, 0), 0),
                     traffic.base_price,
                     0
                   ) AS amount
            FROM traffic_service_order traffic
            LEFT JOIN traffic_shuttle_booking booking
              ON booking.traffic_service_order_id = traffic.id
             AND booking.status <> 'CANCELLED'
             AND (booking.purchase_order_id = ? OR booking.purchase_order_id IS NULL)
            WHERE traffic.purchase_order_id = ? AND traffic.requester_company_id = ?
              AND traffic.status <> 'DISCARDED'
            ORDER BY CASE traffic.fee_type WHEN 'FREIGHT' THEN 0 WHEN 'CUSTOMS' THEN 1 ELSE 2 END, traffic.id
            """,
            (rs, rowNum) -> new TrafficSettlementSource(
                rs.getLong("id"),
                rs.getLong("supplier_company_id"),
                rs.getString("supplier_company_name"),
                amount(rs, "amount")
            ),
            purchaseOrderId,
            purchaseOrderId,
            buyerCompanyId
        );
        if (sources.isEmpty() || (expectedOrderId != null && sources.stream().noneMatch(source -> expectedOrderId.equals(source.trafficServiceOrderId())))) {
            return Optional.empty();
        }
        TrafficSettlementSource primary = sources.get(0);
        BigDecimal total = sources.stream()
            .map(TrafficSettlementSource::quotedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(new TrafficSettlementSource(
            primary.trafficServiceOrderId(),
            primary.providerCompanyId(),
            primary.providerName(),
            total
        ));
    }

    public SettlementOrderResponse upsert(SettlementDraft draft) {
        jdbcTemplate.update(
            """
            INSERT INTO settlement_order (
              settlement_no, source_key, purchase_order_id, supplier_order_id, traffic_service_order_id,
              buyer_company_id, provider_company_id, provider_name, settlement_type, material_type,
              quoted_amount, actual_amount, currency, status, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              id = LAST_INSERT_ID(id),
              supplier_order_id = VALUES(supplier_order_id),
              traffic_service_order_id = VALUES(traffic_service_order_id),
              provider_company_id = VALUES(provider_company_id),
              provider_name = VALUES(provider_name),
              material_type = VALUES(material_type),
              quoted_amount = VALUES(quoted_amount),
              actual_amount = CASE WHEN status IN ('PENDING', 'PENDING_INVOICE') THEN actual_amount ELSE actual_amount END,
              currency = VALUES(currency),
              status = CASE WHEN status = 'PENDING' THEN 'PENDING_INVOICE' ELSE status END,
              updated_at = CURRENT_TIMESTAMP
            """,
            draft.settlementNo(),
            draft.sourceKey(),
            draft.purchaseOrderId(),
            draft.supplierOrderId(),
            draft.trafficServiceOrderId(),
            draft.buyerCompanyId(),
            draft.providerCompanyId(),
            draft.providerName(),
            draft.settlementType(),
            draft.materialType(),
            draft.quotedAmount(),
            draft.actualAmount(),
            draft.currency(),
            draft.status(),
            draft.createdBy()
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findById(draft.buyerCompanyId(), id).orElseThrow();
    }

    public Optional<SettlementOrderResponse> updateProviderActualAmount(Long providerCompanyId, Long id, BigDecimal actualAmount) {
        int updated = jdbcTemplate.update(
            """
            UPDATE settlement_order
            SET actual_amount = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND provider_company_id = ? AND status = 'PENDING_INVOICE'
            """,
            actualAmount,
            id,
            providerCompanyId
        );
        return updated == 0 ? Optional.empty() : findByProvider(providerCompanyId, id);
    }

    public Optional<SettlementOrderResponse> submitInvoice(Long providerCompanyId, Long id, BigDecimal actualAmount, String attachmentsJson) {
        int updated = jdbcTemplate.update("""
            UPDATE settlement_order
            SET actual_amount = ?, invoice_attachments_json = ?, status = 'PENDING_SETTLEMENT',
                invoice_submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND provider_company_id = ? AND status = 'PENDING_INVOICE'
            """, actualAmount, attachmentsJson, id, providerCompanyId);
        return updated == 0 ? Optional.empty() : findByProvider(providerCompanyId, id);
    }

    public Optional<SettlementOrderResponse> settle(Long buyerCompanyId, Long id) {
        int updated = jdbcTemplate.update("""
            UPDATE settlement_order
            SET status = 'SETTLED', settled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ? AND status = 'PENDING_SETTLEMENT'
            """, id, buyerCompanyId);
        return updated == 0 ? Optional.empty() : findById(buyerCompanyId, id);
    }

    public Optional<SettlementOrderResponse> pay(Long buyerCompanyId, Long id) {
        int updated = jdbcTemplate.update("""
            UPDATE settlement_order
            SET status = 'PAID', updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ? AND status = 'SETTLED'
            """, id, buyerCompanyId);
        return updated == 0 ? Optional.empty() : findById(buyerCompanyId, id);
    }

    public boolean delete(Long buyerCompanyId, Long id) {
        return jdbcTemplate.update(
            "DELETE FROM settlement_order WHERE id = ? AND buyer_company_id = ?",
            id,
            buyerCompanyId
        ) > 0;
    }

    public Optional<SettlementOrderResponse> findById(Long buyerCompanyId, Long id) {
        return jdbcTemplate.query(
            SELECT_COLUMNS + " WHERE settlement.id = ? AND settlement.buyer_company_id = ?",
            (rs, rowNum) -> settlement(rs),
            id,
            buyerCompanyId
        ).stream().findFirst();
    }

    public Optional<SettlementOrderResponse> findByProvider(Long providerCompanyId, Long id) {
        return jdbcTemplate.query(
            SELECT_COLUMNS + " WHERE settlement.id = ? AND settlement.provider_company_id = ?",
            (rs, rowNum) -> settlement(rs), id, providerCompanyId
        ).stream().findFirst();
    }

    public SettlementOrderListResponse listBuyer(Long companyId, String keyword, String status, int page, int size) {
        return list("settlement.buyer_company_id = ?", companyId, null, keyword, status, page, size);
    }

    public SettlementOrderListResponse listSupplier(Long companyId, String keyword, String status, int page, int size) {
        return list("settlement.provider_company_id = ?", companyId, "SUPPLIER", keyword, status, page, size);
    }

    public SettlementOrderListResponse listBarge(Long companyId, String keyword, String status, int page, int size) {
        return list("settlement.provider_company_id = ?", companyId, "BARGE", keyword, status, page, size);
    }

    public String nextSettlementNo() {
        return "ST-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private SettlementOrderListResponse list(
        String ownerPredicate,
        Long companyId,
        String settlementType,
        String keyword,
        String status,
        int page,
        int size
    ) {
        StringBuilder where = new StringBuilder(" WHERE ").append(ownerPredicate);
        List<Object> params = new ArrayList<>();
        params.add(companyId);
        if (settlementType != null) {
            where.append(" AND settlement.settlement_type = ?");
            params.add(settlementType);
        }
        if (keyword != null) {
            where.append(" AND (settlement.settlement_no LIKE ? OR purchase.order_no LIKE ? OR settlement.provider_name LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            where.append(" AND settlement.status = ?");
            params.add(status);
        }
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM settlement_order settlement JOIN purchase_order purchase ON purchase.id = settlement.purchase_order_id" + where,
            Long.class,
            params.toArray()
        );
        int offset = (page - 1) * size;
        List<Object> itemParams = new ArrayList<>(params);
        itemParams.add(size);
        itemParams.add(offset);
        List<SettlementOrderResponse> items = jdbcTemplate.query(
            SELECT_COLUMNS + where + " ORDER BY settlement.created_at DESC, settlement.id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> settlement(rs),
            itemParams.toArray()
        );
        return new SettlementOrderListResponse(items, page, size, total == null ? 0 : total);
    }

    private SettlementOrderResponse settlement(ResultSet rs) throws SQLException {
        return new SettlementOrderResponse(
            rs.getLong("id"),
            rs.getString("settlement_no"),
            rs.getLong("purchase_order_id"),
            rs.getString("purchase_order_no"),
            rs.getString("vessel_name"),
            nullableLong(rs, "supplier_order_id"),
            nullableLong(rs, "traffic_service_order_id"),
            rs.getLong("buyer_company_id"),
            rs.getString("buyer_company_name"),
            rs.getLong("provider_company_id"),
            rs.getString("provider_name"),
            rs.getString("settlement_type"),
            rs.getString("material_type"),
            amount(rs, "quoted_amount"),
            amount(rs, "actual_amount"),
            rs.getString("currency"),
            rs.getString("status"),
            attachments(rs.getString("invoice_attachments_json")),
            rs.getString("invoice_submitted_at_text"),
            rs.getString("settled_at_text"),
            nullableLong(rs, "created_by"),
            rs.getString("created_at_text"),
            rs.getString("updated_at_text")
        );
    }

    private List<FileSnapshot> attachments(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<FileSnapshot>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void addColumnIfMissing(String column, String definition) {
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'settlement_order' AND column_name = ?
            """, Long.class, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE settlement_order ADD COLUMN " + column + " " + definition);
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static BigDecimal amount(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }
}
