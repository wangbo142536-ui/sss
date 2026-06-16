package com.zswy.shipsupply.procurement.materials;

public record MaterialMatchCandidate(
    String impaCode,
    String categoryCode,
    String categoryName,
    String nameCn,
    String nameEn,
    String specification,
    String unit,
    String candidateMatchType,
    String reason
) {
}
