package com.FaceLit.backend.schedule.dto.response.schedule;

import com.FaceLit.backend.schedule.model.enums.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ScheduleResponseDTO {

    private UUID idSchedule;
    private String chipName;
    private String environmentName;
    private String instructorName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String message;

    public static ScheduleResponseDTO created(
            UUID idSchedule, String chipName, String environmentName,
            String instructorName, DayOfWeek dayOfWeek,
            LocalTime startTime, LocalTime endTime) {
        return new ScheduleResponseDTO(
                idSchedule, chipName, environmentName, instructorName,
                dayOfWeek, startTime, endTime,
                "ACTIVE", "Horario registrado correctamente");
    }

    public static ScheduleResponseDTO updated(
            UUID idSchedule, String chipName, String environmentName,
            String instructorName, DayOfWeek dayOfWeek,
            LocalTime startTime, LocalTime endTime) {
        return new ScheduleResponseDTO(
                idSchedule, chipName, environmentName, instructorName,
                dayOfWeek, startTime, endTime,
                "ACTIVE", "Horario actualizado correctamente");
    }

}
