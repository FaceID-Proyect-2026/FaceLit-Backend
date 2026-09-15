package com.FaceLit.backend.environments.service.environment;

import com.FaceLit.backend.environments.dto.request.environment.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.EnvironmentResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface EnvironmentService {

    // Registra un nuevo ambiente — solo COORDINATOR
    EnvironmentResponseDTO createEnvironment(EnvironmentRequestDTO dto);

    // Edita un ambiente existente — solo COORDINATOR
    EnvironmentResponseDTO updateEnvironment(UUID id, EnvironmentRequestDTO dto);

    // Consulta todos los ambientes
    List<EnvironmentResponseDTO> getAllEnvironments();

    // Consulta un ambiente por ID
    EnvironmentResponseDTO getEnvironmentById(UUID id);

    // Consulta por nombre
    EnvironmentResponseDTO getEnvironmentByName(String name);

    // Elimina el ambiente
    void deleteEnvironment(UUID id);

    // Listado paginado
    Page<EnvironmentResponseDTO> getAllEnvironmentsPaged(int page, int size);

    // Elimina permanentemente — solo si ya está INACTIVE
    void permanentDeleteEnvironment(UUID id);

}