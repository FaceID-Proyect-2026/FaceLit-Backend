package com.FaceLit.backend.auth.repository.legal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.FaceLit.backend.auth.model.legal.Consent;
import com.FaceLit.backend.auth.model.legal.ConsentVerification;

public interface ConsentVerificationRepository extends JpaRepository<ConsentVerification, UUID> {

    // Buscar verificación por token — el acudiente llega con este token
    Optional<ConsentVerification> findByToken(String token);

    // Trae el código más reciente del consentimiento — evita el error
    // "más de un resultado" cuando ya hubo reenvíos previos
    @Query("SELECT cv FROM ConsentVerification cv WHERE cv.consent = :consent ORDER BY cv.createdAt DESC LIMIT 1")
    Optional<ConsentVerification> findByConsent(@Param("consent") Consent consent);

}
