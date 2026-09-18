package com.FaceLit.backend.academic.dto.response.academic;

import java.util.UUID;

import com.FaceLit.backend.academic.model.academic.Chip;

import lombok.Getter;

@Getter
public class ChipResponseDTO {

    private final UUID idChip;
    private final UUID idProgram;
    private final String chipCode;
    private final String state;
    private final String deactivationReason;

    public ChipResponseDTO(Chip chip) {
        this.idChip = chip.getIdChip();
        this.idProgram = chip.getProgram().getIdProgram();
        this.chipCode = chip.getChipCode();
        this.state = chip.getState().name();
        this.deactivationReason = chip.getDeactivationReason();
    }
}