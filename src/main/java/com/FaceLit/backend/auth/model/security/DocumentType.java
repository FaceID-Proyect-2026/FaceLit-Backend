package com.FaceLit.backend.auth.model.security;

import java.util.UUID;
import com.FaceLit.backend.shared.model.AuditBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU-01
@Entity
@Table(name = "type_document", schema = "security")
@Getter 
@Setter 
@NoArgsConstructor   // Tipo de socumento
public class DocumentType extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_type_document")
    private UUID idDocumentType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;   // nombre (Cedula de cioudadania, tarjeera de identidad)
   
    @Column(name = "abbreviation", nullable = false, length = 10)
    private String abbreviation; // TI, CC, CE, PA
}