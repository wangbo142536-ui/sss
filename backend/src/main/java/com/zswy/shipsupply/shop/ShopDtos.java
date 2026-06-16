package com.zswy.shipsupply.shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

record ShopProfileResponse(
    Long shopId,
    Long companyId,
    String companyName,
    String shopName,
    String logoFileId,
    String logoUrl,
    String description,
    List<String> mainCategories,
    List<String> servicePorts,
    List<String> deliveryAreas,
    String contactName,
    String contactPhone,
    String contactEmail,
    String status,
    String createdAt,
    String updatedAt,
    boolean dataReady
) {
}

record ShopProfileSaveRequest(
    String shopName,
    String logoFileId,
    String logoUrl,
    String description,
    List<String> mainCategories,
    List<String> servicePorts,
    List<String> deliveryAreas,
    String contactName,
    String contactPhone,
    String contactEmail,
    String status
) {
}

record ShopSkuListResponse(
    List<ShopSkuResponse> items,
    long total,
    int page,
    int size
) {
}

record ShopSkuRequest(
    String productType,
    String categoryCode,
    String categoryName,
    String platformCode,
    String impaCode,
    String supplierSkuCode,
    String productName,
    List<ShopSkuAttributeRequest> specifications,
    BigDecimal stockQty,
    String stockUnit,
    Integer leadTimeDays,
    String deliveryArea,
    List<String> servicePorts,
    Integer monthlySales,
    BigDecimal unitPrice,
    String currency,
    String brand,
    String unit,
    String packageSpec,
    String barcode,
    String shelfStatus,
    String codeStatus,
    String exceptionReason,
    List<ShopSkuImageRequest> images,
    String imageFileId,
    String imageUrl,
    String thumbnailUrl,
    Long importBatchId,
    Integer importRowNumber,
    Map<String, Object> rawRow
) {
}

record ShopSkuAttributeRequest(
    String key,
    String name,
    String value,
    String unit,
    Integer sortOrder,
    String rawText
) {
}

record ShopSkuImageRequest(
    String fileId,
    String imageUrl,
    String thumbnailUrl,
    Boolean primary,
    Integer sortOrder
) {
}

record ShopSkuResponse(
    Long skuId,
    Long shopId,
    Long companyId,
    String productType,
    String categoryCode,
    String categoryName,
    String platformCode,
    String impaCode,
    String supplierSkuCode,
    String productName,
    List<ShopSkuAttributeResponse> specifications,
    String attributeSummary,
    BigDecimal stockQty,
    String stockUnit,
    Integer leadTimeDays,
    String deliveryArea,
    String imageUrl,
    String thumbnailUrl,
    String imageFileId,
    Integer monthlySales,
    BigDecimal unitPrice,
    String currency,
    String currencySymbol,
    String brand,
    String unit,
    String packageSpec,
    String barcode,
    String shelfStatus,
    String codeStatus,
    String exceptionReason,
    Long importBatchId,
    Integer importRowNumber,
    String createdAt,
    String updatedAt,
    List<ShopSkuImageResponse> images
) {
}

record ShopSkuAttributeResponse(
    Long attributeId,
    String key,
    String name,
    String value,
    String unit,
    Integer sortOrder,
    String rawText
) {
}

record ShopSkuImageResponse(
    Long imageId,
    String fileId,
    String imageUrl,
    String thumbnailUrl,
    Boolean primary,
    Integer sortOrder
) {
}

record ShopShelfStatusRequest(String shelfStatus) {
}

record ShopShelfStatusBatchRequest(
    List<Long> skuIds,
    String shelfStatus
) {
}

record ShopShelfStatusBatchResponse(
    String shelfStatus,
    int totalCount,
    List<ShopSkuResponse> items
) {
}

record ShopSkuUpsertItem(
    Long skuId,
    Long importBatchId,
    Integer importRowNumber,
    String supplierSkuCode,
    ShopSkuRequest sku
) {
    ShopSkuUpsertItem(Long skuId, ShopSkuRequest sku) {
        this(skuId, null, null, null, sku);
    }
}

record ShopSkuBatchUpsertRequest(
    Long importBatchId,
    List<ShopSkuUpsertItem> items
) {
    ShopSkuBatchUpsertRequest(List<ShopSkuUpsertItem> items) {
        this(null, items);
    }
}

record ShopSkuBatchUpsertResponse(
    int totalCount,
    int successCount,
    int insertedCount,
    int updatedCount,
    int failedCount,
    List<ShopSkuBatchUpsertRowResult> rowResults,
    List<ShopSkuResponse> items
) {
    ShopSkuBatchUpsertResponse(int totalCount, int successCount, List<ShopSkuResponse> items) {
        this(totalCount, successCount, successCount, 0, 0, List.of(), items);
    }
}

record ShopSkuBatchUpsertRowResult(
    Integer importRowNumber,
    String supplierSkuCode,
    Long skuId,
    String status,
    String errorCode,
    String message
) {
}

record ShopExceptionResolveRequest(
    String actionType,
    String platformCode,
    String impaCode,
    Map<String, Object> selectedCandidate,
    String reason
) {
}

record ShopImportPreviewResponse(
    Long batchId,
    String status,
    int totalCount,
    int successCount,
    int exceptionCount,
    List<ShopImportPreviewItem> items
) {
}

record ShopImportPreviewItem(
    Long previewRowId,
    Long skuId,
    Long shopId,
    Long companyId,
    String productType,
    String categoryCode,
    String categoryName,
    String platformCode,
    String impaCode,
    String supplierSkuCode,
    String productName,
    List<ShopSkuAttributeResponse> specifications,
    String attributeSummary,
    BigDecimal stockQty,
    String stockUnit,
    Integer leadTimeDays,
    String deliveryArea,
    String imageUrl,
    String thumbnailUrl,
    String imageFileId,
    Integer monthlySales,
    BigDecimal unitPrice,
    String currency,
    String currencySymbol,
    String brand,
    String unit,
    String packageSpec,
    String barcode,
    String shelfStatus,
    String codeStatus,
    String exceptionReason,
    Long importBatchId,
    Integer importRowNumber,
    Integer rowNo,
    String rawName,
    String rawSpec,
    String cleanName,
    List<ShopParsedAttribute> parsedAttributes,
    ShopImportRecommendation logicRecommendation,
    List<ShopCategoryCandidate> categoryCandidates,
    Map<String, String> rawColumns,
    Long existingSkuId,
    String previewAction,
    ShopSkuResponse existingSnapshot
) {
}

record ShopCategoryCandidate(
    String categoryCode,
    String categoryName,
    String segmentCode,
    String segmentName,
    String matchedKeyword,
    String reason,
    Integer itemCount
) {
}

record ShopParsedAttribute(
    String key,
    String name,
    String value,
    String unit,
    String rawText
) {
}

record ShopImportRecommendation(
    Boolean available,
    String status,
    String impaCode,
    String nameCn,
    String nameEn,
    String specification,
    String categoryCode,
    String categoryName,
    String unit,
    String matchReason,
    String confidenceLevel
) {
}

record ShopImportConfirmItem(
    Integer rowNo,
    String confirmedImpaCode,
    String confirmedPlatformCode,
    String selectedRecommendationSource
) {
}

record ShopImportConfirmRequest(
    Long batchId,
    List<ShopImportConfirmItem> items
) {
}

record ShopImportConfirmResponse(
    Long batchId,
    String status,
    int totalCount,
    int successCount,
    int exceptionCount,
    List<ShopSkuResponse> items
) {
}
