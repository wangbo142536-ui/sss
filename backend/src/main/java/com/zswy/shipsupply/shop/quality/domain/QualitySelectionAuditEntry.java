package com.zswy.shipsupply.shop.quality.domain;

public record QualitySelectionAuditEntry(
    String recordedAt,
    Long operatorUserId,
    String action,
    String inspectionContent,
    String inspectionProcess,
    String inspectionConclusion,
    String reportFileId,
    String reportFileName
) {
}
