package com.FaceLit.backend.academic.dto.request.academic;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.InstructorType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstructorRequestDTO {

    @NotNull(message = "El usuario instructor es obligatorio")
    private UUID idUser;

    @NotNull(message = "El tipo de instructor es obligatorio")
    private InstructorType instructorType;

    private List<UUID> programIds;
}
