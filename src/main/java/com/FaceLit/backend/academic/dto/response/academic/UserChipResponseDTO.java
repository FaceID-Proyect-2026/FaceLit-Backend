package com.FaceLit.backend.academic.dto.response.academic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserChipResponseDTO {

    private UUID idUserChip;
    private UUID idUser;
    private String userName;
    private UUID idChip;
    private String chipName;
    private String chipCode;
    private String programName;
    private LocalDate assignmentDate;
    private String state;
    private String message;

    public static UserChipResponseDTO joined(
            UUID idUserChip, UUID idUser, String userName,
            UUID idChip, String chipName, String chipCode,
            String programName, LocalDate assignmentDate) {
        return new UserChipResponseDTO(
                idUserChip, idUser, userName,
                idChip, chipName, chipCode,
                programName, assignmentDate,
                "ACTIVE", "Código de ficha validado. Aprendiz asociado correctamente");
    }

}
