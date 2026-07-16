package com.zswy.shipsupply.shop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ShopRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ShopRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public String companyName(long companyId) {
        return jdbcTemplate.queryForObject("SELECT company_name FROM company WHERE id = ?", String.class, companyId);
    }

    public Optional<ShopProfileResponse> findProfile(long companyId) {
        return jdbcTemplate.query(
            """
            SELECT s.*, c.company_name,
                   c.logo_file_id AS company_logo_file_id,
                   c.logo_url AS company_logo_url,
                   c.contact_name AS company_contact_name,
                   c.contact_phone AS company_contact_phone,
                   c.contact_email AS company_contact_email
            FROM shop_store s
            JOIN company c ON c.id = s.company_id
            WHERE s.company_id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> profile(rs, true),
            companyId
        ).stream().findFirst();
    }

    public ShopProfileResponse saveProfile(long companyId, long userId, ShopProfileSaveRequest request) {
        jdbcTemplate.update(
            """
            UPDATE company
            SET company_name = COALESCE(?, company_name),
                logo_file_id = COALESCE(?, logo_file_id),
                logo_url = COALESCE(?, logo_url),
                contact_name = COALESCE(?, contact_name),
                contact_phone = COALESCE(?, contact_phone),
                contact_email = COALESCE(?, contact_email)
            WHERE id = ?
            """,
            request.shopName(),
            request.logoFileId(),
            request.logoUrl(),
            request.contactName(),
            request.contactPhone(),
            request.contactEmail(),
            companyId
        );
        if (findProfile(companyId).isPresent()) {
            jdbcTemplate.update(
                """
                UPDATE shop_store
                SET logo_file_id = ?, logo_url = ?, shop_name = ?, introduction = ?,
                    main_categories = ?, delivery_areas = ?, service_ports = ?,
                    contact_name = ?, contact_phone = ?, contact_email = ?,
                    status = ?, updated_by = ?
                WHERE company_id = ?
                """,
                request.logoFileId(),
                request.logoUrl(),
                request.shopName(),
                request.description(),
                join(request.mainCategories()),
                join(request.deliveryAreas()),
                join(request.servicePorts()),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.status(),
                userId,
                companyId
            );
        } else {
            jdbcTemplate.update(
                """
                INSERT INTO shop_store
                  (company_id, logo_file_id, logo_url, shop_name, introduction, main_categories,
                   delivery_areas, service_ports, contact_name, contact_phone, contact_email,
                   status, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                companyId,
                request.logoFileId(),
                request.logoUrl(),
                request.shopName(),
                request.description(),
                join(request.mainCategories()),
                join(request.deliveryAreas()),
                join(request.servicePorts()),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.status(),
                userId,
                userId
            );
        }
        return findProfile(companyId).orElseThrow();
    }

    public long ensureShop(long companyId, long userId) {
        Optional<ShopProfileResponse> existing = findProfile(companyId);
        if (existing.isPresent()) {
            return existing.get().shopId();
        }
        String name = companyName(companyId);
        return saveProfile(companyId, userId, new ShopProfileSaveRequest(
            name, null, null, null, List.of(), List.of(), List.of(), null, null, null, "ACTIVE"
        )).shopId();
    }

    public ShopSkuResponse saveSku(long companyId, long shopId, long userId, Long skuId, ShopSkuRequest request) {
        if (skuId == null) {
            long id = insertSku(companyId, shopId, userId, request, request.importBatchId(), request.importRowNumber());
            replaceChildren(id, request);
            return findSku(companyId, id).orElseThrow();
        }
        int updated = jdbcTemplate.update(
            """
            UPDATE shop_sku
            SET product_type = ?, category_code = ?, category_name = ?, platform_code = ?, impa_code = ?,
                supplier_sku_code = ?, product_name = ?, specification_summary = ?, normalized_name = ?,
                normalized_specification = ?, stock_qty = ?, stock_unit = ?, lead_time_days = ?,
                delivery_area = ?, service_ports = ?, monthly_sales = ?, unit_price = ?, currency = ?,
                brand = ?, unit = ?, packing = ?, barcode = ?, shelf_status = ?, code_status = ?,
                exception_reason = ?, import_batch_id = COALESCE(?, import_batch_id),
                import_row_no = COALESCE(?, import_row_no), raw_row_json = ?, updated_by = ?
            WHERE id = ? AND company_id = ?
            """,
            request.productType(),
            request.categoryCode(),
            request.categoryName(),
            request.platformCode(),
            request.impaCode(),
            request.supplierSkuCode(),
            request.productName(),
            specificationSummary(request.specifications()),
            normalize(request.productName()),
            normalize(specificationSummary(request.specifications())),
            request.stockQty(),
            request.stockUnit(),
            request.leadTimeDays(),
            request.deliveryArea(),
            join(request.servicePorts()),
            value(request.monthlySales(), 0),
            request.unitPrice(),
            value(request.currency(), "CNY"),
            request.brand(),
            request.unit(),
            request.packageSpec(),
            request.barcode(),
            value(request.shelfStatus(), "OFF_SHELF"),
            value(request.codeStatus(), "PENDING_EXCEPTION"),
            request.exceptionReason(),
            request.importBatchId(),
            request.importRowNumber(),
            json(request.rawRow()),
            userId,
            skuId,
            companyId
        );
        if (updated == 0) {
            return null;
        }
        replaceChildren(skuId, request);
        return findSku(companyId, skuId).orElseThrow();
    }

    public Optional<Long> findSkuIdBySupplierSkuCode(long companyId, long shopId, String supplierSkuCode) {
        if (supplierSkuCode == null || supplierSkuCode.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
            "SELECT id FROM shop_sku WHERE company_id = ? AND shop_id = ? AND supplier_sku_code = ? LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"),
            companyId,
            shopId,
            supplierSkuCode
        ).stream().findFirst();
    }

    public Optional<ShopSkuResponse> findSkuBySupplierSkuCode(long companyId, long shopId, String supplierSkuCode) {
        if (supplierSkuCode == null || supplierSkuCode.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
            "SELECT * FROM shop_sku WHERE company_id = ? AND shop_id = ? AND supplier_sku_code = ? LIMIT 1",
            (rs, rowNum) -> sku(rs),
            companyId,
            shopId,
            supplierSkuCode
        ).stream().findFirst();
    }

    public Optional<ShopSkuResponse> findSku(long companyId, long skuId) {
        return jdbcTemplate.query(
            "SELECT * FROM shop_sku WHERE company_id = ? AND id = ?",
            (rs, rowNum) -> sku(rs),
            companyId,
            skuId
        ).stream().findFirst();
    }

    public ShopSkuListResponse listSkus(
        long companyId,
        String productType,
        String codeStatus,
        String shelfStatus,
        String keyword,
        int page,
        int size
    ) {
        long total = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM shop_sku
            WHERE company_id = ?
              AND (? IS NULL OR product_type = ?)
              AND (? IS NULL OR code_status = ?)
              AND (? IS NULL OR shelf_status = ?)
              AND (
                ? IS NULL
                OR product_name LIKE CONCAT('%', ?, '%')
                OR supplier_sku_code LIKE CONCAT('%', ?, '%')
                OR impa_code LIKE CONCAT('%', ?, '%')
                OR platform_code LIKE CONCAT('%', ?, '%')
              )
            """,
            Long.class,
            companyId,
            productType, productType,
            codeStatus, codeStatus,
            shelfStatus, shelfStatus,
            keyword, keyword, keyword, keyword, keyword
        );
        List<ShopSkuResponse> items = jdbcTemplate.query(
            """
            SELECT *
            FROM shop_sku
            WHERE company_id = ?
              AND (? IS NULL OR product_type = ?)
              AND (? IS NULL OR code_status = ?)
              AND (? IS NULL OR shelf_status = ?)
              AND (
                ? IS NULL
                OR product_name LIKE CONCAT('%', ?, '%')
                OR supplier_sku_code LIKE CONCAT('%', ?, '%')
                OR impa_code LIKE CONCAT('%', ?, '%')
                OR platform_code LIKE CONCAT('%', ?, '%')
              )
            ORDER BY
              CASE WHEN import_row_no IS NULL THEN 1 ELSE 0 END ASC,
              import_row_no ASC,
              import_batch_id DESC,
              id ASC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> sku(rs),
            companyId,
            productType, productType,
            codeStatus, codeStatus,
            shelfStatus, shelfStatus,
            keyword, keyword, keyword, keyword, keyword,
            size,
            (page - 1) * size
        );
        return new ShopSkuListResponse(items, total, page, size);
    }

    public SupplierListResponse listSuppliers(String keyword, String port, String category, String status, int page, int size) {
        String baseSql = """
            FROM company c
            LEFT JOIN shop_store s ON s.company_id = c.id
            LEFT JOIN (
              SELECT company_id,
                     COUNT(*) AS sku_count,
                     GROUP_CONCAT(DISTINCT NULLIF(service_ports, '') SEPARATOR ', ') AS sku_ports,
                     GROUP_CONCAT(DISTINCT NULLIF(category_name, '') SEPARATOR ', ') AS sku_categories
              FROM shop_sku
              GROUP BY company_id
            ) sku ON sku.company_id = c.id
            LEFT JOIN (
              SELECT supplier_id AS company_id,
                     COUNT(*) AS legacy_sku_count,
                     GROUP_CONCAT(DISTINCT NULLIF(service_port, '') SEPARATOR ', ') AS legacy_ports
              FROM supplier_sku
              WHERE enabled = 1
              GROUP BY supplier_id
            ) legacy_sku ON legacy_sku.company_id = c.id
            LEFT JOIN (
              SELECT company_id,
                     COUNT(*) AS user_count
              FROM sys_user
              WHERE status = 'ACTIVE'
              GROUP BY company_id
            ) active_user ON active_user.company_id = c.id
            WHERE c.company_type = 'SUPPLIER'
              AND (
                COALESCE(active_user.user_count, 0) > 0
                OR COALESCE(sku.sku_count, 0) > 0
                OR COALESCE(legacy_sku.legacy_sku_count, 0) > 0
              )
            """;
        List<Object> args = new ArrayList<>();
        StringBuilder filters = new StringBuilder();
        appendLikeFilter(filters, args, keyword, "c.company_name", "s.shop_name", "c.contact_name", "c.contact_phone");
        appendLikeFilter(filters, args, port, "s.service_ports", "sku.sku_ports", "legacy_sku.legacy_ports");
        appendLikeFilter(filters, args, category, "s.main_categories", "sku.sku_categories");
        String normalizedStatus = value(status, null);
        if (normalizedStatus != null && !"all".equalsIgnoreCase(normalizedStatus)) {
            if ("active".equalsIgnoreCase(normalizedStatus) || "ACTIVE".equalsIgnoreCase(normalizedStatus)) {
                filters.append(" AND c.status = 'ACTIVE'");
            } else if ("warning".equalsIgnoreCase(normalizedStatus) || "WARNING".equalsIgnoreCase(normalizedStatus)) {
                filters.append(" AND c.status <> 'ACTIVE'");
            }
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + baseSql + filters, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(size);
        queryArgs.add((page - 1) * size);
        List<SupplierSummaryResponse> items = jdbcTemplate.query(
            """
            SELECT
              c.id AS company_id,
              c.company_name,
              c.contact_name,
              c.contact_phone,
              c.status,
              COALESCE(s.service_ports, sku.sku_ports, legacy_sku.legacy_ports, '') AS port,
              COALESCE(s.main_categories, sku.sku_categories, '') AS category,
              COALESCE(sku.sku_count, 0) + COALESCE(legacy_sku.legacy_sku_count, 0) AS sku_count,
              GREATEST(c.updated_at, COALESCE(s.updated_at, c.updated_at)) AS updated_at
            """ + baseSql + filters + """
            ORDER BY
              CASE WHEN c.status = 'ACTIVE' THEN 0 ELSE 1 END ASC,
              COALESCE(sku.sku_count, 0) + COALESCE(legacy_sku.legacy_sku_count, 0) DESC,
              c.id DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> supplier(rs),
            queryArgs.toArray()
        );
        return new SupplierListResponse(items, total == null ? 0 : total, page, size);
    }

    public boolean deleteSku(long companyId, long skuId) {
        return jdbcTemplate.update("DELETE FROM shop_sku WHERE company_id = ? AND id = ?", companyId, skuId) > 0;
    }

    public Optional<ShopSkuResponse> updateShelfStatus(long companyId, long skuId, String shelfStatus) {
        int updated = jdbcTemplate.update(
            "UPDATE shop_sku SET shelf_status = ? WHERE company_id = ? AND id = ?",
            shelfStatus,
            companyId,
            skuId
        );
        return updated == 0 ? Optional.empty() : findSku(companyId, skuId);
    }

    public List<ShopSkuResponse> updateShelfStatusBatch(long companyId, List<Long> skuIds, String shelfStatus) {
        List<Long> safeIds = list(skuIds).stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (safeIds.isEmpty()) {
            jdbcTemplate.update("UPDATE shop_sku SET shelf_status = ? WHERE company_id = ?", shelfStatus, companyId);
            return jdbcTemplate.query(
                "SELECT * FROM shop_sku WHERE company_id = ? ORDER BY updated_at DESC, id DESC",
                (rs, rowNum) -> sku(rs),
                companyId
            );
        }
        String placeholders = safeIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        List<Object> updateArgs = new ArrayList<>();
        updateArgs.add(shelfStatus);
        updateArgs.add(companyId);
        updateArgs.addAll(safeIds);
        jdbcTemplate.update(
            "UPDATE shop_sku SET shelf_status = ? WHERE company_id = ? AND id IN (" + placeholders + ")",
            updateArgs.toArray()
        );

        List<Object> queryArgs = new ArrayList<>();
        queryArgs.add(companyId);
        queryArgs.addAll(safeIds);
        return jdbcTemplate.query(
            "SELECT * FROM shop_sku WHERE company_id = ? AND id IN (" + placeholders + ") ORDER BY updated_at DESC, id DESC",
            (rs, rowNum) -> sku(rs),
            queryArgs.toArray()
        );
    }

    public long createBatch(long companyId, long shopId, long userId, String fileName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO shop_sku_import_batch
                  (company_id, shop_id, source_file_name, status, created_by, started_at)
                VALUES (?, ?, ?, 'PARSING', ?, CURRENT_TIMESTAMP)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, companyId);
            statement.setLong(2, shopId);
            statement.setString(3, fileName);
            statement.setLong(4, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void savePreviewRows(long batchId, long companyId, long shopId, List<ShopImportPreviewItem> rows) {
        jdbcTemplate.update("DELETE FROM shop_sku_import_preview_row WHERE batch_id = ? AND company_id = ?", batchId, companyId);
        for (ShopImportPreviewItem row : rows) {
            jdbcTemplate.update(
                """
                INSERT INTO shop_sku_import_preview_row
                  (batch_id, company_id, shop_id, row_no, supplier_sku_code, product_name,
                   product_type, category_code, category_name, platform_code, impa_code,
                   specification_summary, stock_qty, stock_unit, unit_price, currency,
                   packing, barcode, code_status, exception_reason, image_file_id, image_url,
                   thumbnail_url, raw_name, raw_spec, clean_name, parsed_attributes_json,
                   logic_recommendation_json, model_recommendation_json,
                   selected_recommendation_source, review_required, match_decision,
                   raw_row_json, candidate_snapshot_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        CAST(? AS JSON), CAST(? AS JSON), CAST(? AS JSON), ?, ?, ?, CAST(? AS JSON), NULL)
                """,
                batchId,
                companyId,
                shopId,
                row.rowNo(),
                row.supplierSkuCode(),
                row.productName(),
                value(row.productType(), "MATERIAL"),
                row.categoryCode(),
                row.categoryName(),
                row.platformCode(),
                row.impaCode(),
                row.attributeSummary(),
                row.stockQty(),
                row.stockUnit(),
                row.unitPrice(),
                value(row.currency(), "CNY"),
                row.packageSpec(),
                row.barcode(),
                row.codeStatus(),
                row.exceptionReason(),
                row.imageFileId(),
                row.imageUrl(),
                row.thumbnailUrl(),
                row.rawName(),
                row.rawSpec(),
                row.cleanName(),
                json(row.parsedAttributes()),
                json(row.logicRecommendation()),
                null,
                recommendationSource(row.logicRecommendation()),
                reviewRequired(row) ? 1 : 0,
                matchDecision(row),
                json(row.rawColumns())
            );
        }
        updateBatchCounts(batchId, companyId, rows.size(), (int) rows.stream().filter(r -> !"PENDING_EXCEPTION".equals(r.codeStatus())).count());
    }

    public Optional<Long> batchCompany(long companyId, long batchId) {
        return jdbcTemplate.query(
            "SELECT id FROM shop_sku_import_batch WHERE company_id = ? AND id = ?",
            (rs, rowNum) -> rs.getLong("id"),
            companyId,
            batchId
        ).stream().findFirst();
    }

    public void applyConfirmSelections(long companyId, long batchId, List<ShopImportConfirmItem> items) {
        for (ShopImportConfirmItem item : list(items)) {
            if (item == null || item.rowNo() == null) {
                continue;
            }
            jdbcTemplate.update(
                """
                UPDATE shop_sku_import_preview_row
                SET confirmed_impa_code = COALESCE(?, confirmed_impa_code),
                    confirmed_platform_code = COALESCE(?, confirmed_platform_code),
                    confirmed_recommendation_source = COALESCE(?, confirmed_recommendation_source)
                WHERE company_id = ? AND batch_id = ? AND row_no = ?
                """,
                value(item.confirmedImpaCode(), null),
                value(item.confirmedPlatformCode(), null),
                value(item.selectedRecommendationSource(), null),
                companyId,
                batchId,
                item.rowNo()
            );
        }
    }

    public List<ShopSkuResponse> confirmBatch(long companyId, long userId, long batchId) {
        long shopId = ensureShop(companyId, userId);
        List<ShopSkuRequest> requests = jdbcTemplate.query(
            "SELECT * FROM shop_sku_import_preview_row WHERE company_id = ? AND batch_id = ? ORDER BY row_no ASC",
            (rs, rowNum) -> new ShopSkuRequest(
                rs.getString("product_type"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                value(rs.getString("confirmed_platform_code"), rs.getString("platform_code")),
                value(rs.getString("confirmed_impa_code"), rs.getString("impa_code")),
                rs.getString("supplier_sku_code"),
                value(rs.getString("product_name"), "未命名商品"),
                List.of(new ShopSkuAttributeRequest("specification", "规格", rs.getString("specification_summary"), null, 0, rs.getString("specification_summary"))),
                rs.getBigDecimal("stock_qty"),
                rs.getString("stock_unit"),
                null,
                null,
                List.of(),
                0,
                rs.getBigDecimal("unit_price"),
                rs.getString("currency"),
                null,
                null,
                rs.getString("packing"),
                rs.getString("barcode"),
                "OFF_SHELF",
                rs.getString("code_status"),
                rs.getString("exception_reason"),
                previewImages(rs.getString("image_file_id"), rs.getString("image_url"), rs.getString("thumbnail_url")),
                rs.getString("image_file_id"),
                rs.getString("image_url"),
                rs.getString("thumbnail_url"),
                batchId,
                rs.getInt("row_no"),
                rawRowWithRecognition(rs)
            ),
            companyId,
            batchId
        );
        List<ShopSkuResponse> saved = requests.stream()
            .map(request -> saveSku(companyId, shopId, userId, null, request))
            .toList();
        jdbcTemplate.update(
            "UPDATE shop_sku_import_batch SET status = 'CONFIRMED', success_count = ?, finished_at = CURRENT_TIMESTAMP WHERE company_id = ? AND id = ?",
            saved.size(),
            companyId,
            batchId
        );
        return saved;
    }

    private Map<String, Object> rawRowWithRecognition(ResultSet rs) throws SQLException {
        Map<String, Object> rawRow = new LinkedHashMap<>(readMap(rs.getString("raw_row_json")));
        Map<String, Object> recognition = new LinkedHashMap<>();
        recognition.put("rawName", rs.getString("raw_name"));
        recognition.put("rawSpec", rs.getString("raw_spec"));
        recognition.put("cleanName", rs.getString("clean_name"));
        recognition.put("parsedAttributes", readJson(rs.getString("parsed_attributes_json")));
        recognition.put("logicRecommendation", readJson(rs.getString("logic_recommendation_json")));
        recognition.put("reviewRequired", rs.getInt("review_required") == 1);
        recognition.put("matchDecision", rs.getString("match_decision"));
        recognition.put("confirmedImpaCode", rs.getString("confirmed_impa_code"));
        recognition.put("confirmedPlatformCode", rs.getString("confirmed_platform_code"));
        recognition.put("confirmedRecommendationSource", value(rs.getString("confirmed_recommendation_source"), rs.getString("selected_recommendation_source")));
        rawRow.put("_recognition", recognition);
        return rawRow;
    }

    private List<ShopSkuImageRequest> previewImages(String fileId, String imageUrl, String thumbnailUrl) {
        if ((fileId == null || fileId.isBlank()) && (imageUrl == null || imageUrl.isBlank())) {
            return List.of();
        }
        return List.of(new ShopSkuImageRequest(fileId, imageUrl, thumbnailUrl, true, 0));
    }

    private String recommendationSource(ShopImportRecommendation recommendation) {
        return recommendation != null && Boolean.TRUE.equals(recommendation.available()) ? "LOGIC" : "NONE";
    }

    private boolean reviewRequired(ShopImportPreviewItem row) {
        return row.logicRecommendation() == null
            || !Boolean.TRUE.equals(row.logicRecommendation().available())
            || !"HIGH".equals(row.logicRecommendation().confidenceLevel());
    }

    private String matchDecision(ShopImportPreviewItem row) {
        if (row.logicRecommendation() != null && Boolean.TRUE.equals(row.logicRecommendation().available())) {
            return row.logicRecommendation().impaCode() == null ? "CATEGORY_ONLY" : "LOGIC_ONLY";
        }
        return "UNMATCHED";
    }

    public ShopSkuResponse resolveException(long companyId, long userId, long skuId, ShopExceptionResolveRequest request) {
        ShopSkuResponse before = findSku(companyId, skuId).orElseThrow();
        String newStatus = switch (value(request.actionType(), "").toUpperCase()) {
            case "SELECT_CANDIDATE", "MANUAL_CODE" -> "CONFIRMED";
            case "IGNORE" -> "IGNORED";
            case "REOPEN" -> "PENDING_EXCEPTION";
            default -> before.codeStatus();
        };
        jdbcTemplate.update(
            """
            UPDATE shop_sku
            SET platform_code = COALESCE(?, platform_code),
                impa_code = COALESCE(?, impa_code),
                code_status = ?,
                exception_reason = ?
            WHERE company_id = ? AND id = ?
            """,
            request.platformCode(),
            request.impaCode(),
            newStatus,
            request.reason(),
            companyId,
            skuId
        );
        jdbcTemplate.update(
            """
            INSERT INTO shop_sku_exception_log
              (company_id, shop_id, sku_id, import_batch_id, action_type, before_code_status,
               after_code_status, before_platform_code, after_platform_code, before_impa_code,
               after_impa_code, selected_candidate_json, reason, handled_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?)
            """,
            companyId,
            before.shopId(),
            skuId,
            before.importBatchId(),
            request.actionType(),
            before.codeStatus(),
            newStatus,
            before.platformCode(),
            request.platformCode(),
            before.impaCode(),
            request.impaCode(),
            json(request.selectedCandidate()),
            request.reason(),
            userId
        );
        return findSku(companyId, skuId).orElseThrow();
    }

    private long insertSku(long companyId, long shopId, long userId, ShopSkuRequest request, Long batchId, Integer rowNo) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO shop_sku
                  (company_id, shop_id, product_type, category_code, category_name, platform_code,
                   impa_code, supplier_sku_code, product_name, specification_summary,
                   normalized_name, normalized_specification, stock_qty, stock_unit, lead_time_days,
                   delivery_area, service_ports, monthly_sales, unit_price, currency, brand, unit,
                   packing, barcode, shelf_status, code_status, exception_reason,
                   import_batch_id, import_row_no, raw_row_json, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            int index = 1;
            statement.setLong(index++, companyId);
            statement.setLong(index++, shopId);
            statement.setString(index++, request.productType());
            statement.setString(index++, request.categoryCode());
            statement.setString(index++, request.categoryName());
            statement.setString(index++, request.platformCode());
            statement.setString(index++, request.impaCode());
            statement.setString(index++, request.supplierSkuCode());
            statement.setString(index++, request.productName());
            statement.setString(index++, specificationSummary(request.specifications()));
            statement.setString(index++, normalize(request.productName()));
            statement.setString(index++, normalize(specificationSummary(request.specifications())));
            statement.setBigDecimal(index++, request.stockQty());
            statement.setString(index++, request.stockUnit());
            statement.setObject(index++, request.leadTimeDays());
            statement.setString(index++, request.deliveryArea());
            statement.setString(index++, join(request.servicePorts()));
            statement.setInt(index++, value(request.monthlySales(), 0));
            statement.setBigDecimal(index++, request.unitPrice());
            statement.setString(index++, value(request.currency(), "CNY"));
            statement.setString(index++, request.brand());
            statement.setString(index++, request.unit());
            statement.setString(index++, request.packageSpec());
            statement.setString(index++, request.barcode());
            statement.setString(index++, value(request.shelfStatus(), "OFF_SHELF"));
            statement.setString(index++, value(request.codeStatus(), "PENDING_EXCEPTION"));
            statement.setString(index++, request.exceptionReason());
            statement.setObject(index++, batchId);
            statement.setObject(index++, rowNo);
            statement.setString(index++, json(request.rawRow()));
            statement.setLong(index++, userId);
            statement.setLong(index, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void replaceChildren(long skuId, ShopSkuRequest request) {
        jdbcTemplate.update("DELETE FROM shop_sku_attribute WHERE sku_id = ?", skuId);
        for (ShopSkuAttributeRequest attr : list(request.specifications())) {
            jdbcTemplate.update(
                """
                INSERT INTO shop_sku_attribute
                  (sku_id, attribute_key, attribute_name, attribute_value, attribute_unit, sort_order, raw_text)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                skuId,
                attr.key(),
                value(attr.name(), attr.key()),
                attr.value(),
                attr.unit(),
                value(attr.sortOrder(), 0),
                attr.rawText()
            );
        }
        jdbcTemplate.update("DELETE FROM shop_sku_image WHERE sku_id = ?", skuId);
        for (ShopSkuImageRequest image : list(request.images())) {
            jdbcTemplate.update(
                """
                INSERT INTO shop_sku_image
                  (sku_id, file_id, image_url, thumbnail_url, is_primary, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                skuId,
                image.fileId(),
                image.imageUrl(),
                image.thumbnailUrl(),
                Boolean.TRUE.equals(image.primary()) ? 1 : 0,
                value(image.sortOrder(), 0)
            );
        }
    }

    private void updateBatchCounts(long batchId, long companyId, int total, int success) {
        jdbcTemplate.update(
            """
            UPDATE shop_sku_import_batch
            SET status = 'PREVIEW_READY', total_count = ?, success_count = ?,
                exception_count = ?, finished_at = CURRENT_TIMESTAMP
            WHERE id = ? AND company_id = ?
            """,
            total,
            success,
            total - success,
            batchId,
            companyId
        );
    }

    private ShopProfileResponse profile(ResultSet rs, boolean dataReady) throws SQLException {
        return new ShopProfileResponse(
            rs.getLong("id"),
            rs.getLong("company_id"),
            rs.getString("company_name"),
            value(rs.getString("shop_name"), rs.getString("company_name")),
            value(rs.getString("company_logo_file_id"), rs.getString("logo_file_id")),
            value(rs.getString("company_logo_url"), rs.getString("logo_url")),
            rs.getString("introduction"),
            split(rs.getString("main_categories")),
            split(rs.getString("service_ports")),
            split(rs.getString("delivery_areas")),
            value(rs.getString("company_contact_name"), rs.getString("contact_name")),
            value(rs.getString("company_contact_phone"), rs.getString("contact_phone")),
            value(rs.getString("company_contact_email"), rs.getString("contact_email")),
            rs.getString("status"),
            string(rs.getTimestamp("created_at")),
            string(rs.getTimestamp("updated_at")),
            dataReady
        );
    }

    private ShopSkuResponse sku(ResultSet rs) throws SQLException {
        long skuId = rs.getLong("id");
        List<ShopSkuAttributeResponse> attributes = attributes(skuId);
        List<ShopSkuImageResponse> images = images(skuId);
        ShopSkuImageResponse primaryImage = images.stream()
            .filter(image -> Boolean.TRUE.equals(image.primary()))
            .findFirst()
            .orElse(images.stream().findFirst().orElse(null));
        return new ShopSkuResponse(
            skuId,
            rs.getLong("shop_id"),
            rs.getLong("company_id"),
            rs.getString("product_type"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            rs.getString("platform_code"),
            rs.getString("impa_code"),
            rs.getString("supplier_sku_code"),
            rs.getString("product_name"),
            attributes,
            attributeSummary(attributes),
            rs.getBigDecimal("stock_qty"),
            rs.getString("stock_unit"),
            (Integer) rs.getObject("lead_time_days"),
            rs.getString("delivery_area"),
            primaryImage == null ? null : primaryImage.imageUrl(),
            primaryImage == null ? null : primaryImage.thumbnailUrl(),
            primaryImage == null ? null : primaryImage.fileId(),
            rs.getInt("monthly_sales"),
            rs.getBigDecimal("unit_price"),
            rs.getString("currency"),
            currencySymbol(rs.getString("currency")),
            rs.getString("brand"),
            rs.getString("unit"),
            rs.getString("packing"),
            rs.getString("barcode"),
            rs.getString("shelf_status"),
            rs.getString("code_status"),
            rs.getString("exception_reason"),
            (Long) rs.getObject("import_batch_id"),
            (Integer) rs.getObject("import_row_no"),
            string(rs.getTimestamp("created_at")),
            string(rs.getTimestamp("updated_at")),
            images
        );
    }

    private SupplierSummaryResponse supplier(ResultSet rs) throws SQLException {
        long companyId = rs.getLong("company_id");
        long skuCount = rs.getLong("sku_count");
        String status = "ACTIVE".equalsIgnoreCase(value(rs.getString("status"), "")) ? "active" : "warning";
        int score = Math.min(99, 88 + (int) Math.min(9, skuCount / 80));
        return new SupplierSummaryResponse(
            companyId,
            "SUP-" + companyId,
            rs.getString("company_name"),
            value(rs.getString("port"), "--"),
            value(rs.getString("category"), "--"),
            String.valueOf(score),
            status,
            status,
            skuCount,
            rs.getString("contact_name"),
            rs.getString("contact_phone"),
            string(rs.getTimestamp("updated_at"))
        );
    }

    private List<ShopSkuAttributeResponse> attributes(long skuId) {
        return jdbcTemplate.query(
            "SELECT * FROM shop_sku_attribute WHERE sku_id = ? ORDER BY sort_order ASC, id ASC",
            (rs, rowNum) -> new ShopSkuAttributeResponse(
                rs.getLong("id"),
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

    private List<ShopSkuImageResponse> images(long skuId) {
        return jdbcTemplate.query(
            "SELECT * FROM shop_sku_image WHERE sku_id = ? ORDER BY sort_order ASC, id ASC",
            (rs, rowNum) -> new ShopSkuImageResponse(
                rs.getLong("id"),
                rs.getString("file_id"),
                rs.getString("image_url"),
                rs.getString("thumbnail_url"),
                rs.getBoolean("is_primary"),
                rs.getInt("sort_order")
            ),
            skuId
        );
    }

    private String specificationSummary(List<ShopSkuAttributeRequest> attributes) {
        return list(attributes).stream()
            .map(ShopSkuAttributeRequest::value)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String attributeSummary(List<ShopSkuAttributeResponse> attributes) {
        return attributes.stream()
            .map(attribute -> value(attribute.name(), attribute.key()) + ": " + value(attribute.value(), ""))
            .filter(value -> !value.endsWith(": "))
            .collect(java.util.stream.Collectors.joining(" / "));
    }

    private String join(List<String> values) {
        return values == null ? null : values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.joining(","));
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void appendLikeFilter(StringBuilder filters, List<Object> args, String keyword, String... columns) {
        String value = value(keyword, null);
        if (value == null || columns.length == 0) {
            return;
        }
        filters.append(" AND (");
        for (int index = 0; index < columns.length; index++) {
            if (index > 0) {
                filters.append(" OR ");
            }
            filters.append(columns[index]).append(" LIKE CONCAT('%', ?, '%')");
            args.add(value);
        }
        filters.append(")");
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON_SERIALIZE_FAILED", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.replaceAll("[\\p{Punct}\\s]+", "").toUpperCase(java.util.Locale.ROOT);
    }

    private String string(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String currencySymbol(String currency) {
        return "USD".equalsIgnoreCase(value(currency, "CNY")) ? "$" : "\u00A5";
    }
}
