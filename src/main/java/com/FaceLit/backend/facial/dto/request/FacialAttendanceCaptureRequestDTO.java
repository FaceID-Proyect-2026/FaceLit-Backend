package com.FaceLit.backend.facial.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacialAttendanceCaptureRequestDTO {

    @NotNull(message = "La sesión es obligatoria.")
    private UUID idRecordEnvironment;

    @NotNull(message = "El dispositivo es obligatorio.")
    private UUID idDevice;

    @NotBlank(message = "La imagen es obligatoria.")
    private String imageBase64;

    private String origin;
}
