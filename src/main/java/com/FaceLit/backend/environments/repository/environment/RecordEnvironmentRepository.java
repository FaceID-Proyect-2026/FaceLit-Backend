package com.FaceLit.backend.environments.repository.environment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FaceLit.backend.environments.model.environment.RecordEnvironment;

import java.util.UUID;
import java.util.List;

@Repository
public interface RecordEnvironmentRepository extends JpaRepository<RecordEnvironment, UUID> {

    List<RecordEnvironment> findAllByEnvironment_IdEnvironment(UUID idEnvironment);

    // Este es el que usa EnvironmentServiceImpl para validar dependencias
    long countAllByEnvironment_IdEnvironment(UUID idEnvironment);
}