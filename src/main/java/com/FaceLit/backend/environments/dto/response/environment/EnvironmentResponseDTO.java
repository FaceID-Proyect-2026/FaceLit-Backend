package com.FaceLit.backend.environments.dto.response.environment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class EnvironmentResponseDTO {

    private UUID idEnvironment;
    private String environmentName;
    private String message;

    // Metodo de fabrica para registro exitoso
    public static EnvironmentResponseDTO created(
                        UUID idEnvironment, String environmentName) {
        return new EnvironmentResponseDTO(
                                                                idEnvironment, environmentName,
                "Ambiente registrado correctamente");
    }

     // Metodo de fabrica para edicion exitosa
    public static EnvironmentResponseDTO updated(
                        UUID idEnvironment, String environmentName) {
        return new EnvironmentResponseDTO(
                                                                idEnvironment, environmentName,
                "Ambiente actualizado correctamente");
        }
}
