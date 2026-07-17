package com.FaceLit.backend.schedule.model.schedule;

import com.FaceLit.backend.environments.model.environment.Environment;
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

import java.time.LocalDate;
import java.util.UUID;

import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionStatus;
import com.FaceLit.backend.shared.model.AuditBase;

@Entity
@Table(name = "schedule_exception", schema = "schedule")
@Getter
@Setter
@NoArgsConstructor
public class ScheduleException extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_schedule_exception", nullable = false)
    private UUID idScheduleException;

    // Relación N:1 — muchas excepciones pertenecen a un horario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_schedule", nullable = false)
    private Schedule schedule;

    // Relación N:1 — muchas excepciones pertenecen a un ambiente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_environment", nullable = false)
    private Environment environment;

    // Fecha en la que el horario presenta la excepción
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    // Motivo por el cual el ambiente no estará disponible
    @Column(name = "reason", length = 255)
    private String reason;

    // Estado de la excepción
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleExceptionStatus status = ScheduleExceptionStatus.ACTIVE;

    // Fecha en la que se registró la excepción
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

}
