package com.zswy.shipsupply.standardlibrary.category;

import java.util.List;

public record ImpaCategoryResponse(
    String code,
    String nameCn,
    String nameEn,
    String parentCode,
    int level,
    int sortOrder,
    int itemCount,
    List<ImpaCategoryResponse> children
) {
}
