package com.zswy.shipsupply.auth;

import java.util.List;

record EnterpriseProfileResponse(
    Long companyId,
    String companyName,
    String unifiedSocialCreditCode,
    String logoFileId,
    String logoUrl,
    String contactName,
    String contactPhone,
    String contactEmail,
    String companyType,
    String status
) {
}

record EnterpriseProfileSaveRequest(
    String companyName,
    String unifiedSocialCreditCode,
    String logoFileId,
    String logoUrl,
    String contactName,
    String contactPhone,
    String contactEmail
) {
}

record CompanyQualificationResponse(
    Long qualificationId,
    Long companyId,
    String fileId,
    String fileName,
    String fileUrl,
    String qualificationType,
    String title,
    String description,
    String status,
    String uploadedAt,
    String updatedAt
) {
}

record CompanyQualificationSaveRequest(
    String fileId,
    String fileName,
    String fileUrl,
    String qualificationType,
    String title,
    String description,
    String status
) {
}

record CompanyQualificationListResponse(
    List<CompanyQualificationResponse> items
) {
}
