package com.FaceLit.backend.face.repository.facialrecognition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.face.model.enums.FaceStatus;
import com.FaceLit.backend.face.model.facialrecognition.UserFace;

@Repository
public interface UserFaceRepository extends JpaRepository<UserFace, UUID> {

    List<UserFace> findByUser_IdUser(UUID idUserApp);

    List<UserFace> findByStatus(FaceStatus status);

    Optional<UserFace> findByUser_IdUserAndStatus(UUID idUserApp, FaceStatus status);
}
