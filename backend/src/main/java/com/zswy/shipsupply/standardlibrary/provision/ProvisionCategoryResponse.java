package com.zswy.shipsupply.standardlibrary.provision;

public record ProvisionCategoryResponse(
    String categoryCode,
    String nameCn,
    String nameEn,
    String parentCode,
    int level,
    int sortOrder
) {
}
