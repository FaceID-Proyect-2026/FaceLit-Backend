package com.FaceLit.backend.face.repository.facialrecognition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;
import com.FaceLit.backend.face.model.facialrecognition.FacialEvent;

@Repository
public interface FacialEventRepository extends JpaRepository<FacialEvent, UUID> {

    List<FacialEvent> findByUser_IdUser(UUID idUserApp);

    List<FacialEvent> findByEnvironment_IdEnvironment(UUID idEnvironment);

    List<FacialEvent> findByDevice_IdDevice(UUID idDevice);

    List<FacialEvent> findByEventType(EventType eventType);

    List<FacialEvent> findByRecognitionResult(RecognitionResult recognitionResult);

    List<FacialEvent> findBySendStatus(SendStatus sendStatus);

    List<FacialEvent> findByEventDatetimeBetween(LocalDateTime start, LocalDateTime end);
}
