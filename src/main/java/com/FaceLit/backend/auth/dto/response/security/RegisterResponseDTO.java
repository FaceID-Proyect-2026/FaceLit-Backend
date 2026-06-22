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

    // Si Requiere Consentimiento
    private boolean requiresConsent; // false ( el usuario es Mayor de edad)-- se registra en el login
                                     // True ( El usuario es menor de edad) - se espera la configuracion del
                                     // acudiente.

    // ID del usuario solo si es mayor de edad)
    private UUID id_user; // El ID que se genera en el Usuario,
                          // recordar que solo tiene valor si es Mayor de edad, si es menor (NULL) por que
                          // su registro no esa completo.

    // Crea la respuesta automatica para un mayor de edad
    public static RegisterResponseDTO majorAge(UUID id_user) { // recibe el UUID del usuario
        return new RegisterResponseDTO(null, "REGISTERED", false, id_user);
        // crea un nuevo objeto, contruye una nueva respuesta DTO // Crea la respuesta
        // automatica para un mayor de edad
    }

    // Usuario menor de edad
    public static RegisterResponseDTO minorAge() {
        return new RegisterResponseDTO(null, "PENDING_CONSENT", true, null);
    }

    //El menor y mayor de edad deben verificar el email antes de continuar con el registro
                                         // recibimos el id del  usuario para
    public static RegisterResponseDTO pendingEmailVerification(UUID id_user) {
        return new RegisterResponseDTO("Se envió un código de verificación a tu correo electrónico", "PENDING_EMAIL_VERIFICATION", false, id_user); 
    }

}