package com.FaceLit.backend.schedule.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionType;

@Getter
@AllArgsConstructor
public class ScheduleExceptionResponseDTO {

    private UUID idScheduleException;
    private UUID idSchedule;
    private String chipName;
    private ScheduleExceptionType exceptionType;
    private String originalEnvironment;
    private String alternateEnvironment; // null si el tipo es INSTRUCTOR_CHANGE
    private String originalInstructor;
    private String replacementInstructor; // null si el tipo es ENVIRONMENT_CHANGE
    private LocalDate exceptionDate;
    private LocalDate endDate; // null si la excepción es de un solo día
    private String reason;
    private String status;
    private String message;

    public static ScheduleExceptionResponseDTO created(
            UUID idScheduleException, UUID idSchedule, String chipName,
            ScheduleExceptionType exceptionType,
            String originalEnvironment, String alternateEnvironment,
            String originalInstructor, String replacementInstructor,
            LocalDate exceptionDate, LocalDate endDate, String reason) {
        return new ScheduleExceptionResponseDTO(
                idScheduleException, idSchedule, chipName, exceptionType,
                originalEnvironment, alternateEnvironment,
                originalInstructor, replacementInstructor,
                exceptionDate, endDate, reason,
                "ACTIVE", "Excepcion registrada correctamente");
    }

}
