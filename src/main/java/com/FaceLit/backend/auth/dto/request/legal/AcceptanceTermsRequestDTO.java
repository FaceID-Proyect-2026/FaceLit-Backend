package com.FaceLit.backend.auth.dto.request.legal;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AcceptanceTermsRequestDTO {

    // UUID del usuario que viene del frontend
    // El frontend lo tiene desde la respuesta del registro
    @NotNull(message = "El ID de usuario es obligatorio")
    private UUID id_user;

    // true = aceptó leer los derechos
    // false = no aceptó — el backend lo rechaza
    @NotNull(message = "El campo aceptado es obligatorio")
    private Boolean accepted;

}
