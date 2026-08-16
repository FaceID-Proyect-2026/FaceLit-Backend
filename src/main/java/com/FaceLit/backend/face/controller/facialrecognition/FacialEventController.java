package com.FaceLit.backend.face.controller.facialrecognition;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.face.dto.request.facialrecognition.FacialEventRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.FacialEventResponseDTO;
import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;
import com.FaceLit.backend.face.service.facialrecognition.FacialEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/facial-events")
public class FacialEventController {

    private final FacialEventService facialEventService;

    public FacialEventController(FacialEventService facialEventService) {
        this.facialEventService = facialEventService;
    }

    @PostMapping
    public ResponseEntity<FacialEventResponseDTO> create(@Valid @RequestBody FacialEventRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facialEventService.createFacialEvent(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacialEventResponseDTO> update(@PathVariable UUID id,
            @Valid @RequestBody FacialEventRequestDTO dto) {
        return ResponseEntity.ok(facialEventService.updateFacialEvent(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<FacialEventResponseDTO>> getAll() {
        return ResponseEntity.ok(facialEventService.getAllFacialEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacialEventResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(facialEventService.getFacialEventById(id));
    }

    @GetMapping("/user/{idUserApp}")
    public ResponseEntity<List<FacialEventResponseDTO>> getByUser(@PathVariable UUID idUserApp) {
        return ResponseEntity.ok(facialEventService.getFacialEventsByUser(idUserApp));
    }

    @GetMapping("/environment/{idEnvironment}")
    public ResponseEntity<List<FacialEventResponseDTO>> getByEnvironment(@PathVariable UUID idEnvironment) {
        return ResponseEntity.ok(facialEventService.getFacialEventsByEnvironment(idEnvironment));
    }

    @GetMapping("/device/{idDevice}")
    public ResponseEntity<List<FacialEventResponseDTO>> getByDevice(@PathVariable UUID idDevice) {
        return ResponseEntity.ok(facialEventService.getFacialEventsByDevice(idDevice));
    }

    @GetMapping("/type")
    public ResponseEntity<List<FacialEventResponseDTO>> getByType(@RequestParam EventType eventType) {
        return ResponseEntity.ok(facialEventService.getFacialEventsByType(eventType));
    }

    @GetMapping("/recognition-result")
    public ResponseEntity<List<FacialEventResponseDTO>> getByRecognitionResult(@RequestParam RecognitionResult recognitionResult) {
        return ResponseEntity.ok(facialEventService.getFacialEventsByRecognitionResult(recognitionResult));
    }

    @GetMapping("/send-status")
    public ResponseEntity<List<FacialEventResponseDTO>> getBySendStatus(@RequestParam SendStatus sendStatus) {
        return ResponseEntity.ok(facialEventService.getFacialEventsBySendStatus(sendStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        facialEventService.deleteFacialEvent(id);
        return ResponseEntity.noContent().build();
    }
}
