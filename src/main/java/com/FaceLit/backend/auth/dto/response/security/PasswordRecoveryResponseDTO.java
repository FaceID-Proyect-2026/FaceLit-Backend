package com.FaceLit.backend.auth.dto.response.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordRecoveryResponseDTO {

    // Mensaje legible para el frontend
    private String message;

    // Se devuelve cuando el código de 6 dígitos fue enviado al correo
    public static PasswordRecoveryResponseDTO codeSent() {
        return new PasswordRecoveryResponseDTO("Se envió un código de recuperación a tu correo electrónico");

    }

    // Se devuelve cuando la contraseña se actualizó correctamente
    public static PasswordRecoveryResponseDTO passwordReset() {
        return new PasswordRecoveryResponseDTO("Contraseña restablecida correctamente");

    }
}
