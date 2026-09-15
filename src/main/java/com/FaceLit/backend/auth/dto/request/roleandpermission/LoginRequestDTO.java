package com.FaceLit.backend.auth.dto.request.roleandpermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank(message = "El número de documento es obligatorio")
    private String documentNumber;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotNull(message = "Debe aceptar las políticas de privacidad")
    private Boolean accepted;

}
