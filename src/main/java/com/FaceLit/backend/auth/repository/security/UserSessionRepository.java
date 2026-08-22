package com.FaceLit.backend.auth.repository.security;

import com.FaceLit.backend.auth.model.security.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    // No necesita métodos personalizados por ahora
    // JpaRepository ya tiene save() que es lo único que necesitamos en el login

    // Verifica si el usuario ha iniciado sesion al menos una vez
    boolean existsByUser_IdUser(UUID idUser);

    List<UserSession> findByUser_IdUser(UUID idUser);

}
