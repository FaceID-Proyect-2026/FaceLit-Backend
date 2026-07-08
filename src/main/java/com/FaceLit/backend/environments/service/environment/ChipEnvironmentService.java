package com.FaceLit.backend.environments.service.environment;

import java.util.List;
import java.util.UUID;
import com.FaceLit.backend.environments.dto.request.environment.ChipEnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.ChipEnvironmentResponseDTO;

public interface ChipEnvironmentService {

    // Asigna una ficha a un ambiente
    ChipEnvironmentResponseDTO assignChipToEnvironment(ChipEnvironmentRequestDTO dto);

    // Lista todas las fichas de un ambiente
    List<ChipEnvironmentResponseDTO> getChipsByEnvironment(UUID idEnvironment);

    // Lista todos los ambientes de una ficha
    List<ChipEnvironmentResponseDTO> getEnvironmentsByChip(UUID idChip);

    // Elimina la asignacion logicamente
    void removeAssignment(UUID idChipEnvironment);

}
