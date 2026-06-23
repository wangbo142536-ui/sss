package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;

public record MaterialSupplierUnitPriceOption(
    String unit,
    BigDecimal unitPrice,
    BigDecimal unitPriceUsd,
    boolean defaultSelected
) {
}
