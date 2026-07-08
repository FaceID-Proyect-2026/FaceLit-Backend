package com.FaceLit.backend.academic.model.academic;

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

import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.model.enums.WorkingDay;
import com.FaceLit.backend.shared.model.AuditBase;

// RF-3.2 — Registro de fichas asociadas a un programa
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "chip", schema = "academic")
public class Chip extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_chip", nullable = false)
    private UUID idChip;

    // Relacion N:1 — muchas fichas pertenecen a un programa
    // Una ficha solo pertenece a un programa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_program", nullable = false)
    private Program program;

    // Codigo de la ficha — ejemplo: 2977225
    @Column(name = "chip_code", length = 50)
    private String chipCode;

    // Nombre de la ficha
    @Column(name = "chip_name", length = 100)
    private String chipName;

    // Estado — ACTIVE o INACTIVE
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20)
    private ChipState state = ChipState.ACTIVE;

    // Jornada — MANANA, TARDE o NOCHE
    @Enumerated(EnumType.STRING)
    @Column(name = "workingday", length = 50)
    private WorkingDay workingDay;
}
