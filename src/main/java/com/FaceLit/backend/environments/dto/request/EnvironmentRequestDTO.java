package com.FaceLit.backend.environments.dto.request;

import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvironmentRequestDTO {

    // Nombre del ambiente — obligatorio, máximo 100 caracteres
    @NotBlank(message = "El nombre del ambiente es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String environmentName;

    // Capacidad máxima de usuarios — mínimo 1
    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    private Integer capacity;

    // Estado — ACTIVE o INACTIVE
    // Si no se manda, el ServiceImpl lo pone ACTIVE por defecto
    private EnvironmentStatus status;
}
