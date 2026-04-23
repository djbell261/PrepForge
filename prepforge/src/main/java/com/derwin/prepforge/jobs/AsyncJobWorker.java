package com.derwin.prepforge.jobs;

import com.derwin.prepforge.analytics.cache.AnalyticsCacheService;
import com.derwin.prepforge.analytics.cache.AnalyticsCacheType;
import com.derwin.prepforge.behavioral.dto.BehavioralComparisonAnalysisResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralFeedbackResponse;
import com.derwin.prepforge.behavioral.entity.BehavioralQuestion;
import com.derwin.prepforge.behavioral.entity.BehavioralSession;
import com.derwin.prepforge.behavioral.entity.BehavioralSubmission;
import com.derwin.prepforge.behavioral.repository.BehavioralQuestionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSessionRepository;
import com.derwin.prepforge.behavioral.repository.BehavioralSubmissionRepository;
import com.derwin.prepforge.common.logging.LoggingContext;
import com.derwin.prepforge.coding.service.AiService;
import com.derwin.prepforge.summary.SessionSummaryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncJobWorker {
    private final AsyncJobService asyncJobService;
    private final BehavioralSubmissionRepository behavioralSubmissionRepository;
    private final BehavioralSessionRepository behavioralSessionRepository;
    private final BehavioralQuestionRepository behavioralQuestionRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final AnalyticsCacheService analyticsCacheService;
    private final AsyncJobProperties asyncJobProperties;
    private final SessionSummaryService sessionSummaryService;

    @Scheduled(fixedDelayString = "${app.jobs.worker.poll-delay-ms:3000}")
    public void pollAndProcessJobs() {
        if (!asyncJobProperties.getWorker().isEnabled()) {
            return;
        }

        for (int index = 0; index < asyncJobProperties.getWorker().getMaxJobsPerPoll(); index++) {
            Optional<AsyncJob> job = asyncJobService.claimNextEligibleJob();
            if (job.isEmpty()) {
                return;
            }

            processJob(job.get());
        }
    }

    private void processJob(AsyncJob job) {
        Map<String, String> previousContext = LoggingContext.capture();
        LoggingContext.putCorrelationId(job.getCorrelationId());
        LoggingContext.putJobContext(
                job.getId().toString(),
                job.getJobType().name(),
                job.getAggregateType().name(),
                job.getAggregateId().toString());

        try {
            log.info(
                    "async_job_processing_start jobId={} jobType={} aggregateType={} aggregateId={} correlationId={}",
                    job.getId(),
                    job.getJobType(),
                    job.getAggregateType(),
                    job.getAggregateId(),
                    job.getCorrelationId());

            if (job.getJobType() == AsyncJobType.BEHAVIORAL_FEEDBACK_GENERATION) {
                processBehavioralFeedbackJob(job);
                asyncJobService.markSucceeded(job.getId());
                return;
            }

            if (job.getJobType() == AsyncJobType.BEHAVIORAL_IMPROVEMENT_ANALYSIS) {
                processBehavioralImprovementAnalysisJob(job);
                asyncJobService.markSucceeded(job.getId());
                return;
            }

            asyncJobService.markFailedOrRetry(
                    job.getId(),
                    AsyncJobFailureCode.UNKNOWN,
                    "Unsupported job type: " + job.getJobType(),
                    false);
        } catch (Exception exception) {
            AsyncJobFailureCode failureCode = classifyFailureCode(exception);
            asyncJobService.markFailedOrRetry(job.getId(), failureCode, exception.getMessage(), isRetryable(exception));
            log.warn(
                    "async_job_processing_exception jobId={} jobType={} aggregateType={} aggregateId={} correlationId={}",
                    job.getId(),
                    job.getJobType(),
                    job.getAggregateType(),
                    job.getAggregateId(),
                    job.getCorrelationId(),
                    exception);
        } finally {
            LoggingContext.restore(previousContext);
        }
    }

    @Transactional
    protected void processBehavioralFeedbackJob(AsyncJob job) throws JsonProcessingException {
        BehavioralSubmission submission = behavioralSubmissionRepository.findById(job.getAggregateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral submission not found"));

        if (submission.getAiFeedback() != null && !submission.getAiFeedback().isBlank()) {
            enqueueComparisonJobIfNeeded(submission);
            return;
        }

        BehavioralSession session = behavioralSessionRepository.findById(submission.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral session not found"));
        BehavioralQuestion question = behavioralQuestionRepository.findById(session.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral question not found"));

        List<BehavioralSubmission> submissions = behavioralSubmissionRepository.findBySessionIdOrderBySubmittedAtDesc(session.getId());
        String previousResponseText = submissions.stream()
                .filter(candidate -> !candidate.getId().equals(submission.getId()))
                .map(BehavioralSubmission::getResponseText)
                .findFirst()
                .orElse(null);

        BehavioralFeedbackResponse feedback = aiService.evaluateBehavioralResponse(
                question,
                previousResponseText,
                submission.getResponseText());

        submission.setAiFeedback(objectMapper.writeValueAsString(feedback));
        behavioralSubmissionRepository.save(submission);
        log.info(
                "behavioral_feedback_saved submissionId={} sessionId={} userId={} score={}",
                submission.getId(),
                session.getId(),
                submission.getUserId(),
                feedback.getScore());
        analyticsCacheService.evictAllAnalytics(submission.getUserId());
        sessionSummaryService.invalidateBehavioralSummary(session.getId());
        enqueueComparisonJobIfNeeded(submission);
    }

    @Transactional
    protected void processBehavioralImprovementAnalysisJob(AsyncJob job) throws JsonProcessingException {
        BehavioralSubmission submission = behavioralSubmissionRepository.findById(job.getAggregateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral submission not found"));

        BehavioralFeedbackResponse currentFeedback = readFeedback(submission);
        if (currentFeedback == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Behavioral feedback is not available yet for comparison");
        }

        if (hasComparisonResults(currentFeedback)) {
            return;
        }

        BehavioralSession session = behavioralSessionRepository.findById(submission.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral session not found"));
        BehavioralQuestion question = behavioralQuestionRepository.findById(session.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral question not found"));

        BehavioralSubmission previousSubmission = findPreviousSubmission(session.getId(), submission.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Previous behavioral submission not found"));
        BehavioralFeedbackResponse previousFeedback = readFeedback(previousSubmission);

        BehavioralComparisonAnalysisResponse analysis = aiService.analyzeBehavioralImprovement(
                question,
                previousSubmission.getResponseText(),
                submission.getResponseText(),
                previousFeedback == null ? null : previousFeedback.getSummary(),
                currentFeedback.getSummary());

        currentFeedback.setImprovements(analysis.getImprovements());
        currentFeedback.setRegressions(analysis.getRegressions());
        submission.setAiFeedback(objectMapper.writeValueAsString(currentFeedback));
        behavioralSubmissionRepository.save(submission);
        log.info(
                "behavioral_comparison_saved submissionId={} sessionId={} userId={} improvementsCount={} regressionsCount={}",
                submission.getId(),
                session.getId(),
                submission.getUserId(),
                analysis.getImprovements() == null ? 0 : analysis.getImprovements().size(),
                analysis.getRegressions() == null ? 0 : analysis.getRegressions().size());
        analyticsCacheService.evictAllAnalytics(submission.getUserId());
        sessionSummaryService.invalidateBehavioralSummary(session.getId());
    }

    private boolean isRetryable(Exception exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode() == HttpStatus.BAD_GATEWAY
                    || responseStatusException.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
                    || responseStatusException.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT
                    || responseStatusException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
        }

        return true;
    }

    private AsyncJobFailureCode classifyFailureCode(Exception exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            if (responseStatusException.getStatusCode() == HttpStatus.NOT_FOUND) {
                return AsyncJobFailureCode.AGGREGATE_NOT_FOUND;
            }

            if (isRetryable(exception)) {
                return AsyncJobFailureCode.AI_PROVIDER_TRANSIENT;
            }

            return AsyncJobFailureCode.AI_PROVIDER_PERMANENT;
        }

        return AsyncJobFailureCode.UNKNOWN;
    }

    private void enqueueComparisonJobIfNeeded(BehavioralSubmission submission) {
        if (findPreviousSubmission(submission.getSessionId(), submission.getId()).isPresent()) {
            asyncJobService.enqueueBehavioralImprovementAnalysis(submission.getId());
        }
    }

    private Optional<BehavioralSubmission> findPreviousSubmission(java.util.UUID sessionId, java.util.UUID submissionId) {
        return behavioralSubmissionRepository.findBySessionIdOrderBySubmittedAtDesc(sessionId).stream()
                .filter(candidate -> !candidate.getId().equals(submissionId))
                .findFirst();
    }

    private BehavioralFeedbackResponse readFeedback(BehavioralSubmission submission) throws JsonProcessingException {
        if (submission.getAiFeedback() == null || submission.getAiFeedback().isBlank()) {
            return null;
        }

        return objectMapper.readValue(submission.getAiFeedback(), BehavioralFeedbackResponse.class);
    }

    private boolean hasComparisonResults(BehavioralFeedbackResponse feedback) {
        return feedback != null
                && feedback.getImprovements() != null
                && feedback.getRegressions() != null;
    }
}
