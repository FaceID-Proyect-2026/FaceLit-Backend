package com.FaceLit.backend.auth.controller.legal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.request.legal.AcceptanceTermsRequestDTO;
import com.FaceLit.backend.auth.dto.response.legal.AcceptanceTermsResponseDTO;
import com.FaceLit.backend.auth.service.legal.AcceptanceTermsService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AcceptanceTermsController {

    private final AcceptanceTermsService acceptanceTermsService;

    public AcceptanceTermsController(AcceptanceTermsService acceptanceTermsService) {
        this.acceptanceTermsService = acceptanceTermsService;

    }
    // POST /api/auth/aceptar-terminos
    // Público — el usuario aún no tiene JWT

    @PostMapping("/aceptar-terminos")
    public ResponseEntity<AcceptanceTermsResponseDTO> acceptanceTerms(
            @Valid @RequestBody AcceptanceTermsRequestDTO dto) {

        AcceptanceTermsResponseDTO response = acceptanceTermsService.acceptanceTerms(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
