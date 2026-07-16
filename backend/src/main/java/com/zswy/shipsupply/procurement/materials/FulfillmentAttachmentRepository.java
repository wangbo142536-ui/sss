package com.zswy.shipsupply.procurement.materials;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FulfillmentAttachmentRepository {

    private static final String SELECT_COLUMNS = """
        SELECT attachment.id, attachment.provider_type, attachment.purchase_order_id,
               attachment.supplier_order_id, attachment.traffic_shuttle_id,
               attachment.booking_id, attachment.traffic_service_order_id, attachment.node_index, attachment.node_name,
               attachment.provider_company_id, company.company_name AS provider_name,
               attachment.file_id, attachment.file_name, attachment.file_url,
               attachment.uploaded_by,
               DATE_FORMAT(attachment.created_at, '%Y-%m-%d %H:%i:%s') AS created_at_text
        FROM fulfillment_attachment attachment
        LEFT JOIN company ON company.id = attachment.provider_company_id
        """;

    private final JdbcTemplate jdbcTemplate;

    public FulfillmentAttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS fulfillment_attachment (
              id BIGINT NOT NULL AUTO_INCREMENT,
              provider_type VARCHAR(20) NOT NULL,
              purchase_order_id BIGINT NULL,
              supplier_order_id BIGINT NULL,
              traffic_shuttle_id BIGINT NULL,
              booking_id BIGINT NULL,
              traffic_service_order_id BIGINT NULL,
              node_index INT NULL,
              node_name VARCHAR(120) NULL,
              provider_company_id BIGINT NOT NULL,
              file_id VARCHAR(100) NULL,
              file_name VARCHAR(255) NOT NULL,
              file_url VARCHAR(1000) NOT NULL,
              uploaded_by BIGINT NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_fulfillment_order (purchase_order_id, provider_type, created_at),
              KEY idx_fulfillment_supplier (supplier_order_id, created_at),
              KEY idx_fulfillment_booking (booking_id, created_at),
              KEY idx_fulfillment_shuttle (traffic_shuttle_id, node_index, created_at),
              KEY idx_fulfillment_traffic_order (traffic_service_order_id, created_at),
              KEY idx_fulfillment_provider (provider_company_id, created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """);
        ensureColumn("booking_id", "BIGINT NULL");
        ensureIndex("idx_fulfillment_booking", "booking_id, created_at");
    }

    public boolean buyerOwnsOrder(Long companyId, Long orderId) {
        return count("SELECT COUNT(*) FROM purchase_order WHERE id = ? AND buyer_company_id = ?", orderId, companyId) > 0;
    }

    public boolean supplierOwnsOrder(Long companyId, Long orderId, Long supplierOrderId) {
        return count("""
            SELECT COUNT(*) FROM purchase_order_supplier
            WHERE id = ? AND order_id = ? AND supplier_company_id = ?
            """, supplierOrderId, orderId, companyId) > 0;
    }

    public boolean supplierHasOrder(Long companyId, Long orderId) {
        return count("""
            SELECT COUNT(*) FROM purchase_order_supplier
            WHERE order_id = ? AND supplier_company_id = ?
            """, orderId, companyId) > 0;
    }

    public boolean bargeHasOrder(Long companyId, Long orderId) {
        return count("""
            SELECT COUNT(*) FROM traffic_service_order
            WHERE purchase_order_id = ? AND supplier_company_id = ? AND status <> 'DISCARDED'
            """, orderId, companyId) > 0;
    }

    public boolean shuttleOwnedBy(Long companyId, Long shuttleId) {
        return count("SELECT COUNT(*) FROM traffic_shuttle_service WHERE id = ? AND supplier_company_id = ?", shuttleId, companyId) > 0;
    }

    public boolean hasSupplierAttachment(Long supplierOrderId) {
        return count("SELECT COUNT(*) FROM fulfillment_attachment WHERE supplier_order_id = ?", supplierOrderId) > 0;
    }

    public boolean hasShuttleAttachment(Long shuttleId) {
        return count("SELECT COUNT(*) FROM fulfillment_attachment WHERE traffic_shuttle_id = ?", shuttleId) > 0;
    }

    public List<FulfillmentAttachmentResponse> listForPurchaseOrder(Long orderId) {
        return jdbcTemplate.query(
            SELECT_COLUMNS + """
             WHERE attachment.purchase_order_id = ?
                OR (attachment.traffic_shuttle_id IS NOT NULL AND EXISTS (
                    SELECT 1 FROM traffic_shuttle_booking booking
                    JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
                    WHERE booking.shuttle_service_id = attachment.traffic_shuttle_id
                      AND service_order.purchase_order_id = ?
                ))
             ORDER BY attachment.created_at DESC, attachment.id DESC
            """,
            (rs, rowNum) -> map(rs),
            orderId,
            orderId
        );
    }

    public List<FulfillmentAttachmentResponse> listProviderForPurchaseOrder(Long companyId, Long orderId) {
        return jdbcTemplate.query(
            SELECT_COLUMNS + """
             WHERE attachment.provider_company_id = ?
               AND (
                 (attachment.provider_type = 'SUPPLIER' AND attachment.purchase_order_id = ?)
                 OR (
                   attachment.provider_type = 'BARGE'
                   AND (
                     attachment.purchase_order_id = ?
                     OR attachment.traffic_service_order_id IN (
                       SELECT id FROM traffic_service_order
                       WHERE purchase_order_id = ? AND supplier_company_id = ?
                     )
                     OR (attachment.traffic_shuttle_id IS NOT NULL AND EXISTS (
                       SELECT 1
                       FROM traffic_shuttle_booking booking
                       JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
                       WHERE booking.shuttle_service_id = attachment.traffic_shuttle_id
                         AND service_order.purchase_order_id = ?
                         AND service_order.supplier_company_id = ?
                     ))
                   )
                 )
               )
             ORDER BY attachment.created_at DESC, attachment.id DESC
            """,
            (rs, rowNum) -> map(rs),
            companyId,
            orderId,
            orderId,
            orderId,
            companyId,
            orderId,
            companyId
        );
    }

    public List<FulfillmentAttachmentResponse> listForShuttle(Long shuttleId) {
        return jdbcTemplate.query(
            SELECT_COLUMNS + " WHERE attachment.traffic_shuttle_id = ? ORDER BY attachment.node_index, attachment.created_at, attachment.id",
            (rs, rowNum) -> map(rs),
            shuttleId
        );
    }

    public boolean deleteOwnedAttachment(Long companyId, Long attachmentId) {
        return jdbcTemplate.update(
            "DELETE FROM fulfillment_attachment WHERE id = ? AND provider_company_id = ?",
            attachmentId,
            companyId
        ) > 0;
    }

    public FulfillmentAttachmentResponse saveSupplier(
        Long companyId,
        Long userId,
        Long orderId,
        Long supplierOrderId,
        FileSnapshot file
    ) {
        return insert("SUPPLIER", orderId, supplierOrderId, null, null, null, null, null, companyId, userId, file);
    }

    public List<FulfillmentAttachmentResponse> saveShuttle(
        Long companyId,
        Long userId,
        Long shuttleId,
        Integer nodeIndex,
        String nodeName,
        Long requestedBookingId,
        Long requestedTrafficServiceOrderId,
        FileSnapshot file
    ) {
        List<ShuttleLink> links = shuttleLinks(companyId, shuttleId, requestedBookingId, requestedTrafficServiceOrderId);
        if (links.isEmpty()) {
            insert("BARGE", null, null, shuttleId, requestedBookingId, requestedTrafficServiceOrderId, nodeIndex, nodeName, companyId, userId, file);
        } else {
            for (ShuttleLink link : links) {
                insert("BARGE", link.purchaseOrderId(), null, shuttleId, link.bookingId(), link.trafficServiceOrderId(), nodeIndex, nodeName, companyId, userId, file);
            }
        }
        return listForShuttle(shuttleId);
    }

    public Optional<String> changeShuttleStatus(Long companyId, Long shuttleId, String status) {
        int updated = jdbcTemplate.update(
            "UPDATE traffic_shuttle_service SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND supplier_company_id = ?",
            status,
            shuttleId,
            companyId
        );
        if (updated == 0) return Optional.empty();
        return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT status FROM traffic_shuttle_service WHERE id = ?", String.class, shuttleId));
    }

    public void changeRelatedTrafficOrderStatus(Long companyId, Long shuttleId, String status) {
        jdbcTemplate.update("""
            UPDATE traffic_service_order service_order
            JOIN traffic_shuttle_booking booking ON booking.traffic_service_order_id = service_order.id
            JOIN traffic_shuttle_service shuttle ON shuttle.id = booking.shuttle_service_id
            SET service_order.status = ?, service_order.updated_at = CURRENT_TIMESTAMP
            WHERE shuttle.id = ? AND shuttle.supplier_company_id = ?
              AND service_order.status <> 'DISCARDED'
            """, status, shuttleId, companyId);
        String purchaseOrderStatus = switch ((status == null ? "" : status).toUpperCase()) {
            case "IN_PROGRESS" -> "SUPPLYING";
            case "COMPLETED" -> "COMPLETED";
            default -> null;
        };
        if (purchaseOrderStatus == null) return;
        jdbcTemplate.update("""
            UPDATE purchase_order purchase
            JOIN (
              SELECT DISTINCT COALESCE(booking.purchase_order_id, service_order.purchase_order_id) AS purchase_order_id
              FROM traffic_shuttle_booking booking
              JOIN traffic_shuttle_service shuttle ON shuttle.id = booking.shuttle_service_id
              LEFT JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
              WHERE shuttle.id = ? AND shuttle.supplier_company_id = ?
                AND booking.status <> 'CANCELLED'
            ) linked ON linked.purchase_order_id = purchase.id
            SET purchase.status = ?, purchase.updated_at = CURRENT_TIMESTAMP
            """, shuttleId, companyId, purchaseOrderStatus);
    }

    private FulfillmentAttachmentResponse insert(
        String providerType,
        Long purchaseOrderId,
        Long supplierOrderId,
        Long shuttleId,
        Long bookingId,
        Long trafficServiceOrderId,
        Integer nodeIndex,
        String nodeName,
        Long companyId,
        Long userId,
        FileSnapshot file
    ) {
        jdbcTemplate.update("""
            INSERT INTO fulfillment_attachment (
              provider_type, purchase_order_id, supplier_order_id, traffic_shuttle_id,
              booking_id, traffic_service_order_id, node_index, node_name, provider_company_id,
              file_id, file_name, file_url, uploaded_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            providerType, purchaseOrderId, supplierOrderId, shuttleId, bookingId, trafficServiceOrderId,
            nodeIndex, nodeName, companyId, file.fileId(), file.fileName(), file.fileUrl(), userId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return jdbcTemplate.query(
            SELECT_COLUMNS + " WHERE attachment.id = ?",
            (rs, rowNum) -> map(rs),
            id
        ).stream().findFirst().orElseThrow();
    }

    private List<ShuttleLink> shuttleLinks(Long companyId, Long shuttleId, Long requestedBookingId, Long requestedTrafficServiceOrderId) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT service_order.purchase_order_id, booking.id AS booking_id, service_order.id AS traffic_service_order_id
            FROM traffic_shuttle_service shuttle
            JOIN traffic_shuttle_booking booking ON booking.shuttle_service_id = shuttle.id
            JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id
            WHERE shuttle.id = ? AND shuttle.supplier_company_id = ?
              AND service_order.purchase_order_id IS NOT NULL
            """);
        List<Object> args = new ArrayList<>(List.of(shuttleId, companyId));
        if (requestedBookingId != null) {
            sql.append(" AND booking.id = ?");
            args.add(requestedBookingId);
        }
        if (requestedTrafficServiceOrderId != null) {
            sql.append(" AND service_order.id = ?");
            args.add(requestedTrafficServiceOrderId);
        }
        return jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> new ShuttleLink(rs.getLong("purchase_order_id"), rs.getLong("booking_id"), rs.getLong("traffic_service_order_id")),
            args.toArray()
        );
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private FulfillmentAttachmentResponse map(ResultSet rs) throws SQLException {
        return new FulfillmentAttachmentResponse(
            rs.getLong("id"), rs.getString("provider_type"), nullableLong(rs, "purchase_order_id"),
            nullableLong(rs, "supplier_order_id"), nullableLong(rs, "traffic_shuttle_id"),
            nullableLong(rs, "booking_id"), nullableLong(rs, "traffic_service_order_id"), nullableInteger(rs, "node_index"),
            rs.getString("node_name"), rs.getLong("provider_company_id"), rs.getString("provider_name"),
            rs.getString("file_id"), rs.getString("file_name"), rs.getString("file_url"),
            nullableLong(rs, "uploaded_by"), rs.getString("created_at_text")
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void ensureColumn(String column, String definition) {
        Integer exists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE() AND table_name = 'fulfillment_attachment' AND column_name = ?
            """, Integer.class, column);
        if (exists == null || exists == 0) {
            jdbcTemplate.execute("ALTER TABLE fulfillment_attachment ADD COLUMN " + column + " " + definition);
        }
    }

    private void ensureIndex(String indexName, String columns) {
        Integer exists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE table_schema = DATABASE() AND table_name = 'fulfillment_attachment' AND index_name = ?
            """, Integer.class, indexName);
        if (exists == null || exists == 0) {
            jdbcTemplate.execute("ALTER TABLE fulfillment_attachment ADD KEY " + indexName + " (" + columns + ")");
        }
    }

    private record ShuttleLink(Long purchaseOrderId, Long bookingId, Long trafficServiceOrderId) { }
}
