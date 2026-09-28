package com.FaceLit.backend.environment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.environment.model.RecordEnvironment;

public interface RecordEnvironmentRepository extends JpaRepository<RecordEnvironment, UUID> {
}
