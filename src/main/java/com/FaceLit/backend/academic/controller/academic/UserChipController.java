package com.FaceLit.backend.academic.controller.academic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.academic.dto.request.academic.UserChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import com.FaceLit.backend.academic.service.academic.UserChipService;

import java.util.List;
import java.util.UUID;

@RestController
public class UserChipController {

    private final UserChipService userChipService;

    public UserChipController(UserChipService userChipService) {
        this.userChipService = userChipService;
    }

    // POST /api/apprentice/join-chip
    // Solo APPRENTICE — el aprendiz ingresa el codigo de ficha
    // El userId viene del JWT, no del body — seguridad
    @PostMapping("/api/apprentice/join-chip")
    public ResponseEntity<UserChipResponseDTO> joinChip(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UserChipRequestDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userChipService.joinChip(userId, dto));
    }

    // GET /api/admin/chips/{idChip}/apprentices
    // El admin ve todos los aprendices de una ficha
    @GetMapping("/api/admin/chips/{idChip}/apprentices")
    public ResponseEntity<List<UserChipResponseDTO>> getApprentices(
            @PathVariable UUID idChip) {
        return ResponseEntity.ok(userChipService.getApprenticesByChip(idChip));
    }

    // DELETE /api/admin/user-chips/{idUserChip}
    // El admin desvincula un aprendiz de una ficha
    @DeleteMapping("/api/admin/user-chips/{idUserChip}")
    public ResponseEntity<Void> removeApprentice(
            @PathVariable UUID idUserChip) {
        userChipService.removeApprenticeFromChip(idUserChip);
        return ResponseEntity.noContent().build();
    }

    // GET /api/apprentice/my-chips
    // El aprendiz ve su historial de fichas
    @GetMapping("/api/apprentice/my-chips")
    public ResponseEntity<List<UserChipResponseDTO>> myChips(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userChipService.getChipsByUser(userId));
    }
}
