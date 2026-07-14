package com.FaceLit.backend.auth.dto.response.legal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AcceptanceTermsResponseDTO {

    private String message; // mensajes que vera el usuario

    // "ACCEPTED" → el usuario aceptó los terminos, puede continuar
    // "REJECTED" → el usuario no aceptó los terminos
    private String status;

    public static AcceptanceTermsResponseDTO accepted() {
        return new AcceptanceTermsResponseDTO("Derechos de acceso aceptados correctamente",
                "ACCEPTED");

    }

    public static AcceptanceTermsResponseDTO rejected() {
        return new AcceptanceTermsResponseDTO("No puede continuar sin confirmar lectura o aceptar responsabilidad",
                "REJECTED");
    }

}
