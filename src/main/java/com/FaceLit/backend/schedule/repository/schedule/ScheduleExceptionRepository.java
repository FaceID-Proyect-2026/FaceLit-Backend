package com.FaceLit.backend.schedule.repository.schedule;

import com.FaceLit.backend.schedule.model.schedule.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {

    // Todas las excepciones de un horario
    List<ScheduleException> findBySchedule_IdSchedule(UUID idSchedule);

    // Verifica si ya existe una excepcion activa en ese horario y fecha
    // Evita registrar dos excepciones el mismo dia para el mismo horario
    boolean existsBySchedule_IdScheduleAndExceptionDate(
            UUID idSchedule, LocalDate exceptionDate);

}
