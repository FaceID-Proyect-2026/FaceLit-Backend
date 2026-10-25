package com.FaceLit.backend.facial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Only exposes enrollment for the authenticated owner; never proxies arbitrary paths or vectors. */
@RestController
@RequestMapping("/api/facial/embeddings")
public class FacialEmbeddingGatewayController {
    private final FacialEmbeddingVerificationClient client;

    public FacialEmbeddingGatewayController(FacialEmbeddingVerificationClient client) {
        this.client = client;
    }

    public record Registration(@NotBlank @Size(max = 12000000) String image_base64,
            @Size(max = 18) List<@NotBlank @Size(max = 12000000) String> image_frames,
            @Size(max = 40) String liveness_challenge,
            @Size(max = 3) List<@NotBlank @Size(max = 40) String> liveness_challenges,
            @Size(max = 500) String photo_reference, boolean replace_existing, UUID reset_token) {}

    public record Liveness(@Size(max = 18) List<@NotBlank @Size(max = 12000000) String> image_frames,
            @Size(max = 40) String liveness_challenge) {}

    @PostMapping("/from-image")
    public ResponseEntity<JsonNode> register(@AuthenticationPrincipal UUID owner,
            @Valid @RequestBody Registration request) {
        requireOwner(owner);
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", owner);
        body.put("created_by", owner.toString());
        body.put("image_base64", request.image_base64());
        body.put("image_frames", request.image_frames() == null ? List.of() : request.image_frames());
        body.put("liveness_challenge", request.liveness_challenge());
        body.put("liveness_challenges", request.liveness_challenges() == null ? List.of() : request.liveness_challenges());
        body.put("photo_reference", request.photo_reference());
        body.put("replace_existing", request.replace_existing());
        body.put("reset_token", request.reset_token());
        JsonNode result = call(() -> client.registerFromImage(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(select(result,
                "id", "user_id", "embedding_dimension", "model_name", "photo_reference", "status", "created_at"));
    }

    @PostMapping("/liveness-check")
    public JsonNode liveness(@AuthenticationPrincipal UUID owner, @Valid @RequestBody Liveness request) {
        requireOwner(owner);
        Map<String, Object> body = new HashMap<>();
        body.put("image_frames", request.image_frames() == null ? List.of() : request.image_frames());
        body.put("liveness_challenge", request.liveness_challenge());
        return select(call(() -> client.checkLiveness(body)), "live", "reason");
    }

    private static void requireOwner(UUID owner) {
        if (owner == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida.");
    }

    private static ObjectNode select(JsonNode response, String... fields) {
        if (response == null || !response.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta facial inválida.");
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        for (String field : fields) if (response.has(field)) result.set(field, response.get(field));
        return result;
    }

    private static JsonNode call(Supplier<JsonNode> operation) {
        try {
            return operation.get();
        } catch (RestClientResponseException error) {
            // Service credentials are an operator error, not an expired user session.
            int status = error.getStatusCode().value();
            if (status == 400 || status == 404 || status == 409 || status == 422) {
                throw new ResponseStatusException(HttpStatus.valueOf(status),
                        status == 409 ? "El rostro ya está registrado. Revisa la autorización de reemplazo."
                                : "No fue posible validar el registro facial o su autorización.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Servicio facial no disponible.");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Servicio facial no disponible.");
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
}
