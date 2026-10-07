package com.FaceLit.backend.facial;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class FacialEmbeddingVerificationClient {

    private final String apiKey;
    private final RestClient client;

    public FacialEmbeddingVerificationClient(
            @Value("${facial.embedding.url:http://localhost:8090}") String baseUrl,
            @Value("${facial.embedding.api-key:change-me}") String apiKey) {
        this.apiKey = apiKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(180));
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public VerificationResponse verifySession(UUID idRecordEnvironment, String imageBase64) {
        return client.post()
                .uri("/api/v1/facial-embeddings/verify-session")
                .header("X-API-Key", apiKey)
                .body(new VerificationRequest(idRecordEnvironment, imageBase64))
                .retrieve()
                .body(VerificationResponse.class);
    }

    record VerificationRequest(
            @JsonProperty("record_environment_id") UUID recordEnvironmentId,
            @JsonProperty("image_base64") String imageBase64) {
    }

    public record VerificationResponse(
            boolean match,
            @JsonProperty("id_apprentice") UUID idApprentice,
            BigDecimal similarity,
            BigDecimal threshold,
            @JsonProperty("model_name") String modelName,
            String reason) {
    }
}
