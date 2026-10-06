package com.FaceLit.backend.environment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.FaceLit.backend.environment.model.RecordEnvironment;

public interface RecordEnvironmentRepository extends JpaRepository<RecordEnvironment, UUID> {

    @Query("""
            SELECT DISTINCT record.chip.idChip
            FROM RecordEnvironment record
            WHERE record.instructorScheduled.idInstructor = :idInstructor
               OR record.instructorInCharge.idInstructor = :idInstructor
            """)
    List<UUID> findDistinctChipIdsByInstructor(@Param("idInstructor") UUID idInstructor);

    @Query(value = """
            SELECT DISTINCT instructor_user.id_user_app
            FROM (
                SELECT scheduled.id_user_app
                FROM environment.record_environment record
                JOIN academic.instructor scheduled
                    ON scheduled.id_instructor = record.id_instructor_scheduled
                WHERE record.id_chip = :idChip
                UNION
                SELECT in_charge.id_user_app
                FROM environment.record_environment record
                JOIN academic.instructor in_charge
                    ON in_charge.id_instructor = record.id_instructor_in_charge
                WHERE record.id_chip = :idChip
                  AND record.id_instructor_in_charge IS NOT NULL
            ) instructor_user
            """, nativeQuery = true)
    List<UUID> findDistinctInstructorUserIdsByChip(@Param("idChip") UUID idChip);
}
