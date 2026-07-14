package com.FaceLit.backend.academic.dto.response.academic;

import com.FaceLit.backend.academic.model.enums.ProgramState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProgramResponseDTO {

    private UUID idProgram;
    private String programName;
    private ProgramState state;
    private String message;

    public static ProgramResponseDTO created(UUID id, String name, ProgramState state) {
        return new ProgramResponseDTO(id, name, state, "Programa registrado correctamente");
    }

    public static ProgramResponseDTO updated(UUID id, String name, ProgramState state) {
        return new ProgramResponseDTO(id, name, state, "Programa actualizado correctamente");
    }

}
