package com.FaceLit.backend.academic.service.audit;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.model.audit.ChangeHistory;
import com.FaceLit.backend.academic.model.enums.ChangeAction;

public interface ChangeHistoryService {

    ChangeHistory record(String entityName, UUID entityId, ChangeAction action,
            String fieldName, String oldValue, String newValue, String createdBy);

    List<ChangeHistory> findByEntity(String entityName, UUID entityId);
}
