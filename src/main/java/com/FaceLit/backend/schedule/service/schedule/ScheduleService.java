package com.FaceLit.backend.schedule.service.schedule;

import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    // Registra un nuevo horario con instructor y ambiente
    ScheduleResponseDTO createSchedule(ScheduleRequestDTO dto);

    // Actualiza un horario — elimina relaciones anteriores y crea nuevas
    ScheduleResponseDTO updateSchedule(UUID id, ScheduleRequestDTO dto);

    // Eliminacion logica
    void deleteSchedule(UUID id);

    // Elimina permanentemente — solo si ya está INACTIVE
    void permanentDeleteSchedule(UUID id);

    // Lista todos los horarios
    List<ScheduleResponseDTO> getAllSchedules();

    // Consulta por ID
    ScheduleResponseDTO getScheduleById(UUID id);

    // Horarios de una ficha especifica
    List<ScheduleResponseDTO> getSchedulesByChip(UUID idChip);

    // Por ambiente — admin y coordinator
    List<ScheduleResponseDTO> getSchedulesByEnvironment(UUID idEnvironment);

    // Horarios del instructor autenticado
    List<ScheduleResponseDTO> getMySchedulesAsInstructor(UUID idUser);

    // Horario del aprendiz autenticado
    List<ScheduleResponseDTO> getMyScheduleAsApprentice(UUID idUser);

    // Admin consulta horario de un aprendiz especifico por su UUID
    List<ScheduleResponseDTO> getSchedulesByUser(UUID idUser);

}
