package com.FaceLit.backend.environment.controller;

import com.FaceLit.backend.environment.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.request.EnvironmentStateRequestDTO;
import com.FaceLit.backend.environment.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environment.service.EnvironmentAdministrationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrative catalog API; instructor session endpoints remain under /api/environment. */
@RestController
@RequestMapping("/api/admin/environments")
public class EnvironmentAdministrationController {
    private final EnvironmentAdministrationService service;

    public EnvironmentAdministrationController(EnvironmentAdministrationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EnvironmentResponseDTO>> search(@RequestParam(required = false) String search) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.search(search));
    }

    @GetMapping("/{idEnvironment}")
    public ResponseEntity<EnvironmentResponseDTO> find(@PathVariable UUID idEnvironment) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find(idEnvironment));
    }

    @PostMapping
    public ResponseEntity<EnvironmentResponseDTO> create(
            @Valid @RequestBody EnvironmentRequestDTO request,
            @AuthenticationPrincipal UUID actorId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actorId));
    }

    @PutMapping("/{idEnvironment}")
    public ResponseEntity<EnvironmentResponseDTO> update(
            @PathVariable UUID idEnvironment,
            @Valid @RequestBody EnvironmentRequestDTO request,
            @AuthenticationPrincipal UUID actorId) {
        return ResponseEntity.ok(service.update(idEnvironment, request, actorId));
    }

    @PatchMapping("/{idEnvironment}/state")
    public ResponseEntity<EnvironmentResponseDTO> changeState(
            @PathVariable UUID idEnvironment,
            @Valid @RequestBody EnvironmentStateRequestDTO request,
            @AuthenticationPrincipal UUID actorId) {
        return ResponseEntity.ok(service.changeState(idEnvironment, request.getState(), actorId));
    }
}
