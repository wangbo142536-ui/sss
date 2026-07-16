package com.zswy.shipsupply.procurement.materials;

import java.util.List;

record FileSnapshot(
    String fileId,
    String fileName,
    String fileUrl
) {
}

record FulfillmentAttachmentCreateRequest(
    String fileId,
    String fileName,
    String fileUrl,
    String nodeName,
    Long bookingId,
    Long trafficServiceOrderId,
    List<FileSnapshot> attachments
) {
}

record FulfillmentAttachmentResponse(
    Long id,
    String providerType,
    Long purchaseOrderId,
    Long supplierOrderId,
    Long trafficShuttleId,
    Long bookingId,
    Long trafficServiceOrderId,
    Integer nodeIndex,
    String nodeName,
    Long providerCompanyId,
    String providerName,
    String fileId,
    String fileName,
    String fileUrl,
    Long uploadedBy,
    String createdAt
) {
}

record FulfillmentAttachmentListResponse(
    List<FulfillmentAttachmentResponse> items
) {
}
