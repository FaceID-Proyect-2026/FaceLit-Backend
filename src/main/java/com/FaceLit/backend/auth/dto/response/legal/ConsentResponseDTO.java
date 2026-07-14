package com.FaceLit.backend.auth.dto.response.legal;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ConsentResponseDTO {  //  respuesta cuando el menor envía los datos del acudiente

    private String message;
    // " Pendiente " mostrar pantalla de espera
    // "Aceptado" puede ir a login
    // "Rechazado" muestra lagePage (no fue aceptado)
    private String status;
    private UUID idConsent;

    // Acudiente aún no ha respondido
    public static ConsentResponseDTO waitingGuardian(UUID idConsent) {
        return new ConsentResponseDTO("Se envió una solicitud de autorización al correo del acudiente", "PENDING",
                idConsent);

    }
    
}
