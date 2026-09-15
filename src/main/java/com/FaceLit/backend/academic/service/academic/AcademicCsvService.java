package com.FaceLit.backend.academic.service.academic;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.FaceLit.backend.academic.dto.response.academic.CsvImportSummaryDTO;
import com.FaceLit.backend.academic.model.academic.PendingTransfer;

public interface AcademicCsvService {
    CsvImportSummaryDTO importFile(MultipartFile file);
    PendingTransfer confirmTransfer(UUID id);
    PendingTransfer cancelTransfer(UUID id);
}
