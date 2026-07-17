package com.FaceLit.backend.schedule.model.schedule;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.schedule.model.enums.DayOfWeek;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// RF-4.1 — Horario de una ficha
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "schedule", schema = "schedule")

public class Schedule extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_schedule", nullable = false)
    private UUID idSchedule;

    // Relacion N:1 — muchos horarios pertenecen a una ficha
    // Una ficha puede tener muchos horarios
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chip", nullable = false)
    private Chip chip;

    // Dia de la semana — LUNES a SABADO
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    // Hora de inicio de la clase
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // Hora de fin de la clase
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Estado del horario — ACTIVE o INACTIVE
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    // Fecha de creacion del horario
    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    // Un horario puede tener un instructor asignado
    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY)
    private List<ScheduleInstructor> instructors = new ArrayList<>();

    // Un horario puede tener muchas excepciones registradas
    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY)
    private List<ScheduleException> scheduleExceptions = new ArrayList<>();

}
