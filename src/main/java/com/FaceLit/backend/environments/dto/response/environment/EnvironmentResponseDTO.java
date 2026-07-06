package com.FaceLit.backend.environments.dto.response.environment;

import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class EnvironmentResponseDTO {

    private UUID idEnvironment;
    private String environmentName;
    private Integer capacity;
    private EnvironmentStatus status;
    private String message;

    // Metodo de fabrica para registro exitoso
    public static EnvironmentResponseDTO created(
            UUID idEnvironment, String environmentName, Integer capacity, EnvironmentStatus status) {
        return new EnvironmentResponseDTO(
                idEnvironment, environmentName, capacity, status,
                "Ambiente registrado correctamente");
    }

     // Metodo de fabrica para edicion exitosa
    public static EnvironmentResponseDTO updated(
            UUID idEnvironment, String environmentName, Integer capacity, EnvironmentStatus status) {
        return new EnvironmentResponseDTO(
                idEnvironment, environmentName, capacity, status,
                "Ambiente actualizado correctamente");
        }
}
