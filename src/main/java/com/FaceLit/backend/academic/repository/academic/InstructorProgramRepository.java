package com.FaceLit.backend.academic.repository.academic;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.academic.InstructorProgram;

public interface InstructorProgramRepository extends JpaRepository<InstructorProgram, UUID> {

    boolean existsByInstructor_IdInstructorAndProgram_IdProgram(UUID idInstructor, UUID idProgram);

    List<InstructorProgram> findByInstructor_IdInstructor(UUID idInstructor);

    List<InstructorProgram> findByProgram_IdProgram(UUID idProgram);

    void deleteByInstructor_IdInstructor(UUID idInstructor);
}
