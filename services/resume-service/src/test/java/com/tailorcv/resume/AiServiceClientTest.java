package com.tailorcv.resume;

import com.sun.net.httpserver.HttpServer;
import com.tailorcv.resume.client.AiServiceClient;
import com.tailorcv.resume.client.AiServiceException;
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

    @Test void downstreamFailureIsUnavailable() throws Exception {
        var client = start(503, "down", 0, 500);
        assertEquals(503, assertThrows(AiServiceException.class, client::checkHealth).status());
    }

    @Test void timeoutIsGatewayTimeout() throws Exception {
        var client = start(200, "{\"status\":\"UP\"}", 300, 50);
        assertEquals(504, assertThrows(AiServiceException.class, client::checkHealth).status());
    }
}
