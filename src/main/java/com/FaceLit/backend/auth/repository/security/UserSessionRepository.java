package com.FaceLit.backend.auth.repository.security;

import com.FaceLit.backend.auth.model.security.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    // No necesita métodos personalizados por ahora
    // JpaRepository ya tiene save() que es lo único que necesitamos en el login

}
