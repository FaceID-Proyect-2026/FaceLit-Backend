package com.FaceLit.backend.facial.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.facial.model.FacialEvent;
import com.FaceLit.backend.facial.model.enums.FacialEventType;

public interface FacialEventRepository extends JpaRepository<FacialEvent, UUID> {

    Optional<FacialEvent> findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
            UUID idRecordEnvironment,
            UUID idApprentice,
            FacialEventType eventType);

    boolean existsByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNull(
            UUID idRecordEnvironment,
            UUID idApprentice,
            FacialEventType eventType);
}
