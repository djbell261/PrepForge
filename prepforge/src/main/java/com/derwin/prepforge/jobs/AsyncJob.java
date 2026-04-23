package com.derwin.prepforge.jobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "async_jobs")
public class AsyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AsyncJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AsyncJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AsyncJobAggregateType aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private Instant scheduledAt;

    private Instant startedAt;

    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private AsyncJobFailureCode failureCode;

    @Column(length = 1000)
    private String failureMessage;

    @Column(nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(length = 100)
    private String correlationId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
