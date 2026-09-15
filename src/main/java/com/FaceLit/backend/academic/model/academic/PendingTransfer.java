package com.FaceLit.backend.academic.model.academic;

import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.PendingTransferStatus;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pending_transfer", schema = "academic")
public class PendingTransfer extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_pending_transfer", nullable = false)
    private UUID idPendingTransfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_chip_id", nullable = false)
    private Chip currentChip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_chip_id", nullable = false)
    private Chip proposedChip;

    @Column(name = "source_row", nullable = false)
    private Integer sourceRow;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PendingTransferStatus status = PendingTransferStatus.PENDING;
}
