package com.zswy.shipsupply.common.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transport-only OpenAI-compatible JSON client. Business modules own prompts,
 * candidate validation and conservation rules; this class owns no business data.
 */
public final class OpenAiCompatibleJsonClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public OpenAiCompatibleJsonClient(ObjectMapper objectMapper, String baseUrl, String apiKey) {
        this.objectMapper = objectMapper;
        this.baseUrl = (baseUrl == null ? "" : baseUrl).replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public boolean configured(boolean enabled) {
        return enabled && !baseUrl.isBlank() && !apiKey.isBlank();
    }

    public String completeJson(Map<String, Object> payload, int maxResponseChars) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("MODEL_HTTP_" + response.statusCode());
        }
        if (response.body() == null || response.body().length() > maxResponseChars) {
            throw new IllegalStateException("MODEL_RESPONSE_TOO_LARGE");
        }
        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("MODEL_RESPONSE_CONTENT_EMPTY");
        }
        return content;
    }
}
