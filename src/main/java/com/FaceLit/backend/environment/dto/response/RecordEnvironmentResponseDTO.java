package com.FaceLit.backend.environment.dto.response;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.FaceLit.backend.environment.model.RecordEnvironment;

import lombok.Getter;

@Getter
public class RecordEnvironmentResponseDTO {

    private final UUID idRecordEnvironment;
    private final UUID idEnvironment;
    private final String environmentName;
    private final UUID idChip;
    private final String chipCode;
    private final UUID idInstructorInCharge;
    private final String instructorName;
    private final OffsetDateTime sessionStart;
    private final Integer registrationMinutes;
    private final LocalTime exitTime;
    private final LocalTime shutdownTime;
    private final Boolean active;
    private final String createdBy;
    private final String updatedBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public RecordEnvironmentResponseDTO(RecordEnvironment record) {
        this.idRecordEnvironment = record.getIdRecordEnvironment();
        this.idEnvironment = record.getEnvironment().getIdEnvironment();
        this.environmentName = record.getEnvironment().getEnvironmentName();
        this.idChip = record.getChip().getIdChip();
        this.chipCode = record.getChip().getChipCode();
        this.idInstructorInCharge = record.getInstructorInCharge().getIdInstructor();
        this.instructorName = "%s %s".formatted(
                record.getInstructorInCharge().getUser().getFirstName(),
                record.getInstructorInCharge().getUser().getLastName()).trim();
        this.sessionStart = record.getCreatedAt();
        this.registrationMinutes = record.getRegistrationMinutes();
        this.exitTime = record.getExitTime();
        this.shutdownTime = record.getShutdownTime();
        this.active = record.getActive();
        this.createdBy = record.getCreatedBy();
        this.updatedBy = record.getUpdatedBy();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
    }
}
