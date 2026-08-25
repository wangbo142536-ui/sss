package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IntelligentImportSafetyContractTest {

    @Test
    void enablesTheModelPathByDefaultButKeepsTheApiKeyEnvironmentOnly() throws Exception {
        String config = Files.readString(Path.of("src", "main", "resources", "application.yml"));

        assertThat(config).contains("enabled: ${SHOP_IMPORT_MODEL_ENABLED:true}");
        assertThat(config).contains("api-key: ${SHOP_IMPORT_MODEL_API_KEY:}");
        assertThat(config).doesNotContain("sk-");
    }

    @Test
    void backendStartupLoadsLocalArkConfigurationIntoProcessEnvironmentWithoutPrintingSecrets() throws Exception {
        String startup = Files.readString(Path.of("..", "start-backend.cmd"));

        assertThat(startup).contains("huoshan.txt");
        assertThat(startup).contains("ARK_BASE_URL").contains("SHOP_IMPORT_MODEL_BASE_URL");
        assertThat(startup).contains("ARK_MODEL").contains("SHOP_IMPORT_MODEL_NAME");
        assertThat(startup).contains("ARK_API_KEY").contains("SHOP_IMPORT_MODEL_API_KEY");
        assertThat(startup).contains("SHOP_IMPORT_MODEL_ENABLED");
        assertThat(startup).doesNotContain("echo %SHOP_IMPORT_MODEL_API_KEY%");
        assertThat(startup).doesNotContain("echo !SHOP_IMPORT_MODEL_API_KEY!");
    }

    @Test
    void intelligentImportUsesDeterministicAnalysisBeforeModelAndNeverUsesFirstTokenLikeMatch() throws Exception {
        String service = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "IntelligentImportService.java"));
        String repository = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "IntelligentImportRepository.java"));

        assertTrue(service.contains("analyzeForIntelligentImport"));
        assertTrue(service.contains("rowsNeedingModel"));
        assertFalse(service.contains("repository.recommendImpa"));
        assertFalse(repository.contains("firstKeyword(String value)"));
    }

    @Test
    void modelFallbackHasExplicitInputOutputLimitsAndCandidateCodeValidation() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "ShopImportModelAnalyzer.java"));

        assertTrue(source.contains("MAX_MODEL_ROWS_PER_REQUEST"));
        assertTrue(source.contains("MAX_MODEL_INPUT_CHARS"));
        assertTrue(source.contains("MAX_MODEL_RESPONSE_CHARS"));
        assertTrue(source.contains("MAX_MODEL_OUTPUT_TOKENS"));
        assertTrue(source.contains("candidateCodes"));
        assertTrue(source.contains("candidateCategoryCodes"));
        assertTrue(source.contains("impaCategoryCode"));
        assertTrue(source.contains("\"max_tokens\""));
    }

    @Test
    void materialCategoryAndExactImpaAreValidatedIndependently() throws Exception {
        String service = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "IntelligentImportService.java"));

        assertTrue(service.contains("validatedModelCategory"));
        assertTrue(service.contains("modelCategoryCode"));
        assertTrue(service.contains("_impaCategoryDecision"));
    }

    @Test
    void executionPersistsParsedRowsThatStillRequireStandardCodeReview() throws Exception {
        String repository = Files.readString(Path.of("src", "main", "java", "com", "zswy", "shipsupply", "shop", "intelligent", "IntelligentImportRepository.java"));

        assertTrue(repository.contains("pending++"));
        assertFalse(repository.contains("pending++;\n                continue;"));
        assertTrue(repository.contains("long skuId = insertSku"));
    }
}
