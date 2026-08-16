package com.FaceLit.backend.face.service.facialrecognition;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.face.dto.request.facialrecognition.BiometricLogRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.BiometricLogResponseDTO;

public interface BiometricLogService {

    BiometricLogResponseDTO createBiometricLog(BiometricLogRequestDTO dto);

    BiometricLogResponseDTO updateBiometricLog(UUID id, BiometricLogRequestDTO dto);

    List<BiometricLogResponseDTO> getAllBiometricLogs();

    BiometricLogResponseDTO getBiometricLogById(UUID id);

    List<BiometricLogResponseDTO> getBiometricLogsByFacialEvent(UUID idFacialEvent);

    void deleteBiometricLog(UUID id);
}
