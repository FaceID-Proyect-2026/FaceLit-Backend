package com.FaceLit.backend.environment.dto.request;

import com.FaceLit.backend.environment.model.enums.EnvironmentState;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvironmentStateRequestDTO {
    @NotNull(message = "El estado del ambiente es obligatorio.")
    private EnvironmentState state;
}
