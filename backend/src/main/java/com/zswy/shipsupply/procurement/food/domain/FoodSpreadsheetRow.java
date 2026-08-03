package com.zswy.shipsupply.procurement.food.domain;

import java.math.BigDecimal;
import java.util.Map;

public record FoodSpreadsheetRow(
    int sourceRow,
    int sequenceNo,
    String nameEn,
    String nameZh,
    String remark,
    String specification,
    String unit,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal amount,
    String matchStatus,
    String matchReason,
    String matchKey,
    Map<String, String> rawColumns
) {
}

