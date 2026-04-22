package com.derwin.prepforge.summary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSummaryRepository extends JpaRepository<SessionSummary, UUID> {
    Optional<SessionSummary> findBySessionTypeAndSessionId(SessionSummaryType sessionType, UUID sessionId);

    void deleteBySessionTypeAndSessionId(SessionSummaryType sessionType, UUID sessionId);
}
