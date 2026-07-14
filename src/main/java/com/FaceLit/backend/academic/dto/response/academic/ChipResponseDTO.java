package com.FaceLit.backend.academic.dto.response.academic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.model.enums.WorkingDay;

@Getter
@AllArgsConstructor

public class ChipResponseDTO {

    private UUID idChip;
    private String chipCode;
    private String chipName;
    private WorkingDay workingDay;
    private ChipState state;
    private UUID idProgram;
    private String programName;
    private String message;

    public static ChipResponseDTO created(
            UUID idChip, String chipCode, String chipName,
            WorkingDay workingDay, ChipState state,
            UUID idProgram, String programName) {
        return new ChipResponseDTO(
                idChip, chipCode, chipName, workingDay, state,
                idProgram, programName, "Ficha registrada correctamente");
    }

    public static ChipResponseDTO updated(
            UUID idChip, String chipCode, String chipName,
            WorkingDay workingDay, ChipState state,
            UUID idProgram, String programName) {
        return new ChipResponseDTO(
                idChip, chipCode, chipName, workingDay, state,
                idProgram, programName, "Ficha actualizada correctamente");
    }

}
