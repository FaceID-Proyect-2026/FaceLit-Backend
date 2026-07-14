package com.FaceLit.backend.auth.controller.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.request.security.RequestPasswordRecoveryDTO;
import com.FaceLit.backend.auth.dto.request.security.ResetPasswordDTO;
import com.FaceLit.backend.auth.dto.response.security.PasswordRecoveryResponseDTO;
import com.FaceLit.backend.auth.service.security.PasswordRecoveryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    public PasswordRecoveryController(PasswordRecoveryService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    // PASO 1 — Solicitar recuperación: el usuario manda su correo
    // POST /api/auth/solicitar-recuperacion
    // Público — el usuario aún no tiene token
    @PostMapping("/request-recovery")
    public ResponseEntity<PasswordRecoveryResponseDTO> requestRecovery(
            @Valid @RequestBody RequestPasswordRecoveryDTO dto) {
        PasswordRecoveryResponseDTO response = passwordRecoveryService.requestRecovery(dto);
        return ResponseEntity.ok(response);
    }

    // PASO 2 — Restablecer contraseña: el usuario manda el código + nueva
    // contraseña
    // POST /api/auth/restablecer-contrasena
    // Público — el usuario aún no tiene token
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordRecoveryResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordDTO dto) {
        PasswordRecoveryResponseDTO response = passwordRecoveryService.resetPassword(dto);
        return ResponseEntity.ok(response);

    }
}
