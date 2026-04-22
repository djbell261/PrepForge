package com.derwin.prepforge.coding.repository;

import com.derwin.prepforge.coding.entity.CodingQuestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodingQuestionRepository extends JpaRepository<CodingQuestion, UUID> {
}
