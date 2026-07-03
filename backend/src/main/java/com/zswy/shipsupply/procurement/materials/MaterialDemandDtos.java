package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

record MaterialDemandSaveRequest(
    Long demandId,
    String demandNo,
    String applicationNo,
    String inquiryNo,
    String materialType,
    String currency,
    String recipientCompany,
    String handlerName,
    String handlerEmail,
    String vesselName,
    String supplyPortCode,
    String supplyPortName,
    String vesselEta,
    String inquiryDate,
    String sourceFileName,
    String sourceFileId,
    String documentType,
    Integer headerRowIndex,
    List<MaterialDemandItemRequest> items
) {
    MaterialDemandSaveRequest(
        Long demandId,
        String demandNo,
        String applicationNo,
        String vesselName,
        String supplyPortCode,
        String supplyPortName,
        String vesselEta,
        String inquiryDate,
        String sourceFileName,
        String documentType,
        Integer headerRowIndex,
        List<MaterialDemandItemRequest> items
    ) {
        this(demandId, demandNo, applicationNo, null, null, null, null, null, null, vesselName, supplyPortCode, supplyPortName, vesselEta, inquiryDate, sourceFileName, null, documentType, headerRowIndex, items);
    }
}

record MaterialDemandItemRequest(
    String documentType,
    Integer headerRowIndex,
    Integer sequence,
    Integer sourceRowNo,
    Integer sourceRowNumber,
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
    String selectedImpaCode,
    String candidateImpaCode,
    String candidateNameCn,
    String candidateNameEn,
    String candidateSpec,
    String matchResult,
    String matchResultName,
    String reason,
    String validationStatus,
    String validationReason,
    boolean hasImage,
    Integer imageIndex,
    String imageAnchor,
    java.math.BigDecimal actualQuotePrice,
    String actualQuoteCurrency,
    java.math.BigDecimal quoteMarkupPercent,
    Long quoteSupplierSkuId,
    String quoteSelectedUnit,
    java.math.BigDecimal quoteUnitPrice,
    java.math.BigDecimal quoteUnitPriceUsd,
    String quoteStrategyType,
    List<MaterialMatchCandidate> candidateSnapshot,
    List<MaterialMatchCandidate> candidates
) {
    MaterialDemandItemRequest(
        String documentType,
        Integer headerRowIndex,
        Integer sequence,
        Integer sourceRowNo,
        Integer sourceRowNumber,
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
        String selectedImpaCode,
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
        List<MaterialMatchCandidate> candidateSnapshot,
        List<MaterialMatchCandidate> candidates
    ) {
        this(
            documentType,
            headerRowIndex,
            sequence,
            sourceRowNo,
            sourceRowNumber,
            rawColumns,
            impaCode,
            description,
            sizeModel,
            quantity,
            unit,
            remarks,
            supplierItemNo,
            rawNameSpec,
            price,
            packing,
            stock,
            selectedImpaCode,
            candidateImpaCode,
            candidateNameCn,
            candidateNameEn,
            candidateSpec,
            matchResult,
            matchResultName,
            reason,
            null,
            null,
            hasImage,
            imageIndex,
            imageAnchor,
            candidateSnapshot,
            candidates
        );
    }

    MaterialDemandItemRequest(
        String documentType,
        Integer headerRowIndex,
        Integer sequence,
        Integer sourceRowNo,
        Integer sourceRowNumber,
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
        String selectedImpaCode,
        String candidateImpaCode,
        String candidateNameCn,
        String candidateNameEn,
        String candidateSpec,
        String matchResult,
        String matchResultName,
        String reason,
        String validationStatus,
        String validationReason,
        boolean hasImage,
        Integer imageIndex,
        String imageAnchor,
        List<MaterialMatchCandidate> candidateSnapshot,
        List<MaterialMatchCandidate> candidates
    ) {
        this(
            documentType,
            headerRowIndex,
            sequence,
            sourceRowNo,
            sourceRowNumber,
            rawColumns,
            impaCode,
            description,
            sizeModel,
            quantity,
            unit,
            remarks,
            supplierItemNo,
            rawNameSpec,
            price,
            packing,
            stock,
            selectedImpaCode,
            candidateImpaCode,
            candidateNameCn,
            candidateNameEn,
            candidateSpec,
            matchResult,
            matchResultName,
            reason,
            validationStatus,
            validationReason,
            hasImage,
            imageIndex,
            imageAnchor,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            candidateSnapshot,
            candidates
        );
    }
}

record MaterialDemandSaveResponse(
    Long demandId,
    String demandNo,
    String status,
    String defaultRoute,
    String redirectTo
) {
}

record MaterialDemandStatusResponse(
    Long demandId,
    String status,
    int discardedPurchaseOrderCount
) {
}

record MaterialDemandListResponse(
    List<MaterialDemandSummaryResponse> items,
    int page,
    int size,
    long total
) {
}

record MaterialDemandSummaryResponse(
    Long demandId,
    String demandNo,
    String applicationNo,
    String inquiryNo,
    String materialType,
    String currency,
    String recipientCompany,
    String handlerName,
    String handlerEmail,
    String vesselName,
    String supplyPortCode,
    String supplyPortName,
    String vesselEta,
    String inquiryDate,
    String sourceFileName,
    String sourceFileId,
    String documentType,
    Integer headerRowIndex,
    int skuCount,
    int exactCount,
    int similarCount,
    int unmatchedCount,
    String status,
    String createdAt,
    String updatedAt
) {
    MaterialDemandSummaryResponse(
        Long demandId,
        String demandNo,
        String applicationNo,
        String vesselName,
        String supplyPortCode,
        String supplyPortName,
        String vesselEta,
        String inquiryDate,
        String sourceFileName,
        String documentType,
        Integer headerRowIndex,
        int skuCount,
        int exactCount,
        int similarCount,
        int unmatchedCount,
        String status,
        String createdAt,
        String updatedAt
    ) {
        this(demandId, demandNo, applicationNo, null, null, null, null, null, null, vesselName, supplyPortCode, supplyPortName, vesselEta, inquiryDate, sourceFileName, null, documentType, headerRowIndex, skuCount, exactCount, similarCount, unmatchedCount, status, createdAt, updatedAt);
    }

    @JsonProperty("itemCount")
    int itemCount() {
        return skuCount;
    }

    @JsonProperty("exactMatchCount")
    int exactMatchCount() {
        return exactCount;
    }

    @JsonProperty("similarMatchCount")
    int similarMatchCount() {
        return similarCount;
    }

    @JsonProperty("unmatchedMatchCount")
    int unmatchedMatchCount() {
        return unmatchedCount;
    }
}

record MaterialDemandDetailResponse(
    MaterialDemandSummaryResponse demand,
    List<MaterialDemandItemResponse> items
) {
}

record MaterialDemandItemResponse(
    Long itemId,
    String documentType,
    Integer headerRowIndex,
    Integer sequence,
    Integer sourceRowNo,
    Integer sourceRowNumber,
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
    String selectedImpaCode,
    String candidateImpaCode,
    String candidateNameCn,
    String candidateNameEn,
    String candidateSpec,
    String matchResult,
    String matchResultName,
    String reason,
    String validationStatus,
    String validationReason,
    boolean hasImage,
    Integer imageIndex,
    String imageAnchor,
    java.math.BigDecimal actualQuotePrice,
    String actualQuoteCurrency,
    java.math.BigDecimal quoteMarkupPercent,
    Long quoteSupplierSkuId,
    String quoteSelectedUnit,
    java.math.BigDecimal quoteUnitPrice,
    java.math.BigDecimal quoteUnitPriceUsd,
    String quoteStrategyType,
    List<MaterialMatchCandidate> candidateSnapshot,
    List<MaterialMatchCandidate> candidates
) {
    MaterialDemandItemResponse(
        Long itemId,
        String documentType,
        Integer headerRowIndex,
        Integer sequence,
        Integer sourceRowNo,
        Integer sourceRowNumber,
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
        String selectedImpaCode,
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
        List<MaterialMatchCandidate> candidateSnapshot,
        List<MaterialMatchCandidate> candidates
    ) {
        this(
            itemId,
            documentType,
            headerRowIndex,
            sequence,
            sourceRowNo,
            sourceRowNumber,
            rawColumns,
            impaCode,
            description,
            sizeModel,
            quantity,
            unit,
            remarks,
            supplierItemNo,
            rawNameSpec,
            price,
            packing,
            stock,
            selectedImpaCode,
            candidateImpaCode,
            candidateNameCn,
            candidateNameEn,
            candidateSpec,
            matchResult,
            matchResultName,
            reason,
            null,
            null,
            hasImage,
            imageIndex,
            imageAnchor,
            candidateSnapshot,
            candidates
        );
    }

    MaterialDemandItemResponse(
        Long itemId,
        String documentType,
        Integer headerRowIndex,
        Integer sequence,
        Integer sourceRowNo,
        Integer sourceRowNumber,
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
        String selectedImpaCode,
        String candidateImpaCode,
        String candidateNameCn,
        String candidateNameEn,
        String candidateSpec,
        String matchResult,
        String matchResultName,
        String reason,
        String validationStatus,
        String validationReason,
        boolean hasImage,
        Integer imageIndex,
        String imageAnchor,
        List<MaterialMatchCandidate> candidateSnapshot,
        List<MaterialMatchCandidate> candidates
    ) {
        this(
            itemId,
            documentType,
            headerRowIndex,
            sequence,
            sourceRowNo,
            sourceRowNumber,
            rawColumns,
            impaCode,
            description,
            sizeModel,
            quantity,
            unit,
            remarks,
            supplierItemNo,
            rawNameSpec,
            price,
            packing,
            stock,
            selectedImpaCode,
            candidateImpaCode,
            candidateNameCn,
            candidateNameEn,
            candidateSpec,
            matchResult,
            matchResultName,
            reason,
            validationStatus,
            validationReason,
            hasImage,
            imageIndex,
            imageAnchor,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            candidateSnapshot,
            candidates
        );
    }

    @JsonProperty("rowNo")
    Integer rowNo() {
        return sourceRowNo;
    }

    @JsonProperty("platformCode")
    String platformCode() {
        return impaCode;
    }

    @JsonProperty("productName")
    String productName() {
        return description;
    }

    @JsonProperty("specification")
    String specification() {
        return sizeModel;
    }
}

record MaterialDemandStats(
    int skuCount,
    int exactCount,
    int similarCount,
    int unmatchedCount
) {
}

record MaterialComparisonQuoteSaveRequest(
    String strategyType,
    java.math.BigDecimal markupPercent,
    List<MaterialComparisonQuoteItemRequest> items
) {
}

record MaterialComparisonQuoteItemRequest(
    Long demandItemId,
    Long skuId,
    String selectedUnit,
    String quantity,
    String remarks,
    java.math.BigDecimal unitPrice,
    java.math.BigDecimal unitPriceUsd,
    java.math.BigDecimal actualQuotePrice,
    java.math.BigDecimal quoteMarkupPercent,
    String currency
) {
}

record MaterialComparisonQuoteSaveResponse(
    Long demandId,
    int savedCount
) {
}
