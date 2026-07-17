package com.FaceLit.backend.schedule.service.schedule;

import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleExceptionRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleExceptionResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ScheduleExceptionService {

    // Registra una excepcion — ambiente alterno para una fecha especifica
    ScheduleExceptionResponseDTO createException(ScheduleExceptionRequestDTO dto);

    // Lista todas las excepciones de un horario
    List<ScheduleExceptionResponseDTO> getExceptionsBySchedule(UUID idSchedule);

    // Elimina logicamente la excepcion — el horario vuelve al ambiente normal
    void deleteException(UUID id);

}
