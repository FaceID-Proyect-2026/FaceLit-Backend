package com.FaceLit.backend.auth.dto.response.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.FaceLit.backend.auth.model.enums.Language;

@Getter
@AllArgsConstructor
public class UserConfigurationResponseDTO {

    private UUID idUserConfiguration;
    private UUID idUser;
    private String configurationName;
    private String description;
    private boolean notificationsActive;
    private boolean darkMode;
    private Language language;
    private OffsetDateTime updateDate;
    private String message;

    public static UserConfigurationResponseDTO created(
            UUID idUserConfiguration, UUID idUser,
            String configurationName, String description,
            boolean notificationsActive, boolean darkMode,
            Language language, OffsetDateTime updateDate) {
        return new UserConfigurationResponseDTO(
                idUserConfiguration, idUser,
                configurationName, description,
                notificationsActive, darkMode,
                language, updateDate,
                "Configuracion creada correctamente");
    }

    public static UserConfigurationResponseDTO updated(
            UUID idUserConfiguration, UUID idUser,
            String configurationName, String description,
            boolean notificationsActive, boolean darkMode,
            Language language, OffsetDateTime updateDate) {
        return new UserConfigurationResponseDTO(
                idUserConfiguration, idUser,
                configurationName, description,
                notificationsActive, darkMode,
                language, updateDate,
                "Configuracion actualizada correctamente");
    }

}
