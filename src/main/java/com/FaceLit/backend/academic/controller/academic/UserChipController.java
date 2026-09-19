package com.FaceLit.backend.academic.controller.academic;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.academic.dto.request.academic.TransferChipRequestDTO;
import com.FaceLit.backend.academic.dto.request.academic.UserChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import com.FaceLit.backend.academic.service.academic.UserChipService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/academic")
public class UserChipController {

    private final UserChipService userChipService;

    public UserChipController(UserChipService userChipService) {
        this.userChipService = userChipService;
    }

    @PostMapping("/chips/{idChip}/apprentices")
    public ResponseEntity<UserChipResponseDTO> assignInitialChip(
            @PathVariable UUID idChip,
            @Valid @RequestBody UserChipRequestDTO dto) {
        return ResponseEntity.ok(userChipService.assignInitialChip(idChip, dto));
    }

    @GetMapping("/chips/{idChip}/apprentices")
    public ResponseEntity<List<UserChipResponseDTO>> findApprenticesByChip(@PathVariable UUID idChip) {
        return ResponseEntity.ok(userChipService.findApprenticesByChip(idChip));
    }

    @GetMapping("/users/{idUser}/chip")
    public ResponseEntity<UserChipResponseDTO> getActiveChipByUser(@PathVariable UUID idUser) {
        return ResponseEntity.ok(userChipService.getActiveChipByUser(idUser));
    }

    @GetMapping("/users/{idUser}/chip/history")
    public ResponseEntity<List<UserChipResponseDTO>> getChipHistoryByUser(@PathVariable UUID idUser) {
        return ResponseEntity.ok(userChipService.getChipHistoryByUser(idUser));
    }

    @GetMapping("/users/{idUser}/chip/transfer-targets")
    public ResponseEntity<List<UserChipResponseDTO>> getTransferTargets(@PathVariable UUID idUser) {
        return ResponseEntity.ok(userChipService.getTransferTargets(idUser));
    }

    @PostMapping("/users/{idUser}/chip/transfer")
    public ResponseEntity<UserChipResponseDTO> transferChip(
            @PathVariable UUID idUser,
            @Valid @RequestBody TransferChipRequestDTO dto) {
        return ResponseEntity.ok(userChipService.transferChip(idUser, dto));
    }
}
