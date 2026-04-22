package com.derwin.prepforge.recommendations.repository;

import com.derwin.prepforge.recommendations.entity.Recommendation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
}
