package com.FaceLit.backend.notification.controller;

import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.dto.request.FacialDecisionRequestDTO;
import com.FaceLit.backend.notification.dto.request.MonolithEventRequestDTO;
import com.FaceLit.backend.notification.dto.response.NotificationResponseDTO;
import com.FaceLit.backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponseDTO> list(Authentication authentication) {
        return notificationService.findForUser(currentUserId(authentication));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponseDTO markRead(@PathVariable UUID id, Authentication authentication) {
        return notificationService.markRead(currentUserId(authentication), id);
    }

    @PatchMapping("/read-all")
    public List<NotificationResponseDTO> markAllRead(Authentication authentication) {
        return notificationService.markAllRead(currentUserId(authentication));
    }

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> createDirect(
            @Valid @RequestBody CreateNotificationRequestDTO request,
            Authentication authentication) {
        requireCoordinator(authentication);
        return ResponseEntity.ok(notificationService.createForRecipient(request));
    }

    @PostMapping("/events")
    public ResponseEntity<List<NotificationResponseDTO>> createCoordinatorEvent(
            @Valid @RequestBody MonolithEventRequestDTO request,
            Authentication authentication) {
        requireCoordinator(authentication);
        return ResponseEntity.ok(notificationService.createCoordinatorEvent(request));
    }

    @PatchMapping("/{id}/facial-request")
    public ResponseEntity<Map<String, String>> resolveFacialRequest(
            @PathVariable UUID id,
            @Valid @RequestBody FacialDecisionRequestDTO request,
            Authentication authentication) {
        requireCoordinator(authentication);
        notificationService.markRead(currentUserId(authentication), id);
        // Hook futuro: llamar al micro PATCH /api/faces/reset-requests/{requestId}
        // con la decision aceptada/rechazada cuando ese servicio exista.
        return ResponseEntity.ok(Map.of("status", "registered", "decision", request.decision()));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private void requireCoordinator(Authentication authentication) {
        boolean coordinator = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_COORDINATOR"::equals);
        if (!coordinator) {
            throw new IllegalArgumentException("Solo Coordinador puede crear eventos globales");
        }
    }
}
