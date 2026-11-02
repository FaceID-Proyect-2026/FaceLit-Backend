package com.FaceLit.backend.facial.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@io.swagger.v3.oas.annotations.media.Schema(name = "Liveness")
public record FacialLivenessRequest(
        @JsonProperty("image_frames") @NotNull @Size(min = 3, max = 18) List<@NotBlank @Size(max = 5000000) String> imageFrames,
        @JsonProperty("liveness_challenge") @Size(max = 40) String livenessChallenge) {}
