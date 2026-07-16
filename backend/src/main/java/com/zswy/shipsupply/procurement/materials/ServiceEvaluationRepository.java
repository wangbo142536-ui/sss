package com.zswy.shipsupply.procurement.materials;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceEvaluationRepository {

    private static final String SELECT_COLUMNS = """
        SELECT evaluation.id, evaluation.settlement_id, evaluation.purchase_order_id,
               purchase.order_no AS purchase_order_no, evaluation.buyer_company_id,
               evaluation.provider_company_id, evaluation.provider_name, evaluation.service_type,
               evaluation.rating, evaluation.logistics_rating, evaluation.content, evaluation.attachments_json, evaluation.status,
               evaluation.reviewer, COALESCE(reviewer.full_name, reviewer.username) AS reviewer_name, evaluation.review_remark,
               DATE_FORMAT(evaluation.submitted_at, '%Y-%m-%d %H:%i:%s') AS submitted_at_text,
               DATE_FORMAT(evaluation.reviewed_at, '%Y-%m-%d %H:%i:%s') AS reviewed_at_text,
               DATE_FORMAT(evaluation.created_at, '%Y-%m-%d %H:%i:%s') AS created_at_text,
               DATE_FORMAT(evaluation.updated_at, '%Y-%m-%d %H:%i:%s') AS updated_at_text
        FROM service_evaluation evaluation
        JOIN purchase_order purchase ON purchase.id = evaluation.purchase_order_id
        LEFT JOIN sys_user reviewer ON reviewer.id = evaluation.reviewer
        """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ServiceEvaluationRepository(
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        SettlementOrderRepository settlementOrderRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS service_evaluation (
              id BIGINT NOT NULL AUTO_INCREMENT,
              settlement_id BIGINT NOT NULL,
              purchase_order_id BIGINT NOT NULL,
              buyer_company_id BIGINT NOT NULL,
              provider_company_id BIGINT NOT NULL,
              provider_name VARCHAR(200) NULL,
              service_type VARCHAR(30) NOT NULL,
              rating TINYINT NULL,
              logistics_rating TINYINT NULL,
              content TEXT NULL,
              attachments_json TEXT NULL,
              status VARCHAR(30) NOT NULL DEFAULT 'PENDING_EVALUATION',
              reviewer BIGINT NULL,
              review_remark VARCHAR(1000) NULL,
              submitted_at DATETIME NULL,
              reviewed_at DATETIME NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_evaluation_settlement (settlement_id),
              KEY idx_evaluation_buyer (buyer_company_id, status, created_at),
              KEY idx_evaluation_provider (provider_company_id, status, created_at),
              KEY idx_evaluation_review (status, submitted_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """);
        try {
            jdbcTemplate.execute("ALTER TABLE service_evaluation ADD COLUMN logistics_rating TINYINT NULL AFTER rating");
        } catch (Exception ignored) {
            // Column already exists on upgraded databases.
        }
        jdbcTemplate.update("""
            INSERT INTO service_evaluation (
              settlement_id, purchase_order_id, buyer_company_id, provider_company_id,
              provider_name, service_type, status, created_at, updated_at
            )
            SELECT settlement.id, settlement.purchase_order_id, settlement.buyer_company_id,
                   settlement.provider_company_id, settlement.provider_name,
                   settlement.settlement_type, 'PENDING_EVALUATION',
                   settlement.created_at, CURRENT_TIMESTAMP
            FROM settlement_order settlement
            ON DUPLICATE KEY UPDATE
              provider_name = VALUES(provider_name),
              service_type = VALUES(service_type),
              updated_at = service_evaluation.updated_at
            """);
    }

    public void ensureForSettlement(SettlementOrderResponse settlement) {
        jdbcTemplate.update("""
            INSERT INTO service_evaluation (
              settlement_id, purchase_order_id, buyer_company_id, provider_company_id,
              provider_name, service_type, status
            ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING_EVALUATION')
            ON DUPLICATE KEY UPDATE provider_name = VALUES(provider_name), service_type = VALUES(service_type)
            """, settlement.id(), settlement.purchaseOrderId(), settlement.buyerCompanyId(),
            settlement.providerCompanyId(), settlement.providerName(), settlement.settlementType());
    }

    public ServiceEvaluationListResponse listBuyer(Long companyId, String keyword, String status, int page, int size) {
        return list("evaluation.buyer_company_id = ?", companyId, keyword, status, page, size);
    }

    public ServiceEvaluationListResponse listRegulatory(String keyword, String status, int page, int size) {
        return list("1 = 1", null, keyword, status, page, size);
    }

    public Optional<ServiceEvaluationResponse> submit(
        Long buyerCompanyId, Long id, int rating, int logisticsRating, String content, String attachmentsJson
    ) {
        int updated = jdbcTemplate.update("""
            UPDATE service_evaluation
            SET rating = ?, logistics_rating = ?, content = ?, attachments_json = ?, status = 'PENDING_REVIEW',
                submitted_at = CURRENT_TIMESTAMP, reviewer = NULL, review_remark = NULL,
                reviewed_at = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ? AND status IN ('PENDING_EVALUATION', 'REJECTED')
            """, rating, logisticsRating, content, attachmentsJson, id, buyerCompanyId);
        return updated == 0 ? Optional.empty() : find(id);
    }

    public Optional<ServiceEvaluationResponse> review(Long reviewerId, Long id, String status, String remark) {
        int updated = jdbcTemplate.update("""
            UPDATE service_evaluation
            SET status = ?, reviewer = ?, review_remark = ?, reviewed_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'PENDING_REVIEW'
            """, status, reviewerId, remark, id);
        return updated == 0 ? Optional.empty() : find(id);
    }

    private Optional<ServiceEvaluationResponse> find(Long id) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE evaluation.id = ?", (rs, rowNum) -> map(rs), id)
            .stream().findFirst();
    }

    private ServiceEvaluationListResponse list(String ownerPredicate, Long companyId, String keyword, String status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE ").append(ownerPredicate);
        List<Object> args = new ArrayList<>();
        if (companyId != null) args.add(companyId);
        if (keyword != null) {
            String like = "%" + keyword + "%";
            where.append("""
                 AND (
                   purchase.order_no LIKE ?
                   OR evaluation.provider_name LIKE ?
                   OR evaluation.service_type LIKE ?
                   OR evaluation.content LIKE ?
                   OR evaluation.status LIKE ?
                 )
                """);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null) {
            where.append(" AND evaluation.status = ?");
            args.add(status);
        }
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM service_evaluation evaluation JOIN purchase_order purchase ON purchase.id = evaluation.purchase_order_id" + where,
            Long.class,
            args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(size);
        queryArgs.add((page - 1) * size);
        List<ServiceEvaluationResponse> items = jdbcTemplate.query(
            SELECT_COLUMNS + where + " ORDER BY evaluation.created_at DESC, evaluation.id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> map(rs), queryArgs.toArray());
        return new ServiceEvaluationListResponse(items, page, size, total == null ? 0 : total);
    }

    private ServiceEvaluationResponse map(ResultSet rs) throws SQLException {
        return new ServiceEvaluationResponse(
            rs.getLong("id"), rs.getLong("settlement_id"), rs.getLong("purchase_order_id"),
            rs.getString("purchase_order_no"), rs.getLong("buyer_company_id"),
            rs.getLong("provider_company_id"), rs.getString("provider_name"), rs.getString("service_type"),
            nullableInteger(rs, "rating"), nullableInteger(rs, "logistics_rating"), rs.getString("content"), attachments(rs.getString("attachments_json")),
            rs.getString("status"), nullableLong(rs, "reviewer"), rs.getString("reviewer_name"),
            rs.getString("review_remark"), rs.getString("submitted_at_text"), rs.getString("reviewed_at_text"),
            rs.getString("created_at_text"), rs.getString("updated_at_text")
        );
    }

    private List<FileSnapshot> attachments(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<FileSnapshot>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
