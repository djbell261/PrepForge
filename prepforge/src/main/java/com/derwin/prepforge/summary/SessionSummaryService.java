package com.derwin.prepforge.summary;

import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.behavioral.dto.BehavioralFeedbackResponse;
import com.derwin.prepforge.behavioral.entity.BehavioralQuestion;
import com.derwin.prepforge.behavioral.entity.BehavioralSession;
import com.derwin.prepforge.behavioral.entity.BehavioralSubmission;
import com.derwin.prepforge.behavioral.repository.BehavioralQuestionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSessionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSubmissionRepository;
import com.derwin.prepforge.coding.entity.CodingQuestion;
import com.derwin.prepforge.coding.entity.CodingSession;
import com.derwin.prepforge.coding.entity.CodingSubmission;
import com.derwin.prepforge.coding.repository.CodingQuestionRepository;
import com.derwin.prepforge.coding.repository.CodingSessionRepository;
import com.derwin.prepforge.coding.repository.CodingSubmissionRepository;
import com.derwin.prepforge.summary.dto.SessionSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class SessionSummaryService {
    private final SessionSummaryRepository sessionSummaryRepository;
    private final CodingSessionRepository codingSessionRepository;
    private final CodingQuestionRepository codingQuestionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final BehavioralSessionRepository behavioralSessionRepository;
    private final BehavioralQuestionRepository behavioralQuestionRepository;
    private final BehavioralSubmissionRepository behavioralSubmissionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SessionSummaryResponse getCodingSummary(UUID sessionId) {
        CodingSession session = getOwnedCodingSession(sessionId);

        return sessionSummaryRepository.findBySessionTypeAndSessionId(SessionSummaryType.CODING, sessionId)
                .map(this::mapResponse)
                .orElseGet(() -> createCodingSummary(session));
    }

    @Transactional
    public SessionSummaryResponse getBehavioralSummary(UUID sessionId) {
        BehavioralSession session = getOwnedBehavioralSession(sessionId);

        return sessionSummaryRepository.findBySessionTypeAndSessionId(SessionSummaryType.BEHAVIORAL, sessionId)
                .map(this::mapResponse)
                .orElseGet(() -> createBehavioralSummary(session));
    }

    @Transactional
    public void invalidateCodingSummary(UUID sessionId) {
        sessionSummaryRepository.deleteBySessionTypeAndSessionId(SessionSummaryType.CODING, sessionId);
    }

    @Transactional
    public void invalidateBehavioralSummary(UUID sessionId) {
        sessionSummaryRepository.deleteBySessionTypeAndSessionId(SessionSummaryType.BEHAVIORAL, sessionId);
    }

    private SessionSummaryResponse createCodingSummary(CodingSession session) {
        CodingQuestion question = codingQuestionRepository.findById(session.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coding question not found"));
        List<CodingSubmission> submissions = codingSubmissionRepository.findBySessionIdOrderBySubmittedAtDesc(session.getId());
        CodingSubmission latestSubmission = submissions.stream().findFirst().orElse(null);
        CodingFeedbackSnapshot feedback = parseCodingFeedback(latestSubmission == null ? null : latestSubmission.getAiFeedback());

        Set<String> strengths = new LinkedHashSet<>();
        Set<String> weaknesses = new LinkedHashSet<>();
        Set<String> nextSteps = new LinkedHashSet<>();

        if (feedback != null) {
            strengths.addAll(defaultList(feedback.strengths()));
            weaknesses.addAll(defaultList(feedback.weaknesses()));
            nextSteps.addAll(defaultList(feedback.recommendations()));
        }

        if (hasText(session.getPlannedApproach())) {
            strengths.add("You documented a planned approach before or during implementation.");
        } else {
            weaknesses.add("You do not yet have a saved problem-solving plan for this session.");
            nextSteps.add("Write a short planned approach before coding to sharpen your interview communication.");
        }

        if (latestSubmission == null) {
            weaknesses.add("You do not have a reviewed coding submission yet.");
            nextSteps.add("Submit a full solution to unlock stronger coaching on correctness and communication.");
        }

        String summary = feedback != null && hasText(feedback.summary())
                ? "On " + question.getTitle() + ", " + feedback.summary()
                : "This coding session on " + question.getTitle() + " is ready for review, but a detailed AI summary is not available yet.";

        nextSteps.add("Practice explaining your tradeoffs out loud before you start coding.");

        Instant now = Instant.now();

        SessionSummary sessionSummary = sessionSummaryRepository.save(SessionSummary.builder()
                .sessionType(SessionSummaryType.CODING)
                .sessionId(session.getId())
                .userId(session.getUserId())
                .summary(summary)
                .strengthsJson(writeList(strengths))
                .weaknessesJson(writeList(weaknesses))
                .nextStepsJson(writeList(nextSteps))
                .createdAt(now)
                .updatedAt(now)
                .build());

        log.info(
                "session_summary_generated sessionType={} sessionId={} userId={} strengthsCount={} weaknessesCount={} nextStepsCount={}",
                SessionSummaryType.CODING,
                session.getId(),
                session.getUserId(),
                readList(sessionSummary.getStrengthsJson()).size(),
                readList(sessionSummary.getWeaknessesJson()).size(),
                readList(sessionSummary.getNextStepsJson()).size());
        return mapResponse(sessionSummary);
    }

    private SessionSummaryResponse createBehavioralSummary(BehavioralSession session) {
        BehavioralQuestion question = behavioralQuestionRepository.findById(session.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral question not found"));
        List<BehavioralSubmission> submissions = behavioralSubmissionRepository.findBySessionIdOrderBySubmittedAtDesc(session.getId());
        BehavioralSubmission latestSubmission = submissions.stream().findFirst().orElse(null);
        BehavioralFeedbackResponse latestFeedback = latestSubmission == null ? null : parseBehavioralFeedback(latestSubmission.getAiFeedback());

        Set<String> strengths = new LinkedHashSet<>();
        Set<String> weaknesses = new LinkedHashSet<>();
        Set<String> nextSteps = new LinkedHashSet<>();

        if (latestFeedback != null) {
            strengths.addAll(defaultList(latestFeedback.getStrengths()));
            weaknesses.addAll(defaultList(latestFeedback.getWeaknesses()));
            nextSteps.addAll(defaultList(latestFeedback.getRecommendations()));

            if (latestFeedback.getImprovements() != null && !latestFeedback.getImprovements().isEmpty()) {
                strengths.add("Compared with your previous attempt, you improved: " + latestFeedback.getImprovements().get(0));
            }

            if (latestFeedback.getRegressions() != null && !latestFeedback.getRegressions().isEmpty()) {
                weaknesses.add("One regression to watch: " + latestFeedback.getRegressions().get(0));
            }
        } else {
            weaknesses.add("Behavioral feedback is still unavailable for the latest submission.");
            nextSteps.add("Refresh after feedback completes to see the full coaching summary.");
        }

        if (submissions.size() > 1) {
            strengths.add("You are iterating on the same story, which is how stronger interview answers take shape.");
        } else {
            nextSteps.add("Try a second attempt so you can compare what improved from one version to the next.");
        }

        String summary = latestFeedback != null && hasText(latestFeedback.getSummary())
                ? "For this " + question.getCategory().toLowerCase() + " behavioral prompt, " + latestFeedback.getSummary()
                : "This behavioral session is complete, but the coaching summary is still warming up.";

        Instant now = Instant.now();

        SessionSummary sessionSummary = sessionSummaryRepository.save(SessionSummary.builder()
                .sessionType(SessionSummaryType.BEHAVIORAL)
                .sessionId(session.getId())
                .userId(session.getUserId())
                .summary(summary)
                .strengthsJson(writeList(strengths))
                .weaknessesJson(writeList(weaknesses))
                .nextStepsJson(writeList(nextSteps))
                .createdAt(now)
                .updatedAt(now)
                .build());

        log.info(
                "session_summary_generated sessionType={} sessionId={} userId={} strengthsCount={} weaknessesCount={} nextStepsCount={}",
                SessionSummaryType.BEHAVIORAL,
                session.getId(),
                session.getUserId(),
                readList(sessionSummary.getStrengthsJson()).size(),
                readList(sessionSummary.getWeaknessesJson()).size(),
                readList(sessionSummary.getNextStepsJson()).size());
        return mapResponse(sessionSummary);
    }

    private SessionSummaryResponse mapResponse(SessionSummary sessionSummary) {
        return SessionSummaryResponse.builder()
                .id(sessionSummary.getId())
                .sessionType(sessionSummary.getSessionType().name())
                .sessionId(sessionSummary.getSessionId())
                .userId(sessionSummary.getUserId())
                .summary(sessionSummary.getSummary())
                .strengths(readList(sessionSummary.getStrengthsJson()))
                .weaknesses(readList(sessionSummary.getWeaknessesJson()))
                .nextSteps(readList(sessionSummary.getNextStepsJson()))
                .createdAt(sessionSummary.getCreatedAt())
                .updatedAt(sessionSummary.getUpdatedAt())
                .build();
    }

    private CodingSession getOwnedCodingSession(UUID sessionId) {
        UUID userId = getCurrentUser().getId();
        return codingSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coding session not found"));
    }

    private BehavioralSession getOwnedBehavioralSession(UUID sessionId) {
        UUID userId = getCurrentUser().getId();
        return behavioralSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral session not found"));
    }

    private BehavioralFeedbackResponse parseBehavioralFeedback(String rawFeedback) {
        if (!hasText(rawFeedback)) {
            return null;
        }

        try {
            return objectMapper.readValue(rawFeedback, BehavioralFeedbackResponse.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private CodingFeedbackSnapshot parseCodingFeedback(String rawFeedback) {
        if (!hasText(rawFeedback)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawFeedback);
            return new CodingFeedbackSnapshot(
                    root.path("summary").asText(null),
                    readStringList(root.path("strengths")),
                    readStringList(root.path("weaknesses")),
                    readStringList(root.path("recommendations")));
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual() && hasText(item.asText())) {
                values.add(item.asText());
            }
        });
        return values;
    }

    private List<String> readList(String value) {
        if (!hasText(value)) {
            return List.of();
        }

        try {
            return objectMapper.readerForListOf(String.class).readValue(value);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String writeList(Set<String> values) {
        try {
            return objectMapper.writeValueAsString(values.stream().filter(this::hasText).limit(4).toList());
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store session summary", exception);
        }
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return user;
    }

    private record CodingFeedbackSnapshot(
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recommendations) {
    }
}
