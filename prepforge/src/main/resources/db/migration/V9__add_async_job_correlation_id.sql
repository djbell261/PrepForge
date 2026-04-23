ALTER TABLE async_jobs
    ADD COLUMN correlation_id VARCHAR(100);

CREATE INDEX idx_async_jobs_correlation_id ON async_jobs(correlation_id);
