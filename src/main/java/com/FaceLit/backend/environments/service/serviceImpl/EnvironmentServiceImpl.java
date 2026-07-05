package com.FaceLit.backend.environments.service.serviceImpl;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.environments.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environments.exception.EnvironmentException;
import com.FaceLit.backend.environments.repository.EnvironmentRepository;
import com.FaceLit.backend.environments.model.Environment;
import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import com.FaceLit.backend.environments.service.EnvironmentService;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentRepository environmentRepository;

    public EnvironmentServiceImpl(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    @Transactional
    public EnvironmentResponseDTO createEnvironment(EnvironmentRequestDTO dto) {

        // 1. Verificar que no exista un ambiente con el mismo nombre
        if (environmentRepository.existsByEnvironmentName(dto.getEnvironmentName())) {
            throw new EnvironmentException("Ya existe un ambiente con ese nombre");
        }

        // 2. Construir el ambiente
        Environment environment = new Environment();
        environment.setEnvironmentName(dto.getEnvironmentName());
        environment.setCapacity(dto.getCapacity());

        // 3. Si no manda status, queda ACTIVE por defecto
        environment.setStatus(
                dto.getStatus() != null ? dto.getStatus() : EnvironmentStatus.ACTIVE);

        // 4. Guardar en BD
        Environment saved = environmentRepository.save(environment);

        // 5. Devolver la respuesta
        return EnvironmentResponseDTO.created(
                saved.getIdEnvironment(),
                saved.getEnvironmentName(),
                saved.getCapacity(),
                saved.getStatus());

    }

    @Override
    @Transactional
    public EnvironmentResponseDTO updateEnvironment(UUID id, EnvironmentRequestDTO dto) {

        // 1. Verificar que el ambiente existe
        Environment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new EnvironmentException("Ambiente no encontrado"));

        // 2. Verificar que el nuevo nombre no lo tenga otro ambiente distinto
        // Si el nombre cambió y ya existe en otro registro → rechazar
        if (!environment.getEnvironmentName().equals(dto.getEnvironmentName())
                && environmentRepository.existsByEnvironmentName(dto.getEnvironmentName())) {
            throw new EnvironmentException("Ya existe un ambiente con ese nombre");
        }

        // 3. Actualizar campos
        environment.setEnvironmentName(dto.getEnvironmentName());
        environment.setCapacity(dto.getCapacity());
        if (dto.getStatus() != null) {
            environment.setStatus(dto.getStatus());
        }

        // 4. Guardar cambios
        Environment updated = environmentRepository.save(environment);

        // 5. Retornar respuesta
        return EnvironmentResponseDTO.updated(
                updated.getIdEnvironment(),
                updated.getEnvironmentName(),
                updated.getCapacity(),
                updated.getStatus());
    }

    @Override
    public List<EnvironmentResponseDTO> getAllEnvironments() {
        return environmentRepository.findAll().stream()
                .map(env -> new EnvironmentResponseDTO(
                        env.getIdEnvironment(),
                        env.getEnvironmentName(),
                        env.getCapacity(),
                        env.getStatus(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public EnvironmentResponseDTO getEnvironmentById(UUID id) {
        Environment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new EnvironmentException("Ambiente no encontrado"));

        return new EnvironmentResponseDTO(
                environment.getIdEnvironment(),
                environment.getEnvironmentName(),
                environment.getCapacity(),
                environment.getStatus(),
                null);
    }

    @Override
    public EnvironmentResponseDTO getEnvironmentByName(String name) {

        // Busca sin importar mayúsculas o minúsculas
        // Ejemplo: "ambiente 101" encuentra "Ambiente 101"
        Environment environment = environmentRepository
                .findByEnvironmentNameIgnoreCase(name)
                .orElseThrow(() -> new EnvironmentException(
                        "No se encontró un ambiente con el nombre: " + name));

        return new EnvironmentResponseDTO(
                environment.getIdEnvironment(),
                environment.getEnvironmentName(),
                environment.getCapacity(),
                environment.getStatus(),
                null);
    }

    @Override
    public List<EnvironmentResponseDTO> getEnvironmentsByStatus(EnvironmentStatus status) {

        // Trae todos los ambientes con ese estado
        return environmentRepository.findByStatus(status).stream()
                .map(env -> new EnvironmentResponseDTO(
                        env.getIdEnvironment(),
                        env.getEnvironmentName(),
                        env.getCapacity(),
                        env.getStatus(),
                        null))
                .collect(Collectors.toList());
    }
}