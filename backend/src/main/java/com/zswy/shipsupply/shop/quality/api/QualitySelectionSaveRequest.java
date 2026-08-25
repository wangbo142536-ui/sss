package com.zswy.shipsupply.shop.quality.api;

public record QualitySelectionSaveRequest(
    String inspectionTime,
    String inspectionContent,
    String inspectionProcess,
    String reportFileId,
    String reportFileName,
    String inspectionConclusion
) {
}
