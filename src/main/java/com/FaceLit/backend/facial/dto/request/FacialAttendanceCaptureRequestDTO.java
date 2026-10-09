package com.FaceLit.backend.facial.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacialAttendanceCaptureRequestDTO {

    @NotNull(message = "La sesión es obligatoria.")
    private UUID idRecordEnvironment;

    @NotNull(message = "El dispositivo es obligatorio.")
    private UUID idDevice;

    private String imageBase64;

    @Size(min = 3, max = 6, message = "La validación de vida requiere entre 3 y 6 capturas.")
    private List<String> imageFrames;

    private String livenessChallenge;

    private String origin;
}
