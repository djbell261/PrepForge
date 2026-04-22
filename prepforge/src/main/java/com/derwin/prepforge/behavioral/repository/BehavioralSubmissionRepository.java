package com.derwin.prepforge.behavioral.repository;

import com.derwin.prepforge.behavioral.entity.BehavioralSubmission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehavioralSubmissionRepository extends JpaRepository<BehavioralSubmission, UUID> {
    List<BehavioralSubmission> findBySessionIdOrderBySubmittedAtDesc(UUID sessionId);
}
