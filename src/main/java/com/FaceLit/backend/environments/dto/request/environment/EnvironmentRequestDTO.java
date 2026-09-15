package com.FaceLit.backend.environments.dto.request.environment;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvironmentRequestDTO {

    // Nombre del ambiente — obligatorio, máximo 100 caracteres
    @NotBlank(message = "El nombre del ambiente es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String environmentName;

}

