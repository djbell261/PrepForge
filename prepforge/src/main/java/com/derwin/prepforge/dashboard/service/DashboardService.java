package com.derwin.prepforge.dashboard.service;

import com.derwin.prepforge.dashboard.dto.DashboardResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    public DashboardResponse getDashboard(UUID userId) {
        return DashboardResponse.builder()
                .userId(userId.toString())
                .activeSessions(1)
                .completedSessions(0)
                .build();
    }
}
