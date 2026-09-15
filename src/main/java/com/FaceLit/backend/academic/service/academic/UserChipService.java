package com.FaceLit.backend.academic.service.academic;

import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import java.util.List;
import java.util.UUID;


public interface UserChipService {

    // El admin consulta todos los aprendices de una ficha
    List<UserChipResponseDTO> getApprenticesByChip(UUID idChip);

    // El admin desvincula un aprendiz de una ficha
    // NO elimina al aprendiz ni la ficha — solo la relacion
    void removeApprenticeFromChip(UUID idUserChip);

    // Historial de fichas de un usuario
    List<UserChipResponseDTO> getChipsByUser(UUID idUser);

    List<ChipResponseDTO> getAvailableTransferTargets(UUID idUser);

    UserChipResponseDTO transferChip(UUID idUser, UUID targetChipId);

    UserChipResponseDTO transferChip(UUID idUser, UUID targetChipId, ChangeAction action);

}
