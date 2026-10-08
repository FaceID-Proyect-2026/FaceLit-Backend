package com.FaceLit.backend.facial.service.serviceImpl;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.FaceLit.backend.academic.model.academic.Apprentice;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.repository.ApprenticeRepository;
import com.FaceLit.backend.academic.repository.InstructorRepository;
import com.FaceLit.backend.academic.repository.UserChipRepository;
import com.FaceLit.backend.environment.model.RecordEnvironment;
import com.FaceLit.backend.environment.repository.RecordEnvironmentRepository;
import com.FaceLit.backend.facial.FacialEmbeddingVerificationClient;
import com.FaceLit.backend.facial.FacialEmbeddingVerificationClient.VerificationResponse;
import com.FaceLit.backend.facial.dto.request.FacialAttendanceCaptureRequestDTO;
import com.FaceLit.backend.facial.dto.request.FacialEventRequestDTO;
import com.FaceLit.backend.facial.dto.response.AttendanceMatrixResponseDTO;
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
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final InstructorRepository instructorRepository;
    private final FacialEmbeddingVerificationClient verificationClient;
    private final JdbcTemplate jdbcTemplate;

    public FacialEventServiceImpl(
            FacialEventRepository facialEventRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            DeviceRepository deviceRepository,
            ApprenticeRepository apprenticeRepository,
            UserChipRepository userChipRepository,
            InstructorRepository instructorRepository,
            FacialEmbeddingVerificationClient verificationClient,
            JdbcTemplate jdbcTemplate) {
        this.facialEventRepository = facialEventRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.deviceRepository = deviceRepository;
        this.apprenticeRepository = apprenticeRepository;
        this.userChipRepository = userChipRepository;
        this.instructorRepository = instructorRepository;
        this.verificationClient = verificationClient;
        this.jdbcTemplate = jdbcTemplate;
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

    @Override
    @Transactional(readOnly = true)
    public AttendanceMatrixResponseDTO getAttendanceMatrix(
            UUID idChip,
            LocalDate dateFrom,
            LocalDate dateTo,
            UUID authenticatedUserId) {
        if (dateFrom == null || dateTo == null || dateFrom.isAfter(dateTo)) {
            throw new FacialEventException("El rango de fechas no es válido.", HttpStatus.BAD_REQUEST);
        }

        Instructor instructor = instructorRepository.findByUser_IdUser(authenticatedUserId)
                .orElseThrow(() -> new FacialEventException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        List<AttendanceMatrixRow> rows = findAttendanceMatrixRows(
                idChip,
                instructor.getIdInstructor(),
                dateFrom,
                dateTo);

        if (rows.isEmpty()) {
            return new AttendanceMatrixResponseDTO(idChip, null, null, null, List.of(), List.of());
        }

        AttendanceMatrixRow first = rows.get(0);
        Map<UUID, AttendanceMatrixResponseDTO.AttendanceMatrixSessionDTO> sessions = new LinkedHashMap<>();
        Map<UUID, LearnerMatrixBuilder> learners = new LinkedHashMap<>();

        for (AttendanceMatrixRow row : rows) {
            sessions.putIfAbsent(
                    row.recordEnvironmentId(),
                    new AttendanceMatrixResponseDTO.AttendanceMatrixSessionDTO(
                            row.recordEnvironmentId(),
                            row.sessionDate(),
                            row.environmentName(),
                            row.instructorName()));

            LearnerMatrixBuilder learner = learners.computeIfAbsent(row.idUser(), key -> new LearnerMatrixBuilder(
                    row.idUser(),
                    row.idApprentice(),
                    fullName(row.firstName(), row.lastName()),
                    row.documentNumber()));

            String status = row.attendanceStatus() == null || row.attendanceStatus().isBlank()
                    ? "absent"
                    : row.attendanceStatus().toLowerCase(Locale.ROOT);
            learner.days.add(new AttendanceMatrixResponseDTO.AttendanceMatrixDayDTO(
                    row.recordEnvironmentId(),
                    row.sessionDate(),
                    status,
                    row.entryTime(),
                    row.delayMinutes() == null ? 0 : row.delayMinutes(),
                    row.environmentName(),
                    row.instructorName(),
                    row.chipCode(),
                    row.programName(),
                    row.exitRegistered()));
        }

        return new AttendanceMatrixResponseDTO(
                first.idChip(),
                first.chipCode(),
                first.idProgram(),
                first.programName(),
                new ArrayList<>(sessions.values()),
                learners.values().stream().map(LearnerMatrixBuilder::toDto).toList());
    }

    private List<AttendanceMatrixRow> findAttendanceMatrixRows(
            UUID idChip,
            UUID idInstructor,
            LocalDate dateFrom,
            LocalDate dateTo) {
        String sql = """
                WITH selected_sessions AS (
                    SELECT *
                    FROM (
                        SELECT
                            re.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY CAST(re.entry_time AT TIME ZONE 'America/Bogota' AS date)
                                ORDER BY re.entry_time DESC
                            ) AS session_rank
                        FROM environment.record_environment re
                        WHERE re.id_chip = ?
                          AND re.deleted_at IS NULL
                          AND CAST(re.entry_time AT TIME ZONE 'America/Bogota' AS date) BETWEEN ? AND ?
                          AND (
                                re.id_instructor_scheduled = ?
                             OR re.id_instructor_in_charge = ?
                          )
                    ) ranked_sessions
                    WHERE ranked_sessions.session_rank = 1
                )
                SELECT
                    re.id_record_environment AS record_environment_id,
                    CAST(re.entry_time AT TIME ZONE 'America/Bogota' AS date) AS session_date,
                    c.id_chip,
                    c.chip_code,
                    p.id_program,
                    p.program_name,
                    a.id_apprentice,
                    u.id_user_app,
                    u.first_name,
                    u.last_name,
                    u.number_document,
                    env.environment_name,
                    CONCAT(instructor_user.first_name, ' ', instructor_user.last_name) AS instructor_name,
                    COALESCE(TO_CHAR(fe.event_datetime AT TIME ZONE 'America/Bogota', 'HH24:MI'), '') AS entry_time,
                    fe.attendance_status,
                    CASE
                        WHEN fex.id_facial_event IS NOT NULL
                         AND fex.attendance_status IS NOT NULL
                         AND fex.attendance_status <> 'ABSENT'
                        THEN TRUE
                        ELSE FALSE
                    END AS exit_registered,
                    CASE
                        WHEN fe.event_datetime IS NULL THEN 0
                        WHEN fe.event_datetime <= re.entry_time + (re.registration_minutes * INTERVAL '1 minute') THEN 0
                        ELSE FLOOR(EXTRACT(EPOCH FROM (fe.event_datetime - (re.entry_time + (re.registration_minutes * INTERVAL '1 minute')))) / 60)::INT
                    END AS delay_minutes
                FROM selected_sessions re
                JOIN academic.chip c
                  ON c.id_chip = re.id_chip
                JOIN academic.program p
                  ON p.id_program = c.id_program
                JOIN environment.environment env
                  ON env.id_environment = re.id_environment
                JOIN academic.instructor instructor
                  ON instructor.id_instructor = COALESCE(re.id_instructor_in_charge, re.id_instructor_scheduled)
                JOIN security.user_app instructor_user
                  ON instructor_user.id_user_app = instructor.id_user_app
                JOIN academic.apprentice_chip ac
                  ON ac.id_chip = re.id_chip
                 AND ac.state = 'ACTIVE'
                 AND ac.deleted_at IS NULL
                JOIN academic.apprentice a
                  ON a.id_apprentice = ac.id_apprentice
                 AND a.deleted_at IS NULL
                JOIN security.user_app u
                  ON u.id_user_app = a.id_user_app
                 AND u.deleted_at IS NULL
                LEFT JOIN facialrecognition.facial_event fe
                  ON fe.id_record_environment = re.id_record_environment
                 AND fe.id_apprentice = a.id_apprentice
                 AND fe.event_type = 'ENTRY'
                 AND fe.deleted_at IS NULL
                LEFT JOIN facialrecognition.facial_event fex
                  ON fex.id_record_environment = re.id_record_environment
                 AND fex.id_apprentice = a.id_apprentice
                 AND fex.event_type = 'EXIT'
                 AND fex.deleted_at IS NULL
                ORDER BY re.entry_time ASC, u.last_name ASC, u.first_name ASC, u.number_document ASC
                """;

        return jdbcTemplate.query(sql, ps -> {
            ps.setObject(1, idChip);
            ps.setObject(2, dateFrom);
            ps.setObject(3, dateTo);
            ps.setObject(4, idInstructor);
            ps.setObject(5, idInstructor);
        }, (rs, rowNum) -> new AttendanceMatrixRow(
                rs.getObject("record_environment_id", UUID.class),
                rs.getObject("session_date", LocalDate.class),
                rs.getObject("id_chip", UUID.class),
                rs.getString("chip_code"),
                rs.getObject("id_program", UUID.class),
                rs.getString("program_name"),
                rs.getObject("id_apprentice", UUID.class),
                rs.getObject("id_user_app", UUID.class),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("number_document"),
                rs.getString("environment_name"),
                rs.getString("instructor_name"),
                rs.getString("entry_time"),
                rs.getString("attendance_status"),
                rs.getInt("delay_minutes"),
                rs.getBoolean("exit_registered")));
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

    private String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    private record AttendanceMatrixRow(
            UUID recordEnvironmentId,
            LocalDate sessionDate,
            UUID idChip,
            String chipCode,
            UUID idProgram,
            String programName,
            UUID idApprentice,
            UUID idUser,
            String firstName,
            String lastName,
            String documentNumber,
            String environmentName,
            String instructorName,
            String entryTime,
            String attendanceStatus,
            Integer delayMinutes,
            Boolean exitRegistered) {
    }

    private static final class LearnerMatrixBuilder {
        private final UUID learnerId;
        private final UUID apprenticeId;
        private final String learnerName;
        private final String learnerDocument;
        private final List<AttendanceMatrixResponseDTO.AttendanceMatrixDayDTO> days = new ArrayList<>();

        private LearnerMatrixBuilder(UUID learnerId, UUID apprenticeId, String learnerName, String learnerDocument) {
            this.learnerId = learnerId;
            this.apprenticeId = apprenticeId;
            this.learnerName = learnerName;
            this.learnerDocument = learnerDocument;
        }

        private AttendanceMatrixResponseDTO.AttendanceMatrixLearnerDTO toDto() {
            return new AttendanceMatrixResponseDTO.AttendanceMatrixLearnerDTO(
                    learnerId,
                    apprenticeId,
                    learnerName,
                    learnerDocument,
                    days);
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
        } catch (JsonProcessingException ignored) {
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
