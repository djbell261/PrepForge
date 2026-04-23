package com.derwin.prepforge.dashboard.service;

import com.derwin.prepforge.analytics.cache.AnalyticsCacheService;
import com.derwin.prepforge.analytics.cache.AnalyticsCacheType;
import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.behavioral.dto.BehavioralFeedbackResponse;
import com.derwin.prepforge.behavioral.entity.BehavioralSession;
import com.derwin.prepforge.behavioral.entity.BehavioralSubmission;
import com.derwin.prepforge.behavioral.repository.BehavioralSessionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSubmissionRepository;
import com.derwin.prepforge.coding.entity.CodingSession;
import com.derwin.prepforge.coding.entity.CodingSubmission;
import com.derwin.prepforge.coding.repository.CodingSessionRepository;
import com.derwin.prepforge.coding.repository.CodingSubmissionRepository;
import com.derwin.prepforge.dashboard.dto.DashboardCoachingSummaryResponse;
import com.derwin.prepforge.summary.SessionSummary;
import com.derwin.prepforge.summary.SessionSummaryRepository;
import com.derwin.prepforge.summary.SessionSummaryService;
import com.derwin.prepforge.summary.SessionSummaryType;
import com.derwin.prepforge.summary.dto.SessionSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardCoachingSummaryService {
    private final AnalyticsCacheService analyticsCacheService;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final SessionSummaryService sessionSummaryService;
    private final CodingSessionRepository codingSessionRepository;
    private final BehavioralSessionRepository behavioralSessionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final BehavioralSubmissionRepository behavioralSubmissionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DashboardCoachingSummaryResponse getCoachingSummary() {
        UUID userId = getCurrentUser().getId();
        return analyticsCacheService.getOrLoad(
                userId,
                AnalyticsCacheType.DASHBOARD_COACHING_SUMMARY,
                DashboardCoachingSummaryResponse.class,
                () -> computeCoachingSummary(userId));
    }

    private DashboardCoachingSummaryResponse computeCoachingSummary(UUID userId) {
        SummarySnapshot codingSnapshot = buildCodingSnapshot(userId);
        SummarySnapshot behavioralSnapshot = buildBehavioralSnapshot(userId);
        ScoreSnapshot pressureSnapshot = buildPressureSnapshot(userId);

        String recommendedSessionType = determineRecommendedSessionType(codingSnapshot, behavioralSnapshot);
        SummarySnapshot recommendedSnapshot = "BEHAVIORAL".equals(recommendedSessionType) ? behavioralSnapshot : codingSnapshot;
        SummarySnapshot strongestSnapshot = pickStrongestSnapshot(codingSnapshot, behavioralSnapshot);
        SummarySnapshot weakestSnapshot = pickWeakestSnapshot(codingSnapshot, behavioralSnapshot, recommendedSessionType);

        DashboardCoachingSummaryResponse response = DashboardCoachingSummaryResponse.builder()
                .headline(buildHeadline(codingSnapshot, behavioralSnapshot, recommendedSessionType))
                .strongestArea(buildStrongestArea(strongestSnapshot))
                .weakestArea(buildWeakestArea(weakestSnapshot))
                .recommendedNextStep(buildRecommendedNextStep(recommendedSnapshot, recommendedSessionType))
                .recommendedSessionType(recommendedSessionType)
                .pressureInsight(buildPressureInsight(pressureSnapshot))
                .confidenceNote(buildConfidenceNote(codingSnapshot, behavioralSnapshot))
                .build();
        log.info(
                "dashboard_coaching_summary_generated userId={} recommendedSessionType={} codingReviewedCount={} behavioralReviewedCount={} pressureTimedCount={} pressureUntimedCount={}",
                userId,
                response.getRecommendedSessionType(),
                codingSnapshot.reviewedCount(),
                behavioralSnapshot.reviewedCount(),
                pressureSnapshot.timedCount(),
                pressureSnapshot.untimedCount());
        return response;
    }

    private SummarySnapshot buildCodingSnapshot(UUID userId) {
        SessionSummaryResponse latestSummary = ensureLatestSummary(
                userId,
                SessionSummaryType.CODING,
                codingSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                        .map(CodingSession::getId)
                        .toList());
        List<Integer> scores = codingSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(CodingSubmission::getAiFeedback)
                .map(this::extractCodingScore)
                .filter(score -> score != null)
                .toList();

        return SummarySnapshot.builder()
                .sessionType("CODING")
                .reviewedCount(scores.size())
                .averageScore(average(scores))
                .latestSummary(latestSummary)
                .build();
    }

    private SummarySnapshot buildBehavioralSnapshot(UUID userId) {
        SessionSummaryResponse latestSummary = ensureLatestSummary(
                userId,
                SessionSummaryType.BEHAVIORAL,
                behavioralSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                        .map(BehavioralSession::getId)
                        .toList());
        List<Integer> scores = behavioralSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(BehavioralSubmission::getAiFeedback)
                .map(this::extractBehavioralScore)
                .filter(score -> score != null)
                .toList();

        return SummarySnapshot.builder()
                .sessionType("BEHAVIORAL")
                .reviewedCount(scores.size())
                .averageScore(average(scores))
                .latestSummary(latestSummary)
                .build();
    }

    private SessionSummaryResponse ensureLatestSummary(UUID userId, SessionSummaryType sessionType, List<UUID> recentSessionIds) {
        List<SessionSummary> persistedSummaries = sessionSummaryRepository
                .findTop3ByUserIdAndSessionTypeOrderByUpdatedAtDesc(userId, sessionType);
        if (!persistedSummaries.isEmpty()) {
            return mapSummary(persistedSummaries.get(0));
        }

        for (UUID sessionId : recentSessionIds) {
            try {
                return sessionType == SessionSummaryType.CODING
                        ? sessionSummaryService.getCodingSummary(sessionId)
                        : sessionSummaryService.getBehavioralSummary(sessionId);
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode() != HttpStatus.NOT_FOUND) {
                    throw exception;
                }
            }
        }

        return null;
    }

    private ScoreSnapshot buildPressureSnapshot(UUID userId) {
        Map<UUID, CodingSession> codingSessionsById = codingSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .collect(java.util.stream.Collectors.toMap(CodingSession::getId, session -> session));
        Map<UUID, BehavioralSession> behavioralSessionsById = behavioralSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .collect(java.util.stream.Collectors.toMap(BehavioralSession::getId, session -> session));

        List<Integer> timedScores = new ArrayList<>();
        List<Integer> untimedScores = new ArrayList<>();

        for (CodingSubmission submission : codingSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId)) {
            Integer score = extractCodingScore(submission.getAiFeedback());
            if (score == null) {
                continue;
            }

            CodingSession session = codingSessionsById.get(submission.getSessionId());
            if (session != null && session.isTimedMode()) {
                timedScores.add(score);
            } else {
                untimedScores.add(score);
            }
        }

        for (BehavioralSubmission submission : behavioralSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId)) {
            Integer score = extractBehavioralScore(submission.getAiFeedback());
            if (score == null) {
                continue;
            }

            BehavioralSession session = behavioralSessionsById.get(submission.getSessionId());
            if (session != null && session.isTimed()) {
                timedScores.add(score);
            } else {
                untimedScores.add(score);
            }
        }

        return new ScoreSnapshot(average(timedScores), timedScores.size(), average(untimedScores), untimedScores.size());
    }

    private String determineRecommendedSessionType(SummarySnapshot codingSnapshot, SummarySnapshot behavioralSnapshot) {
        if (codingSnapshot.reviewedCount() == 0 && behavioralSnapshot.reviewedCount() == 0) {
            return "CODING";
        }

        if (codingSnapshot.reviewedCount() == 0) {
            return "CODING";
        }

        if (behavioralSnapshot.reviewedCount() == 0) {
            return "BEHAVIORAL";
        }

        if (codingSnapshot.averageScore() == null) {
            return "CODING";
        }

        if (behavioralSnapshot.averageScore() == null) {
            return "BEHAVIORAL";
        }

        return codingSnapshot.averageScore() <= behavioralSnapshot.averageScore() ? "CODING" : "BEHAVIORAL";
    }

    private SummarySnapshot pickStrongestSnapshot(SummarySnapshot codingSnapshot, SummarySnapshot behavioralSnapshot) {
        if (codingSnapshot.reviewedCount() == 0) {
            return behavioralSnapshot;
        }

        if (behavioralSnapshot.reviewedCount() == 0) {
            return codingSnapshot;
        }

        if (codingSnapshot.averageScore() == null) {
            return behavioralSnapshot;
        }

        if (behavioralSnapshot.averageScore() == null) {
            return codingSnapshot;
        }

        return codingSnapshot.averageScore() >= behavioralSnapshot.averageScore() ? codingSnapshot : behavioralSnapshot;
    }

    private SummarySnapshot pickWeakestSnapshot(
            SummarySnapshot codingSnapshot,
            SummarySnapshot behavioralSnapshot,
            String recommendedSessionType) {
        if ("BEHAVIORAL".equals(recommendedSessionType)) {
            return behavioralSnapshot;
        }

        return codingSnapshot;
    }

    private String buildHeadline(SummarySnapshot codingSnapshot, SummarySnapshot behavioralSnapshot, String recommendedSessionType) {
        if (codingSnapshot.reviewedCount() == 0 && behavioralSnapshot.reviewedCount() == 0) {
            return "You’re still building your baseline. Finish one coding or behavioral rep to unlock sharper coaching.";
        }

        if (codingSnapshot.reviewedCount() == 0) {
            return "Your behavioral reps are on the board. Add coding practice next so your dashboard can coach both interview tracks.";
        }

        if (behavioralSnapshot.reviewedCount() == 0) {
            return "Your coding reps are carrying the dashboard right now. Add behavioral practice next for a fuller interview readout.";
        }

        if ("CODING".equals(recommendedSessionType)) {
            return "Behavioral communication is keeping pace better than coding execution right now, so your next leverage point is on the coding side.";
        }

        return "Coding performance is slightly ahead of behavioral communication right now, so the next leverage point is in your interview stories.";
    }

    private String buildStrongestArea(SummarySnapshot strongestSnapshot) {
        if (strongestSnapshot == null || strongestSnapshot.reviewedCount() == 0) {
            return "Not enough reviewed sessions yet to call a strongest area.";
        }

        String strongestBullet = firstItem(strongestSnapshot.latestSummary() == null ? null : strongestSnapshot.latestSummary().getStrengths());
        if (strongestBullet != null) {
            return label(strongestSnapshot.sessionType()) + ": " + strongestBullet;
        }

        return label(strongestSnapshot.sessionType()) + " is your strongest track right now based on reviewed performance.";
    }

    private String buildWeakestArea(SummarySnapshot weakestSnapshot) {
        if (weakestSnapshot == null || weakestSnapshot.reviewedCount() == 0) {
            return "The biggest gap is simply missing data on one side of your interview practice.";
        }

        String weaknessBullet = firstItem(weakestSnapshot.latestSummary() == null ? null : weakestSnapshot.latestSummary().getWeaknesses());
        if (weaknessBullet != null) {
            return label(weakestSnapshot.sessionType()) + ": " + weaknessBullet;
        }

        return label(weakestSnapshot.sessionType()) + " is the weaker track right now and needs the next focused rep.";
    }

    private String buildRecommendedNextStep(SummarySnapshot recommendedSnapshot, String recommendedSessionType) {
        String nextStep = firstItem(recommendedSnapshot == null || recommendedSnapshot.latestSummary() == null
                ? null
                : recommendedSnapshot.latestSummary().getNextSteps());
        if (nextStep != null) {
            return nextStep;
        }

        return "BEHAVIORAL".equals(recommendedSessionType)
                ? "Run a fresh behavioral rep and tighten one story until it sounds concise, specific, and measurable."
                : "Run a fresh coding rep and narrate your tradeoffs before and during implementation.";
    }

    private String buildPressureInsight(ScoreSnapshot pressureSnapshot) {
        if (pressureSnapshot.timedCount() < 2 || pressureSnapshot.untimedCount() < 2
                || pressureSnapshot.timedAverage() == null || pressureSnapshot.untimedAverage() == null) {
            return "Not enough timed and untimed reps yet to tell whether pressure mode is helping or hurting.";
        }

        double difference = pressureSnapshot.timedAverage() - pressureSnapshot.untimedAverage();
        if (difference <= -0.75d) {
            return "Pressure mode is dragging performance down by about "
                    + String.format(Locale.US, "%.1f", Math.abs(difference))
                    + " points. Mix one timed rep in after untimed practice so the clock feels less disruptive.";
        }

        if (difference >= 0.75d) {
            return "You’re holding up well under pressure. Timed reps are matching or outperforming your untimed work right now.";
        }

        return "Pressure mode looks roughly neutral right now. Your timed and untimed scores are landing in the same range.";
    }

    private String buildConfidenceNote(SummarySnapshot codingSnapshot, SummarySnapshot behavioralSnapshot) {
        if (codingSnapshot.reviewedCount() == 0 && behavioralSnapshot.reviewedCount() == 0) {
            return "Low confidence: this recommendation is based on very limited reviewed data so far.";
        }

        if (codingSnapshot.reviewedCount() == 0 || behavioralSnapshot.reviewedCount() == 0) {
            return "Medium confidence: one interview track has good signal, but the other still needs reviewed reps.";
        }

        int totalReviewed = codingSnapshot.reviewedCount() + behavioralSnapshot.reviewedCount();
        if (totalReviewed >= 6) {
            return "Higher confidence: this recommendation is grounded in multiple reviewed coding and behavioral attempts.";
        }

        return "Medium confidence: the recommendation is directionally useful, but it will sharpen as more sessions are reviewed.";
    }

    private SessionSummaryResponse mapSummary(SessionSummary sessionSummary) {
        try {
            return SessionSummaryResponse.builder()
                    .id(sessionSummary.getId())
                    .sessionType(sessionSummary.getSessionType().name())
                    .sessionId(sessionSummary.getSessionId())
                    .userId(sessionSummary.getUserId())
                    .summary(sessionSummary.getSummary())
                    .strengths(objectMapper.readerForListOf(String.class).readValue(sessionSummary.getStrengthsJson()))
                    .weaknesses(objectMapper.readerForListOf(String.class).readValue(sessionSummary.getWeaknessesJson()))
                    .nextSteps(objectMapper.readerForListOf(String.class).readValue(sessionSummary.getNextStepsJson()))
                    .createdAt(sessionSummary.getCreatedAt())
                    .updatedAt(sessionSummary.getUpdatedAt())
                    .build();
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read session summary", exception);
        }
    }

    private Integer extractCodingScore(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawFeedback);
            return root.path("score").isNumber() ? root.path("score").asInt() : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private Integer extractBehavioralScore(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return null;
        }

        try {
            BehavioralFeedbackResponse feedback = objectMapper.readValue(rawFeedback, BehavioralFeedbackResponse.class);
            return feedback.getScore();
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private Double average(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }

        return scores.stream().mapToInt(Integer::intValue).average().orElse(0.0d);
    }

    private String firstItem(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream().filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
    }

    private String label(String sessionType) {
        return "BEHAVIORAL".equals(sessionType) ? "Behavioral" : "Coding";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return user;
    }

    @lombok.Builder
    private record SummarySnapshot(
            String sessionType,
            int reviewedCount,
            Double averageScore,
            SessionSummaryResponse latestSummary) {
    }

    private record ScoreSnapshot(
            Double timedAverage,
            int timedCount,
            Double untimedAverage,
            int untimedCount) {
    }
}
