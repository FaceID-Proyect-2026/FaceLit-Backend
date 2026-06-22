package com.FaceLit.backend.auth.service.legal;

import java.util.UUID;
import com.FaceLit.backend.auth.dto.request.legal.ConsentRequestDTO;
import com.FaceLit.backend.auth.dto.request.legal.ConsentVerificationRequestDTO;
import com.FaceLit.backend.auth.dto.response.legal.ConsentResponseDTO;
import com.FaceLit.backend.auth.dto.response.legal.ConsentVerificationResponseDTO;

public interface ConsentService {

    // paso 1- menor envia datos del acudiente, el sistema envia el correo
    ConsentResponseDTO requestConsent(ConsentRequestDTO dto);

    // paso 2a - acudiente acepta desde el enlace del correo
    ConsentVerificationResponseDTO confirmConsent(ConsentVerificationRequestDTO dto);

    // Paso 2b — acudiente acepta desde el enlace del correo
    ConsentVerificationResponseDTO refuseConsent(ConsentVerificationRequestDTO dto);

    // reenvia el Token si expiro
    ConsentResponseDTO resendConsentRequest(UUID id_user);

}
