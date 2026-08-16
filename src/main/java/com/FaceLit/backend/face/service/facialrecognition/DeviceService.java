package com.FaceLit.backend.face.service.facialrecognition;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.face.dto.request.facialrecognition.DeviceRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.DeviceResponseDTO;
import com.FaceLit.backend.face.model.enums.DeviceStatus;

public interface DeviceService {

    DeviceResponseDTO createDevice(DeviceRequestDTO dto);

    DeviceResponseDTO updateDevice(UUID id, DeviceRequestDTO dto);

    List<DeviceResponseDTO> getAllDevices();

    DeviceResponseDTO getDeviceById(UUID id);

    DeviceResponseDTO getDeviceByCode(String deviceCode);

    List<DeviceResponseDTO> getDevicesByStatus(DeviceStatus status);

    void deleteDevice(UUID id);
}
