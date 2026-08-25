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
    String reason,
    String selectedUnit,
    BigDecimal unitPriceUsd,
    BigDecimal lineAmount,
    BigDecimal lineAmountUsd,
    List<MaterialSupplierUnitPriceOption> unitPriceOptions,
    List<String> productTags,
    int qualityScore,
    int priceScore
) {
    public MaterialSupplierCandidate(
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
        String reason,
        String selectedUnit,
        BigDecimal unitPriceUsd,
        BigDecimal lineAmount,
        BigDecimal lineAmountUsd,
        List<MaterialSupplierUnitPriceOption> unitPriceOptions
    ) {
        this(
            skuId, companyId, supplierName, supplierSkuCode, productName, impaCode, platformCode,
            categoryCode, categoryName, specifications, attributeSummary, unitPrice, currency,
            currencySymbol, stockQty, stockUnit, packageSpec, imageUrl, thumbnailUrl, shelfStatus,
            codeStatus, matchType, reason, selectedUnit, unitPriceUsd, lineAmount, lineAmountUsd,
            unitPriceOptions, List.of(), 0, 0
        );
    }

    public MaterialSupplierCandidate(
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
        this(
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
            reason,
            stockUnit,
            null,
            null,
            null,
            List.of(),
            List.of(),
            0,
            0
        );
    }

    MaterialSupplierCandidate withProductTags(List<String> tags) {
        List<String> safeTags = tags == null ? List.of() : List.copyOf(tags);
        return new MaterialSupplierCandidate(
            skuId, companyId, supplierName, supplierSkuCode, productName, impaCode, platformCode,
            categoryCode, categoryName, specifications, attributeSummary, unitPrice, currency,
            currencySymbol, stockQty, stockUnit, packageSpec, imageUrl, thumbnailUrl, shelfStatus,
            codeStatus, matchType, reason, selectedUnit, unitPriceUsd, lineAmount, lineAmountUsd,
            unitPriceOptions, safeTags, safeTags.contains("质量高") ? 5 : 0,
            safeTags.contains("价格低") ? 5 : 0
        );
    }

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
            reason,
            selectedUnit,
            unitPriceUsd,
            lineAmount,
            lineAmountUsd,
            unitPriceOptions,
            productTags,
            qualityScore,
            priceScore
        );
    }

    MaterialSupplierCandidate withUnitPriceOptions(List<MaterialSupplierUnitPriceOption> options) {
        List<MaterialSupplierUnitPriceOption> safeOptions = options == null ? List.of() : options;
        MaterialSupplierUnitPriceOption selected = safeOptions.stream()
            .filter(MaterialSupplierUnitPriceOption::defaultSelected)
            .findFirst()
            .orElse(safeOptions.isEmpty() ? null : safeOptions.get(0));
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
            selected == null ? unitPrice : selected.unitPrice(),
            "CNY",
            "\u00A5",
            stockQty,
            selected == null ? stockUnit : selected.unit(),
            packageSpec,
            imageUrl,
            thumbnailUrl,
            shelfStatus,
            codeStatus,
            matchType,
            reason,
            selected == null ? stockUnit : selected.unit(),
            selected == null ? unitPriceUsd : selected.unitPriceUsd(),
            lineAmount,
            lineAmountUsd,
            safeOptions,
            productTags,
            qualityScore,
            priceScore
        );
    }

    MaterialSupplierCandidate withLineAmounts(BigDecimal pricingQuantity) {
        BigDecimal safeQuantity = pricingQuantity == null ? BigDecimal.ONE : pricingQuantity;
        BigDecimal cnyAmount = unitPrice == null ? null : unitPrice.multiply(safeQuantity);
        BigDecimal usdAmount = unitPriceUsd == null ? null : unitPriceUsd.multiply(safeQuantity);
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
            reason,
            selectedUnit,
            unitPriceUsd,
            cnyAmount,
            usdAmount,
            unitPriceOptions,
            productTags,
            qualityScore,
            priceScore
        );
    }
}
