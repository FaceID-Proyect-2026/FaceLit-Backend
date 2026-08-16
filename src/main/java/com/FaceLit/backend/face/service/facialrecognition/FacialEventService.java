package com.FaceLit.backend.face.service.facialrecognition;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.face.dto.request.facialrecognition.FacialEventRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.FacialEventResponseDTO;
import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;

public interface FacialEventService {

    FacialEventResponseDTO createFacialEvent(FacialEventRequestDTO dto);

    FacialEventResponseDTO updateFacialEvent(UUID id, FacialEventRequestDTO dto);

    List<FacialEventResponseDTO> getAllFacialEvents();

    FacialEventResponseDTO getFacialEventById(UUID id);

    List<FacialEventResponseDTO> getFacialEventsByUser(UUID idUserApp);

    List<FacialEventResponseDTO> getFacialEventsByEnvironment(UUID idEnvironment);

    List<FacialEventResponseDTO> getFacialEventsByDevice(UUID idDevice);

    List<FacialEventResponseDTO> getFacialEventsByType(EventType eventType);

    List<FacialEventResponseDTO> getFacialEventsByRecognitionResult(RecognitionResult recognitionResult);

    List<FacialEventResponseDTO> getFacialEventsBySendStatus(SendStatus sendStatus);

    void deleteFacialEvent(UUID id);
}
