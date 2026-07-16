package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class SettlementOrderService {

    private static final String SUPPLIER = "SUPPLIER";
    private static final String BARGE = "BARGE";
    private static final String PENDING_INVOICE = "PENDING_INVOICE";
    private static final String PENDING_SETTLEMENT = "PENDING_SETTLEMENT";
    private static final String SETTLED = "SETTLED";

    private final CurrentUserService currentUserService;
    private final SettlementOrderRepository settlementOrderRepository;
    private final ObjectMapper objectMapper;
    private final ServiceEvaluationRepository serviceEvaluationRepository;

    public SettlementOrderService(
        CurrentUserService currentUserService,
        SettlementOrderRepository settlementOrderRepository,
        ObjectMapper objectMapper,
        ServiceEvaluationRepository serviceEvaluationRepository
    ) {
        this.currentUserService = currentUserService;
        this.settlementOrderRepository = settlementOrderRepository;
        this.objectMapper = objectMapper;
        this.serviceEvaluationRepository = serviceEvaluationRepository;
    }

    @Transactional
    public void ensureForPurchaseOrder(Long buyerCompanyId, Long userId, Long purchaseOrderId) {
        SettlementPurchaseContext purchase = settlementOrderRepository.purchaseContext(buyerCompanyId, purchaseOrderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND"));
        for (SupplierSettlementSource source : settlementOrderRepository.supplierSources(buyerCompanyId, purchaseOrderId)) {
            serviceEvaluationRepository.ensureForSettlement(upsertSupplier(purchase, userId, source, PENDING_INVOICE));
        }
        Map<String, Object> trafficService = trafficService(purchase.trafficServiceJson());
        Long snapshotOrderId = longValue(trafficService.get("trafficServiceOrderId"));
        settlementOrderRepository.linkExistingTrafficSource(
            buyerCompanyId, purchaseOrderId, purchase.demandId(), snapshotOrderId
        ).ifPresent(source -> serviceEvaluationRepository.ensureForSettlement(
            upsertBarge(purchase, userId, source, PENDING_INVOICE)
        ));
    }

    public SettlementOrderListResponse listBuyer(String authorizationHeader, String keyword, String status, int page, int size) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return settlementOrderRepository.listBuyer(user.companyId(), text(keyword), normalizedStatus(status), safePage(page), safeSize(size));
    }

    public SettlementOrderListResponse listSupplier(String authorizationHeader, String keyword, String status, int page, int size) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return settlementOrderRepository.listSupplier(user.companyId(), text(keyword), normalizedStatus(status), safePage(page), safeSize(size));
    }

    public SettlementOrderListResponse listBarge(String authorizationHeader, String keyword, String status, int page, int size) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return settlementOrderRepository.listBarge(user.companyId(), text(keyword), normalizedStatus(status), safePage(page), safeSize(size));
    }

    public SettlementOrderListResponse list(
        String authorizationHeader,
        String scope,
        String keyword,
        String status,
        int page,
        int size
    ) {
        String normalizedScope = text(scope) == null ? "BUYER" : scope.trim().toUpperCase();
        return switch (normalizedScope) {
            case "BUYER" -> listBuyer(authorizationHeader, keyword, status, page, size);
            case "SUPPLIER" -> listSupplier(authorizationHeader, keyword, status, page, size);
            case "BARGE" -> listBarge(authorizationHeader, keyword, status, page, size);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SETTLEMENT_SCOPE");
        };
    }

    @Transactional
    public SettlementBatchResponse createBatch(String authorizationHeader, SettlementBatchRequest request) {
        return saveBatch(authorizationHeader, request, PENDING_INVOICE);
    }

    @Transactional
    public SettlementBatchResponse settleBatch(String authorizationHeader, SettlementBatchRequest request) {
        return saveBatch(authorizationHeader, request, PENDING_INVOICE);
    }

    @Transactional
    public SettlementOrderResponse updateActualAmount(String authorizationHeader, Long settlementId, SettlementAmountUpdateRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        BigDecimal actualAmount = validAmount(request == null ? null : request.actualAmount(), "ACTUAL_AMOUNT_REQUIRED");
        return settlementOrderRepository.updateProviderActualAmount(user.companyId(), settlementId, actualAmount)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "ACTUAL_AMOUNT_LOCKED_OR_SETTLEMENT_NOT_FOUND"));
    }

    @Transactional
    public SettlementOrderResponse submitInvoice(
        String authorizationHeader, Long settlementId, SettlementInvoiceSubmitRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        BigDecimal actualAmount = validAmount(request == null ? null : request.actualAmount(), "ACTUAL_AMOUNT_REQUIRED");
        List<FileSnapshot> attachments = validAttachments(request == null ? null : request.invoiceAttachments());
        if (attachments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVOICE_ATTACHMENT_REQUIRED");
        }
        return settlementOrderRepository.submitInvoice(user.companyId(), settlementId, actualAmount, json(attachments))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "SETTLEMENT_NOT_PENDING_INVOICE"));
    }

    @Transactional
    public SettlementOrderResponse settle(String authorizationHeader, Long settlementId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return settlementOrderRepository.settle(user.companyId(), settlementId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "SETTLEMENT_NOT_PENDING_SETTLEMENT"));
    }

    @Transactional
    public SettlementOrderResponse pay(String authorizationHeader, Long settlementId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return settlementOrderRepository.pay(user.companyId(), settlementId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "SETTLEMENT_NOT_WAITING_PAYMENT"));
    }

    @Transactional
    public void delete(String authorizationHeader, Long settlementId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!settlementOrderRepository.delete(user.companyId(), settlementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SETTLEMENT_NOT_FOUND");
        }
    }

    private SettlementBatchResponse saveBatch(String authorizationHeader, SettlementBatchRequest request, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null || request.purchaseOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PURCHASE_ORDER_ID_REQUIRED");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SETTLEMENT_ITEMS_REQUIRED");
        }
        SettlementPurchaseContext purchase = settlementOrderRepository.purchaseContext(user.companyId(), request.purchaseOrderId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND"));
        List<SettlementOrderResponse> saved = new ArrayList<>();
        for (SettlementSourceRequest item : request.items()) {
            String sourceType = sourceType(item == null ? null : item.sourceType());
            saved.add(SUPPLIER.equals(sourceType)
                ? saveSupplier(purchase, user, item, status)
                : saveBarge(purchase, user, item, status));
        }
        saved.forEach(serviceEvaluationRepository::ensureForSettlement);
        return new SettlementBatchResponse(purchase.purchaseOrderId(), status, saved.size(), saved);
    }

    private SettlementOrderResponse saveSupplier(
        SettlementPurchaseContext purchase,
        CurrentUserContext user,
        SettlementSourceRequest item,
        String status
    ) {
        if (item.sourceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER_ORDER_ID_REQUIRED");
        }
        SupplierSettlementSource source = settlementOrderRepository.supplierSource(
            purchase.buyerCompanyId(),
            purchase.purchaseOrderId(),
            item.sourceId()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_ORDER_NOT_FOUND"));
        return upsertSupplier(purchase, user.userId(), source, status);
    }

    private SettlementOrderResponse upsertSupplier(
        SettlementPurchaseContext purchase, Long userId, SupplierSettlementSource source, String status
    ) {
        return settlementOrderRepository.upsert(new SettlementDraft(
            settlementOrderRepository.nextSettlementNo(),
            SUPPLIER + ":" + source.supplierOrderId(),
            purchase.purchaseOrderId(),
            source.supplierOrderId(),
            null,
            purchase.buyerCompanyId(),
            source.providerCompanyId(),
            source.providerName(),
            SUPPLIER,
            purchase.materialType(),
            source.quotedAmount(),
            BigDecimal.ZERO,
            firstText(source.currency(), purchase.currency()),
            status,
            userId
        ));
    }

    private SettlementOrderResponse saveBarge(
        SettlementPurchaseContext purchase,
        CurrentUserContext user,
        SettlementSourceRequest item,
        String status
    ) {
        TrafficSettlementSource source;
        if (item.sourceId() != null) {
            source = settlementOrderRepository.trafficSource(
                purchase.buyerCompanyId(),
                purchase.purchaseOrderId(),
                item.sourceId()
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_ORDER_NOT_FOUND"));
        } else {
            source = settlementOrderRepository.trafficSource(
                purchase.buyerCompanyId(),
                purchase.purchaseOrderId(),
                null
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRAFFIC_SERVICE_ORDER_NOT_FOUND"));
        }
        return upsertBarge(purchase, user.userId(), source, status);
    }

    private SettlementOrderResponse upsertBarge(
        SettlementPurchaseContext purchase, Long userId, TrafficSettlementSource source, String status
    ) {
        return settlementOrderRepository.upsert(new SettlementDraft(
            settlementOrderRepository.nextSettlementNo(),
            BARGE,
            purchase.purchaseOrderId(),
            null,
            source.trafficServiceOrderId(),
            purchase.buyerCompanyId(),
            source.providerCompanyId(),
            source.providerName(),
            BARGE,
            purchase.materialType(),
            source.quotedAmount(),
            BigDecimal.ZERO,
            purchase.currency(),
            status,
            userId
        ));
    }

    private Map<String, Object> trafficService(String json) {
        if (text(json) == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "TRAFFIC_SERVICE_DATA_INVALID");
        }
    }

    private boolean isLandSupply(Map<String, Object> trafficService) {
        return "LAND".equalsIgnoreCase(String.valueOf(trafficService.get("supplyMode")))
            || "SUPPLIER".equalsIgnoreCase(String.valueOf(trafficService.get("fixedProviderType")));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String sourceType(String value) {
        String normalized = text(value) == null ? null : value.trim().toUpperCase();
        if (!SUPPLIER.equals(normalized) && !BARGE.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SETTLEMENT_SOURCE_TYPE");
        }
        return normalized;
    }

    private String normalizedStatus(String value) {
        String normalized = text(value) == null ? null : value.trim().toUpperCase();
        if (normalized != null && !PENDING_INVOICE.equals(normalized)
            && !PENDING_SETTLEMENT.equals(normalized) && !SETTLED.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SETTLEMENT_STATUS");
        }
        return normalized;
    }

    private List<FileSnapshot> validAttachments(List<FileSnapshot> attachments) {
        if (attachments == null) return List.of();
        return attachments.stream().map(file -> {
            if (file == null || text(file.fileName()) == null || (text(file.fileId()) == null && text(file.fileUrl()) == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_INVOICE_ATTACHMENT");
            }
            String fileId = text(file.fileId());
            String fileUrl = text(file.fileUrl());
            if (fileUrl == null) fileUrl = "/api/files/" + fileId;
            return new FileSnapshot(fileId, text(file.fileName()), fileUrl);
        }).toList();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ATTACHMENT_DATA_INVALID");
        }
    }

    private BigDecimal validAmount(BigDecimal value, String error) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return value;
    }

    private int safePage(int page) {
        return Math.max(page, 1);
    }

    private int safeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private String text(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()) ? null : value.trim();
    }

    private String firstText(String first, String fallback) {
        String value = text(first);
        return value == null ? fallback : value;
    }
}
