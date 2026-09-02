package com.FaceLit.backend.schedule.service.serviceImpl.schedule;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.environments.model.enums.RecordEnvironmentStatus;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.environments.repository.environment.EnvironmentRepository;
import com.FaceLit.backend.environments.repository.environment.RecordEnvironmentRepository;
import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleExceptionRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleExceptionResponseDTO;
import com.FaceLit.backend.schedule.exception.ScheduleExceptionException;
import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionType;
import com.FaceLit.backend.schedule.model.enums.ScheduleStatus;
import com.FaceLit.backend.schedule.model.schedule.Schedule;
import com.FaceLit.backend.schedule.model.schedule.ScheduleException;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleExceptionRepository;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleInstructorRepository;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleRepository;
import com.FaceLit.backend.schedule.service.schedule.ScheduleExceptionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScheduleExceptionServiceImpl implements ScheduleExceptionService {

        private final ScheduleExceptionRepository scheduleExceptionRepository;
        private final ScheduleRepository scheduleRepository;
        private final ScheduleInstructorRepository scheduleInstructorRepository;
        private final EnvironmentRepository environmentRepository;
        private final RecordEnvironmentRepository recordEnvironmentRepository;
        private final UserRepository userRepository;
        private final UserRoleRepository userRoleRepository;

        public ScheduleExceptionServiceImpl(
                        ScheduleExceptionRepository scheduleExceptionRepository,
                        ScheduleRepository scheduleRepository,
                        EnvironmentRepository environmentRepository,
                        ScheduleInstructorRepository scheduleInstructorRepository,
                        RecordEnvironmentRepository recordEnvironmentRepository,
                        UserRepository userRepository,
                        UserRoleRepository userRoleRepository) {
                this.scheduleExceptionRepository = scheduleExceptionRepository;
                this.scheduleRepository = scheduleRepository;
                this.scheduleInstructorRepository = scheduleInstructorRepository;
                this.environmentRepository = environmentRepository;
                this.recordEnvironmentRepository = recordEnvironmentRepository;
                this.userRepository = userRepository;
                this.userRoleRepository = userRoleRepository;
        }

        private String originalEnvironmentName(UUID idSchedule) {
                return recordEnvironmentRepository
                                .findBySchedule_IdScheduleAndActive(idSchedule, RecordEnvironmentStatus.ACTIVE)
                                .map(re -> re.getEnvironment().getEnvironmentName())
                                .orElse("Sin ambiente original");
        }

        private UUID originalEnvironmentId(UUID idSchedule) {
                return recordEnvironmentRepository
                                .findBySchedule_IdScheduleAndActive(idSchedule, RecordEnvironmentStatus.ACTIVE)
                                .map(re -> re.getEnvironment().getIdEnvironment())
                                .orElse(null);
        }

        private String originalInstructorName(UUID idSchedule) {
                return scheduleInstructorRepository
                                .findBySchedule_IdScheduleAndStatus(idSchedule, ScheduleStatus.ACTIVE)
                                .map(si -> si.getUser().getFirstName() + " " + si.getUser().getLastName())
                                .orElse("Sin instructor");
        }

        private ScheduleExceptionResponseDTO toDTO(ScheduleException ex, String message) {
                UUID idSchedule = ex.getSchedule().getIdSchedule();
                return new ScheduleExceptionResponseDTO(
                                ex.getIdScheduleException(),
                                idSchedule,
                                ex.getSchedule().getChip().getChipName(),
                                ex.getExceptionType(),
                                originalEnvironmentName(idSchedule),
                                ex.getEnvironment() != null ? ex.getEnvironment().getEnvironmentName() : null,
                                originalInstructorName(idSchedule),
                                ex.getInstructorReplacement() != null
                                                ? ex.getInstructorReplacement().getFirstName() + " "
                                                                + ex.getInstructorReplacement().getLastName()
                                                : null,
                                ex.getExceptionDate(),
                                ex.getEndDate(),
                                ex.getReason(),
                                ex.getStatus().name(),
                                message); // ← se agrega aquí
        }

        @Override
        @Transactional
        public ScheduleExceptionResponseDTO createException(ScheduleExceptionRequestDTO dto) {

                // 1. Verificar que el horario existe y está activo
                Schedule schedule = scheduleRepository.findById(dto.getIdSchedule())
                                .orElseThrow(() -> new ScheduleExceptionException("Horario no encontrado"));

                // 2. end_date no puede ser anterior a exception_date
                if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getExceptionDate())) {
                        throw new ScheduleExceptionException(
                                        "La fecha de fin no puede ser anterior a la fecha de inicio.");
                }

                // 3. No debe solaparse con otra excepción activa del mismo horario
                if (scheduleExceptionRepository.existsOverlappingException(
                                dto.getIdSchedule(), dto.getExceptionDate(), dto.getEndDate())) {
                        throw new ScheduleExceptionException(
                                        "Ya existe una excepción registrada para este horario en la fecha seleccionada.");
                }

                ScheduleException exception = new ScheduleException();
                exception.setSchedule(schedule);
                exception.setExceptionType(dto.getExceptionType());
                exception.setExceptionDate(dto.getExceptionDate());
                exception.setEndDate(dto.getEndDate());
                exception.setReason(dto.getReason());
                exception.setStatus(com.FaceLit.backend.schedule.model.enums.ScheduleExceptionStatus.ACTIVE);
                exception.setRegistrationDate(LocalDate.now());

                // 4. Validaciones específicas por tipo
                if (dto.getExceptionType() == ScheduleExceptionType.ENVIRONMENT_CHANGE) {
                        validateEnvironmentChange(dto, schedule, exception);
                } else {
                        validateInstructorChange(dto, schedule, exception);
                }

                ScheduleException saved = scheduleExceptionRepository.save(exception);

                return ScheduleExceptionResponseDTO.created(
                                saved.getIdScheduleException(),
                                schedule.getIdSchedule(),
                                schedule.getChip().getChipName(),
                                saved.getExceptionType(),
                                originalEnvironmentName(schedule.getIdSchedule()),
                                saved.getEnvironment() != null ? saved.getEnvironment().getEnvironmentName() : null,
                                originalInstructorName(schedule.getIdSchedule()),
                                saved.getInstructorReplacement() != null
                                                ? saved.getInstructorReplacement().getFirstName() + " "
                                                                + saved.getInstructorReplacement().getLastName()
                                                : null,
                                saved.getExceptionDate(),
                                saved.getEndDate(),
                                saved.getReason());
        }

        private void validateEnvironmentChange(
                        ScheduleExceptionRequestDTO dto, Schedule schedule, ScheduleException exception) {

                // Ambiente alterno obligatorio para este tipo
                if (dto.getIdEnvironment() == null) {
                        throw new ScheduleExceptionException(
                                        "Debe seleccionar un ambiente alterno para este tipo de excepción.");
                }

                Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                                .orElseThrow(() -> new ScheduleExceptionException("Ambiente no encontrado"));

                // El ambiente alterno debe ser diferente al original
                UUID originalEnvId = originalEnvironmentId(schedule.getIdSchedule());
                if (dto.getIdEnvironment().equals(originalEnvId)) {
                        throw new ScheduleExceptionException(
                                        "El ambiente alterno debe ser diferente al ambiente original del horario.");
                }

                // No debe tener conflicto de horario en esa franja
                // excludeId = el propio horario, para no comparar contra su propia franja
                if (scheduleRepository.existsEnvironmentConflict(
                                dto.getIdEnvironment(), schedule.getDayOfWeek(),
                                schedule.getStartTime(), schedule.getEndTime(), schedule.getIdSchedule())) {
                        throw new ScheduleExceptionException(
                                        "El ambiente ya está asignado para esa franja horaria.");
                }

                exception.setEnvironment(environment);
        }

        private void validateInstructorChange(
                        ScheduleExceptionRequestDTO dto, Schedule schedule, ScheduleException exception) {

                // Instructor de reemplazo obligatorio para este tipo
                if (dto.getIdInstructorReplacement() == null) {
                        throw new ScheduleExceptionException(
                                        "Debe seleccionar un instructor de reemplazo para este tipo de excepción.");
                }

                User replacement = userRepository.findById(dto.getIdInstructorReplacement())
                                .orElseThrow(() -> new ScheduleExceptionException(
                                                "El usuario seleccionado no tiene rol de Instructor."));

                // Debe tener rol INSTRUCTOR
                UserRole role = userRoleRepository.findByUserId(replacement.getIdUser())
                                .orElseThrow(() -> new ScheduleExceptionException(
                                                "El usuario seleccionado no tiene rol de Instructor."));

                if (role.getRole().getNameRole() != RoleName.INSTRUCTOR) {
                        throw new ScheduleExceptionException(
                                        "El usuario seleccionado no tiene rol de Instructor.");
                }

                // No debe tener conflicto de horario en esa franja
                if (scheduleRepository.existsInstructorConflict(
                                dto.getIdInstructorReplacement(), schedule.getDayOfWeek(),
                                schedule.getStartTime(), schedule.getEndTime(), schedule.getIdSchedule())) {
                        throw new ScheduleExceptionException(
                                        "El instructor ya tiene una asignación para esa franja horaria.");
                }

                exception.setInstructorReplacement(replacement);
        }

        @Override
        public List<ScheduleExceptionResponseDTO> getExceptionsBySchedule(UUID idSchedule) {
                return scheduleExceptionRepository
                                .findBySchedule_IdSchedule(idSchedule).stream()
                                .map(ex -> toDTO(ex, null))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void deleteException(UUID id) {
                ScheduleException exception = scheduleExceptionRepository.findById(id)
                                .orElseThrow(() -> new ScheduleExceptionException("Excepcion no encontrada"));

                if (exception.getStatus() == com.FaceLit.backend.schedule.model.enums.ScheduleExceptionStatus.INACTIVE) {
                        throw new ScheduleExceptionException("La excepcion ya está inactiva");
                }

                exception.setStatus(com.FaceLit.backend.schedule.model.enums.ScheduleExceptionStatus.INACTIVE);
                scheduleExceptionRepository.save(exception);
        }

}