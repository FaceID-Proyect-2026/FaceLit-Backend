package com.FaceLit.backend.academic.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.ProgramState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    // Verifica si ya existe un programa con ese nombre
    boolean existsByProgramName(String programName);

    boolean existsByProgramCode(String programCode);

    // Busca por nombre ignorando mayúsculas — para consulta
    Optional<Program> findByProgramNameIgnoreCase(String programName);

    Optional<Program> findByProgramCodeIgnoreCase(String programCode);

    // Busca por estado — para filtrar
    List<Program> findByState(ProgramState state);

}
