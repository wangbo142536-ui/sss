package com.zswy.shipsupply.shop;

import java.util.List;

public record ShopIntelligentImportAnalysis(
    String cleanName,
    String specification,
    String recommendedImpaCode,
    String categoryCode,
    String categoryName,
    String confidenceLevel,
    boolean reviewRequired,
    List<String> candidateCodes,
    List<String> candidateCategoryCodes
) {
}
