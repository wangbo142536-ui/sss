package com.zswy.shipsupply.standardlibrary.item;

public record ImpaItemResponse(
    String impaCode,
    String categoryCode,
    String categoryName,
    String segmentCode,
    String nameCn,
    String nameEn,
    String specification,
    String unit
) {
}
