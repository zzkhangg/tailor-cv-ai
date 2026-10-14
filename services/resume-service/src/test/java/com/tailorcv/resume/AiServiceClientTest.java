package com.tailorcv.resume;

import com.sun.net.httpserver.HttpServer;
import com.tailorcv.resume.client.AiServiceClient;
import com.tailorcv.resume.client.AiServiceException;
import com.tailorcv.resume.dto.CanonicalResume;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceClientTest {
    private HttpServer server;

    private AiServiceClient start(int code, String body, long delayMs, int timeoutMs) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            try {
                if (delayMs > 0) Thread.sleep(delayMs);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        server.start();
        return new AiServiceClient("http://127.0.0.1:" + server.getAddress().getPort(), 500, timeoutMs);
    }

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void healthyResponse() throws Exception {
        assertDoesNotThrow(() -> start(200, "{\"status\":\"UP\"}", 0, 500).checkHealth());
    }

    @Test void invalidResponseIsBadGateway() throws Exception {
        var client = start(200, "{\"status\":\"UNKNOWN\"}", 0, 500);
        assertEquals(502, assertThrows(AiServiceException.class, client::checkHealth).status());
    }

    @Test void malformedJsonIsBadGateway() throws Exception {
        var client = start(200, "{invalid", 0, 500);
        assertEquals(502, assertThrows(AiServiceException.class, client::checkHealth).status());
    }

    @Test void downstreamFailureIsUnavailable() throws Exception {
        var client = start(503, "down", 0, 500);
        assertEquals(503, assertThrows(AiServiceException.class, client::checkHealth).status());
    }

    @Test void timeoutIsGatewayTimeout() throws Exception {
        var client = start(200, "{\"status\":\"UP\"}", 300, 50);
        assertEquals(504, assertThrows(AiServiceException.class, client::checkHealth).status());
    }

    @Test void parsesCanonicalSnakeCaseContract() throws Exception {
        var client = start(200, "{\"status\":\"UP\"}", 0, 500);
        server.createContext("/internal/parse", exchange -> {
            byte[] bytes = ("{\"name\":\"Jane Doe\",\"contact\":{\"email\":\"jane@example.com\","
                    + "\"phone\":\"\",\"location\":\"\"},\"summary\":\"\",\"skills\":[],"
                    + "\"experience\":[{\"company\":\"Example Co\",\"title\":\"Intern\","
                    + "\"start_date\":\"2025-01\",\"end_date\":\"2025-03\",\"bullets\":[]}],"
                    + "\"projects\":[],\"education\":[]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        assertEquals("2025-01", client.parse("Jane Doe").experience().getFirst().startDate());
    }

    @Test void tailorsUsingSnakeCaseContract() throws Exception {
        var client = start(200, "{\"status\":\"UP\"}", 0, 500);
        server.createContext("/internal/tailor", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("job_description"));
            byte[] bytes = ("{\"tailored_resume\":{\"name\":\"Jane Doe\",\"contact\":{\"email\":\"\","
                    + "\"phone\":\"\",\"location\":\"\"},\"summary\":\"\",\"skills\":[\"Python\"],"
                    + "\"experience\":[],\"projects\":[],\"education\":[]},"
                    + "\"change_summary\":[\"Reordered skills.\"],\"missing_requirements\":[]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        var source = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("", "", ""),
                "", java.util.List.of("Python"), java.util.List.of(), java.util.List.of(), java.util.List.of());
        assertEquals("Python", client.tailor(source, "Build Python services for a small team.")
                .tailoredResume().skills().getFirst());
    }

    @Test void tailorMapsUnsupportedClaimsTo422() throws Exception {
        var client = start(200, "{\"status\":\"UP\"}", 0, 500);
        server.createContext("/internal/tailor", exchange -> {
            byte[] bytes = "{\"detail\":\"unsafe\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(422, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        var source = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("", "", ""),
                "", java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
        assertEquals(422, assertThrows(AiServiceException.class,
                () -> client.tailor(source, "Build reliable services for a small team.")).status());
    }

    @Test void tailorMapsTimeoutTo504() throws Exception {
        start(200, "{\"status\":\"UP\"}", 0, 500);
        server.createContext("/internal/tailor", exchange -> {
            try {
                Thread.sleep(300);
                byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        var client = new AiServiceClient("http://127.0.0.1:" + server.getAddress().getPort(), 500, 500, 50);
        var source = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("", "", ""),
                "", java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
        assertEquals(504, assertThrows(AiServiceException.class,
                () -> client.tailor(source, "Build reliable services for a small team.")).status());
    }
}
