package com.derwin.prepforge.sessions.repository;

import com.derwin.prepforge.sessions.entity.InterviewSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {
}
