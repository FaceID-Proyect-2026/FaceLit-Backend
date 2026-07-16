package com.FaceLit.backend.schedule.dto.request.schedule;

import jakarta.validation.constraints.NotNull;
import com.FaceLit.backend.schedule.model.enums.DayOfWeek;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class ScheduleRequestDTO {

    @NotNull(message = "La ficha es obligatoria")
    private UUID idChip;

    @NotNull(message = "El ambiente es obligatorio")
    private UUID idEnvironment;

    @NotNull(message = "El instructor es obligatorio")
    private UUID idInstructor;

    @NotNull(message = "El día es obligatorio")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;

}
