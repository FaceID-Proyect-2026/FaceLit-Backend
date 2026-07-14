package com.FaceLit.backend.schedule.controller.schedule;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.schedule.dto.request.schedule.ScheduleRequestDTO;
import com.FaceLit.backend.schedule.dto.response.schedule.ScheduleResponseDTO;
import com.FaceLit.backend.schedule.service.schedule.ScheduleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // POST /api/admin/schedules
    @PostMapping
    public ResponseEntity<ScheduleResponseDTO> create(
            @Valid @RequestBody ScheduleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createSchedule(dto));
    }

    // PUT /api/admin/schedules/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleRequestDTO dto) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, dto));
    }

    // DELETE /api/admin/schedules/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/admin/schedules
    @GetMapping
    public ResponseEntity<List<ScheduleResponseDTO>> getAll() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    // GET /api/admin/schedules/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    // GET /api/admin/schedules/chip/{idChip}
    @GetMapping("/chip/{idChip}")
    public ResponseEntity<List<ScheduleResponseDTO>> getByChip(
            @PathVariable UUID idChip) {
        return ResponseEntity.ok(scheduleService.getSchedulesByChip(idChip));
    }

    // GET /api/admin/schedules/environment/{idEnvironment}
    // Admin y Coordinator ven horarios de un ambiente especifico
    @GetMapping("/environment/{idEnvironment}")
    public ResponseEntity<List<ScheduleResponseDTO>> getByEnvironment(
            @PathVariable UUID idEnvironment) {
        return ResponseEntity.ok(
                scheduleService.getSchedulesByEnvironment(idEnvironment));
    }

}
