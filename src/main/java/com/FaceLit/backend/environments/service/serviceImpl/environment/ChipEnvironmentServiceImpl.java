package com.FaceLit.backend.environments.service.serviceImpl.environment;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.exception.ChipException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.environments.dto.request.environment.ChipEnvironmentRequestDTO;
import com.FaceLit.backend.environments.dto.response.environment.ChipEnvironmentResponseDTO;
import com.FaceLit.backend.environments.exception.ChipEnvironmentException;
import com.FaceLit.backend.environments.model.enums.ChipEnvironmentStatus;
import com.FaceLit.backend.environments.model.environment.ChipEnvironment;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.environments.repository.environment.ChipEnvironmentRepository;
import com.FaceLit.backend.environments.repository.environment.EnvironmentRepository;
import com.FaceLit.backend.environments.service.environment.ChipEnvironmentService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChipEnvironmentServiceImpl implements ChipEnvironmentService {

    private final ChipEnvironmentRepository chipEnvironmentRepository;
    private final ChipRepository chipRepository;
    private final EnvironmentRepository environmentRepository;

    public ChipEnvironmentServiceImpl(
            ChipEnvironmentRepository chipEnvironmentRepository,
            ChipRepository chipRepository,
            EnvironmentRepository environmentRepository) {
        this.chipEnvironmentRepository = chipEnvironmentRepository;
        this.chipRepository = chipRepository;
        this.environmentRepository = environmentRepository;
    }

    private ChipEnvironmentResponseDTO toDTO(ChipEnvironment ce, String message) {
        return new ChipEnvironmentResponseDTO(
                ce.getIdChipEnvironment(),
                ce.getChip().getIdChip(),
                ce.getChip().getChipName(),
                ce.getEnvironment().getIdEnvironment(),
                ce.getEnvironment().getEnvironmentName(),
                ce.getAssignmentDate(),
                ce.getStatus().name(),
                message);
    }

    @Override
    @Transactional
    public ChipEnvironmentResponseDTO assignChipToEnvironment(ChipEnvironmentRequestDTO dto) {

        // 1. Verificar que la ficha existe
        Chip chip = chipRepository.findById(dto.getIdChip())
                .orElseThrow(() -> new ChipException("Ficha no encontrada"));

        // 2. Verificar que el ambiente existe
        Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                .orElseThrow(() -> new ChipEnvironmentException("Ambiente no encontrado"));

        // 3. Verificar que no existe ya esa asignacion
        if (chipEnvironmentRepository.existsByChip_IdChipAndEnvironment_IdEnvironment(
                dto.getIdChip(), dto.getIdEnvironment())) {
            throw new ChipEnvironmentException(
                    "Esta ficha ya está asignada a ese ambiente");

        }

        // 4. Crear la asignacion
        ChipEnvironment assignment = new ChipEnvironment();
        assignment.setChip(chip);
        assignment.setEnvironment(environment);
        assignment.setAssignmentDate(dto.getAssignmentDate());
        assignment.setStatus(ChipEnvironmentStatus.ACTIVE);

        ChipEnvironment saved = chipEnvironmentRepository.save(assignment);

        return ChipEnvironmentResponseDTO.assigned(
                saved.getIdChipEnvironment(),
                saved.getChip().getIdChip(),
                saved.getChip().getChipName(),
                saved.getEnvironment().getIdEnvironment(),
                saved.getEnvironment().getEnvironmentName(),
                saved.getAssignmentDate());
    }

    @Override
    public List<ChipEnvironmentResponseDTO> getChipsByEnvironment(UUID idEnvironment) {
        return chipEnvironmentRepository
                .findByEnvironment_IdEnvironment(idEnvironment).stream()
                .map(ce -> toDTO(ce, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChipEnvironmentResponseDTO> getEnvironmentsByChip(UUID idChip) {
        return chipEnvironmentRepository
                .findByChip_IdChip(idChip).stream()
                .map(ce -> toDTO(ce, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeAssignment(UUID idChipEnvironment) {
        ChipEnvironment assignment = chipEnvironmentRepository
                .findById(idChipEnvironment)
                .orElseThrow(() -> new ChipEnvironmentException("Asignacion no encontrada"));

        if (assignment.getStatus() == ChipEnvironmentStatus.INACTIVE) {
            throw new ChipEnvironmentException("La asignacion ya está inactiva");
        }

        // Eliminacion logica
        assignment.setStatus(ChipEnvironmentStatus.INACTIVE);
        chipEnvironmentRepository.save(assignment);
    }

    // Agregar en ChipEnvironmentServiceImpl.java

    @Override
    @Transactional
    public void permanentDeleteAssignment(UUID idChipEnvironment) {

        ChipEnvironment assignment = chipEnvironmentRepository
                .findById(idChipEnvironment)
                .orElseThrow(() -> new ChipEnvironmentException("Asignacion no encontrada"));

        if (assignment.getStatus() == ChipEnvironmentStatus.ACTIVE) {
            throw new ChipEnvironmentException(
                    "La asignación debe estar inactiva antes de eliminarse permanentemente");
        }

        chipEnvironmentRepository.deleteById(idChipEnvironment);
    }
}
