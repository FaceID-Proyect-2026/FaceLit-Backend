package com.FaceLit.backend.auth.dto.response.roleandpermission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {

    // JWT que el frontend guarda y manda en cada request posterior
    private String token;

    // Rol del usuario — el frontend lo usa para redirigir al dashboard correcto
    // Ejemplo: "ADMINISTRATOR" → dashboard admin
    private String role;

    // Lista de permisos que tiene ese rol
    // Ejemplo: ["VIEW_OWN_PROFILE", "VIEW_ATTENDANCE"]
    private List<String> permissions;

    // ID del usuario — útil para que el frontend sepa quién es
    private UUID userId;

    // Mensaje legible
    // Ejemplo: "Inicio de sesión exitoso"
    private String message;

    // Método de fábrica — igual que usas en RegisterResponseDTO
    public static LoginResponseDTO success(String token, String role, List<String> permissions, UUID userId) {
        return new LoginResponseDTO(token, role, permissions, userId, "Inicio de sesión exitoso");
    }

}
