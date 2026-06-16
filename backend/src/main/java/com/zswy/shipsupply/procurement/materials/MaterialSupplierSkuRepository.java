package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialSupplierSkuRepository implements MaterialSupplierCandidateProvider {

    private static final int MAX_POOL_SIZE = 5000;

    private final JdbcTemplate jdbcTemplate;

    public MaterialSupplierSkuRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MaterialSupplierCandidate> findOnShelfCandidates() {
        return jdbcTemplate.query(
            """
            SELECT s.*,
                   COALESCE(NULLIF(st.shop_name, ''), c.company_name) AS supplier_name
            FROM shop_sku s
            JOIN company c ON c.id = s.company_id
            LEFT JOIN shop_store st ON st.company_id = s.company_id
            WHERE s.product_type = 'MATERIAL'
              AND s.shelf_status = 'ON_SHELF'
              AND c.status = 'ACTIVE'
            ORDER BY s.updated_at DESC, s.id DESC
            LIMIT ?
            """,
            (rs, rowNum) -> candidate(rs),
            MAX_POOL_SIZE
        );
    }

    private MaterialSupplierCandidate candidate(ResultSet rs) throws SQLException {
        long skuId = rs.getLong("id");
        List<MaterialSupplierSkuAttribute> attributes = attributes(skuId);
        SupplierImage image = primaryImage(skuId);
        return new MaterialSupplierCandidate(
            skuId,
            rs.getLong("company_id"),
            rs.getString("supplier_name"),
            rs.getString("supplier_sku_code"),
            rs.getString("product_name"),
            rs.getString("impa_code"),
            rs.getString("platform_code"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            attributes,
            attributeSummary(attributes, rs.getString("specification_summary")),
            rs.getBigDecimal("unit_price"),
            value(rs.getString("currency"), "CNY"),
            currencySymbol(rs.getString("currency")),
            rs.getBigDecimal("stock_qty"),
            rs.getString("stock_unit"),
            rs.getString("packing"),
            image == null ? null : image.imageUrl(),
            image == null ? null : image.thumbnailUrl(),
            rs.getString("shelf_status"),
            rs.getString("code_status"),
            null,
            null
        );
    }

    private List<MaterialSupplierSkuAttribute> attributes(long skuId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM shop_sku_attribute
            WHERE sku_id = ?
            ORDER BY sort_order ASC, id ASC
            """,
            (rs, rowNum) -> new MaterialSupplierSkuAttribute(
                rs.getString("attribute_key"),
                rs.getString("attribute_name"),
                rs.getString("attribute_value"),
                rs.getString("attribute_unit"),
                rs.getInt("sort_order"),
                rs.getString("raw_text")
            ),
            skuId
        );
    }

    private SupplierImage primaryImage(long skuId) {
        return jdbcTemplate.query(
            """
            SELECT image_url, thumbnail_url
            FROM shop_sku_image
            WHERE sku_id = ?
            ORDER BY is_primary DESC, sort_order ASC, id ASC
            LIMIT 1
            """,
            (rs, rowNum) -> new SupplierImage(rs.getString("image_url"), rs.getString("thumbnail_url")),
            skuId
        ).stream().findFirst().orElse(null);
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
}
