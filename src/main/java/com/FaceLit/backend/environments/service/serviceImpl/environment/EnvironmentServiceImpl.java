package com.FaceLit.backend.environments.service.serviceImpl.environment;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.environments.dto.request.environment.EnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.EnvironmentResponseDTO;
import com.FaceLit.backend.environments.exception.EnvironmentException;
import com.FaceLit.backend.environments.model.environment.ChipEnvironment;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.environments.model.environment.RecordEnvironment;
import com.FaceLit.backend.environments.repository.environment.ChipEnvironmentRepository;
import com.FaceLit.backend.environments.repository.environment.EnvironmentRepository;
import com.FaceLit.backend.environments.repository.environment.RecordEnvironmentRepository;
import com.FaceLit.backend.environments.service.environment.EnvironmentService;
import com.FaceLit.backend.shared.util.DeletionGuard;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EnvironmentServiceImpl implements EnvironmentService {

        private final EnvironmentRepository environmentRepository;
        private final ChipEnvironmentRepository chipEnvironmentRepository;
        private final RecordEnvironmentRepository recordEnvironmentRepository;

        public EnvironmentServiceImpl(EnvironmentRepository environmentRepository,
                        ChipEnvironmentRepository chipEnvironmentRepository,
                        RecordEnvironmentRepository recordEnvironmentRepository) {
                this.environmentRepository = environmentRepository;
                this.chipEnvironmentRepository = chipEnvironmentRepository;
                this.recordEnvironmentRepository = recordEnvironmentRepository;
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

                // 4. Guardar en BD
                Environment saved = environmentRepository.save(environment);

                // 5. Devolver la respuesta
                return EnvironmentResponseDTO.created(
                                saved.getIdEnvironment(),
                                saved.getEnvironmentName());

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
                // 4. Guardar cambios
                Environment updated = environmentRepository.save(environment);

                // 5. Retornar respuesta
                return EnvironmentResponseDTO.updated(
                                updated.getIdEnvironment(),
                                updated.getEnvironmentName());
        }

        @Override
        public List<EnvironmentResponseDTO> getAllEnvironments() {
                return environmentRepository.findAll().stream()
                                .map(env -> new EnvironmentResponseDTO(
                                                env.getIdEnvironment(),
                                                env.getEnvironmentName(),
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
                                null);
        }

        @Override
        @Transactional
        public void deleteEnvironment(UUID id) {

                // 1. Verificar que el ambiente existe
                Environment environment = environmentRepository.findById(id)
                                .orElseThrow(() -> new EnvironmentException("Ambiente no encontrado"));

                environmentRepository.delete(environment);

        }

        @Override
        public Page<EnvironmentResponseDTO> getAllEnvironmentsPaged(int page, int size) {

                // Pageable — page empieza en 0, size es cuantos por pagina
                Pageable pageable = PageRequest.of(page, size, Sort.by("environmentName").ascending());

                return environmentRepository.findAll(pageable)
                                .map(env -> new EnvironmentResponseDTO(
                                                env.getIdEnvironment(),
                                                env.getEnvironmentName(),

                                                null));
        }

        @Override
        @Transactional
        public void permanentDeleteEnvironment(UUID id) {
                Environment environment = environmentRepository.findById(id)
                                .orElseThrow(() -> new EnvironmentException("Ambiente no encontrado"));

                // Verifica fichas asignadas a este ambiente
                DeletionGuard.assertNoDependents(
                                chipEnvironmentRepository.countByEnvironment_IdEnvironment(id),
                                "ficha",
                                "Elimina primero las asignaciones.",
                                EnvironmentException::new);

                DeletionGuard.assertNoDependents(
                                recordEnvironmentRepository.countAllByEnvironment_IdEnvironment(id),
                                "sesion de reconocimiento",
                                "Elimina primero las sesiones de reconocimiento.",
                                EnvironmentException::new);

                environmentRepository.deleteById(id);
        }
}
