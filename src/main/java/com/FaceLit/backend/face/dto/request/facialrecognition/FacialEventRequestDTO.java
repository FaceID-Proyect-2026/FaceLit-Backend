package com.FaceLit.backend.face.dto.request.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.face.model.enums.EventOrigin;
import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacialEventRequestDTO {

    private UUID idUserApp;

    @NotNull(message = "El ambiente es obligatorio")
    private UUID idEnvironment;

    private UUID idChip;

    @NotNull(message = "El dispositivo es obligatorio")
    private UUID idDevice;

    @NotNull(message = "La fecha del evento es obligatoria")
    private LocalDateTime eventDatetime;

    @NotNull(message = "El tipo de evento es obligatorio")
    private EventType eventType;

    @NotNull(message = "El resultado del reconocimiento es obligatorio")
    private RecognitionResult recognitionResult;

    @NotNull(message = "El estado de envío es obligatorio")
    private SendStatus sendStatus;

    @NotNull(message = "El origen del evento es obligatorio")
    private EventOrigin origin;
}
