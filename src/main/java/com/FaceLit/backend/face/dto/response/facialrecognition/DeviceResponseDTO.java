package com.FaceLit.backend.face.dto.response.facialrecognition;

import java.time.LocalDateTime;
import java.util.UUID;

import com.FaceLit.backend.face.model.enums.DeviceStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceResponseDTO {

    private UUID idDevice;
    private String deviceCode;
    private String location;
    private DeviceStatus status;
    private String originIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
