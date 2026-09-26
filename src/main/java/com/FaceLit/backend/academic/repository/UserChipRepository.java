package com.FaceLit.backend.academic.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.AcademicState;

public interface UserChipRepository extends JpaRepository<UserChip, UUID> {

    boolean existsByUser_IdUserAndState(UUID idUser, AcademicState state);

    long countByChip_IdChip(UUID idChip);

    long countByChip_IdChipAndState(UUID idChip, AcademicState state);

    Optional<UserChip> findByUser_IdUserAndState(UUID idUser, AcademicState state);

    List<UserChip> findByChip_IdChip(UUID idChip);

    @EntityGraph(attributePaths = {"user", "user.credential", "chip", "chip.program"})
    List<UserChip> findByChip_IdChipInAndState(List<UUID> idChips, AcademicState state);

    List<UserChip> findByUser_IdUser(UUID idUser);
}
