package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

class MaterialComparisonModelRerankerTest {

    @Test
    void onlyReordersIdsFromTheDeterministicCandidateSet() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String modelJson = "{\"orderedSkuIds\":[2,999,1]}";
            String body = new ObjectMapper().writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", modelJson)))
            ));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            MaterialComparisonModelReranker reranker = new MaterialComparisonModelReranker(
                new ObjectMapper(), true, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model"
            );

            List<MaterialSupplierCandidate> result = reranker.rerank(item(), List.of(candidate(1L), candidate(2L)));

            assertThat(result).extracting(MaterialSupplierCandidate::skuId).containsExactly(2L, 1L);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsConfigurationRequiredInsteadOfPretendingAiWasApplied() {
        MaterialComparisonModelReranker reranker = new MaterialComparisonModelReranker(
            new ObjectMapper(), true, "https://api.openai.com/v1", "", "test-model"
        );

        MaterialComparisonModelReranker.RerankResult result = reranker.rerankWithStatus(
            item(), List.of(candidate(1L), candidate(2L))
        );

        assertThat(result.status()).isEqualTo("MODEL_CONFIGURATION_REQUIRED");
        assertThat(result.candidates()).extracting(MaterialSupplierCandidate::skuId).containsExactly(1L, 2L);
    }

    private MaterialDemandItemResponse item() {
        return new MaterialDemandItemResponse(
            201L, "DEMAND_INQUIRY", 1, 1, 2, 2, Map.of(), "", "船用荧光笔", "橙色", "1", "PCS", "", "",
            "船用荧光笔", null, "", "", null, null, null, null, null, "SIMILAR", "Similar Match", "NAME_SPEC_MATCH",
            false, null, null, List.of(), List.of()
        );
    }

    private MaterialSupplierCandidate candidate(Long id) {
        return new MaterialSupplierCandidate(
            id, id + 10, "供应商" + id, "SKU-" + id, "荧光笔", null, null, "47", "文具类", List.of(), "橙色",
            new BigDecimal("5"), "CNY", "¥", new BigDecimal("10"), "PCS", "1/盒", null, null,
            "ON_SHELF", "PENDING_EXCEPTION", "NAME_SPEC_MATCH", "NAME_SPEC_MATCH"
        );
    }
}
