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

import com.FaceLit.backend.face.dto.request.facialrecognition.UserFaceRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.UserFaceResponseDTO;
import com.FaceLit.backend.face.model.enums.FaceStatus;
import com.FaceLit.backend.face.service.facialrecognition.UserFaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/user-faces")
public class UserFaceController {

    private final UserFaceService userFaceService;

    public UserFaceController(UserFaceService userFaceService) {
        this.userFaceService = userFaceService;
    }

    @PostMapping
    public ResponseEntity<UserFaceResponseDTO> create(@Valid @RequestBody UserFaceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userFaceService.createUserFace(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserFaceResponseDTO> update(@PathVariable UUID id,
            @Valid @RequestBody UserFaceRequestDTO dto) {
        return ResponseEntity.ok(userFaceService.updateUserFace(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<UserFaceResponseDTO>> getAll() {
        return ResponseEntity.ok(userFaceService.getAllUserFaces());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserFaceResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userFaceService.getUserFaceById(id));
    }

    @GetMapping("/user/{idUserApp}")
    public ResponseEntity<List<UserFaceResponseDTO>> getByUser(@PathVariable UUID idUserApp) {
        return ResponseEntity.ok(userFaceService.getUserFacesByUser(idUserApp));
    }

    @GetMapping("/status")
    public ResponseEntity<List<UserFaceResponseDTO>> getByStatus(@RequestParam FaceStatus status) {
        return ResponseEntity.ok(userFaceService.getUserFacesByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userFaceService.deleteUserFace(id);
        return ResponseEntity.noContent().build();
    }
}
