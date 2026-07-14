package com.FaceLit.backend.auth.controller.legal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.request.legal.ConsentRequestDTO;
import com.FaceLit.backend.auth.dto.request.legal.ConsentResendRequestDTO;
import com.FaceLit.backend.auth.dto.request.legal.ConsentVerificationRequestDTO;
import com.FaceLit.backend.auth.dto.response.legal.ConsentResponseDTO;
import com.FaceLit.backend.auth.dto.response.legal.ConsentVerificationResponseDTO;
import com.FaceLit.backend.auth.service.legal.ConsentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/consent")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    // PASO 1 — menor envía datos del acudiente
    // POST /api/consent/request
    @PostMapping("/request")
    public ResponseEntity<ConsentResponseDTO> requestConsent(@Valid @RequestBody ConsentRequestDTO dto) {

        ConsentResponseDTO response = consentService.requestConsent(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // PASO 2a — acudiente acepta
    // POST /api/consent/confirm

    @PostMapping("/confirm")
    public ResponseEntity<ConsentVerificationResponseDTO> confirmConsent(
            @Valid @RequestBody ConsentVerificationRequestDTO dto) {

        ConsentVerificationResponseDTO response = consentService.confirmConsent(dto);
        return ResponseEntity.ok(response);

    }

    // PASO 2b — acudiente rechaza
    // POST /api/consent/refuse
    @PostMapping("/refuse")
    public ResponseEntity<ConsentVerificationResponseDTO> refuseConsent(
            @Valid @RequestBody ConsentVerificationRequestDTO dto) {

        ConsentVerificationResponseDTO response = consentService.refuseConsent(dto);
        return ResponseEntity.ok(response);

    }

    // RENVIAR TOKEN SI EXPIRO ( ESTO ES PARA CONSENTIMIENTO PENDIENTE DEL MENOR DE
    // EDAD)
    @PostMapping("/resend")
   public ResponseEntity<ConsentResponseDTO> resendConsent(
        @RequestBody ConsentResendRequestDTO dto) {

    return ResponseEntity.ok(
            consentService.resendConsentRequest(dto.getId_user())); 
    }

}