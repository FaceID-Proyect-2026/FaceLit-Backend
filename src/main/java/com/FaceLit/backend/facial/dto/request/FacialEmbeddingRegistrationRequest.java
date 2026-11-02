package com.FaceLit.backend.facial.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@io.swagger.v3.oas.annotations.media.Schema(name = "Registration")
public record FacialEmbeddingRegistrationRequest(
        @JsonProperty("user_id") @NotNull UUID userId,
        @JsonProperty("image_base64") @NotBlank @Size(max = 5000000) String imageBase64,
        @JsonProperty("image_frames") @Size(max = 18) List<@NotBlank @Size(max = 5000000) String> imageFrames,
        @JsonProperty("liveness_challenge") @Size(max = 40) String livenessChallenge,
        @JsonProperty("liveness_challenges") @Size(max = 3) List<@NotBlank @Size(max = 40) String> livenessChallenges,
        @JsonProperty("photo_reference") @Size(max = 500) String photoReference,
        @JsonProperty("replace_existing") boolean replaceExisting,
        @JsonProperty("reset_token") UUID resetToken) {}
