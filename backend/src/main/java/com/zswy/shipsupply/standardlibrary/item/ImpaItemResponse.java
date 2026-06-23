package com.zswy.shipsupply.standardlibrary.item;

public record ImpaItemResponse(
    String impaCode,
    String cnCode,
    String categoryCode,
    String categoryName,
    String segmentCode,
    String nameCn,
    String nameEn,
    String specification,
    String unit
) {
    public ImpaItemResponse(
        String impaCode,
        String categoryCode,
        String categoryName,
        String segmentCode,
        String nameCn,
        String nameEn,
        String specification,
        String unit
    ) {
        this(impaCode, null, categoryCode, categoryName, segmentCode, nameCn, nameEn, specification, unit);
    }
}
