package com.FaceLit.backend.environments.controller.environment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.environments.dto.request.environment.ChipEnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.ChipEnvironmentResponseDTO;
import com.FaceLit.backend.environments.service.environment.ChipEnvironmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/chip-environments")
public class ChipEnvironmentController {

    private final ChipEnvironmentService chipEnvironmentService;

    public ChipEnvironmentController(ChipEnvironmentService chipEnvironmentService) {
        this.chipEnvironmentService = chipEnvironmentService;
    }

    // POST /api/admin/chip-environments
    @PostMapping
    public ResponseEntity<ChipEnvironmentResponseDTO> assign(
            @Valid @RequestBody ChipEnvironmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chipEnvironmentService.assignChipToEnvironment(dto));
    }

    // GET /api/admin/chip-environments/environment/{idEnvironment}
    @GetMapping("/environment/{idEnvironment}")
    public ResponseEntity<List<ChipEnvironmentResponseDTO>> getByEnvironment(
            @PathVariable UUID idEnvironment) {
        return ResponseEntity.ok(
                chipEnvironmentService.getChipsByEnvironment(idEnvironment));
    }

    // GET /api/admin/chip-environments/chip/{idChip}
    @GetMapping("/chip/{idChip}")
    public ResponseEntity<List<ChipEnvironmentResponseDTO>> getByChip(
            @PathVariable UUID idChip) {
        return ResponseEntity.ok(
                chipEnvironmentService.getEnvironmentsByChip(idChip));
    }

    // DELETE /api/admin/chip-environments/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        chipEnvironmentService.removeAssignment(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/admin/chip-environments/{id}/permanent
    // Elimina completamente — solo si ya está INACTIVE
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id) {
        chipEnvironmentService.permanentDeleteAssignment(id);
        return ResponseEntity.noContent().build();

    }

}
