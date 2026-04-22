package com.derwin.prepforge.coding.service;

import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.coding.dto.CodingAnalyticsResponse;
import com.derwin.prepforge.coding.dto.CodingSessionRequest;
import com.derwin.prepforge.coding.dto.CodingSessionDetailResponse;
import com.derwin.prepforge.coding.dto.CodingSessionResponse;
import com.derwin.prepforge.coding.dto.CodingSubmissionRequest;
import com.derwin.prepforge.coding.dto.CodingSubmissionResponse;
import com.derwin.prepforge.coding.dto.CodingQuestionResponse;
import com.derwin.prepforge.coding.entity.CodingQuestion;
import com.derwin.prepforge.coding.entity.CodingSession;
import com.derwin.prepforge.coding.entity.CodingSessionStatus;
import com.derwin.prepforge.coding.entity.CodingSubmission;
import com.derwin.prepforge.coding.entity.SubmissionStatus;
import com.derwin.prepforge.coding.repository.CodingQuestionRepository;
import com.derwin.prepforge.coding.repository.CodingSessionRepository;
import com.derwin.prepforge.coding.repository.CodingSubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CodingService {

    private final CodingQuestionRepository codingQuestionRepository;
    private final CodingSessionRepository codingSessionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CodingSessionResponse startSession(CodingSessionRequest request) {
        UUID userId = getCurrentUser().getId();
        CodingQuestion question = getQuestionEntity(request.getQuestionId());

        CodingSession session = codingSessionRepository.save(CodingSession.builder()
                .userId(userId)
                .questionId(question.getId())
                .status(CodingSessionStatus.STARTED)
                .startedAt(Instant.now())
                .build());

        return mapSession(session);
    }

    @Transactional(readOnly = true)
    public List<CodingSessionResponse> getSessionHistory() {
        UUID userId = getCurrentUser().getId();

        return codingSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(this::mapSession)
                .toList();
    }

    @Transactional(readOnly = true)
    public CodingAnalyticsResponse getAnalytics() {
        UUID userId = getCurrentUser().getId();
        List<CodingSubmission> submissions = codingSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        long totalSessionsCount = codingSessionRepository.findByUserIdOrderByStartedAtDesc(userId).size();

        List<Integer> scores = submissions.stream()
                .map(this::extractScore)
                .filter(score -> score != null)
                .toList();

        Double averageScore = scores.isEmpty()
                ? null
                : scores.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);

        Integer latestSubmissionScore = submissions.stream()
                .map(this::extractScore)
                .filter(score -> score != null)
                .findFirst()
                .orElse(null);

        return CodingAnalyticsResponse.builder()
                .averageScore(averageScore)
                .totalSessionsCount(totalSessionsCount)
                .latestSubmissionScore(latestSubmissionScore)
                .build();
    }

    @Transactional(readOnly = true)
    public CodingSessionDetailResponse getSessionDetail(UUID sessionId) {
        CodingSession session = getOwnedSession(sessionId);
        CodingQuestion question = getQuestionEntity(session.getQuestionId());
        List<CodingSubmissionResponse> submissions = codingSubmissionRepository
                .findBySessionIdOrderBySubmittedAtDesc(session.getId()).stream()
                .map(this::mapSubmission)
                .toList();

        return CodingSessionDetailResponse.builder()
                .session(mapSession(session))
                .question(mapQuestion(question))
                .submissions(submissions)
                .build();
    }

    @Transactional(readOnly = true)
    public CodingQuestionResponse getQuestion(UUID questionId) {
        return mapQuestion(getQuestionEntity(questionId));
    }

    @Transactional(readOnly = true)
    public CodingQuestionResponse getSessionProblem(UUID sessionId) {
        CodingSession session = getOwnedSession(sessionId);
        return mapQuestion(getQuestionEntity(session.getQuestionId()));
    }

    @Transactional
    public CodingSubmissionResponse submitSolution(UUID sessionId, CodingSubmissionRequest request) {
        CodingSession session = getOwnedSession(sessionId);
        CodingQuestion question = getQuestionEntity(session.getQuestionId());

        CodingSubmission submission = CodingSubmission.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .language(request.getLanguage())
                .solutionCode(request.getSolutionCode())
                .status(SubmissionStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build();

        String aiFeedback = aiService.generateSubmissionFeedback(question, submission);

        submission.setAiFeedback(aiFeedback);
        submission.setStatus(SubmissionStatus.REVIEWED);

        CodingSubmission savedSubmission = codingSubmissionRepository.save(submission);
        session.setStatus(CodingSessionStatus.COMPLETED);
        codingSessionRepository.save(session);

        return mapSubmission(savedSubmission);
    }

    @Transactional(readOnly = true)
    public List<CodingSubmissionResponse> getSubmissionHistory(UUID sessionId) {
        CodingSession session = getOwnedSession(sessionId);

        return codingSubmissionRepository.findBySessionIdOrderBySubmittedAtDesc(session.getId()).stream()
                .map(this::mapSubmission)
                .toList();
    }

    private CodingQuestion getQuestionEntity(UUID questionId) {
        return codingQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coding question not found"));
    }

    private CodingSession getOwnedSession(UUID sessionId) {
        UUID userId = getCurrentUser().getId();

        return codingSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coding session not found"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found");
        }

        return user;
    }

    private CodingQuestionResponse mapQuestion(CodingQuestion question) {
        return CodingQuestionResponse.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .difficulty(question.getDifficulty())
                .prompt(question.getPrompt())
                .build();
    }

    private CodingSubmissionResponse mapSubmission(CodingSubmission submission) {
        return CodingSubmissionResponse.builder()
                .submissionId(submission.getId())
                .sessionId(submission.getSessionId())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .aiFeedback(submission.getAiFeedback())
                .build();
    }

    private CodingSessionResponse mapSession(CodingSession session) {
        return CodingSessionResponse.builder()
                .sessionId(session.getId())
                .questionId(session.getQuestionId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .build();
    }

    private Integer extractScore(CodingSubmission submission) {
        try {
            if (submission.getAiFeedback() == null || submission.getAiFeedback().isBlank()) {
                return null;
            }

            JsonNode feedback = objectMapper.readTree(submission.getAiFeedback());
            JsonNode scoreNode = feedback.get("score");

            return scoreNode != null && scoreNode.isInt() ? scoreNode.asInt() : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
