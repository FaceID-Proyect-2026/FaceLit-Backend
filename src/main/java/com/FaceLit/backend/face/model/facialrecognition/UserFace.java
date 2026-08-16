package com.FaceLit.backend.face.model.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.face.model.enums.FaceStatus;
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
@Table(name = "user_face", schema = "facialrecognition")
public class UserFace extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user_face", nullable = false)
    private UUID idUserFace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app", nullable = false)
    private User user;

    @Column(name = "biometric_vector", nullable = false)
    private byte[] biometricVector;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FaceStatus status = FaceStatus.PENDING;
}
