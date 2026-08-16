package com.FaceLit.backend.face.model.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.shared.model.AuditBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "biometric_log", schema = "facialrecognition")
public class BiometricLog extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_biometric_log", nullable = false)
    private UUID idBiometricLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_facial_event", nullable = false)
    private FacialEvent facialEvent;

    @Column(name = "description")
    private String description;

    @Column(name = "log_date", nullable = false)
    private LocalDateTime logDate;
}
