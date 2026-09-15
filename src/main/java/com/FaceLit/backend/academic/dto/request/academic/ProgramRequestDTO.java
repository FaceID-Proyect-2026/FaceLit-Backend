package com.FaceLit.backend.academic.dto.request.academic;

import lombok.Setter;
import com.FaceLit.backend.academic.model.enums.ProgramState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Setter
public class ProgramRequestDTO {

     @NotBlank(message = "El nombre del programa es obligatorio")
     @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
     private String programName;

    @NotBlank(message = "El código del programa es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,15}$", message = "El código del programa no es válido")
    private String programCode;

    // Si no se manda, el ServiceImpl lo pone ACTIVE por defecto
    private ProgramState state;

}
