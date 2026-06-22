package com.FaceLit.backend.auth.repository.legal;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.auth.model.legal.Guardian;

@Repository
public interface GuardianRepository  extends JpaRepository <Guardian, UUID>{

    // Verificar si ya existe un acudiente con ese correo
    boolean existsByEmailGuardian(String emailGuardian); 

}
