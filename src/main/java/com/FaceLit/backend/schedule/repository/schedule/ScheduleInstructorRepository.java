package com.FaceLit.backend.schedule.repository.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.FaceLit.backend.environments.model.environment.RecordEnvironment;
import com.FaceLit.backend.schedule.model.enums.ScheduleStatus;
import com.FaceLit.backend.schedule.model.schedule.ScheduleInstructor;

@Repository
public interface ScheduleInstructorRepository extends JpaRepository<ScheduleInstructor, UUID> {

        // Busca Instructor activo de un horario
        Optional<ScheduleInstructor> findBySchedule_IdScheduleAndStatus(
                        UUID idSchedule, ScheduleStatus status);

        // Busca todos los horarios de un instructor
        List<ScheduleInstructor> findByUser_IdUser(UUID idUser);

        // Busca todos los instructores de un horario sin filtrar por status
        List<ScheduleInstructor> findBySchedule_IdSchedule(UUID idSchedule);

}
