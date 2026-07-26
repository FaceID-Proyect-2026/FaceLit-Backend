package com.FaceLit.backend.auth.service.security;

import java.util.UUID;

import com.FaceLit.backend.auth.dto.request.security.EmailVerificationRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.RegisterRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.EmailVerificationResponseDTO;
import com.FaceLit.backend.auth.dto.response.security.RegisterResponseDTO;
import com.FaceLit.backend.auth.dto.response.security.RegistrationStatusResponseDTO;

public interface RegisterService {
    // impementamos un metodo
    // RegisterResponseDTO = cuando termina el registro, devuelve informacion
    // register = nombre del metodo
    // RegisterRequestDTO = datos que envia el usuario
    // Recibe los datos del formulario
    RegisterResponseDTO register(RegisterRequestDTO dto);

    // Valida el codigo de 6 digitos que llega al correo
    EmailVerificationResponseDTO emailVerification(EmailVerificationRequestDTO dto);

    // reenvia el codigo si expiro
    void resendCode(UUID id_user);

    // este metodo llamado register que recibe datos de RegisterRequestDTO y
    // devolver una respuesta
    // que en esye caso seria (RegisterResponseDTO )

    RegistrationStatusResponseDTO checkStatus(String documentNumber, String email);

}
