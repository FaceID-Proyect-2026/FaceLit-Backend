package com.FaceLit.backend.environments.dto.request.environment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter

public class ChipEnvironmentRequestDTO {

    @NotNull(message = "La ficha es obligatoria")
    private UUID idChip;

    @NotNull(message = "El ambiente es obligatorio")
    private UUID idEnvironment;

    @NotNull(message = "La fecha de asignacion es obligatoria")
    private LocalDate assignmentDate;

}
