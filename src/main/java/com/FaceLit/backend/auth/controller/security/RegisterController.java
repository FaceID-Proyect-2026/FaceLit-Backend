package com.FaceLit.backend.auth.controller.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.request.security.RegisterRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.RegisterResponseDTO;
import com.FaceLit.backend.auth.service.security.RegisterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/coordinator/users")
public class RegisterController {

    private final RegisterService registerService;

    // Inyeccion por constructor - igual que en el serviceImpl
    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    // POST /api/coordinator/users
    // Solo COORDINATOR. El usuario no se registra a sí mismo.
    @PostMapping
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        RegisterResponseDTO response = registerService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
