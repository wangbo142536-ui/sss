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

record CompanyContactResponse(
    Long contactId,
    Long companyId,
    String contactName,
    String contactPhone,
    String contactEmail,
    String status,
    String createdAt,
    String updatedAt
) {
}

record CompanyContactSaveRequest(
    String contactName,
    String contactPhone,
    String contactEmail,
    String status
) {
}

record CompanyContactListResponse(
    List<CompanyContactResponse> items
) {
}

record CompanyVesselResponse(
    Long vesselId,
    Long companyId,
    String vesselName,
    String vesselType,
    String buildDate,
    String nextMaintenanceDate,
    String capacity,
    String status,
    String remark,
    String createdAt,
    String updatedAt
) {
}

record CompanyVesselSaveRequest(
    String vesselName,
    String vesselType,
    String buildDate,
    String nextMaintenanceDate,
    String capacity,
    String status,
    String remark
) {
}

record CompanyVesselListResponse(
    List<CompanyVesselResponse> items
) {
}

record CompanyValueAddedServiceResponse(
    Long companyId,
    java.math.BigDecimal freightPrice,
    java.math.BigDecimal customsPrice,
    java.math.BigDecimal cranePrice,
    String remark,
    String updatedAt
) {
}

record CompanyValueAddedServiceSaveRequest(
    java.math.BigDecimal freightPrice,
    java.math.BigDecimal customsPrice,
    java.math.BigDecimal cranePrice,
    String remark
) {
}
