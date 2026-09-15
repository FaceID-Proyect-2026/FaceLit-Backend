package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.dto.request.academic.ChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.exception.ChipException;
import com.FaceLit.backend.academic.exception.ProgramException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.academic.repository.academic.ProgramRepository;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.service.academic.ChipService;
import com.FaceLit.backend.academic.service.audit.ChangeHistoryService;
import com.FaceLit.backend.environments.repository.environment.ChipEnvironmentRepository;
import com.FaceLit.backend.shared.util.DeletionGuard;

import jakarta.transaction.Transactional;

@Service
public class ChipServiceImpl implements ChipService {

        private final ChipRepository chipRepository;
        private final ProgramRepository programRepository;
        private final UserChipRepository userChipRepository;
        private final ChipEnvironmentRepository chipEnvironmentRepository;
        private final ChangeHistoryService changeHistoryService;

        public ChipServiceImpl(
                        ChipRepository chipRepository,
                        ProgramRepository programRepository,
                        UserChipRepository userChipRepository,
                        ChipEnvironmentRepository chipEnvironmentRepository,
                        ChangeHistoryService changeHistoryService) {
                this.chipRepository = chipRepository;
                this.programRepository = programRepository;
                this.userChipRepository = userChipRepository;
                this.chipEnvironmentRepository = chipEnvironmentRepository;
                this.changeHistoryService = changeHistoryService;
        }

        @Override
        @Transactional
        public ChipResponseDTO createChip(ChipRequestDTO dto) {

                // 1. Verificar que el programa existe y esta activo
                Program program = programRepository.findById(dto.getIdProgram())
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

                // 2. Construir la ficha
                Chip chip = new Chip();
                chip.setProgram(program);
                chip.setState(dto.getState() != null ? dto.getState() : ChipState.ACTIVE);

                if (chipRepository.findByChipCode(dto.getChipCode()).isPresent()) {
                        throw new ChipException("Ya existe una ficha con ese código");
                }
                chip.setChipCode(dto.getChipCode());

                // 4. Guardar
                Chip saved = chipRepository.save(chip);
                changeHistoryService.record("chip", saved.getIdChip(), ChangeAction.CREATE,
                                "chip", null, saved.getChipCode(), null);

                return toDTO(saved, "Ficha registrada correctamente");
        }

        @Override
        @Transactional
        public ChipResponseDTO updateChip(UUID id, ChipRequestDTO dto) {

                // 1. Verificar que la ficha existe
                Chip chip = chipRepository.findById(id)
                                .orElseThrow(() -> new ChipException("Ficha no encontrada"));

                // 2. Verificar que el programa existe
                Program program = programRepository.findById(dto.getIdProgram())
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

                String oldCode = chip.getChipCode();
                chip.setProgram(program);
                if (!oldCode.equals(dto.getChipCode()) && chipRepository.findByChipCode(dto.getChipCode()).isPresent()) {
                        throw new ChipException("Ya existe una ficha con ese código");
                }
                chip.setChipCode(dto.getChipCode());
                if (dto.getState() != null) {
                        chip.setState(dto.getState());
                }

                Chip updated = chipRepository.save(chip);
                if (!oldCode.equals(updated.getChipCode())) {
                        changeHistoryService.record("chip", id, ChangeAction.UPDATE, "chip_code",
                                        oldCode, updated.getChipCode(), null);
                }

                return toDTO(updated, "Ficha actualizada correctamente");
        }

        @Override
        @Transactional
        public void deleteChip(UUID id) {

                Chip chip = chipRepository.findById(id)
                                .orElseThrow(() -> new ChipException("Ficha no encontrada"));

                if (chip.getState() == ChipState.INACTIVE) {
                        throw new ChipException("La ficha ya está inactiva");
                }

                // Eliminacion logica
                chip.setState(ChipState.INACTIVE);
                chipRepository.save(chip);
                changeHistoryService.record("chip", id, ChangeAction.DEACTIVATE, "state",
                                ChipState.ACTIVE.name(), ChipState.INACTIVE.name(), null);
        }

        @Override
        @Transactional
        public void reactivateChip(UUID id) {
                Chip chip = chipRepository.findById(id)
                                .orElseThrow(() -> new ChipException("Ficha no encontrada"));
                if (chip.getState() == ChipState.ACTIVE) {
                        throw new ChipException("La ficha ya está activa");
                }
                chip.setState(ChipState.ACTIVE);
                chipRepository.save(chip);
                changeHistoryService.record("chip", id, ChangeAction.REACTIVATE, "state",
                                ChipState.INACTIVE.name(), ChipState.ACTIVE.name(), null);
        }

        @Override
        public List<ChipResponseDTO> getAllChips() {
                return chipRepository.findAll().stream()
                                .map(c -> toDTO(c, null))
                                .collect(Collectors.toList());
        }

        @Override
        public ChipResponseDTO getChipById(UUID id) {
                Chip chip = chipRepository.findById(id)
                                .orElseThrow(() -> new ChipException("Ficha no encontrada"));
                return toDTO(chip, null);
        }

        @Override
        public List<ChipResponseDTO> getChipsByProgram(UUID idProgram) {
                // Verifica que el programa existe
                programRepository.findById(idProgram)
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

                return chipRepository.findByProgram_IdProgram(idProgram).stream()
                                .map(c -> toDTO(c, null))
                                .collect(Collectors.toList());
        }

        @Override
        public List<ChipResponseDTO> getChipsByState(ChipState state) {
                return chipRepository.findByState(state).stream()
                                .map(c -> toDTO(c, null))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void permanentDeleteChip(UUID id) {
                Chip chip = chipRepository.findById(id)
                                .orElseThrow(() -> new ChipException("Ficha no encontrada"));

                if (chip.getState() == ChipState.ACTIVE) {
                        throw new ChipException(
                                        "La ficha debe estar inactiva antes de eliminarse permanentemente");
                }

                // Verifica aprendices vinculados
                // DESPUÉS
                DeletionGuard.assertNoDependents(
                                userChipRepository.countByChip_IdChip(id),
                                "aprendiz",
                                "Desvincula primero los aprendices.",
                                ChipException::new);

                DeletionGuard.assertNoDependents(
                                chipEnvironmentRepository.countByChip_IdChip(id),
                                "ambiente",
                                "Elimina primero las asignaciones.",
                                ChipException::new);

                chipRepository.deleteById(id);
                changeHistoryService.record("chip", id, ChangeAction.DELETE, "chip",
                                chip.getChipCode(), null, null);
        }

        // Convierte entidad a DTO — reutilizado en todos los metodos
        private ChipResponseDTO toDTO(Chip chip, String message) {
                return new ChipResponseDTO(
                                chip.getIdChip(),
                                chip.getChipCode(),
                                chip.getState(),
                                chip.getProgram().getIdProgram(),
                                chip.getProgram().getProgramName(),
                                message, chip.getCreatedAt(), chip.getUpdatedAt());
        }

}
