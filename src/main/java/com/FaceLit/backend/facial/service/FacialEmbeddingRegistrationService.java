package com.FaceLit.backend.facial.service;

import com.FaceLit.backend.academic.repository.ApprenticeRepository;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.facial.FacialEmbeddingVerificationClient;
import com.FaceLit.backend.facial.dto.request.FacialEmbeddingRegistrationRequest;
import com.FaceLit.backend.facial.dto.request.FacialLivenessRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Coordinates authorized enrollment without holding a database transaction during inference. */
@Service
public class FacialEmbeddingRegistrationService {
    private final FacialEmbeddingVerificationClient client;
    private final ApprenticeRepository apprentices;

    public FacialEmbeddingRegistrationService(FacialEmbeddingVerificationClient client, ApprenticeRepository apprentices) {
        this.client = client;
        this.apprentices = apprentices;
    }

    public JsonNode register(FacialEmbeddingRegistrationRequest request, UUID actorId) {
        if (!request.userId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes registrar tu propio rostro.");
        }
        var apprentice = apprentices.findByUser_IdUser(actorId)
                .filter(value -> value.getDeletedAt() == null
                        && value.getUser().getDeletedAt() == null
                        && value.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró un perfil de aprendiz activo para esta cuenta."));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("apprentice_id", apprentice.getIdApprentice());
        payload.put("reset_owner_user_id", actorId);
        payload.put("created_by", actorId.toString());
        payload.put("image_base64", request.imageBase64());
        payload.put("image_frames", request.imageFrames() == null ? List.of() : request.imageFrames());
        payload.put("liveness_challenge", request.livenessChallenge());
        payload.put("liveness_challenges", request.livenessChallenges() == null ? List.of() : request.livenessChallenges());
        payload.put("photo_reference", request.photoReference());
        payload.put("replace_existing", request.replaceExisting());
        payload.put("reset_token", request.resetToken());
        JsonNode response = client.registerFromImage(payload);
        // Never return the internal biometric vector to the browser.
        if (response instanceof ObjectNode object) object.remove("embedding");
        return response;
    }

    public JsonNode checkLiveness(FacialLivenessRequest request) {
        return client.checkLiveness(request);
    }
}
