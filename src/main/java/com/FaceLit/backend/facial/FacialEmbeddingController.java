package com.FaceLit.backend.facial;

import java.util.Map;
import com.FaceLit.backend.facial.service.FacialEmbeddingRegistrationService;
import com.FaceLit.backend.facial.dto.request.FacialEmbeddingRegistrationRequest;
import com.FaceLit.backend.facial.dto.request.FacialLivenessRequest;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
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
    private final FacialEmbeddingRegistrationService service;

    public FacialEmbeddingController(FacialEmbeddingRegistrationService service) {
        this.service = service;
    }

    @PostMapping("/from-image")
    public ResponseEntity<JsonNode> register(@Valid @RequestBody FacialEmbeddingRegistrationRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request, userId));
    }

    @PostMapping("/liveness-check")
    public JsonNode checkLiveness(@Valid @RequestBody FacialLivenessRequest request) {
        return service.checkLiveness(request);
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
