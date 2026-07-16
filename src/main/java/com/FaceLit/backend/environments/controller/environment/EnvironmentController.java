package com.FaceLit.backend.environments.controller.environment;

import com.FaceLit.backend.environments.dto.request.environment.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.EnvironmentResponseDTO;
import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import com.FaceLit.backend.environments.service.environment.EnvironmentService;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // DELETE /api/admin/environments/{id}
    // Eliminacion logica — cambia status a INACTIVE
    // Solo ADMINISTRATOR y COORDINATOR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable UUID id) {
        environmentService.deleteEnvironment(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // GET /api/admin/environments/paged?page=0&size=10
    // Listado paginado de ambientes
    @GetMapping("/paged")
    public ResponseEntity<Page<EnvironmentResponseDTO>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(environmentService.getAllEnvironmentsPaged(page, size));
    }

    // DELETE /api/admin/environments/{id}/permanent
    // Elimina completamente — solo si ya está INACTIVE
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id) {
        environmentService.permanentDeleteEnvironment(id);
        return ResponseEntity.noContent().build();
    }
}
