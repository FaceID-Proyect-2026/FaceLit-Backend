package com.FaceLit.backend.facial.repository;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.FaceLit.backend.facial.dto.response.AttendanceMatrixRowProjection;
import com.FaceLit.backend.facial.model.FacialEvent;
import com.FaceLit.backend.facial.model.enums.FacialEventType;

public interface FacialEventRepository extends JpaRepository<FacialEvent, UUID> {

    Optional<FacialEvent> findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
            UUID idRecordEnvironment,
            UUID idApprentice,
            FacialEventType eventType);

    boolean existsByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNull(
            UUID idRecordEnvironment,
            UUID idApprentice,
            FacialEventType eventType);

    @Query(value = """
            SELECT
                re.id_record_environment AS "recordEnvironmentId",
                TO_CHAR(re.entry_time AT TIME ZONE 'America/Bogota', 'YYYY-MM-DD') AS "sessionDate",
                c.id_chip AS "idChip",
                c.chip_code AS "chipCode",
                p.id_program AS "idProgram",
                p.program_name AS "programName",
                a.id_apprentice AS "idApprentice",
                u.id_user_app AS "idUser",
                u.first_name AS "firstName",
                u.last_name AS "lastName",
                u.number_document AS "documentNumber",
                env.environment_name AS "environmentName",
                CONCAT(instructor_user.first_name, ' ', instructor_user.last_name) AS "instructorName",
                COALESCE(TO_CHAR(fe.event_datetime AT TIME ZONE 'America/Bogota', 'HH24:MI'), '') AS "entryTime",
                fe.attendance_status AS "attendanceStatus",
                CASE
                    WHEN fe.event_datetime IS NULL THEN 0
                    WHEN fe.event_datetime <= re.entry_time + (re.registration_minutes * INTERVAL '1 minute') THEN 0
                    ELSE FLOOR(EXTRACT(EPOCH FROM (fe.event_datetime - (re.entry_time + (re.registration_minutes * INTERVAL '1 minute')))) / 60)::INT
                END AS "delayMinutes"
            FROM environment.record_environment re
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
            WHERE re.id_chip = :idChip
              AND re.deleted_at IS NULL
              AND CAST(re.entry_time AT TIME ZONE 'America/Bogota' AS date) BETWEEN :dateFrom AND :dateTo
              AND (
                    re.id_instructor_scheduled = :idInstructor
                 OR re.id_instructor_in_charge = :idInstructor
              )
            ORDER BY re.entry_time ASC, u.last_name ASC, u.first_name ASC, u.number_document ASC
            """, nativeQuery = true)
    List<AttendanceMatrixRowProjection> findAttendanceMatrixRows(
            @Param("idChip") UUID idChip,
            @Param("idInstructor") UUID idInstructor,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);
}
