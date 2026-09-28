package com.FaceLit.backend.environment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;
import com.FaceLit.backend.environment.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.request.RecordEnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environment.dto.response.RecordEnvironmentResponseDTO;
import com.FaceLit.backend.environment.service.EnvironmentService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/environment")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping("/environments")
    public ResponseEntity<List<EnvironmentResponseDTO>> searchEnvironments(
            @RequestParam(required = false) String search) {
        return noStore(environmentService.searchEnvironments(search));
    }

    @PostMapping("/environments")
    public ResponseEntity<EnvironmentResponseDTO> getOrCreateEnvironment(
            @Valid @RequestBody EnvironmentRequestDTO dto,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(environmentService.getOrCreateEnvironment(dto, principalId(principal)));
    }

    @GetMapping("/instructors-for-session")
    public ResponseEntity<List<InstructorResponseDTO>> findInstructorsForSession(
            @AuthenticationPrincipal Object principal) {
        return noStore(environmentService.findInstructorsForSession(principalId(principal)));
    }

    @GetMapping("/chips-for-instructor/{idInstructor}")
    public ResponseEntity<List<ChipResponseDTO>> findChipsForInstructor(@PathVariable UUID idInstructor) {
        return noStore(environmentService.findChipsForInstructor(idInstructor));
    }

    @PostMapping("/sessions")
    public ResponseEntity<RecordEnvironmentResponseDTO> createSession(
            @Valid @RequestBody RecordEnvironmentRequestDTO dto,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(environmentService.createSession(dto, principalId(principal)));
    }

    @PutMapping("/sessions/{idRecordEnvironment}")
    public ResponseEntity<RecordEnvironmentResponseDTO> updateSession(
            @PathVariable UUID idRecordEnvironment,
            @Valid @RequestBody RecordEnvironmentRequestDTO dto,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(environmentService.updateSession(idRecordEnvironment, dto, principalId(principal)));
    }

    private UUID principalId(Object principal) {
        return UUID.fromString(principal.toString());
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
