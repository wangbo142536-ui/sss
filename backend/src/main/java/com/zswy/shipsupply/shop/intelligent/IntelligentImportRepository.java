package com.zswy.shipsupply.shop.intelligent;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IntelligentImportRepository {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public IntelligentImportRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    long createBatch(long companyId, long shopId, long userId, String jobId, String fileName, String fileSha256) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO shop_sku_import_batch
                  (job_id, company_id, shop_id, source_file_name, source_file_sha256,
                   import_type, status, stage, overall_percent, created_by, started_at)
                VALUES (?, ?, ?, ?, ?, 'INTELLIGENT_SKU', 'QUEUED', 'UPLOAD', 0, ?, CURRENT_TIMESTAMP)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, jobId);
            statement.setLong(2, companyId);
            statement.setLong(3, shopId);
            statement.setString(4, fileName);
            statement.setString(5, fileSha256);
            statement.setLong(6, userId);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    void progress(
        long batchId,
        String status,
        String stage,
        int percent,
        String currentSheet,
        int currentChunk,
        int totalChunks,
        ImportCounters counters,
        String error
    ) {
        jdbc.update(
            """
            UPDATE shop_sku_import_batch
            SET status = ?, stage = ?, overall_percent = ?, current_sheet = ?, current_chunk = ?, total_chunks = ?,
                sheet_total = ?, sheet_processed = ?, total_count = ?, success_count = ?, exception_count = ?,
                material_count = ?, food_count = ?, impa_matched_count = ?, category_matched_count = ?,
                pending_review_count = ?, image_total = ?, image_processed = ?, failed_count = ?,
                error_message = ?, event_seq = event_seq + 1,
                finished_at = CASE WHEN ? IN ('COMPLETED','PARTIAL','FAILED') THEN CURRENT_TIMESTAMP ELSE finished_at END
            WHERE id = ?
            """,
            status,
            stage,
            Math.max(0, Math.min(100, percent)),
            currentSheet,
            currentChunk,
            totalChunks,
            counters.sheetTotal(),
            counters.sheetProcessed(),
            counters.itemTotal(),
            counters.itemProcessed() - counters.failedCount(),
            counters.pendingReviewCount() + counters.failedCount(),
            counters.materialCount(),
            counters.foodCount(),
            counters.impaMatchedCount(),
            counters.categoryMatchedCount(),
            counters.pendingReviewCount(),
            counters.imageTotal(),
            counters.imageProcessed(),
            counters.failedCount(),
            error,
            status,
            batchId
        );
    }

    Optional<ImportBatchRow> findBatch(long companyId, String jobId) {
        return jdbc.query(
            "SELECT * FROM shop_sku_import_batch WHERE company_id = ? AND job_id = ?",
            (rs, rowNum) -> batch(rs),
            companyId,
            jobId
        ).stream().findFirst();
    }

    Optional<ResolvedStandardItem> findImpa(String rawCode) {
        String normalized = rawCode == null ? "" : rawCode.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return jdbc.query(
            """
            SELECT item.id, item.impa_code, item.cn_code, item.category_code,
                   COALESCE(zh.description, en.description, item.impa_code) AS item_name,
                   COALESCE(category.category_name_cn, item.category_code) AS category_name,
                   COALESCE(zh.specification, en.specification) AS specification
            FROM impa_item item
            LEFT JOIN impa_item_i18n zh ON zh.impa_code = item.impa_code AND zh.language = 'zh-CN'
            LEFT JOIN impa_item_i18n en ON en.impa_code = item.impa_code AND en.language = 'en-US'
            LEFT JOIN impa_category category ON category.category_code = item.category_code
            WHERE item.enabled = 1 AND (item.impa_code = ? OR item.cn_code = ?)
            LIMIT 1
            """,
            (rs, rowNum) -> standardItem(rs),
            normalized,
            normalized
        ).stream().findFirst();
    }

    Optional<String> impaCategoryName(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) return Optional.empty();
        return jdbc.query(
            "SELECT category_name_cn FROM impa_category WHERE category_code = ? AND enabled = 1 LIMIT 1",
            (rs, rowNum) -> rs.getString(1),
            categoryCode
        ).stream().findFirst();
    }

    ProvisionCategory provisionCategory(String code) {
        return jdbc.query(
            "SELECT category_code, category_name_cn FROM provision_category WHERE category_code = ? AND enabled = 1",
            (rs, rowNum) -> new ProvisionCategory(rs.getString(1), rs.getString(2)),
            code
        ).stream().findFirst().orElse(new ProvisionCategory("PROVISION_OTHER", "其他伙食"));
    }

    long savePreview(long batchId, long companyId, long shopId, ResolvedImportRow row) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO shop_sku_import_preview_row
                  (batch_id, company_id, shop_id, row_no, product_type, source_sheet, source_item_id,
                   standard_library_type, standard_category_code, impa_item_id,
                   supplier_sku_code, product_name, category_code, category_name, platform_code, impa_code,
                   specification_summary, stock_qty, stock_unit, unit_price, currency, packing, barcode,
                   code_status, exception_reason, image_file_id, image_url, thumbnail_url,
                   raw_name, raw_spec, clean_name, parsed_attributes_json, logic_recommendation_json,
                   selected_recommendation_source, review_required, match_decision, raw_row_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, CAST('[]' AS JSON), CAST(? AS JSON), ?, ?, ?, CAST(? AS JSON))
                ON DUPLICATE KEY UPDATE
                  source_item_id = VALUES(source_item_id), product_type = VALUES(product_type),
                  standard_library_type = VALUES(standard_library_type),
                  supplier_sku_code = VALUES(supplier_sku_code), product_name = VALUES(product_name),
                  impa_item_id = VALUES(impa_item_id), impa_code = VALUES(impa_code),
                  standard_category_code = VALUES(standard_category_code), category_code = VALUES(category_code),
                  category_name = VALUES(category_name), platform_code = VALUES(platform_code),
                  specification_summary = VALUES(specification_summary), stock_qty = VALUES(stock_qty),
                  stock_unit = VALUES(stock_unit), unit_price = VALUES(unit_price), currency = VALUES(currency),
                  packing = VALUES(packing), barcode = VALUES(barcode), code_status = VALUES(code_status),
                  exception_reason = VALUES(exception_reason), image_file_id = VALUES(image_file_id),
                  image_url = VALUES(image_url), thumbnail_url = VALUES(thumbnail_url), raw_row_json = VALUES(raw_row_json),
                  raw_name = VALUES(raw_name), raw_spec = VALUES(raw_spec), clean_name = VALUES(clean_name),
                  logic_recommendation_json = VALUES(logic_recommendation_json),
                  selected_recommendation_source = VALUES(selected_recommendation_source),
                  review_required = VALUES(review_required), match_decision = VALUES(match_decision),
                  id = LAST_INSERT_ID(id)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            int index = 1;
            statement.setLong(index++, batchId);
            statement.setLong(index++, companyId);
            statement.setLong(index++, shopId);
            statement.setInt(index++, row.sourceRowNo());
            statement.setString(index++, row.productType());
            statement.setString(index++, row.sourceSheet());
            statement.setString(index++, row.sourceItemId());
            statement.setString(index++, row.standardLibraryType());
            statement.setString(index++, row.standardCategoryCode());
            if (row.impaItemId() == null) statement.setNull(index++, java.sql.Types.BIGINT); else statement.setLong(index++, row.impaItemId());
            statement.setString(index++, row.supplierSkuCode());
            statement.setString(index++, row.productName());
            statement.setString(index++, row.categoryCode());
            statement.setString(index++, row.categoryName());
            statement.setString(index++, row.impaCode());
            statement.setString(index++, row.impaCode());
            statement.setString(index++, row.specification());
            statement.setBigDecimal(index++, row.stockQty());
            statement.setString(index++, row.unit());
            statement.setBigDecimal(index++, row.unitPrice());
            statement.setString(index++, row.currency());
            statement.setString(index++, row.packing());
            statement.setString(index++, row.barcode());
            statement.setString(index++, row.codeStatus());
            statement.setString(index++, row.exceptionReason());
            statement.setString(index++, row.imageFileId());
            statement.setString(index++, row.imageUrl());
            statement.setString(index++, row.imageUrl());
            statement.setString(index++, row.productName());
            statement.setString(index++, row.specification());
            statement.setString(index++, row.productName());
            statement.setString(index++, json(row.standardSnapshot()));
            statement.setString(index++, row.impaItemId() == null ? "NONE" : "STANDARD_LIBRARY");
            statement.setInt(index++, row.reviewRequired() ? 1 : 0);
            statement.setString(index++, row.matchDecision());
            statement.setString(index, json(row.rawColumns()));
            return statement;
        }, keys);
        long previewId = keys.getKey() == null ? 0 : keys.getKey().longValue();
        jdbc.update(
            """
            INSERT INTO shop_sku_import_item(batch_id, source_item_id, source_sheet, source_row_no, action, raw_row_hash)
            VALUES (?, ?, ?, ?, 'PREVIEW', ?)
            ON DUPLICATE KEY UPDATE source_sheet = VALUES(source_sheet), source_row_no = VALUES(source_row_no),
              raw_row_hash = VALUES(raw_row_hash), updated_at = CURRENT_TIMESTAMP
            """,
            batchId,
            row.sourceItemId(),
            row.sourceSheet(),
            row.sourceRowNo(),
            row.rawRowHash()
        );
        return previewId;
    }

    List<IntelligentImportPreviewRow> previewRows(long companyId, long batchId) {
        return jdbc.query(
            "SELECT * FROM shop_sku_import_preview_row WHERE company_id = ? AND batch_id = ? ORDER BY source_sheet, row_no, id",
            (rs, rowNum) -> preview(rs),
            companyId,
            batchId
        );
    }

    int previewRowCount(long batchId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM shop_sku_import_preview_row WHERE batch_id = ?",
            Integer.class,
            batchId
        );
        return count == null ? 0 : count;
    }

    @Transactional
    ExecutionResult execute(long companyId, long userId, long shopId, long batchId, List<Long> selectedIds) {
        List<IntelligentImportPreviewRow> rows = previewRows(companyId, batchId).stream()
            .filter(row -> selectedIds == null || selectedIds.isEmpty() || selectedIds.contains(row.previewRowId()))
            .toList();
        int inserted = 0;
        int pending = 0;
        for (IntelligentImportPreviewRow row : rows) {
            if (("MATERIAL".equals(row.productType()) && (row.impaCode() == null || row.impaCode().isBlank()))
                || ("FOOD".equals(row.productType()) && (row.standardCategoryCode() == null || row.standardCategoryCode().isBlank()))) {
                pending++;
            }
            Long existingSkuId = jdbc.query(
                "SELECT sku_id FROM shop_sku_import_item WHERE batch_id = ? AND source_item_id = ? AND sku_id IS NOT NULL",
                (rs, rowNum) -> rs.getLong(1),
                batchId,
                row.sourceItemId()
            ).stream().findFirst().orElse(null);
            if (existingSkuId != null) {
                continue;
            }
            long skuId = insertSku(companyId, shopId, userId, batchId, row);
            jdbc.update(
                "UPDATE shop_sku_import_item SET sku_id = ?, action = 'INSERTED' WHERE batch_id = ? AND source_item_id = ?",
                skuId,
                batchId,
                row.sourceItemId()
            );
            jdbc.update("UPDATE shop_sku_import_preview_row SET confirmed_at = CURRENT_TIMESTAMP WHERE id = ?", row.previewRowId());
            inserted++;
        }
        return new ExecutionResult(rows.size(), inserted, pending);
    }

    private long insertSku(long companyId, long shopId, long userId, long batchId, IntelligentImportPreviewRow row) {
        Long impaItemId = row.impaCode() == null ? null : jdbc.query(
            "SELECT id FROM impa_item WHERE impa_code = ? AND enabled = 1",
            (rs, rowNum) -> rs.getLong(1),
            row.impaCode()
        ).stream().findFirst().orElse(null);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO shop_sku
                  (company_id, shop_id, product_type, standard_library_type, standard_category_code,
                   category_code, category_name, platform_code, impa_code, impa_item_id,
                   supplier_sku_code, product_name, specification_summary, normalized_name,
                   normalized_specification, stock_qty, stock_unit, unit_price, currency, unit,
                   packing, barcode, shelf_status, code_status, exception_reason, import_batch_id,
                   import_row_no, raw_row_json, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        'OFF_SHELF', ?, ?, ?, ?, CAST(? AS JSON), ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            int index = 1;
            statement.setLong(index++, companyId);
            statement.setLong(index++, shopId);
            statement.setString(index++, row.productType());
            statement.setString(index++, row.standardLibraryType());
            statement.setString(index++, row.standardCategoryCode());
            statement.setString(index++, row.categoryCode());
            statement.setString(index++, row.categoryName());
            statement.setString(index++, row.impaCode());
            statement.setString(index++, row.impaCode());
            if (impaItemId == null) statement.setNull(index++, java.sql.Types.BIGINT); else statement.setLong(index++, impaItemId);
            statement.setString(index++, row.supplierSkuCode());
            statement.setString(index++, row.productName());
            statement.setString(index++, row.attributeSummary());
            statement.setString(index++, normalize(row.productName()));
            statement.setString(index++, normalize(row.attributeSummary()));
            statement.setBigDecimal(index++, row.stockQty());
            statement.setString(index++, row.stockUnit());
            statement.setBigDecimal(index++, row.unitPrice());
            statement.setString(index++, row.currency());
            statement.setString(index++, row.stockUnit());
            statement.setString(index++, row.packageSpec());
            statement.setString(index++, row.barcode());
            statement.setString(index++, row.codeStatus());
            statement.setString(index++, row.exceptionReason());
            statement.setLong(index++, batchId);
            statement.setInt(index++, row.rowNo());
            statement.setString(index++, json(row.rawColumns()));
            statement.setLong(index++, userId);
            statement.setLong(index, userId);
            return statement;
        }, keys);
        long skuId = keys.getKey().longValue();
        if (row.attributeSummary() != null && !row.attributeSummary().isBlank()) {
            jdbc.update(
                "INSERT INTO shop_sku_attribute(sku_id, attribute_key, attribute_name, attribute_value, sort_order, raw_text) VALUES (?, 'specification', '规格', ?, 0, ?)",
                skuId,
                row.attributeSummary(),
                row.attributeSummary()
            );
        }
        if (row.imageFileId() != null || row.imageUrl() != null) {
            jdbc.update(
                "INSERT INTO shop_sku_image(sku_id, file_id, image_url, thumbnail_url, is_primary, sort_order) VALUES (?, ?, ?, ?, 1, 0)",
                skuId,
                row.imageFileId(),
                row.imageUrl(),
                row.thumbnailUrl()
            );
        }
        return skuId;
    }

    private ImportBatchRow batch(ResultSet rs) throws SQLException {
        return new ImportBatchRow(
            rs.getLong("id"), rs.getString("job_id"), rs.getString("status"), rs.getString("stage"),
            rs.getInt("overall_percent"), rs.getString("current_sheet"), rs.getInt("current_chunk"),
            rs.getInt("total_chunks"), counters(rs), rs.getString("error_message"),
            time(rs.getObject("started_at", LocalDateTime.class)), time(rs.getObject("finished_at", LocalDateTime.class))
        );
    }

    private ImportCounters counters(ResultSet rs) throws SQLException {
        return new ImportCounters(
            rs.getInt("sheet_total"), rs.getInt("sheet_processed"), rs.getInt("total_count"),
            rs.getInt("success_count") + rs.getInt("failed_count"), rs.getInt("material_count"),
            rs.getInt("food_count"), rs.getInt("impa_matched_count"), rs.getInt("category_matched_count"),
            rs.getInt("pending_review_count"), rs.getInt("image_total"), rs.getInt("image_processed"),
            rs.getInt("failed_count")
        );
    }

    private ResolvedStandardItem standardItem(ResultSet rs) throws SQLException {
        return new ResolvedStandardItem(
            rs.getLong("id"), rs.getString("impa_code"), rs.getString("cn_code"),
            rs.getString("category_code"), rs.getString("category_name"), rs.getString("item_name"),
            rs.getString("specification")
        );
    }

    private IntelligentImportPreviewRow preview(ResultSet rs) throws SQLException {
        return new IntelligentImportPreviewRow(
            rs.getLong("id"), rs.getString("source_item_id"), rs.getString("source_sheet"), rs.getInt("row_no"),
            rs.getString("product_type"), rs.getString("standard_library_type"), rs.getString("standard_category_code"),
            rs.getString("category_code"), rs.getString("category_name"), rs.getString("platform_code"),
            rs.getString("impa_code"), rs.getString("supplier_sku_code"), rs.getString("product_name"),
            rs.getString("specification_summary"), rs.getBigDecimal("stock_qty"), rs.getString("stock_unit"),
            rs.getBigDecimal("unit_price"), rs.getString("currency"), rs.getString("packing"), rs.getString("barcode"),
            rs.getString("image_file_id"), rs.getString("image_url"), rs.getString("thumbnail_url"), "OFF_SHELF",
            rs.getString("code_status"), rs.getString("exception_reason"), "INSERT", readMap(rs.getString("raw_row_json"))
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.replaceAll("[\\p{Punct}\\s]+", "").toUpperCase(java.util.Locale.ROOT);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("INTELLIGENT_IMPORT_JSON_FAILED", ex);
        }
    }

    private Map<String, String> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, STRING_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String time(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}

record ImportBatchRow(
    long batchId, String jobId, String status, String stage, int percent, String currentSheet,
    int currentChunk, int totalChunks, ImportCounters counters, String error, String startedAt, String finishedAt
) { }

record ImportCounters(
    int sheetTotal, int sheetProcessed, int itemTotal, int itemProcessed, int materialCount,
    int foodCount, int impaMatchedCount, int categoryMatchedCount, int pendingReviewCount,
    int imageTotal, int imageProcessed, int failedCount
) {
    static ImportCounters empty() { return new ImportCounters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
}

record ResolvedStandardItem(
    long id, String impaCode, String cnCode, String categoryCode, String categoryName, String itemName, String specification
) { }

record ProvisionCategory(String code, String name) { }

record ResolvedImportRow(
    String sourceItemId, String sourceSheet, int sourceRowNo, String productType,
    String standardLibraryType, String standardCategoryCode, Long impaItemId, String impaCode,
    String categoryCode, String categoryName, String supplierSkuCode, String productName,
    String specification, String unit, BigDecimal unitPrice, BigDecimal stockQty, String currency,
    String packing, String barcode, String imageFileId, String imageUrl, String codeStatus,
    String exceptionReason, boolean reviewRequired, String matchDecision, Map<String, String> rawColumns,
    Map<String, Object> standardSnapshot, String rawRowHash
) { }

record ExecutionResult(int total, int inserted, int pending) { }
