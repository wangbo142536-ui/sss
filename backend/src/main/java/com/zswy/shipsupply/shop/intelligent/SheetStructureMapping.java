package com.zswy.shipsupply.shop.intelligent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

record SheetStructureMapping(
    String sheetName,
    int headerRowNumber,
    int dataStartRowNumber,
    Map<String, String> columns,
    double confidence
) {
    private static final List<String> ALLOWED_FIELDS = List.of(
        "productName", "standardCode", "supplierSkuCode", "specification", "unit",
        "price", "stock", "packing", "barcode"
    );

    static SheetStructureMapping validated(
        String sheetName,
        Integer headerRowNumber,
        Integer dataStartRowNumber,
        Map<String, String> columns,
        List<String> availableColumns,
        Double confidence
    ) {
        int header = headerRowNumber == null ? 0 : headerRowNumber;
        int dataStart = dataStartRowNumber == null ? header + 1 : dataStartRowNumber;
        if (header <= 0 || dataStart <= header) throw new IllegalArgumentException("MODEL_STRUCTURE_ROW_RANGE_INVALID");
        Map<String, String> safe = new LinkedHashMap<>();
        if (columns != null) {
            for (String field : ALLOWED_FIELDS) {
                String value = columns.get(field);
                if (value == null || value.isBlank()) continue;
                String column = value.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
                if (availableColumns.contains(column)) safe.put(field, column);
            }
        }
        if (!safe.containsKey("productName")) throw new IllegalArgumentException("MODEL_STRUCTURE_PRODUCT_NAME_REQUIRED");
        double safeConfidence = confidence == null ? 0 : Math.max(0, Math.min(1, confidence));
        if (safeConfidence < 0.70) throw new IllegalArgumentException("MODEL_STRUCTURE_CONFIDENCE_LOW");
        return new SheetStructureMapping(sheetName, header, dataStart, Map.copyOf(safe), safeConfidence);
    }
}

class ModelConfigurationRequiredException extends IllegalStateException {
    ModelConfigurationRequiredException() {
        super("MODEL_CONFIGURATION_REQUIRED");
    }
}
