package com.zswy.shipsupply.system.dictionary;

public record DictionaryTypeResponse(
    Long id,
    String typeCode,
    String typeName,
    String description,
    Integer sortOrder,
    Boolean enabled
) {
}
