package com.zswy.shipsupply.procurement.materials;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class MaterialDemandRepository {

    private static final TypeReference<Map<String, String>> RAW_COLUMNS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> TRAFFIC_SERVICE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<MaterialMatchCandidate>> CANDIDATES_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MaterialDemandRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public String nextDemandNo(long companyId, LocalDate inquiryDate) {
        String prefix = "REQ-" + inquiryDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-";
        List<String> demandNos = jdbcTemplate.query(
            """
            SELECT demand_no
            FROM material_demand
            WHERE company_id = ? AND demand_no LIKE CONCAT(?, '%')
            ORDER BY demand_no DESC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("demand_no"),
            companyId,
            prefix
        );
        return MaterialDemandNo.next(inquiryDate, demandNos.isEmpty() ? null : demandNos.get(0));
    }

    public long insertDemand(
        long companyId,
        long userId,
        String demandNo,
        MaterialDemandSaveRequest request,
        MaterialDemandStats stats
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO material_demand
                  (company_id, created_by, updated_by, demand_no, application_no, inquiry_no,
                   material_type, currency, recipient_company, handler_name, handler_email, vessel_name,
                   supply_port_code, supply_port_name, vessel_eta, inquiry_date, source_file_name, source_file_id, document_type, header_row_index, traffic_service_json, sku_count, exact_count,
                   similar_count, unmatched_count, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SAVED')
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, companyId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setString(4, demandNo);
            statement.setString(5, request.applicationNo());
            statement.setString(6, request.inquiryNo());
            statement.setString(7, request.materialType());
            statement.setString(8, request.currency());
            statement.setString(9, request.recipientCompany());
            statement.setString(10, request.handlerName());
            statement.setString(11, request.handlerEmail());
            statement.setString(12, request.vesselName());
            statement.setString(13, request.supplyPortCode());
            statement.setString(14, request.supplyPortName());
            statement.setString(15, request.vesselEta());
            statement.setObject(16, LocalDate.parse(request.inquiryDate()));
            statement.setString(17, request.sourceFileName());
            statement.setString(18, request.sourceFileId());
            statement.setString(19, request.documentType());
            statement.setInt(20, request.headerRowIndex());
            statement.setString(21, jsonObject(request.trafficService()));
            statement.setInt(22, stats.skuCount());
            statement.setInt(23, stats.exactCount());
            statement.setInt(24, stats.similarCount());
            statement.setInt(25, stats.unmatchedCount());
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateDemand(
        long demandId,
        long companyId,
        long userId,
        MaterialDemandSaveRequest request,
        MaterialDemandStats stats
    ) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET updated_by = ?,
                application_no = ?,
                inquiry_no = ?,
                material_type = ?,
                currency = ?,
                recipient_company = ?,
                handler_name = ?,
                handler_email = ?,
                vessel_name = ?,
                supply_port_code = ?,
                supply_port_name = ?,
                vessel_eta = ?,
                inquiry_date = ?,
                source_file_name = ?,
                source_file_id = ?,
                document_type = ?,
                header_row_index = ?,
                traffic_service_json = ?,
                sku_count = ?,
                exact_count = ?,
                similar_count = ?,
                unmatched_count = ?,
                status = CASE WHEN status = 'COMPARING' THEN 'COMPARING' ELSE 'SAVED' END
            WHERE id = ? AND company_id = ?
            """,
            userId,
            request.applicationNo(),
            request.inquiryNo(),
            request.materialType(),
            request.currency(),
            request.recipientCompany(),
            request.handlerName(),
            request.handlerEmail(),
            request.vesselName(),
            request.supplyPortCode(),
            request.supplyPortName(),
            request.vesselEta(),
            LocalDate.parse(request.inquiryDate()),
            request.sourceFileName(),
            request.sourceFileId(),
            request.documentType(),
            request.headerRowIndex(),
            jsonObject(request.trafficService()),
            stats.skuCount(),
            stats.exactCount(),
            stats.similarCount(),
            stats.unmatchedCount(),
            demandId,
            companyId
        );
    }

    public void replaceItems(long demandId, long companyId, List<MaterialDemandItemRequest> items) {
        jdbcTemplate.update("DELETE FROM material_demand_item WHERE demand_id = ? AND company_id = ?", demandId, companyId);
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Object[]> args = new ArrayList<>(items.size());
        for (MaterialDemandItemRequest item : items) {
            args.add(new Object[] {
                demandId,
                companyId,
                item.documentType(),
                item.headerRowIndex(),
                item.sequence(),
                item.sourceRowNo(),
                item.sourceRowNumber(),
                json(item.rawColumns()),
                item.impaCode(),
                item.description(),
                item.sizeModel(),
                item.quantity(),
                item.unit(),
                item.remarks(),
                item.supplierItemNo(),
                item.rawNameSpec(),
                item.price(),
                item.packing(),
                item.stock(),
                item.selectedImpaCode(),
                item.candidateImpaCode(),
                item.candidateNameCn(),
                item.candidateNameEn(),
                item.candidateSpec(),
                item.matchResult(),
                item.matchResultName(),
                item.reason(),
                item.validationStatus(),
                item.validationReason(),
                item.hasImage() ? 1 : 0,
                item.imageIndex(),
                item.imageAnchor(),
                json(candidateSnapshot(item)),
                item.actualQuotePrice(),
                item.actualQuoteCurrency(),
                item.quoteMarkupPercent(),
                item.quoteSupplierSkuId(),
                item.quoteSelectedUnit(),
                item.quoteUnitPrice(),
                item.quoteUnitPriceUsd(),
                item.quoteStrategyType()
            });
        }
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO material_demand_item
              (demand_id, company_id, document_type, header_row_index, sequence_no,
               source_row_no, source_row_number, raw_columns_json, impa_code,
               description, size_model, quantity, unit, remarks, supplier_item_no,
               raw_name_spec, price, packing, stock, selected_impa_code,
               candidate_impa_code, candidate_name_cn, candidate_name_en,
               candidate_spec, match_result, match_result_name, reason, validation_status, validation_reason, has_image,
               image_index, image_anchor, candidates_json, actual_quote_price,
               actual_quote_currency, quote_markup_percent, quote_supplier_sku_id,
               quote_selected_unit, quote_unit_price, quote_unit_price_usd, quote_strategy_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            args
        );
    }

    public Optional<MaterialDemandSummaryResponse> findSummaryById(long companyId, long demandId) {
        return querySummaries(
            """
            SELECT *
            FROM material_demand
            WHERE company_id = ? AND id = ?
            LIMIT 1
            """,
            companyId,
            demandId
        ).stream().findFirst();
    }

    public Optional<MaterialDemandSummaryResponse> findSummaryByNo(long companyId, String demandNo) {
        return querySummaries(
            """
            SELECT *
            FROM material_demand
            WHERE company_id = ? AND demand_no = ?
            LIMIT 1
            """,
            companyId,
            demandNo
        ).stream().findFirst();
    }

    public void markComparingIfSaved(long companyId, long demandId) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET status = 'COMPARING',
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ?
              AND id = ?
              AND status = 'SAVED'
            """,
            companyId,
            demandId
        );
    }

    public void markOrdered(long companyId, long demandId) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET status = 'ORDERED',
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ?
              AND id = ?
              AND status <> 'DISCARDED'
            """,
            companyId,
            demandId
        );
    }

    public void markDiscarded(long companyId, long demandId) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET status = 'DISCARDED',
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ?
              AND id = ?
            """,
            companyId,
            demandId
        );
    }

    public MaterialDemandListResponse list(
        long companyId,
        String keyword,
        String status,
        LocalDate dateFrom,
        LocalDate dateTo,
        String stage,
        int page,
        int size
    ) {
        String stageFilter = comparisonStage(stage);
        long total = count(companyId, keyword, status, dateFrom, dateTo, stageFilter);
        int offset = (page - 1) * size;
        List<MaterialDemandSummaryResponse> items = jdbcTemplate.query(
            """
            SELECT *
            FROM material_demand
            WHERE company_id = ?
              AND (? IS NULL OR status = ?)
              AND (? IS NULL OR ? IS NOT NULL OR status IN ('COMPARING', 'ORDERED'))
              AND (? IS NULL OR inquiry_date >= ?)
              AND (? IS NULL OR inquiry_date <= ?)
              AND (
                ? IS NULL
                OR demand_no LIKE CONCAT('%', ?, '%')
                OR application_no LIKE CONCAT('%', ?, '%')
                OR inquiry_no LIKE CONCAT('%', ?, '%')
                OR vessel_name LIKE CONCAT('%', ?, '%')
                OR supply_port_name LIKE CONCAT('%', ?, '%')
                OR source_file_name LIKE CONCAT('%', ?, '%')
              )
            ORDER BY updated_at DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> summary(rs),
            companyId,
            status,
            status,
            stageFilter,
            status,
            dateFrom,
            dateFrom,
            dateTo,
            dateTo,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword,
            size,
            offset
        );
        return new MaterialDemandListResponse(items, page, size, total);
    }

    public List<MaterialDemandItemResponse> items(long companyId, long demandId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM material_demand_item
            WHERE company_id = ? AND demand_id = ?
            ORDER BY sequence_no ASC, id ASC
            """,
            (rs, rowNum) -> item(rs),
            companyId,
            demandId
        );
    }

    public int updateComparisonQuotes(
        long companyId,
        long demandId,
        MaterialComparisonQuoteSaveRequest request
    ) {
        if (request == null) {
            return 0;
        }
        updateComparisonFixedFees(companyId, demandId, request);
        if (request.items() == null) {
            return 0;
        }
        jdbcTemplate.update(
            """
            UPDATE material_demand_item
            SET actual_quote_price = NULL,
                actual_quote_currency = NULL,
                quote_markup_percent = NULL,
                quote_supplier_sku_id = NULL,
                quote_selected_unit = NULL,
                quote_unit_price = NULL,
                quote_unit_price_usd = NULL,
                quote_strategy_type = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ? AND demand_id = ?
            """,
            companyId,
            demandId
        );
        List<MaterialComparisonQuoteItemRequest> validItems = request.items().stream()
            .filter(item -> item != null && item.demandItemId() != null)
            .toList();
        if (validItems.isEmpty()) {
            return 0;
        }

        List<Object[]> args = new ArrayList<>(validItems.size());
        for (MaterialComparisonQuoteItemRequest item : validItems) {
            boolean selected = item.actualQuotePrice() != null;
            args.add(new Object[] {
                optionalText(item.quantity()),
                optionalText(item.remarks()),
                selected ? item.actualQuotePrice() : null,
                selected ? optionalText(item.currency()) : null,
                selected ? (item.quoteMarkupPercent() == null ? request.markupPercent() : item.quoteMarkupPercent()) : null,
                selected ? item.skuId() : null,
                selected ? optionalText(item.selectedUnit()) : null,
                selected ? item.unitPrice() : null,
                selected ? item.unitPriceUsd() : null,
                selected ? optionalText(request.strategyType()) : null,
                companyId,
                demandId,
                item.demandItemId()
            });
        }

        int[] updates = jdbcTemplate.batchUpdate(
            """
            UPDATE material_demand_item
            SET quantity = COALESCE(?, quantity),
                remarks = ?,
                actual_quote_price = ?,
                actual_quote_currency = ?,
                quote_markup_percent = ?,
                quote_supplier_sku_id = ?,
                quote_selected_unit = ?,
                quote_unit_price = ?,
                quote_unit_price_usd = ?,
                quote_strategy_type = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ? AND demand_id = ? AND id = ?
            """,
            args
        );
        int saved = 0;
        for (int index = 0; index < validItems.size(); index++) {
            if (validItems.get(index).actualQuotePrice() == null) {
                continue;
            }
            if (updates[index] > 0 || updates[index] == Statement.SUCCESS_NO_INFO) {
                saved++;
            }
        }
        return saved;
    }

    private void updateComparisonFixedFees(
        long companyId,
        long demandId,
        MaterialComparisonQuoteSaveRequest request
    ) {
        jdbcTemplate.update(
            """
            UPDATE material_demand
            SET fixed_freight_fee = ?,
                fixed_customs_fee = ?,
                fixed_crane_fee = ?,
                fixed_other_fee = ?,
                traffic_service_json = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ? AND id = ?
            """,
            request.fixedFreightFee(),
            request.fixedCustomsFee(),
            request.fixedCraneFee(),
            request.fixedOtherFee(),
            jsonObject(request.trafficService()),
            companyId,
            demandId
        );
    }

    private long count(long companyId, String keyword, String status, LocalDate dateFrom, LocalDate dateTo, String stageFilter) {
        Long total = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM material_demand
            WHERE company_id = ?
              AND (? IS NULL OR status = ?)
              AND (? IS NULL OR ? IS NOT NULL OR status IN ('COMPARING', 'ORDERED'))
              AND (? IS NULL OR inquiry_date >= ?)
              AND (? IS NULL OR inquiry_date <= ?)
              AND (
                ? IS NULL
                OR demand_no LIKE CONCAT('%', ?, '%')
                OR application_no LIKE CONCAT('%', ?, '%')
                OR inquiry_no LIKE CONCAT('%', ?, '%')
                OR vessel_name LIKE CONCAT('%', ?, '%')
                OR supply_port_name LIKE CONCAT('%', ?, '%')
                OR source_file_name LIKE CONCAT('%', ?, '%')
              )
            """,
            Long.class,
            companyId,
            status,
            status,
            stageFilter,
            status,
            dateFrom,
            dateFrom,
            dateTo,
            dateTo,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword
        );
        return total == null ? 0L : total;
    }

    private String comparisonStage(String stage) {
        return stage != null && "COMPARISON".equalsIgnoreCase(stage.trim()) ? "COMPARISON" : null;
    }

    private List<MaterialDemandSummaryResponse> querySummaries(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> summary(rs), args);
    }

    private MaterialDemandSummaryResponse summary(ResultSet rs) throws SQLException {
        LocalDate inquiryDate = rs.getObject("inquiry_date", LocalDate.class);
        return new MaterialDemandSummaryResponse(
            rs.getLong("id"),
            rs.getString("demand_no"),
            rs.getString("application_no"),
            safeString(rs, "inquiry_no"),
            safeString(rs, "material_type"),
            safeString(rs, "currency"),
            nullableBigDecimal(rs, "fixed_freight_fee"),
            nullableBigDecimal(rs, "fixed_customs_fee"),
            nullableBigDecimal(rs, "fixed_crane_fee"),
            nullableBigDecimal(rs, "fixed_other_fee"),
            readTrafficService(safeString(rs, "traffic_service_json")),
            safeString(rs, "recipient_company"),
            safeString(rs, "handler_name"),
            safeString(rs, "handler_email"),
            rs.getString("vessel_name"),
            safeString(rs, "supply_port_code"),
            safeString(rs, "supply_port_name"),
            safeString(rs, "vessel_eta"),
            inquiryDate == null ? null : inquiryDate.toString(),
            rs.getString("source_file_name"),
            safeString(rs, "source_file_id"),
            rs.getString("document_type"),
            rs.getInt("header_row_index"),
            rs.getInt("sku_count"),
            rs.getInt("exact_count"),
            rs.getInt("similar_count"),
            rs.getInt("unmatched_count"),
            rs.getString("status"),
            timestampToString(rs.getTimestamp("created_at")),
            timestampToString(rs.getTimestamp("updated_at"))
        );
    }

    private MaterialDemandItemResponse item(ResultSet rs) throws SQLException {
        List<MaterialMatchCandidate> candidateSnapshot = readCandidates(rs.getString("candidates_json"));
        return new MaterialDemandItemResponse(
            rs.getLong("id"),
            rs.getString("document_type"),
            nullableInt(rs, "header_row_index"),
            nullableInt(rs, "sequence_no"),
            nullableInt(rs, "source_row_no"),
            nullableInt(rs, "source_row_number"),
            readRawColumns(rs.getString("raw_columns_json")),
            rs.getString("impa_code"),
            rs.getString("description"),
            rs.getString("size_model"),
            rs.getString("quantity"),
            rs.getString("unit"),
            rs.getString("remarks"),
            rs.getString("supplier_item_no"),
            rs.getString("raw_name_spec"),
            rs.getString("price"),
            rs.getString("packing"),
            rs.getString("stock"),
            rs.getString("selected_impa_code"),
            rs.getString("candidate_impa_code"),
            rs.getString("candidate_name_cn"),
            rs.getString("candidate_name_en"),
            rs.getString("candidate_spec"),
            rs.getString("match_result"),
            rs.getString("match_result_name"),
            rs.getString("reason"),
            safeString(rs, "validation_status"),
            safeString(rs, "validation_reason"),
            rs.getBoolean("has_image"),
            nullableInt(rs, "image_index"),
            rs.getString("image_anchor"),
            rs.getBigDecimal("actual_quote_price"),
            safeString(rs, "actual_quote_currency"),
            rs.getBigDecimal("quote_markup_percent"),
            nullableLong(rs, "quote_supplier_sku_id"),
            safeString(rs, "quote_selected_unit"),
            rs.getBigDecimal("quote_unit_price"),
            rs.getBigDecimal("quote_unit_price_usd"),
            safeString(rs, "quote_strategy_type"),
            candidateSnapshot,
            candidateSnapshot
        );
    }

    private List<MaterialMatchCandidate> candidateSnapshot(MaterialDemandItemRequest item) {
        if (item.candidateSnapshot() != null) {
            return item.candidateSnapshot();
        }
        if (item.candidates() != null) {
            return item.candidates();
        }
        return List.of();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize material demand JSON", ex);
        }
    }

    private String jsonObject(Object value) {
        try {
            if (value == null) {
                return null;
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize material demand object JSON", ex);
        }
    }

    private Map<String, String> readRawColumns(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, RAW_COLUMNS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read raw columns JSON", ex);
        }
    }

    private Map<String, Object> readTrafficService(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, TRAFFIC_SERVICE_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private List<MaterialMatchCandidate> readCandidates(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, CANDIDATES_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read candidates JSON", ex);
        }
    }

    private Integer nullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private java.math.BigDecimal nullableBigDecimal(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getBigDecimal(columnName);
        } catch (SQLException ex) {
            return null;
        }
    }

    private String timestampToString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toString();
    }

    private String safeString(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getString(columnName);
        } catch (SQLException ex) {
            return null;
        }
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
