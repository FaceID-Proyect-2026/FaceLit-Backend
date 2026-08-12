package com.FaceLit.backend.schedule.repository.schedule;

import com.FaceLit.backend.schedule.model.schedule.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {

    // Todas las excepciones de un horario
    List<ScheduleException> findBySchedule_IdSchedule(UUID idSchedule);

    // Detecta solapamiento entre el rango nuevo [exceptionDate, endDate]
    // y el rango de cualquier excepción activa ya registrada para ese horario.
    // Si endDate viene null, se trata como un rango de un solo día (COALESCE lo
    // cubre).
    @Query("""
                SELECT COUNT(se) > 0
                FROM ScheduleException se
                WHERE se.schedule.idSchedule = :idSchedule
                AND se.status = 'ACTIVE'
                AND :exceptionDate <= COALESCE(se.endDate, se.exceptionDate)
                AND COALESCE(:endDate, :exceptionDate) >= se.exceptionDate
            """)
    boolean existsOverlappingException(
            @Param("idSchedule") UUID idSchedule,
            @Param("exceptionDate") LocalDate exceptionDate,
            @Param("endDate") LocalDate endDate);

}