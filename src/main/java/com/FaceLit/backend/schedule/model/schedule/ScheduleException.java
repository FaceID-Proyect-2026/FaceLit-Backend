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

import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionStatus;
import com.FaceLit.backend.schedule.model.enums.ScheduleExceptionType;
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

     // Tipo de excepción — determina qué campo de reemplazo aplica
    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 30)
    private ScheduleExceptionType exceptionType;

    // Cuando la excepción es de tipo INSTRUCTOR_CHANGE, el ambiente original del 
    // horario se conserva y este campo se deja nulo; para ENVIRONMENT_CHANGE se
    // guarda el ambiente alterno autorizado.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_environment", nullable = true)
    private Environment environment;

    // Solo se usa si exceptionType = INSTRUCTOR_CHANGE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instructor_replacement", nullable = true)
    private User instructorReplacement;

    // Fecha en la que el horario presenta la excepción
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    // Si es null, la excepción aplica solo a exceptionDate (un solo día)
    @Column(name = "end_date")
    private LocalDate endDate;

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
