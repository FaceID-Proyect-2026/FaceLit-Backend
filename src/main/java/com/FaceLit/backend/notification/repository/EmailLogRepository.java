package com.FaceLit.backend.notification.repository;

import com.FaceLit.backend.notification.model.EmailLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    Optional<EmailLog> findByIdNotification(UUID idNotification);
    List<EmailLog> findBySendStatus(EmailLog.SendStatus sendStatus);
}
