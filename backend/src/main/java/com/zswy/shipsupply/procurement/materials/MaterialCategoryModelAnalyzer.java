package com.zswy.shipsupply.procurement.materials;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.common.ai.OpenAiCompatibleJsonClient;

@Component
class MaterialCategoryModelAnalyzer {

    private static final int CHUNK_SIZE = 8;
    private static final int MAX_INPUT_CHARS = 24_000;
    private static final int MAX_RESPONSE_CHARS = 128_000;
    private static final Set<String> ALLOWED_CATEGORIES = MaterialImpaCategories.codes();

    private final ObjectMapper objectMapper;
    private final OpenAiCompatibleJsonClient client;
    private final boolean enabled;
    private final String model;

    MaterialCategoryModelAnalyzer(
        ObjectMapper objectMapper,
        @Value("${ship-supply.shop.intelligent-import.model.enabled:false}") boolean enabled,
        @Value("${ship-supply.shop.intelligent-import.model.base-url:https://api.openai.com/v1}") String baseUrl,
        @Value("${ship-supply.shop.intelligent-import.model.api-key:}") String apiKey,
        @Value("${ship-supply.shop.intelligent-import.model.name:gpt-5-mini}") String model
    ) {
        this.objectMapper = objectMapper;
        this.client = new OpenAiCompatibleJsonClient(objectMapper, baseUrl, apiKey);
        this.enabled = enabled;
        this.model = model;
    }

    boolean configured() {
        return client.configured(enabled);
    }

    Map<Integer, CategoryDecision> analyze(List<MaterialMatchPreviewItem> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        if (!configured()) {
            throw new IllegalStateException("MODEL_CONFIGURATION_REQUIRED");
        }
        Map<Integer, CategoryDecision> decisions = new LinkedHashMap<>();
        for (int start = 0; start < rows.size(); start += CHUNK_SIZE) {
            List<MaterialMatchPreviewItem> chunk = rows.subList(start, Math.min(rows.size(), start + CHUNK_SIZE));
            try {
                request(chunk).forEach(decision -> {
                    if (decision != null && chunk.stream().anyMatch(row -> row.sourceRowNumber() == decision.sourceRowNumber())) {
                        CategoryDecision valid = validate(decision);
                        if (valid != null) {
                            decisions.put(valid.sourceRowNumber(), valid);
                        }
                    }
                });
            } catch (Exception ignored) {
                // Row conservation is handled by the caller: absent decisions remain pending review.
            }
        }
        return Map.copyOf(decisions);
    }

    private List<CategoryDecision> request(List<MaterialMatchPreviewItem> rows) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (MaterialMatchPreviewItem row : rows) {
            items.add(Map.of(
                "sourceRowNumber", row.sourceRowNumber(),
                "name", bounded(row.cleanName(), 500),
                "sourceName", bounded(first(row.description(), row.rawNameSpec()), 900),
                "specification", bounded(first(row.sizeModel(), row.candidateSpec()), 900),
                "unit", bounded(row.unit(), 80),
                "packing", bounded(row.packing(), 300),
                "allowedCategories", MaterialImpaCategories.namesByCode()
            ));
        }
        String system = """
            Choose one official IMPA top-level category for each material row. Return JSON object {"items": [...]}.
            Each item must contain sourceRowNumber, categoryCode and confidence. categoryCode must be copied from
            allowedCategories. Each allowedCategories entry maps code to its official Chinese category name. Consider
            Chinese/English name, specification, model, material, unit and packing.
            Do not return or invent a six-digit IMPA item code. Return confidence from 0 to 1.
            """;
        String user = objectMapper.writeValueAsString(Map.of("items", items));
        if (user.length() > MAX_INPUT_CHARS) {
            throw new IllegalArgumentException("MODEL_INPUT_TOO_LARGE");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("max_tokens", 2_000);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
        JsonNode root = objectMapper.readTree(client.completeJson(payload, MAX_RESPONSE_CHARS)).path("items");
        List<CategoryDecision> result = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode item : root) {
                result.add(new CategoryDecision(
                    item.path("sourceRowNumber").asInt(-1),
                    item.path("categoryCode").asText(""),
                    item.path("confidence").asDouble(0)
                ));
            }
        }
        return result;
    }

    private CategoryDecision validate(CategoryDecision decision) {
        String category = decision.categoryCode() == null ? "" : decision.categoryCode().replaceAll("[^0-9]", "");
        if (!ALLOWED_CATEGORIES.contains(category) || decision.confidence() < 0.80 || decision.sourceRowNumber() < 1) {
            return null;
        }
        return new CategoryDecision(decision.sourceRowNumber(), category, Math.min(1, decision.confidence()));
    }

    private String bounded(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    record CategoryDecision(int sourceRowNumber, String categoryCode, double confidence) {
    }
}
