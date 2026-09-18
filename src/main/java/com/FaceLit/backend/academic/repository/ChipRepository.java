package com.FaceLit.backend.academic.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.enums.AcademicState;

public interface ChipRepository extends JpaRepository<Chip, UUID> {

    boolean existsByChipCode(String chipCode);

    long countByProgram_IdProgram(UUID idProgram);

    long countByProgram_IdProgramAndState(UUID idProgram, AcademicState state);
}