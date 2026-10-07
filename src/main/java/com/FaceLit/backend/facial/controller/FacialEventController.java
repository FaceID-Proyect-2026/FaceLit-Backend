package com.FaceLit.backend.facial.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.facial.dto.request.FacialEventRequestDTO;
import com.FaceLit.backend.facial.dto.request.FacialAttendanceCaptureRequestDTO;
import com.FaceLit.backend.facial.dto.response.AttendanceStatusResponseDTO;
import com.FaceLit.backend.facial.dto.response.FacialEventResponseDTO;
import com.FaceLit.backend.facial.service.FacialEventService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/facial/events")
public class FacialEventController {

    private final FacialEventService facialEventService;

    public FacialEventController(FacialEventService facialEventService) {
        this.facialEventService = facialEventService;
    }

    @PostMapping
    public ResponseEntity<FacialEventResponseDTO> registerEvent(
            @Valid @RequestBody FacialEventRequestDTO dto,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(facialEventService.registerEvent(dto, principalId(principal)));
    }

    @PostMapping("/from-image")
    public ResponseEntity<FacialEventResponseDTO> registerEventFromImage(
            @Valid @RequestBody FacialAttendanceCaptureRequestDTO dto,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(facialEventService.registerEventFromImage(dto, principalId(principal)));
    }

    @GetMapping("/attendance-status")
    public ResponseEntity<AttendanceStatusResponseDTO> getAttendanceStatus(
            @RequestParam UUID idRecordEnvironment,
            @RequestParam UUID idApprentice) {
        return ResponseEntity.ok(facialEventService.getAttendanceStatus(idRecordEnvironment, idApprentice));
    }

    private UUID principalId(Object principal) {
        return UUID.fromString(principal.toString());
    }
}
