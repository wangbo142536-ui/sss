package com.zswy.shipsupply.procurement.materials;

import java.util.List;

public record MaterialMatchPreviewResponse(
    String documentType,
    String sourceFormat,
    int headerRowIndex,
    int totalRows,
    int exactCount,
    int similarCount,
    int unmatchedCount,
    List<MaterialMatchPreviewItem> items
) {
}
