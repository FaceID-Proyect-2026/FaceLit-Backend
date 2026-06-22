package com.FaceLit.backend.auth.repository.legal;

import java.util.Optional;
import java.util.UUID;

import com.FaceLit.backend.auth.model.security.User; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.auth.model.legal.Consent;

@Repository
public interface ConsentRepository extends JpaRepository < Consent, UUID> {

    // Buscar el consentimiento de un usuario menor
    Optional<Consent> findByUser(User user); 

    
}
