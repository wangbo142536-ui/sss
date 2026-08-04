package com.zswy.shipsupply.customs.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CustomsDeclarationDtos {
    private CustomsDeclarationDtos() {}

    public record DeclareRequest(String businessType, Long purchaseOrderId) {}

    public record DeclarationItem(
        Long id,
        String code,
        String productName,
        String specification,
        String quantity,
        String unit
    ) {}

    public record DeclarationContext(
        String businessType,
        Long purchaseOrderId,
        String purchaseOrderNo,
        String responsibleType,
        Long responsibleCompanyId,
        String responsibleCompanyName,
        boolean canDeclare,
        Long declarationId,
        String status,
        BigDecimal customsFee,
        List<DeclarationItem> items
    ) {}

    public record DeclarationRecord(
        Long id,
        String businessType,
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long buyerCompanyId,
        Long responsibleCompanyId,
        String responsibleType,
        String responsibleCompanyName,
        String status,
        String vesselName,
        String shipAgent,
        String goodsCategory,
        String tradeType,
        String declarantCompany,
        String declarantContact,
        String declarantPhone,
        String deliveryStart,
        String deliveryEnd,
        String deliveryLocation,
        String supplyVessel,
        String captainContact,
        String applicant,
        String applicantPhone,
        LocalDate applicationDate,
        BigDecimal customsFee,
        LocalDateTime declaredAt
    ) {}

    public record DeclarationPage(List<DeclarationRecord> items, long total, int page, int size) {}
}
