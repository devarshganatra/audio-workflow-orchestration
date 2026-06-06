package com.devarsh.audio_workflow.repository;

import com.devarsh.audio_workflow.domain.Workflow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow,Long> {
    Optional<Workflow> findByExternalId(UUID externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Workflow> findById(Long id);

}
