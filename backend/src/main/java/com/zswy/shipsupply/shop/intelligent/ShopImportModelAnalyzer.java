package com.zswy.shipsupply.shop.intelligent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser.GenericProductRow;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser.GenericSheetSampleRow;
import com.zswy.shipsupply.common.ai.OpenAiCompatibleJsonClient;
import com.zswy.shipsupply.shop.ShopIntelligentImportAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Optional OpenAI-compatible semantic mapper. It never writes business data and
 * always returns results keyed by immutable sourceItemId. When no model is
 * configured, deterministic parsing and standard-library lookup remain active.
 */
@Component
public class ShopImportModelAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ShopImportModelAnalyzer.class);
    static final int MAX_MODEL_ROWS_PER_REQUEST = 8;
    static final int MAX_MODEL_INPUT_CHARS = 24_000;
    static final int MAX_MODEL_RESPONSE_CHARS = 128_000;
    static final int MAX_MODEL_OUTPUT_TOKENS = 4_000;
    private static final int MAX_FIELD_CHARS = 1_200;
    private static final int MAX_RAW_COLUMNS = 10;
    private static final int MAX_RAW_VALUE_CHARS = 240;
    private static final int MAX_STRUCTURE_COLUMNS = 24;
    private static final int MAX_STRUCTURE_ROWS = 8;
    private static final TypeReference<List<ModelAnalysis>> ANALYSIS_LIST = new TypeReference<>() { };

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final OpenAiCompatibleJsonClient aiClient;

    public ShopImportModelAnalyzer(
        ObjectMapper objectMapper,
        @Value("${ship-supply.shop.intelligent-import.model.enabled:false}") boolean enabled,
        @Value("${ship-supply.shop.intelligent-import.model.base-url:https://api.openai.com/v1}") String baseUrl,
        @Value("${ship-supply.shop.intelligent-import.model.api-key:}") String apiKey,
        @Value("${ship-supply.shop.intelligent-import.model.name:gpt-5-mini}") String model
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.aiClient = new OpenAiCompatibleJsonClient(objectMapper, baseUrl, apiKey);
    }

    public Map<String, ModelAnalysis> analyze(
        List<GenericProductRow> rows,
        Map<String, ShopIntelligentImportAnalysis> deterministicAnalyses
    ) {
        if (rows == null || rows.isEmpty()) return Map.of();
        requireConfiguredForNonOfficial();
        Map<String, ModelAnalysis> results = new LinkedHashMap<>();
        for (int start = 0; start < rows.size(); start += MAX_MODEL_ROWS_PER_REQUEST) {
            List<GenericProductRow> chunk = rows.subList(start, Math.min(rows.size(), start + MAX_MODEL_ROWS_PER_REQUEST));
            try {
                for (ModelAnalysis item : request(chunk, deterministicAnalyses)) {
                    if (item != null && item.sourceItemId() != null && chunk.stream().anyMatch(row -> item.sourceItemId().equals(row.sourceItemId()))) {
                        results.put(item.sourceItemId(), validate(item, deterministicAnalyses.get(item.sourceItemId())));
                    }
                }
            } catch (Exception error) {
                log.warn("SHOP_IMPORT_MODEL_CHUNK_FAILED start={} size={}", start, chunk.size(), error);
            }
        }
        retryMissingRows(rows, deterministicAnalyses, results);
        List<String> missing = rows.stream().map(GenericProductRow::sourceItemId).filter(id -> !results.containsKey(id)).toList();
        if (!missing.isEmpty()) throw new IllegalStateException("MODEL_RESPONSE_ROWS_MISSING:" + String.join(",", missing));
        return Map.copyOf(results);
    }

    void requireConfiguredForNonOfficial() {
        if (!aiClient.configured(enabled)) throw new ModelConfigurationRequiredException();
    }

    private void retryMissingRows(
        List<GenericProductRow> rows,
        Map<String, ShopIntelligentImportAnalysis> deterministicAnalyses,
        Map<String, ModelAnalysis> results
    ) {
        List<GenericProductRow> missing = rows.stream().filter(row -> !results.containsKey(row.sourceItemId())).toList();
        for (int start = 0; start < missing.size(); start += MAX_MODEL_ROWS_PER_REQUEST) {
            List<GenericProductRow> retry = missing.subList(start, Math.min(missing.size(), start + MAX_MODEL_ROWS_PER_REQUEST));
            try {
                for (ModelAnalysis item : request(retry, deterministicAnalyses)) {
                    if (item != null && item.sourceItemId() != null
                        && retry.stream().anyMatch(row -> item.sourceItemId().equals(row.sourceItemId()))) {
                        results.put(item.sourceItemId(), validate(item, deterministicAnalyses.get(item.sourceItemId())));
                    }
                }
            } catch (Exception error) {
                log.warn("SHOP_IMPORT_MODEL_MISSING_RETRY_FAILED start={} size={}", start, retry.size(), error);
            }
        }
    }

    SheetStructureMapping analyzeStructure(String sheetName, List<GenericSheetSampleRow> samples) {
        requireConfiguredForNonOfficial();
        if (samples == null || samples.isEmpty()) throw new IllegalArgumentException("MODEL_STRUCTURE_SAMPLE_EMPTY");
        try {
            List<Map<String, Object>> boundedSamples = samples.stream().limit(MAX_STRUCTURE_ROWS).map(row -> Map.<String, Object>of(
                "rowNumber", row.rowNumber(),
                "cells", boundedStructureCells(row.cells())
            )).toList();
            String instruction = """
                Map one spreadsheet sheet to product fields. Return one JSON object only with keys:
                sheetName, headerRowNumber, dataStartRowNumber, columns, confidence.
                columns maps semantic names productName, standardCode, supplierSkuCode, specification,
                unit, price, stock, packing, barcode to Excel column letters. productName is required.
                Do not return a column letter absent from the samples and do not extract business rows.
                """;
            Map<String, Object> payload = payload(instruction, objectMapper.writeValueAsString(Map.of(
                "sheetName", bounded(sheetName, 180), "samples", boundedSamples
            )));
            JsonNode parsed = objectMapper.readTree(send(payload));
            Map<String, String> columns = objectMapper.convertValue(parsed.path("columns"), new TypeReference<Map<String, String>>() { });
            List<String> available = samples.stream().flatMap(row -> row.cells().keySet().stream()).distinct().toList();
            return SheetStructureMapping.validated(
                bounded(parsed.path("sheetName").asText(sheetName), 180),
                parsed.path("headerRowNumber").isInt() ? parsed.path("headerRowNumber").asInt() : null,
                parsed.path("dataStartRowNumber").isInt() ? parsed.path("dataStartRowNumber").asInt() : null,
                columns, available,
                parsed.path("confidence").isNumber() ? parsed.path("confidence").asDouble() : null
            );
        } catch (ModelConfigurationRequiredException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("MODEL_STRUCTURE_ANALYSIS_FAILED", error);
        }
    }

    private List<ModelAnalysis> request(
        List<GenericProductRow> rows,
        Map<String, ShopIntelligentImportAnalysis> deterministicAnalyses
    ) throws Exception {
        List<Map<String, Object>> sourceRows = new ArrayList<>();
        for (GenericProductRow row : rows) {
            ShopIntelligentImportAnalysis deterministic = deterministicAnalyses == null
                ? null
                : deterministicAnalyses.get(row.sourceItemId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sourceItemId", bounded(row.sourceItemId(), 180));
            item.put("sheet", bounded(row.sourceSheet(), 180));
            item.put("row", row.sourceRowNo());
            item.put("name", bounded(first(deterministic == null ? null : deterministic.cleanName(), row.productName()), MAX_FIELD_CHARS));
            item.put("sourceStandardCode", bounded(row.rawStandardCode(), 80));
            item.put("supplierSku", bounded(row.supplierSkuCode(), 180));
            item.put("specification", bounded(first(deterministic == null ? null : deterministic.specification(), row.specification()), MAX_FIELD_CHARS));
            item.put("unit", bounded(row.unit(), 80));
            item.put("candidateCodes", deterministic == null || deterministic.candidateCodes() == null
                ? List.of()
                : deterministic.candidateCodes().stream().limit(12).toList());
            item.put("candidateCategoryCodes", deterministic == null || deterministic.candidateCategoryCodes() == null
                ? List.of()
                : deterministic.candidateCategoryCodes().stream().limit(40).toList());
            item.put("columns", boundedColumns(row.rawColumns()));
            sourceRows.add(item);
        }
        String instruction = """
            Analyze spreadsheet product rows. Return only a JSON object with an items array, one object for every sourceItemId, in the same order.
            Never merge, remove or invent sourceItemId values. Classify productType as MATERIAL or FOOD.
            Official IMPA material categories are: 11 crew welfare/recreation, 15 linen, 17 galley utensils,
            19 clothing, 21 ropes/wires, 23 rigging/deck consumables, 25 marine paints, 27 painting tools,
            31 safety protection, 33 lifesaving/firefighting, 35 couplings, 37 nautical instruments,
            39 medical/sanitary, 45 petroleum products, 47 stationery, 49 hardware, 51 brushes/mats,
            53 lavatory equipment, 55 cleaning/chemicals, 59 pneumatic/electric tools, 61 hand tools,
            63 cutting tools, 65 measuring tools, 67 metal plates/bars, 69 screws/nuts, 71 pipes,
            73 pipe fittings, 75 valves/cocks, 77 bearings, 79 electrical equipment, 81 sealing materials,
            85 welding equipment, 87 machinery/other equipment.
            For MATERIAL, classify materialKind as STORE or SPARE_PART. First choose impaCategoryCode from
            candidateCategoryCodes by considering product name, specification/model, material, unit and packing together.
            impaCategoryCode must be copied from candidateCategoryCodes only. Then choose standardCode from candidateCodes
            only when name plus specification/model/material/unit identify that exact IMPA item; otherwise return an empty
            standardCode. Never infer an exact IMPA code from the category prefix and never invent an IMPA code.
            For FOOD, provisionCategoryCode must be one of PROVISION_FRESH, PROVISION_GRAIN_OIL, PROVISION_SEASONING,
            PROVISION_DRY_FOOD, PROVISION_BEVERAGE, PROVISION_FROZEN, PROVISION_OTHER.
            Extract productName and specification without deleting source meaning. Include confidence from 0 to 1.
            Required keys: sourceItemId, productType, materialKind, productName, specification, standardCode,
            impaCategoryCode, provisionCategoryCode, confidence.
            """;
        String userContent = objectMapper.writeValueAsString(Map.of("items", sourceRows));
        if (userContent.length() > MAX_MODEL_INPUT_CHARS) {
            throw new IllegalArgumentException("MODEL_INPUT_TOO_LARGE");
        }
        Map<String, Object> payload = payload(instruction, userContent);
        JsonNode root = objectMapper.readTree(send(payload));
        JsonNode items = root.isArray() ? root : root.path("items");
        List<ModelAnalysis> converted = objectMapper.convertValue(items, ANALYSIS_LIST);
        if (converted.size() > rows.size()) {
            throw new IllegalStateException("MODEL_RESPONSE_ITEM_COUNT_INVALID");
        }
        return converted;
    }

    private Map<String, Object> payload(String instruction, String userContent) {
        if (userContent.length() > MAX_MODEL_INPUT_CHARS) throw new IllegalArgumentException("MODEL_INPUT_TOO_LARGE");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("max_tokens", MAX_MODEL_OUTPUT_TOKENS);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(Map.of("role", "system", "content", instruction), Map.of("role", "user", "content", userContent)));
        return payload;
    }

    private String send(Map<String, Object> payload) throws Exception {
        return aiClient.completeJson(payload, MAX_MODEL_RESPONSE_CHARS);
    }

    private ModelAnalysis validate(ModelAnalysis item, ShopIntelligentImportAnalysis deterministic) {
        String normalizedCode = normalizeCode(item.standardCode());
        List<String> candidateCodes = deterministic == null || deterministic.candidateCodes() == null
            ? List.of()
            : deterministic.candidateCodes();
        String validatedCode = candidateCodes.stream()
            .filter(candidate -> normalizedCode.equals(normalizeCode(candidate)))
            .findFirst()
            .orElse(null);
        List<String> candidateCategoryCodes = deterministic == null || deterministic.candidateCategoryCodes() == null
            ? List.of()
            : deterministic.candidateCategoryCodes();
        String normalizedCategory = normalizeCategory(item.impaCategoryCode());
        String validatedCategory = candidateCategoryCodes.stream()
            .filter(candidate -> normalizedCategory.equals(normalizeCategory(candidate)))
            .findFirst()
            .orElse(null);
        double confidence = item.confidence() == null ? 0 : Math.max(0, Math.min(1, item.confidence()));
        return new ModelAnalysis(
            item.sourceItemId(),
            bounded(item.productType(), 20),
            bounded(item.materialKind(), 30),
            bounded(item.productName(), 500),
            bounded(item.specification(), 1_000),
            validatedCode,
            validatedCategory,
            bounded(item.provisionCategoryCode(), 80),
            confidence
        );
    }

    private Map<String, String> boundedColumns(Map<String, String> columns) {
        if (columns == null || columns.isEmpty()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        columns.entrySet().stream().limit(MAX_RAW_COLUMNS).forEach(entry ->
            result.put(bounded(entry.getKey(), 120), bounded(entry.getValue(), MAX_RAW_VALUE_CHARS))
        );
        return result;
    }

    private Map<String, String> boundedStructureCells(Map<String, String> cells) {
        if (cells == null || cells.isEmpty()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        cells.entrySet().stream().limit(MAX_STRUCTURE_COLUMNS).forEach(entry ->
            result.put(bounded(entry.getKey(), 8), bounded(entry.getValue(), 120))
        );
        return result;
    }

    private String bounded(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String normalizeCategory(String value) {
        String normalized = normalizeCode(value);
        return normalized.length() >= 2 ? normalized.substring(0, 2) : normalized;
    }
}

record ModelAnalysis(
    String sourceItemId,
    String productType,
    String materialKind,
    String productName,
    String specification,
    String standardCode,
    String impaCategoryCode,
    String provisionCategoryCode,
    Double confidence
) { }
