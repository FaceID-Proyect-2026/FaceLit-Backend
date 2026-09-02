package com.FaceLit.backend.auth.dto.request.security;

import com.FaceLit.backend.auth.model.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserConfigurationRequestDTO {

    @NotBlank(message = "El nombre de la configuracion es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String configurationName;

    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
    private String description;

    // true = activadas, false = desactivadas
    private boolean notificationsActive = true;

    // true = modo oscuro, false = modo claro
    private boolean darkMode = false;

    // Idioma: ES, EN, DE, PA o FR
    @NotNull(message = "El idioma es obligatorio")
    private Language language;

}
