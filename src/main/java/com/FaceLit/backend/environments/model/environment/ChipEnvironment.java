package com.FaceLit.backend.environments.model.environment;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.shared.model.AuditBase;
import com.FaceLit.backend.environments.model.enums.ChipEnvironmentStatus;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "chip_environment", schema = "environment")
public class ChipEnvironment extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_chip_environment", nullable = false)
    private UUID idChipEnvironment;

    // FK hacia academic.chip
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chip", nullable = false)
    private Chip chip;

    // FK hacia environment.environment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_environment", nullable = false)
    private Environment environment;

    // Fecha de asignacion
    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    // Estado de la asignacion — ACTIVE o INACTIVE
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ChipEnvironmentStatus status = ChipEnvironmentStatus.ACTIVE;

}
