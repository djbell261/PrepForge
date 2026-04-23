package com.derwin.prepforge.summary;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSummaryRepository extends JpaRepository<SessionSummary, UUID> {
    Optional<SessionSummary> findBySessionTypeAndSessionId(SessionSummaryType sessionType, UUID sessionId);

    List<SessionSummary> findTop3ByUserIdAndSessionTypeOrderByUpdatedAtDesc(UUID userId, SessionSummaryType sessionType);

    void deleteBySessionTypeAndSessionId(SessionSummaryType sessionType, UUID sessionId);
}
