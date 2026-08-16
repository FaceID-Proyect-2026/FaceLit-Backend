package com.FaceLit.backend.face.dto.response.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.face.model.enums.FaceStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserFaceResponseDTO {

    private UUID idUserFace;
    private UUID idUserApp;
    private FaceStatus status;
    private LocalDateTime registrationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
