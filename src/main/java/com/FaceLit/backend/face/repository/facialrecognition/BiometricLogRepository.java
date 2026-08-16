package com.FaceLit.backend.face.repository.facialrecognition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.face.model.facialrecognition.BiometricLog;

@Repository
public interface BiometricLogRepository extends JpaRepository<BiometricLog, UUID> {

    List<BiometricLog> findByFacialEvent_IdFacialEvent(UUID idFacialEvent);

    List<BiometricLog> findByLogDateBetween(LocalDateTime start, LocalDateTime end);
}
