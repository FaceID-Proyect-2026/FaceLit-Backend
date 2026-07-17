package com.FaceLit.backend.schedule.controller.schedule;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleExceptionRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleExceptionResponseDTO;
import com.FaceLit.backend.schedule.service.schedule.ScheduleExceptionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/schedule-exceptions")
public class ScheduleExceptionController {

    private final ScheduleExceptionService scheduleExceptionService;

    public ScheduleExceptionController(
            ScheduleExceptionService scheduleExceptionService) {
        this.scheduleExceptionService = scheduleExceptionService;
    }

    // POST /api/admin/schedule-exceptions
    // Registra un ambiente alterno para una fecha especifica
    @PostMapping
    public ResponseEntity<ScheduleExceptionResponseDTO> create(
            @Valid @RequestBody ScheduleExceptionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleExceptionService.createException(dto));
    }

    // GET /api/admin/schedule-exceptions/schedule/{idSchedule}
    // Lista todas las excepciones de un horario
    @GetMapping("/schedule/{idSchedule}")
    public ResponseEntity<List<ScheduleExceptionResponseDTO>> getBySchedule(
            @PathVariable UUID idSchedule) {
        return ResponseEntity.ok(
                scheduleExceptionService.getExceptionsBySchedule(idSchedule));
    }

    // DELETE /api/admin/schedule-exceptions/{id}
    // Elimina la excepcion — el horario vuelve al ambiente original
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduleExceptionService.deleteException(id);
        return ResponseEntity.noContent().build();
    }

}
