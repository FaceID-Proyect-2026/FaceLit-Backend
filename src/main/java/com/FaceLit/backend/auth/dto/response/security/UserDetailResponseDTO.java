package com.FaceLit.backend.auth.dto.response.security;

import java.time.OffsetDateTime;
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
    private String email;
    private String role;
    private String accountStatus;    // Estado real de la cuenta.
    private String sessionStatus;    // ← NUEVO — ACTIVE/INACTIVE calculado por JWT, solo informativo
    private OffsetDateTime registrationDate;

    private boolean hasSession;
}
