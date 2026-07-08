package com.FaceLit.backend.academic.repository.academic;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.enums.ChipState;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChipRepository extends JpaRepository<Chip, UUID> {

     // Todas las fichas de un programa
    List<Chip> findByProgram_IdProgram(UUID idProgram);

     // Verifica si ya existe una ficha con ese codigo
    boolean existsByChipCode(String chipCode);

     // Filtra por estado
    List<Chip> findByState(ChipState state);
}
