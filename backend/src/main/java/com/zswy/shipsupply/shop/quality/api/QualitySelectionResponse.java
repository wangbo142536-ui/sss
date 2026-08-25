package com.zswy.shipsupply.shop.quality.api;

import java.util.List;

import com.zswy.shipsupply.shop.quality.domain.QualitySelectionAuditEntry;

public record QualitySelectionResponse(
    Long qualitySelectionId,
    Long companyId,
    Long skuId,
    String inspectionTime,
    String inspectionContent,
    String inspectionProcess,
    String reportFileId,
    String reportFileName,
    String reportUrl,
    String inspectionConclusion,
    List<QualitySelectionAuditEntry> auditTrail,
    String status,
    String updatedAt
) {
}
