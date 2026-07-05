package com.FaceLit.backend.environments.controller;

import com.FaceLit.backend.environments.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import com.FaceLit.backend.environments.service.EnvironmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    // POST /api/admin/environments
    // Solo ADMINISTRATOR y COORDINATOR — protegido por SecurityConfig /api/admin/**
    @PostMapping
    public ResponseEntity<EnvironmentResponseDTO> createEnvironment(
            @Valid @RequestBody EnvironmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(environmentService.createEnvironment(dto));
    }

    // PUT /api/admin/environments/{id}
    // Solo ADMINISTRATOR y COORDINATOR
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponseDTO> updateEnvironment(
            @PathVariable UUID id,
            @Valid @RequestBody EnvironmentRequestDTO dto) {
        return ResponseEntity.ok(environmentService.updateEnvironment(id, dto));
    }

    // GET /api/admin/environments
    // Solo ADMINISTRATOR y COORDINATOR
    @GetMapping
    public ResponseEntity<List<EnvironmentResponseDTO>> getAllEnvironments() {
        return ResponseEntity.ok(environmentService.getAllEnvironments());
    }

    // GET /api/admin/environments/{id}
    // Solo ADMINISTRATOR y COORDINATOR
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponseDTO> getEnvironmentById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(environmentService.getEnvironmentById(id));
    }

    // GET /api/admin/environments/search?name=Ambiente 101
    @GetMapping("/search")
    public ResponseEntity<EnvironmentResponseDTO> getByName(
            @RequestParam String name) {
        return ResponseEntity.ok(environmentService.getEnvironmentByName(name));
    }

    // GET /api/admin/environments/status?status=ACTIVE
    @GetMapping("/status")
    public ResponseEntity<List<EnvironmentResponseDTO>> getByStatus(
            @RequestParam EnvironmentStatus status) {
        return ResponseEntity.ok(environmentService.getEnvironmentsByStatus(status));
    }
}
