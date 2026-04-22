package com.derwin.prepforge.coding.service;

import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.coding.dto.*;
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
import java.util.Set;
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
    private static final Set<Integer> ALLOWED_TIMED_DURATIONS = Set.of(15, 30, 45);

    private final CodingQuestionRepository codingQuestionRepository;
    private final CodingSessionRepository codingSessionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final AiService aiService;
    private final CodeExecutionService codeExecutionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CodingSessionResponse startSession(CodingSessionRequest request) {
        UUID userId = getCurrentUser().getId();
        CodingQuestion question = getQuestionEntity(request.getQuestionId());
        boolean timedMode = Boolean.TRUE.equals(request.getTimedMode());
        Integer durationMinutes = validateDurationMinutes(timedMode, request.getDurationMinutes());
        Instant startedAt = Instant.now();

        CodingSession session = codingSessionRepository.save(CodingSession.builder()
                .userId(userId)
                .questionId(question.getId())
                .status(CodingSessionStatus.STARTED)
                .timedMode(timedMode)
                .durationMinutes(durationMinutes)
                .expiresAt(timedMode ? startedAt.plusSeconds((long) durationMinutes * 60) : null)
                .startedAt(startedAt)
                .build());

        return mapSession(session);
    }

    @Transactional(readOnly = true)
    public List<CodingQuestionListItemResponse> getQuestions() {
        return codingQuestionRepository.findAllByOrderByTitleAsc().stream()
                .map(this::mapQuestionListItem)
                .toList();
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
                .strategy(mapStrategy(session))
                .question(mapQuestion(question))
                .submissions(submissions)
                .build();
    }

    @Transactional
    public CodingStrategyResponse saveStrategy(UUID sessionId, CodingStrategyRequest request) {
        CodingSession session = getOwnedSession(sessionId);

        session.setClarificationQuestions(request.getClarificationQuestions());
        session.setPlannedApproach(request.getPlannedApproach());
        session.setExpectedTimeComplexity(request.getExpectedTimeComplexity());
        session.setExpectedSpaceComplexity(request.getExpectedSpaceComplexity());

        CodingSession savedSession = codingSessionRepository.save(session);
        return mapStrategy(savedSession);
    }

    @Transactional(readOnly = true)
    public StrategyEvaluationResponse evaluateStrategy(UUID sessionId, CodingStrategyRequest request) {
        CodingSession session = getOwnedSession(sessionId);
        CodingQuestion question = getQuestionEntity(session.getQuestionId());

        return aiService.evaluateStrategy(question, request);
    }

    @Transactional(readOnly = true)
    public ApproachImplementationComparisonResponse compareApproachAndImplementation(UUID sessionId) {
        CodingSession session = getOwnedSession(sessionId);
        CodingQuestion question = getQuestionEntity(session.getQuestionId());

        if (isBlank(session.getPlannedApproach())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Save a planned approach before comparing it to your implementation");
        }

        CodingSubmission latestSubmission = codingSubmissionRepository
                .findBySessionIdOrderBySubmittedAtDesc(session.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Submit code before comparing approach and implementation"));

        return aiService.compareApproachAndImplementation(question, session, latestSubmission);
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
        expireSessionIfNeeded(session);

        if (isSessionExpired(session)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This timed assessment has expired. New submissions are no longer allowed.");
        }

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

    @Transactional(readOnly = true)
    public CodingImprovementResponse improveSubmission(UUID submissionId) {
        UUID userId = getCurrentUser().getId();

        CodingSubmission submission = codingSubmissionRepository.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coding submission not found"));
        CodingSession session = getOwnedSession(submission.getSessionId());
        CodingQuestion question = getQuestionEntity(session.getQuestionId());

        return aiService.improveSubmissionCode(question, submission);
    }

    @Transactional(readOnly = true)
    public RunCodeResponse runCode(UUID sessionId, RunCodeRequest request) {
        CodingSession session = getOwnedSession(sessionId);
        CodingQuestion question = getQuestionEntity(session.getQuestionId());
        validateRunLanguage(request);
        List<CodeExecutionService.ExecutionTestCase> testCases = buildExecutionTestCases(question);

        if (testCases.isEmpty()) {
            return RunCodeResponse.builder()
                    .success(true)
                    .passedTests(0)
                    .totalTests(0)
                    .error(null)
                    .compileError(null)
                    .runtimeError(null)
                    .timedOut(false)
                    .testResults(List.of())
                    .results(List.of())
                    .build();
        }

        CodeExecutionService.JavaHarnessFactory harnessFactory = buildHarnessFactory(question);
        return codeExecutionService.executeJava(request.getSolutionCode(), testCases, harnessFactory);
    }

    private List<CodeExecutionService.ExecutionTestCase> buildExecutionTestCases(CodingQuestion question) {
        String normalizedTitle = question.getTitle() == null ? "" : question.getTitle().trim().toLowerCase();

        if ("two sum".equals(normalizedTitle)) {
            return List.of(
                    new CodeExecutionService.ExecutionTestCase(
                            "[2,7,11,15], 9",
                            "2 7 11 15\n9\n",
                            "[0,1]"),
                    new CodeExecutionService.ExecutionTestCase(
                            "[3,2,4], 6",
                            "3 2 4\n6\n",
                            "[1,2]")
            );
        }

        if ("valid palindrome".equals(normalizedTitle)) {
            return List.of(
                    new CodeExecutionService.ExecutionTestCase(
                            "\"A man, a plan, a canal: Panama\"",
                            "A man, a plan, a canal: Panama\n",
                            "true"),
                    new CodeExecutionService.ExecutionTestCase(
                            "\"race a car\"",
                            "race a car\n",
                            "false")
            );
        }

        return List.of();
    }

    private CodeExecutionService.JavaHarnessFactory buildHarnessFactory(CodingQuestion question) {
        String normalizedTitle = question.getTitle() == null ? "" : question.getTitle().trim().toLowerCase();

        if ("two sum".equals(normalizedTitle)) {
            return solutionClassName -> """
                    import java.io.BufferedReader;
                    import java.io.InputStreamReader;
                    import java.lang.reflect.Method;
                    import java.util.Arrays;
                    import java.util.stream.IntStream;

                    public class PrepForgeHarness {
                        public static void main(String[] args) throws Exception {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                            String numbersLine = reader.readLine();
                            String targetLine = reader.readLine();

                            int[] nums = Arrays.stream(numbersLine.trim().split("\\\\s+"))
                                    .filter(part -> !part.isBlank())
                                    .mapToInt(Integer::parseInt)
                                    .toArray();
                            int target = Integer.parseInt(targetLine.trim());

                            Class<?> solutionClass = Class.forName("%s");
                            Object solution = solutionClass.getDeclaredConstructor().newInstance();
                            Method method = solutionClass.getMethod("twoSum", int[].class, int.class);
                            int[] result = (int[]) method.invoke(solution, nums, target);

                            String output = IntStream.of(result)
                                    .mapToObj(String::valueOf)
                                    .reduce((left, right) -> left + "," + right)
                                    .map(value -> "[" + value + "]")
                                    .orElse("[]");

                            System.out.print(output);
                        }
                    }
                    """.formatted(solutionClassName);
        }

        if ("valid palindrome".equals(normalizedTitle)) {
            return solutionClassName -> """
                    import java.io.BufferedReader;
                    import java.io.InputStreamReader;
                    import java.lang.reflect.Method;

                    public class PrepForgeHarness {
                        public static void main(String[] args) throws Exception {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                            String value = reader.readLine();

                            Class<?> solutionClass = Class.forName("%s");
                            Object solution = solutionClass.getDeclaredConstructor().newInstance();
                            Method method = solutionClass.getMethod("isPalindrome", String.class);
                            boolean result = (boolean) method.invoke(solution, value);

                            System.out.print(result);
                        }
                    }
                    """.formatted(solutionClassName);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported coding question for execution");
    }

    private void validateRunLanguage(RunCodeRequest request) {
        if (request.getLanguage() == null || !request.getLanguage().equalsIgnoreCase("java")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Java execution is currently supported");
        }
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

    private CodingStrategyResponse mapStrategy(CodingSession session) {
        return CodingStrategyResponse.builder()
                .sessionId(session.getId())
                .clarificationQuestions(session.getClarificationQuestions())
                .plannedApproach(session.getPlannedApproach())
                .expectedTimeComplexity(session.getExpectedTimeComplexity())
                .expectedSpaceComplexity(session.getExpectedSpaceComplexity())
                .build();
    }

    private CodingQuestionListItemResponse mapQuestionListItem(CodingQuestion question) {
        return CodingQuestionListItemResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .difficulty(question.getDifficulty())
                .build();
    }

    private CodingSubmissionResponse mapSubmission(CodingSubmission submission) {
        return CodingSubmissionResponse.builder()
                .submissionId(submission.getId())
                .sessionId(submission.getSessionId())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .code(submission.getSolutionCode())
                .aiFeedback(submission.getAiFeedback())
                .build();
    }

    private CodingSessionResponse mapSession(CodingSession session) {
        boolean expired = isSessionExpired(session);

        return CodingSessionResponse.builder()
                .sessionId(session.getId())
                .questionId(session.getQuestionId())
                .status(expired && session.getStatus() == CodingSessionStatus.STARTED
                        ? CodingSessionStatus.EXPIRED
                        : session.getStatus())
                .timedMode(session.isTimedMode())
                .durationMinutes(session.getDurationMinutes())
                .expiresAt(session.getExpiresAt())
                .expired(expired)
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Integer validateDurationMinutes(boolean timedMode, Integer durationMinutes) {
        if (!timedMode) {
            return null;
        }

        if (durationMinutes == null || !ALLOWED_TIMED_DURATIONS.contains(durationMinutes)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timed assessments must use one of the supported durations: 15, 30, or 45 minutes.");
        }

        return durationMinutes;
    }

    private boolean isSessionExpired(CodingSession session) {
        return session.isTimedMode()
                && session.getExpiresAt() != null
                && !Instant.now().isBefore(session.getExpiresAt());
    }

    private void expireSessionIfNeeded(CodingSession session) {
        if (isSessionExpired(session) && session.getStatus() == CodingSessionStatus.STARTED) {
            session.setStatus(CodingSessionStatus.EXPIRED);
            codingSessionRepository.save(session);
        }
    }
}
