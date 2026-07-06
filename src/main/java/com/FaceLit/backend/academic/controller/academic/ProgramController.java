package com.FaceLit.backend.academic.controller.academic;

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
import com.FaceLit.backend.academic.dto.request.academic.ProgramRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ProgramResponseDTO;
import com.FaceLit.backend.academic.model.enums.ProgramState;
import com.FaceLit.backend.academic.service.academic.ProgramService;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    // POST /api/admin/programs
    @PostMapping
    public ResponseEntity<ProgramResponseDTO> create(
            @Valid @RequestBody ProgramRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(programService.createProgram(dto));
    }

    // PUT /api/admin/programs/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramRequestDTO dto) {
        return ResponseEntity.ok(programService.updateProgram(id, dto));
    }

    // DELETE /api/admin/programs/{id} — eliminacion logica
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/admin/programs
    @GetMapping
    public ResponseEntity<List<ProgramResponseDTO>> getAll() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    // GET /api/admin/programs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    // GET /api/admin/programs/search?name=ADSO
    @GetMapping("/search")
    public ResponseEntity<ProgramResponseDTO> getByName(@RequestParam String name) {
        return ResponseEntity.ok(programService.getProgramByName(name));
    }

    // GET /api/admin/programs/status?state=ACTIVE
    @GetMapping("/status")
    public ResponseEntity<List<ProgramResponseDTO>> getByState(
            @RequestParam ProgramState state) {
        return ResponseEntity.ok(programService.getProgramsByState(state));
    }
}
