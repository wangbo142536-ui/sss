package com.zswy.shipsupply.shop.quality.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.shop.quality.domain.QualitySelectionAuditEntry;
import com.zswy.shipsupply.shop.quality.domain.ShopSkuQualitySelection;

@Repository
public class ShopSkuQualitySelectionRepository {

    private static final TypeReference<List<QualitySelectionAuditEntry>> AUDIT_LIST_TYPE = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ShopSkuQualitySelectionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isSupplierSku(long companyId, long skuId) {
        Long count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM shop_sku sku
            JOIN company company ON company.id = sku.company_id
            WHERE sku.id = ? AND sku.company_id = ? AND company.company_type = 'SUPPLIER'
            """,
            Long.class,
            skuId,
            companyId
        );
        return count != null && count > 0;
    }

    public boolean isSupplierCompany(long companyId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM company WHERE id = ? AND company_type = 'SUPPLIER'",
            Long.class,
            companyId
        );
        return count != null && count > 0;
    }

    public boolean activeFileExists(String fileId) {
        if (fileId == null || fileId.isBlank()) return false;
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_file WHERE file_id = ? AND status = 'ACTIVE'",
            Long.class,
            fileId
        );
        return count != null && count > 0;
    }

    public String fileName(String fileId) {
        if (fileId == null || fileId.isBlank()) return null;
        return jdbcTemplate.query(
            "SELECT original_name FROM sys_file WHERE file_id = ? AND status = 'ACTIVE' LIMIT 1",
            (rs, rowNum) -> rs.getString("original_name"),
            fileId
        ).stream().findFirst().orElse(null);
    }

    public List<ShopSkuQualitySelection> listByCompany(long companyId) {
        return jdbcTemplate.query(
            """
            SELECT *
            FROM shop_sku_quality_selection
            WHERE company_id = ? AND status = 'ACTIVE'
            ORDER BY updated_at DESC, id DESC
            """,
            (rs, rowNum) -> qualitySelection(rs),
            companyId
        );
    }

    public Optional<ShopSkuQualitySelection> find(long companyId, long skuId) {
        return jdbcTemplate.query(
            "SELECT * FROM shop_sku_quality_selection WHERE company_id = ? AND sku_id = ? LIMIT 1",
            (rs, rowNum) -> qualitySelection(rs),
            companyId,
            skuId
        ).stream().findFirst();
    }

    public ShopSkuQualitySelection save(
        long companyId,
        long skuId,
        long operatorUserId,
        LocalDateTime inspectionTime,
        String inspectionContent,
        String inspectionProcess,
        String reportFileId,
        String reportFileName,
        String inspectionConclusion
    ) {
        Optional<ShopSkuQualitySelection> existing = find(companyId, skuId);
        List<QualitySelectionAuditEntry> auditTrail = new ArrayList<>(existing.map(ShopSkuQualitySelection::auditTrail).orElse(List.of()));
        auditTrail.add(new QualitySelectionAuditEntry(
            LocalDateTime.now().toString(),
            operatorUserId,
            existing.isPresent() ? "UPDATED" : "CREATED",
            inspectionContent,
            inspectionProcess,
            inspectionConclusion,
            reportFileId,
            reportFileName
        ));
        String auditJson = writeAuditTrail(auditTrail);
        jdbcTemplate.update(
            """
            INSERT INTO shop_sku_quality_selection
              (company_id, sku_id, inspection_time, inspection_content, inspection_process,
               inspection_report_file_id, inspection_report_file_name, inspection_conclusion,
               audit_trail_json, status, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), 'ACTIVE', ?, ?)
            ON DUPLICATE KEY UPDATE
              inspection_time = VALUES(inspection_time),
              inspection_content = VALUES(inspection_content),
              inspection_process = VALUES(inspection_process),
              inspection_report_file_id = VALUES(inspection_report_file_id),
              inspection_report_file_name = VALUES(inspection_report_file_name),
              inspection_conclusion = VALUES(inspection_conclusion),
              audit_trail_json = VALUES(audit_trail_json),
              status = 'ACTIVE',
              updated_by = VALUES(updated_by),
              updated_at = CURRENT_TIMESTAMP
            """,
            companyId,
            skuId,
            inspectionTime,
            inspectionContent,
            inspectionProcess,
            reportFileId,
            reportFileName,
            inspectionConclusion,
            auditJson,
            operatorUserId,
            operatorUserId
        );
        return find(companyId, skuId).orElseThrow();
    }

    private ShopSkuQualitySelection qualitySelection(ResultSet rs) throws SQLException {
        return new ShopSkuQualitySelection(
            rs.getLong("id"),
            rs.getLong("company_id"),
            rs.getLong("sku_id"),
            rs.getTimestamp("inspection_time").toLocalDateTime(),
            rs.getString("inspection_content"),
            rs.getString("inspection_process"),
            rs.getString("inspection_report_file_id"),
            rs.getString("inspection_report_file_name"),
            rs.getString("inspection_conclusion"),
            readAuditTrail(rs.getString("audit_trail_json")),
            rs.getString("status"),
            rs.getLong("created_by"),
            rs.getLong("updated_by"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private List<QualitySelectionAuditEntry> readAuditTrail(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, AUDIT_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeAuditTrail(List<QualitySelectionAuditEntry> auditTrail) {
        try {
            return objectMapper.writeValueAsString(auditTrail);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize quality selection audit trail", exception);
        }
    }
}
