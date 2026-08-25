package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
        return lightweightRows(baseSql() + " ORDER BY s.updated_at DESC, s.id DESC LIMIT ?", List.of(MAX_POOL_SIZE)).stream()
            .map(row -> candidate(row, List.of(), null, List.of()))
            .toList();
    }

    @Override
    public boolean supportsTargetedSearch() {
        return true;
    }

    @Override
    public List<MaterialSupplierCandidate> findCandidates(MaterialSupplierCandidateQuery query) {
        MaterialSupplierCandidateQuery safeQuery = query == null ? MaterialSupplierCandidateQuery.empty() : query;
        StringBuilder sql = new StringBuilder(baseSql());
        List<Object> args = new ArrayList<>();
        if (safeQuery.hasEvidence()) {
            List<String> evidenceClauses = new ArrayList<>();
            appendIn(evidenceClauses, args, "s.impa_code", safeQuery.standardCodes());
            appendIn(evidenceClauses, args, "s.platform_code", safeQuery.standardCodes());
            appendIn(evidenceClauses, args, "s.supplier_sku_code", safeQuery.supplierSkuCodes());
            appendIn(evidenceClauses, args, "s.category_code", safeQuery.categoryCodes());
            for (String keyword : safeQuery.keywords()) {
                evidenceClauses.add("(s.product_name LIKE ? ESCAPE '\\\\' OR s.specification_summary LIKE ? ESCAPE '\\\\' OR s.packing LIKE ? ESCAPE '\\\\')");
                String like = "%" + escapeLike(keyword) + "%";
                args.add(like);
                args.add(like);
                args.add(like);
            }
            if (!evidenceClauses.isEmpty()) {
                sql.append(" AND (").append(String.join(" OR ", evidenceClauses)).append(")");
            }
        }
        sql.append(" ORDER BY s.updated_at DESC, s.id DESC");
        return lightweightRows(sql.toString(), args).stream()
            .map(row -> candidate(row, List.of(), null, List.of()))
            .toList();
    }

    @Override
    public List<MaterialSupplierCandidate> enrichCandidates(java.util.Set<Long> requestedSkuIds) {
        List<Long> skuIds = requestedSkuIds == null ? List.of() : requestedSkuIds.stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (skuIds.isEmpty()) {
            return List.of();
        }
        String sql = baseSql() + " AND s.id IN (" + placeholders(skuIds.size()) + ")";
        List<SupplierSkuRow> rows = lightweightRows(sql, new ArrayList<>(skuIds));
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

    private String baseSql() {
        return """
            SELECT s.id, s.company_id, s.supplier_sku_code, s.product_name,
                   s.impa_code, s.platform_code, s.category_code, s.category_name,
                   s.specification_summary, s.unit_price, s.currency, s.stock_qty,
                   s.stock_unit, s.packing, s.shelf_status, s.code_status, s.unit,
                   s.product_tags,
                   COALESCE(NULLIF(st.shop_name, ''), c.company_name) AS supplier_name
            FROM shop_sku s
            JOIN company c ON c.id = s.company_id
            LEFT JOIN shop_store st ON st.company_id = s.company_id
            WHERE s.product_type = 'MATERIAL'
              AND s.shelf_status = 'ON_SHELF'
              AND c.status = 'ACTIVE'
            """;
    }

    private List<SupplierSkuRow> lightweightRows(String sql, Collection<?> args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> supplierSkuRow(rs), args.toArray());
    }

    private void appendIn(List<String> clauses, List<Object> args, String column, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> normalized = values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
        if (normalized.isEmpty()) {
            return;
        }
        clauses.add(column + " IN (" + placeholders(normalized.size()) + ")");
        args.addAll(normalized);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        List<String> tags = new ArrayList<>();
        if (json.contains("质量高")) tags.add("质量高");
        if (json.contains("价格低")) tags.add("价格低");
        return List.copyOf(tags);
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
        ).withProductTags(row.productTags())
         .withUnitPriceOptions(options.isEmpty()
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
            rs.getString("unit"),
            readStringList(rs.getString("product_tags"))
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
        String unit,
        List<String> productTags
    ) {
    }
}
