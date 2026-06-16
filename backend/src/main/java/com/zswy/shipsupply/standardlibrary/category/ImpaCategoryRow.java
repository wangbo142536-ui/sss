package com.zswy.shipsupply.standardlibrary.category;

record ImpaCategoryRow(
    String categoryCode,
    String categoryNameCn,
    String categoryNameEn,
    String parentCode,
    int level,
    int sortOrder,
    int itemCount
) {
}
