package com.FaceLit.backend.academic.service.academic;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.dto.request.academic.ProgramRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ProgramResponseDTO;
import com.FaceLit.backend.academic.model.enums.ProgramState;

public interface ProgramService {

    // Registra un nuevo programa
    ProgramResponseDTO createProgram(ProgramRequestDTO dto);

    // Edita un programa existente
    ProgramResponseDTO updateProgram(UUID id, ProgramRequestDTO dto);

    // Eliminacion logica — cambia state a INACTIVE
    void deleteProgram(UUID id);

    // Lista todos los programas
    List<ProgramResponseDTO> getAllPrograms();

    // Consulta por ID
    ProgramResponseDTO getProgramById(UUID id);

    // Consulta por nombre
    ProgramResponseDTO getProgramByName(String name);

    // Consulta por estado
    List<ProgramResponseDTO> getProgramsByState(ProgramState state);

    void permanentDeleteProgram(UUID id);

}
