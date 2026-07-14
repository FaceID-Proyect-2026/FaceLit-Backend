package com.FaceLit.backend.auth.repository.security;

import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    // Validar si ya existe una credencial con ese email
    boolean existsByEmail(String email);

    // Busacr credenciales por email
    Optional<Credential> findByEmail(String email); // Opcional es una clase que puede contener un valor o vacio.

    // En auth/repository/security/CredentialRepository.java
    Optional<Credential> findByUser(User user);

}
