package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.auth.TokenService;

class MaterialAiCapabilityControllerTest {

    @Test
    void requiresAuthenticationAndReportsMissingModelConfiguration() {
        TokenService tokenService = mock(TokenService.class);
        MaterialCategoryModelAnalyzer categoryAnalyzer = new MaterialCategoryModelAnalyzer(
            new ObjectMapper(), true, "https://api.openai.com/v1", "", "gpt-5-mini"
        );
        MaterialComparisonModelReranker comparisonReranker = new MaterialComparisonModelReranker(
            new ObjectMapper(), true, "https://api.openai.com/v1", "", "gpt-5-mini"
        );
        MaterialAiCapabilityController controller = new MaterialAiCapabilityController(
            tokenService, categoryAnalyzer, comparisonReranker
        );

        MaterialAiCapabilities response = controller.capabilities("Bearer token");

        verify(tokenService).requireUserId("Bearer token");
        assertThat(response.status()).isEqualTo("MODEL_CONFIGURATION_REQUIRED");
        assertThat(response.categoryAnalysisConfigured()).isFalse();
        assertThat(response.comparisonRerankConfigured()).isFalse();
    }
}
