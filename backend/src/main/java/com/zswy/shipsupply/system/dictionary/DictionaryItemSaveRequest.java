package com.zswy.shipsupply.system.dictionary;

public record DictionaryItemSaveRequest(
    String typeCode,
    String itemCode,
    String itemName,
    String itemValue,
    String itemNameEn,
    String description,
    Integer sortOrder,
    Boolean enabled
) {
}
