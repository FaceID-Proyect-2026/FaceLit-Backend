package com.FaceLit.backend.facial.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.FaceLit.backend.facial.model.FaceResetRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FaceResetRequestRepository extends JpaRepository<FaceResetRequest, UUID> {
    Optional<FaceResetRequest> findFirstByUserIdOrderByRequestedAtDesc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FaceResetRequest r where r.id = :id")
    Optional<FaceResetRequest> lockById(@Param("id") UUID id);

    @Query("select r.id from FaceResetRequest r where r.contextJson is not null and (r.requestNotified = false or (r.status <> 'PENDING' and r.decisionNotified = false) or (r.consumedAt is not null and r.completionNotified = false))")
    List<UUID> findPendingNotifications();
}
