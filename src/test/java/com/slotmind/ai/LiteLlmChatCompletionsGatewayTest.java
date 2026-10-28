package com.slotmind.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiteLlmChatCompletionsGatewayTest {
    private HttpServer server;
    private LiteLlmChatCompletionsGateway gateway;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void extractsMessageContentFromChatCompletionsEnvelope() {
        server.createContext("/v1/chat/completions", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Content-Type")).isEqualTo("application/json");
            assertThat(exchange.getRequestHeaders().getFirst("product")).isEqualTo("PC1");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"slotId\\\":\\\"S1\\\"}\"}}]}");
        });
        server.start();
        gateway = gateway();

        assertThat(gateway.complete("{}")) .isEqualTo("{\"slotId\":\"S1\"}");
    }

    @Test
    void rejectsHttpErrorsWithoutExposingResponseDetails() {
        server.createContext("/v1/chat/completions", exchange -> respond(exchange, 503, "gateway unavailable"));
        server.start();
        gateway = gateway();

        assertThatThrownBy(() -> gateway.complete("{}"))
                .isInstanceOf(LlmGatewayException.class)
                .hasMessage("LLM gateway returned HTTP 503");
    }

    private LiteLlmChatCompletionsGateway gateway() {
        var properties = new LlmProperties("http://localhost:" + server.getAddress().getPort() + "/v1/chat/completions",
                "test-model", "PC1", "test-key", Duration.ofSeconds(2));
        return new LiteLlmChatCompletionsGateway(new ObjectMapper(), properties);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}