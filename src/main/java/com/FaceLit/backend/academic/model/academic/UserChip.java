package com.FaceLit.backend.academic.model.academic;

import com.FaceLit.backend.shared.model.AuditBase;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;
import com.FaceLit.backend.auth.model.security.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_chip", schema = "academic")
public class UserChip extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user_chip", nullable = false)
    private UUID idUserChip;

    // FK hacia security.user_app
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user;

    // FK hacia academic.chip
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chip", nullable = false)
    private Chip chip;

    // Fecha de asignación
    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    // Estado de la asignación
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20)
    private UserChipStatus state = UserChipStatus.ACTIVE;

}
