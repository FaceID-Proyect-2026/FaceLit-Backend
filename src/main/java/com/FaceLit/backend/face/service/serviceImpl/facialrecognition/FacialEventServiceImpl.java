package com.FaceLit.backend.face.service.serviceImpl.facialrecognition;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.environments.model.environment.Environment;
import com.FaceLit.backend.environments.repository.environment.EnvironmentRepository;
import com.FaceLit.backend.face.dto.request.facialrecognition.FacialEventRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.FacialEventResponseDTO;
import com.FaceLit.backend.face.model.enums.EventType;
import com.FaceLit.backend.face.model.enums.RecognitionResult;
import com.FaceLit.backend.face.model.enums.SendStatus;
import com.FaceLit.backend.face.model.facialrecognition.Device;
import com.FaceLit.backend.face.model.facialrecognition.FacialEvent;
import com.FaceLit.backend.face.repository.facialrecognition.DeviceRepository;
import com.FaceLit.backend.face.repository.facialrecognition.FacialEventRepository;
import com.FaceLit.backend.face.service.facialrecognition.FacialEventService;

import jakarta.transaction.Transactional;

@Service
public class FacialEventServiceImpl implements FacialEventService {

    private final FacialEventRepository facialEventRepository;
    private final UserRepository userRepository;
    private final EnvironmentRepository environmentRepository;
    private final ChipRepository chipRepository;
    private final DeviceRepository deviceRepository;

    public FacialEventServiceImpl(
            FacialEventRepository facialEventRepository,
            UserRepository userRepository,
            EnvironmentRepository environmentRepository,
            ChipRepository chipRepository,
            DeviceRepository deviceRepository) {
        this.facialEventRepository = facialEventRepository;
        this.userRepository = userRepository;
        this.environmentRepository = environmentRepository;
        this.chipRepository = chipRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public FacialEventResponseDTO createFacialEvent(FacialEventRequestDTO dto) {
        FacialEvent event = new FacialEvent();

        if (dto.getIdUserApp() != null) {
            User user = userRepository.findById(dto.getIdUserApp())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            event.setUser(user);
        }

        Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                .orElseThrow(() -> new IllegalArgumentException("Ambiente no encontrado"));
        event.setEnvironment(environment);

        if (dto.getIdChip() != null) {
            Chip chip = chipRepository.findById(dto.getIdChip())
                    .orElseThrow(() -> new IllegalArgumentException("Ficha no encontrada"));
            event.setChip(chip);
        }

        Device device = deviceRepository.findById(dto.getIdDevice())
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado"));
        event.setDevice(device);

        event.setEventDatetime(dto.getEventDatetime());
        event.setEventType(dto.getEventType());
        event.setRecognitionResult(dto.getRecognitionResult());
        event.setSendStatus(dto.getSendStatus() != null ? dto.getSendStatus() : SendStatus.PENDING);
        event.setOrigin(dto.getOrigin());

        FacialEvent saved = facialEventRepository.save(event);

        return new FacialEventResponseDTO(
                saved.getIdFacialEvent(),
                saved.getUser() != null ? saved.getUser().getIdUser() : null,
                saved.getEnvironment() != null ? saved.getEnvironment().getIdEnvironment() : null,
                saved.getChip() != null ? saved.getChip().getIdChip() : null,
                saved.getDevice() != null ? saved.getDevice().getIdDevice() : null,
                saved.getEventDatetime(),
                saved.getEventType(),
                saved.getRecognitionResult(),
                saved.getSendStatus(),
                saved.getOrigin(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                "Evento facial registrado correctamente");
    }

    @Override
    @Transactional
    public FacialEventResponseDTO updateFacialEvent(UUID id, FacialEventRequestDTO dto) {
        FacialEvent event = facialEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento facial no encontrado"));

        if (dto.getIdUserApp() != null) {
            User user = userRepository.findById(dto.getIdUserApp())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            event.setUser(user);
        }

        if (dto.getIdEnvironment() != null) {
            Environment environment = environmentRepository.findById(dto.getIdEnvironment())
                    .orElseThrow(() -> new IllegalArgumentException("Ambiente no encontrado"));
            event.setEnvironment(environment);
        }

        if (dto.getIdChip() != null) {
            Chip chip = chipRepository.findById(dto.getIdChip())
                    .orElseThrow(() -> new IllegalArgumentException("Ficha no encontrada"));
            event.setChip(chip);
        }

        if (dto.getIdDevice() != null) {
            Device device = deviceRepository.findById(dto.getIdDevice())
                    .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado"));
            event.setDevice(device);
        }

        if (dto.getEventDatetime() != null) {
            event.setEventDatetime(dto.getEventDatetime());
        }
        if (dto.getEventType() != null) {
            event.setEventType(dto.getEventType());
        }
        if (dto.getRecognitionResult() != null) {
            event.setRecognitionResult(dto.getRecognitionResult());
        }
        if (dto.getSendStatus() != null) {
            event.setSendStatus(dto.getSendStatus());
        }
        if (dto.getOrigin() != null) {
            event.setOrigin(dto.getOrigin());
        }

        FacialEvent updated = facialEventRepository.save(event);

        return new FacialEventResponseDTO(
                updated.getIdFacialEvent(),
                updated.getUser() != null ? updated.getUser().getIdUser() : null,
                updated.getEnvironment() != null ? updated.getEnvironment().getIdEnvironment() : null,
                updated.getChip() != null ? updated.getChip().getIdChip() : null,
                updated.getDevice() != null ? updated.getDevice().getIdDevice() : null,
                updated.getEventDatetime(),
                updated.getEventType(),
                updated.getRecognitionResult(),
                updated.getSendStatus(),
                updated.getOrigin(),
                updated.getCreatedAt(),
                updated.getUpdatedAt(),
                "Evento facial actualizado correctamente");
    }

    @Override
    public List<FacialEventResponseDTO> getAllFacialEvents() {
        return facialEventRepository.findAll().stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public FacialEventResponseDTO getFacialEventById(UUID id) {
        FacialEvent event = facialEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento facial no encontrado"));

        return new FacialEventResponseDTO(
                event.getIdFacialEvent(),
                event.getUser() != null ? event.getUser().getIdUser() : null,
                event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                event.getChip() != null ? event.getChip().getIdChip() : null,
                event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                event.getEventDatetime(),
                event.getEventType(),
                event.getRecognitionResult(),
                event.getSendStatus(),
                event.getOrigin(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                null);
    }

    @Override
    public List<FacialEventResponseDTO> getFacialEventsByUser(UUID idUserApp) {
        return facialEventRepository.findByUser_IdUser(idUserApp).stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public List<FacialEventResponseDTO> getFacialEventsByEnvironment(UUID idEnvironment) {
        return facialEventRepository.findByEnvironment_IdEnvironment(idEnvironment).stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public List<FacialEventResponseDTO> getFacialEventsByDevice(UUID idDevice) {
        return facialEventRepository.findByDevice_IdDevice(idDevice).stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public List<FacialEventResponseDTO> getFacialEventsByType(EventType eventType) {
        return facialEventRepository.findByEventType(eventType).stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public List<FacialEventResponseDTO> getFacialEventsByRecognitionResult(RecognitionResult recognitionResult) {
        return facialEventRepository.findByRecognitionResult(recognitionResult).stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public List<FacialEventResponseDTO> getFacialEventsBySendStatus(SendStatus sendStatus) {
        return facialEventRepository.findBySendStatus(sendStatus).stream()
                .map(event -> new FacialEventResponseDTO(
                        event.getIdFacialEvent(),
                        event.getUser() != null ? event.getUser().getIdUser() : null,
                        event.getEnvironment() != null ? event.getEnvironment().getIdEnvironment() : null,
                        event.getChip() != null ? event.getChip().getIdChip() : null,
                        event.getDevice() != null ? event.getDevice().getIdDevice() : null,
                        event.getEventDatetime(),
                        event.getEventType(),
                        event.getRecognitionResult(),
                        event.getSendStatus(),
                        event.getOrigin(),
                        event.getCreatedAt(),
                        event.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFacialEvent(UUID id) {
        FacialEvent event = facialEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento facial no encontrado"));

        facialEventRepository.delete(event);
    }
}
