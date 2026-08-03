package com.zswy.shipsupply.procurement.food.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FoodComparisonQuoteRepository {

    private final JdbcTemplate jdbcTemplate;

    public FoodComparisonQuoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ComparisonQuoteRow> rows(long demandId, long buyerCompanyId, List<Long> quoteItemIds) {
        if (quoteItemIds == null || quoteItemIds.isEmpty()) return List.of();
        StringJoiner placeholders = new StringJoiner(",");
        quoteItemIds.forEach(ignored -> placeholders.add("?"));
        List<Object> arguments = new ArrayList<>();
        arguments.add(demandId);
        arguments.add(buyerCompanyId);
        arguments.addAll(quoteItemIds);
        return jdbcTemplate.query(
            """
            SELECT qi.id quote_item_id, di.id demand_item_id, di.sequence_no, di.name_zh, di.name_en, di.specification, di.unit,
                   di.requested_quantity, supplier.company_name supplier_name,
                   COALESCE(override_price.quoted_quantity, qi.quoted_quantity) quoted_quantity,
                   COALESCE(override_price.unit_price, qi.unit_price) unit_price,
                   COALESCE(override_price.supplier_remark, qi.supplier_remark) supplier_remark
            FROM food_supplier_quote_item qi
            JOIN food_supplier_quote q ON q.id = qi.quote_id AND q.status = 'SUBMITTED'
            JOIN food_demand_item di ON di.id = qi.demand_item_id
            JOIN company supplier ON supplier.id = q.supplier_company_id
            LEFT JOIN food_comparison_quote_override override_price
              ON override_price.demand_id = q.demand_id AND override_price.quote_item_id = qi.id
            WHERE q.demand_id = ? AND q.buyer_company_id = ?
              AND qi.id IN (""" + placeholders + ") ORDER BY di.sequence_no, qi.id",
            (rs, rowNum) -> new ComparisonQuoteRow(
                rs.getLong("quote_item_id"), rs.getLong("demand_item_id"), rs.getInt("sequence_no"), rs.getString("name_zh"),
                rs.getString("name_en"), rs.getString("specification"), rs.getString("unit"),
                rs.getBigDecimal("requested_quantity"), rs.getString("supplier_name"),
                rs.getBigDecimal("quoted_quantity"), rs.getBigDecimal("unit_price"), rs.getString("supplier_remark")
            ),
            arguments.toArray()
        );
    }

    public int saveOverrides(long demandId, long userId, List<ComparisonQuoteUpdate> updates) {
        int updated = 0;
        for (ComparisonQuoteUpdate item : updates) {
            jdbcTemplate.update(
                "UPDATE food_demand_item SET requested_quantity = ? WHERE id = ? AND demand_id = ?",
                item.requestedQuantity(), item.demandItemId(), demandId
            );
            jdbcTemplate.update(
                """
                UPDATE food_supplier_quote_item
                SET requested_quantity = ?
                WHERE demand_item_id = ?
                  AND quote_id IN (SELECT id FROM food_supplier_quote WHERE demand_id = ?)
                """,
                item.requestedQuantity(), item.demandItemId(), demandId
            );
            updated += jdbcTemplate.update(
                """
                INSERT INTO food_comparison_quote_override
                  (demand_id, quote_item_id, quoted_quantity, unit_price, supplier_remark, updated_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  quoted_quantity = VALUES(quoted_quantity), unit_price = VALUES(unit_price),
                  supplier_remark = VALUES(supplier_remark), updated_by = VALUES(updated_by),
                  updated_at = CURRENT_TIMESTAMP
                """,
                demandId, item.quoteItemId(), item.quotedQuantity(), item.unitPrice(), item.remark(), userId
            );
        }
        return updated;
    }

    public record ComparisonQuoteRow(
        long quoteItemId,
        long demandItemId,
        int sequenceNo,
        String nameZh,
        String nameEn,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        String supplierName,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        String remark
    ) {
    }

    public record ComparisonQuoteUpdate(
        long quoteItemId,
        long demandItemId,
        BigDecimal requestedQuantity,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        String remark
    ) {
    }
}
