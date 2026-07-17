package com.FaceLit.backend.schedule.service.serviceImpl.schedule;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.environments.repository.environment.EnvironmentRepository;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.environments.repository.environment.RecordEnvironmentRepository;
import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleExceptionRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleExceptionResponseDTO;
import com.FaceLit.backend.schedule.exception.ScheduleExceptionException;
import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionStatus;
import com.FaceLit.backend.schedule.model.schedule.ScheduleException;
import com.FaceLit.backend.schedule.model.schedule.Schedule;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleExceptionRepository;
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
    private final EnvironmentRepository environmentRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;

    public ScheduleExceptionServiceImpl(
            ScheduleExceptionRepository scheduleExceptionRepository,
            ScheduleRepository scheduleRepository,
            EnvironmentRepository environmentRepository,
            RecordEnvironmentRepository recordEnvironmentRepository) {
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.scheduleRepository = scheduleRepository;
        this.environmentRepository = environmentRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
    }

    private ScheduleExceptionResponseDTO toDTO(
            ScheduleException ex, String message) {

        // Obtiene el ambiente original del horario
        String originalEnvironment = recordEnvironmentRepository
                .findBySchedule_IdScheduleAndActive(
                        ex.getSchedule().getIdSchedule(), "ACTIVE")
                .map(re -> re.getEnvironment().getEnvironmentName())
                .orElse("Sin ambiente original");

        return new ScheduleExceptionResponseDTO(
                ex.getIdScheduleException(),
                ex.getSchedule().getIdSchedule(),
                ex.getSchedule().getChip().getChipName(),
                originalEnvironment,
                ex.getEnvironment().getEnvironmentName(),
                ex.getExceptionDate(),
                ex.getReason(),
                ex.getStatus().name(),
                message);
    }

    @Override
    @Transactional
    public ScheduleExceptionResponseDTO createException(
            ScheduleExceptionRequestDTO dto) {

        // 1. Verificar que el horario existe y esta activo
        Schedule schedule = scheduleRepository.findById(dto.getIdSchedule())
                .orElseThrow(() -> new ScheduleExceptionException(
                        "Horario no encontrado"));

        // 2. Verificar que el ambiente alterno existe
        Environment environment = environmentRepository
                .findById(dto.getIdEnvironment())
                .orElseThrow(() -> new ScheduleExceptionException(
                        "Ambiente no encontrado"));

        // 3. Verificar que no exista ya una excepcion en ese horario y fecha
        if (scheduleExceptionRepository
                .existsBySchedule_IdScheduleAndExceptionDate(
                        dto.getIdSchedule(), dto.getExceptionDate())) {
            throw new ScheduleExceptionException(
                    "Ya existe una excepcion registrada para este horario en esa fecha");
        }

        // 4. Verificar que el ambiente alterno sea diferente al original
        String originalEnvironmentId = recordEnvironmentRepository
                .findBySchedule_IdScheduleAndActive(
                        dto.getIdSchedule(), "ACTIVE")
                .map(re -> re.getEnvironment().getIdEnvironment().toString())
                .orElse("");

        if (originalEnvironmentId.equals(dto.getIdEnvironment().toString())) {
            throw new ScheduleExceptionException(
                    "El ambiente alterno debe ser diferente al ambiente original del horario");
        }

        // 5. Crear la excepcion
        ScheduleException exception = new ScheduleException();
        exception.setSchedule(schedule);
        exception.setEnvironment(environment);
        exception.setExceptionDate(dto.getExceptionDate());
        exception.setReason(dto.getReason());
        exception.setStatus(ScheduleExceptionStatus.ACTIVE);
        exception.setRegistrationDate(LocalDate.now());

        ScheduleException saved = scheduleExceptionRepository.save(exception);

        return ScheduleExceptionResponseDTO.created(
                saved.getIdScheduleException(),
                saved.getSchedule().getIdSchedule(),
                saved.getSchedule().getChip().getChipName(),
                recordEnvironmentRepository
                        .findBySchedule_IdScheduleAndActive(
                                dto.getIdSchedule(), "ACTIVE")
                        .map(re -> re.getEnvironment().getEnvironmentName())
                        .orElse("Sin ambiente original"),
                saved.getEnvironment().getEnvironmentName(),
                saved.getExceptionDate(),
                saved.getReason());

    }

    @Override
    public List<ScheduleExceptionResponseDTO> getExceptionsBySchedule(
            UUID idSchedule) {
        return scheduleExceptionRepository
                .findBySchedule_IdSchedule(idSchedule).stream()
                .map(ex -> toDTO(ex, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteException(UUID id) {
        ScheduleException exception = scheduleExceptionRepository
                .findById(id)
                .orElseThrow(() -> new ScheduleExceptionException(
                        "Excepcion no encontrada"));

        if (exception.getStatus() == ScheduleExceptionStatus.INACTIVE) {
            throw new ScheduleExceptionException(
                    "La excepcion ya está inactiva");
        }

        // Eliminacion logica — el horario vuelve al ambiente original
        exception.setStatus(ScheduleExceptionStatus.INACTIVE);
        scheduleExceptionRepository.save(exception);
    }

}
