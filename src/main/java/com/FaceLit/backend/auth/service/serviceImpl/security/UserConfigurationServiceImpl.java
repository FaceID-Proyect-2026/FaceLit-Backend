package com.FaceLit.backend.auth.service.serviceImpl.security;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.security.UserConfigurationRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserConfigurationResponseDTO;
import com.FaceLit.backend.auth.exception.UserConfigurationException;
import com.FaceLit.backend.auth.model.security.UserConfiguration;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.repository.security.UserConfigurationRepository;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.service.security.UserConfigurationService;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserConfigurationServiceImpl implements UserConfigurationService {

    private final UserConfigurationRepository userConfigurationRepository;
    private final UserRepository userRepository;

    public UserConfigurationServiceImpl(
            UserConfigurationRepository userConfigurationRepository,
            UserRepository userRepository) {
        this.userConfigurationRepository = userConfigurationRepository;
        this.userRepository = userRepository;
    }

    private UserConfigurationResponseDTO toDTO(
            UserConfiguration config, String message) {
        return new UserConfigurationResponseDTO(
                config.getIdUserConfiguration(),
                config.getUser().getIdUser(),
                config.getConfigurationName(),
                config.getDescription(),
                config.isNotificationsActive(),
                config.isDarkMode(),
                config.getLanguage(),
                config.getUpdateDate(),
                message);
    }

    @Override
    @Transactional
    public UserConfigurationResponseDTO createConfiguration(
            UUID userId, UserConfigurationRequestDTO dto) {

        // 1. Verificar que el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserConfigurationException(
                        "Usuario no encontrado"));

        // 2. Verificar que no tenga ya una configuracion
        // Solo se permite UNA configuracion por usuario
        if (userConfigurationRepository.existsByUser_IdUser(userId)) {
            throw new UserConfigurationException(
                    "Ya tienes una configuracion creada. Usa la opcion de actualizar");
        }

        // 3. Crear la configuracion
        UserConfiguration config = new UserConfiguration();
        config.setUser(user);
        config.setConfigurationName(dto.getConfigurationName());
        config.setDescription(dto.getDescription());
        config.setNotificationsActive(dto.isNotificationsActive());
        config.setDarkMode(dto.isDarkMode());
        config.setLanguage(dto.getLanguage());
        config.setUpdateDate(OffsetDateTime.now());

        UserConfiguration saved = userConfigurationRepository.save(config);

        return UserConfigurationResponseDTO.created(
                saved.getIdUserConfiguration(),
                saved.getUser().getIdUser(),
                saved.getConfigurationName(),
                saved.getDescription(),
                saved.isNotificationsActive(),
                saved.isDarkMode(),
                saved.getLanguage(),
                saved.getUpdateDate());

    }

    @Override
    @Transactional
    public UserConfigurationResponseDTO updateConfiguration(
            UUID userId, UserConfigurationRequestDTO dto) {

        // 1. Buscar la configuracion del usuario
        UserConfiguration config = userConfigurationRepository
                .findByUser_IdUser(userId)
                .orElseThrow(() -> new UserConfigurationException(
                        "No tienes una configuracion creada. Crea una primero"));

        // 2. Actualizar los campos
        config.setConfigurationName(dto.getConfigurationName());
        config.setDescription(dto.getDescription());
        config.setNotificationsActive(dto.isNotificationsActive());
        config.setDarkMode(dto.isDarkMode());
        config.setLanguage(dto.getLanguage());
        // Actualiza la fecha de modificacion
        config.setUpdateDate(OffsetDateTime.now());

        UserConfiguration updated = userConfigurationRepository.save(config);

        return UserConfigurationResponseDTO.updated(
                updated.getIdUserConfiguration(),
                updated.getUser().getIdUser(),
                updated.getConfigurationName(),
                updated.getDescription(),
                updated.isNotificationsActive(),
                updated.isDarkMode(),
                updated.getLanguage(),
                updated.getUpdateDate());
    }

    @Override
    public UserConfigurationResponseDTO getConfiguration(UUID userId) {
        UserConfiguration config = userConfigurationRepository
                .findByUser_IdUser(userId)
                .orElseThrow(() -> new UserConfigurationException(
                        "No tienes una configuracion creada todavia"));

        return toDTO(config, null);
    }

}
