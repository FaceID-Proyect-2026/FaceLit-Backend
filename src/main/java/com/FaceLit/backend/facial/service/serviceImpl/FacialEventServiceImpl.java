package com.FaceLit.backend.facial.service.serviceImpl;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FacialEventServiceImpl implements FacialEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacialEventServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        validateUniqueAttendanceEvent(record.getIdRecordEnvironment(), apprentice.getIdApprentice(), eventType);
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

        try {
            return new FacialEventResponseDTO(facialEventRepository.saveAndFlush(event));
        } catch (DataIntegrityViolationException ex) {
            throw new FacialEventException("La asistencia ya fue registrada anteriormente", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public FacialEventResponseDTO registerEventFromImage(FacialAttendanceCaptureRequestDTO dto, UUID authenticatedUserId) {
        VerificationResponse verification;
        try {
            verification = verificationClient.verifySession(
                    dto.getIdRecordEnvironment(),
                    dto.getImageBase64());
        } catch (RestClientResponseException ex) {
            String detail = extractVerificationServiceDetail(ex);
            LOGGER.warn(
                    "El microservicio facial rechazo la verificacion. status={}, body={}",
                    ex.getStatusCode(),
                    detail);
            throw new FacialEventException(resolveVerificationServiceMessage(detail), HttpStatus.BAD_REQUEST);
        } catch (RestClientException ex) {
            LOGGER.warn("No fue posible conectar con el microservicio facial para verificar el rostro.", ex);
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
                .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
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
                .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
                        record.getIdRecordEnvironment(),
                        apprentice.getIdApprentice(),
                        FacialEventType.ENTRY)
                .map(FacialEvent::getAttendanceStatus)
                .orElse(AttendanceStatus.ABSENT);
    }

    private void validateUniqueAttendanceEvent(
            UUID idRecordEnvironment,
            UUID idApprentice,
            FacialEventType eventType) {
        boolean alreadyRegistered = facialEventRepository
                .existsByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNull(
                        idRecordEnvironment,
                        idApprentice,
                        eventType);
        if (alreadyRegistered) {
            throw new FacialEventException("La asistencia ya fue registrada anteriormente", HttpStatus.CONFLICT);
        }
    }

    private FacialEventOrigin parseOrigin(String rawOrigin) {
        try {
            return FacialEventOrigin.valueOf(rawOrigin.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new FacialEventException("El origen debe ser PC o MOBILE.", HttpStatus.BAD_REQUEST);
        }
    }

    private String extractVerificationServiceDetail(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getMessage();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode detail = root.get("detail");
            if (detail != null && detail.isTextual()) {
                return detail.asText();
            }
        } catch (RuntimeException ignored) {
            // Se deja caer al cuerpo crudo para que el log conserve la respuesta original.
        }
        return body;
    }

    private String resolveVerificationServiceMessage(String detail) {
        if (detail == null || detail.isBlank()) {
            return "No fue posible verificar el rostro capturado.";
        }

        String normalized = detail.toLowerCase(Locale.ROOT);
        if (normalized.contains("no se detect")) {
            return "No se detectó un rostro válido en la imagen.";
        }
        if (normalized.contains("mas de un rostro") || normalized.contains("más de un rostro")) {
            return "La imagen contiene más de un rostro.";
        }
        if (normalized.contains("base64") || normalized.contains("leer la imagen") || normalized.contains("imagen es obligatoria")) {
            return "No fue posible leer la imagen capturada.";
        }
        return "No fue posible verificar el rostro capturado.";
    }
}
