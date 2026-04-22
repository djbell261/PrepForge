package com.derwin.prepforge.jobs.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncJobStatusResponse {
    private UUID jobId;
    private String jobType;
    private String status;
    private UUID aggregateId;
    private int attemptCount;
    private int maxAttempts;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private String failureCode;
    private String failureMessage;
}
