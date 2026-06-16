package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.List;

public record MaterialSupplierCandidate(
    Long skuId,
    Long companyId,
    String supplierName,
    String supplierSkuCode,
    String productName,
    String impaCode,
    String platformCode,
    String categoryCode,
    String categoryName,
    List<MaterialSupplierSkuAttribute> specifications,
    String attributeSummary,
    BigDecimal unitPrice,
    String currency,
    String currencySymbol,
    BigDecimal stockQty,
    String stockUnit,
    String packageSpec,
    String imageUrl,
    String thumbnailUrl,
    String shelfStatus,
    String codeStatus,
    String matchType,
    String reason
) {
    MaterialSupplierCandidate withMatch(String matchType, String reason) {
        return new MaterialSupplierCandidate(
            skuId,
            companyId,
            supplierName,
            supplierSkuCode,
            productName,
            impaCode,
            platformCode,
            categoryCode,
            categoryName,
            specifications,
            attributeSummary,
            unitPrice,
            currency,
            currencySymbol,
            stockQty,
            stockUnit,
            packageSpec,
            imageUrl,
            thumbnailUrl,
            shelfStatus,
            codeStatus,
            matchType,
            reason
        );
    }
}
