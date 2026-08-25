package com.zswy.shipsupply.shop.quality.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ShopSkuQualitySelection(
    Long qualitySelectionId,
    Long companyId,
    Long skuId,
    LocalDateTime inspectionTime,
    String inspectionContent,
    String inspectionProcess,
    String reportFileId,
    String reportFileName,
    String inspectionConclusion,
    List<QualitySelectionAuditEntry> auditTrail,
    String status,
    Long createdBy,
    Long updatedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
