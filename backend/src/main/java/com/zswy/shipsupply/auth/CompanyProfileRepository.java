package com.zswy.shipsupply.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public CompanyProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<EnterpriseProfileResponse> findProfile(long companyId) {
        return jdbcTemplate.query(
            """
            SELECT id, company_name, unified_social_credit_code, logo_file_id, logo_url,
                   contact_name, contact_phone, contact_email, company_type, status
            FROM company
            WHERE id = ?
            """,
            (rs, rowNum) -> profile(rs),
            companyId
        ).stream().findFirst();
    }

    public EnterpriseProfileResponse saveProfile(long companyId, EnterpriseProfileSaveRequest request) {
        EnterpriseProfileResponse current = findProfile(companyId).orElseThrow();
        jdbcTemplate.update(
            """
            UPDATE company
            SET company_name = ?,
                unified_social_credit_code = ?,
                logo_file_id = ?,
                logo_url = ?,
                contact_name = ?,
                contact_phone = ?,
                contact_email = ?
            WHERE id = ?
            """,
            value(request.companyName(), current.companyName()),
            value(request.unifiedSocialCreditCode(), current.unifiedSocialCreditCode()),
            value(request.logoFileId(), current.logoFileId()),
            value(request.logoUrl(), current.logoUrl()),
            value(request.contactName(), current.contactName()),
            value(request.contactPhone(), current.contactPhone()),
            value(request.contactEmail(), current.contactEmail()),
            companyId
        );
        return findProfile(companyId).orElseThrow();
    }

    public List<CompanyQualificationResponse> listQualifications(long companyId, String status) {
        return jdbcTemplate.query(
            """
            SELECT qualification.id, qualification.company_id, qualification.file_id,
                   qualification.file_name, qualification.file_type, qualification.title,
                   qualification.description, qualification.status, qualification.created_at,
                   qualification.updated_at
            FROM company_qualification qualification
            WHERE qualification.company_id = ?
              AND qualification.status = ?
            ORDER BY qualification.created_at DESC, qualification.id DESC
            """,
            (rs, rowNum) -> qualification(rs),
            companyId,
            status
        );
    }

    public CompanyQualificationResponse createQualification(long companyId, CompanyQualificationSaveRequest request) {
        jdbcTemplate.update(
            """
            INSERT INTO company_qualification
              (company_id, file_id, file_type, file_name, title, description, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            companyId,
            request.fileId(),
            value(request.qualificationType(), "BUSINESS_LICENSE"),
            value(request.fileName(), fileName(request.fileId())),
            request.title(),
            request.description(),
            value(request.status(), CompanyProfileService.DEFAULT_QUALIFICATION_STATUS)
        );
        return listQualifications(companyId, value(request.status(), CompanyProfileService.DEFAULT_QUALIFICATION_STATUS)).stream().findFirst().orElseThrow();
    }

    public Optional<CompanyQualificationResponse> updateQualification(long companyId, long qualificationId, CompanyQualificationSaveRequest request) {
        CompanyQualificationResponse current = findQualification(companyId, qualificationId).orElse(null);
        if (current == null) {
            return Optional.empty();
        }
        int updated = jdbcTemplate.update(
            """
            UPDATE company_qualification
            SET file_id = ?,
                file_type = ?,
                file_name = ?,
                title = ?,
                description = ?,
                status = ?
            WHERE company_id = ? AND id = ?
            """,
            value(request.fileId(), current.fileId()),
            value(request.qualificationType(), current.qualificationType()),
            value(request.fileName(), current.fileName()),
            value(request.title(), current.title()),
            value(request.description(), current.description()),
            value(request.status(), current.status()),
            companyId,
            qualificationId
        );
        return updated == 0 ? Optional.empty() : findQualification(companyId, qualificationId);
    }

    public boolean deleteQualification(long companyId, long qualificationId) {
        return softDeleteQualification(companyId, qualificationId);
    }

    public boolean softDeleteQualification(long companyId, long qualificationId) {
        return jdbcTemplate.update(
            """
            UPDATE company_qualification
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ? AND id = ?
            """,
            CompanyProfileService.DELETED_QUALIFICATION_STATUS,
            companyId,
            qualificationId
        ) > 0;
    }

    public Optional<CompanyQualificationResponse> findQualification(long companyId, long qualificationId) {
        return jdbcTemplate.query(
            """
            SELECT id, company_id, file_id, file_name, file_type, title, description,
                   status, created_at, updated_at
            FROM company_qualification
            WHERE company_id = ? AND id = ?
            """,
            (rs, rowNum) -> qualification(rs),
            companyId,
            qualificationId
        ).stream().findFirst();
    }

    public List<CompanyContactResponse> listContacts(long companyId, String status) {
        return jdbcTemplate.query(
            """
            SELECT id, company_id, contact_name, contact_phone, contact_email, status, created_at, updated_at
            FROM company_contact
            WHERE company_id = ?
              AND status = ?
            ORDER BY updated_at DESC, id DESC
            """,
            (rs, rowNum) -> contact(rs),
            companyId,
            status
        );
    }

    public CompanyContactResponse createContact(long companyId, CompanyContactSaveRequest request) {
        jdbcTemplate.update(
            """
            INSERT INTO company_contact
              (company_id, contact_name, contact_phone, contact_email, status)
            VALUES (?, ?, ?, ?, ?)
            """,
            companyId,
            request.contactName(),
            request.contactPhone(),
            request.contactEmail(),
            value(request.status(), CompanyProfileService.DEFAULT_CONTACT_STATUS)
        );
        return listContacts(companyId, value(request.status(), CompanyProfileService.DEFAULT_CONTACT_STATUS)).stream().findFirst().orElseThrow();
    }

    public Optional<CompanyContactResponse> updateContact(long companyId, long contactId, CompanyContactSaveRequest request) {
        CompanyContactResponse current = findContact(companyId, contactId).orElse(null);
        if (current == null) {
            return Optional.empty();
        }
        int updated = jdbcTemplate.update(
            """
            UPDATE company_contact
            SET contact_name = ?,
                contact_phone = ?,
                contact_email = ?,
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ? AND id = ?
            """,
            value(request.contactName(), current.contactName()),
            value(request.contactPhone(), current.contactPhone()),
            request.contactEmail() == null ? current.contactEmail() : request.contactEmail(),
            value(request.status(), current.status()),
            companyId,
            contactId
        );
        return updated == 0 ? Optional.empty() : findContact(companyId, contactId);
    }

    public boolean softDeleteContact(long companyId, long contactId) {
        return jdbcTemplate.update(
            """
            UPDATE company_contact
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE company_id = ? AND id = ?
            """,
            CompanyProfileService.DELETED_CONTACT_STATUS,
            companyId,
            contactId
        ) > 0;
    }

    public Optional<CompanyContactResponse> findContact(long companyId, long contactId) {
        return jdbcTemplate.query(
            """
            SELECT id, company_id, contact_name, contact_phone, contact_email, status, created_at, updated_at
            FROM company_contact
            WHERE company_id = ? AND id = ?
            """,
            (rs, rowNum) -> contact(rs),
            companyId,
            contactId
        ).stream().findFirst();
    }

    private EnterpriseProfileResponse profile(ResultSet rs) throws SQLException {
        return new EnterpriseProfileResponse(
            rs.getLong("id"),
            rs.getString("company_name"),
            rs.getString("unified_social_credit_code"),
            rs.getString("logo_file_id"),
            rs.getString("logo_url"),
            rs.getString("contact_name"),
            rs.getString("contact_phone"),
            rs.getString("contact_email"),
            rs.getString("company_type"),
            rs.getString("status")
        );
    }

    private CompanyQualificationResponse qualification(ResultSet rs) throws SQLException {
        String fileId = rs.getString("file_id");
        return new CompanyQualificationResponse(
            rs.getLong("id"),
            rs.getLong("company_id"),
            fileId,
            rs.getString("file_name"),
            fileId == null ? null : "/api/files/" + fileId,
            rs.getString("file_type"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("status"),
            string(rs.getTimestamp("created_at")),
            string(rs.getTimestamp("updated_at"))
        );
    }

    private CompanyContactResponse contact(ResultSet rs) throws SQLException {
        return new CompanyContactResponse(
            rs.getLong("id"),
            rs.getLong("company_id"),
            rs.getString("contact_name"),
            rs.getString("contact_phone"),
            rs.getString("contact_email"),
            rs.getString("status"),
            string(rs.getTimestamp("created_at")),
            string(rs.getTimestamp("updated_at"))
        );
    }

    private String fileName(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return jdbcTemplate.query(
            "SELECT original_name FROM sys_file WHERE file_id = ? LIMIT 1",
            (rs, rowNum) -> rs.getString("original_name"),
            fileId
        ).stream().findFirst().orElse(fileId);
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String string(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }
}

