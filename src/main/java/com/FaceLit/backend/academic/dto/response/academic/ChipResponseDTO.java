package com.FaceLit.backend.academic.dto.response.academic;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.ChipState;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class ChipResponseDTO {

    public static ChipResponseDTO created(
            UUID idChip, String chipCode, ChipState state,
            UUID idProgram, String programName) {
        return new ChipResponseDTO(
                idChip, chipCode, state,
                idProgram, programName, "Ficha registrada correctamente", null, null);
    }
    public static ChipResponseDTO updated(
            UUID idChip, String chipCode, ChipState state,
            UUID idProgram, String programName) {
        return new ChipResponseDTO(
                idChip, chipCode, state,
                idProgram, programName, "Ficha actualizada correctamente", null, null);
    }
    private UUID idChip;
    private String chipCode;
    private ChipState state;
    private UUID idProgram;
    private String programName;
    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
