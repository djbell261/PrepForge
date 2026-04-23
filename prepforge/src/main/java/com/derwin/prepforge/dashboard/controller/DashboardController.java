package com.derwin.prepforge.dashboard.controller;

import com.derwin.prepforge.dashboard.dto.DashboardResponse;
import com.derwin.prepforge.dashboard.dto.DashboardCoachingSummaryResponse;
import com.derwin.prepforge.dashboard.service.DashboardCoachingSummaryService;
import com.derwin.prepforge.dashboard.service.DashboardService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardCoachingSummaryService dashboardCoachingSummaryService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@RequestParam UUID userId) {
        return ResponseEntity.ok(dashboardService.getDashboard(userId));
    }

    @GetMapping("/coaching-summary")
    public ResponseEntity<DashboardCoachingSummaryResponse> getCoachingSummary() {
        return ResponseEntity.ok(dashboardCoachingSummaryService.getCoachingSummary());
    }
}
