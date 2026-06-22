package com.FaceLit.backend.auth.dto.response.security;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class EmailVerificationResponseDTO {

    private String message; // mensajes que vera el usuario


    // estado de la respuesta
    // "EMAIL_VERIFIED" -- Mayor de edad, puede ir al login
    // "MINOR_AGE_REDIRECT" -- menor de edad, va al flujo del acudiente 
    private String status;


    private boolean minor; // ¿ necesita autenticacion del acudiente (true-- si necesita / flase - no lo necesita)

    // true si es menor de edad - frontend lo usa para redirigir

    // Metodo de fábrica — igual que en RegisterResponseDTO
    public static EmailVerificationResponseDTO majorAge() { // respuesta para el mayor de edad 
        return new EmailVerificationResponseDTO("Email verificado. Registro completado",
            "EMAIL_VERIFIED",
            false);
    }
    public static EmailVerificationResponseDTO minorAge() { // respuesta  para el menor de edad 
        return new EmailVerificationResponseDTO("Email verificado. Completa el registro con la autorización del acudiente", "MINOR_AGE_REDIRECT", true); 


}
}
