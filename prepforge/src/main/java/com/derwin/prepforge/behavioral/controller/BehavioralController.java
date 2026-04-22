package com.derwin.prepforge.behavioral.controller;

import com.derwin.prepforge.behavioral.dto.BehavioralImproveRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralImproveResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralQuestionResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralAnalyticsResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSubmissionCreateRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionDetailResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSubmissionRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSubmissionResponse;
import com.derwin.prepforge.behavioral.service.BehavioralService;
import com.derwin.prepforge.summary.dto.SessionSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/behavioral")
public class BehavioralController {

    private final BehavioralService behavioralService;

    @GetMapping("/questions")
    public ResponseEntity<List<BehavioralQuestionResponse>> getQuestions() {
        return ResponseEntity.ok(behavioralService.getQuestions());
    }

    @GetMapping("/analytics/me")
    public ResponseEntity<BehavioralAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(behavioralService.getAnalytics());
    }

    @PostMapping("/sessions")
    public ResponseEntity<BehavioralSessionResponse> startSession(
            @Valid @RequestBody BehavioralSessionRequest request) {
        return ResponseEntity.ok(behavioralService.startSession(request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<BehavioralSessionDetailResponse> getSessionDetail(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(behavioralService.getSessionDetail(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<SessionSummaryResponse> getSessionSummary(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(behavioralService.getSessionSummary(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/submissions")
    public ResponseEntity<BehavioralSubmissionResponse> submitResponse(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BehavioralSubmissionRequest request) {
        return ResponseEntity.ok(behavioralService.submitResponse(sessionId, request));
    }

    @PostMapping("/submissions")
    public ResponseEntity<BehavioralSubmissionResponse> submitResponse(
            @Valid @RequestBody BehavioralSubmissionCreateRequest request) {
        return ResponseEntity.ok(behavioralService.submitResponse(request.getSessionId(), request));
    }

    @PostMapping("/improve")
    public ResponseEntity<BehavioralImproveResponse> improveResponse(
            @Valid @RequestBody BehavioralImproveRequest request) {
        return ResponseEntity.ok(behavioralService.improveResponse(request));
    }
}
