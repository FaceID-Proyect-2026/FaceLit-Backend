package com.FaceLit.backend.schedule.model.schedule;

import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.schedule.model.enums.ScheduleStatus;
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
import java.util.UUID;

// Tabla pivote entre horario e instructor
// Un horario tiene un instructor responsable
// Un instructor puede tener varios horarios
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "schedule_instructor", schema = "schedule")

public class ScheduleInstructor extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_schedule_instructor", nullable = false)
    private UUID idScheduleInstructor;

    // FK hacia schedule.schedule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_schedule", nullable = false)
    private Schedule schedule;

    // FK hacia security.user_app — solo usuarios con rol INSTRUCTOR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user;

    // Estado de la asignacion — ACTIVE o INACTIVE
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.ACTIVE;
    
}
