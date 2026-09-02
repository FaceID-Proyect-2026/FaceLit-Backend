package com.FaceLit.backend.auth.repository.legal;

import java.util.UUID;
import java.util.Optional; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.auth.model.legal.AcceptanceTerms;
import com.FaceLit.backend.auth.model.security.User;

@Repository
public interface AcceptanceTermsRepository extends JpaRepository<AcceptanceTerms, UUID> {

    // Verifica si el usuario yaa acepto los terminos
    // Se usa para no registrar duplicacion
    boolean existsByUser(User user);

    // Busca la aceptación de términos del usuario, para poder eliminarla
    Optional<AcceptanceTerms> findByUser(User user);

}
