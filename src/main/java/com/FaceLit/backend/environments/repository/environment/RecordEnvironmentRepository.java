package com.FaceLit.backend.environments.repository.environment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.environments.model.environment.RecordEnvironment;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordEnvironmentRepository  extends JpaRepository<RecordEnvironment, UUID> {

    // Ambiente activo de un horario
    Optional<RecordEnvironment> findBySchedule_IdSchedule(UUID idSchedule);

}
