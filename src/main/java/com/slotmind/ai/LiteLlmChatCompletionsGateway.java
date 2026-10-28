package com.slotmind.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class LiteLlmChatCompletionsGateway implements LlmGateway {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties properties;

    @Autowired
    public LiteLlmChatCompletionsGateway(ObjectMapper objectMapper, LlmProperties properties) {
        this(HttpClient.newBuilder().connectTimeout(properties.timeout()).build(), objectMapper, properties);
    }

    LiteLlmChatCompletionsGateway(HttpClient httpClient, ObjectMapper objectMapper, LlmProperties properties) {
        this.httpClient = httpClient; this.objectMapper = objectMapper; this.properties = properties;
    }

    @Override
    public String complete(String requestJson) {
        if (properties.apiKey() == null || properties.apiKey().isBlank())
            throw new LlmGatewayException("LLM API key is not configured");
        try {
            var request = HttpRequest.newBuilder(URI.create(properties.endpoint()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("product", properties.product())
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new LlmGatewayException("LLM gateway returned HTTP " + response.statusCode());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank())
                throw new LlmGatewayException("LLM response has no message content");
            return content.asText();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmGatewayException("LLM request interrupted", exception);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof LlmGatewayException gatewayException) throw gatewayException;
            throw new LlmGatewayException("LLM request failed", exception);
        }
    }
}