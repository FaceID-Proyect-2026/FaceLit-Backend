package com.FaceLit.backend.facial;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Authenticated bridge for the existing client capture flow. */
@RestController
@RequestMapping("/api/facial/embeddings")
public class FacialEmbeddingController {
    private final FacialEmbeddingVerificationClient client;

    public FacialEmbeddingController(FacialEmbeddingVerificationClient client) {
        this.client = client;
    }

    public record Registration(
            @JsonProperty("user_id") @NotNull UUID userId,
            @JsonProperty("image_base64") @NotBlank @Size(max = 5000000) String imageBase64,
            @JsonProperty("image_frames") @Size(max = 18) List<@NotBlank @Size(max = 5000000) String> imageFrames,
            @JsonProperty("liveness_challenge") @Size(max = 40) String livenessChallenge,
            @JsonProperty("liveness_challenges") @Size(max = 3) List<@NotBlank @Size(max = 40) String> livenessChallenges,
            @JsonProperty("photo_reference") @Size(max = 500) String photoReference,
            @JsonProperty("replace_existing") boolean replaceExisting,
            @JsonProperty("reset_token") UUID resetToken) {}

    public record Liveness(
            @JsonProperty("image_frames") @NotNull @Size(min = 3, max = 18) List<@NotBlank @Size(max = 5000000) String> imageFrames,
            @JsonProperty("liveness_challenge") @Size(max = 40) String livenessChallenge) {}

    @PostMapping("/from-image")
    public ResponseEntity<?> register(@Valid @RequestBody Registration request, @AuthenticationPrincipal UUID userId) {
        if (!request.userId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Solo puedes registrar tu propio rostro."));
        }
        // Identity and audit author come from the validated session, never from a client claim.
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("user_id", userId);
        payload.put("created_by", userId.toString());
        payload.put("image_base64", request.imageBase64());
        payload.put("image_frames", request.imageFrames() == null ? List.of() : request.imageFrames());
        payload.put("liveness_challenge", request.livenessChallenge());
        payload.put("liveness_challenges", request.livenessChallenges() == null ? List.of() : request.livenessChallenges());
        payload.put("photo_reference", request.photoReference());
        payload.put("replace_existing", request.replaceExisting());
        payload.put("reset_token", request.resetToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(withoutBiometricVector(client.registerFromImage(payload)));
    }

    @PostMapping("/liveness-check")
    public JsonNode checkLiveness(@Valid @RequestBody Liveness request) {
        return client.checkLiveness(request);
    }

    private JsonNode withoutBiometricVector(JsonNode response) {
        if (response instanceof ObjectNode object) object.remove("embedding");
        return response;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> remoteError(RestClientResponseException error) {
        int status = error.getStatusCode().value();
        // A rejected service credential is an integration failure, not an expired user session.
        if (status == 401 || status == 403 || status >= 500) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "El servicio facial no está disponible."));
        }
        String message = "No fue posible procesar la captura facial.";
        try {
            JsonNode detail = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(error.getResponseBodyAsString()).get("detail");
            if (detail != null && detail.isTextual()) message = detail.asText();
        } catch (java.io.IOException ignored) { }
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> connectionError(RestClientException error) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No fue posible conectar con el servicio facial. Intenta nuevamente."));
    }
}
