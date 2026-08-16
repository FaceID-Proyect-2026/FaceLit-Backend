package com.FaceLit.backend.face.model.facialrecognition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.face.model.enums.DeviceStatus;
import com.FaceLit.backend.shared.model.AuditBase;

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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "device", schema = "facialrecognition")
public class Device extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_device", nullable = false)
    private UUID idDevice;

    @Column(name = "device_code", nullable = false, length = 50)
    private String deviceCode;

    @Column(name = "location", length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "origin_ip", length = 50)
    private String originIp;

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
    private List<FacialEvent> facialEvents = new ArrayList<>();
}
