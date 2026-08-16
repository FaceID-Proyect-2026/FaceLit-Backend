package com.FaceLit.backend.environments.model.environment;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import com.FaceLit.backend.schedule.model.schedule.ScheduleException;
import com.FaceLit.backend.environments.model.enums.EnvironmentStatus;
import com.FaceLit.backend.face.model.facialrecognition.FacialEvent;
import com.FaceLit.backend.shared.model.AuditBase;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "environment", schema = "environment")

public class Environment extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_environment", nullable = false)
    private UUID idEnvironment;

    // Nombre del ambiente — debe ser único, no se permiten duplicados
    @Column(name = "environment_name", nullable = false, length = 100)
    private String environmentName;

    // Capacidad máxima de usuarios del ambiente
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    // Estado del ambiente — ACTIVE o INACTIVE
    // Por defecto ACTIVE al registrarse
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnvironmentStatus status = EnvironmentStatus.ACTIVE;

    // Un ambiente puede tener muchas fichas asignadas
    @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY)
    private List<ChipEnvironment> chipEnvironments = new ArrayList<>();

    // Un ambiente puede tener muchas excepciones registradas
    @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY)
    private List<ScheduleException> scheduleExceptions = new ArrayList<>();

    // Un ambiente puede registrar muchos eventos faciales
    @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY)
    private List<FacialEvent> facialEvents = new ArrayList<>();
}
