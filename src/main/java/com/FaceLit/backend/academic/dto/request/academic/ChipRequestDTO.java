package com.FaceLit.backend.academic.dto.request.academic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.ChipState;

@Getter
@Setter
public class ChipRequestDTO {

    @NotBlank(message = "El código de ficha es obligatorio")
    @Pattern(regexp = "^[0-9]{7}$", message = "El código de ficha debe tener 7 dígitos")
    private String chipCode;

    @NotNull(message = "El programa es obligatorio")
    private UUID idProgram;

    private ChipState state;

}
