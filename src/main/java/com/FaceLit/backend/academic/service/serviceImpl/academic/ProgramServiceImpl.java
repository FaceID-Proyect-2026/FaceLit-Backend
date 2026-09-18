package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import com.FaceLit.backend.academic.dto.request.academic.ProgramRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ProgramResponseDTO;
import com.FaceLit.backend.academic.exception.AcademicException;
import com.FaceLit.backend.academic.model.academic.ChangeHistory;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.repository.ChangeHistoryRepository;
import com.FaceLit.backend.academic.repository.ChipRepository;
import com.FaceLit.backend.academic.repository.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.ProgramRepository;
import com.FaceLit.backend.academic.service.academic.ProgramService;

@Service
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final ChipRepository chipRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final ChangeHistoryRepository changeHistoryRepository;

    public ProgramServiceImpl(
            ProgramRepository programRepository,
            ChipRepository chipRepository,
            InstructorProgramRepository instructorProgramRepository,
            ChangeHistoryRepository changeHistoryRepository) {
        this.programRepository = programRepository;
        this.chipRepository = chipRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.changeHistoryRepository = changeHistoryRepository;
    }

    @Override
    @Transactional
    public ProgramResponseDTO create(ProgramRequestDTO dto) {
        String name = dto.getProgramName().trim();
        String code = dto.getProgramCode().trim();

        if (programRepository.existsByProgramNameIgnoreCase(name)) {
            throw new AcademicException("Ya existe un programa con ese nombre");
        }
        if (programRepository.existsByProgramCodeIgnoreCase(code)) {
            throw new AcademicException("Ya existe un programa con ese código");
        }

        Program program = new Program();
        program.setProgramName(name);
        program.setProgramCode(code);
        program.setState(AcademicState.ACTIVE);
        program = programRepository.save(program);
        record(program, "program", null, program.getProgramName(), ChangeAction.CREATE);
        return new ProgramResponseDTO(program);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> findAll() {
        return programRepository.findAll().stream().map(ProgramResponseDTO::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponseDTO findById(UUID idProgram) {
        return new ProgramResponseDTO(getProgram(idProgram));
    }

    @Override
    @Transactional
    public ProgramResponseDTO update(UUID idProgram, ProgramRequestDTO dto) {
        Program program = getProgram(idProgram);
        String name = dto.getProgramName().trim();
        String code = dto.getProgramCode().trim();

        if (programRepository.existsByProgramNameIgnoreCaseAndIdProgramNot(name, idProgram)) {
            throw new AcademicException("Ya existe un programa con ese nombre");
        }
        if (programRepository.existsByProgramCodeIgnoreCaseAndIdProgramNot(code, idProgram)) {
            throw new AcademicException("Ya existe un programa con ese código");
        }

        String oldValue = program.getProgramName() + " / " + program.getProgramCode();
        program.setProgramName(name);
        program.setProgramCode(code);
        program = programRepository.save(program);
        record(program, "program", oldValue, name + " / " + code, ChangeAction.UPDATE);
        return new ProgramResponseDTO(program);
    }

    @Override
    @Transactional
    public ProgramResponseDTO reactivate(UUID idProgram) {
        Program program = getProgram(idProgram);
        if (program.getState() == AcademicState.ACTIVE) {
            throw new AcademicException("El programa ya está activo");
        }
        program.setState(AcademicState.ACTIVE);
        program.setDeactivationReason(null);
        program = programRepository.save(program);
        record(program, "program", AcademicState.INACTIVE.name(), AcademicState.ACTIVE.name(), ChangeAction.REACTIVATE);
        return new ProgramResponseDTO(program);
    }

    @Override
    @Transactional
    public ProgramResponseDTO delete(UUID idProgram, String reason) {
        Program program = getProgram(idProgram);
        if (program.getState() == AcademicState.ACTIVE) {
            program.setState(AcademicState.INACTIVE);
            program.setDeactivationReason(reason == null || reason.isBlank() ? null : reason.trim());
            program = programRepository.save(program);
            record(program, "program", AcademicState.ACTIVE.name(), AcademicState.INACTIVE.name(), ChangeAction.DEACTIVATE);
            return new ProgramResponseDTO(program);
        }

        long chips = chipRepository.countByProgram_IdProgram(idProgram);
        long instructors = instructorProgramRepository.countByProgram_IdProgram(idProgram);
        if (chips > 0 || instructors > 0) {
            throw new AcademicException("No se puede eliminar el programa porque tiene fichas o instructores asociados");
        }

        record(program, "program", AcademicState.INACTIVE.name(), null, ChangeAction.DELETE);
        programRepository.delete(program);
        return new ProgramResponseDTO(program);
    }

    private Program getProgram(@NonNull UUID idProgram) {
        UUID requiredId = Objects.requireNonNull(idProgram, "idProgram");
        return programRepository.findById(requiredId)
                .orElseThrow(() -> new AcademicException("Programa no encontrado"));
    }

    private void record(Program program, String entityName, String oldValue, String newValue, ChangeAction action) {
        ChangeHistory history = new ChangeHistory();
        history.setEntityName(entityName);
        history.setEntityId(program.getIdProgram());
        history.setFieldName("program");
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setAction(action);
        changeHistoryRepository.save(history);
    }
}