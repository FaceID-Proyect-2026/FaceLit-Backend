package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.exception.RegisterException;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.roleandpermission.Role;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.roleandpermission.RoleRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.service.security.InstitutionalAccountService;

import jakarta.transaction.Transactional;

@Service
public class InstitutionalAccountServiceImpl implements InstitutionalAccountService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public InstitutionalAccountServiceImpl(UserRepository userRepository,
            CredentialRepository credentialRepository, PasswordEncoder passwordEncoder,
            RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional
    public ProvisionedAccount create(String firstName, String lastName, String documentNumber,
            String email, RoleName role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User existing = userRepository.findByDocumentNumber(documentNumber).orElse(null);
        if (existing != null) {
            Credential existingCredential = credentialRepository.findByUser(existing)
                    .orElseThrow(() -> new RegisterException("La cuenta existente no tiene credencial configurada"));
            if (!existingCredential.getEmail().trim().equalsIgnoreCase(normalizedEmail)) {
                throw new RegisterException("El documento ya está registrado con otro correo");
            }
            RoleName currentRole = userRoleRepository.findByUserId(existing.getIdUser())
                    .map(userRole -> userRole.getRole().getNameRole()).orElse(null);
            if (currentRole != role) {
                throw new RegisterException("El documento ya está registrado con otro rol");
            }
            return new ProvisionedAccount(existing, null, false);
        }
        if (credentialRepository.existsByEmail(normalizedEmail)) {
            throw new RegisterException("El correo ya está registrado para otro usuario");
        }
        if (role == RoleName.COORDINATOR) {
            throw new RegisterException("El rol COORDINATOR no se crea desde la carga académica");
        }
        Role assignedRole = roleRepository.findByNameRole(role)
                .orElseThrow(() -> new RegisterException("El rol indicado no está configurado"));

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setDocumentNumber(documentNumber);
        user.setAccountStatus(AccountStatus.ACTIVE);
        User saved = userRepository.save(user);

        String temporaryPassword = generateTemporaryPassword();
        Credential credential = new Credential();
        credential.setEmail(normalizedEmail);
        credential.setPassword(passwordEncoder.encode(temporaryPassword));
        credential.setCredentialStatus(CredentialStatus.ACTIVE);
        credential.setFailedAttempts(0);
        credential.setUser(saved);
        credentialRepository.save(credential);

        UserRole userRole = new UserRole();
        userRole.setUser(saved);
        userRole.setRole(assignedRole);
        userRole.setAssignedAt(java.time.OffsetDateTime.now());
        userRoleRepository.save(userRole);
        return new ProvisionedAccount(saved, temporaryPassword, true);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int index = 0; index < 12; index++) {
            password.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
