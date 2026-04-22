package com.derwin.prepforge.jobs;

public enum AsyncJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED
}
