package com.FaceLit.backend.academic.service.academic;

import com.FaceLit.backend.academic.dto.request.academic.UserChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import java.util.List;
import java.util.UUID;


public interface UserChipService {

     // El aprendiz ingresa el codigo de ficha para unirse
    // Solo usuarios con rol APPRENTICE pueden llamar este endpoint
    UserChipResponseDTO joinChip(UUID userId, UserChipRequestDTO dto);

    // El admin consulta todos los aprendices de una ficha
    List<UserChipResponseDTO> getApprenticesByChip(UUID idChip);

    // El admin desvincula un aprendiz de una ficha
    // NO elimina al aprendiz ni la ficha — solo la relacion
    void removeApprenticeFromChip(UUID idUserChip);

    // Historial de fichas de un usuario
    List<UserChipResponseDTO> getChipsByUser(UUID idUser);

}
