package com.FaceLit.backend.face.repository.facialrecognition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.face.model.enums.DeviceStatus;
import com.FaceLit.backend.face.model.facialrecognition.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByDeviceCode(String deviceCode);

    boolean existsByDeviceCode(String deviceCode);

    List<Device> findByStatus(DeviceStatus status);
}
