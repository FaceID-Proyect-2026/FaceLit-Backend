package com.FaceLit.backend.auth.dto.request.roleandpermission;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

//  este ya es imolementacion del Correo y la contraeña
@Getter
@Setter
public class LoginRequestDTO {

    // El email ya existe en Credential — se usa para buscar al usuario
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String email;

    // Se compara con el hash BCrypt guardado en Credential.password
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Igual que en registro — sin esto el sistema rechaza el acceso
    @NotNull(message = "Debe aceptar las políticas de privacidad")
    private Boolean aceptoPoliticas;
}
