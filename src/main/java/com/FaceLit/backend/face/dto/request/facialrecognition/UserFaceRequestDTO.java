package com.FaceLit.backend.face.dto.request.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.face.dto.validation.ValidBiometricVector;
import com.FaceLit.backend.face.model.enums.FaceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFaceRequestDTO {

    @NotNull(message = "El usuario es obligatorio")
    private UUID idUserApp;

    @NotNull(message = "El vector biométrico es obligatorio")
    @ValidBiometricVector
    private byte[] biometricVector;

    @NotNull(message = "La fecha de registro es obligatoria")
    private LocalDateTime registrationDate;

    @NotNull(message = "El estado de la cara es obligatorio")
    private FaceStatus status;
}
