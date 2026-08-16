package com.FaceLit.backend.face.dto.response.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BiometricLogResponseDTO {

    private UUID idBiometricLog;
    private UUID idFacialEvent;
    private String description;
    private LocalDateTime logDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
