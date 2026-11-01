package com.deadlineshield.backend.repository;

import com.deadlineshield.backend.model.Milestone;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Milestone> findByToken(UUID token);

    List<Milestone> findAllByProjectId(UUID projectId);
}
