package com.derwin.prepforge.behavioral.repository;

import com.derwin.prepforge.behavioral.entity.BehavioralSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehavioralSessionRepository extends JpaRepository<BehavioralSession, UUID> {
    Optional<BehavioralSession> findByIdAndUserId(UUID id, UUID userId);

    List<BehavioralSession> findByIdIn(List<UUID> ids);
}
