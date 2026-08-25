package com.zswy.shipsupply.shop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IntelligentImportV2ContractTest {

    @Test
    void modelValuesHaveRealPersistencePriorityAndRowsAreConserved() throws Exception {
        String service = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "IntelligentImportService.java"));
        String analyzer = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "ShopImportModelAnalyzer.java"));
        String repository = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "IntelligentImportRepository.java"));

        assertTrue(service.contains("validModelText(model) ? model.productName()"));
        assertTrue(service.contains("validModelText(model) ? model.specification()"));
        assertTrue(service.contains("previewRowCount(batchId) != totalRows"));
        assertTrue(analyzer.contains("retryMissingRows"));
        assertTrue(repository.contains("previewRowCount"));
        assertTrue(repository.contains("ON DUPLICATE KEY UPDATE"));
    }
}
