package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Map;

import com.zswy.shipsupply.common.material.MaterialNameAttribute;

public record MaterialMatchPreviewItem(
    String documentType,
    String sourceFormat,
    int headerRowIndex,
    int sequence,
    int sourceRowNo,
    int sourceRowNumber,
    Map<String, String> rawColumns,
    String cleanName,
    String coreName,
    List<MaterialNameAttribute> parsedAttributes,
    List<String> riskFlags,
    String impaCode,
    String description,
    String sizeModel,
    String quantity,
    String unit,
    String remarks,
    String supplierItemNo,
    String rawNameSpec,
    String price,
    String packing,
    String stock,
    String candidateImpaCode,
    String candidateNameCn,
    String candidateNameEn,
    String candidateSpec,
    String matchResult,
    String matchResultName,
    String reason,
    boolean hasImage,
    Integer imageIndex,
    String imageAnchor,
    List<MaterialMatchCandidate> candidates,
    List<MaterialSupplierCandidate> supplierCandidates
) {
}
