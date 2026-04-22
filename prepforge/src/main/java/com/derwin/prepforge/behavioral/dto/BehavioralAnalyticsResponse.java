package com.derwin.prepforge.behavioral.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehavioralAnalyticsResponse {
    private Double averageScore;
    private Long attemptsCount;
    private Map<String, Integer> categoryBreakdown;
    private String weakestCategory;
}
