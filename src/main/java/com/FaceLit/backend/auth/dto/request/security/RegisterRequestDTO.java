package com.FaceLit.backend.auth.dto.request.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.FaceLit.backend.auth.model.enums.RoleName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {

    // Solo letras, máximo 50 caracteres
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras")

    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String firstName;

    // Solo letras, máximo 50 caracteres
    // @Pattern, ^ — inicio del texto,
    // [a-zA-Z...] — solo letras minúsculas, mayúsculas y letras con tilde
    // ñÑ — incluye la ñ porque es español
    // el espacio adentro del corchete permite nombres compuestos como "María José"
    // + — al menos un carácter
    // $ — fin del texto
    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El apellido solo puede contener letras")
    @Size(max = 60, message = "El apellido no puede superar los 60 caracteres")
    private String lastName;

    // Solo números, exactamente máximo 10 caracteres
    @NotBlank(message = "El numero de documento es obligatorio")
    // [0-9] — solo dígitos del 0 al 9, ninguna letra
    @Pattern(regexp = "^[0-9]{10}$", message = "El documento debe tener exactamente 10 dígitos numéricos")

    private String documentNumber;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    private String email;

    @NotNull(message = "El rol es obligatorio")
    private RoleName role;

    // La validación de 8-15 chars va aquí en el DTO
    // En la BD se guarda el hash de 255 chars — son cosas distintas
    // Request DTO Es el objeto que se recibe desde el cliente ( datos que envia el
    // usuario)
}
