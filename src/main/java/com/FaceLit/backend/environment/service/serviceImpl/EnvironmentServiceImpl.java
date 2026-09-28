package com.FaceLit.backend.environment.service.serviceImpl;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.academic.repository.ChipRepository;
import com.FaceLit.backend.academic.repository.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.InstructorRepository;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.environment.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.request.RecordEnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environment.dto.response.RecordEnvironmentResponseDTO;
import com.FaceLit.backend.environment.exception.EnvironmentException;
import com.FaceLit.backend.environment.model.Environment;
import com.FaceLit.backend.environment.model.RecordEnvironment;
import com.FaceLit.backend.environment.model.enums.EnvironmentState;
import com.FaceLit.backend.environment.repository.EnvironmentRepository;
import com.FaceLit.backend.environment.repository.RecordEnvironmentRepository;
import com.FaceLit.backend.environment.service.EnvironmentService;

@Service
public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final ChipRepository chipRepository;

    public EnvironmentServiceImpl(
            EnvironmentRepository environmentRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            InstructorRepository instructorRepository,
            InstructorProgramRepository instructorProgramRepository,
            ChipRepository chipRepository) {
        this.environmentRepository = environmentRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.chipRepository = chipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentResponseDTO> searchEnvironments(String search) {
        String normalizedSearch = search == null ? "" : search.trim();
        List<Environment> environments = normalizedSearch.isBlank()
                ? environmentRepository.findByStateOrderByEnvironmentNameAsc(EnvironmentState.ACTIVE)
                : environmentRepository.searchActive(normalizedSearch, EnvironmentState.ACTIVE);
        return environments.stream().map(EnvironmentResponseDTO::new).toList();
    }

    @Override
    @Transactional
    public EnvironmentResponseDTO getOrCreateEnvironment(EnvironmentRequestDTO dto, UUID authenticatedUserId) {
        String environmentName = normalizeName(dto.getEnvironmentName());
        if (environmentName.isBlank()) {
            throw new EnvironmentException("El nombre del ambiente es obligatorio.", HttpStatus.BAD_REQUEST);
        }

        return environmentRepository.findByNormalizedName(environmentName)
                .map(environment -> {
                    if (environment.getState() != EnvironmentState.ACTIVE) {
                        environment.setState(EnvironmentState.ACTIVE);
                        environment.setUpdatedBy(authenticatedUserId.toString());
                        if (dto.getCapacity() != null) {
                            environment.setCapacity(dto.getCapacity());
                        }
                        return new EnvironmentResponseDTO(environmentRepository.save(environment));
                    }
                    return new EnvironmentResponseDTO(environment);
                })
                .orElseGet(() -> {
                    Environment environment = new Environment();
                    environment.setEnvironmentName(environmentName);
                    environment.setCapacity(dto.getCapacity());
                    environment.setState(EnvironmentState.ACTIVE);
                    environment.setCreatedBy(authenticatedUserId.toString());
                    return new EnvironmentResponseDTO(environmentRepository.save(environment));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponseDTO> findInstructorsForSession(UUID authenticatedUserId) {
        Instructor authenticatedInstructor = instructorRepository.findByUser_IdUser(authenticatedUserId)
                .orElseThrow(() -> new EnvironmentException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        List<Instructor> candidates = authenticatedInstructor.getInstructorType() == InstructorType.TRANSVERSAL
                ? instructorRepository.findAll()
                : findEligibleForSpecificInstructor(authenticatedInstructor);

        return candidates.stream()
                .filter(instructor -> instructor.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .sorted(Comparator.comparing(instructor -> fullName(instructor).toLowerCase()))
                .map(this::toInstructorResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChipResponseDTO> findChipsForInstructor(UUID idInstructor) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new EnvironmentException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        List<Chip> chips = instructor.getInstructorType() == InstructorType.TRANSVERSAL
                ? chipRepository.findByState(AcademicState.ACTIVE)
                : chipRepository.findByProgram_IdProgramIn(programIdsForInstructor(instructor)).stream()
                        .filter(chip -> chip.getState() == AcademicState.ACTIVE)
                        .toList();

        return chips.stream()
                .sorted(Comparator.comparing(Chip::getChipCode))
                .map(ChipResponseDTO::new)
                .toList();
    }

    @Override
    @Transactional
    public RecordEnvironmentResponseDTO createSession(RecordEnvironmentRequestDTO dto, UUID authenticatedUserId) {
        validateTimes(dto);
        Environment environment = findActiveEnvironment(dto.getIdEnvironment());
        Instructor instructor = findInstructor(dto.getIdInstructorInCharge());
        Chip chip = findActiveChip(dto.getIdChip());

        RecordEnvironment record = new RecordEnvironment();
        record.setEnvironment(environment);
        record.setInstructorInCharge(instructor);
        record.setChip(chip);
        record.setSessionStart(OffsetDateTime.now());
        record.setRegistrationMinutes(dto.getRegistrationMinutes());
        record.setExitTime(dto.getExitTime());
        record.setShutdownTime(dto.getShutdownTime());
        record.setExitReminderSent(false);
        record.setActive(true);
        record.setCreatedBy(authenticatedUserId.toString());

        return new RecordEnvironmentResponseDTO(recordEnvironmentRepository.save(record));
    }

    @Override
    @Transactional
    public RecordEnvironmentResponseDTO updateSession(UUID idRecordEnvironment, RecordEnvironmentRequestDTO dto, UUID authenticatedUserId) {
        validateTimes(dto);
        RecordEnvironment record = recordEnvironmentRepository.findById(idRecordEnvironment)
                .orElseThrow(() -> new EnvironmentException("Sesión no encontrada.", HttpStatus.NOT_FOUND));

        record.setEnvironment(findActiveEnvironment(dto.getIdEnvironment()));
        record.setInstructorInCharge(findInstructor(dto.getIdInstructorInCharge()));
        record.setChip(findActiveChip(dto.getIdChip()));
        record.setRegistrationMinutes(dto.getRegistrationMinutes());
        record.setExitTime(dto.getExitTime());
        record.setShutdownTime(dto.getShutdownTime());
        record.setUpdatedBy(authenticatedUserId.toString());

        return new RecordEnvironmentResponseDTO(recordEnvironmentRepository.save(record));
    }

    private List<Instructor> findEligibleForSpecificInstructor(Instructor instructor) {
        List<UUID> programIds = programIdsForInstructor(instructor);
        Map<UUID, Instructor> unique = new LinkedHashMap<>();
        instructorRepository.findAll().stream()
                .filter(candidate -> candidate.getInstructorType() == InstructorType.TRANSVERSAL
                        || instructorProgramRepository.findByInstructor_IdInstructor(candidate.getIdInstructor()).stream()
                                .map(ip -> ip.getProgram().getIdProgram())
                                .anyMatch(programIds::contains))
                .forEach(candidate -> unique.put(candidate.getIdInstructor(), candidate));
        return unique.values().stream().toList();
    }

    private List<UUID> programIdsForInstructor(Instructor instructor) {
        return instructorProgramRepository.findByInstructor_IdInstructor(instructor.getIdInstructor()).stream()
                .map(InstructorProgram::getProgram)
                .map(program -> program.getIdProgram())
                .distinct()
                .toList();
    }

    private InstructorResponseDTO toInstructorResponse(Instructor instructor) {
        return new InstructorResponseDTO(instructor,
                instructorProgramRepository.findByInstructor_IdInstructor(instructor.getIdInstructor()));
    }

    private Environment findActiveEnvironment(UUID idEnvironment) {
        Environment environment = environmentRepository.findById(idEnvironment)
                .orElseThrow(() -> new EnvironmentException("Ambiente no encontrado.", HttpStatus.NOT_FOUND));
        if (environment.getState() != EnvironmentState.ACTIVE) {
            throw new EnvironmentException("Ambiente no encontrado.", HttpStatus.NOT_FOUND);
        }
        return environment;
    }

    private Instructor findInstructor(UUID idInstructor) {
        return instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new EnvironmentException("Instructor no encontrado.", HttpStatus.NOT_FOUND));
    }

    private Chip findActiveChip(UUID idChip) {
        Chip chip = chipRepository.findById(idChip)
                .orElseThrow(() -> new EnvironmentException("La ficha no está activa.", HttpStatus.BAD_REQUEST));
        if (chip.getState() != AcademicState.ACTIVE) {
            throw new EnvironmentException("La ficha no está activa.", HttpStatus.BAD_REQUEST);
        }
        return chip;
    }

    private void validateTimes(RecordEnvironmentRequestDTO dto) {
        boolean hasExit = dto.getExitTime() != null;
        boolean hasShutdown = dto.getShutdownTime() != null;
        if (hasExit != hasShutdown || (hasExit && !dto.getShutdownTime().isAfter(dto.getExitTime()))) {
            throw new EnvironmentException("La hora de apagado debe ser posterior a la hora de salida.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeName(String environmentName) {
        return environmentName == null ? "" : environmentName.trim().replaceAll("\\s+", " ");
    }

    private String fullName(Instructor instructor) {
        return "%s %s".formatted(instructor.getUser().getFirstName(), instructor.getUser().getLastName()).trim();
    }
}
