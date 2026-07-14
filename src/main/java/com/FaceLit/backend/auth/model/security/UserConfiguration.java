package com.FaceLit.backend.auth.model.security;

import java.time.OffsetDateTime;

import com.FaceLit.backend.auth.model.enums.Language;
import com.FaceLit.backend.shared.model.AuditBase;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU-05 — Configuración personal del usuario
// Guarda las preferencias que el usuario elige dentro del sistema
// Ejemplo: activar notificaciones, cambiar idioma, modo oscuro
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_configuration", schema = "security")
public class UserConfiguration extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user_configuration", nullable = false)
    private UUID idUserConfiguration;

    // Relación N:1 — un usuario puede tener varias configuraciones
    // registradas en el tiempo (historial de cambios)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user;

    // Nombre de la configuración
    // Ejemplo: "Configuración principal"
    @Column(name = "configuration_name", nullable = false, length = 100)
    private String configurationName;

    // Descripción opcional del cambio
    @Column(name = "description", length = 255)
    private String description;

    // true = el usuario quiere recibir notificaciones
    // false = el usuario desactivó las notificaciones
    @Column(name = "notifications_active", nullable = false)
    private boolean notificationsActive = true;

    // true = modo oscuro activado
    // false = modo claro (por defecto)
    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode = false;

    // Fecha en que el usuario hizo este cambio de configuración
    // Sirve para tener el historial de cuándo cambió cada preferencia
    @Column(name = "update_date")
    private OffsetDateTime updateDate;

    // Idioma seleccionado por el usuario
    // Solo acepta: ES, EN, DE, PT
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private Language language;

}
