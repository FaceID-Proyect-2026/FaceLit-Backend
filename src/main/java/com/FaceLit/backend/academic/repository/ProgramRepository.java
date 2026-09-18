package com.FaceLit.backend.academic.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.academic.Program;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    boolean existsByProgramNameIgnoreCase(String programName);
    boolean existsByProgramNameIgnoreCaseAndIdProgramNot(String programName, UUID idProgram);
    boolean existsByProgramCodeIgnoreCase(String programCode);
    boolean existsByProgramCodeIgnoreCaseAndIdProgramNot(String programCode, UUID idProgram);
}