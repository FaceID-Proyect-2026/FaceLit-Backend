package com.FaceLit.backend.face.service.serviceImpl.facialrecognition;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.face.dto.request.facialrecognition.BiometricLogRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.BiometricLogResponseDTO;
import com.FaceLit.backend.face.model.facialrecognition.BiometricLog;
import com.FaceLit.backend.face.model.facialrecognition.FacialEvent;
import com.FaceLit.backend.face.repository.facialrecognition.BiometricLogRepository;
import com.FaceLit.backend.face.repository.facialrecognition.FacialEventRepository;
import com.FaceLit.backend.face.service.facialrecognition.BiometricLogService;

import jakarta.transaction.Transactional;

@Service
public class BiometricLogServiceImpl implements BiometricLogService {

    private final BiometricLogRepository biometricLogRepository;
    private final FacialEventRepository facialEventRepository;

    public BiometricLogServiceImpl(
            BiometricLogRepository biometricLogRepository,
            FacialEventRepository facialEventRepository) {
        this.biometricLogRepository = biometricLogRepository;
        this.facialEventRepository = facialEventRepository;
    }

    @Override
    @Transactional
    public BiometricLogResponseDTO createBiometricLog(BiometricLogRequestDTO dto) {
        FacialEvent facialEvent = facialEventRepository.findById(dto.getIdFacialEvent())
                .orElseThrow(() -> new IllegalArgumentException("Evento facial no encontrado"));

        BiometricLog biometricLog = new BiometricLog();
        biometricLog.setFacialEvent(facialEvent);
        biometricLog.setDescription(dto.getDescription());
        biometricLog.setLogDate(dto.getLogDate());

        BiometricLog saved = biometricLogRepository.save(biometricLog);

        return new BiometricLogResponseDTO(
                saved.getIdBiometricLog(),
                saved.getFacialEvent() != null ? saved.getFacialEvent().getIdFacialEvent() : null,
                saved.getDescription(),
                saved.getLogDate(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                "Log biométrico registrado correctamente");
    }

    @Override
    @Transactional
    public BiometricLogResponseDTO updateBiometricLog(UUID id, BiometricLogRequestDTO dto) {
        BiometricLog biometricLog = biometricLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log biométrico no encontrado"));

        if (dto.getIdFacialEvent() != null) {
            FacialEvent facialEvent = facialEventRepository.findById(dto.getIdFacialEvent())
                    .orElseThrow(() -> new IllegalArgumentException("Evento facial no encontrado"));
            biometricLog.setFacialEvent(facialEvent);
        }

        if (dto.getDescription() != null) {
            biometricLog.setDescription(dto.getDescription());
        }

        if (dto.getLogDate() != null) {
            biometricLog.setLogDate(dto.getLogDate());
        }

        BiometricLog updated = biometricLogRepository.save(biometricLog);

        return new BiometricLogResponseDTO(
                updated.getIdBiometricLog(),
                updated.getFacialEvent() != null ? updated.getFacialEvent().getIdFacialEvent() : null,
                updated.getDescription(),
                updated.getLogDate(),
                updated.getCreatedAt(),
                updated.getUpdatedAt(),
                "Log biométrico actualizado correctamente");
    }

    @Override
    public List<BiometricLogResponseDTO> getAllBiometricLogs() {
        return biometricLogRepository.findAll().stream()
                .map(log -> new BiometricLogResponseDTO(
                        log.getIdBiometricLog(),
                        log.getFacialEvent() != null ? log.getFacialEvent().getIdFacialEvent() : null,
                        log.getDescription(),
                        log.getLogDate(),
                        log.getCreatedAt(),
                        log.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public BiometricLogResponseDTO getBiometricLogById(UUID id) {
        BiometricLog biometricLog = biometricLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log biométrico no encontrado"));

        return new BiometricLogResponseDTO(
                biometricLog.getIdBiometricLog(),
                biometricLog.getFacialEvent() != null ? biometricLog.getFacialEvent().getIdFacialEvent() : null,
                biometricLog.getDescription(),
                biometricLog.getLogDate(),
                biometricLog.getCreatedAt(),
                biometricLog.getUpdatedAt(),
                null);
    }

    @Override
    public List<BiometricLogResponseDTO> getBiometricLogsByFacialEvent(UUID idFacialEvent) {
        return biometricLogRepository.findByFacialEvent_IdFacialEvent(idFacialEvent).stream()
                .map(log -> new BiometricLogResponseDTO(
                        log.getIdBiometricLog(),
                        log.getFacialEvent() != null ? log.getFacialEvent().getIdFacialEvent() : null,
                        log.getDescription(),
                        log.getLogDate(),
                        log.getCreatedAt(),
                        log.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBiometricLog(UUID id) {
        BiometricLog biometricLog = biometricLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log biométrico no encontrado"));

        biometricLogRepository.delete(biometricLog);
    }
}
