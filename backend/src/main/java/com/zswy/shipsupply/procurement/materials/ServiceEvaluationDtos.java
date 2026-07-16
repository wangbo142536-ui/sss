package com.zswy.shipsupply.procurement.materials;

import java.util.List;

record ServiceEvaluationSubmitRequest(
    Integer rating,
    Integer logisticsRating,
    String content,
    List<FileSnapshot> attachments
) { }

record ServiceEvaluationReviewRequest(
    String remark
) { }

record ServiceEvaluationResponse(
    Long id,
    Long settlementId,
    Long purchaseOrderId,
    String purchaseOrderNo,
    Long buyerCompanyId,
    Long providerCompanyId,
    String providerName,
    String serviceType,
    Integer rating,
    Integer logisticsRating,
    String content,
    List<FileSnapshot> attachments,
    String status,
    Long reviewer,
    String reviewerName,
    String reviewRemark,
    String submittedAt,
    String reviewedAt,
    String createdAt,
    String updatedAt
) { }

record ServiceEvaluationListResponse(
    List<ServiceEvaluationResponse> items,
    int page,
    int size,
    long total
) { }
