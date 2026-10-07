package com.FaceLit.backend.facial.service.serviceImpl;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import com.FaceLit.backend.academic.model.academic.Apprentice;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.repository.ApprenticeRepository;
import com.FaceLit.backend.academic.repository.UserChipRepository;
import com.FaceLit.backend.environment.model.RecordEnvironment;
import com.FaceLit.backend.environment.repository.RecordEnvironmentRepository;
import com.FaceLit.backend.facial.FacialEmbeddingVerificationClient;
import com.FaceLit.backend.facial.FacialEmbeddingVerificationClient.VerificationResponse;
import com.FaceLit.backend.facial.dto.request.FacialAttendanceCaptureRequestDTO;
import com.FaceLit.backend.facial.dto.request.FacialEventRequestDTO;
import com.FaceLit.backend.facial.dto.response.AttendanceStatusResponseDTO;
import com.FaceLit.backend.facial.dto.response.FacialEventResponseDTO;
import com.FaceLit.backend.facial.exception.FacialEventException;
import com.FaceLit.backend.facial.model.Device;
import com.FaceLit.backend.facial.model.FacialEvent;
import com.FaceLit.backend.facial.model.enums.AttendanceStatus;
import com.FaceLit.backend.facial.model.enums.FacialEventOrigin;
import com.FaceLit.backend.facial.model.enums.FacialEventType;
import com.FaceLit.backend.facial.repository.DeviceRepository;
import com.FaceLit.backend.facial.repository.FacialEventRepository;
import com.FaceLit.backend.facial.service.FacialEventService;

@Service
public class FacialEventServiceImpl implements FacialEventService {

    private final FacialEventRepository facialEventRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;
    private final DeviceRepository deviceRepository;
    private final ApprenticeRepository apprenticeRepository;
    private final UserChipRepository userChipRepository;
    private final FacialEmbeddingVerificationClient verificationClient;

    public FacialEventServiceImpl(
            FacialEventRepository facialEventRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            DeviceRepository deviceRepository,
            ApprenticeRepository apprenticeRepository,
            UserChipRepository userChipRepository,
            FacialEmbeddingVerificationClient verificationClient) {
        this.facialEventRepository = facialEventRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.deviceRepository = deviceRepository;
        this.apprenticeRepository = apprenticeRepository;
        this.userChipRepository = userChipRepository;
        this.verificationClient = verificationClient;
    }

    @Override
    @Transactional
    public FacialEventResponseDTO registerEvent(FacialEventRequestDTO dto, UUID authenticatedUserId) {
        RecordEnvironment record = recordEnvironmentRepository.findById(dto.getIdRecordEnvironment())
                .orElseThrow(() -> new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND));
        Device device = deviceRepository.findById(dto.getIdDevice())
                .orElseThrow(() -> new FacialEventException("Dispositivo no encontrado.", HttpStatus.NOT_FOUND));
        Apprentice apprentice = apprenticeRepository.findById(dto.getIdApprentice())
                .orElseThrow(() -> new FacialEventException("Aprendiz no encontrado.", HttpStatus.NOT_FOUND));

        if (!device.getIdDevice().equals(record.getDevice().getIdDevice())) {
            throw new FacialEventException("El dispositivo no pertenece a la sesión indicada.", HttpStatus.BAD_REQUEST);
        }
        if (!userChipRepository.existsByApprentice_IdApprenticeAndChip_IdChipAndState(
                apprentice.getIdApprentice(),
                record.getChip().getIdChip(),
                AcademicState.ACTIVE)) {
            throw new FacialEventException("El aprendiz no pertenece a la ficha activa de esta sesión.", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime eventDatetime = dto.getEventDatetime() == null ? OffsetDateTime.now() : dto.getEventDatetime();
        FacialEventType eventType = resolveEventType(eventDatetime, record);
        AttendanceStatus attendanceStatus = resolveAttendanceStatus(eventType, eventDatetime, record, apprentice);

        FacialEvent event = new FacialEvent();
        event.setRecordEnvironment(record);
        event.setDevice(device);
        event.setApprentice(apprentice);
        event.setEventDatetime(eventDatetime);
        event.setEventType(eventType);
        event.setRecognitionResult(dto.getRecognitionResult().trim());
        event.setAttendanceStatus(attendanceStatus);
        event.setMatchScore(dto.getMatchScore());
        event.setOrigin(parseOrigin(dto.getOrigin()));
        event.setCreatedBy(authenticatedUserId.toString());

        return new FacialEventResponseDTO(facialEventRepository.save(event));
    }

    @Override
    @Transactional
    public FacialEventResponseDTO registerEventFromImage(FacialAttendanceCaptureRequestDTO dto, UUID authenticatedUserId) {
        VerificationResponse verification;
        try {
            verification = verificationClient.verifySession(
                    dto.getIdRecordEnvironment(),
                    dto.getImageBase64());
        } catch (RestClientException ex) {
            throw new FacialEventException("No fue posible verificar el rostro capturado.", HttpStatus.BAD_GATEWAY);
        }

        if (verification == null || !verification.match() || verification.idApprentice() == null) {
            throw new FacialEventException("Rostro no reconocido para esta ficha", HttpStatus.BAD_REQUEST);
        }

        FacialEventRequestDTO event = new FacialEventRequestDTO();
        event.setIdRecordEnvironment(dto.getIdRecordEnvironment());
        event.setIdDevice(dto.getIdDevice());
        event.setIdApprentice(verification.idApprentice());
        event.setRecognitionResult("MATCH");
        event.setMatchScore(verification.similarity());
        event.setOrigin(dto.getOrigin() == null || dto.getOrigin().isBlank() ? "MOBILE" : dto.getOrigin());

        return registerEvent(event, authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatusResponseDTO getAttendanceStatus(UUID idRecordEnvironment, UUID idApprentice) {
        if (!recordEnvironmentRepository.existsById(idRecordEnvironment)) {
            throw new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND);
        }
        if (!apprenticeRepository.existsById(idApprentice)) {
            throw new FacialEventException("Aprendiz no encontrado.", HttpStatus.NOT_FOUND);
        }

        AttendanceStatus status = facialEventRepository
                .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeOrderByEventDatetimeAsc(
                        idRecordEnvironment,
                        idApprentice,
                        FacialEventType.ENTRY)
                .map(FacialEvent::getAttendanceStatus)
                .orElse(AttendanceStatus.ABSENT);

        return new AttendanceStatusResponseDTO(idRecordEnvironment, idApprentice, status);
    }

    private FacialEventType resolveEventType(OffsetDateTime eventDatetime, RecordEnvironment record) {
        if (!eventDatetime.isBefore(record.getEntryTime()) && eventDatetime.isBefore(record.getExitTime())) {
            return FacialEventType.ENTRY;
        }
        if (!eventDatetime.isBefore(record.getExitTime()) && eventDatetime.isBefore(record.getShutdownTime())) {
            return FacialEventType.EXIT;
        }
        throw new FacialEventException(
                "El evento está fuera de las ventanas permitidas de entrada o salida.",
                HttpStatus.BAD_REQUEST);
    }

    private AttendanceStatus resolveAttendanceStatus(
            FacialEventType eventType,
            OffsetDateTime eventDatetime,
            RecordEnvironment record,
            Apprentice apprentice) {
        if (eventType == FacialEventType.ENTRY) {
            OffsetDateTime punctualLimit = record.getEntryTime().plusMinutes(record.getRegistrationMinutes());
            return eventDatetime.isAfter(punctualLimit)
                    ? AttendanceStatus.LATE
                    : AttendanceStatus.PUNCTUAL;
        }

        return facialEventRepository
                .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeOrderByEventDatetimeAsc(
                        record.getIdRecordEnvironment(),
                        apprentice.getIdApprentice(),
                        FacialEventType.ENTRY)
                .map(FacialEvent::getAttendanceStatus)
                .orElse(AttendanceStatus.ABSENT);
    }

    private FacialEventOrigin parseOrigin(String rawOrigin) {
        try {
            return FacialEventOrigin.valueOf(rawOrigin.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new FacialEventException("El origen debe ser PC o MOBILE.", HttpStatus.BAD_REQUEST);
        }
    }
}
