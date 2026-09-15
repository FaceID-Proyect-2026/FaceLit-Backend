package com.FaceLit.backend.academic.controller.academic;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.academic.model.audit.ChangeHistory;
import com.FaceLit.backend.academic.service.audit.ChangeHistoryService;

@RestController
@RequestMapping("/api/coordinator/change-history")
public class ChangeHistoryController {

    private final ChangeHistoryService changeHistoryService;

    public ChangeHistoryController(ChangeHistoryService changeHistoryService) {
        this.changeHistoryService = changeHistoryService;
    }

    @GetMapping("/{entityName}/{entityId}")
    public ResponseEntity<List<ChangeHistory>> findByEntity(
            @PathVariable String entityName, @PathVariable UUID entityId) {
        return ResponseEntity.ok(changeHistoryService.findByEntity(entityName, entityId));
    }
}
