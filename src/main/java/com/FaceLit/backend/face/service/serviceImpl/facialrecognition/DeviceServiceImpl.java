package com.FaceLit.backend.face.service.serviceImpl.facialrecognition;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.face.dto.request.facialrecognition.DeviceRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.DeviceResponseDTO;
import com.FaceLit.backend.face.exception.DeviceException;
import com.FaceLit.backend.face.model.enums.DeviceStatus;
import com.FaceLit.backend.face.model.facialrecognition.Device;
import com.FaceLit.backend.face.repository.facialrecognition.DeviceRepository;
import com.FaceLit.backend.face.service.facialrecognition.DeviceService;

import jakarta.transaction.Transactional;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public DeviceResponseDTO createDevice(DeviceRequestDTO dto) {
        if (deviceRepository.existsByDeviceCode(dto.getDeviceCode())) {
            throw new IllegalArgumentException("Ya existe un dispositivo con ese código");
        }

        Device device = new Device();
        device.setDeviceCode(dto.getDeviceCode());
        device.setLocation(dto.getLocation());
        device.setStatus(dto.getStatus() != null ? dto.getStatus() : DeviceStatus.ACTIVE);
        device.setOriginIp(dto.getOriginIp());

        Device saved = deviceRepository.save(device);

        return new DeviceResponseDTO(
                saved.getIdDevice(),
                saved.getDeviceCode(),
                saved.getLocation(),
                saved.getStatus(),
                saved.getOriginIp(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                "Dispositivo registrado correctamente");
    }

    @Override
    @Transactional
    public DeviceResponseDTO updateDevice(UUID id, DeviceRequestDTO dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceException("Dispositivo no encontrado"));

        if (!device.getDeviceCode().equals(dto.getDeviceCode())
                && deviceRepository.existsByDeviceCode(dto.getDeviceCode())) {
            throw new IllegalArgumentException("Ya existe un dispositivo con ese código");
        }

        device.setDeviceCode(dto.getDeviceCode());
        device.setLocation(dto.getLocation());

        if (dto.getStatus() != null) {
            device.setStatus(dto.getStatus());
        }

        device.setOriginIp(dto.getOriginIp());

        Device updated = deviceRepository.save(device);

        return new DeviceResponseDTO(
                updated.getIdDevice(),
                updated.getDeviceCode(),
                updated.getLocation(),
                updated.getStatus(),
                updated.getOriginIp(),
                updated.getCreatedAt(),
                updated.getUpdatedAt(),
                "Dispositivo actualizado correctamente");
    }

    @Override
    public List<DeviceResponseDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(device -> new DeviceResponseDTO(
                        device.getIdDevice(),
                        device.getDeviceCode(),
                        device.getLocation(),
                        device.getStatus(),
                        device.getOriginIp(),
                        device.getCreatedAt(),
                        device.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public DeviceResponseDTO getDeviceById(UUID id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceException("Dispositivo no encontrado"));

        return new DeviceResponseDTO(
                device.getIdDevice(),
                device.getDeviceCode(),
                device.getLocation(),
                device.getStatus(),
                device.getOriginIp(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                null);
    }

    @Override
    public DeviceResponseDTO getDeviceByCode(String deviceCode) {
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new DeviceException("Dispositivo no encontrado"));

        return new DeviceResponseDTO(
                device.getIdDevice(),
                device.getDeviceCode(),
                device.getLocation(),
                device.getStatus(),
                device.getOriginIp(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                null);
    }

    @Override
    public List<DeviceResponseDTO> getDevicesByStatus(DeviceStatus status) {
        return deviceRepository.findByStatus(status).stream()
                .map(device -> new DeviceResponseDTO(
                        device.getIdDevice(),
                        device.getDeviceCode(),
                        device.getLocation(),
                        device.getStatus(),
                        device.getOriginIp(),
                        device.getCreatedAt(),
                        device.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceException("Dispositivo no encontrado"));

        deviceRepository.delete(device);
    }
}
