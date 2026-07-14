package com.FaceLit.backend.auth.model.legal;

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

// HU-03
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guardian", schema = "legal")
public class Guardian extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_guardian")
    private UUID idGuardian;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "identity_document", nullable = false, length = 50)
    private String identityDocument;

    @Column(name = "guardian_email", nullable = false, length = 150)
    private String emailGuardian;

}
