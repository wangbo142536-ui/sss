package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialSupplierSkuRepository implements MaterialSupplierCandidateProvider {

    private static final int MAX_POOL_SIZE = 5000;
    private static final BigDecimal USD_RATE = new BigDecimal("7");

    private final JdbcTemplate jdbcTemplate;

    public MaterialSupplierSkuRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MaterialSupplierCandidate> findOnShelfCandidates() {
        List<SupplierSkuRow> rows = jdbcTemplate.query(
            """
            SELECT s.*,
                   COALESCE(NULLIF(st.shop_name, ''), c.company_name) AS supplier_name
            FROM shop_sku s
            JOIN company c ON c.id = s.company_id
            LEFT JOIN shop_store st ON st.company_id = s.company_id
            WHERE s.product_type = 'MATERIAL'
              AND s.shelf_status = 'ON_SHELF'
              AND c.status = 'ACTIVE'
              AND EXISTS (
                SELECT 1
                FROM sys_user u
                WHERE u.company_id = c.id
                  AND u.status = 'ACTIVE'
                  AND u.username IN ('supplier_a_e2e', 'supplier_e_demo', 'supplier_f_demo')
              )
            ORDER BY s.updated_at DESC, s.id DESC
            LIMIT ?
            """,
            (rs, rowNum) -> supplierSkuRow(rs),
            MAX_POOL_SIZE
        );
        List<Long> skuIds = rows.stream().map(SupplierSkuRow::skuId).toList();
        Map<Long, List<MaterialSupplierSkuAttribute>> attributesBySku = attributesBySkuIds(skuIds);
        Map<Long, SupplierImage> primaryImagesBySku = primaryImagesBySkuIds(skuIds);
        Map<Long, List<MaterialSupplierUnitPriceOption>> unitPricesBySku = unitPriceOptionsBySkuIds(skuIds);
        return rows.stream()
            .map(row -> candidate(
                row,
                attributesBySku.getOrDefault(row.skuId(), List.of()),
                primaryImagesBySku.get(row.skuId()),
                unitPricesBySku.getOrDefault(row.skuId(), List.of())
            ))
            .toList();
    }

    private MaterialSupplierCandidate candidate(
        SupplierSkuRow row,
        List<MaterialSupplierSkuAttribute> attributes,
        SupplierImage image,
        List<MaterialSupplierUnitPriceOption> options
    ) {
        return new MaterialSupplierCandidate(
            row.skuId(),
            row.companyId(),
            row.supplierName(),
            row.supplierSkuCode(),
            row.productName(),
            row.impaCode(),
            row.platformCode(),
            row.categoryCode(),
            row.categoryName(),
            attributes,
            attributeSummary(attributes, row.specificationSummary()),
            row.unitPrice(),
            value(row.currency(), "CNY"),
            currencySymbol(row.currency()),
            row.stockQty(),
            row.stockUnit(),
            row.packing(),
            image == null ? null : image.imageUrl(),
            image == null ? null : image.thumbnailUrl(),
            row.shelfStatus(),
            row.codeStatus(),
            null,
            null
        ).withUnitPriceOptions(options.isEmpty()
            ? fallbackUnitPriceOptions(row.unit(), row.stockUnit(), row.unitPrice())
            : options);
    }

    private SupplierSkuRow supplierSkuRow(ResultSet rs) throws SQLException {
        return new SupplierSkuRow(
            rs.getLong("id"),
            rs.getLong("company_id"),
            rs.getString("supplier_name"),
            rs.getString("supplier_sku_code"),
            rs.getString("product_name"),
            rs.getString("impa_code"),
            rs.getString("platform_code"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            rs.getString("specification_summary"),
            rs.getBigDecimal("unit_price"),
            rs.getString("currency"),
            rs.getBigDecimal("stock_qty"),
            rs.getString("stock_unit"),
            rs.getString("packing"),
            rs.getString("shelf_status"),
            rs.getString("code_status"),
            rs.getString("unit")
        );
    }

    private Map<Long, List<MaterialSupplierSkuAttribute>> attributesBySkuIds(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<MaterialSupplierSkuAttribute>> grouped = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
            SELECT *
            FROM shop_sku_attribute
            WHERE sku_id IN (%s)
            ORDER BY sort_order ASC, id ASC
            """.formatted(placeholders(skuIds.size())),
            (RowCallbackHandler) rs -> grouped.computeIfAbsent(rs.getLong("sku_id"), ignored -> new java.util.ArrayList<>())
                .add(new MaterialSupplierSkuAttribute(
                    rs.getString("attribute_key"),
                    rs.getString("attribute_name"),
                    rs.getString("attribute_value"),
                    rs.getString("attribute_unit"),
                    rs.getInt("sort_order"),
                    rs.getString("raw_text")
                )),
            skuIds.toArray()
        );
        return grouped;
    }

    private Map<Long, SupplierImage> primaryImagesBySkuIds(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SupplierImage> images = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
            SELECT sku_id, image_url, thumbnail_url
            FROM shop_sku_image
            WHERE sku_id IN (%s)
            ORDER BY is_primary DESC, sort_order ASC, id ASC
            """.formatted(placeholders(skuIds.size())),
            (RowCallbackHandler) rs -> images.putIfAbsent(
                rs.getLong("sku_id"),
                new SupplierImage(rs.getString("image_url"), rs.getString("thumbnail_url"))
            ),
            skuIds.toArray()
        );
        return images;
    }

    private Map<Long, List<MaterialSupplierUnitPriceOption>> unitPriceOptionsBySkuIds(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<MaterialSupplierUnitPriceOption>> grouped = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
            SELECT sku_id, unit, unit_price_cny, unit_price_usd, is_default
            FROM shop_sku_unit_price
            WHERE sku_id IN (%s) AND enabled = 1
            ORDER BY is_default DESC, sort_order ASC, id ASC
            """.formatted(placeholders(skuIds.size())),
            (RowCallbackHandler) rs -> grouped.computeIfAbsent(rs.getLong("sku_id"), ignored -> new java.util.ArrayList<>())
                .add(new MaterialSupplierUnitPriceOption(
                    rs.getString("unit"),
                    rs.getBigDecimal("unit_price_cny"),
                    rs.getBigDecimal("unit_price_usd"),
                    rs.getBoolean("is_default")
                )),
            skuIds.toArray()
        );
        return grouped;
    }

    private List<MaterialSupplierUnitPriceOption> fallbackUnitPriceOptions(String unit, String stockUnit, BigDecimal fallbackUnitPrice) {
        if (fallbackUnitPrice == null) {
            return List.of();
        }
        return List.of(new MaterialSupplierUnitPriceOption(
            value(unit, value(stockUnit, null)),
            fallbackUnitPrice,
            fallbackUnitPrice.divide(USD_RATE, 4, RoundingMode.HALF_UP),
            true
        ));
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private String attributeSummary(List<MaterialSupplierSkuAttribute> attributes, String fallback) {
        String summary = attributes.stream()
            .map(attribute -> value(attribute.name(), attribute.key()) + ": " + value(attribute.value(), ""))
            .filter(value -> !value.endsWith(": "))
            .collect(java.util.stream.Collectors.joining(" / "));
        return summary.isBlank() ? fallback : summary;
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String currencySymbol(String currency) {
        return "USD".equalsIgnoreCase(value(currency, "CNY")) ? "$" : "\u00A5";
    }

    private record SupplierImage(String imageUrl, String thumbnailUrl) {
    }

    private record SupplierSkuRow(
        Long skuId,
        Long companyId,
        String supplierName,
        String supplierSkuCode,
        String productName,
        String impaCode,
        String platformCode,
        String categoryCode,
        String categoryName,
        String specificationSummary,
        BigDecimal unitPrice,
        String currency,
        BigDecimal stockQty,
        String stockUnit,
        String packing,
        String shelfStatus,
        String codeStatus,
        String unit
    ) {
    }
}
