package com.FaceLit.backend.face.service.facialrecognition;

import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.face.dto.request.facialrecognition.UserFaceRequestDTO;
import com.FaceLit.backend.face.dto.response.facialrecognition.UserFaceResponseDTO;
import com.FaceLit.backend.face.model.enums.FaceStatus;

public interface UserFaceService {

    UserFaceResponseDTO createUserFace(UserFaceRequestDTO dto);

    UserFaceResponseDTO updateUserFace(UUID id, UserFaceRequestDTO dto);

    List<UserFaceResponseDTO> getAllUserFaces();

    UserFaceResponseDTO getUserFaceById(UUID id);

    List<UserFaceResponseDTO> getUserFacesByUser(UUID idUserApp);

    List<UserFaceResponseDTO> getUserFacesByStatus(FaceStatus status);

    void deleteUserFace(UUID id);
}
