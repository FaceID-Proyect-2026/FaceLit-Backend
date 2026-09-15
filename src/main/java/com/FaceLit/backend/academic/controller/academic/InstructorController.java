package com.FaceLit.backend.academic.controller.academic;

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
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.academic.dto.request.academic.InstructorRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;
import com.FaceLit.backend.academic.service.academic.InstructorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/coordinator/instructors")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping
    public ResponseEntity<InstructorResponseDTO> create(@Valid @RequestBody InstructorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instructorService.create(dto));
    }

    @PutMapping("/{idInstructor}")
    public ResponseEntity<InstructorResponseDTO> update(
            @PathVariable UUID idInstructor, @Valid @RequestBody InstructorRequestDTO dto) {
        return ResponseEntity.ok(instructorService.update(idInstructor, dto));
    }

    @DeleteMapping("/{idInstructor}")
    public ResponseEntity<Void> delete(@PathVariable UUID idInstructor) {
        instructorService.delete(idInstructor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eligible/program/{idProgram}")
    public ResponseEntity<List<InstructorResponseDTO>> findEligibleByProgram(@PathVariable UUID idProgram) {
        return ResponseEntity.ok(instructorService.findEligibleByProgram(idProgram));
    }
}
