package com.FaceLit.backend.academic.dto.response.academic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.InstructorType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstructorResponseDTO {
    private UUID idInstructor;
    private UUID idUser;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String email;
    private InstructorType instructorType;
    private List<UUID> programIds;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
