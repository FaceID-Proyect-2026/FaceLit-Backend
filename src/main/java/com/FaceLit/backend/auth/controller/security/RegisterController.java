package com.FaceLit.backend.auth.controller.security;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.request.security.EmailVerificationRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.RegisterRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.EmailVerificationResponseDTO;
import com.FaceLit.backend.auth.dto.response.security.RegisterResponseDTO;
import com.FaceLit.backend.auth.dto.response.security.RegistrationStatusResponseDTO;
import com.FaceLit.backend.auth.service.security.RegisterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class RegisterController {

    private final RegisterService registerService;

    // Inyeccion por constructor - igual que en el serviceImpl
    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    // PASO 1 — Registro: recibe datos del formulario
    // POST /api/auth/register
    // Público — el usuario aún no tiene token

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        RegisterResponseDTO response = registerService.register(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        // Siempre devuelve 202 Accepted porque el registro no está completo
        // hasta que el usuario verifique su email

        // PASO 2 — Verificar el código de 6 dígitos
        // POST /api/auth/verify-email
        // Público — el usuario aún no tiene token
    }

    @PostMapping("/verify-email")

    public ResponseEntity<EmailVerificationResponseDTO> verifyEmail(
            @Valid @RequestBody EmailVerificationRequestDTO dto) {
        EmailVerificationResponseDTO response = registerService.emailVerification(dto);

        return ResponseEntity.ok(response);
    }

    // PASO 3 — Reenviar código si expiró
    // POST /api/auth/resend-code?userId={uuid}
    // Público

    @PostMapping("/resend-code")

    public ResponseEntity<Map<String, String>> resendCode(
            @RequestParam UUID id_user) {
        registerService.resendCode(id_user);

        return ResponseEntity.ok(Map.of("message", "Código reenviado a tu correo electrónico"));
    }

    // GET /api/auth/registration-status?document=...&email=...
    // Público — se usa cuando el registro falla por duplicado
    @GetMapping("/registration-status")
    public ResponseEntity<RegistrationStatusResponseDTO> registrationStatus(
            @RequestParam(required = false) String document,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(registerService.checkStatus(document, email));
    }

}
