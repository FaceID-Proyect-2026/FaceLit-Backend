package com.FaceLit.backend.academic.model.academic;

import com.FaceLit.backend.shared.model.AuditBase;
import com.FaceLit.backend.academic.model.enums.ProgramState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

// RF-3.1 — Registro de programas de formación
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "program", schema = "academic")
public class Program extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_program", nullable = false)
    private UUID idProgram;

    // Nombre del programa — único, no se permiten duplicados
    @Column(name = "program_name", nullable = false, length = 100, unique = true)
    private String programName;

    // Estado del programa — ACTIVE o INACTIVE
    // Por defecto ACTIVE al registrarse
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private ProgramState state = ProgramState.ACTIVE;

    // Dentro de Program.java, agrega este atributo:

    // Un programa tiene muchas fichas
    // mappedBy = el campo en Chip que tiene la FK
    @OneToMany(mappedBy = "program", fetch = FetchType.LAZY)
    private List<Chip> chips = new ArrayList<>();

}
