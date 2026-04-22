package com.derwin.prepforge.behavioral.service;

import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.behavioral.dto.BehavioralFeedbackResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralQuestionResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionDetailResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralSessionResponse;
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
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class BehavioralService {

    private final BehavioralQuestionRepository behavioralQuestionRepository;
    private final BehavioralSessionRepository behavioralSessionRepository;
    private final BehavioralSubmissionRepository behavioralSubmissionRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<BehavioralQuestionResponse> getQuestions() {
        return behavioralQuestionRepository.findAllByOrderByCategoryAscDifficultyAscCreatedAtAsc().stream()
                .map(this::mapQuestion)
                .toList();
    }

    @Transactional
    public BehavioralSessionResponse startSession(BehavioralSessionRequest request) {
        UUID userId = getCurrentUser().getId();
        BehavioralQuestion question = getQuestionEntity(request.getQuestionId());

        BehavioralSession session = behavioralSessionRepository.save(BehavioralSession.builder()
                .userId(userId)
                .questionId(question.getId())
                .status(BehavioralSessionStatus.STARTED)
                .startedAt(Instant.now())
                .build());

        return mapSession(session);
    }

    @Transactional(readOnly = true)
    public BehavioralSessionDetailResponse getSessionDetail(UUID sessionId) {
        BehavioralSession session = getOwnedSession(sessionId);
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
        BehavioralSession session = getOwnedSession(sessionId);
        BehavioralQuestion question = getQuestionEntity(session.getQuestionId());
        BehavioralFeedbackResponse feedback = aiService.evaluateBehavioralResponse(question, request.getResponseText());

        BehavioralSubmission submission = behavioralSubmissionRepository.save(BehavioralSubmission.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .responseText(request.getResponseText())
                .aiFeedback(writeFeedback(feedback))
                .submittedAt(Instant.now())
                .build());

        session.setStatus(BehavioralSessionStatus.COMPLETED);
        behavioralSessionRepository.save(session);

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
        return BehavioralSessionResponse.builder()
                .sessionId(session.getId())
                .questionId(session.getQuestionId())
                .status(session.getStatus().name())
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return user;
    }
}
