package com.derwin.prepforge.coding.controller;

import com.derwin.prepforge.coding.dto.*;
import com.derwin.prepforge.coding.service.CodingService;
import com.derwin.prepforge.summary.dto.SessionSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coding")
public class CodingController {

    private final CodingService codingService;

    @GetMapping("/analytics")
    public ResponseEntity<CodingAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(codingService.getAnalytics());
    }

    @GetMapping("/questions")
    public ResponseEntity<List<CodingQuestionListItemResponse>> getQuestions() {
        return ResponseEntity.ok(codingService.getQuestions());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<CodingSessionResponse>> getSessionHistory() {
        return ResponseEntity.ok(codingService.getSessionHistory());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<CodingSessionDetailResponse> getSessionDetail(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(codingService.getSessionDetail(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<SessionSummaryResponse> getSessionSummary(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(codingService.getSessionSummary(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/strategy")
    public ResponseEntity<CodingStrategyResponse> saveStrategy(
            @PathVariable UUID sessionId,
            @RequestBody CodingStrategyRequest request) {
        return ResponseEntity.ok(codingService.saveStrategy(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/strategy/evaluate")
    public ResponseEntity<StrategyEvaluationResponse> evaluateStrategy(
            @PathVariable UUID sessionId,
            @RequestBody CodingStrategyRequest request) {
        return ResponseEntity.ok(codingService.evaluateStrategy(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/compare")
    public ResponseEntity<ApproachImplementationComparisonResponse> compareApproachAndImplementation(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(codingService.compareApproachAndImplementation(sessionId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<CodingSessionResponse> startSession(
            @Valid @RequestBody CodingSessionRequest request) {
        return ResponseEntity.ok(codingService.startSession(request));
    }

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<CodingQuestionResponse> getQuestion(@PathVariable UUID questionId) {
        return ResponseEntity.ok(codingService.getQuestion(questionId));
    }

    @GetMapping("/sessions/{sessionId}/problem")
    public ResponseEntity<CodingQuestionResponse> getSessionProblem(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(codingService.getSessionProblem(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/submissions")
    public ResponseEntity<CodingSubmissionResponse> submitSolution(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CodingSubmissionRequest request) {
        return ResponseEntity.ok(codingService.submitSolution(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/submissions")
    public ResponseEntity<List<CodingSubmissionResponse>> getSubmissionHistory(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(codingService.getSubmissionHistory(sessionId));
    }

    @PostMapping("/submissions/{submissionId}/improve")
    public ResponseEntity<CodingImprovementResponse> improveSubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(codingService.improveSubmission(submissionId));
    }

    @PostMapping("/sessions/{sessionId}/run")
    public ResponseEntity<RunCodeResponse> runCode(
            @PathVariable UUID sessionId,
            @RequestBody RunCodeRequest request) {

        return ResponseEntity.ok(codingService.runCode(sessionId, request));
    }
}
