package com.zswy.shipsupply.procurement.materials;

import java.util.List;

public record MaterialParsedDocument(
    String documentType,
    String sourceFormat,
    int headerRowIndex,
    List<MaterialQuoteRow> rows
) {
}
