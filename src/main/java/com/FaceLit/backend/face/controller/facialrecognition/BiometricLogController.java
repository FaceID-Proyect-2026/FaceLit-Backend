package com.FaceLit.backend.face.controller.facialrecognition;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.face.dto.request.facialrecognition.BiometricLogRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.BiometricLogResponseDTO;
import com.FaceLit.backend.face.service.facialrecognition.BiometricLogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/biometric-logs")
public class BiometricLogController {

    private final BiometricLogService biometricLogService;

    public BiometricLogController(BiometricLogService biometricLogService) {
        this.biometricLogService = biometricLogService;
    }

    @PostMapping
    public ResponseEntity<BiometricLogResponseDTO> create(@Valid @RequestBody BiometricLogRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(biometricLogService.createBiometricLog(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BiometricLogResponseDTO> update(@PathVariable UUID id,
            @Valid @RequestBody BiometricLogRequestDTO dto) {
        return ResponseEntity.ok(biometricLogService.updateBiometricLog(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<BiometricLogResponseDTO>> getAll() {
        return ResponseEntity.ok(biometricLogService.getAllBiometricLogs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BiometricLogResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(biometricLogService.getBiometricLogById(id));
    }

    @GetMapping("/facial-event/{idFacialEvent}")
    public ResponseEntity<List<BiometricLogResponseDTO>> getByFacialEvent(@PathVariable UUID idFacialEvent) {
        return ResponseEntity.ok(biometricLogService.getBiometricLogsByFacialEvent(idFacialEvent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        biometricLogService.deleteBiometricLog(id);
        return ResponseEntity.noContent().build();
    }
}
