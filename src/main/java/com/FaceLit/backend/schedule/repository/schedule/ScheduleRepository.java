package com.FaceLit.backend.schedule.repository.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import com.FaceLit.backend.schedule.model.enums.DayOfWeek;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import com.FaceLit.backend.schedule.model.schedule.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    /**
     * Obtiene todos los horarios asociados a una ficha.
     *
     * Spring Data JPA genera automáticamente la consulta a partir del nombre
     * del método (findByChip_IdChip), equivalente a:
     *
     * SELECT * FROM schedule
     * WHERE id_chip = :idChip
     *
     * @param idChip Identificador único de la ficha.
     * @return Lista de horarios pertenecientes a la ficha.
     */
    List<Schedule> findByChip_IdChip(UUID idChip);

    /**
     * Verifica si existe un conflicto de horario para un ambiente.
     *
     * Un conflicto ocurre cuando:
     * - El ambiente es el mismo.
     * - El día de la semana coincide.
     * - El horario está activo.
     * - No se compara el mismo horario (útil al editar).
     * - El rango de horas se traslapa con otro horario existente.
     *
     * Ejemplo:
     * Horario existente: 08:00 - 10:00
     * Nuevo horario: 09:00 - 11:00
     * Resultado: Conflicto (true)
     */
    @Query("""
                SELECT COUNT(re) > 0
                FROM RecordEnvironment re
                JOIN re.schedule s
                WHERE re.environment.idEnvironment = :idEnvironment
                AND s.dayOfWeek = :dayOfWeek
                AND s.status = 'ACTIVE'
                AND s.idSchedule <> :excludeId
                AND (
                    (:startTime < s.endTime AND :endTime > s.startTime)
                )
            """)
    boolean existsEnvironmentConflict(
            @Param("idEnvironment") UUID idEnvironment,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    /**
     * Verifica si un instructor tiene otro horario asignado
     * que se cruce con el horario que se desea registrar.
     *
     * La validación se realiza sobre:
     * - Instructor.
     * - Día de la semana.
     * - Horarios activos.
     * - Se excluye el horario actual cuando se está editando.
     * - Se verifica si existe traslape entre las horas.
     */
    @Query("""
                SELECT COUNT(si) > 0
                FROM ScheduleInstructor si
                JOIN si.schedule s
                WHERE si.user.idUser = :idUser
                AND s.dayOfWeek = :dayOfWeek
                AND s.status = 'ACTIVE'
                AND s.idSchedule <> :excludeId
                AND (
                    (:startTime < s.endTime AND :endTime > s.startTime)
                )
            """)
    boolean existsInstructorConflict(
            @Param("idUser") UUID idUser,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    /**
     * Verifica si una ficha ya posee un horario que se
     * superpone con el nuevo horario.
     *
     * Condiciones de validación:
     * - Misma ficha.
     * - Mismo día de la semana.
     * - Horarios activos.
     * - Se excluye el horario actual durante una actualización.
     * - Existe traslape entre la hora de inicio y la hora de fin.
     */
    @Query("""
                SELECT COUNT(s) > 0
                FROM Schedule s
                WHERE s.chip.idChip = :idChip
                AND s.dayOfWeek = :dayOfWeek
                AND s.status = 'ACTIVE'
                AND s.idSchedule <> :excludeId
                AND (
                    (:startTime < s.endTime AND :endTime > s.startTime)
                )
            """)
    boolean existsChipConflict(
            @Param("idChip") UUID idChip,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    // Busca horarios por ambiente
    @Query("""
                SELECT s FROM Schedule s
                JOIN RecordEnvironment re ON re.schedule.idSchedule = s.idSchedule
                WHERE re.environment.idEnvironment = :idEnvironment
                AND re.active = 'ACTIVE'
                AND s.status = 'ACTIVE'
            """)
    List<Schedule> findByEnvironment(@Param("idEnvironment") UUID idEnvironment);

    // Busca horarios de un instructor
    @Query("""
                SELECT s FROM Schedule s
                JOIN ScheduleInstructor si ON si.schedule.idSchedule = s.idSchedule
                WHERE si.user.idUser = :idUser
                AND si.status = 'ACTIVE'
                AND s.status = 'ACTIVE'
            """)
    List<Schedule> findByInstructor(@Param("idUser") UUID idUser);

    // Busca horarios del aprendiz a través de su ficha activa
    @Query("""
                SELECT s FROM Schedule s
                JOIN UserChip uc ON uc.chip.idChip = s.chip.idChip
                WHERE uc.user.idUser = :idUser
                AND uc.state = 'ACTIVE'
                AND s.status = 'ACTIVE'
            """)
    List<Schedule> findByApprentice(@Param("idUser") UUID idUser);

    // Admin busca horarios de un aprendiz especifico por su UUID
    @Query("""
                SELECT s FROM Schedule s
                JOIN UserChip uc ON uc.chip.idChip = s.chip.idChip
                WHERE uc.user.idUser = :idUser
                AND uc.state = 'ACTIVE'
                AND s.status = 'ACTIVE'
            """)
    List<Schedule> findByApprenticeId(@Param("idUser") UUID idUser);

}
