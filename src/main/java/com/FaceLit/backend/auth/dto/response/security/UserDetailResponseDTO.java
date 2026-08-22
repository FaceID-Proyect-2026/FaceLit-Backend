package com.FaceLit.backend.auth.dto.response.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDetailResponseDTO {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String documentType;
    private LocalDate birthDate;
    private String email;
    private String role;
    private String accountStatus;
    private LocalDateTime registrationDate; // nuevo — pedido en el detalle

    // Solo aplica si el rol es APPRENTICE
    // null si no tiene ficha, "Pendiente por ficha" lo maneja el frontend
    private String chipName;
    private String chipCode;
    private String programName;

    // true si ha iniciado sesion al menos una vez
    private boolean hasSession;

}
