package com.FaceLit.backend.auth.service.security;

import com.FaceLit.backend.auth.model.enums.RoleName;

public record InitialCredentialsEmailRequested(
        String email,
        String firstName,
        String lastName,
        String role,
        String loginIdentifier,
        String initialPassword,
        String fichaCode,
        String programName) {

    public InitialCredentialsEmailRequested(
            String email,
            String firstName,
            String lastName,
            RoleName role,
            String loginIdentifier,
            String initialPassword,
            String fichaCode,
            String programName) {
        this(email, firstName, lastName, role.name(), loginIdentifier, initialPassword, fichaCode, programName);
    }
}
