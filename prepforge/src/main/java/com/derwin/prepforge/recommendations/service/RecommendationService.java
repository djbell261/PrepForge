package com.derwin.prepforge.recommendations.service;

import com.derwin.prepforge.recommendations.dto.RecommendationResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    public RecommendationResponse getRecommendation(UUID recommendationId) {
        return RecommendationResponse.builder()
                .recommendationId(recommendationId.toString())
                .type("FOCUS_AREA")
                .summary("Practice more medium difficulty array problems.")
                .build();
    }
}
