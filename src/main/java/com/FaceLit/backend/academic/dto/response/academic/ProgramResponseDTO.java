package com.FaceLit.backend.academic.dto.response.academic;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.ProgramState;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgramResponseDTO {

    public static ProgramResponseDTO created(UUID id, String name, String code, ProgramState state,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ProgramResponseDTO(id, name, code, state, "Programa registrado correctamente", createdAt, updatedAt);
    }
    public static ProgramResponseDTO updated(UUID id, String name, String code, ProgramState state,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ProgramResponseDTO(id, name, code, state, "Programa actualizado correctamente", createdAt, updatedAt);
    }
    private UUID idProgram;
    private String programName;
    private String programCode;
    private ProgramState state;
    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
