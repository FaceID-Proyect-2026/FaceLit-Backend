package com.FaceLit.backend.academic.repository.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.audit.ChangeHistory;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, UUID> {

    List<ChangeHistory> findByEntityNameAndEntityIdOrderByCreatedAtDesc(
            String entityName, UUID entityId);
}
