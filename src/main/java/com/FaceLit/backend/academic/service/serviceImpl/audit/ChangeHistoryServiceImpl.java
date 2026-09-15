package com.FaceLit.backend.academic.service.serviceImpl.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.model.audit.ChangeHistory;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.repository.audit.ChangeHistoryRepository;
import com.FaceLit.backend.academic.service.audit.ChangeHistoryService;

import jakarta.transaction.Transactional;

@Service
public class ChangeHistoryServiceImpl implements ChangeHistoryService {

    private final ChangeHistoryRepository changeHistoryRepository;

    public ChangeHistoryServiceImpl(ChangeHistoryRepository changeHistoryRepository) {
        this.changeHistoryRepository = changeHistoryRepository;
    }

    @Override
    @Transactional
    public ChangeHistory record(String entityName, UUID entityId, ChangeAction action,
            String fieldName, String oldValue, String newValue, String createdBy) {
        ChangeHistory history = new ChangeHistory();
        history.setEntityName(entityName);
        history.setEntityId(entityId);
        history.setAction(action);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setCreatedBy(createdBy);
        return changeHistoryRepository.save(history);
    }

    @Override
    public List<ChangeHistory> findByEntity(String entityName, UUID entityId) {
        return changeHistoryRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId);
    }
}
