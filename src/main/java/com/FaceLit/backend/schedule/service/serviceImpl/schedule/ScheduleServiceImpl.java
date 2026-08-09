package com.FaceLit.backend.schedule.service.serviceImpl.schedule;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.environments.model.enums.RecordEnvironmentStatus;
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

        // UUID sentinela usado para representar "sin exclusión" en las validaciones
        // de cruce de horario (creación, donde aún no existe un ID previo a excluir).
        private static final UUID NO_EXCLUSION = new UUID(0L, 0L);

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

        private String resolveInstructorName(Schedule schedule) {
                return scheduleInstructorRepository.findAll().stream()
                                .filter(si -> si.getSchedule() != null
                                                && schedule.getIdSchedule() != null
                                                && schedule.getIdSchedule().equals(si.getSchedule().getIdSchedule())
                                                && si.getStatus() == ScheduleStatus.ACTIVE)
                                .findFirst()
                                .map(si -> si.getUser().getFirstName() + " " + si.getUser().getLastName())
                                .orElse("Sin instructor");
        }

        private String resolveEnvironmentName(Schedule schedule) {
                return recordEnvironmentRepository.findAll().stream()
                                .filter(re -> re.getSchedule() != null
                                                && schedule.getIdSchedule() != null
                                                && schedule.getIdSchedule().equals(re.getSchedule().getIdSchedule())
                                                && re.getActive() == RecordEnvironmentStatus.ACTIVE)
                                .findFirst()
                                .map(re -> re.getEnvironment().getEnvironmentName())
                                .orElse("Sin ambiente");
        }

        // Agrupa las 3 entidades relacionadas — evita repetir 3 findById+orElseThrow en cada método público
        private record ScheduleContext(Chip chip, Environment environment, User instructor) {}

        private ScheduleContext loadEntities(ScheduleRequestDTO dto) {
                Chip chip = chipRepository.findById(dto.getIdChip())
                                .orElseThrow(() -> new ScheduleException("Ficha no encontrada"));
                Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                                .orElseThrow(() -> new ScheduleException("Ambiente no encontrado"));
                User instructor = userRepository.findById(dto.getIdInstructor())
                                .orElseThrow(() -> new ScheduleException("Instructor no encontrado"));
                return new ScheduleContext(chip, environment, instructor);
        }

        // Agrupa los 3 checks de conflicto — mismo excludeId para los tres
        private void validateNoConflicts(ScheduleRequestDTO dto, UUID excludeId) {
                if (scheduleRepository.existsEnvironmentConflict(
                                dto.getIdEnvironment(), dto.getDayOfWeek(),
                                dto.getStartTime(), dto.getEndTime(), excludeId)) {
                        throw new ScheduleException("El ambiente ya está asignado para esta franja");
                }
                if (scheduleRepository.existsInstructorConflict(
                                dto.getIdInstructor(), dto.getDayOfWeek(),
                                dto.getStartTime(), dto.getEndTime(), excludeId)) {
                        throw new ScheduleException("El instructor ya tiene clase en esta franja");
                }
                if (scheduleRepository.existsChipConflict(
                                dto.getIdChip(), dto.getDayOfWeek(),
                                dto.getStartTime(), dto.getEndTime(), excludeId)) {
                        throw new ScheduleException("La ficha ya tiene un horario en esta franja");
                }
        }

        // Convierte entidad a DTO reutilizable
        private ScheduleResponseDTO toDTO(Schedule schedule, String message) {
                String instructorName = resolveInstructorName(schedule);
                String environmentName = resolveEnvironmentName(schedule);

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

                ScheduleContext ctx = loadEntities(dto);
                validateNoConflicts(dto, NO_EXCLUSION);

                // 8. Crear el horario
                Schedule schedule = new Schedule();
                schedule.setChip(ctx.chip());
                schedule.setDayOfWeek(dto.getDayOfWeek());
                schedule.setStartTime(dto.getStartTime());
                schedule.setEndTime(dto.getEndTime());
                schedule.setStatus(ScheduleStatus.ACTIVE);
                schedule.setCreationDate(LocalDate.now());
                Schedule saved = scheduleRepository.save(schedule);

                // 9. Crear relacion con instructor
                ScheduleInstructor scheduleInstructor = new ScheduleInstructor();
                scheduleInstructor.setSchedule(saved);
                scheduleInstructor.setUser(ctx.instructor());
                scheduleInstructor.setStatus(ScheduleStatus.ACTIVE);
                scheduleInstructorRepository.save(scheduleInstructor);

                // 10. Crear relacion con ambiente
                RecordEnvironment recordEnvironment = new RecordEnvironment();
                recordEnvironment.setSchedule(saved);
                recordEnvironment.setEnvironment(ctx.environment());
                recordEnvironment.setAssignmentDate(OffsetDateTime.now());
                recordEnvironment.setActive(RecordEnvironmentStatus.ACTIVE);
                recordEnvironmentRepository.save(recordEnvironment);

                return ScheduleResponseDTO.created(
                                saved.getIdSchedule(),
                                saved.getChip().getChipName(),
                                ctx.environment().getEnvironmentName(),
                                ctx.instructor().getFirstName() + " " + ctx.instructor().getLastName(),
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

                ScheduleContext ctx = loadEntities(dto);
                validateNoConflicts(dto, id);

                // 4. Actualizar el horario
                schedule.setChip(ctx.chip());
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
                newInstructor.setUser(ctx.instructor());
                newInstructor.setStatus(ScheduleStatus.ACTIVE);
                scheduleInstructorRepository.save(newInstructor);

                // 6. Eliminar relacion anterior con ambiente y crear nueva
                recordEnvironmentRepository
                                .findBySchedule_IdSchedule(id)
                                .ifPresent(re -> {
                                        re.setActive(RecordEnvironmentStatus.INACTIVE);
                                        recordEnvironmentRepository.save(re);
                                });

                RecordEnvironment newEnvironment = new RecordEnvironment();
                newEnvironment.setSchedule(schedule);
                newEnvironment.setEnvironment(ctx.environment());
                newEnvironment.setAssignmentDate(OffsetDateTime.now());
                newEnvironment.setActive(RecordEnvironmentStatus.ACTIVE);
                recordEnvironmentRepository.save(newEnvironment);

                return ScheduleResponseDTO.updated(
                                schedule.getIdSchedule(),
                                ctx.chip().getChipName(),
                                ctx.environment().getEnvironmentName(),
                                ctx.instructor().getFirstName() + " " + ctx.instructor().getLastName(),
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
                                        re.setActive(RecordEnvironmentStatus.INACTIVE);
                                        recordEnvironmentRepository.save(re);
                                });

        }

        @Override
        @Transactional
        public void permanentDeleteSchedule(UUID id) {
                Schedule schedule = scheduleRepository.findById(id)
                                .orElseThrow(() -> new ScheduleException("Horario no encontrado"));

                // Solo se puede eliminar permanentemente si ya está INACTIVE
                if (schedule.getStatus() == ScheduleStatus.ACTIVE) {
                        throw new ScheduleException(
                                        "El horario debe estar inactivo antes de eliminarse permanentemente");
                }

                // Elimina primero las relaciones para evitar errores de FK
                scheduleInstructorRepository
                                .findBySchedule_IdScheduleAndStatus(id, ScheduleStatus.INACTIVE)
                                .ifPresent(si -> scheduleInstructorRepository.delete(si));

                recordEnvironmentRepository
                                .findBySchedule_IdScheduleAndActive(id, RecordEnvironmentStatus.INACTIVE)
                                .ifPresent(re -> recordEnvironmentRepository.delete(re));

                scheduleRepository.deleteById(id);
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

        @Override
        public List<ScheduleResponseDTO> getSchedulesByEnvironment(UUID idEnvironment) {
                // Verifica que el ambiente existe
                environmentRepository.findById(idEnvironment)
                                .orElseThrow(() -> new ScheduleException("Ambiente no encontrado"));

                List<Schedule> schedules = scheduleRepository.findByEnvironment(idEnvironment);

                if (schedules.isEmpty()) {
                        throw new ScheduleException(
                                        "No hay horarios disponibles para este ambiente");
                }

                return schedules.stream()
                                .map(s -> toDTO(s, null))
                                .collect(Collectors.toList());
        }

        @Override
        public List<ScheduleResponseDTO> getMySchedulesAsInstructor(UUID idUser) {
                List<Schedule> schedules = scheduleRepository.findByInstructor(idUser);

                if (schedules.isEmpty()) {
                        throw new ScheduleException(
                                        "No hay horarios disponibles para este instructor");
                }

                return schedules.stream()
                                .map(s -> toDTO(s, null))
                                .collect(Collectors.toList());
        }

        @Override
        public List<ScheduleResponseDTO> getMyScheduleAsApprentice(UUID idUser) {
                List<Schedule> schedules = scheduleRepository.findByApprentice(idUser);

                if (schedules.isEmpty()) {
                        throw new ScheduleException(
                                        "No hay horarios disponibles para esta ficha");
                }

                return schedules.stream()
                                .map(s -> toDTO(s, null))
                                .collect(Collectors.toList());
        }

        @Override
        public List<ScheduleResponseDTO> getSchedulesByUser(UUID idUser) {
                // Verifica que el usuario existe
                userRepository.findById(idUser)
                                .orElseThrow(() -> new ScheduleException("Usuario no encontrado"));

                List<Schedule> schedules = scheduleRepository.findByApprenticeId(idUser);

                if (schedules.isEmpty()) {
                        throw new ScheduleException(
                                        "No hay horarios disponibles para este usuario");
                }

                return schedules.stream()
                                .map(s -> toDTO(s, null))
                                .collect(Collectors.toList());
        }
}