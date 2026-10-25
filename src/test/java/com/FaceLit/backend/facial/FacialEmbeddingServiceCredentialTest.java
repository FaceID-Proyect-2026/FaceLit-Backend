package com.FaceLit.backend.facial;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class FacialEmbeddingServiceCredentialTest {
    record Request(String path, String key, String body) {}

    @Test void onlyServerClientAddsInternalCredential() throws Exception {
        var requests = new LinkedBlockingQueue<Request>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/facial-embeddings", exchange -> {
            requests.add(new Request(exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("X-API-Key"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] body = "{\"live\":true,\"reason\":\"LIVE\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var client = new FacialEmbeddingVerificationClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-server-credential");
            client.registerFromImage(Map.of("image_base64", "image", "user_id", "owner"));
            Request registration = requests.poll(5, TimeUnit.SECONDS);
            assertNotNull(registration);
            assertEquals("/api/v1/facial-embeddings/from-image", registration.path());
            assertEquals("test-server-credential", registration.key());
            assertTrue(registration.body().contains("owner"));
            assertFalse(registration.body().contains("test-server-credential"));
            assertTrue(client.checkLiveness(Map.of("image_frames", java.util.List.of("frame"))).get("live").asBoolean());
            Request liveness = requests.poll(5, TimeUnit.SECONDS);
            assertNotNull(liveness);
            assertEquals("/api/v1/facial-embeddings/liveness-check", liveness.path());
            assertEquals("test-server-credential", liveness.key());
        } finally {
            server.stop(0);
        }
    }
}
