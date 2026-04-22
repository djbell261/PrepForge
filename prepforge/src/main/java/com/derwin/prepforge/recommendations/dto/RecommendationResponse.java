package com.derwin.prepforge.recommendations.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationResponse {
    private final String recommendationId;
    private final String type;
    private final String summary;
}
