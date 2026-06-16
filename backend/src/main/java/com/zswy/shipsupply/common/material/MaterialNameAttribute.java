package com.zswy.shipsupply.common.material;

public record MaterialNameAttribute(
    String key,
    String name,
    String value,
    String unit,
    String rawText
) {
}
