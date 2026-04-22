package com.derwin.prepforge.coding.repository;

import com.derwin.prepforge.coding.entity.CodingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodingSessionRepository extends JpaRepository<CodingSession, UUID> {
    Optional<CodingSession> findByIdAndUserId(UUID id, UUID userId);

    List<CodingSession> findByUserIdOrderByStartedAtDesc(UUID userId);
}
