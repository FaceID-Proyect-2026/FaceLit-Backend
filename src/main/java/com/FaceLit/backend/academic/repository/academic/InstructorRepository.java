package com.FaceLit.backend.academic.repository.academic;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.academic.Instructor;

public interface InstructorRepository extends JpaRepository<Instructor, UUID> {

    boolean existsByUser_IdUser(UUID idUser);

    Optional<Instructor> findByUser_IdUser(UUID idUser);
}
