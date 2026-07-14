package com.FaceLit.backend.schedule.service.serviceImpl.schedule;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.auth.model.security.User;

import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.environments.model.environment.RecordEnvironment;
import com.FaceLit.backend.environments.repository.environment.EnvironmentRepository;
import com.FaceLit.backend.environments.repository.environment.RecordEnvironmentRepository;
import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleResponseDTO;
import com.FaceLit.backend.schedule.exception.ScheduleException;
import com.FaceLit.backend.schedule.model.enums.ScheduleStatus;
import com.FaceLit.backend.schedule.model.schedule.Schedule;
import com.FaceLit.backend.schedule.model.schedule.ScheduleInstructor;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleInstructorRepository;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleRepository;
import com.FaceLit.backend.schedule.service.schedule.ScheduleService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleInstructorRepository scheduleInstructorRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;
    private final ChipRepository chipRepository;
    private final UserRepository userRepository;
    private final EnvironmentRepository environmentRepository;

    public ScheduleServiceImpl(
            ScheduleRepository scheduleRepository,
            ScheduleInstructorRepository scheduleInstructorRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            ChipRepository chipRepository,
            UserRepository userRepository,
            EnvironmentRepository environmentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleInstructorRepository = scheduleInstructorRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.chipRepository = chipRepository;
        this.userRepository = userRepository;
        this.environmentRepository = environmentRepository;
    }

    // Convierte entidad a DTO reutilizable
    private ScheduleResponseDTO toDTO(Schedule schedule, String message) {
        // Obtener instructor activo
        String instructorName = scheduleInstructorRepository
                .findBySchedule_IdScheduleAndStatus(
                        schedule.getIdSchedule(), ScheduleStatus.ACTIVE)
                .map(si -> si.getUser().getFirstName()
                        + " " + si.getUser().getLastName())
                .orElse("Sin instructor");

        // Obtener ambiente activo
        String environmentName = recordEnvironmentRepository
                .findBySchedule_IdSchedule(schedule.getIdSchedule())
                .map(re -> re.getEnvironment().getEnvironmentName())
                .orElse("Sin ambiente");

        return new ScheduleResponseDTO(
                schedule.getIdSchedule(),
                schedule.getChip().getChipName(),
                environmentName,
                instructorName,
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getStatus().name(),
                message);
    }

    @Override
    @Transactional
    public ScheduleResponseDTO createSchedule(ScheduleRequestDTO dto) {

        // 1. Verificar que la ficha existe y está activa
        Chip chip = chipRepository.findById(dto.getIdChip())
                .orElseThrow(() -> new ScheduleException("Ficha no encontrada"));

        // 2. Verificar que el ambiente existe
        Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                .orElseThrow(() -> new ScheduleException("Ambiente no encontrado"));

        // 3. Verificar que el instructor existe
        User instructor = userRepository.findById(dto.getIdInstructor())
                .orElseThrow(() -> new ScheduleException("Instructor no encontrado"));

        // 4. UUID nulo para excluir — en creacion no hay ID previo que excluir
        UUID excludeId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // 5. Validar cruce de ambiente — no puede estar ocupado en esa franja
        if (scheduleRepository.existsEnvironmentConflict(
                dto.getIdEnvironment(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), excludeId)) {
            throw new ScheduleException(
                    "El ambiente ya está asignado para esta franja");
        }

        // 6. Validar cruce de instructor — no puede tener otra clase en esa franja
        if (scheduleRepository.existsInstructorConflict(
                dto.getIdInstructor(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), excludeId)) {
            throw new ScheduleException(
                    "El instructor ya tiene clase en esta franja");
        }

        // 7. Validar cruce de ficha — la ficha no puede tener otro horario en esa
        // franja
        if (scheduleRepository.existsChipConflict(
                dto.getIdChip(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), excludeId)) {
            throw new ScheduleException(
                    "La ficha ya tiene un horario en esta franja");
        }

        // 8. Crear el horario
        Schedule schedule = new Schedule();
        schedule.setChip(chip);
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setStatus(ScheduleStatus.ACTIVE);
        schedule.setCreationDate(LocalDate.now());
        Schedule saved = scheduleRepository.save(schedule);

        // 9. Crear relacion con instructor
        ScheduleInstructor scheduleInstructor = new ScheduleInstructor();
        scheduleInstructor.setSchedule(saved);
        scheduleInstructor.setUser(instructor);
        scheduleInstructor.setStatus(ScheduleStatus.ACTIVE);
        scheduleInstructorRepository.save(scheduleInstructor);

        // 10. Crear relacion con ambiente
        RecordEnvironment recordEnvironment = new RecordEnvironment();
        recordEnvironment.setSchedule(saved);
        recordEnvironment.setEnvironment(environment);
        recordEnvironment.setAssignmentDate(OffsetDateTime.now());
        recordEnvironment.setActive("ACTIVE");
        recordEnvironmentRepository.save(recordEnvironment);

        return ScheduleResponseDTO.created(
                saved.getIdSchedule(),
                saved.getChip().getChipName(),
                environment.getEnvironmentName(),
                instructor.getFirstName() + " " + instructor.getLastName(),
                saved.getDayOfWeek(),
                saved.getStartTime(),
                saved.getEndTime());

    }

    @Override
    @Transactional
    public ScheduleResponseDTO updateSchedule(UUID id, ScheduleRequestDTO dto) {

        // 1. Verificar que el horario existe
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ScheduleException("Horario no encontrado"));

        // 2. Verificar ficha, ambiente e instructor
        Chip chip = chipRepository.findById(dto.getIdChip())
                .orElseThrow(() -> new ScheduleException("Ficha no encontrada"));
        Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                .orElseThrow(() -> new ScheduleException("Ambiente no encontrado"));
        User instructor = userRepository.findById(dto.getIdInstructor())
                .orElseThrow(() -> new ScheduleException("Instructor no encontrado"));

        // 3. Validar cruces excluyendo el horario actual
        if (scheduleRepository.existsEnvironmentConflict(
                dto.getIdEnvironment(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), id)) {
            throw new ScheduleException(
                    "El ambiente ya está asignado para esta franja");
        }
        if (scheduleRepository.existsInstructorConflict(
                dto.getIdInstructor(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), id)) {
            throw new ScheduleException(
                    "El instructor ya tiene clase en esta franja");
        }
        if (scheduleRepository.existsChipConflict(
                dto.getIdChip(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), id)) {
            throw new ScheduleException(
                    "La ficha ya tiene un horario en esta franja");
        }

        // 4. Actualizar el horario
        schedule.setChip(chip);
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        scheduleRepository.save(schedule);

        // 5. Eliminar relacion anterior con instructor y crear nueva
        scheduleInstructorRepository
                .findBySchedule_IdScheduleAndStatus(id, ScheduleStatus.ACTIVE)
                .ifPresent(si -> {
                    si.setStatus(ScheduleStatus.INACTIVE);
                    scheduleInstructorRepository.save(si);
                });

        ScheduleInstructor newInstructor = new ScheduleInstructor();
        newInstructor.setSchedule(schedule);
        newInstructor.setUser(instructor);
        newInstructor.setStatus(ScheduleStatus.ACTIVE);
        scheduleInstructorRepository.save(newInstructor);

        // 6. Eliminar relacion anterior con ambiente y crear nueva
        recordEnvironmentRepository
                .findBySchedule_IdSchedule(id)
                .ifPresent(re -> {
                    re.setActive("INACTIVE");
                    recordEnvironmentRepository.save(re);
                });

        RecordEnvironment newEnvironment = new RecordEnvironment();
        newEnvironment.setSchedule(schedule);
        newEnvironment.setEnvironment(environment);
        newEnvironment.setAssignmentDate(OffsetDateTime.now());
        newEnvironment.setActive("ACTIVE");
        recordEnvironmentRepository.save(newEnvironment);

        return ScheduleResponseDTO.updated(
                schedule.getIdSchedule(),
                chip.getChipName(),
                environment.getEnvironmentName(),
                instructor.getFirstName() + " " + instructor.getLastName(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime());
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ScheduleException("Horario no encontrado"));

        if (schedule.getStatus() == ScheduleStatus.INACTIVE) {
            throw new ScheduleException("El horario ya está inactivo");
        }

        // Inactiva el horario y sus relaciones
        schedule.setStatus(ScheduleStatus.INACTIVE);
        scheduleRepository.save(schedule);

        scheduleInstructorRepository
                .findBySchedule_IdScheduleAndStatus(id, ScheduleStatus.ACTIVE)
                .ifPresent(si -> {
                    si.setStatus(ScheduleStatus.INACTIVE);
                    scheduleInstructorRepository.save(si);
                });

        recordEnvironmentRepository.findBySchedule_IdSchedule(id)
                .ifPresent(re -> {
                    re.setActive("INACTIVE");
                    recordEnvironmentRepository.save(re);
                });

    }

    @Override
    public List<ScheduleResponseDTO> getAllSchedules() {
        return scheduleRepository.findAll().stream()
                .map(s -> toDTO(s, null))
                .collect(Collectors.toList());
    }

    @Override
    public ScheduleResponseDTO getScheduleById(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ScheduleException("Horario no encontrado"));
        return toDTO(schedule, null);

    }

    @Override
    public List<ScheduleResponseDTO> getSchedulesByChip(UUID idChip) {
        return scheduleRepository.findByChip_IdChip(idChip).stream()
                .map(s -> toDTO(s, null))
                .collect(Collectors.toList());
    }
}
