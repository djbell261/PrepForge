package com.derwin.prepforge.infrastructure.observability;

import com.derwin.prepforge.jobs.AsyncJobRepository;
import com.derwin.prepforge.jobs.AsyncJobStatus;
import com.derwin.prepforge.jobs.AsyncJobType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PrepForgeMetrics {
    private static final List<AsyncJobStatus> QUEUE_DEPTH_STATUSES = List.of(AsyncJobStatus.QUEUED, AsyncJobStatus.RETRY_SCHEDULED);
    private static final List<AsyncJobStatus> PENDING_FEEDBACK_STATUSES = List.of(
            AsyncJobStatus.QUEUED,
            AsyncJobStatus.RUNNING,
            AsyncJobStatus.RETRY_SCHEDULED);

    private final MeterRegistry meterRegistry;

    public PrepForgeMetrics(MeterRegistry meterRegistry, AsyncJobRepository asyncJobRepository) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("prepforge.async.jobs.queue.depth", asyncJobRepository,
                        repository -> repository.countByStatusIn(QUEUE_DEPTH_STATUSES))
                .description("Current async job queue depth for runnable jobs")
                .register(meterRegistry);

        Gauge.builder("prepforge.behavioral.feedback.pending", asyncJobRepository,
                        repository -> repository.countByJobTypeAndStatusIn(
                                AsyncJobType.BEHAVIORAL_FEEDBACK_GENERATION,
                                PENDING_FEEDBACK_STATUSES))
                .description("Current count of behavioral feedback jobs not yet completed")
                .register(meterRegistry);
    }

    public void incrementAsyncJobQueued(String jobType) {
        counter("prepforge.async.jobs.queued", "jobType", jobType).increment();
    }

    public void incrementAsyncJobSucceeded(String jobType) {
        counter("prepforge.async.jobs.succeeded", "jobType", jobType).increment();
    }

    public void incrementAsyncJobFailed(String jobType, String failureCode) {
        counter("prepforge.async.jobs.failed", "jobType", jobType, "failureCode", sanitize(failureCode)).increment();
    }

    public void recordAsyncJobDuration(String jobType, Duration duration) {
        record("prepforge.async.jobs.duration", duration, "jobType", jobType);
    }

    public void incrementAiRequest(String category) {
        counter("prepforge.ai.requests", "category", category).increment();
    }

    public void incrementAiFailure(String category) {
        counter("prepforge.ai.failures", "category", category).increment();
    }

    public void recordAiLatency(String category, Duration duration) {
        record("prepforge.ai.latency", duration, "category", category);
    }

    public void incrementRateLimitHit(String endpointCategory) {
        counter("prepforge.rate_limit.hits", "endpointCategory", endpointCategory).increment();
    }

    public void incrementTimerExpiration(String sessionType) {
        counter("prepforge.timed_sessions.expirations", "sessionType", sessionType).increment();
    }

    public void incrementTimerLookup(String sessionType, String result) {
        counter("prepforge.timed_sessions.lookups", "sessionType", sessionType, "result", result).increment();
    }

    public void incrementExecutionRequest(String language) {
        counter("prepforge.execution.requests", "language", language).increment();
    }

    public void incrementExecutionCompileFailure(String language) {
        counter("prepforge.execution.compile_failures", "language", language).increment();
    }

    public void incrementExecutionRuntimeFailure(String language) {
        counter("prepforge.execution.runtime_failures", "language", language).increment();
    }

    public void incrementExecutionTimeout(String language, String phase) {
        counter("prepforge.execution.timeouts", "language", language, "phase", phase).increment();
    }

    public void incrementExecutionSuccess(String language) {
        counter("prepforge.execution.successes", "language", language).increment();
    }

    public void recordExecutionDuration(String language, Duration duration) {
        record("prepforge.execution.duration", duration, "language", language);
    }

    public void incrementBehavioralSubmission() {
        counter("prepforge.behavioral.submissions").increment();
    }

    public void incrementBehavioralComparisonJobQueued() {
        counter("prepforge.behavioral.comparison_jobs.queued").increment();
    }

    private Counter counter(String name, String... tags) {
        return meterRegistry.counter(name, tags);
    }

    private void record(String name, Duration duration, String... tags) {
        Timer.builder(name)
                .tags(tags)
                .register(meterRegistry)
                .record(duration == null ? Duration.ZERO : duration);
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
