package com.FaceLit.backend.academic.dto.request.academic;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserChipRequestDTO {

    // El aprendiz ingresa el codigo de ficha que le compartio el admin
    // Ejemplo: A3F9K2M7
    @NotBlank(message = "El código de ficha es obligatorio")
    private String chipCode;

}
