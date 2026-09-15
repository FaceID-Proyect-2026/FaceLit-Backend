package com.FaceLit.backend.academic.dto.response.academic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

@Getter
public class CsvImportSummaryDTO {
    public record TemporaryPasswordDTO(String document, String name, String role, String temporaryPassword) {}
    private int created;
    private int updated;
    private int blocked;
    private int errors;
    private int pendingTransfers;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    private final List<UUID> pendingTransferIds = new ArrayList<>();

    private final List<TemporaryPasswordDTO> temporaryPasswords = new ArrayList<>();
    public void created() { created++; }
    public void updated() { updated++; }
    public void blocked() { blocked++; }
    public void error(String message) { errors++; messages.add(message); }
    public void warning(String message) { warnings.add(message); }
    public void pending(UUID id) { pendingTransfers++; pendingTransferIds.add(id); }

    public void temporaryPassword(String document, String name, String role, String password) {
        temporaryPasswords.add(new TemporaryPasswordDTO(document, name, role, password));
    }
}
