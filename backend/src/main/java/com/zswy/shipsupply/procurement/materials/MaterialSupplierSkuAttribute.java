package com.zswy.shipsupply.procurement.materials;

public record MaterialSupplierSkuAttribute(
    String key,
    String name,
    String value,
    String unit,
    Integer sortOrder,
    String rawText
) {
}
