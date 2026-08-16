package com.FaceLit.backend.face.service.serviceImpl.facialrecognition;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.face.dto.request.facialrecognition.UserFaceRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.UserFaceResponseDTO;
import com.FaceLit.backend.face.model.enums.FaceStatus;
import com.FaceLit.backend.face.model.facialrecognition.UserFace;
import com.FaceLit.backend.face.repository.facialrecognition.UserFaceRepository;
import com.FaceLit.backend.face.service.facialrecognition.UserFaceService;

import jakarta.transaction.Transactional;

@Service
public class UserFaceServiceImpl implements UserFaceService {

    private final UserFaceRepository userFaceRepository;
    private final UserRepository userRepository;

    public UserFaceServiceImpl(UserFaceRepository userFaceRepository, UserRepository userRepository) {
        this.userFaceRepository = userFaceRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserFaceResponseDTO createUserFace(UserFaceRequestDTO dto) {
        User user = userRepository.findById(dto.getIdUserApp())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserFace userFace = new UserFace();
        userFace.setUser(user);
        userFace.setBiometricVector(dto.getBiometricVector());
        userFace.setRegistrationDate(dto.getRegistrationDate());
        userFace.setStatus(dto.getStatus() != null ? dto.getStatus() : FaceStatus.PENDING);

        UserFace saved = userFaceRepository.save(userFace);

        return new UserFaceResponseDTO(
                saved.getIdUserFace(),
                saved.getUser().getIdUser(),
                saved.getStatus(),
                saved.getRegistrationDate(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                "Cara biométrica registrada correctamente");
    }

    @Override
    @Transactional
    public UserFaceResponseDTO updateUserFace(UUID id, UserFaceRequestDTO dto) {
        UserFace userFace = userFaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cara biométrica no encontrada"));

        if (dto.getIdUserApp() != null) {
            User user = userRepository.findById(dto.getIdUserApp())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            userFace.setUser(user);
        }

        if (dto.getBiometricVector() != null) {
            userFace.setBiometricVector(dto.getBiometricVector());
        }

        if (dto.getRegistrationDate() != null) {
            userFace.setRegistrationDate(dto.getRegistrationDate());
        }

        if (dto.getStatus() != null) {
            userFace.setStatus(dto.getStatus());
        }

        UserFace updated = userFaceRepository.save(userFace);

        return new UserFaceResponseDTO(
                updated.getIdUserFace(),
                updated.getUser().getIdUser(),
                updated.getStatus(),
                updated.getRegistrationDate(),
                updated.getCreatedAt(),
                updated.getUpdatedAt(),
                "Cara biométrica actualizada correctamente");
    }

    @Override
    public List<UserFaceResponseDTO> getAllUserFaces() {
        return userFaceRepository.findAll().stream()
                .map(userFace -> new UserFaceResponseDTO(
                        userFace.getIdUserFace(),
                        userFace.getUser() != null ? userFace.getUser().getIdUser() : null,
                        userFace.getStatus(),
                        userFace.getRegistrationDate(),
                        userFace.getCreatedAt(),
                        userFace.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public UserFaceResponseDTO getUserFaceById(UUID id) {
        UserFace userFace = userFaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cara biométrica no encontrada"));

        return new UserFaceResponseDTO(
                userFace.getIdUserFace(),
                userFace.getUser() != null ? userFace.getUser().getIdUser() : null,
                userFace.getStatus(),
                userFace.getRegistrationDate(),
                userFace.getCreatedAt(),
                userFace.getUpdatedAt(),
                null);
    }

    @Override
    public List<UserFaceResponseDTO> getUserFacesByUser(UUID idUserApp) {
        return userFaceRepository.findByUser_IdUser(idUserApp).stream()
                .map(userFace -> new UserFaceResponseDTO(
                        userFace.getIdUserFace(),
                        userFace.getUser() != null ? userFace.getUser().getIdUser() : null,
                        userFace.getStatus(),
                        userFace.getRegistrationDate(),
                        userFace.getCreatedAt(),
                        userFace.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserFaceResponseDTO> getUserFacesByStatus(FaceStatus status) {
        return userFaceRepository.findByStatus(status).stream()
                .map(userFace -> new UserFaceResponseDTO(
                        userFace.getIdUserFace(),
                        userFace.getUser() != null ? userFace.getUser().getIdUser() : null,
                        userFace.getStatus(),
                        userFace.getRegistrationDate(),
                        userFace.getCreatedAt(),
                        userFace.getUpdatedAt(),
                        null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUserFace(UUID id) {
        UserFace userFace = userFaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cara biométrica no encontrada"));

        userFace.setStatus(FaceStatus.INACTIVE);
        userFaceRepository.save(userFace);
    }
}
