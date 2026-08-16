package com.FaceLit.backend.face.dto.response.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.face.model.enums.EventOrigin;
import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacialEventResponseDTO {

    private UUID idFacialEvent;
    private UUID idUserApp;
    private UUID idEnvironment;
    private UUID idChip;
    private UUID idDevice;
    private LocalDateTime eventDatetime;
    private EventType eventType;
    private RecognitionResult recognitionResult;
    private SendStatus sendStatus;
    private EventOrigin origin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
