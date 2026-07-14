package com.FaceLit.backend.auth.repository.security;


import java.util.UUID;
import com.FaceLit.backend.auth.model.security.DocumentType;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTypeRepository  extends JpaRepository<DocumentType, UUID>{
    
    // Busca por abreviación - usado en el ServiceImpl para detectar TI
    Optional<DocumentType> findByAbbreviation (String abbreviation); 
}
