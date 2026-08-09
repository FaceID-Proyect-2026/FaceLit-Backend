package com.FaceLit.backend.schedule.controller.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleResponseDTO;
import com.FaceLit.backend.schedule.service.schedule.ScheduleService;
import java.util.List;
import java.util.UUID;

@RestController
public class MyScheduleController {

    private final ScheduleService scheduleService;

    public MyScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // GET /api/instructor/schedules/my-schedules
    // El instructor ve los horarios donde dicta clase
    // El userId se extrae del JWT — no del body
    @GetMapping("/api/instructor/schedules/my-schedules")
    public ResponseEntity<List<ScheduleResponseDTO>> mySchedulesAsInstructor(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                scheduleService.getMySchedulesAsInstructor(userId));
    }

    // GET /api/apprentice/schedules/my-schedule
    // El aprendiz ve su horario segun la ficha activa a la que pertenece
    // El userId se extrae del JWT — no del body
    @GetMapping("/api/apprentice/schedules/my-schedule")
    public ResponseEntity<List<ScheduleResponseDTO>> myScheduleAsApprentice(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                scheduleService.getMyScheduleAsApprentice(userId));
    }

}
