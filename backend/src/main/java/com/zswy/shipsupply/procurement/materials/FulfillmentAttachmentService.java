package com.zswy.shipsupply.procurement.materials;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class FulfillmentAttachmentService {

    private final CurrentUserService currentUserService;
    private final FulfillmentAttachmentRepository repository;

    public FulfillmentAttachmentService(CurrentUserService currentUserService, FulfillmentAttachmentRepository repository) {
        this.currentUserService = currentUserService;
        this.repository = repository;
    }

    public FulfillmentAttachmentListResponse listForOrder(String authorizationHeader, Long orderId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (repository.buyerOwnsOrder(user.companyId(), orderId)) {
            return new FulfillmentAttachmentListResponse(repository.listForPurchaseOrder(orderId));
        }
        if (repository.supplierHasOrder(user.companyId(), orderId)
            || repository.bargeHasOrder(user.companyId(), orderId)) {
            return new FulfillmentAttachmentListResponse(
                repository.listProviderForPurchaseOrder(user.companyId(), orderId)
            );
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND");
    }

    public FulfillmentAttachmentListResponse listForShuttle(String authorizationHeader, Long shuttleId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!repository.shuttleOwnedBy(user.companyId(), shuttleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND");
        }
        return new FulfillmentAttachmentListResponse(repository.listForShuttle(shuttleId));
    }

    @Transactional
    public void deleteAttachment(String authorizationHeader, Long attachmentId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!repository.deleteOwnedAttachment(user.companyId(), attachmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FULFILLMENT_ATTACHMENT_NOT_FOUND");
        }
    }

    @Transactional
    public FulfillmentAttachmentListResponse saveSupplier(
        String authorizationHeader,
        Long orderId,
        Long supplierOrderId,
        FulfillmentAttachmentCreateRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!repository.supplierOwnsOrder(user.companyId(), orderId, supplierOrderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_ORDER_NOT_FOUND");
        }
        for (FileSnapshot file : files(request)) {
            repository.saveSupplier(user.companyId(), user.userId(), orderId, supplierOrderId, file);
        }
        return new FulfillmentAttachmentListResponse(repository.listForPurchaseOrder(orderId));
    }

    @Transactional
    public FulfillmentAttachmentListResponse saveShuttleNode(
        String authorizationHeader,
        Long shuttleId,
        Integer nodeIndex,
        FulfillmentAttachmentCreateRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!repository.shuttleOwnedBy(user.companyId(), shuttleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND");
        }
        List<FulfillmentAttachmentResponse> items = List.of();
        for (FileSnapshot file : files(request)) {
            items = repository.saveShuttle(
                user.companyId(), user.userId(), shuttleId, nodeIndex,
                text(request == null ? null : request.nodeName()),
                request == null ? null : request.bookingId(),
                request == null ? null : request.trafficServiceOrderId(), file
            );
        }
        return new FulfillmentAttachmentListResponse(items);
    }

    @Transactional
    public TrafficShuttleExecutionResponse startShuttle(String authorizationHeader, Long shuttleId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        String status = repository.changeShuttleStatus(user.companyId(), shuttleId, "IN_PROGRESS")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND"));
        repository.changeRelatedTrafficOrderStatus(user.companyId(), shuttleId, "IN_PROGRESS");
        return new TrafficShuttleExecutionResponse(shuttleId, status, repository.listForShuttle(shuttleId));
    }

    @Transactional
    public TrafficShuttleExecutionResponse completeShuttle(String authorizationHeader, Long shuttleId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!repository.shuttleOwnedBy(user.companyId(), shuttleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SHUTTLE_NOT_FOUND");
        }
        if (!repository.hasShuttleAttachment(shuttleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ATTACHMENT_REQUIRED");
        }
        String status = repository.changeShuttleStatus(user.companyId(), shuttleId, "COMPLETED").orElseThrow();
        repository.changeRelatedTrafficOrderStatus(user.companyId(), shuttleId, "COMPLETED");
        return new TrafficShuttleExecutionResponse(shuttleId, status, repository.listForShuttle(shuttleId));
    }

    void saveLegacySupplierImage(Long companyId, Long userId, Long orderId, Long supplierOrderId, String fileId, String fileUrl) {
        String normalizedUrl = text(fileUrl);
        String normalizedId = text(fileId);
        if (normalizedUrl == null && normalizedId != null) normalizedUrl = "/api/files/" + normalizedId;
        if (normalizedUrl == null) return;
        repository.saveSupplier(companyId, userId, orderId, supplierOrderId,
            new FileSnapshot(normalizedId, normalizedId == null ? "delivery-image" : normalizedId, normalizedUrl));
    }

    boolean hasSupplierAttachment(Long supplierOrderId) {
        return repository.hasSupplierAttachment(supplierOrderId);
    }

    private FileSnapshot file(FulfillmentAttachmentCreateRequest request) {
        String fileId = text(request == null ? null : request.fileId());
        String fileName = text(request == null ? null : request.fileName());
        String fileUrl = text(request == null ? null : request.fileUrl());
        if (fileUrl == null && fileId != null) fileUrl = "/api/files/" + fileId;
        if (fileName == null && fileId != null) fileName = fileId;
        if (fileName == null || fileUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ATTACHMENT_FILE_REQUIRED");
        }
        return new FileSnapshot(fileId, fileName, fileUrl);
    }

    private List<FileSnapshot> files(FulfillmentAttachmentCreateRequest request) {
        if (request != null && request.attachments() != null && !request.attachments().isEmpty()) {
            return request.attachments().stream().map(this::file).toList();
        }
        return List.of(file(request));
    }

    private FileSnapshot file(FileSnapshot file) {
        if (file == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ATTACHMENT_FILE_REQUIRED");
        String fileId = text(file.fileId());
        String fileName = text(file.fileName());
        String fileUrl = text(file.fileUrl());
        if (fileUrl == null && fileId != null) fileUrl = "/api/files/" + fileId;
        if (fileName == null && fileId != null) fileName = fileId;
        if (fileName == null || fileUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ATTACHMENT_FILE_REQUIRED");
        }
        return new FileSnapshot(fileId, fileName, fileUrl);
    }

    private String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

record TrafficShuttleExecutionResponse(
    Long shuttleId,
    String status,
    List<FulfillmentAttachmentResponse> attachments
) { }
