package com.FaceLit.backend.auth.service.security;

import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.security.User;

public interface InstitutionalAccountService {

    ProvisionedAccount create(String firstName, String lastName, String documentNumber,
            String email, RoleName role);

    record ProvisionedAccount(User user, String temporaryPassword, boolean created) {
    }
}
