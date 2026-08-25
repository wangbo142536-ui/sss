package com.zswy.shipsupply.shop.intelligent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

record IntelligentImportStartResponse(String jobId, Long batchId, String status) {
}

record IntelligentImportJobResponse(
    String jobId,
    Long batchId,
    String status,
    String stage,
    int stageIndex,
    int stageCount,
    int overallPercent,
    int stageProcessed,
    int stageTotal,
    String currentSheet,
    int currentChunk,
    int totalChunks,
    IntelligentImportCounts counts,
    String message,
    String errorCode,
    boolean retryable,
    String startedAt,
    String finishedAt,
    IntelligentImportPreview preview
) {
}

record IntelligentImportCounts(
    int sheetTotal,
    int sheetProcessed,
    int itemTotal,
    int itemProcessed,
    int materialCount,
    int foodCount,
    int impaMatchedCount,
    int categoryMatchedCount,
    int pendingReviewCount,
    int imageTotal,
    int imageProcessed,
    int failedCount
) {
}

record IntelligentImportPreview(
    Long batchId,
    String status,
    int totalCount,
    int successCount,
    int exceptionCount,
    List<IntelligentImportPreviewRow> items
) {
}

record IntelligentImportPreviewRow(
    Long previewRowId,
    String sourceItemId,
    String sourceSheet,
    int rowNo,
    String productType,
    String standardLibraryType,
    String standardCategoryCode,
    String categoryCode,
    String categoryName,
    String platformCode,
    String impaCode,
    String supplierSkuCode,
    String productName,
    String attributeSummary,
    BigDecimal stockQty,
    String stockUnit,
    BigDecimal unitPrice,
    String currency,
    String packageSpec,
    String barcode,
    String imageFileId,
    String imageUrl,
    String thumbnailUrl,
    String shelfStatus,
    String codeStatus,
    String exceptionReason,
    String previewAction,
    Map<String, String> rawColumns
) {
}

record IntelligentImportExecutionRequest(List<Long> previewRowIds) {
}
