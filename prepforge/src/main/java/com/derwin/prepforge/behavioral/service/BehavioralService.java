package com.derwin.prepforge.behavioral.service;

import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.behavioral.dto.BehavioralImproveRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralImproveResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralAnalyticsResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralFeedbackResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralQuestionResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionDetailResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSubmissionCreateRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSubmissionRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSubmissionResponse;
import com.derwin.prepforge.behavioral.entity.BehavioralQuestion;
import com.derwin.prepforge.behavioral.entity.BehavioralSession;
import com.derwin.prepforge.behavioral.entity.BehavioralSessionStatus;
import com.derwin.prepforge.behavioral.entity.BehavioralSubmission;
import com.derwin.prepforge.behavioral.repository.BehavioralQuestionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSessionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSubmissionRepository;
import com.derwin.prepforge.coding.service.AiService;
import com.derwin.prepforge.infrastructure.redis.TimedSessionType;
import com.derwin.prepforge.infrastructure.redis.TimerState;
import com.derwin.prepforge.infrastructure.redis.TimerStateStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class BehavioralService {
    private static final List<Integer> ALLOWED_TIME_LIMITS = List.of(300, 600, 900);

    private final BehavioralQuestionRepository behavioralQuestionRepository;
    private final BehavioralSessionRepository behavioralSessionRepository;
    private final BehavioralSubmissionRepository behavioralSubmissionRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final TimerStateStore timerStateStore;

    @Transactional(readOnly = true)
    public List<BehavioralQuestionResponse> getQuestions() {
        return behavioralQuestionRepository.findAllByOrderByCategoryAscDifficultyAscCreatedAtAsc().stream()
                .map(this::mapQuestion)
                .toList();
    }

    @Transactional(readOnly = true)
    public BehavioralAnalyticsResponse getAnalytics() {
        UUID userId = getCurrentUser().getId();
        List<BehavioralSubmission> submissions = behavioralSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);

        if (submissions.isEmpty()) {
            return BehavioralAnalyticsResponse.builder()
                    .averageScore(null)
                    .attemptsCount(0L)
                    .categoryBreakdown(Map.of())
                    .weakestCategory(null)
                    .build();
        }

        Map<UUID, BehavioralSession> sessionsById = behavioralSessionRepository.findByIdIn(
                        submissions.stream().map(BehavioralSubmission::getSessionId).distinct().toList())
                .stream()
                .collect(LinkedHashMap::new, (map, session) -> map.put(session.getId(), session), Map::putAll);

        Map<UUID, BehavioralQuestion> questionsById = behavioralQuestionRepository.findAllById(
                        sessionsById.values().stream().map(BehavioralSession::getQuestionId).distinct().toList())
                .stream()
                .collect(LinkedHashMap::new, (map, question) -> map.put(question.getId(), question), Map::putAll);

        Map<String, Integer> categoryBreakdown = new LinkedHashMap<>();
        Map<String, Integer> categoryScoreTotals = new LinkedHashMap<>();
        Map<String, Integer> categoryScoreCounts = new LinkedHashMap<>();
        int totalScore = 0;
        int scoredAttempts = 0;

        for (BehavioralSubmission submission : submissions) {
            BehavioralSession session = sessionsById.get(submission.getSessionId());
            BehavioralQuestion question = session == null ? null : questionsById.get(session.getQuestionId());
            String category = question != null ? question.getCategory() : "Unknown";
            BehavioralFeedbackResponse feedback = parseFeedback(submission.getAiFeedback());

            categoryBreakdown.merge(category, 1, Integer::sum);

            if (feedback != null && feedback.getScore() != null) {
                totalScore += feedback.getScore();
                scoredAttempts += 1;
                categoryScoreTotals.merge(category, feedback.getScore(), Integer::sum);
                categoryScoreCounts.merge(category, 1, Integer::sum);
            }
        }

        String weakestCategory = categoryScoreTotals.entrySet().stream()
                .min((left, right) -> {
                    double leftAverage = (double) left.getValue() / categoryScoreCounts.get(left.getKey());
                    double rightAverage = (double) right.getValue() / categoryScoreCounts.get(right.getKey());
                    return Double.compare(leftAverage, rightAverage);
                })
                .map(Map.Entry::getKey)
                .orElse(null);

        Double averageScore = scoredAttempts == 0 ? null : (double) totalScore / scoredAttempts;

        return BehavioralAnalyticsResponse.builder()
                .averageScore(averageScore)
                .attemptsCount((long) submissions.size())
                .categoryBreakdown(categoryBreakdown)
                .weakestCategory(weakestCategory)
                .build();
    }

    @Transactional
    public BehavioralSessionResponse startSession(BehavioralSessionRequest request) {
        UUID userId = getCurrentUser().getId();
        BehavioralQuestion question = getQuestionEntity(request.getQuestionId());
        boolean isTimed = Boolean.TRUE.equals(request.getIsTimed());
        Integer timeLimitSeconds = validateTimeLimitSeconds(isTimed, request.getTimeLimitSeconds());
        Instant startedAt = Instant.now();

        BehavioralSession session = behavioralSessionRepository.save(BehavioralSession.builder()
                .userId(userId)
                .questionId(question.getId())
                .status(BehavioralSessionStatus.STARTED)
                .isTimed(isTimed)
                .timeLimitSeconds(timeLimitSeconds)
                .startedAt(startedAt)
                .build());

        cacheTimerState(session);
        return mapSession(session);
    }

    @Transactional(readOnly = true)
    public BehavioralSessionDetailResponse getSessionDetail(UUID sessionId) {
        BehavioralSession session = getOwnedSession(sessionId);
        expireSessionIfNeeded(session);
        BehavioralQuestion question = getQuestionEntity(session.getQuestionId());
        List<BehavioralSubmissionResponse> submissions = behavioralSubmissionRepository
                .findBySessionIdOrderBySubmittedAtDesc(session.getId()).stream()
                .map(this::mapSubmission)
                .toList();

        return BehavioralSessionDetailResponse.builder()
                .session(mapSession(session))
                .question(mapQuestion(question))
                .submissions(submissions)
                .build();
    }

    @Transactional
    public BehavioralSubmissionResponse submitResponse(UUID sessionId, BehavioralSubmissionRequest request) {
        return submitResponseInternal(sessionId, request.getResponseText());
    }

    @Transactional
    public BehavioralSubmissionResponse submitResponse(UUID sessionId, BehavioralSubmissionCreateRequest request) {
        return submitResponseInternal(sessionId, request.getResponseText());
    }

    @Transactional(readOnly = true)
    public BehavioralImproveResponse improveResponse(BehavioralImproveRequest request) {
        return aiService.improveBehavioralResponse(request);
    }

    private BehavioralSubmissionResponse submitResponseInternal(UUID sessionId, String responseText) {
        BehavioralSession session = getOwnedSession(sessionId);
        expireSessionIfNeeded(session);
        if (isSessionExpired(session)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This timed behavioral session has expired. New submissions are no longer allowed.");
        }

        BehavioralQuestion question = getQuestionEntity(session.getQuestionId());
        BehavioralSubmission previousSubmission = behavioralSubmissionRepository
                .findBySessionIdOrderBySubmittedAtDesc(session.getId()).stream()
                .findFirst()
                .orElse(null);
        BehavioralFeedbackResponse feedback = aiService.evaluateBehavioralResponse(
                question,
                previousSubmission == null ? null : previousSubmission.getResponseText(),
                responseText);

        BehavioralSubmission submission = behavioralSubmissionRepository.save(BehavioralSubmission.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .responseText(responseText)
                .aiFeedback(writeFeedback(feedback))
                .submittedAt(Instant.now())
                .build());

        session.setStatus(BehavioralSessionStatus.COMPLETED);
        behavioralSessionRepository.save(session);
        clearTimerState(session);

        return mapSubmission(submission);
    }

    private BehavioralQuestion getQuestionEntity(UUID questionId) {
        return behavioralQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral question not found"));
    }

    private BehavioralSession getOwnedSession(UUID sessionId) {
        UUID userId = getCurrentUser().getId();

        return behavioralSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral session not found"));
    }

    private BehavioralQuestionResponse mapQuestion(BehavioralQuestion question) {
        return BehavioralQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .build();
    }

    private BehavioralSessionResponse mapSession(BehavioralSession session) {
        Optional<TimerState> timerState = resolveTimerState(session);
        Instant expiresAt = timerState.map(TimerState::getExpiresAt).orElse(getExpiresAt(session));
        boolean expired = isSessionExpired(session, expiresAt);

        return BehavioralSessionResponse.builder()
                .sessionId(session.getId())
                .questionId(session.getQuestionId())
                .status(expired && session.getStatus() == BehavioralSessionStatus.STARTED
                        ? BehavioralSessionStatus.EXPIRED.name()
                        : session.getStatus().name())
                .isTimed(session.isTimed())
                .timeLimitSeconds(session.getTimeLimitSeconds())
                .expiresAt(expiresAt)
                .expired(expired)
                .startedAt(session.getStartedAt())
                .build();
    }

    private BehavioralSubmissionResponse mapSubmission(BehavioralSubmission submission) {
        return BehavioralSubmissionResponse.builder()
                .submissionId(submission.getId())
                .responseText(submission.getResponseText())
                .feedback(parseFeedback(submission.getAiFeedback()))
                .submittedAt(submission.getSubmittedAt())
                .build();
    }

    private BehavioralFeedbackResponse parseFeedback(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(rawFeedback, BehavioralFeedbackResponse.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String writeFeedback(BehavioralFeedbackResponse feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store behavioral feedback", exception);
        }
    }

    private Integer validateTimeLimitSeconds(boolean isTimed, Integer timeLimitSeconds) {
        if (!isTimed) {
            return null;
        }

        if (timeLimitSeconds == null || !ALLOWED_TIME_LIMITS.contains(timeLimitSeconds)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timed behavioral sessions must use one of the supported limits: 300, 600, or 900 seconds.");
        }

        return timeLimitSeconds;
    }

    private boolean isSessionExpired(BehavioralSession session) {
        return isSessionExpired(session, getExpiresAt(session));
    }

    private boolean isSessionExpired(BehavioralSession session, Instant expiresAt) {
        return session.isTimed()
                && expiresAt != null
                && !Instant.now().isBefore(expiresAt);
    }

    private Instant getExpiresAt(BehavioralSession session) {
        if (!session.isTimed() || session.getTimeLimitSeconds() == null || session.getStartedAt() == null) {
            return null;
        }

        return session.getStartedAt().plusSeconds(session.getTimeLimitSeconds());
    }

    private void expireSessionIfNeeded(BehavioralSession session) {
        if (isSessionExpired(session) && session.getStatus() == BehavioralSessionStatus.STARTED) {
            session.setStatus(BehavioralSessionStatus.EXPIRED);
            behavioralSessionRepository.save(session);
            clearTimerState(session);
        }
    }

    private Optional<TimerState> resolveTimerState(BehavioralSession session) {
        Instant expiresAt = getExpiresAt(session);
        if (!session.isTimed() || expiresAt == null) {
            return Optional.empty();
        }

        Optional<TimerState> cachedState = timerStateStore.find(session.getId(), TimedSessionType.BEHAVIORAL);
        if (cachedState.isPresent()) {
            return cachedState;
        }

        if (isSessionExpired(session, expiresAt) || session.getStatus() != BehavioralSessionStatus.STARTED) {
            clearTimerState(session);
            return Optional.empty();
        }

        TimerState fallbackState = buildTimerState(
                session.getId(),
                session.getUserId(),
                expiresAt,
                TimedSessionType.BEHAVIORAL);
        timerStateStore.save(fallbackState);
        return Optional.of(fallbackState);
    }

    private void cacheTimerState(BehavioralSession session) {
        Instant expiresAt = getExpiresAt(session);
        if (!session.isTimed() || expiresAt == null || session.getStatus() != BehavioralSessionStatus.STARTED) {
            return;
        }

        timerStateStore.save(buildTimerState(
                session.getId(),
                session.getUserId(),
                expiresAt,
                TimedSessionType.BEHAVIORAL));
    }

    private void clearTimerState(BehavioralSession session) {
        timerStateStore.delete(session.getId(), TimedSessionType.BEHAVIORAL);
    }

    private TimerState buildTimerState(UUID sessionId, UUID userId, Instant expiresAt, TimedSessionType sessionType) {
        return TimerState.builder()
                .sessionId(sessionId)
                .userId(userId)
                .expiresAt(expiresAt)
                .sessionType(sessionType)
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return user;
    }
}
