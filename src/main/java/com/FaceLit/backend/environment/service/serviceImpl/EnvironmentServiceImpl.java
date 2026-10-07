package com.FaceLit.backend.environment.service.serviceImpl;

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
import com.FaceLit.backend.facial.model.Device;
import com.FaceLit.backend.facial.repository.DeviceRepository;
import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.service.NotificationService;

@Service
public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final ChipRepository chipRepository;
    private final DeviceRepository deviceRepository;
    private final NotificationService notificationService;

    public EnvironmentServiceImpl(
            EnvironmentRepository environmentRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            InstructorRepository instructorRepository,
            InstructorProgramRepository instructorProgramRepository,
            ChipRepository chipRepository,
            DeviceRepository deviceRepository,
            NotificationService notificationService) {
        this.environmentRepository = environmentRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.chipRepository = chipRepository;
        this.deviceRepository = deviceRepository;
        this.notificationService = notificationService;
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
        Device device = findOrCreateDevice(dto.getDeviceCode(), environment, authenticatedUserId);
        Instructor scheduledInstructor = findInstructor(dto.getIdInstructorInCharge());
        Instructor inChargeInstructor = resolveInstructorInCharge(scheduledInstructor, authenticatedUserId);
        Chip chip = findActiveChip(dto.getIdChip());

        RecordEnvironment record = new RecordEnvironment();
        record.setEnvironment(environment);
        record.setDevice(device);
        record.setInstructorScheduled(scheduledInstructor);
        record.setInstructorInCharge(inChargeInstructor);
        record.setChip(chip);
        record.setEntryTime(dto.getEntryTime());
        record.setRegistrationMinutes(dto.getRegistrationMinutes());
        record.setExitTime(dto.getExitTime());
        record.setShutdownTime(dto.getShutdownTime());
        record.setExitReminderSent(false);
        record.setActive(true);
        record.setCreatedBy(authenticatedUserId.toString());

        record = recordEnvironmentRepository.save(record);
        notifySessionSubstitution(record, authenticatedUserId);
        return new RecordEnvironmentResponseDTO(record);
    }

    @Override
    @Transactional
    public RecordEnvironmentResponseDTO updateSession(UUID idRecordEnvironment, RecordEnvironmentRequestDTO dto, UUID authenticatedUserId) {
        validateTimes(dto);
        RecordEnvironment record = recordEnvironmentRepository.findById(idRecordEnvironment)
                .orElseThrow(() -> new EnvironmentException("Sesión no encontrada.", HttpStatus.NOT_FOUND));

        Environment environment = findActiveEnvironment(dto.getIdEnvironment());
        record.setEnvironment(environment);
        record.setDevice(findOrCreateDevice(dto.getDeviceCode(), environment, authenticatedUserId));
        Instructor scheduledInstructor = findInstructor(dto.getIdInstructorInCharge());
        record.setInstructorScheduled(scheduledInstructor);
        record.setInstructorInCharge(resolveInstructorInCharge(scheduledInstructor, authenticatedUserId));
        record.setChip(findActiveChip(dto.getIdChip()));
        record.setEntryTime(dto.getEntryTime());
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

    private Instructor resolveInstructorInCharge(Instructor scheduledInstructor, UUID authenticatedUserId) {
        Instructor authenticatedInstructor = instructorRepository.findByUser_IdUser(authenticatedUserId)
                .orElseThrow(() -> new EnvironmentException("Instructor no encontrado.", HttpStatus.NOT_FOUND));
        return authenticatedInstructor.getIdInstructor().equals(scheduledInstructor.getIdInstructor())
                ? null
                : authenticatedInstructor;
    }

    private Chip findActiveChip(UUID idChip) {
        Chip chip = chipRepository.findById(idChip)
                .orElseThrow(() -> new EnvironmentException("La ficha no está activa.", HttpStatus.BAD_REQUEST));
        if (chip.getState() != AcademicState.ACTIVE) {
            throw new EnvironmentException("La ficha no está activa.", HttpStatus.BAD_REQUEST);
        }
        return chip;
    }

    private Device findOrCreateDevice(String rawDeviceCode, Environment environment, UUID authenticatedUserId) {
        String deviceCode = rawDeviceCode == null ? "" : rawDeviceCode.trim();
        if (deviceCode.isBlank() || deviceCode.length() > 50) {
            throw new EnvironmentException("El dispositivo no es valido.", HttpStatus.BAD_REQUEST);
        }
        return deviceRepository.findByDeviceCode(deviceCode)
                .map(device -> {
                    if (!device.getEnvironment().getIdEnvironment().equals(environment.getIdEnvironment())) {
                        device.setEnvironment(environment);
                        device.setLocation(environment.getEnvironmentName());
                        device.setUpdatedBy(authenticatedUserId.toString());
                    }
                    if (!"ACTIVE".equals(device.getStatus())) {
                        device.setStatus("ACTIVE");
                        device.setUpdatedBy(authenticatedUserId.toString());
                    }
                    return deviceRepository.save(device);
                })
                .orElseGet(() -> {
                    Device device = new Device();
                    device.setEnvironment(environment);
                    device.setDeviceCode(deviceCode);
                    device.setLocation(environment.getEnvironmentName());
                    device.setStatus("ACTIVE");
                    device.setCreatedBy(authenticatedUserId.toString());
                    return deviceRepository.save(device);
                });
    }

    private void validateTimes(RecordEnvironmentRequestDTO dto) {
        if (dto.getEntryTime() == null) {
            throw new EnvironmentException("La hora de entrada es obligatoria.", HttpStatus.BAD_REQUEST);
        }
        boolean hasExit = dto.getExitTime() != null;
        boolean hasShutdown = dto.getShutdownTime() != null;
        if (hasExit != hasShutdown) {
            throw new EnvironmentException("La hora de salida y la hora de apagado deben configurarse juntas.", HttpStatus.BAD_REQUEST);
        }
        if (hasExit && !dto.getExitTime().isAfter(dto.getEntryTime())) {
            throw new EnvironmentException("La hora de salida debe ser posterior a la hora de entrada.", HttpStatus.BAD_REQUEST);
        }
        if (hasExit && !dto.getShutdownTime().isAfter(dto.getExitTime())) {
            throw new EnvironmentException("La hora de apagado debe ser posterior a la hora de salida.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeName(String environmentName) {
        return environmentName == null ? "" : environmentName.trim().replaceAll("\\s+", " ");
    }

    private String fullName(Instructor instructor) {
        return "%s %s".formatted(instructor.getUser().getFirstName(), instructor.getUser().getLastName()).trim();
    }

    private void notifySessionSubstitution(RecordEnvironment record, UUID authenticatedUserId) {
        Instructor configuredBy = instructorRepository.findByUser_IdUser(authenticatedUserId).orElse(null);
        Instructor scheduledInstructor = record.getInstructorScheduled();
        if (configuredBy == null || scheduledInstructor == null
                || configuredBy.getIdInstructor().equals(scheduledInstructor.getIdInstructor())) {
            return;
        }

        String environmentName = record.getEnvironment().getEnvironmentName();
        String chipCode = record.getChip().getChipCode();
        String metadata = "{\"entityType\":\"record_environment\",\"entityId\":\"" + record.getIdRecordEnvironment()
                + "\",\"environment\":\"" + safe(environmentName)
                + "\",\"chip\":\"" + safe(chipCode) + "\"}";

        try {
            notificationService.createForRecipient(new CreateNotificationRequestDTO(
                    scheduledInstructor.getUser().getIdUser(),
                    "facial_session_substitution",
                    "Sesion de reconocimiento asignada",
                    fullName(configuredBy) + " abrio una sesion de reconocimiento facial y te dejo a ti como instructor a cargo - Ambiente "
                            + environmentName + ", Ficha " + chipCode + ".",
                    record.getIdRecordEnvironment(),
                    "record_environment",
                    null,
                    metadata));

            notificationService.createForRecipient(new CreateNotificationRequestDTO(
                    configuredBy.getUser().getIdUser(),
                    "facial_session_substitution",
                    "Sesion de reconocimiento configurada",
                    "Configuraste la sesion de reconocimiento facial dejando a " + fullName(scheduledInstructor)
                            + " como responsable - Ambiente " + environmentName + ", Ficha " + chipCode + ".",
                    record.getIdRecordEnvironment(),
                    "record_environment",
                    null,
                    metadata));
        } catch (RuntimeException ignored) {
            // La configuracion de la sesion no debe fallar por notificaciones informativas.
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
