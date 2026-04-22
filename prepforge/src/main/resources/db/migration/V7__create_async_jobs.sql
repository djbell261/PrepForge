CREATE TABLE async_jobs (
    id UUID PRIMARY KEY,
    job_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    failure_code VARCHAR(100) NULL,
    failure_message VARCHAR(1000) NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_async_jobs_status_scheduled_at ON async_jobs(status, scheduled_at);
CREATE INDEX idx_async_jobs_aggregate_lookup ON async_jobs(aggregate_type, aggregate_id, job_type, created_at DESC);
