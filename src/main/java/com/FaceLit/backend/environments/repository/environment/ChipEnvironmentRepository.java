package com.FaceLit.backend.environments.repository.environment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.FaceLit.backend.environments.model.environment.ChipEnvironment;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChipEnvironmentRepository extends JpaRepository<ChipEnvironment, UUID> {

    // Todas las asignaciones de un ambiente
    List<ChipEnvironment> findByEnvironment_IdEnvironment(UUID idEnvironment);

    // Todas las asignaciones de una ficha
    List<ChipEnvironment> findByChip_IdChip(UUID idChip);

    // Verifica si ya existe una asignacion activa entre ficha y ambiente
    boolean existsByChip_IdChipAndEnvironment_IdEnvironment(UUID idChip, UUID idEnvironment);

    // Cuenta asignaciones de una ficha a ambientes — usado en permanentDeleteChip
    long countByChip_IdChip(UUID idChip);

    // Cuenta fichas asignadas a un ambiente — usado en permanentDeleteEnvironment
    long countByEnvironment_IdEnvironment(UUID idEnvironment);

}
