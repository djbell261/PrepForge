package com.derwin.prepforge.coding.repository;

import com.derwin.prepforge.coding.entity.CodingSubmission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, UUID> {
    List<CodingSubmission> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<CodingSubmission> findBySessionIdOrderBySubmittedAtDesc(UUID sessionId);
}
