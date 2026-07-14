package com.FaceLit.backend.auth.repository.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.FaceLit.backend.auth.model.security.EmailVerification;
import com.FaceLit.backend.auth.model.security.User;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

       // Funcionalidad : busca el codigo de verificacion de cuenta, de para asi
       // verificar que el codigo ingresado por el usuario sea el correcto

       // Usuario manda: { userId: "abc-123", code: "483920" }
       // ↓
       // ServiceImpl busca el User por userId
       // ↓
       // Con ese User llama findByUser(user) → encuentra el EmailVerification guardado
       // ↓
       // Compara verification.getCode().equals("483920")
       // ↓
       // Si coincide → email verificado

       // Busca el código activo más reciente del usuario
       // used = false y ordena por created_at descendente, toma el primero
       @Query("SELECT e FROM EmailVerification e WHERE e.user = :user AND e.used = false ORDER BY e.createdAt DESC LIMIT 1")
       Optional<EmailVerification> findByUser(@Param("user") User user);

}
