package com.FaceLit.backend.environment.service;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;
import com.FaceLit.backend.environment.dto.request.EnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.request.RecordEnvironmentRequestDTO;
import com.FaceLit.backend.environment.dto.response.EnvironmentResponseDTO;
import com.FaceLit.backend.environment.dto.response.RecordEnvironmentResponseDTO;

public interface EnvironmentService {

    List<EnvironmentResponseDTO> searchEnvironments(String search);

    EnvironmentResponseDTO getOrCreateEnvironment(EnvironmentRequestDTO dto, UUID authenticatedUserId);

    List<InstructorResponseDTO> findInstructorsForSession(UUID authenticatedUserId);

    List<ChipResponseDTO> findChipsForInstructor(UUID idInstructor);

    RecordEnvironmentResponseDTO createSession(RecordEnvironmentRequestDTO dto, UUID authenticatedUserId);

    RecordEnvironmentResponseDTO updateSession(UUID idRecordEnvironment, RecordEnvironmentRequestDTO dto, UUID authenticatedUserId);
}
