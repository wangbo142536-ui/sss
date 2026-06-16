package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

record MaterialDemandSaveRequest(
    Long demandId,
    String demandNo,
    String applicationNo,
    String vesselName,
    String inquiryDate,
    String sourceFileName,
    String documentType,
    Integer headerRowIndex,
    List<MaterialDemandItemRequest> items
) {
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
    boolean hasImage,
    Integer imageIndex,
    String imageAnchor,
    List<MaterialMatchCandidate> candidateSnapshot,
    List<MaterialMatchCandidate> candidates
) {
}

record MaterialDemandSaveResponse(
    Long demandId,
    String demandNo,
    String status,
    String defaultRoute,
    String redirectTo
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
    String vesselName,
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
    boolean hasImage,
    Integer imageIndex,
    String imageAnchor,
    List<MaterialMatchCandidate> candidateSnapshot,
    List<MaterialMatchCandidate> candidates
) {
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
