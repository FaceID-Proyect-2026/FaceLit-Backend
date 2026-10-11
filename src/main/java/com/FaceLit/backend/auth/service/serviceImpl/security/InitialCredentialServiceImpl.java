package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.security.SecureRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.service.security.InitialCredentialService;
import com.FaceLit.backend.auth.service.security.InitialCredentialsEmailRequested;

@Service
public class InitialCredentialServiceImpl implements InitialCredentialService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?";
    private static final String SYMBOLS = "@$!%*?";

    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public InitialCredentialServiceImpl(PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher) {
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String generateInitialPassword() {
        StringBuilder password = new StringBuilder();
        password.append((char) ('A' + RANDOM.nextInt(26)));
        password.append(RANDOM.nextInt(10));
        password.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));
        while (password.length() < 12) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }

    @Override
    public void encodeGeneratedPassword(Credential credential, String rawPassword) {
        credential.setPassword(passwordEncoder.encode(rawPassword));
    }

    @Override
    public void sendWelcomeAfterCommit(User user, Credential credential, RoleName role, String rawPassword,
            String fichaCode, String programName) {
        eventPublisher.publishEvent(new InitialCredentialsEmailRequested(
                credential.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role,
                user.getDocumentNumber(),
                rawPassword,
                fichaCode,
                programName));
    }
}
