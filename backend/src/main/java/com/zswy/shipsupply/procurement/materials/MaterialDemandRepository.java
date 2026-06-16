package com.zswy.shipsupply.procurement.materials;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
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
                  (company_id, created_by, updated_by, demand_no, application_no, vessel_name,
                   inquiry_date, source_file_name, document_type, header_row_index, sku_count, exact_count,
                   similar_count, unmatched_count, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SAVED')
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, companyId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setString(4, demandNo);
            statement.setString(5, request.applicationNo());
            statement.setString(6, request.vesselName());
            statement.setObject(7, LocalDate.parse(request.inquiryDate()));
            statement.setString(8, request.sourceFileName());
            statement.setString(9, request.documentType());
            statement.setInt(10, request.headerRowIndex());
            statement.setInt(11, stats.skuCount());
            statement.setInt(12, stats.exactCount());
            statement.setInt(13, stats.similarCount());
            statement.setInt(14, stats.unmatchedCount());
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
                vessel_name = ?,
                inquiry_date = ?,
                source_file_name = ?,
                document_type = ?,
                header_row_index = ?,
                sku_count = ?,
                exact_count = ?,
                similar_count = ?,
                unmatched_count = ?,
                status = 'SAVED'
            WHERE id = ? AND company_id = ?
            """,
            userId,
            request.applicationNo(),
            request.vesselName(),
            LocalDate.parse(request.inquiryDate()),
            request.sourceFileName(),
            request.documentType(),
            request.headerRowIndex(),
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
        for (MaterialDemandItemRequest item : items) {
            jdbcTemplate.update(
                """
                INSERT INTO material_demand_item
                  (demand_id, company_id, document_type, header_row_index, sequence_no,
                   source_row_no, source_row_number, raw_columns_json, impa_code,
                   description, size_model, quantity, unit, remarks, supplier_item_no,
                   raw_name_spec, price, packing, stock, selected_impa_code,
                   candidate_impa_code, candidate_name_cn, candidate_name_en,
                   candidate_spec, match_result, match_result_name, reason, has_image,
                   image_index, image_anchor, candidates_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
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
                item.hasImage() ? 1 : 0,
                item.imageIndex(),
                item.imageAnchor(),
                json(candidateSnapshot(item))
            );
        }
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

    public MaterialDemandListResponse list(
        long companyId,
        String keyword,
        String status,
        LocalDate dateFrom,
        LocalDate dateTo,
        int page,
        int size
    ) {
        long total = count(companyId, keyword, status, dateFrom, dateTo);
        int offset = (page - 1) * size;
        List<MaterialDemandSummaryResponse> items = jdbcTemplate.query(
            """
            SELECT *
            FROM material_demand
            WHERE company_id = ?
              AND (? IS NULL OR status = ?)
              AND (? IS NULL OR inquiry_date >= ?)
              AND (? IS NULL OR inquiry_date <= ?)
              AND (
                ? IS NULL
                OR demand_no LIKE CONCAT('%', ?, '%')
                OR application_no LIKE CONCAT('%', ?, '%')
                OR vessel_name LIKE CONCAT('%', ?, '%')
                OR source_file_name LIKE CONCAT('%', ?, '%')
              )
            ORDER BY updated_at DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> summary(rs),
            companyId,
            status,
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

    private long count(long companyId, String keyword, String status, LocalDate dateFrom, LocalDate dateTo) {
        Long total = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM material_demand
            WHERE company_id = ?
              AND (? IS NULL OR status = ?)
              AND (? IS NULL OR inquiry_date >= ?)
              AND (? IS NULL OR inquiry_date <= ?)
              AND (
                ? IS NULL
                OR demand_no LIKE CONCAT('%', ?, '%')
                OR application_no LIKE CONCAT('%', ?, '%')
                OR vessel_name LIKE CONCAT('%', ?, '%')
                OR source_file_name LIKE CONCAT('%', ?, '%')
              )
            """,
            Long.class,
            companyId,
            status,
            status,
            dateFrom,
            dateFrom,
            dateTo,
            dateTo,
            keyword,
            keyword,
            keyword,
            keyword,
            keyword
        );
        return total == null ? 0L : total;
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
            rs.getString("vessel_name"),
            inquiryDate == null ? null : inquiryDate.toString(),
            rs.getString("source_file_name"),
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
            rs.getBoolean("has_image"),
            nullableInt(rs, "image_index"),
            rs.getString("image_anchor"),
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

    private String timestampToString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toString();
    }
}
