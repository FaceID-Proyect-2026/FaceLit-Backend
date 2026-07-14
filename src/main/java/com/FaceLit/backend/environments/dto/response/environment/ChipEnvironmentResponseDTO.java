package com.FaceLit.backend.environments.dto.response.environment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ChipEnvironmentResponseDTO {

    private UUID idChipEnvironment;
    private UUID idChip;
    private String chipName;
    private UUID idEnvironment;
    private String environmentName;
    private LocalDate assignmentDate;
    private String status;
    private String message;

    public static ChipEnvironmentResponseDTO assigned(
            UUID idChipEnvironment, UUID idChip, String chipName,
            UUID idEnvironment, String environmentName, LocalDate assignmentDate) {
        return new ChipEnvironmentResponseDTO(
                idChipEnvironment, idChip, chipName,
                idEnvironment, environmentName, assignmentDate,
                "ACTIVE", "Ficha asignada al ambiente correctamente");
    }

}
