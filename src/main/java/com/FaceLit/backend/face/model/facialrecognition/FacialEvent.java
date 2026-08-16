package com.FaceLit.backend.face.model.facialrecognition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.face.model.enums.EventOrigin;
import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "facial_event", schema = "facialrecognition")
public class FacialEvent extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_facial_event", nullable = false)
    private UUID idFacialEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_app")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_environment", nullable = false)
    private Environment environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chip")
    private Chip chip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_device", nullable = false)
    private Device device;

    @Column(name = "event_datetime", nullable = false)
    private LocalDateTime eventDatetime;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recognition_result", nullable = false, length = 50)
    private RecognitionResult recognitionResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 50)
    private SendStatus sendStatus = SendStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 50)
    private EventOrigin origin;

    @OneToMany(mappedBy = "facialEvent", fetch = FetchType.LAZY)
    private List<BiometricLog> biometricLogs = new ArrayList<>();
}
