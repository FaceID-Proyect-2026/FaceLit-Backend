package com.FaceLit.backend.auth.dto.response.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistrationStatusResponseDTO {

    private UUID idUser;
    private boolean emailVerified;
    private String accountStatus; // ACTIVE, PENDING_CONSENT, etc.
    private boolean isMinor;
    private String consentStatus; // null, PENDING, ACCEPTED, REJECTED
    private String guardianEmail; // null si no aplica o no se ha solicitado

}
