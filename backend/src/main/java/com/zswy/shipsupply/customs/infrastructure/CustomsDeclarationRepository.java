package com.zswy.shipsupply.customs.infrastructure;

import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationItem;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationPage;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class CustomsDeclarationRepository {
    private final JdbcTemplate jdbc;

    public CustomsDeclarationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isPlatformAdmin(Long userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id
            WHERE user_role.user_id = ? AND role.role_code = 'PLATFORM_ADMIN' AND role.enabled = 1
            """, Integer.class, userId);
        return count != null && count > 0;
    }

    public SourceOrder requireSource(String businessType, Long purchaseOrderId) {
        List<SourceOrder> rows = "FOOD".equals(businessType)
            ? jdbc.query(foodSourceSql(), this::mapSource, purchaseOrderId)
            : jdbc.query(materialSourceSql(), this::mapSource, purchaseOrderId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CUSTOMS_ORDER_NOT_FOUND");
        SourceOrder source = rows.get(0);
        if (source.responsibleCompanyId() == null || source.responsibleType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CUSTOMS_RESPONSIBILITY_NOT_SNAPSHOTTED");
        }
        CompanyContact contact = companyContact(source.responsibleCompanyId());
        return source.withResponsible(contact);
    }

    public List<DeclarationItem> listItems(String businessType, Long purchaseOrderId) {
        if ("FOOD".equals(businessType)) {
            return jdbc.query("""
                SELECT item.id, COALESCE(item.name_en, item.name_zh, '') AS code,
                       COALESCE(item.name_zh, item.name_en, '') AS product_name,
                       item.specification, CAST(item.ordered_quantity AS CHAR) AS quantity, item.unit
                FROM food_purchase_order_item item
                WHERE item.order_id = ? ORDER BY item.id
                """, this::mapItem, purchaseOrderId);
        }
        return jdbc.query("""
            SELECT item.id, COALESCE(item.impa_code, item.platform_code, item.supplier_sku_code, '') AS code,
                   item.product_name, item.specification, item.quantity, item.unit
            FROM purchase_order_item item
            WHERE item.order_id = ? ORDER BY item.id
            """, this::mapItem, purchaseOrderId);
    }

    public void insertIfAbsent(SourceOrder source, Long userId) {
        jdbc.update("""
            INSERT IGNORE INTO customs_declaration_demo (
              business_type, purchase_order_id, purchase_order_no, buyer_company_id,
              responsible_company_id, responsible_type, responsible_company_name, status,
              vessel_name, ship_agent, goods_category, trade_type, declarant_company,
              declarant_contact, declarant_phone, delivery_start, delivery_end, delivery_location,
              supply_vessel, captain_contact, applicant, applicant_phone, application_date,
              customs_fee, attachment_code, declared_by, declared_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DECLARED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                      'ATTACHMENT_ONE', ?, CURRENT_TIMESTAMP)
            """,
            source.businessType(), source.purchaseOrderId(), source.purchaseOrderNo(), source.buyerCompanyId(),
            source.responsibleCompanyId(), source.responsibleType(), source.responsibleCompanyName(),
            source.vesselName(), source.shipAgent(), source.goodsCategory(), source.tradeType(),
            source.responsibleCompanyName(), source.declarantContact(), source.declarantPhone(),
            source.deliveryStart(), source.deliveryEnd(), source.deliveryLocation(), source.supplyVessel(),
            source.captainContact(), source.applicant(), source.applicantPhone(), source.applicationDate(),
            source.customsFee(), userId
        );
    }

    public Optional<DeclarationRecord> find(String businessType, Long purchaseOrderId) {
        return jdbc.query(baseSelect() + " WHERE declaration.business_type = ? AND declaration.purchase_order_id = ?",
            this::mapRecord, businessType, purchaseOrderId).stream().findFirst();
    }

    public Optional<DeclarationRecord> findById(Long id) {
        return jdbc.query(baseSelect() + " WHERE declaration.id = ?", this::mapRecord, id).stream().findFirst();
    }

    public DeclarationPage list(
        Long companyId, boolean platformAdmin, String keyword, String status, LocalDate deliveryDate,
        int page, int size
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!platformAdmin) {
            where.append(" AND (declaration.buyer_company_id = ? OR declaration.responsible_company_id = ?)");
            args.add(companyId); args.add(companyId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (declaration.purchase_order_no LIKE ? OR declaration.vessel_name LIKE ? OR declaration.declarant_company LIKE ?)");
            String value = "%" + keyword.trim() + "%";
            args.add(value); args.add(value); args.add(value);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND declaration.status = ?");
            args.add(status);
        }
        if (deliveryDate != null) {
            where.append(" AND LEFT(declaration.delivery_start, 10) = ?");
            args.add(deliveryDate.toString());
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM customs_declaration_demo declaration" + where,
            Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size); listArgs.add((page - 1) * size);
        List<DeclarationRecord> items = jdbc.query(
            baseSelect() + where + " ORDER BY declaration.declared_at DESC, declaration.id DESC LIMIT ? OFFSET ?",
            this::mapRecord, listArgs.toArray()
        );
        return new DeclarationPage(items, total == null ? 0 : total, page, size);
    }

    private String materialSourceSql() {
        return """
            SELECT 'MATERIAL' AS business_type, po.id AS purchase_order_id, po.order_no,
                   po.buyer_company_id, po.buyer_company_name AS ship_agent, po.vessel_name,
                   COALESCE(md.material_type, '物料') AS goods_category, '外贸' AS trade_type,
                   COALESCE(md.recipient_company, po.buyer_company_name) AS recipient_company,
                   po.delivery_contact_name AS applicant, po.delivery_contact_phone AS applicant_phone,
                   po.required_delivery_time AS delivery_start, po.required_delivery_time AS delivery_end,
                   po.supply_port AS delivery_location, md.fixed_customs_fee AS customs_fee,
                   UPPER(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType')), '')) AS responsible_type,
                   CASE
                     WHEN UPPER(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType'))) = 'SUPPLIER'
                       THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderId')) AS UNSIGNED)
                     WHEN UPPER(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderType'))) = 'BARGE'
                       THEN (SELECT shuttle.supplier_company_id FROM traffic_shuttle_service shuttle
                             WHERE shuttle.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderId')) AS UNSIGNED)
                             LIMIT 1)
                   END AS responsible_company_id,
                   COALESCE(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.fixedProviderName')), '') AS responsible_company_name,
                   COALESCE(JSON_UNQUOTE(JSON_EXTRACT(md.traffic_service_json, '$.trafficVesselName')), '') AS supply_vessel,
                   '' AS captain_contact, DATE(po.created_at) AS application_date
            FROM purchase_order po
            JOIN material_demand md ON md.id = po.demand_id
            WHERE po.id = ?
            """;
    }

    private String foodSourceSql() {
        return """
            SELECT 'FOOD' AS business_type, po.id AS purchase_order_id, po.order_no,
                   po.buyer_company_id, buyer.company_name AS ship_agent, demand.vessel_name,
                   '伙食' AS goods_category, '外贸' AS trade_type,
                   buyer.company_name AS recipient_company,
                   po.delivery_contact_name AS applicant, po.delivery_contact_phone AS applicant_phone,
                   CAST(po.required_delivery_time AS CHAR) AS delivery_start,
                   CAST(po.required_delivery_time AS CHAR) AS delivery_end,
                   COALESCE(po.delivery_address, demand.supply_port) AS delivery_location,
                   po.fixed_customs_fee AS customs_fee, UPPER(COALESCE(po.fixed_provider_type, '')) AS responsible_type,
                   CASE
                     WHEN UPPER(po.fixed_provider_type) = 'SUPPLIER' THEN CAST(po.fixed_provider_id AS UNSIGNED)
                     WHEN UPPER(po.fixed_provider_type) = 'BARGE'
                       THEN (SELECT shuttle.supplier_company_id FROM traffic_shuttle_service shuttle
                             WHERE shuttle.id = CAST(po.fixed_provider_id AS UNSIGNED) LIMIT 1)
                   END AS responsible_company_id,
                   COALESCE(po.fixed_provider_name, '') AS responsible_company_name,
                   COALESCE(JSON_UNQUOTE(JSON_EXTRACT(po.traffic_service_json, '$.trafficVesselName')), '') AS supply_vessel,
                   '' AS captain_contact, DATE(po.created_at) AS application_date
            FROM food_purchase_order po
            JOIN food_demand demand ON demand.id = po.demand_id
            JOIN company buyer ON buyer.id = po.buyer_company_id
            WHERE po.id = ?
            """;
    }

    private SourceOrder mapSource(ResultSet rs, int rowNum) throws SQLException {
        Long responsible = nullableLong(rs, "responsible_company_id");
        return new SourceOrder(
            rs.getString("business_type"), rs.getLong("purchase_order_id"), rs.getString("order_no"),
            rs.getLong("buyer_company_id"), responsible, safe(rs, "responsible_type"),
            safe(rs, "responsible_company_name"), safe(rs, "vessel_name"), safe(rs, "ship_agent"),
            safe(rs, "goods_category"), safe(rs, "trade_type"), safe(rs, "recipient_company"),
            "", "", safe(rs, "delivery_start"), safe(rs, "delivery_end"), safe(rs, "delivery_location"),
            safe(rs, "supply_vessel"), safe(rs, "captain_contact"), safe(rs, "applicant"),
            safe(rs, "applicant_phone"), rs.getObject("application_date", LocalDate.class),
            Optional.ofNullable(rs.getBigDecimal("customs_fee")).orElse(BigDecimal.ZERO)
        );
    }

    private DeclarationItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new DeclarationItem(rs.getLong("id"), safe(rs, "code"), safe(rs, "product_name"),
            safe(rs, "specification"), safe(rs, "quantity"), safe(rs, "unit"));
    }

    private DeclarationRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        Date applicationDate = rs.getDate("application_date");
        return new DeclarationRecord(
            rs.getLong("id"), rs.getString("business_type"), rs.getLong("purchase_order_id"),
            rs.getString("purchase_order_no"), rs.getLong("buyer_company_id"), rs.getLong("responsible_company_id"),
            rs.getString("responsible_type"), safe(rs, "responsible_company_name"), rs.getString("status"),
            safe(rs, "vessel_name"), safe(rs, "ship_agent"), safe(rs, "goods_category"), safe(rs, "trade_type"),
            safe(rs, "declarant_company"), safe(rs, "declarant_contact"), safe(rs, "declarant_phone"),
            safe(rs, "delivery_start"), safe(rs, "delivery_end"), safe(rs, "delivery_location"),
            safe(rs, "supply_vessel"), safe(rs, "captain_contact"), safe(rs, "applicant"),
            safe(rs, "applicant_phone"), applicationDate == null ? null : applicationDate.toLocalDate(),
            Optional.ofNullable(rs.getBigDecimal("customs_fee")).orElse(BigDecimal.ZERO),
            rs.getObject("declared_at", LocalDateTime.class)
        );
    }

    private CompanyContact companyContact(Long companyId) {
        return jdbc.query("SELECT company_name, contact_name, contact_phone FROM company WHERE id = ?",
            (rs, rowNum) -> new CompanyContact(rs.getString(1), rs.getString(2), rs.getString(3)), companyId)
            .stream().findFirst().orElse(new CompanyContact("", "", ""));
    }

    private String baseSelect() { return "SELECT declaration.* FROM customs_declaration_demo declaration"; }
    private String safe(ResultSet rs, String column) throws SQLException { String value = rs.getString(column); return value == null ? "" : value; }
    private Long nullableLong(ResultSet rs, String column) throws SQLException { long value = rs.getLong(column); return rs.wasNull() ? null : value; }

    public record SourceOrder(
        String businessType, Long purchaseOrderId, String purchaseOrderNo, Long buyerCompanyId,
        Long responsibleCompanyId, String responsibleType, String responsibleCompanyName,
        String vesselName, String shipAgent, String goodsCategory, String tradeType, String recipientCompany,
        String declarantContact, String declarantPhone, String deliveryStart, String deliveryEnd,
        String deliveryLocation, String supplyVessel, String captainContact, String applicant,
        String applicantPhone, LocalDate applicationDate, BigDecimal customsFee
    ) {
        public boolean canView(Long companyId) {
            return buyerCompanyId.equals(companyId) || responsibleCompanyId.equals(companyId);
        }
        SourceOrder withResponsible(CompanyContact contact) {
            String name = responsibleCompanyName == null || responsibleCompanyName.isBlank() ? contact.name() : responsibleCompanyName;
            return new SourceOrder(businessType, purchaseOrderId, purchaseOrderNo, buyerCompanyId,
                responsibleCompanyId, responsibleType, name, vesselName, shipAgent, goodsCategory, tradeType,
                recipientCompany, contact.contactName(), contact.contactPhone(), deliveryStart, deliveryEnd,
                deliveryLocation, supplyVessel, captainContact, applicant, applicantPhone, applicationDate, customsFee);
        }
    }

    private record CompanyContact(String name, String contactName, String contactPhone) {}
}
