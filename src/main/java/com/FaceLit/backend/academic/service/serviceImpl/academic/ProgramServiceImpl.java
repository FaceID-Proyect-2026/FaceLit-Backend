package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import com.FaceLit.backend.academic.dto.request.academic.ProgramRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ProgramResponseDTO;
import com.FaceLit.backend.academic.exception.ProgramException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.ProgramState;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.academic.repository.academic.ProgramRepository;
import com.FaceLit.backend.academic.service.academic.ProgramService;


import jakarta.transaction.Transactional;

@Service
public class ProgramServiceImpl implements ProgramService {

        private final ProgramRepository programRepository;
        private final ChipRepository chipRepository;

        public ProgramServiceImpl(ProgramRepository programRepository,
                        ChipRepository chipRepository) {
                this.programRepository = programRepository;
                this.chipRepository = chipRepository;
               
        }

        @Override
        @Transactional
        public ProgramResponseDTO createProgram(ProgramRequestDTO dto) {

                // 1. Verificar nombre duplicado
                if (programRepository.existsByProgramName(dto.getProgramName())) {
                        throw new ProgramException("Ya existe un programa con ese nombre");
                }

                // 2. Construir el programa
                Program program = new Program();
                program.setProgramName(dto.getProgramName());
                program.setState(dto.getState() != null ? dto.getState() : ProgramState.ACTIVE);

                // 3. Guardar
                Program saved = programRepository.save(program);

                return ProgramResponseDTO.created(
                                saved.getIdProgram(),
                                saved.getProgramName(),
                                saved.getState());
        }

        @Override
        @Transactional
        public ProgramResponseDTO updateProgram(UUID id, ProgramRequestDTO dto) {

                // 1. Verificar que existe
                Program program = programRepository.findById(id)
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

                // 2. Verificar nombre duplicado solo si cambió
                if (!program.getProgramName().equals(dto.getProgramName())
                                && programRepository.existsByProgramName(dto.getProgramName())) {
                        throw new ProgramException("Ya existe un programa con ese nombre");
                }

                // 3. Actualizar
                program.setProgramName(dto.getProgramName());
                if (dto.getState() != null) {
                        program.setState(dto.getState());
                }

                Program updated = programRepository.save(program);

                return ProgramResponseDTO.updated(
                                updated.getIdProgram(),
                                updated.getProgramName(),
                                updated.getState());
        }

        @Override
        @Transactional
        public void deleteProgram(UUID id) {

                Program program = programRepository.findById(id)
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

                if (program.getState() == ProgramState.INACTIVE) {
                        throw new ProgramException("El programa ya está inactivo");
                }

                // Eliminacion logica
                program.setState(ProgramState.INACTIVE);
                programRepository.save(program);
        }

        @Override
        public List<ProgramResponseDTO> getAllPrograms() {
                return programRepository.findAll().stream()
                                .map(p -> new ProgramResponseDTO(
                                                p.getIdProgram(), p.getProgramName(), p.getState(), null))
                                .collect(Collectors.toList());
        }

        @Override
        public ProgramResponseDTO getProgramById(UUID id) {
                Program program = programRepository.findById(id)
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));
                return new ProgramResponseDTO(
                                program.getIdProgram(), program.getProgramName(), program.getState(), null);
        }

        @Override
        public ProgramResponseDTO getProgramByName(String name) {
                Program program = programRepository.findByProgramNameIgnoreCase(name)
                                .orElseThrow(() -> new ProgramException(
                                                "No se encontró un programa con el nombre: " + name));
                return new ProgramResponseDTO(
                                program.getIdProgram(), program.getProgramName(), program.getState(), null);
        }

        @Override
        public List<ProgramResponseDTO> getProgramsByState(ProgramState state) {
                return programRepository.findByState(state).stream()
                                .map(p -> new ProgramResponseDTO(
                                                p.getIdProgram(), p.getProgramName(), p.getState(), null))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void permanentDeleteProgram(UUID id) {
                Program program = programRepository.findById(id)
                                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

                if (program.getState() == ProgramState.ACTIVE) {
                        throw new ProgramException(
                                        "El programa debe estar inactivo antes de eliminarse permanentemente");
                }

                // Verifica que no tenga fichas asociadas
                List<Chip> chips = chipRepository.findByProgram_IdProgram(id);
                if (!chips.isEmpty()) {
                        throw new ProgramException(
                                        "No se puede eliminar el programa porque tiene "
                                                        + chips.size()
                                                        + " ficha(s) asociada(s). Elimine primero las fichas.");
                }

                programRepository.deleteById(id);
        }

}
