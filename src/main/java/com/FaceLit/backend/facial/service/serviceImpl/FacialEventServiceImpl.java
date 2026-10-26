package com.FaceLit.backend.facial.service.serviceImpl;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.FaceLit.backend.facial.dto.request.UpdateFacialEventExcuseRequestDTO;
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
import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FacialEventServiceImpl implements FacialEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacialEventServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId SESSION_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter SESSION_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SESSION_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final FacialEventRepository facialEventRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;
    private final DeviceRepository deviceRepository;
    private final ApprenticeRepository apprenticeRepository;
    private final UserChipRepository userChipRepository;
    private final InstructorRepository instructorRepository;
    private final FacialEmbeddingVerificationClient verificationClient;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public FacialEventServiceImpl(
            FacialEventRepository facialEventRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            DeviceRepository deviceRepository,
            ApprenticeRepository apprenticeRepository,
            UserChipRepository userChipRepository,
            InstructorRepository instructorRepository,
            FacialEmbeddingVerificationClient verificationClient,
            JdbcTemplate jdbcTemplate,
            NotificationService notificationService) {
        this.facialEventRepository = facialEventRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.deviceRepository = deviceRepository;
        this.apprenticeRepository = apprenticeRepository;
        this.userChipRepository = userChipRepository;
        this.instructorRepository = instructorRepository;
        this.verificationClient = verificationClient;
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public FacialEventResponseDTO registerEvent(FacialEventRequestDTO dto, UUID authenticatedUserId) {
        RecordEnvironment record = recordEnvironmentRepository.findById(dto.getIdRecordEnvironment())
                .orElseThrow(() -> new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND));
        requireSessionOperator(record, authenticatedUserId);
        if (!Boolean.TRUE.equals(record.getActive()) || record.getDeletedAt() != null) {
            throw new FacialEventException("La sesión no está activa.", HttpStatus.CONFLICT);
        }
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
        if (dto.getEventType() != null) {
            eventType = dto.getEventType();
            if (eventType == FacialEventType.ENTRY && !eventDatetime.isBefore(record.getExitTime())) {
                throw new FacialEventException("La ventana de entrada ya terminó.", HttpStatus.BAD_REQUEST);
            }
        }
        if (eventType == FacialEventType.EXIT) {
            boolean attended = facialEventRepository
                        .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
                                record.getIdRecordEnvironment(), apprentice.getIdApprentice(), FacialEventType.ENTRY)
                        .filter(entry -> entry.getAttendanceStatus() != AttendanceStatus.ABSENT
                                && entry.getEventDatetime().isBefore(eventDatetime)).isPresent();
            if (!attended) throw new FacialEventException("Debes registrar una entrada antes de registrar la salida.", HttpStatus.BAD_REQUEST);
        }
        AttendanceStatus attendanceStatus = resolveAttendanceStatus(eventType, eventDatetime, record, apprentice);

        FacialEvent event = facialEventRepository
                .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
                        record.getIdRecordEnvironment(), apprentice.getIdApprentice(), eventType)
                .orElse(null);

        if (event != null && event.getAttendanceStatus() != AttendanceStatus.ABSENT) {
            throw new FacialEventException("La asistencia ya fue registrada anteriormente", HttpStatus.CONFLICT);
        }
        if (event == null) {
            event = new FacialEvent();
            event.setRecordEnvironment(record);
            event.setDevice(device);
            event.setApprentice(apprentice);
        }
        event.setEventDatetime(eventDatetime);
        event.setEventType(eventType);
        event.setRecognitionResult(dto.getRecognitionResult().trim());
        event.setAttendanceStatus(attendanceStatus);
        event.setMatchScore(dto.getMatchScore());
        event.setOrigin(parseOrigin(dto.getOrigin()));
        event.setCreatedBy(authenticatedUserId.toString());
        event.setExcuse(null);

        try {
            FacialEvent saved = facialEventRepository.saveAndFlush(event);
            notifyAttendanceEvent(saved);
            return new FacialEventResponseDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new FacialEventException("La asistencia ya fue registrada anteriormente", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public void initializeSessionAbsences(UUID idRecordEnvironment, UUID authenticatedUserId) {
        RecordEnvironment record = recordEnvironmentRepository.findById(idRecordEnvironment)
                .orElseThrow(() -> new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND));
        requireSessionOperator(record, authenticatedUserId);

        String sql = """
                INSERT INTO facialrecognition.facial_event (
                    id_record_environment, id_device, id_apprentice, event_datetime,
                    event_type, recognition_result, attendance_status, origin, excuse, created_by
                )
                SELECT re.id_record_environment, re.id_device, ac.id_apprentice, re.entry_time,
                       'ENTRY', 'ABSENT', 'ABSENT', 'PC', FALSE, ?
                FROM environment.record_environment re
                JOIN academic.apprentice_chip ac
                  ON ac.id_chip = re.id_chip
                 AND ac.state = 'ACTIVE'
                 AND ac.deleted_at IS NULL
                JOIN academic.apprentice a
                  ON a.id_apprentice = ac.id_apprentice
                 AND a.deleted_at IS NULL
                WHERE re.id_record_environment = ?
                  AND re.deleted_at IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM facialrecognition.facial_event fe
                      WHERE fe.id_record_environment = re.id_record_environment
                        AND fe.id_apprentice = ac.id_apprentice
                        AND fe.event_type = 'ENTRY'
                        AND fe.deleted_at IS NULL
                  )
                """;

        jdbcTemplate.update(sql, authenticatedUserId.toString(), record.getIdRecordEnvironment());
    }

    @Override
    @Transactional
    public FacialEventResponseDTO registerEventFromImage(FacialAttendanceCaptureRequestDTO dto, UUID authenticatedUserId) {
        RecordEnvironment record = recordEnvironmentRepository.findById(dto.getIdRecordEnvironment())
                .orElseThrow(() -> new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND));
        requireSessionOperator(record, authenticatedUserId);
        if (!Boolean.TRUE.equals(record.getActive()) || record.getDeletedAt() != null) {
            throw new FacialEventException("La sesión no está activa.", HttpStatus.CONFLICT);
        }
        VerificationResponse verification;
        try {
            verification = verificationClient.verifySession(
                    dto.getIdRecordEnvironment(),
                    dto.getImageBase64(),
                    dto.getImageFrames(),
                    dto.getLivenessChallenge(),
                    dto.getLivenessChallenges());
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

        if (verification == null || !verification.live()) {
            throw new FacialEventException(resolveLivenessFailureMessage(verification), HttpStatus.BAD_REQUEST);
        }

        if (!verification.match() || verification.idApprentice() == null) {
            throw new FacialEventException("Rostro no reconocido para esta ficha", HttpStatus.BAD_REQUEST);
        }

        FacialEventRequestDTO event = new FacialEventRequestDTO();
        event.setIdRecordEnvironment(dto.getIdRecordEnvironment());
        event.setIdDevice(dto.getIdDevice());
        event.setIdApprentice(verification.idApprentice());
        event.setRecognitionResult("MATCH");
        event.setEventType(dto.getEventType());
        event.setMatchScore(verification.similarity());
        event.setOrigin(dto.getOrigin() == null || dto.getOrigin().isBlank() ? "MOBILE" : dto.getOrigin());

        return registerEvent(event, authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatusResponseDTO getAttendanceStatus(UUID idRecordEnvironment, UUID idApprentice, UUID authenticatedUserId) {
        RecordEnvironment record = recordEnvironmentRepository.findById(idRecordEnvironment)
                .orElseThrow(() -> new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND));
        Apprentice apprentice = apprenticeRepository.findById(idApprentice)
                .orElseThrow(() -> new FacialEventException("Aprendiz no encontrado.", HttpStatus.NOT_FOUND));
        if (hasRole(authenticatedUserId, "APPRENTICE")) {
            if (!apprentice.getUser().getIdUser().equals(authenticatedUserId)
                    || !userChipRepository.existsByApprentice_IdApprenticeAndChip_IdChipAndState(
                            idApprentice, record.getChip().getIdChip(), AcademicState.ACTIVE)) {
                throw new FacialEventException("Solo puedes consultar tu propia asistencia.", HttpStatus.FORBIDDEN);
            }
        } else {
            requireSessionOperator(record, authenticatedUserId);
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

        UUID ownUserId = null;
        UUID idInstructor = null;
        if (hasRole(authenticatedUserId, "APPRENTICE")) {
            if (!userChipRepository.findByApprentice_User_IdUserAndState(authenticatedUserId, AcademicState.ACTIVE)
                    .map(link -> link.getChip().getIdChip().equals(idChip)).orElse(false)) {
                throw new FacialEventException("No puedes consultar esta ficha.", HttpStatus.FORBIDDEN);
            }
            ownUserId = authenticatedUserId;
        } else if (hasRole(authenticatedUserId, "INSTRUCTOR")) {
            idInstructor = instructorRepository.findByUser_IdUser(authenticatedUserId)
                    .map(Instructor::getIdInstructor)
                    .orElseThrow(() -> new FacialEventException("Instructor no autorizado.", HttpStatus.FORBIDDEN));
            boolean assigned = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM academic.instructor_chip
                        WHERE id_instructor = ? AND id_chip = ? AND active = TRUE AND deleted_at IS NULL)
                        OR EXISTS (SELECT 1 FROM environment.record_environment
                        WHERE id_chip = ? AND deleted_at IS NULL
                        AND (id_instructor_scheduled = ? OR id_instructor_in_charge = ?))
                    """, Boolean.class, idInstructor, idChip, idChip, idInstructor, idInstructor));
            if (!assigned) throw new FacialEventException("No puedes consultar esta ficha.", HttpStatus.FORBIDDEN);
        } else if (!hasRole(authenticatedUserId, "COORDINATOR")) {
            throw new FacialEventException("No puedes consultar asistencias.", HttpStatus.FORBIDDEN);
        }

        List<AttendanceMatrixRow> rows = findAttendanceMatrixRows(
                idChip,
                idInstructor,
                ownUserId,
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
                    row.idFacialEvent(),
                    row.recordEnvironmentId(),
                    row.sessionDate(),
                    status,
                    row.entryTime(),
                    row.delayMinutes() == null ? 0 : row.delayMinutes(),
                    row.environmentName(),
                    row.instructorName(),
                    row.chipCode(),
                    row.programName(),
                    row.exitRegistered(),
                    row.excuse(),
                    row.sessionStartTime(),
                    row.sessionEndTime(),
                    row.exitTime(),
                    row.idEnvironment()));
        }

        return new AttendanceMatrixResponseDTO(
                first.idChip(),
                first.chipCode(),
                first.idProgram(),
                first.programName(),
                new ArrayList<>(sessions.values()),
                learners.values().stream().map(LearnerMatrixBuilder::toDto).toList());
    }

    @Override
    @Transactional
    public FacialEventResponseDTO updateExcuse(UpdateFacialEventExcuseRequestDTO dto, UUID authenticatedUserId) {
        RecordEnvironment record = recordEnvironmentRepository.findById(dto.getIdRecordEnvironment())
                .orElseThrow(() -> new FacialEventException("Sesión no encontrada.", HttpStatus.NOT_FOUND));
        requireSessionOperator(record, authenticatedUserId);
        Apprentice apprentice = apprenticeRepository.findById(dto.getIdApprentice())
                .orElseThrow(() -> new FacialEventException("Aprendiz no encontrado.", HttpStatus.NOT_FOUND));

        if (!userChipRepository.existsByApprentice_IdApprenticeAndChip_IdChipAndState(
                apprentice.getIdApprentice(),
                record.getChip().getIdChip(),
                AcademicState.ACTIVE)) {
            throw new FacialEventException("El aprendiz no pertenece a la ficha activa de esta sesión.", HttpStatus.BAD_REQUEST);
        }

        FacialEvent event = facialEventRepository
                .findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
                        record.getIdRecordEnvironment(),
                        apprentice.getIdApprentice(),
                        FacialEventType.ENTRY)
                .orElseGet(() -> buildAbsentEvent(record, apprentice, authenticatedUserId));
        boolean newAbsentEvent = event.getIdFacialEvent() == null;
        Boolean previousExcuse = event.getExcuse();

        if (event.getAttendanceStatus() != AttendanceStatus.ABSENT) {
            throw new FacialEventException("Solo se puede registrar excusa para inasistencias.", HttpStatus.BAD_REQUEST);
        }

        event.setExcuse(dto.getExcuse());
        event.setUpdatedBy(authenticatedUserId.toString());

        FacialEvent saved = facialEventRepository.saveAndFlush(event);
        notifyExcuseOrAbsence(saved, newAbsentEvent, previousExcuse);
        if (Boolean.TRUE.equals(previousExcuse) && !Boolean.TRUE.equals(saved.getExcuse())) {
            notifyExcuseRemoved(saved, authenticatedUserId);
        }
        return new FacialEventResponseDTO(saved);
    }

    private void notifyExcuseRemoved(FacialEvent event, UUID actor) {
        try {
            var names = jdbcTemplate.queryForList("SELECT CONCAT(first_name, ' ', last_name) FROM security.user_app WHERE id_user_app = ?", String.class, actor);
            String actorName = names.isEmpty() ? "Instructor" : names.get(0);
            var meta = (com.fasterxml.jackson.databind.node.ObjectNode) OBJECT_MAPPER.readTree(metadataFor(event, "corrected"));
            meta.put("previousStatus", "ABSENT"); meta.put("newStatus", "ABSENT");
            meta.put("previousExcuse", true); meta.put("newExcuse", false);
            meta.put("previousDatetime", event.getEventDatetime().toString()); meta.put("newDatetime", event.getEventDatetime().toString());
            meta.put("changedByName", actorName); meta.put("changedBy", actor.toString());
            meta.put("correctionReasonCode", "excuse_removed");
            meta.put("correctedAt", OffsetDateTime.now().toString());
            String message = "Se retiró la excusa: la inasistencia pasó de justificada a sin excusa. Modificado por: " + actorName + ".\n" + attendanceContext(event);
            for (UUID recipient : new java.util.LinkedHashSet<>(List.of(event.getApprentice().getUser().getIdUser(), responsibleInstructor(event.getRecordEnvironment()).getUser().getIdUser()))) {
                boolean learner = recipient.equals(event.getApprentice().getUser().getIdUser());
                var user = learner ? event.getApprentice().getUser() : responsibleInstructor(event.getRecordEnvironment()).getUser();
                meta.put("recipientName", fullName(user.getFirstName(), user.getLastName()));
                meta.put("recipientContext", learner ? "learner" : "responsible_instructor");
                notificationService.createForRecipient(new CreateNotificationRequestDTO(recipient, "attendance_corrected", "Asistencia corregida",
                        "Destinatario: " + fullName(user.getFirstName(), user.getLastName()) + ".\n" + message
                                + (learner ? "\nRecibes este aviso porque se modificó tu asistencia." : "\nRecibes este aviso porque eres el instructor responsable seleccionado para esta sesión.")
                                + " Revisa el registro actualizado. Si no estás de acuerdo, contacta al coordinador.",
                        event.getRecordEnvironment().getIdRecordEnvironment(), "record_environment", event.getIdFacialEvent(), meta.toString()));
            }
        } catch (JsonProcessingException error) { throw new IllegalStateException(error); }
    }

    private void notifyAttendanceEvent(FacialEvent event) {
        if (event.getEventType() == FacialEventType.ENTRY && event.getAttendanceStatus() == AttendanceStatus.LATE) {
            createNotification(
                    event.getApprentice().getUser().getIdUser(),
                    "attendance_late",
                    "Retraso registrado",
                    "Tu asistencia fue registrada como tardia para la sesion de la ficha "
                            + event.getRecordEnvironment().getChip().getChipCode() + ".\n" + attendanceContext(event),
                    event,
                    metadataFor(event, "late"));
            return;
        }

        if (event.getEventType() == FacialEventType.EXIT
                && event.getRecordEnvironment().getExitTime() != null
                && event.getEventDatetime().isBefore(event.getRecordEnvironment().getExitTime())) {
            Instructor instructor = responsibleInstructor(event.getRecordEnvironment());
            createNotification(
                    instructor.getUser().getIdUser(),
                    "attendance_early_exit",
                    "Salida anticipada",
                    fullName(event.getApprentice().getUser().getFirstName(), event.getApprentice().getUser().getLastName())
                            + " registro salida antes de la hora configurada para la ficha "
                            + event.getRecordEnvironment().getChip().getChipCode() + ".\n" + attendanceContext(event)
                            + "\nHora de salida registrada: " + event.getEventDatetime().atZoneSameInstant(SESSION_ZONE).format(SESSION_TIME),
                    event,
                    metadataFor(event, "early_exit"));
        }
    }

    private void notifyExcuseOrAbsence(FacialEvent event, boolean newAbsentEvent, Boolean previousExcuse) {
        boolean excuseRegistered = Boolean.TRUE.equals(event.getExcuse()) && !Boolean.TRUE.equals(previousExcuse);
        if (excuseRegistered) {
            String message = "Se registró una excusa para la inasistencia.\n" + attendanceContext(event);
            createNotification(
                    event.getApprentice().getUser().getIdUser(),
                    "attendance_excuse_registered",
                    "Excusa registrada",
                    message,
                    event,
                    metadataFor(event, "excuse_registered"));
            createNotification(
                    responsibleInstructor(event.getRecordEnvironment()).getUser().getIdUser(),
                    "attendance_excuse_registered",
                    "Excusa registrada",
                    message,
                    event,
                    metadataFor(event, "excuse_registered"));
            return;
        }

        if (newAbsentEvent && event.getAttendanceStatus() == AttendanceStatus.ABSENT
                && !event.getRecordEnvironment().getShutdownTime().isAfter(OffsetDateTime.now())) {
            createNotification(
                    event.getApprentice().getUser().getIdUser(),
                    "attendance_absent",
                    "Inasistencia registrada",
                    "Se registró una inasistencia.\n" + attendanceContext(event),
                    event,
                    metadataFor(event, "absent"));
        }
    }

    public String attendanceContext(FacialEvent event) {
        RecordEnvironment record = event.getRecordEnvironment();
        var learner = event.getApprentice().getUser();
        var instructor = responsibleInstructor(record).getUser();
        var sessionStart = record.getEntryTime().atZoneSameInstant(SESSION_ZONE);
        return "Aprendiz: " + fullName(learner.getFirstName(), learner.getLastName())
                + "\nFecha de la sesión: " + sessionStart.format(SESSION_DATE)
                + "\nHora de inicio: " + sessionStart.format(SESSION_TIME) + " (hora de Colombia)"
                + "\nFicha: " + record.getChip().getChipCode()
                + "\nInstructor responsable de la sesión: "
                + fullName(instructor.getFirstName(), instructor.getLastName()) + ".";
    }

    private void createNotification(
            UUID recipientUserId,
            String type,
            String title,
            String message,
            FacialEvent event,
            String metadataJson) {
        try {
            notificationService.createForRecipient(new CreateNotificationRequestDTO(
                    recipientUserId,
                    type,
                    title,
                    message,
                    event.getRecordEnvironment().getIdRecordEnvironment(),
                    "record_environment",
                    event.getIdFacialEvent(),
                    metadataJson));
        } catch (RuntimeException ex) {
            LOGGER.warn("No fue posible crear la notificacion {} para el usuario {}.", type, recipientUserId, ex);
        }
    }

    private Instructor responsibleInstructor(RecordEnvironment record) {
        return record.getInstructorScheduled();
    }

    public String metadataFor(FacialEvent event, String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reason", reason);
        metadata.put("idRecordEnvironment", event.getRecordEnvironment().getIdRecordEnvironment());
        metadata.put("idFacialEvent", event.getIdFacialEvent());
        metadata.put("idApprentice", event.getApprentice().getIdApprentice());
        metadata.put("chipCode", event.getRecordEnvironment().getChip().getChipCode());
        var learner = event.getApprentice().getUser();
        Instructor instructor = responsibleInstructor(event.getRecordEnvironment());
        var sessionStart = event.getRecordEnvironment().getEntryTime().atZoneSameInstant(SESSION_ZONE);
        metadata.put("learnerName", fullName(learner.getFirstName(), learner.getLastName()));
        metadata.put("sessionDate", sessionStart.toLocalDate().toString());
        metadata.put("sessionStart", sessionStart.toOffsetDateTime().toString());
        metadata.put("sessionTimezone", SESSION_ZONE.getId());
        metadata.put("idInstructor", instructor.getIdInstructor());
        metadata.put("instructorName", fullName(instructor.getUser().getFirstName(), instructor.getUser().getLastName()));
        metadata.put("attendanceStatus", event.getAttendanceStatus().name());
        metadata.put("eventType", event.getEventType().name());
        metadata.put("eventDatetime", event.getEventDatetime().toString());
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("No fue posible serializar metadata de notificacion para evento facial {}.",
                    event.getIdFacialEvent(),
                    ex);
            return null;
        }
    }

    private List<AttendanceMatrixRow> findAttendanceMatrixRows(
            UUID idChip,
            UUID idInstructor,
            UUID ownUserId,
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
                                CAST(? AS uuid) IS NULL
                             OR
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
                    fe.id_facial_event,
                    re.id_environment,
                    TO_CHAR(re.entry_time AT TIME ZONE 'America/Bogota', 'HH24:MI') AS session_start_time,
                    TO_CHAR(re.exit_time AT TIME ZONE 'America/Bogota', 'HH24:MI') AS session_end_time,
                    CASE WHEN fex.attendance_status <> 'ABSENT'
                         THEN COALESCE(TO_CHAR(fex.event_datetime AT TIME ZONE 'America/Bogota', 'HH24:MI'), '')
                         ELSE '' END AS actual_exit_time,
                    CASE
                        WHEN fe.attendance_status = 'ABSENT' OR fe.event_datetime IS NULL THEN ''
                        ELSE TO_CHAR(fe.event_datetime AT TIME ZONE 'America/Bogota', 'HH24:MI')
                    END AS entry_time,
                    fe.attendance_status,
                    COALESCE(fe.excuse, FALSE) AS excuse,
                    CASE
                        WHEN fex.id_facial_event IS NOT NULL
                         AND fex.attendance_status IS NOT NULL
                         AND fex.attendance_status <> 'ABSENT'
                        THEN TRUE
                        ELSE FALSE
                    END AS exit_registered,
                    CASE
                        WHEN fe.attendance_status = 'ABSENT' OR fe.event_datetime IS NULL THEN 0
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
                WHERE (CAST(? AS uuid) IS NULL OR u.id_user_app = ?)
                ORDER BY re.entry_time ASC, u.last_name ASC, u.first_name ASC, u.number_document ASC
                """;

        return jdbcTemplate.query(sql, ps -> {
            ps.setObject(1, idChip);
            ps.setObject(2, dateFrom);
            ps.setObject(3, dateTo);
            ps.setObject(4, idInstructor);
            ps.setObject(5, idInstructor);
            ps.setObject(6, idInstructor);
            ps.setObject(7, ownUserId);
            ps.setObject(8, ownUserId);
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
                rs.getObject("id_facial_event", UUID.class),
                rs.getString("entry_time"),
                rs.getString("attendance_status"),
                rs.getObject("excuse", Boolean.class),
                rs.getInt("delay_minutes"),
                rs.getBoolean("exit_registered"),
                rs.getString("session_start_time"),
                rs.getString("session_end_time"),
                rs.getString("actual_exit_time"),
                rs.getObject("id_environment", UUID.class)));
    }

    private FacialEvent buildAbsentEvent(RecordEnvironment record, Apprentice apprentice, UUID authenticatedUserId) {
        FacialEvent event = new FacialEvent();
        event.setRecordEnvironment(record);
        event.setDevice(record.getDevice());
        event.setApprentice(apprentice);
        event.setEventDatetime(record.getEntryTime());
        event.setEventType(FacialEventType.ENTRY);
        event.setRecognitionResult("ABSENT");
        event.setAttendanceStatus(AttendanceStatus.ABSENT);
        event.setOrigin(FacialEventOrigin.PC);
        event.setExcuse(false);
        event.setCreatedBy(authenticatedUserId.toString());
        return event;
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
            UUID idFacialEvent,
            String entryTime,
            String attendanceStatus,
            Boolean excuse,
            Integer delayMinutes,
            Boolean exitRegistered,
            String sessionStartTime,
            String sessionEndTime,
            String exitTime,
            UUID idEnvironment) {
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

    private boolean hasRole(UUID actor, String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return actor != null && authentication != null && authentication.isAuthenticated()
                && actor.toString().equals(authentication.getName())
                && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private void requireSessionOperator(RecordEnvironment record, UUID actor) {
        boolean belongs = record.getInstructorScheduled() != null
                && record.getInstructorScheduled().getUser().getIdUser().equals(actor)
                || record.getInstructorInCharge() != null
                && record.getInstructorInCharge().getUser().getIdUser().equals(actor);
        if (record.getDeletedAt() != null || !(hasRole(actor, "COORDINATOR") || hasRole(actor, "INSTRUCTOR") && belongs)) {
            throw new FacialEventException("No puedes operar esta sesión.", HttpStatus.FORBIDDEN);
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
        if (normalized.contains("liveness") || normalized.contains("vida") || normalized.contains("frames")) {
            return "No se pudo comprobar que la captura sea de una persona en vivo.";
        }
        return "No fue posible verificar el rostro capturado.";
    }

    private String resolveLivenessFailureMessage(VerificationResponse verification) {
        if (verification == null || verification.livenessReason() == null || verification.livenessReason().isBlank()) {
            return "No se pudo comprobar que la captura sea de una persona en vivo.";
        }

        String reason = verification.livenessReason().trim();
        String normalized = reason.toLowerCase(Locale.ROOT);
        if (normalized.contains("misma persona")) {
            return "La secuencia no parece corresponder a la misma persona. Intenta moverte más suave.";
        }
        if (normalized.contains("puntos faciales")) {
            return "No se pudo medir bien el rostro. Centra la cara dentro del óvalo e intenta de nuevo.";
        }
        if (normalized.contains("movimiento facial")) {
            return "No se detectó suficiente movimiento. Haz el reto más despacio y un poco más marcado.";
        }
        if (normalized.contains("secuencia")) {
            return "La cámara no capturó suficientes imágenes para validar vida.";
        }
        return reason;
    }
}
