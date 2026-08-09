package com.FaceLit.backend.environments.model.environment;

import com.FaceLit.backend.environments.model.enums.RecordEnvironmentStatus;
import com.FaceLit.backend.schedule.model.schedule.Schedule;
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

// Tabla pivote entre horario y ambiente
// Un horario ocupa un ambiente en una franja horaria
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

    // FK hacia schedule.schedule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_schedule", nullable = false)
    private Schedule schedule;

    // Fecha y hora de la asignacion
    @Column(name = "assignment_date", nullable = false)
    private OffsetDateTime assignmentDate;

    // Estado de la asignacion
    @Enumerated(EnumType.STRING)
    @Column(name = "active", length = 10)
    private RecordEnvironmentStatus active = RecordEnvironmentStatus.ACTIVE;

}
