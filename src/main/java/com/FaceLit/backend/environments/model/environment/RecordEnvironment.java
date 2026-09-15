package com.FaceLit.backend.environments.model.environment;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.shared.model.AuditBase;
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
import java.time.OffsetDateTime;
import java.util.UUID;

// Sesion activa de reconocimiento facial para una ficha y un ambiente
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "record_environment", schema = "environment")
public class RecordEnvironment extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_record_environment", nullable = false)
    private UUID idRecordEnvironment;

    // FK hacia environment.environment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_environment", nullable = false)
    private Environment environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chip", nullable = false)
    private Chip chip;

    // Instructor responsable, referenciado por user_app
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instructor_in_charge", nullable = false)
    private User instructorInCharge;

    @Column(name = "session_start", nullable = false)
    private OffsetDateTime sessionStart;

    @Column(name = "registration_minutes", nullable = false)
    private Integer registrationMinutes;

    @Column(name = "exit_time")
    private java.time.LocalTime exitTime;

    @Column(name = "shutdown_time")
    private java.time.LocalTime shutdownTime;

    @Column(name = "exit_reminder_sent", nullable = false)
    private boolean exitReminderSent = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

}
