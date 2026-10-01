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
            WHERE record.instructorInCharge.idInstructor = :idInstructor
            """)
    List<UUID> findDistinctChipIdsByInstructor(@Param("idInstructor") UUID idInstructor);

    @Query("""
            SELECT DISTINCT record.instructorInCharge.user.idUser
            FROM RecordEnvironment record
            WHERE record.chip.idChip = :idChip
            """)
    List<UUID> findDistinctInstructorUserIdsByChip(@Param("idChip") UUID idChip);
}
