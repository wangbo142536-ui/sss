package com.zswy.shipsupply.shop.intelligent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser.GenericProductRow;
import org.junit.jupiter.api.Test;

class ShopImportModelAnalyzerV2Test {

    @Test
    void nonOfficialImportFailsExplicitlyWhenModelIsNotConfigured() {
        ShopImportModelAnalyzer analyzer = new ShopImportModelAnalyzer(
            new ObjectMapper(), false, "https://api.openai.com/v1", "", "gpt-5-mini"
        );

        assertThatThrownBy(analyzer::requireConfiguredForNonOfficial)
            .isInstanceOf(ModelConfigurationRequiredException.class)
            .hasMessage("MODEL_CONFIGURATION_REQUIRED");
    }

    @Test
    void validatesSheetMappingBeforeDeterministicFullParsing() {
        SheetStructureMapping mapping = SheetStructureMapping.validated(
            "报价", 3, 4,
            Map.of("productName", "C", "standardCode", "E", "specification", "F"),
            List.of("A", "B", "C", "D", "E", "F"),
            0.91
        );

        assertThat(mapping.headerRowNumber()).isEqualTo(3);
        assertThat(mapping.dataStartRowNumber()).isEqualTo(4);
        assertThat(mapping.columns()).containsEntry("productName", "C");
        assertThat(mapping.confidence()).isEqualTo(0.91);
    }

    @Test
    void retriesOnlyMissingModelRowsAndReturnsAConservedResult() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            int call = calls.incrementAndGet();
            String sourceItemId = call == 1 ? "S!R2" : "S!R3";
            String modelJson = "{\"items\":[{\"sourceItemId\":\"" + sourceItemId
                + "\",\"productType\":\"MATERIAL\",\"materialKind\":\"STORE\","
                + "\"productName\":\"MODEL " + sourceItemId + "\",\"specification\":\"MODEL SPEC\","
                + "\"confidence\":0.95}]}";
            String body = new ObjectMapper().writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", modelJson)))
            ));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            ShopImportModelAnalyzer analyzer = new ShopImportModelAnalyzer(
                new ObjectMapper(), true, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model"
            );
            List<GenericProductRow> rows = List.of(row("S!R2", 2), row("S!R3", 3));
            Map<String, ModelAnalysis> result = analyzer.analyze(rows, Map.of());

            assertThat(result).containsKeys("S!R2", "S!R3");
            assertThat(calls).hasValue(2);
        } finally {
            server.stop(0);
        }
    }

    private GenericProductRow row(String sourceItemId, int rowNo) {
        return new GenericProductRow(
            sourceItemId, "S", rowNo, "source", "", "SKU-" + rowNo, "source spec",
            "PCS", "1", "10", "1/box", "", null, null, Map.of("name", "source")
        );
    }
}
