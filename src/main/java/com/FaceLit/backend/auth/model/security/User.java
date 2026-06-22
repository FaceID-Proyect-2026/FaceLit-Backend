package com.FaceLit.backend.auth.model.security;

import java.time.LocalDate;
import java.util.UUID;

import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.shared.model.AuditBase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU-01
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "user_app",
    schema = "security",

    // Índice para acelerar búsquedas por número de documento
    indexes = {
        @Index(
            name = "idx_user_document",
            columnList = "number_document"
        )
    },

    // Restricción de unicidad para evitar documentos duplicados
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_document",
            columnNames = "number_document"
        )
    }
)
public class User extends AuditBase {

    // =========================================================
    // ID DEL USUARIO
    // =========================================================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user_app", nullable = false)
    private UUID idUser;

    // =========================================================
    // NÚMERO DE DOCUMENTO
    // No puede repetirse
    // =========================================================
    @Column(name = "number_document", nullable = false, length = 50)
    private String documentNumber;

    // =========================================================
    // NOMBRE DEL USUARIO
    // =========================================================
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    // =========================================================
    // APELLIDO DEL USUARIO
    // =========================================================
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    // =========================================================
    // FECHA DE NACIMIENTO
    // =========================================================
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    // =========================================================
    // ESTADO DE LA CUENTA
    //
    // ACTIVE
    // INACTIVE
    // PENDING_CONSENT
    // BLOCKED
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    // =========================================================
    // VERIFICACIÓN DE CORREO ELECTRÓNICO
    //
    // false = pendiente
    // true  = verificado
    // =========================================================
    @Column(name = "email_verification", nullable = false)
    private boolean emailVerified = false;

    // =========================================================
    // RELACIÓN 1:1 CON CREDENTIAL
    //
    // Un usuario tiene una sola credencial.
    //
    // CascadeType.ALL:
    // - Crear usuario → crea credencial
    // - Actualizar usuario → actualiza credencial
    // - Eliminar usuario → elimina credencial
    // =========================================================
    @OneToOne(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY
    )
    private Credential credential;

    // =========================================================
    // RELACIÓN N:1 CON DOCUMENT TYPE
    //
    // Muchos usuarios pueden compartir el mismo tipo
    // de documento (CC, TI, CE, etc.)
    // =========================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "id_type_document",
        nullable = false
    )
    private DocumentType documentType;
}