package com.FaceLit.backend.facial.service;

import java.time.LocalDate;
import java.util.UUID;

import com.FaceLit.backend.facial.dto.request.FacialEventRequestDTO;
import com.FaceLit.backend.facial.dto.request.FacialAttendanceCaptureRequestDTO;
import com.FaceLit.backend.facial.dto.response.AttendanceMatrixResponseDTO;
import com.FaceLit.backend.facial.dto.response.AttendanceStatusResponseDTO;
import com.FaceLit.backend.facial.dto.response.FacialEventResponseDTO;

public interface FacialEventService {

    FacialEventResponseDTO registerEvent(FacialEventRequestDTO dto, UUID authenticatedUserId);

    FacialEventResponseDTO registerEventFromImage(FacialAttendanceCaptureRequestDTO dto, UUID authenticatedUserId);

    AttendanceStatusResponseDTO getAttendanceStatus(UUID idRecordEnvironment, UUID idApprentice);

    AttendanceMatrixResponseDTO getAttendanceMatrix(UUID idChip, LocalDate dateFrom, LocalDate dateTo, UUID authenticatedUserId);
}
