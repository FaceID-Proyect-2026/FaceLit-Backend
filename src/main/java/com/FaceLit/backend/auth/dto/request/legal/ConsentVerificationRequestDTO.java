package com.FaceLit.backend.auth.dto.request.legal;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConsentVerificationRequestDTO {

      // ID del usuario menor — el frontend lo tiene desde el registro
    @NotNull(message = "El ID de usuario es obligatorio")
    private UUID id_user;

    // Código de 6 dígitos que recibió el acudiente por correo
    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código debe tener exactamente 6 dígitos numéricos")
    private String code;

}
