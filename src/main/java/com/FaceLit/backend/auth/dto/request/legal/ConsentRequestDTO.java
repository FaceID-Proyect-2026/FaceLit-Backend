package com.FaceLit.backend.auth.dto.request.legal;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConsentRequestDTO {

    // // EL userId llega desde el frontend despues del registro
    @NotNull(message = "El ID del usuario es obligatorio")
    private UUID id_user;

    // Datos del acudiente
    @NotBlank(message = "El nombre completo del acudiente es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String fullName;

    // identificacion del acudiente
    @NotBlank(message = "El numero de documento es obligatorio")
    // [0-9] — solo dígitos del 0 al 9, ninguna letra
    @Pattern(regexp = "^[0-9]{1,10}$", message = "El documento solo puede contener números y máximo 10 dígitos")
    private String identityDocument;
    // email del acudiente
    @NotBlank(message = "El correo del acudiente es obligatorio")
    @Email(message = "El correo del acudiente no es válido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    private String emailGuardian;

    
}
