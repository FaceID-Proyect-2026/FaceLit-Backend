package com.FaceLit.backend.auth.dto.response.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDetailResponseDTO {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String documentType;
    private LocalDate birthDate;
    private String email;
    private String role;
    private String accountStatus;    // ← vuelve a ser el estado REAL de la BD (ACTIVE/INACTIVE/PENDING_CONSENT/BLOCKED)
    private String sessionStatus;    // ← NUEVO — ACTIVE/INACTIVE calculado por JWT, solo informativo
    private LocalDateTime registrationDate;

    private String chipName;
    private String chipCode;
    private String programName;

    private boolean hasSession;
}
