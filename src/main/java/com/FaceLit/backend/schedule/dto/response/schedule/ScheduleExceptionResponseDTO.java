package com.FaceLit.backend.schedule.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ScheduleExceptionResponseDTO {

    private UUID idScheduleException;
    private UUID idSchedule;
    private String chipName;
    private String originalEnvironment;
    private String alternateEnvironment;
    private LocalDate exceptionDate;
    private String reason;
    private String status;
    private String message;

    public static ScheduleExceptionResponseDTO created(
            UUID idScheduleException, UUID idSchedule,
            String chipName, String originalEnvironment,
            String alternateEnvironment, LocalDate exceptionDate,
            String reason) {
        return new ScheduleExceptionResponseDTO(
                idScheduleException, idSchedule,
                chipName, originalEnvironment,
                alternateEnvironment, exceptionDate,
                reason, "ACTIVE",
                "Excepcion registrada correctamente");
    }

}
