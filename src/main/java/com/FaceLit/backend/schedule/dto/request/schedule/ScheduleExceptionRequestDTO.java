package com.FaceLit.backend.schedule.dto.request.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ScheduleExceptionRequestDTO {

    // Horario al que pertenece la excepcion
    @NotNull(message = "El horario es obligatorio")
    private UUID idSchedule;

    // Ambiente alterno que se usara ese dia
    @NotNull(message = "El ambiente alterno es obligatorio")
    private UUID idEnvironment;

    // Fecha en la que ocurre la excepcion
    @NotNull(message = "La fecha de la excepcion es obligatoria")
    private LocalDate exceptionDate;

    // Motivo del cambio — ejemplo: "Mantenimiento del aula"
    @NotBlank(message = "El motivo es obligatorio")
    private String reason;

}
