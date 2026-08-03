package com.zswy.shipsupply.procurement.food.domain;

import java.util.List;

public record FoodSpreadsheetDocument(
    String fileName,
    String sheetName,
    int headerRow,
    List<FoodSheetOption> sheets,
    List<FoodSpreadsheetRow> rows
) {
    public record FoodSheetOption(String name, int headerRow, int validRows) {
    }
}

