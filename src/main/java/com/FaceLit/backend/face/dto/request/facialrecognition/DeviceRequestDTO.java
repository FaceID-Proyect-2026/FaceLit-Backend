package com.FaceLit.backend.face.dto.request.facialrecognition;

import com.FaceLit.backend.face.model.enums.DeviceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRequestDTO {

    @NotBlank(message = "El código del dispositivo es obligatorio")
    @Size(max = 50, message = "El código del dispositivo no puede superar los 50 caracteres")
    private String deviceCode;

    @Size(max = 100, message = "La ubicación no puede superar los 100 caracteres")
    private String location;

    @NotNull(message = "El estado del dispositivo es obligatorio")
    private DeviceStatus status;

    @Size(max = 50, message = "La IP de origen no puede superar los 50 caracteres")
    private String originIp;
}
