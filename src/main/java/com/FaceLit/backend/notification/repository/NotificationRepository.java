package com.FaceLit.backend.notification.repository;

import com.FaceLit.backend.notification.model.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByReferenceEntityAndReferenceId(String referenceEntity, UUID referenceId);
    List<Notification> findByIdUserAppOrderByCreatedAtDesc(UUID idUserApp);
}
