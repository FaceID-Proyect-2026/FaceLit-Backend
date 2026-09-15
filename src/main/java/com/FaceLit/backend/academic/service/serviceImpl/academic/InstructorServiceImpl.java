package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.dto.request.academic.InstructorRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;
import com.FaceLit.backend.academic.exception.UserChipException;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.academic.repository.academic.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.academic.InstructorRepository;
import com.FaceLit.backend.academic.repository.academic.ProgramRepository;
import com.FaceLit.backend.academic.service.academic.InstructorService;
import com.FaceLit.backend.academic.service.audit.ChangeHistoryService;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final ChangeHistoryService changeHistoryService;

    public InstructorServiceImpl(InstructorRepository instructorRepository,
            InstructorProgramRepository instructorProgramRepository,
            ProgramRepository programRepository,
            UserRepository userRepository,
            ChangeHistoryService changeHistoryService) {
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.changeHistoryService = changeHistoryService;
    }

    @Override
    @Transactional
    public InstructorResponseDTO create(InstructorRequestDTO dto) {
        User user = userRepository.findById(dto.getIdUser())
                .orElseThrow(() -> new UserChipException("Usuario instructor no encontrado"));
        if (instructorRepository.existsByUser_IdUser(dto.getIdUser())) {
            throw new UserChipException("El usuario ya tiene una extensión de instructor");
        }
        validatePrograms(dto.getInstructorType(), dto.getProgramIds());

        Instructor instructor = new Instructor();
        instructor.setUser(user);
        instructor.setInstructorType(dto.getInstructorType());
        Instructor saved = instructorRepository.save(instructor);
        replacePrograms(saved, dto.getProgramIds());
        changeHistoryService.record("instructor", saved.getIdInstructor(), ChangeAction.CREATE,
                "instructor_type", null, dto.getInstructorType().name(), null);
        return toDTO(saved, "Instructor registrado correctamente");
    }

    @Override
    @Transactional
    public InstructorResponseDTO update(UUID idInstructor, InstructorRequestDTO dto) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new UserChipException("Instructor no encontrado"));
        validatePrograms(dto.getInstructorType(), dto.getProgramIds());
        InstructorType oldType = instructor.getInstructorType();
        String oldPrograms = programIds(instructor);
        instructor.setInstructorType(dto.getInstructorType());
        instructorRepository.save(instructor);
        replacePrograms(instructor, dto.getProgramIds());

        if (oldType != dto.getInstructorType()) {
            changeHistoryService.record("instructor", idInstructor, ChangeAction.UPDATE,
                    "instructor_type", oldType.name(), dto.getInstructorType().name(), null);
        }
        String newPrograms = dto.getInstructorType() == InstructorType.ESPECIFICO
                ? idsAsText(dto.getProgramIds()) : "";
        if (!oldPrograms.equals(newPrograms)) {
            changeHistoryService.record("instructor", idInstructor, ChangeAction.UPDATE,
                    "instructor_program", oldPrograms, newPrograms, null);
        }
        return toDTO(instructor, "Instructor actualizado correctamente");
    }

    @Override
    @Transactional
    public void delete(UUID idInstructor) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new UserChipException("Instructor no encontrado"));
        String type = instructor.getInstructorType().name();
        instructorProgramRepository.deleteByInstructor_IdInstructor(idInstructor);
        instructorRepository.delete(instructor);
        changeHistoryService.record("instructor", idInstructor, ChangeAction.DELETE,
                "instructor", type, null, null);
    }

    @Override
    public List<InstructorResponseDTO> findEligibleByProgram(UUID idProgram) {
        List<Instructor> all = instructorRepository.findAll();
        return all.stream()
                .filter(instructor -> instructor.getInstructorType() == InstructorType.TRANSVERSAL
                        || instructorProgramRepository.existsByInstructor_IdInstructorAndProgram_IdProgram(
                                instructor.getIdInstructor(), idProgram))
                .map(instructor -> toDTO(instructor, null))
                .collect(Collectors.toList());
    }

    private void validatePrograms(InstructorType type, List<UUID> programIds) {
        List<UUID> ids = programIds == null ? Collections.emptyList() : programIds.stream().distinct().toList();
        if (type == InstructorType.ESPECIFICO && ids.isEmpty()) {
            throw new UserChipException("Un instructor específico debe indicar el programa al que pertenece.");
        }
        ids.forEach(id -> programRepository.findById(id)
                .orElseThrow(() -> new UserChipException("Programa no encontrado: " + id)));
    }

    private void replacePrograms(Instructor instructor, List<UUID> programIds) {
        instructorProgramRepository.deleteByInstructor_IdInstructor(instructor.getIdInstructor());
        if (instructor.getInstructorType() != InstructorType.ESPECIFICO || programIds == null) {
            return;
        }
        programIds.stream().distinct().forEach(id -> {
            Program program = programRepository.findById(id)
                    .orElseThrow(() -> new UserChipException("Programa no encontrado: " + id));
            InstructorProgram relation = new InstructorProgram();
            relation.setInstructor(instructor);
            relation.setProgram(program);
            instructorProgramRepository.save(relation);
        });
    }

    private InstructorResponseDTO toDTO(Instructor instructor, String message) {
        return new InstructorResponseDTO(instructor.getIdInstructor(), instructor.getUser().getIdUser(),
            instructor.getUser().getFirstName(), instructor.getUser().getLastName(),
            instructor.getUser().getDocumentNumber(),
            instructor.getUser().getCredential() == null ? null : instructor.getUser().getCredential().getEmail(),
                instructor.getInstructorType(), instructorProgramRepository.findByInstructor_IdInstructor(
                        instructor.getIdInstructor()).stream()
                        .map(relation -> relation.getProgram().getIdProgram())
                        .collect(Collectors.toList()), message, instructor.getCreatedAt(), instructor.getUpdatedAt());
    }

    private String programIds(Instructor instructor) {
        return idsAsText(instructorProgramRepository.findByInstructor_IdInstructor(instructor.getIdInstructor())
                .stream().map(relation -> relation.getProgram().getIdProgram()).collect(Collectors.toList()));
    }

    private String idsAsText(List<UUID> ids) {
        return ids == null ? "" : ids.stream().distinct().map(UUID::toString).sorted().collect(Collectors.joining(","));
    }
}
