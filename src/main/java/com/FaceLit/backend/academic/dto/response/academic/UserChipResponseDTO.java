package com.FaceLit.backend.academic.dto.response.academic;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserChipResponseDTO {

    public static UserChipResponseDTO joined(
            UUID idUserChip, UUID idUser, String userName,
            UUID idChip, String chipName, String chipCode,
            String programName, OffsetDateTime assignmentDate) {
        return new UserChipResponseDTO(
                idUserChip, idUser, userName,
            null, null, null,
                idChip, chipName, chipCode,
                programName, assignmentDate,
                "ACTIVE", "Código de ficha validado. Aprendiz asociado correctamente");
    }
    private UUID idUserChip;
    private UUID idUser;
    private String userName;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private UUID idChip;
    private String chipName;
    private String chipCode;
    private String programName;
    private OffsetDateTime assignmentDate;
    private String state;

    private String message;

}
