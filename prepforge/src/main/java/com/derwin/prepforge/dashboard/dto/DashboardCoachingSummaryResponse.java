package com.derwin.prepforge.dashboard.dto;

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
public class DashboardCoachingSummaryResponse {
    private String headline;
    private String strongestArea;
    private String weakestArea;
    private String recommendedNextStep;
    private String recommendedSessionType;
    private String pressureInsight;
    private String confidenceNote;
}
