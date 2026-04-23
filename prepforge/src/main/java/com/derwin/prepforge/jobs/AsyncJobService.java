package com.derwin.prepforge.jobs;

import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.behavioral.entity.BehavioralSubmission;
import com.derwin.prepforge.behavioral.repository.BehavioralSubmissionRepository;
import com.derwin.prepforge.common.logging.LoggingContext;
import com.derwin.prepforge.infrastructure.observability.PrepForgeMetrics;
import com.derwin.prepforge.jobs.dto.AsyncJobStatusResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncJobService {
    private static final List<AsyncJobStatus> ELIGIBLE_STATUSES = List.of(AsyncJobStatus.QUEUED, AsyncJobStatus.RETRY_SCHEDULED);

    private final AsyncJobRepository asyncJobRepository;
    private final BehavioralSubmissionRepository behavioralSubmissionRepository;
    private final AsyncJobProperties asyncJobProperties;
    private final PrepForgeMetrics prepForgeMetrics;

    @Transactional
    public AsyncJob enqueueBehavioralFeedbackGeneration(UUID submissionId) {
        String idempotencyKey = buildBehavioralFeedbackIdempotencyKey(submissionId);
        String correlationId = LoggingContext.resolveCorrelationId();

        AsyncJob job = asyncJobRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> asyncJobRepository.save(AsyncJob.builder()
                        .jobType(AsyncJobType.BEHAVIORAL_FEEDBACK_GENERATION)
                        .status(AsyncJobStatus.QUEUED)
                        .aggregateType(AsyncJobAggregateType.BEHAVIORAL_SUBMISSION)
                        .aggregateId(submissionId)
                        .attemptCount(0)
                        .maxAttempts(asyncJobProperties.getDefaultMaxAttempts())
                        .scheduledAt(Instant.now())
                        .idempotencyKey(idempotencyKey)
                        .correlationId(correlationId)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
        if (job.getCorrelationId() == null || job.getCorrelationId().isBlank()) {
            job.setCorrelationId(correlationId);
            job.setUpdatedAt(Instant.now());
            job = asyncJobRepository.save(job);
        }

        log.info(
                "async_job_queued jobId={} jobType={} aggregateType={} aggregateId={} correlationId={} status={}",
                job.getId(),
                job.getJobType(),
                job.getAggregateType(),
                job.getAggregateId(),
                job.getCorrelationId(),
                job.getStatus());
        prepForgeMetrics.incrementAsyncJobQueued(job.getJobType().name());
        return job;
    }

    @Transactional
    public AsyncJob enqueueBehavioralImprovementAnalysis(UUID submissionId) {
        String idempotencyKey = buildBehavioralImprovementAnalysisIdempotencyKey(submissionId);
        String correlationId = LoggingContext.resolveCorrelationId();

        AsyncJob job = asyncJobRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> asyncJobRepository.save(AsyncJob.builder()
                        .jobType(AsyncJobType.BEHAVIORAL_IMPROVEMENT_ANALYSIS)
                        .status(AsyncJobStatus.QUEUED)
                        .aggregateType(AsyncJobAggregateType.BEHAVIORAL_SUBMISSION)
                        .aggregateId(submissionId)
                        .attemptCount(0)
                        .maxAttempts(asyncJobProperties.getDefaultMaxAttempts())
                        .scheduledAt(Instant.now())
                        .idempotencyKey(idempotencyKey)
                        .correlationId(correlationId)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
        if (job.getCorrelationId() == null || job.getCorrelationId().isBlank()) {
            job.setCorrelationId(correlationId);
            job.setUpdatedAt(Instant.now());
            job = asyncJobRepository.save(job);
        }

        log.info(
                "async_job_queued jobId={} jobType={} aggregateType={} aggregateId={} correlationId={} status={}",
                job.getId(),
                job.getJobType(),
                job.getAggregateType(),
                job.getAggregateId(),
                job.getCorrelationId(),
                job.getStatus());
        prepForgeMetrics.incrementBehavioralComparisonJobQueued();
        prepForgeMetrics.incrementAsyncJobQueued(job.getJobType().name());
        return job;
    }

    @Transactional
    public Optional<AsyncJob> claimNextEligibleJob() {
        List<AsyncJob> jobs = asyncJobRepository.findEligibleJobsForUpdate(
                ELIGIBLE_STATUSES,
                Instant.now(),
                PageRequest.of(0, 1));

        if (jobs.isEmpty()) {
            return Optional.empty();
        }

        AsyncJob job = jobs.get(0);
        job.setStatus(AsyncJobStatus.RUNNING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setStartedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        job.setFailureCode(null);
        job.setFailureMessage(null);
        AsyncJob claimedJob = asyncJobRepository.save(job);
        log.info(
                "async_job_claimed jobId={} jobType={} aggregateType={} aggregateId={} attemptCount={} correlationId={}",
                claimedJob.getId(),
                claimedJob.getJobType(),
                claimedJob.getAggregateType(),
                claimedJob.getAggregateId(),
                claimedJob.getAttemptCount(),
                claimedJob.getCorrelationId());
        return Optional.of(claimedJob);
    }

    @Transactional
    public void markSucceeded(UUID jobId) {
        AsyncJob job = getJobEntity(jobId);
        job.setStatus(AsyncJobStatus.SUCCEEDED);
        job.setCompletedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        asyncJobRepository.save(job);
        log.info(
                "async_job_succeeded jobId={} jobType={} aggregateType={} aggregateId={} attemptCount={} correlationId={}",
                job.getId(),
                job.getJobType(),
                job.getAggregateType(),
                job.getAggregateId(),
                job.getAttemptCount(),
                job.getCorrelationId());
        prepForgeMetrics.incrementAsyncJobSucceeded(job.getJobType().name());
        recordDuration(job);
    }

    @Transactional
    public void markFailedOrRetry(UUID jobId, AsyncJobFailureCode failureCode, String failureMessage, boolean retryable) {
        AsyncJob job = getJobEntity(jobId);
        job.setFailureCode(failureCode);
        job.setFailureMessage(truncateFailureMessage(failureMessage));
        job.setUpdatedAt(Instant.now());

        if (retryable && job.getAttemptCount() < job.getMaxAttempts()) {
            job.setStatus(AsyncJobStatus.RETRY_SCHEDULED);
            job.setScheduledAt(Instant.now().plusSeconds(calculateBackoffSeconds(job.getAttemptCount())));
        } else {
            job.setStatus(AsyncJobStatus.FAILED);
            job.setCompletedAt(Instant.now());
        }

        asyncJobRepository.save(job);
        log.warn(
                "async_job_failed jobId={} jobType={} aggregateType={} aggregateId={} status={} attemptCount={} maxAttempts={} failureCode={} correlationId={} message={}",
                job.getId(),
                job.getJobType(),
                job.getAggregateType(),
                job.getAggregateId(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getFailureCode(),
                job.getCorrelationId(),
                job.getFailureMessage());
        if (job.getStatus() == AsyncJobStatus.FAILED) {
            prepForgeMetrics.incrementAsyncJobFailed(
                    job.getJobType().name(),
                    job.getFailureCode() == null ? null : job.getFailureCode().name());
            recordDuration(job);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AsyncJob> findLatestBehavioralFeedbackJob(UUID submissionId) {
        return asyncJobRepository.findTopByAggregateTypeAndAggregateIdAndJobTypeOrderByCreatedAtDesc(
                AsyncJobAggregateType.BEHAVIORAL_SUBMISSION,
                submissionId,
                AsyncJobType.BEHAVIORAL_FEEDBACK_GENERATION);
    }

    @Transactional(readOnly = true)
    public AsyncJobStatusResponse getJobStatus(UUID jobId) {
        AsyncJob job = getOwnedJob(jobId);
        return mapJobStatus(job);
    }

    private AsyncJob getOwnedJob(UUID jobId) {
        AsyncJob job = getJobEntity(jobId);
        UUID currentUserId = getCurrentUser().getId();

        if (job.getAggregateType() == AsyncJobAggregateType.BEHAVIORAL_SUBMISSION) {
            BehavioralSubmission submission = behavioralSubmissionRepository.findById(job.getAggregateId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavioral submission not found"));

            if (!submission.getUserId().equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
            }
        }

        return job;
    }

    private AsyncJob getJobEntity(UUID jobId) {
        return asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private AsyncJobStatusResponse mapJobStatus(AsyncJob job) {
        return AsyncJobStatusResponse.builder()
                .jobId(job.getId())
                .jobType(job.getJobType().name())
                .status(job.getStatus().name())
                .aggregateId(job.getAggregateId())
                .attemptCount(job.getAttemptCount())
                .maxAttempts(job.getMaxAttempts())
                .scheduledAt(job.getScheduledAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .failureCode(job.getFailureCode() == null ? null : job.getFailureCode().name())
                .failureMessage(job.getFailureMessage())
                .build();
    }

    private String buildBehavioralFeedbackIdempotencyKey(UUID submissionId) {
        return AsyncJobType.BEHAVIORAL_FEEDBACK_GENERATION.name() + ":" + submissionId;
    }

    private String buildBehavioralImprovementAnalysisIdempotencyKey(UUID submissionId) {
        return AsyncJobType.BEHAVIORAL_IMPROVEMENT_ANALYSIS.name() + ":" + submissionId;
    }

    private long calculateBackoffSeconds(int attemptCount) {
        return asyncJobProperties.getRetryBackoffSeconds() * Math.max(attemptCount, 1);
    }

    private void recordDuration(AsyncJob job) {
        if (job.getStartedAt() == null) {
            return;
        }

        Instant finishedAt = job.getCompletedAt() != null ? job.getCompletedAt() : Instant.now();
        prepForgeMetrics.recordAsyncJobDuration(
                job.getJobType().name(),
                Duration.between(job.getStartedAt(), finishedAt));
    }

    private String truncateFailureMessage(String failureMessage) {
        if (failureMessage == null) {
            return null;
        }

        return failureMessage.length() <= 1000 ? failureMessage : failureMessage.substring(0, 1000);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return user;
    }
}
