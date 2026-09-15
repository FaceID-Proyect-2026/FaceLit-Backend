package com.FaceLit.backend.academic.repository.academic;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.academic.model.academic.PendingTransfer;
import com.FaceLit.backend.academic.model.enums.PendingTransferStatus;

public interface PendingTransferRepository extends JpaRepository<PendingTransfer, UUID> {
    List<PendingTransfer> findByStatusOrderByCreatedAtDesc(PendingTransferStatus status);
}
