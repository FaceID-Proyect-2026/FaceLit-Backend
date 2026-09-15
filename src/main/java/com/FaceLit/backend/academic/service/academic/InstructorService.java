package com.FaceLit.backend.academic.service.academic;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.dto.request.academic.InstructorRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;

public interface InstructorService {

    InstructorResponseDTO create(InstructorRequestDTO dto);

    InstructorResponseDTO update(UUID idInstructor, InstructorRequestDTO dto);

    void delete(UUID idInstructor);

    List<InstructorResponseDTO> findEligibleByProgram(UUID idProgram);
}
