package com.derwin.prepforge.jobs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jobs")
public class AsyncJobProperties {
    private int defaultMaxAttempts = 3;
    private long retryBackoffSeconds = 30;
    private Worker worker = new Worker();

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = true;
        private long pollDelayMs = 3000;
        private int maxJobsPerPoll = 3;
    }
}
