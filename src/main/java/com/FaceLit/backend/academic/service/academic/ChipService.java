package com.FaceLit.backend.academic.service.academic;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.dto.request.academic.ChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.model.enums.ChipState;

public interface ChipService {
    
    // Registra una nueva ficha — el sistema genera el codigo automaticamente
    ChipResponseDTO createChip(ChipRequestDTO dto);

    // Edita una ficha existente
    ChipResponseDTO updateChip(UUID id, ChipRequestDTO dto);

    // Eliminacion logica
    void deleteChip(UUID id);

    // Lista todas las fichas
    List<ChipResponseDTO> getAllChips();

    // Consulta por ID
    ChipResponseDTO getChipById(UUID id);

    // Lista fichas de un programa especifico
    List<ChipResponseDTO> getChipsByProgram(UUID idProgram);

    // Filtra por estado
    List<ChipResponseDTO> getChipsByState(ChipState state);

}
