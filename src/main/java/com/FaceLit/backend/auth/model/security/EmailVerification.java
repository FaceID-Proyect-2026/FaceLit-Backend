package com.FaceLit.backend.auth.model.security;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.shared.model.AuditBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU-01 
@Getter
@Setter
@NoArgsConstructor
@Table(name = "email_verification", schema = "security")
@Entity
public class EmailVerification extends AuditBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_email_verification")
    private UUID idVerification;

    // FetchType.LAZY permite que el usuario NO se cargue inmediatamente
    // cuando se consulta EmailVerification.
    //
    // El User solo se obtiene de la base de datos cuando realmente
    // se llama user o getUser()
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user; // UUID del usuario, cada usuario tiene su propia validacion

    @Column(name = "code", nullable = false, length = 6)
    private String code; // código de verificación

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // aspira ( ejemplo: vence en 10 minutos )

    @Column(name = "used", nullable = false)
    private boolean used = false; // usado o no usado ( false = 0 cuando el codigo no a sido utilizado)
                                  // ( true = 1 cuando el codigo ya a sido utilizado)

    // El service llama el metodo para saber si el codigo sigue siendo valido
    // Vigente
    public boolean isCurrent() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
        // aqui Devuelve true si :
        // !used : NO ha sido usado
        // && LocalDateTime.now().isBefore(expiresAt) : Y todavía no ha expirado
    }

}
