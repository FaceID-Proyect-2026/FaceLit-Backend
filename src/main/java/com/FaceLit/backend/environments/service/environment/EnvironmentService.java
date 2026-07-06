package com.FaceLit.backend.environments.service.environment;

import com.FaceLit.backend.environments.dto.request.environment.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.EnvironmentResponseDTO;
import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface EnvironmentService {

    // Registra un nuevo ambiente — solo ADMINISTRATOR y COORDINATOR
    EnvironmentResponseDTO createEnvironment(EnvironmentRequestDTO dto);

    // Edita un ambiente existente — solo ADMINISTRATOR y COORDINATOR
    EnvironmentResponseDTO updateEnvironment(UUID id, EnvironmentRequestDTO dto);

    // Consulta todos los ambientes
    List<EnvironmentResponseDTO> getAllEnvironments();

    // Consulta un ambiente por ID
    EnvironmentResponseDTO getEnvironmentById(UUID id);

    // Consulta por nombre
    EnvironmentResponseDTO getEnvironmentByName(String name);

    // Consulta por estado
    List<EnvironmentResponseDTO> getEnvironmentsByStatus(EnvironmentStatus status);

    // Elimina logicamente el ambiente — cambia status a INACTIVE
    void deleteEnvironment(UUID id);

    // Listado paginado
    Page<EnvironmentResponseDTO> getAllEnvironmentsPaged(int page, int size);

}