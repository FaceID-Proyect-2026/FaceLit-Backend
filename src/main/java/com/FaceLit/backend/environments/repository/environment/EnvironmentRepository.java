package com.FaceLit.backend.environments.repository.environment;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import com.FaceLit.backend.environments.model.environment.Environment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    // Verifica si ya existe un ambiente con ese nombre — no se permiten duplicados
    boolean existsByEnvironmentName(String environmentName);

    // Busca por nombre — usado para validar duplicados al editar
    Optional<Environment> findByEnvironmentName(String environmentName);

    // Busca por nombre exacto — para consulta por nombre
    Optional<Environment> findByEnvironmentNameIgnoreCase(String environmentName);

    // Busca todos los ambientes con ese estado — para consulta por estado
    List<Environment> findByStatus(EnvironmentStatus status);
}
