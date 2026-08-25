package com.zswy.shipsupply.procurement.materials;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.common.ai.OpenAiCompatibleJsonClient;

@Component
class MaterialComparisonModelReranker {

    private static final int MAX_INPUT_CHARS = 24_000;
    private static final int MAX_RESPONSE_CHARS = 64_000;

    private final ObjectMapper objectMapper;
    private final OpenAiCompatibleJsonClient client;
    private final boolean enabled;
    private final String model;

    MaterialComparisonModelReranker(
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

    List<MaterialSupplierCandidate> rerank(
        MaterialDemandItemResponse demand,
        List<MaterialSupplierCandidate> deterministicCandidates
    ) {
        return rerankWithStatus(demand, deterministicCandidates).candidates();
    }

    RerankResult rerankWithStatus(
        MaterialDemandItemResponse demand,
        List<MaterialSupplierCandidate> deterministicCandidates
    ) {
        List<MaterialSupplierCandidate> safeCandidates = deterministicCandidates == null ? List.of() : deterministicCandidates;
        if (safeCandidates.size() < 2 || !needsRerank(safeCandidates)) {
            return new RerankResult(safeCandidates, "DETERMINISTIC");
        }
        if (!configured()) {
            return new RerankResult(safeCandidates, "MODEL_CONFIGURATION_REQUIRED");
        }
        try {
            List<Map<String, Object>> candidates = safeCandidates.stream().map(candidate -> Map.<String, Object>of(
                "skuId", candidate.skuId(),
                "name", value(candidate.productName()),
                "impaCode", value(candidate.impaCode()),
                "categoryCode", value(candidate.categoryCode()),
                "specification", value(candidate.attributeSummary()),
                "unit", value(candidate.stockUnit()),
                "packing", value(candidate.packageSpec()),
                "matchType", value(candidate.matchType()),
                "matchReason", value(candidate.reason())
            )).toList();
            String user = objectMapper.writeValueAsString(Map.of(
                "demand", Map.of(
                    "name", value(first(demand.description(), demand.rawNameSpec())),
                    "specification", value(first(demand.sizeModel(), demand.candidateSpec())),
                    "unit", value(demand.unit()),
                    "packing", value(demand.packing()),
                    "categoryCode", value(preferredCategory(demand))
                ),
                "candidates", candidates
            ));
            if (user.length() > MAX_INPUT_CHARS) return new RerankResult(safeCandidates, "MODEL_FALLBACK");
            String system = """
                Reorder only the supplied marine material SKU candidates by semantic fit. Consider Chinese/English name,
                specification, model, material, unit, packing and category. Never alter price or stock. Return one JSON
                object with orderedSkuIds. Every ID must come from candidates; do not add IDs.
                """;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0);
            payload.put("max_tokens", 1_000);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
            JsonNode idsNode = objectMapper.readTree(client.completeJson(payload, MAX_RESPONSE_CHARS)).path("orderedSkuIds");
            Set<Long> allowed = safeCandidates.stream().map(MaterialSupplierCandidate::skuId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
            if (idsNode.isArray()) {
                idsNode.forEach(node -> {
                    if (node.canConvertToLong() && allowed.contains(node.asLong())) orderedIds.add(node.asLong());
                });
            }
            safeCandidates.stream().map(MaterialSupplierCandidate::skuId).forEach(orderedIds::add);
            Map<Long, MaterialSupplierCandidate> byId = safeCandidates.stream().collect(Collectors.toMap(
                MaterialSupplierCandidate::skuId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new
            ));
            List<MaterialSupplierCandidate> reranked = new ArrayList<>();
            orderedIds.forEach(id -> {
                MaterialSupplierCandidate candidate = byId.get(id);
                if (candidate != null) reranked.add(candidate);
            });
            return reranked.size() == safeCandidates.size()
                ? new RerankResult(List.copyOf(reranked), "MODEL_APPLIED")
                : new RerankResult(safeCandidates, "MODEL_FALLBACK");
        } catch (Exception ignored) {
            return new RerankResult(safeCandidates, "MODEL_FALLBACK");
        }
    }

    private boolean needsRerank(List<MaterialSupplierCandidate> candidates) {
        return candidates.stream().limit(3).anyMatch(candidate ->
            "NAME_SPEC_MATCH".equals(candidate.matchType())
                || "CATEGORY_MATCH".equals(candidate.matchType())
                || "CODE_SPEC_CONFLICT".equals(candidate.matchType())
        );
    }

    private String preferredCategory(MaterialDemandItemResponse item) {
        List<MaterialMatchCandidate> candidates = item.candidateSnapshot() == null ? item.candidates() : item.candidateSnapshot();
        if (candidates == null) return "";
        return candidates.stream().map(MaterialMatchCandidate::categoryCode).filter(value -> value != null && !value.isBlank()).findFirst().orElse("");
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    record RerankResult(List<MaterialSupplierCandidate> candidates, String status) {
    }
}
