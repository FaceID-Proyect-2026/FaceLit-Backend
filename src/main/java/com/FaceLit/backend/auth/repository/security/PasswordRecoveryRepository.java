package com.FaceLit.backend.auth.repository.security;

import com.FaceLit.backend.auth.model.security.PasswordRecovery;
import com.FaceLit.backend.auth.model.security.User;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordRecoveryRepository extends JpaRepository<PasswordRecovery, UUID> {

    //// Busca el token exacto que el usuario ingresa para restablecer la contraseña
    Optional<PasswordRecovery> findByToken(String token);

    // Busca el código activo más reciente de un usuario
    // Se usa para invalidar códigos anteriores al generar uno nuevo
    @Query("SELECT pr FROM PasswordRecovery pr WHERE pr.user = :user AND pr.used = false ORDER BY pr.createdAt DESC LIMIT 1")
    Optional<PasswordRecovery> findActiveByUser(@Param("user") User user); 

}
