package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Map;

public record MaterialMatchPreviewResponse(
    String documentType,
    String sourceFormat,
    String sourceFileId,
    String sourceFileName,
    String inquiryNo,
    String requestNo,
    String vesselName,
    String materialType,
    String currency,
    String suggestedPort,
    String eta,
    String recipientCompany,
    String handlerName,
    String handlerEmail,
    Map<String, String> rawHeaderFields,
    int headerRowIndex,
    int totalRows,
    int exactCount,
    int similarCount,
    int unmatchedCount,
    List<MaterialMatchPreviewItem> items
) {
    public MaterialMatchPreviewResponse(
        String documentType,
        String sourceFormat,
        int headerRowIndex,
        int totalRows,
        int exactCount,
        int similarCount,
        int unmatchedCount,
        List<MaterialMatchPreviewItem> items
    ) {
        this(
            documentType,
            sourceFormat,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Map.of(),
            headerRowIndex,
            totalRows,
            exactCount,
            similarCount,
            unmatchedCount,
            items
        );
    }
}
