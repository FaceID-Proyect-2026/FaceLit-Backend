package com.FaceLit.backend.auth.repository.legal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.auth.model.legal.Consent;
import com.FaceLit.backend.auth.model.legal.ConsentVerification;

public interface ConsentVerificationRepository extends JpaRepository<ConsentVerification, UUID> {

    // Buscar verificación por token — el acudiente llega con este token
    Optional<ConsentVerification> findByToken(String token);

    // Buscar verificación por consentimiento
    Optional<ConsentVerification> findByConsent(Consent consent);

}
