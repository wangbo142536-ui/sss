package com.zswy.shipsupply.procurement.materials;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zswy.shipsupply.auth.TokenService;

@RestController
@RequestMapping("/api/procurement/materials/ai-capabilities")
public class MaterialAiCapabilityController {

    private final TokenService tokenService;
    private final MaterialCategoryModelAnalyzer categoryAnalyzer;
    private final MaterialComparisonModelReranker comparisonReranker;

    public MaterialAiCapabilityController(
        TokenService tokenService,
        MaterialCategoryModelAnalyzer categoryAnalyzer,
        MaterialComparisonModelReranker comparisonReranker
    ) {
        this.tokenService = tokenService;
        this.categoryAnalyzer = categoryAnalyzer;
        this.comparisonReranker = comparisonReranker;
    }

    @GetMapping
    public MaterialAiCapabilities capabilities(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        tokenService.requireUserId(authorizationHeader);
        boolean categoryConfigured = categoryAnalyzer.configured();
        boolean comparisonConfigured = comparisonReranker.configured();
        return new MaterialAiCapabilities(
            categoryConfigured,
            comparisonConfigured,
            categoryConfigured || comparisonConfigured ? "CONFIGURED" : "MODEL_CONFIGURATION_REQUIRED"
        );
    }
}

record MaterialAiCapabilities(
    boolean categoryAnalysisConfigured,
    boolean comparisonRerankConfigured,
    String status
) {
}
