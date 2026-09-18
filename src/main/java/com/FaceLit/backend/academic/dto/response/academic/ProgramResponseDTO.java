package com.FaceLit.backend.academic.dto.response.academic;

import java.util.UUID;

import com.FaceLit.backend.academic.model.academic.Program;

import lombok.Getter;

@Getter
public class ProgramResponseDTO {

    private final UUID idProgram;
    private final String programName;
    private final String programCode;
    private final String state;
    private final String deactivationReason;

    public ProgramResponseDTO(Program program) {
        this.idProgram = program.getIdProgram();
        this.programName = program.getProgramName();
        this.programCode = program.getProgramCode();
        this.state = program.getState().name();
        this.deactivationReason = program.getDeactivationReason();
    }
}