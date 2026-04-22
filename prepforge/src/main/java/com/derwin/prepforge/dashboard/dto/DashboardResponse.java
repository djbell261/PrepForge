package com.derwin.prepforge.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {
    private final String userId;
    private final int activeSessions;
    private final int completedSessions;
}
