package com.FaceLit.backend.academic.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.AcademicState;

public interface UserChipRepository extends JpaRepository<UserChip, UUID> {

    boolean existsByApprentice_User_IdUserAndState(UUID idUser, AcademicState state);

    boolean existsByApprentice_IdApprenticeAndChip_IdChipAndState(
            UUID idApprentice,
            UUID idChip,
            AcademicState state);

    long countByChip_IdChip(UUID idChip);

    long countByChip_IdChipAndState(UUID idChip, AcademicState state);

    Optional<UserChip> findByApprentice_User_IdUserAndState(UUID idUser, AcademicState state);

    List<UserChip> findByChip_IdChip(UUID idChip);

    @EntityGraph(attributePaths = {"apprentice", "apprentice.user", "apprentice.user.credential", "chip", "chip.program"})
    List<UserChip> findByChip_IdChipInAndState(List<UUID> idChips, AcademicState state);

    List<UserChip> findByApprentice_User_IdUser(UUID idUser);
}
