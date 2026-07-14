package com.FaceLit.backend.auth.model.legal;

import java.time.LocalDateTime;
import java.util.UUID;
import com.FaceLit.backend.shared.model.AuditBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.FetchType;

// HU-03
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consent_verification", schema = "legal")
public class ConsentVerification extends AuditBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_consent_verification")
    private UUID idConsentVerification;

    // Relación 1:1 con Consentimiento
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_consent", nullable = false)
    private Consent consent;
      // Token ( codigo de 6 gijitos )
    @Column(name = "token", nullable = false, length = 6)
    private String token;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "used", nullable = false)
    private boolean used = false; 

    // Verifica que el token sea válido — no usado y no expirado
    public boolean isCurrent() {
        return !used && LocalDateTime.now().isBefore(expirationDate); 
    }
}
