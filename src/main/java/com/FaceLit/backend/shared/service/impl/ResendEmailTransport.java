package com.FaceLit.backend.shared.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Shared HTTPS transport. No automatic retries or sensitive response logging. */
@Component
public class ResendEmailTransport {
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String from;
    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;

    @Autowired
    public ResendEmailTransport(ObjectMapper mapper,
            @Value("${facelit.email.api-key:}") String apiKey,
            @Value("${facelit.email.from:}") String from,
            @Value("${facelit.email.from-name:FaceLit}") String fromName) {
        this(mapper, apiKey, from, fromName,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER).build(),
                URI.create("https://api.resend.com/emails"), Duration.ofSeconds(15));
    }

    // Injection point for local HTTP tests; production endpoint is fixed HTTPS.
    ResendEmailTransport(ObjectMapper mapper, String apiKey, String from, String fromName,
            HttpClient client, URI endpoint, Duration timeout) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        if (invalidSender(from) || invalidSender(fromName)) {
            throw new IllegalArgumentException("Remitente de correo invalido");
        }
        this.from = from == null || from.isBlank() ? "" :
                fromName == null || fromName.isBlank() ? from : fromName + " <" + from + ">";
        this.client = client;
        this.endpoint = endpoint;
        this.timeout = timeout;
    }

    private static boolean invalidSender(String value) {
        return value != null && (value.contains("\r") || value.contains("\n")
                || value.contains("<") || value.contains(">"));
    }

    public void send(String to, String subject, String text, String html) {
        if (apiKey == null || apiKey.isBlank() || from.isBlank()
                || apiKey.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalStateException("Configura RESEND_API_KEY y FACELIT_EMAIL_FROM para enviar correo");
        }
        try (var logo = new ClassPathResource("static/email/facelit-logo.png").getInputStream()) {
            var payload = Map.of("from", from, "to", List.of(to), "subject", subject,
                    "text", text, "html", html,
                    "attachments", List.of(Map.of("filename", "facelit-logo.png",
                            "content_type", "image/png", "content_id", "facelitLogo",
                            "content", Base64.getEncoder().encodeToString(logo.readAllBytes()))));
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Proveedor de correo rechazo la solicitud (HTTP " + response.statusCode() + ")");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de correo interrumpido");
        } catch (IOException ex) {
            // Callers log messages: do not expose HTTP bodies, credentials or request data.
            throw new IllegalStateException("No fue posible contactar al proveedor de correo HTTPS");
        }
    }
}
