package com.zswy.shipsupply.system.dictionary;

public record DictionaryTypeSaveRequest(
    String typeCode,
    String typeName,
    String description,
    Integer sortOrder,
    Boolean enabled
) {
}
