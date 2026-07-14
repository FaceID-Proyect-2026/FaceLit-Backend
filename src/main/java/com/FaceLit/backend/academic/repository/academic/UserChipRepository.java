package com.FaceLit.backend.academic.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;

public interface UserChipRepository extends JpaRepository<UserChip, UUID> {

    // Verifica si el aprendiz ya tiene una asignacion ACTIVE en cualquier ficha
    // Regla de negocio: un aprendiz solo puede estar en UNA ficha activa
    boolean existsByUser_IdUserAndState(UUID idUser, UserChipStatus state);

    // Busca la asignacion activa de un usuario — para validar antes de unirse
    Optional<UserChip> findByUser_IdUserAndState(UUID idUser, UserChipStatus state);

    // Lista todos los aprendices de una ficha
    List<UserChip> findByChip_IdChip(UUID idChip);

    // Lista todas las fichas de un usuario (historial)
    List<UserChip> findByUser_IdUser(UUID idUser);

}
