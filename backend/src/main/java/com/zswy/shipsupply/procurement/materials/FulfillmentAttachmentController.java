package com.zswy.shipsupply.procurement.materials;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FulfillmentAttachmentController {

    private final FulfillmentAttachmentService service;

    public FulfillmentAttachmentController(FulfillmentAttachmentService service) {
        this.service = service;
    }

    @GetMapping("/api/procurement/purchase-orders/{orderId}/fulfillment-attachments")
    public FulfillmentAttachmentListResponse listForOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId
    ) {
        return service.listForOrder(authorizationHeader, orderId);
    }

    @PostMapping("/api/supplier/purchase-orders/{orderId}/supplier-orders/{supplierOrderId}/fulfillment-attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public FulfillmentAttachmentListResponse saveSupplier(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId,
        @PathVariable Long supplierOrderId,
        @RequestBody FulfillmentAttachmentCreateRequest request
    ) {
        return service.saveSupplier(authorizationHeader, orderId, supplierOrderId, request);
    }

    @PostMapping("/api/supplier/traffic-shuttles/{shuttleId}/nodes/{nodeIndex}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public FulfillmentAttachmentListResponse saveShuttleNode(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId,
        @PathVariable Integer nodeIndex,
        @RequestBody FulfillmentAttachmentCreateRequest request
    ) {
        return service.saveShuttleNode(authorizationHeader, shuttleId, nodeIndex, request);
    }

    @GetMapping("/api/supplier/traffic-shuttles/{shuttleId}/attachments")
    public FulfillmentAttachmentListResponse listForShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId
    ) {
        return service.listForShuttle(authorizationHeader, shuttleId);
    }

    @DeleteMapping("/api/supplier/fulfillment-attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long attachmentId
    ) {
        service.deleteAttachment(authorizationHeader, attachmentId);
    }

    @PostMapping("/api/supplier/traffic-shuttles/{shuttleId}/start")
    public TrafficShuttleExecutionResponse startShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId
    ) {
        return service.startShuttle(authorizationHeader, shuttleId);
    }

    @PostMapping("/api/supplier/traffic-shuttles/{shuttleId}/complete")
    public TrafficShuttleExecutionResponse completeShuttle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long shuttleId
    ) {
        return service.completeShuttle(authorizationHeader, shuttleId);
    }
}
