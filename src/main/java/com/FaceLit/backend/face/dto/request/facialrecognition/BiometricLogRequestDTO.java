package com.FaceLit.backend.face.dto.request.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BiometricLogRequestDTO {

    @NotNull(message = "El evento facial es obligatorio")
    private UUID idFacialEvent;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String description;

    @NotNull(message = "La fecha del log es obligatoria")
    private LocalDateTime logDate;
}
