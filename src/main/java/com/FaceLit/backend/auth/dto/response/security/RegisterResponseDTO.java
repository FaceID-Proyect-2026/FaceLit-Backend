package com.FaceLit.backend.auth.dto.response.security;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegisterResponseDTO { // Responde la respuesta solo trasnporta repuestas
    // EL Response es lo que el backend le devuelve al fronted, despues de procesar
    // el registro. (Cuando se manda un POST(Registro) al endpoint de registro, el
    // backent procesa la info,de la informacion y responde con este DTO.

    // Mensaje para el Frontend
    private String message; // texto legible que mide el resultado de la operacion.
                            // Ejemplo: "Registro exitoso" o "Se envió confirmación al correo del acudiente.

    // Estado del registro
    private String status; // "REGISTERED" → redirigir al login
                           // "PENDING_CONSENT" → mostrar pantalla "revisa el correo del acudiente"

    // ID del usuario creado durante el registro.
    private UUID id_user;

    private String temporaryPassword;

    public static RegisterResponseDTO registered(UUID idUser) {
        return new RegisterResponseDTO("Registro exitoso", "REGISTERED", idUser, null);
    }

    public static RegisterResponseDTO registered(UUID idUser, String temporaryPassword) {
        return new RegisterResponseDTO("Registro exitoso", "REGISTERED", idUser, temporaryPassword);
    }

}