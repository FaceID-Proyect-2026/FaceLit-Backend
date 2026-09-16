package com.FaceLit.backend.auth.dto.request.roleandpermission;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

//  este ya es imolementacion del Correo y la contraeña
@Getter
@Setter
public class LoginRequestDTO {

    // El usuario se identifica con el documento registrado en user_app.
    @NotBlank(message = "El número de documento es obligatorio")
    private String numberDocument;

    // Se compara con el hash BCrypt guardado en Credential.password
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

}
