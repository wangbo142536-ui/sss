package com.zswy.shipsupply.procurement.materials;

import java.util.Map;

public record MaterialQuoteRow(
    String documentType,
    String sourceFormat,
    int headerRowIndex,
    int sequence,
    int sourceRowNo,
    Map<String, String> rawColumns,
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
    boolean hasImage,
    Integer imageIndex,
    String imageAnchor,
    String imageMediaPath,
    String imageContentType
) {
}
