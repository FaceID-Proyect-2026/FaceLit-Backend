package com.FaceLit.backend.schedule.dto.request.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionType;

@Getter
@Setter
public class ScheduleExceptionRequestDTO {

    // Horario al que pertenece la excepcion
    @NotNull(message = "El horario es obligatorio")
    private UUID idSchedule;

    @NotNull(message = "El tipo de excepción es obligatorio")
    private ScheduleExceptionType exceptionType;

    // Obligatorio solo si exceptionType = ENVIRONMENT_CHANGE
    // (la validación condicional vive en el service — un @NotNull fijo aquí
    // rechazaría por error las solicitudes válidas de INSTRUCTOR_CHANGE)
    // Ambiente alterno que se usara ese dia
    private UUID idEnvironment;

    // Obligatorio solo si exceptionType = INSTRUCTOR_CHANGE
    private UUID idInstructorReplacement;

    // Fecha en la que ocurre la excepcion
    @NotNull(message = "La fecha de la excepcion es obligatoria")
    private LocalDate exceptionDate;

    // Opcional — si no se manda, la excepción aplica solo a exceptionDate
    private LocalDate endDate;

    // Motivo del cambio — ejemplo: "Mantenimiento del aula"
    @NotBlank(message = "El motivo es obligatorio")
    private String reason;

}
