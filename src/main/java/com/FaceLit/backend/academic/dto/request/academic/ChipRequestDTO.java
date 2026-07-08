package com.FaceLit.backend.academic.dto.request.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.model.enums.WorkingDay;

@Getter
@Setter
public class ChipRequestDTO {

    @NotNull(message = "El programa es obligatorio")
    private UUID idProgram;

    @NotBlank(message = "El nombre de la ficha es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String chipName;

    @NotNull(message = "La jornada es obligatoria")
    private WorkingDay workingDay;

    private ChipState state;

}
