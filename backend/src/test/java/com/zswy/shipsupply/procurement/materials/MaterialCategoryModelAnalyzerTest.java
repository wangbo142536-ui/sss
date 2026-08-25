package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

class MaterialCategoryModelAnalyzerTest {

    @Test
    void acceptsOnlyWhitelistedHighConfidenceCategoryAndKeepsSourceRowIdentity() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String modelJson = "{\"items\":[{\"sourceRowNumber\":7,\"categoryCode\":\"47\",\"confidence\":0.93},"
                + "{\"sourceRowNumber\":8,\"categoryCode\":\"99\",\"confidence\":0.99}]}";
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
            MaterialCategoryModelAnalyzer analyzer = new MaterialCategoryModelAnalyzer(
                new ObjectMapper(), true, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model"
            );
            Map<Integer, MaterialCategoryModelAnalyzer.CategoryDecision> result = analyzer.analyze(List.of(row(7), row(8)));

            assertThat(result).containsOnlyKeys(7);
            assertThat(result.get(7).categoryCode()).isEqualTo("47");
            assertThat(requestBody.get()).contains("allowedCategories", "文具类", "厨房用品");
        } finally {
            server.stop(0);
        }
    }

    private MaterialMatchPreviewItem row(int rowNumber) {
        return new MaterialMatchPreviewItem(
            "DEMAND_INQUIRY", "UNKNOWN", 1, rowNumber, rowNumber, rowNumber, Map.of(),
            "船用荧光笔", "荧光笔", List.of(), List.of(), "", "船用荧光笔", "橙色", "1", "PCS", "",
            "", "船用荧光笔 橙色", null, "1/盒", "", null, null, null, null,
            "UNMATCHED", "Unmatched", "NO_CANDIDATE", "ABNORMAL", "CODE_MISSING", false, null, null, List.of(), List.of()
        );
    }
}
