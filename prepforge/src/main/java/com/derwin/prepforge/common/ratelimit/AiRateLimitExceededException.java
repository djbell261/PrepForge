package com.derwin.prepforge.common.ratelimit;

import lombok.Getter;

@Getter
public class AiRateLimitExceededException extends RuntimeException {
    private final AiEndpointCategory endpointCategory;
    private final long retryAfterSeconds;

    public AiRateLimitExceededException(AiEndpointCategory endpointCategory, long retryAfterSeconds) {
        super("AI request rate limit exceeded for " + endpointCategory.getKey());
        this.endpointCategory = endpointCategory;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
