package com.FaceLit.backend.academic.dto.response.academic;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;

import lombok.Getter;

@Getter
// Adapter: convierte la relación UserChip a un contrato estable para el frontend.
public class UserChipResponseDTO {

    private final UUID idUserChip;
    private final UUID idUser;
    private final UUID idChip;
    private final String chipCode;
    private final String firstName;
    private final String lastName;
    private final String document;
    private final String email;
    private final String state;
    private final String initialPassword;
    private final OffsetDateTime assignmentDate;

    public UserChipResponseDTO(UserChip userChip) {
        this(userChip, null);
    }

    public UserChipResponseDTO(UserChip userChip, String initialPassword) {
        User user = userChip.getUser();
        Credential credential = user.getCredential();

        this.idUserChip = userChip.getIdUserChip();
        this.idUser = user.getIdUser();
        this.idChip = userChip.getChip().getIdChip();
        this.chipCode = userChip.getChip().getChipCode();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.document = user.getDocumentNumber();
        this.email = credential != null ? credential.getEmail() : null;
        this.state = userChip.getState().name();
        this.initialPassword = initialPassword;
        this.assignmentDate = userChip.getAssignmentDate();
    }
}
