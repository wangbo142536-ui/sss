package com.zswy.shipsupply.system.dictionary;

public record DictionaryItemResponse(
    Long id,
    String typeCode,
    String itemCode,
    String itemName,
    String itemValue,
    String itemNameEn,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean builtIn
) {
}
