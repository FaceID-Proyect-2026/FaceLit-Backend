package com.FaceLit.backend.face.controller.facialrecognition;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.face.dto.request.facialrecognition.DeviceRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.DeviceResponseDTO;
import com.FaceLit.backend.face.model.enums.DeviceStatus;
import com.FaceLit.backend.face.service.facialrecognition.DeviceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<DeviceResponseDTO> create(@Valid @RequestBody DeviceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.createDevice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponseDTO> update(@PathVariable UUID id,
            @Valid @RequestBody DeviceRequestDTO dto) {
        return ResponseEntity.ok(deviceService.updateDevice(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponseDTO>> getAll() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @GetMapping("/code")
    public ResponseEntity<DeviceResponseDTO> getByCode(@RequestParam String deviceCode) {
        return ResponseEntity.ok(deviceService.getDeviceByCode(deviceCode));
    }

    @GetMapping("/status")
    public ResponseEntity<List<DeviceResponseDTO>> getByStatus(@RequestParam DeviceStatus status) {
        return ResponseEntity.ok(deviceService.getDevicesByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
