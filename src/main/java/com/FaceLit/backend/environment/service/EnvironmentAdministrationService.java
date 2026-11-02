package com.FaceLit.backend.environment.service;

import com.FaceLit.backend.environment.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.request.EnvironmentStateRequestDTO;
import com.FaceLit.backend.environment.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environment.exception.EnvironmentException;
import com.FaceLit.backend.environment.model.Environment;
import com.FaceLit.backend.environment.model.enums.EnvironmentState;
import com.FaceLit.backend.environment.repository.EnvironmentRepository;
import com.FaceLit.backend.environment.repository.RecordEnvironmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinator-facing environment catalog operations, kept separate from instructor session setup. */
@Service
public class EnvironmentAdministrationService {
    private final EnvironmentRepository environments;
    private final RecordEnvironmentRepository sessions;

    public EnvironmentAdministrationService(
            EnvironmentRepository environments,
            RecordEnvironmentRepository sessions) {
        this.environments = environments;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponseDTO> search(String query) {
        String normalized = normalize(query);
        return environments.searchAllForAdministration(normalized.isBlank() ? null : normalized).stream()
                .map(EnvironmentResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDTO find(UUID id) {
        return new EnvironmentResponseDTO(findVisible(id));
    }

    @Transactional
    public EnvironmentResponseDTO create(EnvironmentRequestDTO request, UUID actorId) {
        String name = normalize(request.getEnvironmentName());
        validateName(name);
        if (environments.findByNormalizedName(name).isPresent()) {
            throw new EnvironmentException("Ya existe un ambiente con ese nombre.", HttpStatus.CONFLICT);
        }

        Environment environment = new Environment();
        environment.setEnvironmentName(name);
        environment.setCapacity(request.getCapacity());
        environment.setState(EnvironmentState.ACTIVE);
        environment.setCreatedBy(actorId.toString());
        return new EnvironmentResponseDTO(environments.save(environment));
    }

    @Transactional
    public EnvironmentResponseDTO update(UUID id, EnvironmentRequestDTO request, UUID actorId) {
        Environment environment = findVisible(id);
        String name = normalize(request.getEnvironmentName());
        validateName(name);

        environments.findByNormalizedName(name)
                .filter(existing -> !existing.getIdEnvironment().equals(id))
                .ifPresent(existing -> {
                    throw new EnvironmentException("Ya existe un ambiente con ese nombre.", HttpStatus.CONFLICT);
                });

        if (!environment.getEnvironmentName().equals(name)
                && sessions.existsByEnvironment_IdEnvironmentAndDeletedAtIsNull(id)) {
            throw new EnvironmentException(
                    "No se puede cambiar el nombre porque el ambiente ya tiene sesiones asociadas.",
                    HttpStatus.CONFLICT);
        }

        environment.setEnvironmentName(name);
        environment.setCapacity(request.getCapacity());
        environment.setUpdatedBy(actorId.toString());
        return new EnvironmentResponseDTO(environments.save(environment));
    }

    @Transactional
    public EnvironmentResponseDTO changeState(UUID id, EnvironmentState nextState, UUID actorId) {
        Environment environment = findVisible(id);
        if (nextState == EnvironmentState.INACTIVE
                && sessions.existsByEnvironment_IdEnvironmentAndActiveTrueAndDeletedAtIsNullAndShutdownTimeAfter(
                        id, OffsetDateTime.now())) {
            throw new EnvironmentException(
                    "Cierra o espera a que terminen las sesiones activas o programadas antes de desactivar el ambiente.",
                    HttpStatus.CONFLICT);
        }
        environment.setState(nextState);
        environment.setUpdatedBy(actorId.toString());
        return new EnvironmentResponseDTO(environments.save(environment));
    }

    private Environment findVisible(UUID id) {
        Environment environment = environments.findById(id)
                .orElseThrow(() -> new EnvironmentException("Ambiente no encontrado.", HttpStatus.NOT_FOUND));
        if (environment.getDeletedAt() != null) {
            throw new EnvironmentException("Ambiente no encontrado.", HttpStatus.NOT_FOUND);
        }
        return environment;
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new EnvironmentException("El nombre del ambiente es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (name.length() > 100) {
            throw new EnvironmentException("El nombre del ambiente no puede superar 100 caracteres.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
