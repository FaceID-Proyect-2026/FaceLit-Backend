package com.FaceLit.backend.academic.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.academic.InstructorProgram;

public interface InstructorProgramRepository extends JpaRepository<InstructorProgram, UUID> {

    boolean existsByInstructor_IdInstructorAndProgram_IdProgram(UUID idInstructor, UUID idProgram);

    long countByProgram_IdProgram(UUID idProgram);

    long countByInstructor_IdInstructor(UUID idInstructor);
}