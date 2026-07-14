package com.FaceLit.backend.auth.dto.request.legal;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConsentVerificationRequestDTO {

    //Token UUID que llega en el enlace del correo del acudiente
    @NotBlank(message = "El token es obligatorio")
    private String token;

}
