package com.derwin.prepforge.behavioral.repository;

import com.derwin.prepforge.behavioral.entity.BehavioralQuestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehavioralQuestionRepository extends JpaRepository<BehavioralQuestion, UUID> {
}
