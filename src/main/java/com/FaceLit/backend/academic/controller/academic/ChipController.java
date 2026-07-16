package com.FaceLit.backend.academic.controller.academic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.academic.dto.request.academic.ChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.service.academic.ChipService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/chips")
public class ChipController {
    private final ChipService chipService;

    public ChipController(ChipService chipService) {
        this.chipService = chipService;
    }

    // POST /api/admin/chips
    @PostMapping
    public ResponseEntity<ChipResponseDTO> create(
            @Valid @RequestBody ChipRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chipService.createChip(dto));
    }

    // PUT /api/admin/chips/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ChipResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ChipRequestDTO dto) {
        return ResponseEntity.ok(chipService.updateChip(id, dto));
    }

    // DELETE /api/admin/chips/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        chipService.deleteChip(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/admin/chips
    @GetMapping
    public ResponseEntity<List<ChipResponseDTO>> getAll() {
        return ResponseEntity.ok(chipService.getAllChips());
    }

    // GET /api/admin/chips/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ChipResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(chipService.getChipById(id));
    }

    // GET /api/admin/chips/program/{idProgram}
    @GetMapping("/program/{idProgram}")
    public ResponseEntity<List<ChipResponseDTO>> getByProgram(
            @PathVariable UUID idProgram) {
        return ResponseEntity.ok(chipService.getChipsByProgram(idProgram));
    }

    // GET /api/admin/chips/status?state=ACTIVE
    @GetMapping("/status")
    public ResponseEntity<List<ChipResponseDTO>> getByState(
            @RequestParam ChipState state) {
        return ResponseEntity.ok(chipService.getChipsByState(state));
    }

    // DELETE /api/admin/chips/{id}/permanent
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id) {
        chipService.permanentDeleteChip(id);
        return ResponseEntity.noContent().build();
    }

}
