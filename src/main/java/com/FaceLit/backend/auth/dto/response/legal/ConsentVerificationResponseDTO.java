package com.FaceLit.backend.auth.dto.response.legal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ConsentVerificationResponseDTO { // respuesta cuando el acudiente acepta o rechaza:

    private String message;
    private String status; // // "ACCEPTED" o "REJECTED"

    public static ConsentVerificationResponseDTO accepted() {
        return new ConsentVerificationResponseDTO("Autorización aceptada. El menor ya puede acceder al sistema.",
                "ACCEPTED");

    }

    // Acudiente hizo clic en rechazar — enlace válidoF
    public static ConsentVerificationResponseDTO refused() {
        return new ConsentVerificationResponseDTO("Autorización rechazada. El registro del menor no fue completado.",
                "REFUSED");
    }

    // Acudiente hizo clic pero el enlace ya expiró
    public static ConsentVerificationResponseDTO expired() {
        return new ConsentVerificationResponseDTO("El enlace ha expirado. El menor debe solicitar uno nuevo.",
                "EXPIRED");
    }

}