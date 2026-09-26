package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.security.ChangePasswordRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.PasswordRecoveryResponseDTO;
import com.FaceLit.backend.auth.exception.ChangePasswordException;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.security.ChangePasswordService;

import jakarta.transaction.Transactional;

@Service
public class ChangePasswordServiceImpl implements ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public PasswordRecoveryResponseDTO changePassword(UUID userId, ChangePasswordRequestDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new ChangePasswordException("Las contraseñas no coinciden");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChangePasswordException("Usuario no encontrado"));
        Credential credential = user.getCredential();
        if (credential == null) {
            throw new ChangePasswordException("El usuario no tiene credenciales configuradas");
        }

        credential.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        return PasswordRecoveryResponseDTO.passwordReset();
    }
}