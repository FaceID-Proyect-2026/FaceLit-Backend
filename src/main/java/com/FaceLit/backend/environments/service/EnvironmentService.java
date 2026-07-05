package com.FaceLit.backend.environments.service;

import com.FaceLit.backend.environments.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;

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

}