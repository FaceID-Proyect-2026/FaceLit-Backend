package com.FaceLit.backend.auth.model.legal;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.auth.model.enums.ConsentStatus;
import com.FaceLit.backend.shared.model.AuditBase;
import com.FaceLit.backend.auth.model.security.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.FetchType;
import lombok.Setter;

// HU-03
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consent", schema = "legal")
public class Consent extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_consent")
    private UUID idConsent;

    // FK al usuario menor — la relación existe desde que se crea el consentimiento
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user;

    // FK al acudiente — nullable porque se asigna después
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_guardian", nullable = true)
    private Guardian guardian;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    private ConsentStatus consentStatus;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime requestDate;

    // Se llena cuando el acudiente responde

    @Column(name = "response_date", nullable = true)
    private LocalDateTime responseDate;

}