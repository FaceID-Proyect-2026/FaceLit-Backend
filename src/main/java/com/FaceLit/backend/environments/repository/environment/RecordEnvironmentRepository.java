package com.FaceLit.backend.environments.repository.environment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.environments.model.environment.RecordEnvironment;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface RecordEnvironmentRepository extends JpaRepository<RecordEnvironment, UUID> {

    // Ambiente activo de un horario
    Optional<RecordEnvironment> findBySchedule_IdSchedule(UUID idSchedule);

    // Ahora — trae solo el activo
    Optional<RecordEnvironment> findBySchedule_IdScheduleAndActive(
            UUID idSchedule, String active);

    // Busca todos los registros de ambiente de un horario
    List<RecordEnvironment> findAllBySchedule_IdSchedule(UUID idSchedule);

}
