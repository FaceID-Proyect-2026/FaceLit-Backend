package com.FaceLit.backend.auth.dto.request.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Setter
@Getter
public class EmailVerificationRequestDTO {
   
    // EL userId llega desde el frontend despues del registro
    // El frontend lo guarda temporalmente cuando recibe la respuesta del registro
    @NotNull (message = "El ID de usuario es obligatorio")
    private UUID id_user;

    // El codigo que el usuario recibio en su correo 
    @NotBlank(message = "El código es obligatorio" )
    @Pattern (
        regexp = "^[0-9]{6}$", // numeros del 0 - 9 y de 6 dijitos
        message = "El código debe tener exactamente 6 dígitos numéricos"

    )
    private String code; 

}
