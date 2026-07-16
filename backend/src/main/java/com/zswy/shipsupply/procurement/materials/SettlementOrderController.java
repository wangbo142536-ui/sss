package com.zswy.shipsupply.procurement.materials;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettlementOrderController {

    private final SettlementOrderService settlementOrderService;

    public SettlementOrderController(SettlementOrderService settlementOrderService) {
        this.settlementOrderService = settlementOrderService;
    }

    @GetMapping("/api/settlements")
    public SettlementOrderListResponse list(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "scope", defaultValue = "BUYER") String scope,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return settlementOrderService.list(authorizationHeader, scope, keyword, status, page, size);
    }

    @PostMapping("/api/settlements/batch")
    public SettlementBatchResponse settleBatch(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody SettlementBatchRequest request
    ) {
        return settlementOrderService.settleBatch(authorizationHeader, request);
    }

    @PostMapping("/api/settlements/batch-draft")
    public SettlementBatchResponse createBatchDraft(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody SettlementBatchRequest request
    ) {
        return settlementOrderService.createBatch(authorizationHeader, request);
    }

    @PutMapping("/api/settlements/{settlementId}")
    public SettlementOrderResponse updateActualAmount(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long settlementId,
        @RequestBody SettlementAmountUpdateRequest request
    ) {
        return settlementOrderService.updateActualAmount(authorizationHeader, settlementId, request);
    }

    @PostMapping("/api/settlements/{settlementId}/invoice")
    public SettlementOrderResponse submitInvoice(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long settlementId,
        @RequestBody SettlementInvoiceSubmitRequest request
    ) {
        return settlementOrderService.submitInvoice(authorizationHeader, settlementId, request);
    }

    @PostMapping("/api/settlements/{settlementId}/settle")
    public SettlementOrderResponse settle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long settlementId
    ) {
        return settlementOrderService.settle(authorizationHeader, settlementId);
    }

    @PostMapping("/api/settlements/{settlementId}/pay")
    public SettlementOrderResponse pay(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long settlementId
    ) {
        return settlementOrderService.pay(authorizationHeader, settlementId);
    }

    @DeleteMapping("/api/settlements/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long settlementId
    ) {
        settlementOrderService.delete(authorizationHeader, settlementId);
    }
}
